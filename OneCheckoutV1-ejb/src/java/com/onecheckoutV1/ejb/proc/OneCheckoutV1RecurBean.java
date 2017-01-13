/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.DOKUVoidResponseXML;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.MPGVoidReponseXML;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import com.onechekoutv1.dto.TransactionsDataCardholder;
import com.onechekoutv1.dto.TransactionsDataRecur;
import doku.keylib.tools.KMCardUtils;
import java.io.StringReader;
import java.net.URLEncoder;
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
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author hafizsjafioedin
 */
@Stateless
public class OneCheckoutV1RecurBean extends OneCheckoutChannelBase implements OneCheckoutV1RecurLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;
    private boolean ableToReversal = true;   
    //  @EJB
    //   protected MIPVNConnectorLocal connector;
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    @Resource
    protected SessionContext ctx;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;
        try {
            OneCheckoutLogger.log("OneCheckoutV1RecurBean.doPayment...");
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutV1CreditCardPGBase oneCheckoutV1CreditCardPGBase = null;
            PaymentChannel pcCore = null;

            MerchantPaymentChannel merchantPaymentChannel = queryHelper.getMerchantPaymentChannel(m, OneCheckoutPaymentChannel.CreditCard);
            if (merchantPaymentChannel != null) {
                OneCheckoutLogger.log(": : : MERCHANT HAS PAYMENT CHANNEL [%s]", OneCheckoutPaymentChannel.CreditCard.name());
                pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.CreditCard);
            } else {
                merchantPaymentChannel = queryHelper.getMerchantPaymentChannel(m, OneCheckoutPaymentChannel.BSP);
                if (merchantPaymentChannel != null) {
                    OneCheckoutLogger.log(": : : MERCHANT HAS PAYMENT CHANNEL [%s]", OneCheckoutPaymentChannel.BSP.name());
                    pcCore = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.BSP);
                }
            }

            OneCheckoutPaymentChannel ocopc = OneCheckoutPaymentChannel.findType(pcCore.getPaymentChannelId());
            String processor = super.getCorrectProcessor(trxHelper, pcCore);
            oneCheckoutV1CreditCardPGBase = (OneCheckoutV1CreditCardPGBase) ctx.lookup(processor);
            MerchantPaymentChannel mpcCore = queryHelper.getMerchantPaymentChannel(m, pcCore);

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            boolean needNotify = false;
            String paramString = "";
            OneCheckoutErrorMessage errormsg = OneCheckoutErrorMessage.UNKNOWN;
            // check disini
            String CoreUrl = oneCheckoutV1CreditCardPGBase.getCoreMipURL(pcCore, "DEFAULT");//, CoreUrl)pChannel.getRedirectPaymentUrlMipXml();
            if (trxHelper.getMessage().equalsIgnoreCase("ACS")) {
                trans = trxHelper.getTransactions();
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment incoming from ACS");
                trxHelper.getPaymentRequest().getAllAdditionData().put("FROM", "ACS");
//                String paymentChannelId = pChannel.getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                trxHelper.getPaymentRequest().getAllAdditionData().put("OCOID", trxHelper.getOcoId());
                
                paramString = super.createParamsHTTP(trxHelper.getPaymentRequest().getAllAdditionData());
                CoreUrl = oneCheckoutV1CreditCardPGBase.getCoreMipURL(pcCore, "ACS");
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment DATA => %s", paramString);
            } else {
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment incoming from Merchant");
                IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
                boolean request_good = identrx.isRequestGood();
                MerchantPaymentChannel mpc = identrx.getMpc();
                errormsg = identrx.getErrorMsg();
                needNotify = identrx.isNeedNotify();

                if (!request_good) {
                    trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, identrx.getTrans());
                    trxHelper.getRedirect().getRetryData().remove("MSG");
                    trxHelper.getRedirect().getRetryData().put("MSG", "Registration Failed");
                    return trxHelper;
                }

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }
                double amount_ori = paymentRequest.getAMOUNT();
                paymentRequest.setAMOUNT("10000.00");
                OneCheckoutDataPGRedirect datatoCore = oneCheckoutV1CreditCardPGBase.createRedirectMIP(paymentRequest, pcCore, mpcCore, m, trans.getOcoId());
                paymentRequest.setAMOUNT(amount_ori);
                HashMap<String, String> params = datatoCore.getParameters();
                paramString = super.createParamsHTTP(params);
                String basket = "Recur Authorize Card,10000.00,1,10000.00";
                paramString = "PurchaseDesc=" + URLEncoder.encode(basket, "UTF-8") + "&" + paramString;
            }
            String resultPayment = "";
            try {
                resultPayment = super.postMIP(paramString, CoreUrl, pChannel);
            } catch (Exception ex) {
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE;
                trans.setDokuResponseCode(errormsg.value());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
    //            trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");                 
                trxHelper.getRedirect().getRetryData().remove("MSG");
                trxHelper.getRedirect().getRetryData().put("MSG", "Registration Failed");
                return trxHelper;
            }
            OneCheckoutLogger.log("RESULTPAYMENT[" + resultPayment + "]");

            XMLConfiguration xml = new XMLConfiguration();
            try {
                StringReader sr = new StringReader(resultPayment);
                xml.load(sr);
            } catch (Exception ex) {
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                trans.setDokuResponseCode(errormsg.value());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
//                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");                 
                trxHelper.getRedirect().getRetryData().remove("MSG");
                trxHelper.getRedirect().getRetryData().put("MSG", "Registration Failed");
                return trxHelper;
            }

            String statusResp = xml.getString("status");
            if (statusResp != null && (statusResp.equalsIgnoreCase("THREEDSECURE") || statusResp.equalsIgnoreCase("3DSECURE"))) {
                try {
                    OneCheckoutDataPGRedirect redirect = oneCheckoutV1CreditCardPGBase.parsing3DSecureData(xml);
                    trxHelper.setRedirect(redirect);
                    trans.setInc3dSecureStatus(redirect.getParameters().get("MD"));
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());
                    em.merge(trans);
                    return trxHelper;
                } catch (Exception ex) {
                    needNotify = true;
                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                    trans.setDokuResponseCode(errormsg.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
//                    trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
                    trxHelper.setMessage("VALID");                     
                    trxHelper.getRedirect().getRetryData().remove("MSG");
                    trxHelper.getRedirect().getRetryData().put("MSG", "Registration Failed");
                    return trxHelper;
                }
            } else {
                trans = oneCheckoutV1CreditCardPGBase.parseXMLPayment(trans, xml,m);
                trxHelper.setTransactions(trans);
                if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value()) {
                    // ===========================
                    // REGISTER TO ACCOUNT BILLING
                    // ===========================
                    trxHelper = this.doRegistrationRecAccountBilling(trxHelper, pChannel);

                    TransactionsDataRecur recur = trxHelper.getTransactions().getTransactionsDataRecur();
                    if (trxHelper == null || trxHelper.getNotifyRequest().getRESULTMSG().equals("FAILED")) {

                        OneCheckoutLogger.log("FAILED REGISTER TO ACCOUNTBILLING, VOID THE TRANSACTION...");
                        OneCheckoutVoidRequest oneCheckoutVoidRequest = new OneCheckoutVoidRequest(merchantPaymentChannel.getPaymentChannelCode() + "");
                        if (merchantPaymentChannel.getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.CreditCard.value())) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("MALLID=").append(merchantPaymentChannel.getPaymentChannelCode());
                            sb.append("&").append("OrderNumber=").append(trans.getIncTransidmerchant());
                            sb.append("&").append("ApprovalCode=").append(trans.getDokuApprovalCode());
                            sb.append("&").append("MerchantID=").append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
                            sb.append("&").append("PurchaseAmt=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
                            sb.append("&").append("PurchaseMDR=").append("TEST");
//                            String paymentChannelId = merchantPaymentChannel.getPaymentChannel().getPaymentChannelId();
//                            String ocoId = this.generateOcoId(paymentChannelId);
                            sb.append("&").append("OCOID=").append(trxHelper.getOcoId());

                            
                            OneCheckoutLogger.log("VOID PARAM : " + sb.toString());
                            
                            InternetResponse inetResp = super.doFetchHTTP(sb.toString(), merchantPaymentChannel.getPaymentChannel().getVoidUrl(),  m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                            oneCheckoutVoidRequest = DOKUVoidResponseXML.parseResponseXML(inetResp.getMsgResponse(), oneCheckoutVoidRequest);
                            OneCheckoutLogger.log("Void Status : %s", oneCheckoutVoidRequest.getVoidStatus());
                            if (oneCheckoutVoidRequest.getVoidStatus().equalsIgnoreCase("VOIDED")) {
                                trans.setDokuVoidApprovalCode(oneCheckoutVoidRequest.getApprovalCode());
                                trans.setDokuVoidDatetime(new Date());
                                trans.setDokuVoidResponseCode(oneCheckoutVoidRequest.getResponseCode());
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                                if (oneCheckoutVoidRequest.getType() != null && oneCheckoutVoidRequest.getType().length() > 0) {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                                } else {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE("SUCCESS");
                                }
                            } else {
                                trans.setDokuVoidResponseCode(oneCheckoutVoidRequest.getResponseCode());
                                trans.setDokuVoidDatetime(new Date());
                                if (oneCheckoutVoidRequest.getType() != null && oneCheckoutVoidRequest.getType().length() > 0) {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                                } else {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE("FAILED");
                                }
                            }
                        } else if (merchantPaymentChannel.getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.BSP.value())) {
                            StringBuilder sb = new StringBuilder();
                            oneCheckoutVoidRequest.setApprovalCode(trans.getDokuApprovalCode());
                            //sb.append("TRXCODE=").append(voidRequest.getTRANSIDMERCHANT());
                            sb.append("TRXCODE=").append(trans.getDokuVoidApprovalCode());
                            sb.append("&").append("SERVICEID=").append(merchantPaymentChannel.getPaymentChannel().getServiceId());
                            sb.append("&").append("PAYMENTAPPROVALCODE=").append(trans.getDokuApprovalCode());
                            sb.append("&").append("WORDS=").append(super.generateMPGVoidWords(oneCheckoutVoidRequest, merchantPaymentChannel));
//                            String paymentChannelId = merchantPaymentChannel.getPaymentChannel().getPaymentChannelId();
//                            String ocoId = this.generateOcoId(paymentChannelId);
                            sb.append("&").append("OCOID=").append(trxHelper.getOcoId());
            
                            OneCheckoutLogger.log("VOID PARAM : "+sb.toString());
                            InternetResponse inetResp = super.doFetchHTTP(sb.toString(), merchantPaymentChannel.getPaymentChannel().getVoidUrl(),  m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                            oneCheckoutVoidRequest = MPGVoidReponseXML.parseResponseXML(inetResp.getMsgResponse(), oneCheckoutVoidRequest);
                            OneCheckoutLogger.log("Void Status : %s", oneCheckoutVoidRequest.getVoidStatus());
                            if (oneCheckoutVoidRequest.getVoidStatus().equalsIgnoreCase("SUCCESS")) {
                                trans.setDokuVoidApprovalCode(oneCheckoutVoidRequest.getApprovalCode());
                                trans.setDokuVoidDatetime(new Date());
                                trans.setDokuVoidResponseCode(oneCheckoutVoidRequest.getResponseCode());
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                                if (oneCheckoutVoidRequest.getType() != null && oneCheckoutVoidRequest.getType().length() > 0) {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                                } else {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE("SUCCESS");
                                }
                            } else {
                                trans.setDokuVoidResponseCode(oneCheckoutVoidRequest.getResponseCode());
                                trans.setDokuVoidDatetime(new Date());
                                if (oneCheckoutVoidRequest.getType() != null && oneCheckoutVoidRequest.getType().length() > 0) {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE(inetResp.getMsgResponse() + ";" + oneCheckoutVoidRequest.getMESSAGE());
                                } else {
                                    oneCheckoutVoidRequest.setVOIDRESPONSE("FAILED"  + ";" + oneCheckoutVoidRequest.getMESSAGE());
                                }
                            }
                        }
                        errormsg = OneCheckoutErrorMessage.FAILED_REGISTER_ACCOUNT_BILLING;
                        trans.setDokuResponseCode(errormsg.value());
                        trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        trans.setDokuInvokeStatusDatetime(new Date());
                        em.merge(trans);
                        // lakukan void ke core
                        trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                        trxHelper.getRedirect().getRetryData().remove("MSG");
                        trxHelper.getRedirect().getRetryData().put("MSG", "Registration Failed");
                        return trxHelper;
                    }
                }

                trans = this.notifyStatusMerchantCustom(trans, trxHelper.getPaymentChannel());

                OneCheckoutRedirectData redirectRequest = null;
                trxHelper.setRedirectDoku(redirectRequest);
                trxHelper.setTransactions(trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                em.merge(trans);

                trxHelper.setMessage("VALID");
                return trxHelper;
            }
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

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("ATTEMPTS : " + attempts);
            Transactions trans = (Transactions) obj[1];
            if (redirectRequest == null) {
                trans = trxHelper.getTransactions();
                statusCode = trans.getDokuResponseCode();
            }
            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.name());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            }

            //set data redirect yang akan di kirim ke merchant
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();


            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());


            // 0 = FROM CHANNEL, 1 = RETRY, 2 = REDIRECT TO MERCHANT / KLIK CONTINUE
            if (redirectRequest != null && redirectRequest.getFLAG().equalsIgnoreCase("1")) {
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=1");
                if ((m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) && trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                    OneCheckoutLogger.log("FLAG=1 A");
                    if (attempts >= 3) {
                        OneCheckoutLogger.log("FLAG=1 A1");
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
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
                statusCode = trans.getDokuResponseCode();
                redirectRequest.setSTATUSCODE(statusCode);
                trxHelper.setRedirectDoku(redirectRequest);
                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(redirectRequest.getSTATUSCODE());
                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");
            } else {
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0");
                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(statusCode);
                HashMap resultData = redirect.getRetryData();

                OneCheckoutLogger.log("CUSTOMERNAME1[" + trans.getIncName() + "]");
                OneCheckoutLogger.log("CUSTOMERNAME2[" + trxHelper.getTransactions().getIncName() + "]");
                OneCheckoutLogger.log("BILLINGNUMBER1[" + trans.getTransactionsDataRecur().getIncBillNumber() + "]");
                OneCheckoutLogger.log("BILLINGNUMBER2[" + trxHelper.getTransactions().getTransactionsDataRecur().getIncBillNumber() + "]");

                resultData.put("CUSTOMERNAME", trans.getIncName());
                resultData.put("BILLINGNUMBER", trans.getTransactionsDataRecur().getIncBillNumber());
                resultData.put("BILLINGDETAIL", trans.getTransactionsDataRecur().getIncBillDetail());
                resultData.put("STARTDATE", trans.getTransactionsDataRecur().getIncStartDate());
                resultData.put("ENDDATE", trans.getTransactionsDataRecur().getIncEndDate());
                resultData.put("EXECUTEDATE", trans.getTransactionsDataRecur().getIncExecuteDate());
                resultData.put("EXECUTEMONTH", trans.getTransactionsDataRecur().getIncExecuteMonth());
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
                    resultData.put("MSG", "Register Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");
                    redirect.setPageTemplate("transactionFailed.html");
                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A4");
                    redirect.setPageTemplate("transactionSuccess.html");
                    resultData.put("MSG", "Register Success");
                    resultData.put("MSGDETAIL", "Thank you for doing Recur Transaction with " + m.getMerchantName());
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


            data.put("TRANSIDMERCHANT", invoiceNo);

            data.put("STATUSCODE", statusCode);
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", sessionId);
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
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    protected Transactions notifyStatusMerchantCustom(Transactions trans, OneCheckoutPaymentChannel pChannel) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1RecurBean.notifyStatusMerchantCustom - T0: %d Start", (System.currentTimeMillis() - t1));


            OneCheckoutLogger.log("OneCheckoutV1RecurBean.notifyStatusMerchantCustom - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1RecurBean.notifyStatusMerchantCustom - T1: %d create notify parameters to Merchant", (System.currentTimeMillis() - t1));


            
    
            HashMap<String, String> params = super.getData(trans);
            params.put("TOKENID", trans.getTokenId());
            params.put("CUSTOMERID", trans.getIncCustomerId());
            params.put("PAYMENTCODE", "");
            OneCheckoutLogger.log("OneCheckoutV1RecurBean.notifyStatusMerchantCustom - T2: %d start Notify to Merchant", (System.currentTimeMillis() - t1));               
            
            String resp = super.notifyStatusMerchant(trans, params, pChannel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);            
            OneCheckoutLogger.log("OneCheckoutV1RecurBean.notifyStatusMerchantCustom : statusNotify : %s", resp);
            OneCheckoutLogger.log("OneCheckoutV1RecurBean.notifyStatusMerchantCustom - T3: %d finish Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutLogger.log("OneCheckoutV1RecurBean.notifyStatusMerchantCustom - T4: %d Finish process", (System.currentTimeMillis() - t1));
            return trans;
        } catch (Exception ex) {
            ex.printStackTrace();

            return trans;
        }

    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
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

    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataPGRedirect parsing3DSecureData(XMLConfiguration xml) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getCoreMipURL(PaymentChannel pChannel, String step) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected OneCheckoutDataHelper doRegistrationRecAccountBilling(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {
            Transactions t = trxHelper.getTransactions();
            OneCheckoutLogger.log("===========REG ACCOUNT BILLING============");
            MerchantPaymentChannel mpc = t.getMerchantPaymentChannel();
            Merchants m = mpc.getMerchants();
            StringBuilder sb = new StringBuilder();
            Integer MALLID = mpc.getPaymentChannelCode();
            sb.append("MALLID=").append(MALLID).append("&");
            sb.append("CHAINMALLID=").append(mpc.getPaymentChannelChainCode()).append("&");
            String CUSTOMERID_req = t.getIncCustomerId();
            sb.append("CUSTOMERID=").append(CUSTOMERID_req).append("&");
            String SHAREDKEY = mpc.getMerchantPaymentChannelHash();

            TransactionsDataRecur recur = null;
            if (t.getTransactionsDataRecur() != null) {
                recur = t.getTransactionsDataRecur();
            }

            String BILLNUMBER = recur.getIncBillNumber();
            sb.append("BILLNUMBER=").append(URLEncoder.encode(BILLNUMBER != null ? BILLNUMBER : "", "UTF-8")).append("&");
            String REQUESTDATETIME = OneCheckoutVerifyFormatData.datetimeFormat.format(t.getTransactionsDatetime());
            sb.append("REQUESTDATETIME=").append(URLEncoder.encode(REQUESTDATETIME, "UTF-8")).append("&");
            String WORDS = "";
            String Url = "";
            if (t.getTokenId() == null || t.getTokenId().isEmpty()) {
                if (t.getTransactionsDataCardholders() != null && t.getTransactionsDataCardholders().size() > 0) {
                    TransactionsDataCardholder cardholder = t.getTransactionsDataCardholders().iterator().next();
                    sb.append("CCNAME=").append(URLEncoder.encode(cardholder.getIncCcName() != null ? cardholder.getIncCcName() : "", "UTF-8")).append("&");
                    sb.append("CCEMAIL=").append(URLEncoder.encode(t.getIncEmail() != null ? t.getIncEmail() : "", "UTF-8")).append("&");
                    sb.append("CCADDRESS=").append(URLEncoder.encode(cardholder.getIncAddress() != null ? cardholder.getIncAddress() : "", "UTF-8")).append("&");
                    sb.append("CCCITY=").append(URLEncoder.encode(cardholder.getIncCity() != null ? cardholder.getIncCity() : "", "UTF-8")).append("&");
                    sb.append("CCSTATE=").append(URLEncoder.encode(cardholder.getIncState() != null ? cardholder.getIncState() : "", "UTF-8")).append("&");
                    sb.append("CCCOUNTRY=").append(URLEncoder.encode(cardholder.getIncCountry() != null ? cardholder.getIncCountry() : "", "UTF-8")).append("&");
                    sb.append("CCZIPCODE=").append(URLEncoder.encode(cardholder.getIncZipcode() != null ? cardholder.getIncZipcode() : "", "UTF-8")).append("&");
                    sb.append("CCMOBILEPHONE=").append(URLEncoder.encode(cardholder.getIncMobilephone() != null ? cardholder.getIncMobilephone() : "", "UTF-8")).append("&");
                    sb.append("CCHOMEPHONE=").append(URLEncoder.encode(cardholder.getIncHomephone() != null ? cardholder.getIncHomephone() : "", "UTF-8")).append("&");
                    sb.append("CCWORKPHONE=").append(URLEncoder.encode(cardholder.getIncWorkphone() != null ? cardholder.getIncWorkphone() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERADDRESS=").append(URLEncoder.encode(cardholder.getIncAddress() != null ? cardholder.getIncAddress() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERCITY=").append(URLEncoder.encode(cardholder.getIncCity() != null ? cardholder.getIncCity() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERSTATE=").append(URLEncoder.encode(cardholder.getIncState() != null ? cardholder.getIncState() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERCOUNTRY=").append(URLEncoder.encode(cardholder.getIncCountry() != null ? cardholder.getIncCountry() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERZIPCODE=").append(URLEncoder.encode(cardholder.getIncZipcode() != null ? cardholder.getIncZipcode() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERMOBILEPHONE=").append(URLEncoder.encode(cardholder.getIncMobilephone() != null ? cardholder.getIncMobilephone() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERHOMEPHONE=").append(URLEncoder.encode(cardholder.getIncHomephone() != null ? cardholder.getIncHomephone() : "", "UTF-8")).append("&");
                    sb.append("CUSTOMERWORKPHONE=").append(URLEncoder.encode(cardholder.getIncWorkphone() != null ? cardholder.getIncWorkphone() : "", "UTF-8")).append("&");
                }
                sb.append("CUSTOMERNAME=").append(URLEncoder.encode(t.getIncName() != null ? t.getIncName() : "", "UTF-8")).append("&");
                sb.append("CUSTOMEREMAIL=").append(URLEncoder.encode(t.getIncEmail() != null ? t.getIncEmail() : "", "UTF-8")).append("&");

                String data = KMCardUtils.decrypt(recur.getIncAdditionalData1());
                String[] datas = data.split("\\|");
                String pan = datas[0];
                String exp = datas[1];

                sb.append("CARDNUMBER=").append(URLEncoder.encode(pan != null ? pan : "", "UTF-8")).append("&");
                sb.append("EXPIRYDATE=").append(URLEncoder.encode(exp != null ? exp : "", "UTF-8")).append("&");
                PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
                Url = config.getString("RECUR.URL.RecurRegistration", null);
                WORDS = HashWithSHA1.doHashing(MALLID.toString() + SHAREDKEY + CUSTOMERID_req + BILLNUMBER + REQUESTDATETIME,"SHA1",null);
            } else {
                OneCheckoutLogger.log("TOKEN   : " + t.getTokenId());
                PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
                Url = config.getString("RECUR.URL.RecurRegistrationWithToken", null);

                String TOKENID = t.getTokenId();
                sb.append("TOKENID=").append(URLEncoder.encode(TOKENID, "UTF-8")).append("&");
                WORDS = HashWithSHA1.doHashing(MALLID.toString() + SHAREDKEY + CUSTOMERID_req + TOKENID + BILLNUMBER + REQUESTDATETIME,"SHA1",null);
            }
            OneCheckoutDOKUNotifyData notifyData = new OneCheckoutDOKUNotifyData();
            try {
                sb.append("RECURRING=").append(URLEncoder.encode("R", "UTF-8")).append("&");
                sb.append("AMOUNT=").append(URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(t.getIncAmount().doubleValue()), "UTF-8")).append("&");
                sb.append("PURCHASEAMOUNT=").append(URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(t.getIncPurchaseamount().doubleValue()), "UTF-8")).append("&");
                sb.append("CURRENCY=").append(URLEncoder.encode(t.getIncCurrency(), "UTF-8")).append("&");
                sb.append("PURCHASECURRENCY=").append(URLEncoder.encode(t.getIncPurchasecurrency(), "UTF-8")).append("&");
                sb.append("BILLDETAIL=").append(URLEncoder.encode(recur.getIncBillDetail(), "UTF-8")).append("&");
                sb.append("BILLTYPE=").append(URLEncoder.encode(recur.getIncBillType() + "", "UTF-8")).append("&");
                String STARTDATE = recur.getIncStartDate();
                String ENDDATE = recur.getIncEndDate();
                sb.append("STARTDATE=").append(URLEncoder.encode(STARTDATE, "UTF-8")).append("&");
                sb.append("ENDDATE=").append(URLEncoder.encode(ENDDATE, "UTF-8")).append("&");
                sb.append("EXECUTETYPE=").append(URLEncoder.encode(recur.getIncExecuteType() + "", "UTF-8")).append("&");
                sb.append("EXECUTEDATE=").append(URLEncoder.encode(recur.getIncExecuteDate(), "UTF-8")).append("&");
                sb.append("EXECUTEMONTH=").append(URLEncoder.encode(recur.getIncExecuteMonth(), "UTF-8")).append("&");
                sb.append("WORDS=").append(URLEncoder.encode(WORDS, "UTF-8"));
              //  HashMap<String, String> params = new HashMap<String, String>();
                OneCheckoutLogger.log("DATA TO ACCOUNT BILLING : " + sb.toString());
                InternetResponse inetResp = doFetchHTTP(sb.toString(), Url, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                OneCheckoutLogger.log("RESPONSE CODE   : " + inetResp.getHTTPrespCode());
                OneCheckoutLogger.log("RESPONSE MSG    : \n" + inetResp.getMsgResponse());

                String resultRegRecurAB = inetResp.getMsgResponse();
                recur.setRegistrationResponse(resultRegRecurAB);
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(resultRegRecurAB);
                xml.load(sr);

                /*
                <RESPONSE>
                <RESULTMSG>SUCCESS</RESULTMSG>
                <MALLID>1030</MALLID>
                <REQUESTDATETIME>20140326103951</REQUESTDATETIME>
                <CUSTOMERID>141</CUSTOMERID>
                <CUSTOMERNAME>OCO_Testing</CUSTOMERNAME>
                <CUSTOMEREMAIL>anggar@nsiapay.net</CUSTOMEREMAIL>
                <CUSTOMERADDRESS>Alamat Billing Address</CUSTOMERADDRESS>
                <CUSTOMERCITY>Kota Billing Address</CUSTOMERCITY>
                <CUSTOMERSTATE>Propinsi Billing Address</CUSTOMERSTATE>
                <CUSTOMERCOUNTRY>ID</CUSTOMERCOUNTRY>
                <CUSTOMERZIPCODE>12345</CUSTOMERZIPCODE>
                <CUSTOMERMOBILEPHONE>0987654321</CUSTOMERMOBILEPHONE>
                <CUSTOMERHOMEPHONE>0123456789</CUSTOMERHOMEPHONE>
                <CUSTOMERWORKPHONE>0123456789</CUSTOMERWORKPHONE>
                <TOKENS>
                <TOKEN>
                <TOKENID>3a15cf8be72b1ed36ffb3ec555ba3c97dc43f475</TOKENID>
                <CARDNUMBER>5***********1111</CARDNUMBER>
                <EXPIRYDATE>1505</EXPIRYDATE>
                <CCNAME>Anggar Keren</CCNAME>
                <CCEMAIL>anggar@nsiapay.net</CCEMAIL>
                <CCADDRESS>Alamat Billing Address</CCADDRESS>
                <CCCITY>Kota Billing Address</CCCITY>
                <CCSTATE/>
                <CCCOUNTRY>ID</CCCOUNTRY>
                <CCZIPCODE>12345</CCZIPCODE>
                <CCMOBILEPHONE/>
                <CCHOMEPHONE>0123456789</CCHOMEPHONE>
                <CCWORKPHONE>0123456789</CCWORKPHONE>
                <TOKENSTATUS>ACTIVE</TOKENSTATUS>
                <BILLS>
                <BILL>
                <BILLNUMBER>1237974</BILLNUMBER>
                <AMOUNT>100000.00</AMOUNT>
                <PURCHASEAMOUNT>100000.00</PURCHASEAMOUNT>
                <CURRENCY>360</CURRENCY>
                <PURCHASECURRENCY>360</PURCHASECURRENCY>
                <BILLDETAIL>test</BILLDETAIL>
                <BILLTYPE>S</BILLTYPE>
                <BILLSTATUS>ACTIVE</BILLSTATUS>
                <STARTDATE>20140319</STARTDATE>
                <ENDDATE>20150319</ENDDATE>
                <EXECUTETYPE>DAY</EXECUTETYPE>
                <EXECUTEDATE>WED</EXECUTEDATE>
                <EXECUTEMONTH>MAR</EXECUTEMONTH>
                <FLATSTATUS>true</FLATSTATUS>
                </BILL>
                </BILLS>
                </TOKEN>
                </TOKENS>
                </RESPONSE>
                 */

                String RESULTMSG = xml.getString("RESULTMSG");
                notifyData.setMALLID(MALLID.toString());
                Date REQUESTDATETIMES = OneCheckoutVerifyFormatData.datetimeFormat.parse(REQUESTDATETIME);
                notifyData.setREQUESPAYMENTDATETIME(REQUESTDATETIMES);
                notifyData.setRESULTMSG(RESULTMSG);
                notifyData.setMERCHANTCODE(xml.getString("CUSTOMERID"));
                if (RESULTMSG.equalsIgnoreCase("SUCCESS")) {
                    String AMOUNT = xml.getString("TOKENS.TOKEN.BILLS.BILL.AMOUNT");
                    String CUSTOMERID_res = xml.getString("CUSTOMERID");
                    String STATUSTYPE = "P";
                    String RESPONSECODE = "0000";
                    notifyData.setAMOUNT(AMOUNT);
                    notifyData.setTRANSACTIONID("");
                    notifyData.setSTATUSTYPE(STATUSTYPE);
                    notifyData.setRESPONSECODE(RESPONSECODE);
                    notifyData.setCARDNUMBER(xml.getString("TOKENS.TOKEN.CARDNUMBER"));
                    notifyData.setTOKENID(xml.getString("TOKENS.TOKEN.TOKENID"));
                    notifyData.setCUSTOMERID(CUSTOMERID_res);
                    OneCheckoutLogger.log("TOKEN ACCOUNT BILLING   : " + xml.getString("TOKENS.TOKEN.TOKENID"));

                    /* SCHEDULE RECUR PARAM */
                    HashMap<String, String> RecurSchedule = new HashMap<String, String>();
                    RecurSchedule.put("BILLNUMBER", xml.getString("TOKENS.TOKEN.BILLS.BILL.BILLNUMBER"));
                    RecurSchedule.put("AMOUNT", xml.getString("TOKENS.TOKEN.BILLS.BILL.AMOUNT"));
                    RecurSchedule.put("PURCHASEAMOUNT", xml.getString("TOKENS.TOKEN.BILLS.BILL.PURCHASEAMOUNT"));
                    RecurSchedule.put("CURRENCY", xml.getString("TOKENS.TOKEN.BILLS.BILL.CURRENCY"));
                    RecurSchedule.put("PURCHASECURRENCY", xml.getString("TOKENS.TOKEN.BILLS.BILL.PURCHASECURRENCY"));
                    RecurSchedule.put("BILLDETAIL", xml.getString("TOKENS.TOKEN.BILLS.BILL.BILLDETAIL"));
                    RecurSchedule.put("BILLTYPE", xml.getString("TOKENS.TOKEN.BILLS.BILL.BILLTYPE"));
                    RecurSchedule.put("BILLSTATUS", xml.getString("TOKENS.TOKEN.BILLS.BILL.BILLSTATUS"));
                    RecurSchedule.put("STARTDATE", xml.getString("TOKENS.TOKEN.BILLS.BILL.STARTDATE"));
                    RecurSchedule.put("ENDDATE", xml.getString("TOKENS.TOKEN.BILLS.BILL.ENDDATE"));
                    RecurSchedule.put("EXECUTETYPE", xml.getString("TOKENS.TOKEN.BILLS.BILL.EXECUTETYPE"));
                    RecurSchedule.put("EXECUTEDATE", xml.getString("TOKENS.TOKEN.BILLS.BILL.EXECUTEDATE"));
                    RecurSchedule.put("EXECUTEMONTH", xml.getString("TOKENS.TOKEN.BILLS.BILL.EXECUTEMONTH"));
                    RecurSchedule.put("FLATSTATUS", xml.getString("TOKENS.TOKEN.BILLS.BILL.FLATSTATUS"));
                    /* END SCHEDULE RECUR PARAM */
                    if (notifyData.getTOKENID() != null && !notifyData.getTOKENID().isEmpty()) {
                        t.setTokenId(notifyData.getTOKENID());
                    }

                    trxHelper.setTransactions(t);

                } else {
                    notifyData.setRESULTMSG("FAILED");
                    trxHelper.getEdsUpdateStatus().setACKNOWLEDGE("STOP");
                }
            } catch (Exception ex) {
                notifyData.setRESULTMSG("FAILED");
            }
            trxHelper.setNotifyRequest(notifyData);
        } catch (Throwable t) {
            t.printStackTrace();
            trxHelper.getEdsUpdateStatus().setACKNOWLEDGE("STOP");
            trxHelper.setMessage(t.getMessage());
        }
        return trxHelper;
    }

    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml, Merchants merchants) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
