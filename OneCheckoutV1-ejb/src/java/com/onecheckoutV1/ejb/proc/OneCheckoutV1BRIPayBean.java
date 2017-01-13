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
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.*;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author hafizsjafioedin
 */
@Stateless
public class OneCheckoutV1BRIPayBean extends OneCheckoutChannelBase implements OneCheckoutV1BRIPayBeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;

    private boolean ableToReversal = true;
    
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;


    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();

            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
            String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
            boolean request_good = identrx.isRequestGood();
            String statusCode = identrx.getStatusCode();
            MerchantPaymentChannel mpc = identrx.getMpc();
            boolean needNotify = identrx.isNeedNotify();
            OneCheckoutErrorMessage errormsg = identrx.getErrorMsg();

            if (request_good) {

                OneCheckoutDataPGRedirect redirect = null;

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                    redirect = createRedirectMIP(paymentRequest, pChannel, mpc, m, trans.getOcoId());
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                    redirect = createRedirectMIP(paymentRequest, pChannel, mpc, m, trans.getOcoId());
                }

                trxHelper.setMessage("VALID");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);
            } else {

                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);


            }

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doPayment - T2: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
            OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
            voidRequest.setVOIDRESPONSE("FAILED");
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doVoid : Void is not permitted");

            return trxHelper;
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();

            String invoiceNo = verifyRequest.getTRANSIDMERCHANT();
            String sessionId = verifyRequest.getSESSIONID();
            double amount = verifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInquiryInvoice - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getVerifyTransactionBy(invoiceNo, sessionId, amount, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInquiryInvoice : Transaction is null");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            /* ternyata belum di implement */
            String word = super.generateDOKUVerifyRequestWords(verifyRequest, trans);

            if (!word.equalsIgnoreCase(verifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInquiryInvoice : WORDS doesn't match !");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;


            }

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInquiryInvoice - T2: %d update transaction", (System.currentTimeMillis() - t1));

            em.merge(trans);

            verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);

            trxHelper.setVerifyRequest(verifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getVerifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuInvokeStatusDatetime(new Date());
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
            trans.setVerifyId(notifyRequest.getDFSId());
            trans.setVerifyScore(notifyRequest.getDFSScore());
            trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
      //      trans.setEduStatus(notifyRequest.getDFSStatus().value());

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getRESULT() != null && notifyRequest.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());


            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus - %s", trans.getDokuResultMessage());
            
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));


            em.merge(trans);


            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();

            String invoiceNo = redirectRequest.getTRANSIDMERCHANT();
            String sessionId = redirectRequest.getSESSIONID();
            double amount = redirectRequest.getAMOUNT();

            Object[] obj = queryHelper.getRedirectTransactionWithoutStateNumber(invoiceNo, sessionId, amount);//.getRedirectTransactionBy(invoiceNo, sessionId, amount);

            int attempts = (Integer) obj[0];
            Transactions trans = (Transactions) obj[1];

            OneCheckoutLogger.log("ATTEMPTS : " + attempts);

            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {

                trans.setDokuResponseCode(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.name());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            }

            //set data redirect yang akan di kirim ke merchant
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();

            // ADD PARAMETER CURRENCY IN WORDS AND REDIRECT
            
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());            

            // 0 = FROM CHANNEL, 1 = RETRY, 2 = REDIRECT TO MERCHANT / KLIK CONTINUE
            if (redirectRequest.getFLAG().equalsIgnoreCase("1")) {

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
                    redirect.setAMOUNT(redirectRequest.getAMOUNT());
                    redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
                    redirect.setSTATUSCODE(redirectRequest.getSTATUSCODE());

                    redirect.setUrlAction(m.getMerchantRedirectUrl());
                    redirect.setPageTemplate("redirect.html");

                }

            } else if (redirectRequest.getFLAG() == null || redirectRequest.getFLAG().equalsIgnoreCase("0")) {


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

                OneCheckoutLogger.log("TRANSACTION STATUS ["+trans.getTransactionsStatus()+"]");

                if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {

                    if (!redirectRequest.getSTATUSCODE().equalsIgnoreCase(trans.getDokuResponseCode())) {
                        OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-0");
                        redirectRequest.setSTATUSCODE(trans.getDokuResponseCode());
                        trxHelper.setRedirectDoku(redirectRequest);
                    }


                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");

                    redirect.setPageTemplate("transactionFailed.html");

                } else if (trans.getTransactionsStatus()==OneCheckoutTransactionStatus.SUCCESS.value() && !redirectRequest.getSTATUSCODE().equalsIgnoreCase("0000")) {
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                    redirectRequest.setSTATUSCODE(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                    trxHelper.setRedirectDoku(redirectRequest);


                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");
                    redirect.setPageTemplate("transactionFailed.html");

                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A3");
                    redirect.setPageTemplate("transactionSuccess.html");

                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with "+m.getMerchantName());

                    resultData.put("APPROVAL", trans.getDokuApprovalCode());

                }
                redirect.setRetryData(resultData);

                HashMap<String, String> params = super.getData(trans);
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
                trans = super.notifyOnRedirect(trans, params, channel);

            } else {

                redirectRequest.setSTATUSCODE(trans.getDokuResponseCode());
                trxHelper.setRedirectDoku(redirectRequest);
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
               // redirect = redirectToMerchant(redirect, redirectRequest, trans, m, trxHelper, data);
                redirect.setAMOUNT(redirectRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(redirectRequest.getSTATUSCODE());

                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");

            }

            data.put("TRANSIDMERCHANT", redirectRequest.getTRANSIDMERCHANT());

            data.put("STATUSCODE", redirectRequest.getSTATUSCODE());
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", redirectRequest.getSESSIONID());
            data.put("PAYMENTCODE", "");
            data.put("PURCHASECURRENCY", redirect.getPURCHASECURRENCY());

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
            data.put("WORDS", super.generateRedirectWords(redirect, m));
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


            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            trans.setRedirectDatetime(new Date());
            em.merge(trans);

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    
    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
//        OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();

            String invoiceNo = statusRequest.getTRANSIDMERCHANT();

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            
            Transactions trans = trxHelper.getTransactions();
            if (trans != null) {
                String word = super.generateCheckStatusRequestWords(trans, m);
                if (!word.equalsIgnoreCase(checkStatusRequest.getWORDS())) {
                    // Create Empty Notify
                    OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
                    //notify.setWORDS(word);
                    statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
                    return trxHelper;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode() + "");
                if (trans.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && trans.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                    sb.append("&").append("CHAINMALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
                }
                sb.append("&").append("ACQUIRERID=").append("151");
                sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNo);
                sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
                
                OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
                InternetResponse inetResp = super.doFetchHTTP(sb.toString(), trans.getMerchantPaymentChannel().getPaymentChannel().getCheckStatusUrl(),  m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                    xml.load(sr);
                    String paymentStatus = xml.getString("STATUS") != null ? xml.getString("STATUS").trim() : "";
                    if (paymentStatus != null && !paymentStatus.trim().equalsIgnoreCase("") && !paymentStatus.trim().equalsIgnoreCase("INVALID_REQUEST")) {
                        if (!paymentStatus.trim().equalsIgnoreCase("TRX_NOT_FOUND")) {
                            String paymentResponseCode = xml.getString("RESPONSECODE") != null ? xml.getString("RESPONSECODE").trim() : "99";
                            String paymentRefNumber = xml.getString("HOSTREFNUMBER") != null ? xml.getString("HOSTREFNUMBER").trim() : "";
                            String cancelResponseCode = xml.getString("CANCELRESPONSECODE") != null ? xml.getString("CANCELRESPONSECODE").trim() : "";
                            String cancelRefNumber = xml.getString("CANCELHOSTREFNUMBER") != null ? xml.getString("CANCELHOSTREFNUMBER").trim() : "";
                            String acquirerBank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
                            String maskedCard = xml.getString("CARDNUMBER") != null ? xml.getString("CARDNUMBER").trim() : "";
                            String paymentDateTime = xml.getString("DATETIME") != null ? xml.getString("DATETIME").trim() : "";
                            String bank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";

                            if (paymentResponseCode.length() == 2) {
                                paymentResponseCode = "00" + paymentResponseCode;
                            } else {
                                paymentResponseCode = "0099";
                            }
                            trans.setDokuIssuerBank(bank);
                            trans.setDokuResponseCode(paymentResponseCode);
                            trans.setDokuHostRefNum(paymentRefNumber);
                            try {
                                OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                                notifyRequest.setDFSScore("-1");
                                notifyRequest.setDFSIId("");
                                trans.setVerifyId("");
                                trans.setVerifyScore(-1);
                                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                            //    trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                            } catch (Throwable t) {
                                trans.setVerifyScore(0);
                            }

                            if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                            } else if (paymentStatus.trim().equalsIgnoreCase("REVERSAL")) {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
                            } else if (paymentStatus.trim().equalsIgnoreCase("VOID")) {
                                //trans.setDokuVoidApprovalCode(cancelResponseCode);
                                trans.setDokuVoidResponseCode(cancelResponseCode);
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                            } else if (paymentStatus.trim().equalsIgnoreCase("REFUND")) {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.REFUND.value());
                            } else {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                            }
                            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            trans.setDokuInvokeStatusDatetime(new Date());
                            em.merge(trans);
                        } else {
                            trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                        }
                    } else {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                    }
                } else {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                }
                
                HashMap<String, String> params =  super.getData(trans);
                params.put("PAYMENTCODE","");

                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
                trxHelper.setMessage("VALID");
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params,m));
                trxHelper.setCheckStatusRequest(statusRequest);
                return trxHelper;
            }


            // Create Empty Notify
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest,  trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);

            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());

            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");

            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest,  trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }
    

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
       try {

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlMip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            HashMap<String, String> data = redirect.getParameters();

            data.put("PurchaseCurrency", paymentRequest.getCURRENCY());
            data.put("BASKET", paymentRequest.getBASKET());
            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));

            // Perlu ditanyakan
            data.put("password", "");

            data.put("WORDS", super.generateDokuWords(paymentRequest, mpc));
            data.put("MERCHANTID", mpc.getMerchantPaymentChannelUid());
            data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());

            // Perlu ditanyakan
            data.put("acquirerBIN", "");

            data.put("CURRENCY", paymentRequest.getCURRENCY());
            data.put("TRANSACTIONDATE", OneCheckoutVerifyFormatData.klikbca_datetimeFormat.format(paymentRequest.getREQUESTDATETIME()));
            //data.put("MALLID", m.getMerchantCode() + "");
            data.put("MALLID", mpc.getPaymentChannelCode() + "");
            data.put("TYPE", "SINGLE");
            data.put("SESSIONID", paymentRequest.getSESSIONID());
            //String ocoId = this.generateOcoId(pChannel.getPaymentChannelId());
            data.put("OCOID", ocoId);
//            data.put("IP",paymentRequest.getCUSTIP() != null ?  paymentRequest.getCUSTIP() : "");
            
//            data.put("USERNAME",paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");
//            data.put("USERNAME",mpc.getMerchants().getMerchantName() != null ? mpc.getMerchants().getMerchantName() : "");
            
//            data.put("REASON",paymentRequest.getPAYREASON() != null ? paymentRequest.getPAYREASON() : "");

            redirect.setParameters(data);

            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}