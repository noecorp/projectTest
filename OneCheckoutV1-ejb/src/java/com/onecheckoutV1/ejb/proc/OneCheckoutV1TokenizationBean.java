 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.*;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.*;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.*;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author rienditya
 */
@Stateless
public class OneCheckoutV1TokenizationBean extends OneCheckoutChannelBase implements OneCheckoutV1TokenizationLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    @Resource
    protected SessionContext ctx;
    
    private boolean ableToReversal = true;             

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {

        Transactions trans = null;
        boolean isMPG = false;
        try {
            OneCheckoutV1CreditCardPGBase proc = null;
            PaymentChannel pcCore = null;
            if (queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), OneCheckoutPaymentChannel.CreditCard) != null) {
                OneCheckoutLogger.log(": : : MERCHANT HAS PAYMENT CHANNEL [%s]", OneCheckoutPaymentChannel.CreditCard.name());
                pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.CreditCard);
            } else if (queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), OneCheckoutPaymentChannel.BSP) != null) {
                OneCheckoutLogger.log(": : : MERCHANT HAS PAYMENT CHANNEL [%s]", OneCheckoutPaymentChannel.BSP.name());
                pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.BSP);
                isMPG = true;
            }
            Merchants m = trxHelper.getMerchant();
            
            OneCheckoutPaymentChannel ocopc = OneCheckoutPaymentChannel.findType(pcCore.getPaymentChannelId());
            String processor = super.getCorrectProcessor(trxHelper, pcCore );
            proc = (OneCheckoutV1CreditCardPGBase) ctx.lookup(processor);
            
            getCustomerInfoByToken(trxHelper);
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));
            
            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            boolean needNotify = false;
            String paramString = "";
            OneCheckoutErrorMessage errormsg = OneCheckoutErrorMessage.UNKNOWN;
            String CoreUrl = pcCore.getRedirectPaymentUrlMipXml();
            if (trxHelper.getMessage().equalsIgnoreCase("ACS")) {
                trans = trxHelper.getTransactions();
                OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.doPayment incoming from ACS");
                trxHelper.getPaymentRequest().getAllAdditionData().put("FROM", "ACS");
//                String paymentChannelId = pChannel.getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                trxHelper.getPaymentRequest().getAllAdditionData().put("OCOID", trxHelper.getOcoId());
                
                paramString = super.createParamsHTTP(trxHelper.getPaymentRequest().getAllAdditionData());
                if(isMPG)
                    CoreUrl = pcCore.getRedirectPaymentUrlMip();
                OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.doPayment DATA => %s", paramString);
            } else {
                OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.doPayment incoming from Merchant");
                IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pcCore, m);
                boolean request_good = identrx.isRequestGood();
                MerchantPaymentChannel mpc = identrx.getMpc();
                errormsg = identrx.getErrorMsg();
                needNotify = identrx.isNeedNotify();
                if (!request_good) {
                    trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, identrx.getTrans());
                    return trxHelper;
                }
                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }
                OneCheckoutDataPGRedirect datatoCore = proc.createRedirectMIP(paymentRequest, pcCore, mpc, m, trans.getOcoId());
                HashMap<String, String> params = datatoCore.getParameters();
                paramString = super.createParamsHTTP(params);
            }

            String resultPayment = "";
            try {
                resultPayment = super.postMIP(paramString, CoreUrl, pcCore);
                OneCheckoutLogger.log("Payment Channel: %s", trxHelper.getPaymentChannel());
                OneCheckoutLogger.log("RESULT PAYMENT \n%s", resultPayment);
            } catch (Exception ex) {
                OneCheckoutLogger.log("Got Exception: %s", ex.getMessage());
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE;
                trans.setDokuResponseCode(errormsg.value());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
       //         trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                trxHelper = this.doShowResultPage(trxHelper, pcCore);
                trxHelper.setMessage("VALID");                 
                return trxHelper;
            }

            XMLConfiguration xml = new XMLConfiguration();
            try {
                StringReader sr = new StringReader(resultPayment);
                xml.load(sr);
            } catch (Exception ex) {
                OneCheckoutLogger.log("Got Exception: %s", ex.getMessage());
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                trans.setDokuResponseCode(errormsg.value());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
         //       trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                trxHelper = this.doShowResultPage(trxHelper, pcCore);
                trxHelper.setMessage("VALID");                 
                return trxHelper;
            }

            String statusResp = xml.getString("status");
            if (statusResp != null && (statusResp.equalsIgnoreCase("THREEDSECURE") || statusResp.equalsIgnoreCase("3DSECURE"))) {
                try {
                    OneCheckoutDataPGRedirect redirect = proc.parsing3DSecureData(xml);
                    trxHelper.setRedirect(redirect);
                    trans.setInc3dSecureStatus(redirect.getParameters().get("MD"));
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());
                    em.merge(trans);
                    return trxHelper;
                } catch (Exception ex) {
                    OneCheckoutLogger.log("Got Exception: %s", ex.getMessage());
                    needNotify = true;
                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                    trans.setDokuResponseCode(errormsg.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
//                    trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                    trxHelper = this.doShowResultPage(trxHelper, pcCore);
                    trxHelper.setMessage("VALID");                     
                    return trxHelper;
                }
            } else {
                trans = proc.parseXMLPayment(trans, xml,m);
                trans = this.notifyStatusMerchantCustom(trans, trxHelper.getPaymentChannel());
                em.merge(trans);

                OneCheckoutRedirectData redirectRequest = null;
                trxHelper.setRedirectDoku(redirectRequest);
                trxHelper.setTransactions(trans);
                trxHelper = proc.doRedirectToMerchant(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
            }
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }
    }
    
    private String getCustomerInfoByToken(OneCheckoutDataHelper trxHelper) {
        OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
        MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(trxHelper.getMerchant(), OneCheckoutPaymentChannel.Tokenization);

        OneCheckoutChannelBase base = new OneCheckoutChannelBase();
    //    HashMap<String, String> params = new HashMap<String, String>();

        String reqDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String resp = "";
        try {

            StringBuilder sbWords = new StringBuilder();
            sbWords.append(trxHelper.getPaymentRequest().getMALLID());
            sbWords.append(mpc.getMerchantPaymentChannelHash());
            sbWords.append(trxHelper.getPaymentRequest().getCUSTOMERID());
            sbWords.append(reqDateTime);

            String hashwords = HashWithSHA1.doHashing(sbWords.toString(),"SHA1",null);

            StringBuilder sb = new StringBuilder("");
            sb.append("MALLID=").append(mpc.getPaymentChannelCode());
            sb.append("&CHAINMALLID=").append(trxHelper.getPaymentRequest().getCHAINMERCHANT());
            sb.append("&IDENTIFIERTYPE=").append("T");
            sb.append("&IDENTIFIERNO=").append(trxHelper.getPaymentRequest().getTOKENID());
            sb.append("&REQUESTTYPE=").append("Q");
            sb.append("&REQUESTDATETIME=").append(reqDateTime);
            sb.append("&WORDS=").append(hashwords);

            String urlGetInfo = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.GETINFObyTOKEN", "http://dokulocal_luna_app:8080/AccountBilling/DoServiceGetInfo");
            OneCheckoutLogger.log("=====================================================================");
            OneCheckoutLogger.log("Parameter to send : \n" + sb.toString());
            OneCheckoutLogger.log("send to " + urlGetInfo);

            InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo,  30, 30);
            resp = intResp.getMsgResponse();
            OneCheckoutLogger.log("Response Info Customer : %s", resp);
            XMLConfiguration xml = new XMLConfiguration();

            StringReader sr = new StringReader(resp);
            xml.load(sr);
            
            trxHelper.getPaymentRequest().setCARDNUMBER(xml.getString("TOKENS.TOKEN.CARDNUMBER"));
            trxHelper.getPaymentRequest().setEXPIRYDATE(xml.getString("TOKENS.TOKEN.EXPIRYDATE"));
            trxHelper.getPaymentRequest().setCC_NAME(xml.getString("TOKENS.TOKEN.CCNAME"));
            trxHelper.getPaymentRequest().setEMAIL(xml.getString("TOKENS.TOKEN.CCEMAIL"));
            trxHelper.getPaymentRequest().setADDRESS(xml.getString("TOKENS.TOKEN.CCADDRESS"));
            trxHelper.getPaymentRequest().setCITY(xml.getString("TOKENS.TOKEN.CCCITY"));
            trxHelper.getPaymentRequest().setSTATE(xml.getString("TOKENS.TOKEN.CCSTATE"));
            trxHelper.getPaymentRequest().setCOUNTRY(xml.getString("TOKENS.TOKEN.CCCOUNTRY"));
            trxHelper.getPaymentRequest().setZIPCODE(xml.getString("TOKENS.TOKEN.CCZIPCODE"));
            trxHelper.getPaymentRequest().setMOBILEPHONE(xml.getString("TOKENS.TOKEN.CCMOBILEPHONE"));
            trxHelper.getPaymentRequest().setHOMEPHONE(xml.getString("TOKENS.TOKEN.CCHOMEPHONE"));
            trxHelper.getPaymentRequest().setWORKPHONE(xml.getString("TOKENS.TOKEN.CCWORKPHONE"));
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return resp;
    }
    

    protected Transactions notifyStatusMerchantCustom(Transactions trans, OneCheckoutPaymentChannel pChannel) {
        OneCheckoutLogger.log("============NOTIFY MERCHANT=============");
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.notifyStatusMerchantCustom - T0: %d Start", (System.currentTimeMillis() - t1));

            OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();

   
            notify.setAMOUNT(trans.getIncAmount().doubleValue());
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());
            notify.setRESULTMSG(status.name());
            notify.setTRANSIDMERCHANT(trans.getIncTransidmerchant());
            notify.setPURCHASECURRENCY(trans.getIncPurchasecurrency());
            OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.findType(trans.getVerifyStatus());
            notify.setVERIFYSTATUS(dfsStatus.name());

//            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.notifyStatusMerchantCustom - %s", trans.getDokuResultMessage());
            
            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.notifyStatusMerchantCustom - T1: %d create notify parameters to Merchant", (System.currentTimeMillis() - t1));            

            HashMap<String, String> params = super.getData(trans);
            params.put("TOKENID", trans.getTokenId());
            params.put("CUSTOMERID", trans.getIncCustomerId());
            params.put("PAYMENTCODE", "");
            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.notifyStatusMerchantCustom - T2: %d start Notify to Merchant", (System.currentTimeMillis() - t1));            
            String resp = super.notifyStatusMerchant(trans, params, pChannel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);              

            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.notifyStatusMerchantCustom : statusNotify : %s", resp);
            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.notifyStatusMerchantCustom - T3: %d finish Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutLogger.log("OneCheckoutV1TokenizationBean.notifyStatusMerchantCustom - T4: %d Finish process", (System.currentTimeMillis() - t1));
            return trans;
        } catch (Exception ex) {
            ex.printStackTrace();
            return trans;
        }
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1RecurBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();

            String statusCode = "";
            String invoiceNo = null;
            String sessionId = null;
            double amount = 0.00;
            int attempts = 1;

            if (redirectRequest != null) {
                invoiceNo = redirectRequest.getTRANSIDMERCHANT();
                sessionId = redirectRequest.getSESSIONID();
                amount = redirectRequest.getAMOUNT();
                statusCode = redirectRequest.getSTATUSCODE();

            } else {

                invoiceNo = trxHelper.getTransactions().getIncTransidmerchant();
                sessionId = trxHelper.getTransactions().getIncSessionid();
                amount = trxHelper.getTransactions().getIncAmount().doubleValue();

            }

            Object[] obj = queryHelper.getRedirectTransactionWithoutStateNumber(invoiceNo, sessionId, amount);//.getRedirectTransactionBy(invoiceNo, sessionId, amount);

            attempts = (Integer) obj[0];
            Transactions trans = (Transactions) obj[1];

            if (redirectRequest == null) {
                trans = trxHelper.getTransactions();
                statusCode = trans.getDokuResponseCode();
            }

            OneCheckoutLogger.log("ATTEMPTS : " + attempts);

            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {

                trans.setDokuResponseCode(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.name());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            }

            //set data redirect yang akan di kirim ke merchant
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();

            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());


            // 0 = FROM CHANNEL, 1 = RETRY, 2 = REDIRECT TO MERCHANT / KLIK CONTINUE
            if (redirectRequest != null && redirectRequest.getFLAG().equalsIgnoreCase("1")) {

                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=1");

                if ((m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) && trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                    OneCheckoutLogger.log("FLAG=1 A");
                    if (attempts >= 3) {
                        OneCheckoutLogger.log("FLAG=1 A1");
                        //    redirect.setPageTemplate("transactionFailed.html");

                        //ERROR PAGE 3 ATTEMPTS
                        redirect.setUrlAction(m.getMerchantRedirectUrl());
                        redirect.setPageTemplate("transactionFailed.html");
                    } else {
                        OneCheckoutLogger.log("FLAG=1 A2");
                        //PAYMENT PAGE
                        redirect.setUrlAction(config.getString("URL.RECEIVE"));
                        redirect.setPageTemplate("redirect.html");

                        //MUST GENERATE PAYMENT REQUEST PARAMETER
                        data = super.generatePaymentRequest(trans);
                        redirect.setParameters(new HashMap(data));
                    }

                } else {
                    OneCheckoutLogger.log("FLAG=1 A1");
                    redirect.setAMOUNT(amount);
                    redirect.setTRANSIDMERCHANT(invoiceNo);
                    redirect.setSTATUSCODE(statusCode);

                    redirect.setUrlAction(m.getMerchantRedirectUrl());
                    redirect.setPageTemplate("redirect.html");

                }
                trxHelper.setRedirectDoku(redirectRequest);
            } else if (redirectRequest != null && redirectRequest.getFLAG().equalsIgnoreCase("2")) {
                statusCode = trans.getDokuResponseCode();
                redirectRequest.setSTATUSCODE(statusCode);
                trxHelper.setRedirectDoku(redirectRequest);


                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
                // redirect = redirectToMerchant(redirect, redirectRequest, trans, m, trxHelper, data);
                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(redirectRequest.getSTATUSCODE());

                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");

            } else {

                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(statusCode);

                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0");
                HashMap resultData = redirect.getRetryData();


                resultData.put("DATENOW", OneCheckoutVerifyFormatData.detailDateFormat.format(new Date()));
                resultData.put("DISPLAYNAME", m.getMerchantName());
                resultData.put("DISPLAYADDRESS", m.getMerchantAddress());
                resultData.put("CREDIT_CARD", trans.getAccountId());

                Currency currency = queryHelper.getCurrencyByCode(trans.getIncCurrency());

                resultData.put("CURRENCY", currency.getAlpha3Code());
                resultData.put("INVOICE", invoiceNo);
                resultData.put("DISPLAYAMOUNT", OneCheckoutVerifyFormatData.moneyFormat.format(trans.getIncAmount()));

                if (m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A1");
                    redirect.setRetry(Boolean.TRUE);
                    resultData.put("ATTEMPTS", String.valueOf(attempts));
                }

                redirect.setUrlAction(config.getString("URL.GOBACK"));
                resultData.put("DISPLAYCHANNEL", trxHelper.getPaymentChannel().value());
                OneCheckoutLogger.log("TRANSACTION STATUS [" + trans.getTransactionsStatus() + "]");

                if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {

                    if (trans.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()) {
                        redirect.setRetry(Boolean.FALSE);
                    }
                    
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");
                    redirect.setPageTemplate("transactionFailed.html");

                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A4");
                    redirect.setPageTemplate("transactionSuccess.html");

                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with " + m.getMerchantName());

                    resultData.put("APPROVAL", trans.getDokuApprovalCode());

                }
                redirect.setRetryData(resultData);
                HashMap<String, String> params = super.getData(trans);
                params.put("TOKENID", trans.getTokenId());
                params.put("CUSTOMERID", trans.getIncCustomerId());
                params.put("PAYMENTCODE", "");
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
                trans = super.notifyOnRedirect(trans, params, channel);  

            }

            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT

                
            data.put("PURCHASECURRENCY", redirect.getPURCHASECURRENCY());
      //      data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(amount));
            data.put("TRANSIDMERCHANT", invoiceNo);
            data.put("STATUSCODE", statusCode);
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", sessionId);
            data.put("PAYMENTCODE", "");

            if (trans.getRates()!=null && redirect.getUrlAction().equals(m.getMerchantRedirectUrl())) {
                redirect.setAMOUNT(trans.getIncPurchaseamount().doubleValue());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
                data.put("CURRENCY", trans.getIncPurchasecurrency());//.getPURCHASECURRENCY());
            }
            else {
                redirect.setAMOUNT(trans.getIncAmount().doubleValue());//.getAMOUNT());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));    
                data.put("CURRENCY", trans.getIncCurrency());

            }               
            String word = super.generateRedirectWords(redirect, m);
            data.put("WORDS", word);
            
            StringBuilder sb = new StringBuilder();

            OneCheckoutLogger.log("============== START REDIRECT PARAMETER ==============");
            OneCheckoutLogger.log("MERCHANT URL         [" + m.getMerchantRedirectUrl() + "]");
            OneCheckoutLogger.log("AMOUNT               [" + data.get("AMOUNT") + "]");
            OneCheckoutLogger.log("TRANSIDMERCHANT      [" + data.get("TRANSIDMERCHANT") + "]");
            OneCheckoutLogger.log("WORDS                [" + data.get("WORDS") + "]");
            OneCheckoutLogger.log("STATUSCODE           [" + data.get("STATUSCODE") + "]");
            OneCheckoutLogger.log("PAYMENTCHANNEL       [" + data.get("PAYMENTCHANNEL") + "]");
            OneCheckoutLogger.log("SESSIONID            [" + data.get("SESSIONID") + "]");
            OneCheckoutLogger.log("PAYMENTCODE          [" + data.get("PAYMENTCODE") + "]");
            OneCheckoutLogger.log("CURRENCY             [" + data.get("CURRENCY") + "]");
            OneCheckoutLogger.log("PURCHASECURRENCY     [" + data.get("PURCHASECURRENCY") + "]");            
            OneCheckoutLogger.log("============== END  REDIRECT  PARAMETER ==============");
            
            
            Long t2 = System.currentTimeMillis();
            OneCheckoutLogger.log("START T0        [%d]", (System.currentTimeMillis() - t2));
            sb.append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(amount)).append("&");
            sb.append("TRANSIDMERCHANT=").append(invoiceNo).append("&");
            sb.append("WORDS=").append(word).append("&");
            sb.append("STATUSCODE=").append(statusCode).append("&");
            sb.append("PAYMENTCHANNEL=").append(trxHelper.getPaymentChannel().value()).append("&");
            sb.append("SESSIONID=").append(sessionId).append("&");
            sb.append("PAYMENTCODE=").append(sessionId).append("&");
//            String paymentChannelId = trxHelper.getPaymentChannel().value();
//            String ocoId = this.generateOcoId(paymentChannelId);
            sb.append("OCOID=").append(trxHelper.getOcoId());
            

            InternetResponse internetResponse = doFetchHTTP(sb.toString(), m.getMerchantRedirectUrl(),m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

            OneCheckoutLogger.log("HTTP CONNECTION [%d]", internetResponse.getHTTPrespCode());
            OneCheckoutLogger.log("HTTP MESSAGE    [%d]", internetResponse.getMsgResponse());
            OneCheckoutLogger.log("RESPONSE        [%d]", internetResponse.getMsgResponse());
            OneCheckoutLogger.log("FINISH T1       [%d]", (System.currentTimeMillis() - t2));
            OneCheckoutLogger.log("=============== END REDIRECT PARAMETER ==============");

            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1RecurBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        try {
            OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
            OneCheckoutV1CreditCardPGBase proc = null;

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();
            String wordDoku = super.generateVoidWords(voidRequest, m);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - Checking Hash WORDS");
            if (!wordDoku.equalsIgnoreCase(voidRequest.getWORDS())) {
                voidRequest.setVOIDRESPONSE("FAILED");
                trxHelper.setMessage("VALID");
                trxHelper.setVoidRequest(voidRequest);
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - Hash WORDS doesn't match");
                return trxHelper;
            }
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - Hash WORDS match");
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, paymentChannel, OneCheckoutTransactionState.DONE);
            if (trans != null) {
                MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());
                if (status == OneCheckoutTransactionStatus.SUCCESS) {
                    PaymentChannel pcCore = null;
                    if (queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), OneCheckoutPaymentChannel.CreditCard) != null) {
                        pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.CreditCard);
                        OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - ready to void");
                        StringBuilder sb = new StringBuilder();
                        sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
                        sb.append("&").append("OrderNumber=").append(invoiceNo);
                        sb.append("&").append("ApprovalCode=").append(trans.getDokuApprovalCode());
                        //Tambahan SWIPER
                        sb.append("&").append("MerchantID=").append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
                        sb.append("&").append("PurchaseAmt=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
                        sb.append("&").append("PurchaseMDR=").append("TEST");

                     //   HashMap<String, String> params = new HashMap<String, String>();
                        OneCheckoutLogger.log("VOID PARAM : " + sb.toString());
                        InternetResponse inetResp = super.doFetchHTTP(sb.toString(), pcCore.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                        voidRequest = DOKUVoidResponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                        OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
                        if (voidRequest.getVoidStatus().equalsIgnoreCase("VOIDED")) {
                            trans.setDokuVoidApprovalCode(voidRequest.getApprovalCode());
                            trans.setDokuVoidDatetime(new Date());
                            trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                            if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                                voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                            } else {
                                voidRequest.setVOIDRESPONSE("SUCCESS");
                            }
                            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - update trx as voided");
                        } else {
                            trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                            trans.setDokuVoidDatetime(new Date());
                            if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                                voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                            } else {
                                voidRequest.setVOIDRESPONSE("FAILED");
                            }
                            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - update response code void");
                        }
                        em.merge(trans);
                    } else if (queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), OneCheckoutPaymentChannel.BSP) != null) {
                        pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.BSP);
                        OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - ready to void");
                        StringBuilder sb = new StringBuilder();
                        voidRequest.setApprovalCode(trans.getDokuApprovalCode());
                        sb.append("TRXCODE=").append(trans.getDokuVoidApprovalCode());
                        sb.append("&").append("SERVICEID=").append(mpc.getPaymentChannel().getServiceId());
                        sb.append("&").append("PAYMENTAPPROVALCODE=").append(trans.getDokuApprovalCode());
                        sb.append("&").append("WORDS=").append(super.generateMPGVoidWords(voidRequest, mpc));

                     //   HashMap<String, String> params = new HashMap<String, String>();
                        OneCheckoutLogger.log("VOID PARAM : "+sb.toString());
                        InternetResponse inetResp = super.doFetchHTTP(sb.toString(), pcCore.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                        voidRequest = MPGVoidReponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                        OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
                        if (voidRequest.getVoidStatus().equalsIgnoreCase("SUCCESS")) {
                            trans.setDokuVoidApprovalCode(voidRequest.getApprovalCode());
                            trans.setDokuVoidDatetime(new Date());
                            trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                            if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                                voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                            } else {
                                voidRequest.setVOIDRESPONSE("SUCCESS");
                            }
                            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - update trx as voided");
                        } else {
                            trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                            trans.setDokuVoidDatetime(new Date());
                            if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                                voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse() + ";" + voidRequest.getMESSAGE());
                            } else {
                                voidRequest.setVOIDRESPONSE("FAILED"  + ";" + voidRequest.getMESSAGE());
                            }
                            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - update response code void");
                        }
                        em.merge(trans);
                    }

                } else if (status == OneCheckoutTransactionStatus.VOIDED) {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(super.generateVoidResponse(trans, "VOIDED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - trx already voided");
                } else {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(super.generateVoidResponse(trans, "FAILED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("FAILED");
                    }
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - trx can't be voided");
                }
            } else {
                if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                    voidRequest.setVOIDRESPONSE(super.generateVoidResponse(null, ""));
                } else {
                    voidRequest.setVOIDRESPONSE("NOT FOUND");
                }
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - trx isn't found");
            }
            trxHelper.setMessage("VALID");
            trxHelper.setVoidRequest(voidRequest);
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - T3: %d Finish process", (System.currentTimeMillis() - t1));
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return trxHelper;
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        OneCheckoutDataHelper checkoutDataHelper = null;
        try {
            OneCheckoutV1CreditCardPGBase proc = null;
            PaymentChannel pcCore = null;
            if (queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), OneCheckoutPaymentChannel.CreditCard) != null) {
                OneCheckoutLogger.log(": : : MERCHANT HAS PAYMENT CHANNEL [%s]", OneCheckoutPaymentChannel.CreditCard.name());
                pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.CreditCard);
            } else if (queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), OneCheckoutPaymentChannel.BSP) != null) {
                OneCheckoutLogger.log(": : : MERCHANT HAS PAYMENT CHANNEL [%s]", OneCheckoutPaymentChannel.BSP.name());
                pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.BSP);
            }
            Merchants m = trxHelper.getMerchant();
            OneCheckoutPaymentChannel ocopc = OneCheckoutPaymentChannel.findType(pcCore.getPaymentChannelId());
            String processor = super.getCorrectProcessor(m.getMerchantCode(), ocopc, pcCore );
            proc = (OneCheckoutV1CreditCardPGBase) ctx.lookup(processor);

            //OneCheckoutPaymentChannel ocopc = OneCheckoutPaymentChannel.findType(pcCore.getPaymentChannelId());
            //String processor = super.getCorrectProcessor(trxHelper, pcCore );
            //proc = (OneCheckoutV1CreditCardPGBase) ctx.lookup(processor);
            if (proc != null) {
                checkoutDataHelper = proc.doCheckStatus(trxHelper, pcCore);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return checkoutDataHelper;
    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}