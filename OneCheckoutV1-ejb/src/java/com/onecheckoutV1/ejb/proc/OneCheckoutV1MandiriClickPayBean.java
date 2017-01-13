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
import com.onecheckoutV1.ejb.util.MandiriClickPayResponseXML;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
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

/**
 *
 * @author hafizsjafioedin
 */
@Stateless
public class OneCheckoutV1MandiriClickPayBean extends OneCheckoutChannelBase implements OneCheckoutV1MandiriClickPayBeanLocal {

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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        //MIPDokuSuitePaymentResponse paymentResp = trxHelper.getPaymentResponse();

        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

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

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doPayment - T2: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();
            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();

            String wordDoku = super.generateVoidWords(voidRequest, m);
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - Checking Hash WORDS");

            if (!wordDoku.equalsIgnoreCase(voidRequest.getWORDS())) {
                voidRequest.setVOIDRESPONSE("FAILED");
                trxHelper.setMessage("VALID");
                trxHelper.setVoidRequest(voidRequest);
                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - Hash WORDS doesn't match");

                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - Hash WORDS match");


            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);

            if (trans != null) {

                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());



                if (status == OneCheckoutTransactionStatus.SUCCESS) {

                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - ready to void");

                    StringBuilder sb = new StringBuilder();

                    sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
                    sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNo);
                    sb.append("&").append("HOSTREFNUM=").append(trans.getDokuHostRefNum());

//                    sb.append("&").append("USERNAME").append(m.getMerchantName());
//                    sb.append("&").append("IP=").append(trans.getSystemSession());
//                    sb.append("&").append("REASON=").append(voidRequest.getVOIDREASON());
                    
                    OneCheckoutLogger.log("VOID PARAM : "+sb.toString());
                    InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getVoidUrl(),  m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                    OneCheckoutLogger.log("Merchant Void Response : "+inetResp.getMsgResponse());

                    voidRequest = MandiriClickPayResponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
                    if (voidRequest.getVoidStatus().trim().toUpperCase().equalsIgnoreCase("SUCCESS")) {

                        trans.setDokuVoidApprovalCode(voidRequest.getApprovalCode());
                        trans.setDokuVoidDatetime(new Date());
                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("SUCCESS");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - update trx as voided");

                    } else {

                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setDokuVoidDatetime(new Date());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("FAILED");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - update response code void");
                    }

                    em.merge(trans);

                } else if (status == OneCheckoutTransactionStatus.VOIDED) {

                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "VOIDED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - trx already voided");

                } else {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "FAILED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("FAILED");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - trx can't be voided");
                }

            } else {

                if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                    voidRequest.setVOIDRESPONSE(generateVoidResponse(null, ""));
                } else {
                    voidRequest.setVOIDRESPONSE("NOT FOUND");
                }

                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - trx isn't found");
            }

            trxHelper.setMessage("VALID");
            trxHelper.setVoidRequest(voidRequest);

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doVoid - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;

        } catch (Throwable th) {
            th.printStackTrace();
            voidRequest.setVOIDRESPONSE("PROBLEM");
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();

            String invoiceNo = verifyRequest.getTRANSIDMERCHANT();
            String sessionId = verifyRequest.getSESSIONID();
            double amount = verifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInquiryInvoice - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getVerifyTransactionBy(invoiceNo, sessionId, amount, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInquiryInvoice : Transaction is null");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            /* ternyata belum di implement */
            String word = super.generateDOKUVerifyRequestWords(verifyRequest, trans);

            if (!word.equalsIgnoreCase(verifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInquiryInvoice : WORDS doesn't match !");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;


            }

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInquiryInvoice - T2: %d update transaction", (System.currentTimeMillis() - t1));

            em.merge(trans);

            verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);

            trxHelper.setVerifyRequest(verifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuInvokeStatusDatetime(new Date());
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuHostRefNum(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
            trans.setVerifyId(notifyRequest.getDFSId());
            trans.setVerifyScore(notifyRequest.getDFSScore());
            trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
       //     trans.setEduStatus(notifyRequest.getDFSStatus().value());

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getRESULT() != null && notifyRequest.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

              
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

 //           Merchants m = trans.getMerchantPaymentChannel().getMerchants();
            
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
//            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);     
            
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));
            em.merge(trans);


            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

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
                        redirect.setPageTemplate("transactionFailed.html");

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

//                    redirect = redirectToMerchant(redirect, redirectRequest, trans, m, trxHelper, data);

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

                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");

                redirect.setAMOUNT(redirectRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(trans.getDokuResponseCode());

                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");

              //  redirect = redirectToMerchant(redirect, redirectRequest, trans, m, trxHelper, data);

            }

            data.put("TRANSIDMERCHANT", redirectRequest.getTRANSIDMERCHANT());

            data.put("STATUSCODE", redirectRequest.getSTATUSCODE());
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", redirectRequest.getSESSIONID());
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

            //   redirect.setCookie(trans.getIncCookies());
            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
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

                trans = CheckStatusCOREClickPayMandiri(trans,m);
                
                HashMap<String, String> params =  super.getData(trans); 
                trxHelper.setMessage("VALID");
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
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));
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

        try {

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlMip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            //  redirect.setSTATUSCODE(statusCode);
            HashMap<String, String> data = redirect.getParameters();


            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                data.put("CHAINNUM", "NA");
            } else {
                data.put("CHAINNUM", mpc.getPaymentChannelChainCode() + "");
            }

            data.put("coreid", paymentRequest.getCARDNUMBER());
            data.put("txtCCode1", paymentRequest.getCHALLENGE_CODE_1());
            data.put("txtCCode2", paymentRequest.getCHALLENGE_CODE_2());
            data.put("txtCCode3", paymentRequest.getCHALLENGE_CODE_3());
            data.put("txtToken", paymentRequest.getRESPONSE_TOKEN());
            data.put("TYPE", "SINGLE");
            data.put("BASKET", paymentRequest.getBASKET());
            data.put("MERCHANTID", mpc.getMerchantPaymentChannelUid());
            data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            data.put("CURRENCY", paymentRequest.getCURRENCY());
            data.put("PurchaseCurrency", paymentRequest.getCURRENCY());
            //data.put("MALLID", m.getMerchantCode() + "");
            data.put("MALLID", mpc.getPaymentChannelCode() + "");
            data.put("WORDS", super.generateDokuWords(paymentRequest, mpc));
            data.put("SESSIONID", paymentRequest.getSESSIONID());
            String paymentChannelId = pChannel.getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
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

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
