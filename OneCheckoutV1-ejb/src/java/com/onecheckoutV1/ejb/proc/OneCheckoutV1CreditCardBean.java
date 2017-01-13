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
import com.onecheckoutV1.type.*;
import com.onechekoutv1.dto.*;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
public class OneCheckoutV1CreditCardBean extends OneCheckoutChannelBase implements OneCheckoutV1CreditCardBeanLocal {

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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        //MIPDokuSuitePaymentResponse paymentResp = trxHelper.getPaymentResponse();

        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();

            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
            boolean request_good = identrx.isRequestGood();
            MerchantPaymentChannel mpc = identrx.getMpc();
            boolean needNotify = identrx.isNeedNotify();
            OneCheckoutErrorMessage errormsg = identrx.getErrorMsg();

            if (request_good) {

                OneCheckoutDataPGRedirect redirect = null;

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);

                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);

                }
                redirect = createRedirectMIP(paymentRequest, pChannel, mpc, m, trans.getOcoId());

                trxHelper.setMessage("VALID");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

                OneCheckoutNotifyStatusResponse ack = trxHelper.getNotifyResponse();

            } else {
                
                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);                

            }

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doPayment - T2: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();
            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();

            String wordDoku = super.generateVoidWords(voidRequest, m);
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - Checking Hash WORDS");

            if (!wordDoku.equalsIgnoreCase(voidRequest.getWORDS())) {
                voidRequest.setVOIDRESPONSE("FAILED");
                trxHelper.setMessage("VALID");
                trxHelper.setVoidRequest(voidRequest);
                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - Hash WORDS doesn't match");

                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - Hash WORDS match");


            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);

            if (trans != null) {

                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());

                if (status == OneCheckoutTransactionStatus.SUCCESS) {

                    OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - ready to void");

                    StringBuilder sb = new StringBuilder();

                    sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
                    sb.append("&").append("OrderNumber=").append(invoiceNo);
                    sb.append("&").append("ApprovalCode=").append(trans.getDokuApprovalCode());

                    //Tambahan SWIPER
                    sb.append("&").append("MerchantID=").append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
                    sb.append("&").append("PurchaseAmt=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
                    sb.append("&").append("PurchaseMDR=").append("TEST");

//                    sb.append("&").append("USERNAME=").append(m.getMerchantName());
//                    sb.append("&").append("IP").append(trans.getSystemSession());
//                    sb.append("&").append("REASON").append(voidRequest.getVOIDREASON());
                    
                    OneCheckoutLogger.log("VOID PARAM : " + sb.toString());

                    InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                    voidRequest = DOKUVoidResponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
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

                        OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - update trx as voided");

                    } else {

                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setDokuVoidDatetime(new Date());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("FAILED");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - update response code void");
                    }

                    em.merge(trans);

                } else if (status == OneCheckoutTransactionStatus.VOIDED) {

                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "VOIDED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - trx already voided");

                } else {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "FAILED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("FAILED");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - trx can't be voided");
                }

            } else {

                if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                    voidRequest.setVOIDRESPONSE(generateVoidResponse(null, ""));
                } else {
                    voidRequest.setVOIDRESPONSE("NOT FOUND");
                }

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - trx isn't found");
            }

            trxHelper.setMessage("VALID");
            trxHelper.setVoidRequest(voidRequest);

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doVoid - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;

        } catch (Throwable th) {
            th.printStackTrace();
            voidRequest.setVOIDRESPONSE("FAILED");
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();

            String invoiceNo = verifyRequest.getTRANSIDMERCHANT();
            String sessionId = verifyRequest.getSESSIONID();
            double amount = verifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInquiryInvoice - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getVerifyTransactionBy(invoiceNo, sessionId, amount, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInquiryInvoice : Transaction is null");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUVerifyRequestWords(verifyRequest, trans);

            if (!word.equalsIgnoreCase(verifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInquiryInvoice : WORDS doesn't match !");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInquiryInvoice - T2: %d update transaction", (System.currentTimeMillis() - t1));

            em.merge(trans);

            verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);

            trxHelper.setVerifyRequest(verifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();


            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;


            }
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuInvokeStatusDatetime(new Date());
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuHostRefNum(notifyRequest.getHOSTREFNUM());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
            trans.setVerifyId(notifyRequest.getDFSId());
            trans.setVerifyScore(notifyRequest.getDFSScore());
            trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
 //           trans.setEduStatus(notifyRequest.getDFSStatus().value());


            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getRESULT() != null && notifyRequest.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            trans.setTransactionsStatus(status.value());
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));            
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS); 
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus : statusNotify : %s", resp);            

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));

            // proses parsing ack from merchant, then save it to database

            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }

    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

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

            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
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

                OneCheckoutLogger.log("TRANSACTION STATUS [" + trans.getTransactionsStatus() + "]");

                if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {

                    if (!redirectRequest.getSTATUSCODE().equalsIgnoreCase(trans.getDokuResponseCode())) {
                        OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-0");
                        redirectRequest.setSTATUSCODE(trans.getDokuResponseCode());
                        trxHelper.setRedirectDoku(redirectRequest);
                    }

                    if (trans.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()) {
                        redirect.setRetry(Boolean.FALSE);

                    }
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");

                    redirect.setPageTemplate("transactionFailed.html");

                } else if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() && !redirectRequest.getSTATUSCODE().equalsIgnoreCase("0000")) {
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                    redirectRequest.setSTATUSCODE(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                    trxHelper.setRedirectDoku(redirectRequest);

                    redirect.setRetry(Boolean.FALSE); // tidak boleh diulang, karena di IPG apabila di void, maka akan mengembalikan invoice duplicate


                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A3");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");

                    redirect.setPageTemplate("transactionFailed.html");

                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A4");
                    redirect.setPageTemplate("transactionSuccess.html");

                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with " + m.getMerchantName());

                    resultData.put("APPROVAL", trans.getDokuApprovalCode());
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
                    redirect = super.addDWRegistrationPage(trxHelper, redirect, channel, trans.getMerchantPaymentChannel(), trans);
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
     //       OneCheckoutLogger.log("MERCHANT URL         [" + m.getMerchantRedirectUrl() + "]");
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
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }


    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doGetEDSData - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutEDSGetData getData = trxHelper.getEdsGetData();

            String invoiceNo = getData.getTRANSIDMERCHANT();
            double amount = getData.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doGetEDSData - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            //Transactions trans = queryHelper.getEDSDataTransactionBy(invoiceNo, amount, acq, getData.getWORDS());
            Transactions trans = queryHelper.getEDSDataTransactionBy(invoiceNo, amount, getData.getWORDS());
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doGetEDSData : Transaction is null");

                String ack = this.getEDSDataEmptyACK();

                getData.setACKNOWLEDGE(ack);

                getData.setNULLTRANSACTION(Boolean.TRUE);

                trxHelper.setEdsGetData(getData);
                return trxHelper;

            }

            getData.setNULLTRANSACTION(Boolean.FALSE);

            String ack = this.getEDSDataACK(trans, trans.getMerchantPaymentChannel().getMerchants());
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doGetEDSData : " + ack);

            getData.setACKNOWLEDGE(ack);
            trxHelper.setEdsGetData(getData);

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doGetEDSData - T2: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            String ack = this.getEDSDataEmptyACK();
            trxHelper.getEdsGetData().setACKNOWLEDGE(ack);

            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            trxHelper = super.doUpdateEDSStatusBase(trxHelper, acq);
            return trxHelper;

        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getEdsUpdateStatus().setACKNOWLEDGE("STOP");
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
       // OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();

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
                
                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREIPG(trans, paymentChannel);
 
                    
                HashMap<String, String> params =  super.getData(trans); 
                trxHelper.setMessage("VALID");
//                    params.put("BANK", "BRI");
                params.put("PAYMENTCODE","");
                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();                    
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params,m));//.toMPGString());
                trxHelper.setCheckStatusRequest(statusRequest);
                return trxHelper;

            }

            // Create Empty Notify
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest,  trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());

            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {

        //set data redirect yang akan di kirim ke merchant

        try {
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlMip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            //        redirect.setSTATUSCODE(statusCode);
            HashMap<String, String> data = redirect.getParameters();

            data.put("MerchantID", mpc.getMerchantPaymentChannelUid());

            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                data.put("CHAINNUM", "NA");
            } else {
                data.put("CHAINNUM", mpc.getPaymentChannelChainCode() + "");
            }

            data.put("OrderNumber", paymentRequest.getTRANSIDMERCHANT());
            if (paymentRequest.getBILLINGDESCRIPTION() != null && !paymentRequest.getBILLINGDESCRIPTION().trim().equals("")) {
                data.put("trxDesc", paymentRequest.getBILLINGDESCRIPTION().trim());
            }
            data.put("PurchaseAmt", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            data.put("vat", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getVAT()));
            data.put("insurance", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getINSURANCE()));
            data.put("fuelSurcharge", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()));
            data.put("PurchaseCurrency", paymentRequest.getCURRENCY());
            data.put("MerchantName", m.getMerchantName());
            //data.put("MALLID", m.getMerchantCode() + "");
            data.put("MALLID", mpc.getPaymentChannelCode() + "");
            data.put("WORDS", super.generateDokuWords(paymentRequest, mpc));
            data.put("SESSIONID", paymentRequest.getSESSIONID());
            data.put("NAME", paymentRequest.getCC_NAME());
            data.put("CONTACTABLE_NAME", paymentRequest.getNAME());
            data.put("CITY", paymentRequest.getCITY());
            data.put("STATE", paymentRequest.getSTATE());
           // OneCheckoutLogger.log("COUNTRY VALUE : [%s]", paymentRequest.getCOUNTRY());

            String country = queryHelper.getCountryById(paymentRequest.getCOUNTRY());
            OneCheckoutLogger.log("COUNTRY VALUE : [%s]", country);
            data.put("COUNTRY", country != null ? country : paymentRequest.getCOUNTRY());

            data.put("PHONE", paymentRequest.getMOBILEPHONE());
            data.put("HOMEPHONE", paymentRequest.getHOMEPHONE());
            data.put("OFFICEPHONE", paymentRequest.getWORKPHONE());
//            String ocoId = this.generateOcoId(pChannel.getPaymentChannelId());
            data.put("OCOID", ocoId);
//            data.put("IP",paymentRequest.getCUSTIP() != null ?  paymentRequest.getCUSTIP() : "");
            
//            data.put("USERNAME",paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");            
//            data.put("USERNAME",mpc.getMerchants().getMerchantName() != null ? mpc.getMerchants().getMerchantName() : "");

//            data.put("REASON",paymentRequest.getPAYREASON() != null ? paymentRequest.getPAYREASON() : "");

            //data.put("ADDITIONALINFO",EDSAdditionalInformation.getAdditionalInfoForEdu(paymentRequest));
            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {
                /*
                 * StringBuffer basket = new StringBuffer();
                 * basket.append(paymentRequest.getROUTE());
                 *
                 * if (paymentRequest.getRETURN()==OneCheckoutReturnType.Return)
                 * basket.append(";").append(paymentRequest.getRETURN_ROUTE());
                 *
                 * String amount =
                 * OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT());
                 *
                 * basket.append(",").append(amount).append(",").append("1").append(",").append(amount);
                 */
                data.put("PurchaseDesc", paymentRequest.getBASKET());

                String kam = "";
                if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.OneWay) {
                    if (paymentRequest != null && paymentRequest.getROUTE() != null && paymentRequest.getROUTE().length > 0) {
                        kam = paymentRequest.getROUTE()[0];
                        data.put("decisionManager_travelData_completeRoute", paymentRequest.getROUTE()[0]);
                    }
                    data.put("decisionManager_travelData_journeyType", "one way");
                } else if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.Return) {
                    if (paymentRequest != null && paymentRequest.getROUTE() != null && paymentRequest.getROUTE().length > 0) {
                        String temp = "";
                        if (paymentRequest.getROUTE().length > 1) {
                            temp = " n " + paymentRequest.getROUTE()[1];
                        }
                        kam = paymentRequest.getROUTE()[0] + temp;
                        data.put("decisionManager_travelData_completeRoute", paymentRequest.getROUTE()[0] + temp);
                    }
                    data.put("decisionManager_travelData_journeyType", "round trip");
                }
                OneCheckoutLogger.log("decisionManager_travelData_completeRoute : %s ", kam);

                data.put("decisionManager_travelData_departureDateTime", OneCheckoutVerifyFormatData.cybersource_datetimeFormat.format(paymentRequest.getFLIGHTDATETIME()));



                if (paymentRequest.getPASSENGER_NAME() != null) {

                    String[] passengerName = paymentRequest.getPASSENGER_NAME();
                    String[] passengerType = paymentRequest.getPASSENGER_TYPE();

                    int count = passengerName.length;


                    for (int i = 0; i < count; i++) {
                        if (passengerType[i] != null) {
                            String type = "";
                            if (passengerType[i].equalsIgnoreCase("A")) {
                                type = "ADT";
                            } else if (passengerType[i].equalsIgnoreCase("C")) {
                                type = "CNN";
                            }
                            String name = passengerName[i];
                            String[] names = name.split(" ");
                            String firstName = "";
                            String lastName = "";
                            if (name != null) {
                                if (names.length == 1) {
                                    firstName = names[0];
                                } else if (names.length > 1) {
                                    firstName = names[0];
                                    lastName = names[1];
                                }
                            }
                            data.put("item_" + i + "_passengerFirstName", firstName);
                            data.put("item_" + i + "_passengerLastName", lastName);
                            data.put("item_" + i + "_passengerType", type);
                            data.put("item_" + i + "_passengerEmail", paymentRequest.getEMAIL());
                            data.put("item_" + i + "_passengerPhone", paymentRequest.getMOBILEPHONE());
                            data.put("item_" + i + "_unitPrice", OneCheckoutVerifyFormatData.sdfnodecimal.format(paymentRequest.getAMOUNT()));
                        }
                    }
                    data.put("item_count", count + "");
                }
                data.put("ADDITIONALINFO", EDSAdditionalInformation.getAdditionalInfoForEdu(paymentRequest));
            } else {
                data.put("ADDITIONALINFO", paymentRequest.getADDITIONALINFO());
            }

            data.put("EMAIL", paymentRequest.getEMAIL());
            data.put("ZIP_CODE", paymentRequest.getZIPCODE());
            data.put("ADDRESS", paymentRequest.getADDRESS());
            data.put("ACQCODE", paymentRequest.getINSTALLMENT_ACQUIRER());
            data.put("PLAN", paymentRequest.getPROMOID());
            data.put("TENOR", paymentRequest.getTENOR());
            data.put("TYPE", "IMMEDIATE");

            data.put("PurchaseDesc", paymentRequest.getBASKET());

            HashMap<String, String> data1 = new HashMap(data);

            // data1.putAll(data);

            // data.put("CardNumber", paymentRequest.getCARDNUMBER());
            // data.put("EXPIRYDATE", paymentRequest.getEXPIRYDATE());
            // data.put("CVV2", paymentRequest.getCVV2());

            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            data1.put("CardNumber", base.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
            data1.put("EXPIRYDATE", paymentRequest.getEXPIRYDATE());
            //   data1.put("CVV2", "***");

            OneCheckoutLogger.log("DATA to IPG => " + data1.toString());

            data.put("CardNumber", paymentRequest.getCARDNUMBER());
            data.put("EXPIRYDATE", paymentRequest.getEXPIRYDATE());
            data.put("CVV2", paymentRequest.getCVV2());


            redirect.setParameters(data);

            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }


    public String getEDSDataEmptyACK() {


        StringBuilder sb = new StringBuilder();
        sb.append("<information>");
        sb.append("<personal>");
        sb.append("<full_name></full_name>");
        sb.append("<address></address>");
        sb.append("<zip_code></zip_code>");
        sb.append("<home_phone></home_phone>");
        sb.append("<work_phone></work_phone>");
        sb.append("<mobile_phone></mobile_phone>");
        sb.append("<contactable_phone></contactable_phone>");
        sb.append("<email></email>");
        sb.append("<birth_date></birth_date>");
        sb.append("<billing_address></billing_address>");
        sb.append("<name_on_card></name_on_card>");
        sb.append("</personal>");
        sb.append("</information>");

        return sb.toString();
    }

    private String getEDSDataACK(Transactions trans, Merchants m) {

        try {

            TransactionsDataCardholder ch = queryHelper.getTransactionCHBy(trans);

            StringBuilder sb = new StringBuilder();
            sb.append("<information>");
            sb.append("<personal>");
            sb.append("<full_name>").append((trans.getIncName() != null) ? trans.getIncName() : "").append("</full_name>");
            sb.append("<address>").append((ch.getIncAddress() != null) ? ch.getIncAddress() : "").append("</address>");
            sb.append("<zip_code>").append((ch.getIncZipcode() != null) ? ch.getIncZipcode() : "").append("</zip_code>");
            sb.append("<home_phone>").append((ch.getIncHomephone() != null) ? ch.getIncHomephone() : "").append("</home_phone>");
            sb.append("<work_phone>").append((ch.getIncWorkphone() != null) ? ch.getIncWorkphone() : "").append("</work_phone>");
            sb.append("<mobile_phone>").append((ch.getIncMobilephone() != null) ? ch.getIncMobilephone() : "").append("</mobile_phone>");
            sb.append("<contactable_phone>").append((ch.getIncMobilephone() != null) ? ch.getIncMobilephone() : "").append("</contactable_phone>");
            sb.append("<email>").append((trans.getIncEmail() != null) ? trans.getIncEmail() : "").append("</email>");
            sb.append("<birth_date>").append((ch.getIncBirthdate() != null) ? ch.getIncBirthdate() : "").append("</birth_date>");
            sb.append("<billing_address>").append((ch.getIncAddress() != null) ? ch.getIncAddress() : "").append("</billing_address>");
            sb.append("<name_on_card>").append((ch.getIncCcName() != null) ? ch.getIncCcName() : "").append("</name_on_card>");
            sb.append("</personal>");

            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {

                TransactionsDataAirlines airlines = queryHelper.getTransactionAirlinesBy(trans);

                sb.append("<detail>");
                sb.append("<booking_code>").append(airlines.getIncBookingcode()).append("</booking_code>");
                String route = airlines.getIncRoute();


                //        String flightdatetime = trans.getAirlinesFlightdate() + "" + trans.getAirlinesFlighttime();
                //        Date dflight = OneCheckoutVerifyFormatData.datetimeFormat.parse(flightdatetime);

                SimpleDateFormat flightDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat flightTimeFormat = new SimpleDateFormat("HH:mm:ss");

                sb.append("<route>").append(route).append("</route>");
                sb.append("<flight_date>").append(airlines.getIncFlightdate()).append("</flight_date>");
                sb.append("<flight_time>").append(airlines.getIncFlighttime()).append("</flight_time>");
                sb.append("<flight_number>").append(airlines.getIncFlightnumber()).append("</flight_number>");
                sb.append("<ticket_number>").append("").append("</ticket_number>");

                String pnames = "";
                int plen = 0;
                if (airlines.getIncPassengerName() != null) {
                    pnames = airlines.getIncPassengerName().replace('|', ';');
                    String[] p = airlines.getIncPassengerName().split("\\|");
                    plen = p.length;
                }
                sb.append("<passenger_name>").append(pnames).append("</passenger_name>");
                sb.append("<passenger_count>").append(plen).append("</passenger_count>");
                sb.append("</detail>");

            }

            sb.append("</information>");

            return sb.toString();

        } catch (Throwable th) {
            th.printStackTrace();

            return getEDSDataEmptyACK();
        }


    }

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {

        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doMIPPayment - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();

            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
            boolean request_good = identrx.isRequestGood();
            String statusCode = identrx.getStatusCode();
            MerchantPaymentChannel mpc = identrx.getMpc();
            boolean needNotify = identrx.isNeedNotify();
            OneCheckoutErrorMessage errormsg = identrx.getErrorMsg();

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            if (notifyRequest == null) {
                notifyRequest = new OneCheckoutDOKUNotifyData();
            }

            if (request_good) {

                OneCheckoutDataPGRedirect redirect = null;

                trans = transacBean.saveTransactions(trxHelper, mpc);
                trxHelper.setOcoId(trans.getOcoId());
                String data = createPostMIP(paymentRequest, pChannel, mpc, m, trans.getOcoId());

                String resultPayment = super.postMIP(data, pChannel.getRedirectPaymentUrlMipXml(), pChannel);

                //OneCheckoutLogger.log(":X:X:X:X: PAYMENT REPONSE RECEIVED FROM IPG[" + resultPayment + "]");
                trxHelper.setMessage(resultPayment);
                trxHelper.setRedirect(redirect);

                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {

                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);

                    //OneCheckoutLogger.log(": : : : : PAYMENT REPONSE RECEIVED FROM IPG[" + resultPayment + "]");

                    String responseCode = xml.getString("responsecode");
                    String approvalCode = xml.getString("ApprovalCode");
                    String issuerBankName = xml.getString("IssuerBankName");
                    String verifyStatus = xml.getString("verifyStatus");
                    String verifyScore = xml.getString("verifyScore");
                    String verifyReason = xml.getString("verifyReason");
                    String hostRefNum = xml.getString("HostReferenceNumber");

                    if (responseCode != null && responseCode.length() != 4) {
                        responseCode = "00" + responseCode;
                    }

                    trans.setDokuApprovalCode(approvalCode);
                    trans.setDokuIssuerBank(issuerBankName);
                    trans.setDokuResponseCode(responseCode);
                    trans.setDokuHostRefNum(hostRefNum);

                    try {

                        if (verifyStatus != null) {

                            notifyRequest.setDFSStatus(verifyStatus);
                            notifyRequest.setDFSScore(verifyScore);
                            notifyRequest.setDFSIId(verifyReason);

                            trans.setVerifyId(notifyRequest.getDFSId());
                            trans.setVerifyScore(notifyRequest.getDFSScore());
                            trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                        //    trans.setEduStatus(notifyRequest.getDFSStatus().value());

                        } else {

                            notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                            notifyRequest.setDFSScore("-1");
                            notifyRequest.setDFSIId("");

                            trans.setVerifyId("");
                            trans.setVerifyScore(-1);
                            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                       //     trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                        }

                    } catch (Throwable t) {
                        trans.setVerifyScore(0);
                    }

                    String status = xml.getString("status");
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    if (status!=null && status.equalsIgnoreCase("Success")) {
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                    }
                } else {
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                }

                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());

                /* ################## FOR MY SHORCART ################## */
                if (m.getMerchantNotifyStatusUrl() != null && m.getMerchantNotifyStatusUrl().length() > 0) {

                    
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doMIPPayment - T1: %d start Notify to Merchant", (System.currentTimeMillis() - t1));            
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
        //            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
                    HashMap<String, String> params = super.getData(trans);

                    params.put("PAYMENTCODE", "");

                    String resp = super.notifyStatusMerchant(trans, params, channel, false, OneCheckoutStepNotify.INVOKE_STATUS);
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doMIPPayment : statusNotify : %s", resp);
                }


                em.merge(trans);
            } else {

                if (trans == null) {
                    List<Transactions> list = queryHelper.getTransactionsBy(paymentRequest.getTRANSIDMERCHANT(), mpc);

                    if (list != null && list.size() > 0) {
                        trans = list.get(0);
                        list = null;
                    }
                }

                if (errormsg == null && trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() && trans.getTransactionsState() == OneCheckoutTransactionState.DONE.value()) {
                    trxHelper.setMessage("Duplicate Invoice Number");
                } else {
                    trxHelper.setMessage(errormsg.value());
                }

            }

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doMIPPayment - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public String createPostMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {

        String data = "";
        try {

            data += URLEncoder.encode("TYPE", "UTF-8") + "=" + URLEncoder.encode("IMMEDIATE", "UTF-8") + "&";

            data += URLEncoder.encode("MerchantID", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getMerchantPaymentChannelUid(), "UTF-8") + "&";


            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                data += URLEncoder.encode("CHAINNUM", "UTF-8") + "=" + URLEncoder.encode("NA", "UTF-8") + "&";
            } else {
                data += URLEncoder.encode("CHAINNUM", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getPaymentChannelChainCode(), "UTF-8") + "&";
            }

            //OneCheckoutLogger.log(": : : : : CHAINNUM "+data);
            if (paymentRequest.getBILLINGDESCRIPTION() != null && !paymentRequest.getBILLINGDESCRIPTION().trim().equals("")) {
                data += URLEncoder.encode("trxDesc", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBILLINGDESCRIPTION().trim(), "UTF-8") + "&";
            }
            data += URLEncoder.encode("OrderNumber", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getTRANSIDMERCHANT(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : OrderNumber "+data);
            data += URLEncoder.encode("PurchaseAmt", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PurchaseAmt "+data);
            data += URLEncoder.encode("vat", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getVAT()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : vat "+data);
            data += URLEncoder.encode("insurance", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getINSURANCE()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : insurance "+data);
            data += URLEncoder.encode("fuelSurcharge", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : fuelSurcharge "+data);
            data += URLEncoder.encode("PurchaseCurrency", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCURRENCY(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PurchaseCurrency "+data);
            data += URLEncoder.encode("MerchantName", "UTF-8") + "=" + URLEncoder.encode("" + m.getMerchantName(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : MerchantName "+data);
            //data += URLEncoder.encode("MALLID", "UTF-8") + "=" + URLEncoder.encode("" + m.getMerchantCode(), "UTF-8") + "&";
            data += URLEncoder.encode("MALLID", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getPaymentChannelCode(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : MALLID "+data);
            data += URLEncoder.encode("WORDS", "UTF-8") + "=" + URLEncoder.encode("" + super.generateDokuWords(paymentRequest, mpc), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : WORDS "+data);
            data += URLEncoder.encode("SESSIONID", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getSESSIONID(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : SESSIONID "+data);
            data += URLEncoder.encode("NAME", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCC_NAME(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : NAME "+data);
            data += URLEncoder.encode("CONTACTABLE_NAME", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getNAME(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : CONTACTABLE_NAME "+data);
            data += URLEncoder.encode("CITY", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCITY(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : CITY "+data);
            data += URLEncoder.encode("STATE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getSTATE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : STATE "+data);
            data += URLEncoder.encode("COUNTRY", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCOUNTRY(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : COUNTRY "+data);
            data += URLEncoder.encode("PHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getMOBILEPHONE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PHONE "+data);
            data += URLEncoder.encode("HOMEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getHOMEPHONE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : HOMEPHONE "+data);
            data += URLEncoder.encode("OFFICEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getWORKPHONE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : OFFICEPHONE "+data);

            if (m.getMerchantCategory() != null) {
                if (m.getMerchantCategory() != OneCheckoutMerchantCategory.NONAIRLINE.value()) {
                    data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("" + EDSAdditionalInformation.getAdditionalInfoForEdu(paymentRequest), "UTF-8") + "&";
                } else {
                    data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getADDITIONALINFO(), "UTF-8") + "&";
                }
            } else {
                data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
            }

            //OneCheckoutLogger.log(": : : : : EMAIL "+paymentRequest.getEMAIL());
            data += URLEncoder.encode("EMAIL", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEMAIL(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : EMAIL "+data);
            data += URLEncoder.encode("ZIP_CODE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getZIPCODE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : ZIP_CODE "+data);
            data += URLEncoder.encode("ADDRESS", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getADDRESS(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : ADDRESS "+data);
            data += URLEncoder.encode("ACQCODE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getINSTALLMENT_ACQUIRER(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : ACQCODE "+data);
            data += URLEncoder.encode("PLAN", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getPROMOID(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PLAN "+data);
            data += URLEncoder.encode("TENOR", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getTENOR(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : TENOR "+data);

            data += URLEncoder.encode("PurchaseDesc", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getBASKET(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PurchaseDesc "+data);

            if (!paymentRequest.getECI().isEmpty())  {
  
                data += URLEncoder.encode("Eci", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getECI(), "UTF-8") + "&";
                
                data += URLEncoder.encode("XID", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getXID(), "UTF-8") + "&";
                data += URLEncoder.encode("AuthResResponseCode", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getAUTHRESRESPONSECODE(), "UTF-8") + "&";
           //     data += URLEncoder.encode("VendorCode", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getAUTHRESVENDORCODE(), "UTF-8") + "&";
                data += URLEncoder.encode("CavvAlgorithm", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getCAVVALGORITHM(), "UTF-8") + "&";
                data += URLEncoder.encode("AuthResStatus", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getAUTHRESSTATUS(), "UTF-8") + "&";
                data += URLEncoder.encode("CAVV", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getCAVV(), "UTF-8") + "&";
                                                        
            }

            OneCheckoutLogger.log(": : : : : DATA POST TO IPG[" + data + "]");
            //OneCheckoutLogger.log(": : : : : DATA POST TO IPG[" + data + "]");

            data += URLEncoder.encode("EXPIRYDATE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : EXPIRYDATE "+data);

            data += URLEncoder.encode("CardNumber", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCARDNUMBER(), "UTF-8") + "&";
            
            data += URLEncoder.encode("OCOID", "UTF-8") + "=" + URLEncoder.encode("" + ocoId, "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : CardNumber "+data);

            data += URLEncoder.encode("CVV2", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCVV2(), "UTF-8");
            //OneCheckoutLogger.log(": : : : : EXPIRYDATE "+data);

            return data;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public String createVoidPostMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m) {
        String data = "";

        try {
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }

        return data;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal - T2: %d update transaction", (System.currentTimeMillis() - t1));

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getTYPE().equalsIgnoreCase("REVERSAL")) {
                status = OneCheckoutTransactionStatus.REVERSED;
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);

            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal - T4: %d update trx record", (System.currentTimeMillis() - t1));

            // proses parsing ack from merchant, then save it to database

            em.merge(trans);


            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doReversal - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doCybersourceReview - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutCyberSourceData cybs = trxHelper.getCyberSourceData();


            String invoiceNo = cybs.getInvoiceNumber();
            String requestID = cybs.getRequestID();

            Transactions trans = queryHelper.getCyberSourceTransactionsBy(requestID, invoiceNo);

            if (trans != null) {

                Merchants m = trans.getMerchantPaymentChannel().getMerchants();

                HashMap<String, String> params = new HashMap<String, String>();
                params.put("Cookie", "JSESSIONID=" + trans.getIncSessionid());// + ";" + "current_PoS=" + trans.getIncCookies());
                //params.put("Cookie1", "current_PoS=" + trans.getIncCookies());

                if (cybs.getNewDecision() != OneCheckoutCyberSourceVerifyStatus.ACCEPT) {
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                }

                trans.setVerifyDatetime(new Date());
                trans.setVerifyStatus(cybs.getNewDecision().value());
                trans.setEduReason(cybs.getCYBSXML());
                trans.setEduStatus(cybs.getNewDecision().value());

                em.merge(trans);

                //  String data = "content=" + URLEncoder.encode(cybs.getCYBSXML(), "UTF-8");
                String ack = super.CyberSourceNotifyMerchant(cybs.getCYBSXML(), m.getMerchantCybersourceReviewUrl(), m);

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doCybersourceReview RESPONSE ACK = " + ack);

            }


            trxHelper.setMessage("OK");



            OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doCybersourceReview - T2: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }


    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
