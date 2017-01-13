/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutCheckStatusData;
import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDOKUVerifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutEDSGetData;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.EDSAdditionalInformation;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.MPGVoidReponseXML;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMerchantCategory;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutReturnType;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Country;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import com.onechekoutv1.dto.TransactionsDataAirlines;
import com.onechekoutv1.dto.TransactionsDataCardholder;
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
public class OneCheckoutV1MultiCurrencyCreditCardBean extends OneCheckoutChannelBase implements OneCheckoutV1MultiCurrencyCreditCardBeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;

    private boolean ableToReversal = true;
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

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
                //    redirect = createRedirectMIP(paymentRequest, pChannel, mpc, m);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
              //      redirect = createRedirectCIP(paymentRequest, pChannel, mpc, m);
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

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doPayment - T2: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();
            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();

            String wordDoku = super.generateVoidWords(voidRequest, m);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - Checking Hash WORDS");

            if (!wordDoku.equalsIgnoreCase(voidRequest.getWORDS())) {
                voidRequest.setVOIDRESPONSE("FAILED");
                trxHelper.setMessage("VALID");
                trxHelper.setVoidRequest(voidRequest);
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - Hash WORDS doesn't match");

                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - Hash WORDS match");


            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);

            
            if (trans != null) {

                MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();

                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());

                if (status == OneCheckoutTransactionStatus.SUCCESS) {

                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - ready to void");

                    StringBuilder sb = new StringBuilder();

                    voidRequest.setApprovalCode(trans.getDokuApprovalCode());
                    //sb.append("TRXCODE=").append(voidRequest.getTRANSIDMERCHANT());
                    sb.append("TRXCODE=").append(trans.getDokuVoidApprovalCode());
                    sb.append("&").append("SERVICEID=").append(mpc.getPaymentChannel().getServiceId());
                    sb.append("&").append("PAYMENTAPPROVALCODE=").append(trans.getDokuApprovalCode());
                    sb.append("&").append("WORDS=").append(super.generateMPGVoidWords(voidRequest, mpc));
                    
//                    sb.append("&").append("USERNAME=").append(m.getMerchantName());
//                    sb.append("&").append("IP").append(trans.getSystemSession());
//                    sb.append("&").append("REASON").append(voidRequest.getVOIDREASON());

                    OneCheckoutLogger.log("VOID PARAM : "+sb.toString());
                    InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                    voidRequest = MPGVoidReponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
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

                        OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - update trx as voided");

                    } else {

                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setDokuVoidDatetime(new Date());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse() + ";" + voidRequest.getMESSAGE());
                        } else {
                            voidRequest.setVOIDRESPONSE("FAILED"  + ";" + voidRequest.getMESSAGE());
                        }

                        OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - update response code void");
                    }

                    em.merge(trans);

                } else if (status == OneCheckoutTransactionStatus.VOIDED) {

                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "VOIDED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - trx already voided");

                } else {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "FAILED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("FAILED" + ";" + voidRequest.getMESSAGE());
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - trx can't be voided");
                }

            } else {

                if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                    voidRequest.setVOIDRESPONSE(generateVoidResponse(null, ""));
                } else {
                    voidRequest.setVOIDRESPONSE("FAILED" + ";" + "Transaction Not Found");
                }

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - trx isn't found");
            }

            trxHelper.setMessage("VALID");
            trxHelper.setVoidRequest(voidRequest);

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doVoid - T3: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();

            String invoiceNo = verifyRequest.getTRANSIDMERCHANT();
            String sessionId = verifyRequest.getSESSIONID();
            double amount = verifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInquiryInvoice - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getVerifyTransactionBy(invoiceNo, sessionId, amount, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInquiryInvoice : Transaction is null");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            if (!trans.getIncCurrency().equalsIgnoreCase(verifyRequest.getCURRENCY())) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInquiryInvoice : CURRENCY does not match !");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            String word = super.generateMPGVerifyRequestWords(verifyRequest, trans);

            if (!word.equalsIgnoreCase(verifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInquiryInvoice : WORDS doesn't match !");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInquiryInvoice - T2: %d update transaction", (System.currentTimeMillis() - t1));

            em.merge(trans);

            verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);

            trxHelper.setVerifyRequest(verifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateMPGNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;


            }
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            //trans.setDokuInvokeStatusDatetime(new Date());
            trans.setDokuInvokeStatusDatetime(notifyRequest.getREQUESPAYMENTDATETIME());
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuHostRefNum(notifyRequest.getHOSTREFNUM());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());

            if(notifyRequest.getDFSStatus() != null || notifyRequest.getDFSId() != null || notifyRequest.getDFSScore() != 0) {
                trans.setVerifyId(notifyRequest.getDFSId());
                trans.setVerifyScore(notifyRequest.getDFSScore());
                trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
    //            trans.setEduStatus(notifyRequest.getDFSStatus().value());
            } else {
                trans.setVerifyId("");
                trans.setVerifyScore(0);
                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
   //             trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
            }



            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getRESULT() != null && notifyRequest.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            trans.setInc3dSecureStatus(notifyRequest.getTHREEDSECURESTATUS());
            trans.setIncLiability(notifyRequest.getLIABILITY());


            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            
            

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
//            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus : statusNotify : %s", resp);


            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));

            // proses parsing ack from merchant, then save it to database

            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

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

                OneCheckoutLogger.log("TRANSACTION STATUS ["+trans.getTransactionsStatus()+"]");

                if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {

                    if (redirectRequest.getSTATUSCODE().equalsIgnoreCase("TRUE")) { //trans.getDokuResponseCode())) {
                        OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-0 1");
                        redirectRequest.setSTATUSCODE(trans.getDokuResponseCode());
                        trxHelper.setRedirectDoku(redirectRequest);
                    } else {
                        OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-0 2");
                        redirectRequest.setSTATUSCODE(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                        trxHelper.setRedirectDoku(redirectRequest);
                    }

                    if (trans.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()) {       
                        redirect.setRetry(Boolean.FALSE);
                    }
                 
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");

                    redirect.setPageTemplate("transactionFailed.html");

                } else if (trans.getTransactionsStatus()==OneCheckoutTransactionStatus.SUCCESS.value() && !redirectRequest.getSTATUSCODE().equalsIgnoreCase("TRUE")) {
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

                    redirectRequest.setSTATUSCODE(trans.getDokuResponseCode());
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A4");
                    redirect.setPageTemplate("transactionSuccess.html");

                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with "+m.getMerchantName());

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

              if(trans.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()){
                redirectRequest.setSTATUSCODE(OneCheckoutErrorMessage.HIGHRISK_OR_REJECT.value());
                redirect.setAMOUNT(redirectRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(redirectRequest.getSTATUSCODE());
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
            String words = super.generateRedirectWords(redirect, m);
            data.put("WORDS", words);            
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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doGetEDSData - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutEDSGetData getData = trxHelper.getEdsGetData();



            String invoiceNo = getData.getTRANSIDMERCHANT();
            double amount = getData.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doGetEDSData - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getEDSDataTransactionBy(invoiceNo, amount, acq, getData.getWORDS());
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doGetEDSData : Transaction is null");

                String ack = this.getEDSDataEmptyACK();

                getData.setACKNOWLEDGE(ack);

                trxHelper.setEdsGetData(getData);
                return trxHelper;

            }

            String ack = this.getEDSDataACK(trans, trans.getMerchantPaymentChannel().getMerchants());
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doGetEDSData : " + ack);

            getData.setACKNOWLEDGE(ack);
            trxHelper.setEdsGetData(getData);

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doGetEDSData - T2: %d Finish process", (System.currentTimeMillis() - t1));

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
        return super.doUpdateEDSStatusBase(trxHelper, acq);

    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
//        OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

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
                
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREMPG(trans, paymentChannel);
 
                    
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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));
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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal - T2: %d update transaction", (System.currentTimeMillis() - t1));

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getTYPE().equalsIgnoreCase("REVERSAL")) {
                status = OneCheckoutTransactionStatus.REVERSED;
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());


            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
//            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal : statusNotify : %s", resp);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal - T4: %d update trx record", (System.currentTimeMillis() - t1));

            // proses parsing ack from merchant, then save it to database

            em.merge(trans);


            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doCCReversal - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {

        try {
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlCip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            HashMap<String, String> data = redirect.getParameters();

            data.put("MALLID", mpc.getPaymentChannelCode() + "");
            if (mpc.getPaymentChannelChainCode() != null && mpc.getPaymentChannelChainCode() > 0) {
                data.put("CHAINMALLID", mpc.getPaymentChannelChainCode() + "");
            }

            data.put("INVOICENUMBER", paymentRequest.getTRANSIDMERCHANT());
            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            data.put("VAT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getVAT()));
            data.put("INSURANCE", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getINSURANCE()));
            data.put("FUELCHARGE", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()));

            OneCheckoutLogger.log("CURENCY "+paymentRequest.getCURRENCY());
            Currency currency = queryHelper.getCurrencyByCode(paymentRequest.getCURRENCY());

            data.put("CURRENCY", currency != null ? currency.getAlpha3Code() : "");
            data.put("SESSIONID", paymentRequest.getSESSIONID());
            data.put("WORDS", super.generateDokuMPGWords(paymentRequest, mpc,currency.getAlpha3Code()));
            data.put("BASKET", paymentRequest.getBASKET());
            data.put("CCNAME", paymentRequest.getCC_NAME());
            data.put("CCEMAIL", paymentRequest.getEMAIL());
            data.put("CCCITY", paymentRequest.getCITY());
            data.put("CCREGION", paymentRequest.getSTATE());
            data.put("CCCOUNTRY", paymentRequest.getCOUNTRY());
            data.put("CCPHONE", paymentRequest.getMOBILEPHONE());
            data.put("CCZIPCODE", paymentRequest.getZIPCODE());
            data.put("SERVICEID", pChannel.getServiceId());  // sementara di disable, belum disiapkan
            if (paymentRequest.getBILLINGDESCRIPTION() != null && !paymentRequest.getBILLINGDESCRIPTION().trim().equals("")) {
                data.put("BILLINGSTATEMENTDESCRIPTION", paymentRequest.getBILLINGDESCRIPTION().trim());
            }
            data.put("BILLINGADDRESS", paymentRequest.getADDRESS());
            data.put("HOMEPHONE", paymentRequest.getHOMEPHONE());
            data.put("CONTACTABLE_NAME", paymentRequest.getNAME());
            data.put("OFFICEPHONE", paymentRequest.getMOBILEPHONE());
            data.put("BIRTHDATE", paymentRequest.getBIRTHDATE());
            data.put("DEVICEID", paymentRequest.getDEVICEID());

//            data.put("IP",paymentRequest.getCUSTIP() != null ?  paymentRequest.getCUSTIP() : "");
            
//            data.put("USERNAME",paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");
//            data.put("USERNAME",mpc.getMerchants().getMerchantName() != null ? mpc.getMerchants().getMerchantName() : "");            
            
//            data.put("REASON",paymentRequest.getPAYREASON() != null ? paymentRequest.getPAYREASON() : "");
            
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
                data.put("decisionManager_travelData_journeyType", paymentRequest.getFLIGHTTYPE().value() + "");
                data.put("decisionManager_travelData_departureDateTime", OneCheckoutVerifyFormatData.cybersource_datetimeFormat.format(paymentRequest.getFLIGHTDATETIME()));
                data.put("flightType", paymentRequest.getFLIGHT().value());
                data.put("ffNumber", paymentRequest.getFFNUMBER());
                if (paymentRequest.getFLIGHTNUMBER() != null && paymentRequest.getFLIGHTNUMBER().length > 0) {
                    data.put("flightNumber", paymentRequest.parseDataAirLine(paymentRequest.getFLIGHTNUMBER()));
                }
                if (paymentRequest.getFLIGHTDATE() != null && paymentRequest.getFLIGHTDATE().length > 0) {
                    data.put("flightDate", paymentRequest.parseDataAirLine(paymentRequest.getFLIGHTDATE()));
                }
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

            HashMap<String, String> data1 = new HashMap(data);

            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            data1.put("CARDNUMBER", base.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
            data1.put("EXPIRYMONTH", paymentRequest.getEXPIRYDATE().substring(0,2));
            data1.put("EXPIRYYEAR", paymentRequest.getEXPIRYDATE().substring(2,4));

            OneCheckoutLogger.log("DATA to IPG => " + data1.toString());

            data.put("CARDNUMBER", paymentRequest.getCARDNUMBER());
            data.put("EXPIRYMONTH", paymentRequest.getEXPIRYDATE().substring(2,4));
            data.put("EXPIRYYEAR", paymentRequest.getEXPIRYDATE().substring(0,2));
            data.put("CVV2", paymentRequest.getCVV2());
//            String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            data.put("OCOID", ocoId);
            redirect.setParameters(data);

            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }


    private String getEDSDataEmptyACK() {


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
                    String[] p = airlines.getIncPassengerName().split("|");
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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doMIPPayment - T0: %d Start", (System.currentTimeMillis() - t1));
            Merchants merchant = trxHelper.getMerchant();
            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, merchant);
            boolean request_good = identrx.isRequestGood();
            String statusCode = identrx.getStatusCode();
            MerchantPaymentChannel mpc = identrx.getMpc();
            OneCheckoutErrorMessage errormsg = identrx.getErrorMsg();
            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
            if (notifyRequest == null) {
                notifyRequest = new OneCheckoutDOKUNotifyData();
            }

            if (request_good) {
                OneCheckoutDataPGRedirect redirect = null;
                trans = transacBean.saveTransactions(trxHelper, mpc);
                trxHelper.setOcoId(trans.getOcoId());
                String data = createPostMIP(paymentRequest, pChannel, mpc, merchant, trans.getOcoId());
                String resultPayment = super.postMIP(data, pChannel.getRedirectPaymentUrlMipXml(), pChannel);
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doMIPPayment - T1: %d Start", (System.currentTimeMillis() - t1));
                String amount = OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT());
                String words = "";
                String responseCode = "";
                String approvalCode = "";
                String issuerBank = "";
                String verifyId = "";
                String verifyStatus = "";
                String verifyScore = "";
                String verifyReason = "";
                String hostRefNum = "";
                String bank = "";
                String maskedCard = "";
                String paymentDateTime = "";
                String trxCode = "";
                String errorCode = "";
                String mid = "";
                String verifyStatusRiskEngine="";
                //String tid = "";
                //trxHelper.setMessage(resultPayment);
                trxHelper.setRedirect(redirect);
                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);
                    String result = xml.getString("result") != null ? xml.getString("result").trim() : "";
                    responseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
                    approvalCode = xml.getString("approvalCode") != null ? xml.getString("approvalCode").trim() : "";
                    issuerBank = xml.getString("issuerBank") != null ? xml.getString("issuerBank").trim() : "";
                    verifyId = xml.getString("fraudScreeningId") != null ? xml.getString("fraudScreeningId").trim() : "";
                    verifyStatus = xml.getString("fraudScreeningStatus") != null ? xml.getString("fraudScreeningStatus").trim() : "NA";
                    verifyScore = xml.getString("fraudScreeningScore") != null ? xml.getString("fraudScreeningScore").trim() : "-1";
                    verifyReason = xml.getString("fraudScreeningReason") != null ? xml.getString("fraudScreeningReason").trim() : "";
                    hostRefNum = xml.getString("hostReferenceNumber") != null ? xml.getString("hostReferenceNumber").trim() : "";
                    bank = xml.getString("bank") != null ? xml.getString("bank").trim() : "";
                    mid = xml.getString("mid") != null ? xml.getString("mid").trim() : "";
                    //tid = xml.getString("tid") != null ? xml.getString("tid").trim() : "";
                    maskedCard = xml.getString("cardNumber") != null ? xml.getString("cardNumber").trim() : "";
                    paymentDateTime = xml.getString("paymentDate") != null ? xml.getString("paymentDate").trim() : "";
                    trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
                    errorCode = xml.getString("errorCode") != null ? xml.getString("errorCode").trim() : "";
                    if (responseCode == null || responseCode.trim().equals("")) {
                        if (errorCode != null && !errorCode.trim().equals("")) {
                            if (errorCode.trim().equalsIgnoreCase("XX07")) {
                                responseCode = "BB";
                            }  else {
                                responseCode = "99";
                            }
                        } else {
                            responseCode = "99";
                        }
                    }
                    responseCode = "00" + responseCode;
                    trans.setDokuApprovalCode(approvalCode);
                    trans.setDokuIssuerBank(issuerBank);
                    trans.setDokuResponseCode(responseCode);
                    trans.setDokuHostRefNum(hostRefNum);
                    trans.setDokuVoidApprovalCode(trxCode);
                    try {
                        if (verifyStatus != null) {
                            notifyRequest.setDFSStatus(verifyStatus);
                            notifyRequest.setDFSScore(verifyScore);
                            notifyRequest.setDFSIId(verifyReason);
                            trans.setVerifyId(notifyRequest.getDFSId());
                            trans.setVerifyScore(notifyRequest.getDFSScore());
                            trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                            if (trans.getVerifyStatus() != null && !trans.getVerifyStatus().equals("")) {
                                if (trans.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()) {
                                    verifyStatusRiskEngine = OneCheckoutDFSStatus.REJECT.name();
                                } else if(trans.getVerifyStatus() == OneCheckoutDFSStatus.MEDIUM_RISK.value()) {
                                    verifyStatusRiskEngine = OneCheckoutDFSStatus.REVIEW.name();
                                } else if (trans.getVerifyStatus() == OneCheckoutDFSStatus.LOW_RISK.value()) {
                                    verifyStatusRiskEngine = OneCheckoutDFSStatus.APPROVE.name();
                                } else {
                                    verifyStatusRiskEngine = OneCheckoutDFSStatus.NA.name();
                                }
                            }
                        //    trans.setEduStatus(notifyRequest.getDFSStatus().value());
                        } else {
                            notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                            notifyRequest.setDFSScore("-1");
                            notifyRequest.setDFSIId("");
                            trans.setVerifyId("");
                            trans.setVerifyScore(-1);
                            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                         //   trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                        }
                    } catch (Throwable t) {
                        trans.setVerifyScore(0);
                    }

                    if (result != null && result.trim().toUpperCase().indexOf("SUCCESS") >= 0) {
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                    } else {
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    }
                    /*
                    if (responseCode != null) {
                        if (responseCode.equalsIgnoreCase(OneCheckoutErrorMessage.SUCCESS.value())) {
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                        } else {
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                        }
                    } else {
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    }
                    */
                } else {
                    trans.setDokuResponseCode("00TO");
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                }
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);

                words = HashWithSHA1.doHashing(amount+paymentRequest.getMALLID() + trxHelper.getMerchant().getMerchantHashPassword() + trans.getIncTransidmerchant() + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + verifyStatus,"SHA2", null);
                String xmlMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<PAYMENT_STATUS>" +
                    "<AMOUNT>" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()) + "</AMOUNT>" +
                    "<TRANSIDMERCHANT>" + trans.getIncTransidmerchant() + "</TRANSIDMERCHANT>" +
                    "<WORDS>" + words + "</WORDS>" +
                    "<RESPONSECODE>" + trans.getDokuResponseCode() + "</RESPONSECODE>" +
                    "<APPROVALCODE>" + approvalCode + "</APPROVALCODE>" +
                    "<RESULTMSG>" + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + "</RESULTMSG>" +
                    "<PAYMENTCHANNEL>" + pChannel.getPaymentChannelId() + "</PAYMENTCHANNEL>" +
                    "<PAYMENTCODE></PAYMENTCODE>" +
                    "<SESSIONID>" + paymentRequest.getSESSIONID() + "</SESSIONID>" +
                    "<BANK>" + issuerBank + "</BANK>" +
                    "<MID>" + mid + "</MID>" +
                    "<MCN>" + maskedCard + "</MCN>" +
                    "<PAYMENTDATETIME>" + paymentDateTime + "</PAYMENTDATETIME>" +
                    "<VERIFYID>" + verifyId + "</VERIFYID>" +
                    "<VERIFYSCORE>" + verifyScore + "</VERIFYSCORE>" +
//                  "<VERIFYSTATUS>" + verifyStatus + "</VERIFYSTATUS>" +
                    "<VERIFYSTATUS>" + verifyStatusRiskEngine + "</VERIFYSTATUS>" +
                    "</PAYMENT_STATUS>";
                trxHelper.setMessage(xmlMessage);

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardBean.doMIPPayment - T2: %d Start", (System.currentTimeMillis() - t1));
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
            // MERCHANT DETAIL
            data += URLEncoder.encode("SERVICEID", "UTF-8") + "=" + URLEncoder.encode(pChannel.getServiceId(), "UTF-8") + "&";
            data += URLEncoder.encode("MALLID", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getPaymentChannelCode(), "UTF-8") + "&";
            if (mpc.getPaymentChannelChainCode() != null && mpc.getPaymentChannelChainCode() != 0) {
                data += URLEncoder.encode("CHAINMALLID", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getPaymentChannelChainCode(), "UTF-8") + "&";
            }

            // ORDER DETAIL
            data += URLEncoder.encode("INVOICENUMBER", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getTRANSIDMERCHANT(), "UTF-8") + "&";
            data += URLEncoder.encode("AMOUNT", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()), "UTF-8") + "&";
            data += URLEncoder.encode("VAT", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getVAT()), "UTF-8") + "&";
            data += URLEncoder.encode("INSURANCE", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getINSURANCE()), "UTF-8") + "&";
            data += URLEncoder.encode("FUELCHARGE", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()), "UTF-8") + "&";
            Currency currency = queryHelper.getCurrencyByCode(paymentRequest.getCURRENCY());
            data += URLEncoder.encode("CURRENCY", "UTF-8") + "=" + URLEncoder.encode(currency != null ? currency.getAlpha3Code() : "", "UTF-8") + "&";
            data += URLEncoder.encode("SESSIONID", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getSESSIONID(), "UTF-8") + "&";
            data += URLEncoder.encode("WORDS", "UTF-8") + "=" + URLEncoder.encode(super.generateDokuMPGWords(paymentRequest, mpc,currency.getAlpha3Code()), "UTF-8") + "&";
            data += URLEncoder.encode("BASKET", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBASKET(), "UTF-8") + "&";
            data += URLEncoder.encode("BILLINGADDRESS", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getADDRESS(), "UTF-8") + "&";
            data += URLEncoder.encode("HOMEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getHOMEPHONE(), "UTF-8") + "&";
            data += URLEncoder.encode("CONTACTABLENAME", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getNAME(), "UTF-8") + "&";
            data += URLEncoder.encode("OFFICEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getMOBILEPHONE(), "UTF-8") + "&";
            data += URLEncoder.encode("BIRTHDATE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBIRTHDATE(), "UTF-8") + "&";
            if (paymentRequest.getBILLINGDESCRIPTION() != null && !paymentRequest.getBILLINGDESCRIPTION().trim().equals("")) {
                data += URLEncoder.encode("BILLINGSTATEMENTDESCRIPTION", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBILLINGDESCRIPTION().trim(), "UTF-8") + "&";
            }
            
            // INSTALLMENT DETAIL
            data += URLEncoder.encode("PLAN", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getPROMOID(), "UTF-8") + "&";
            data += URLEncoder.encode("TENOR", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getTENOR(), "UTF-8") + "&";
            
            // MOTO TRX
            if (pChannel.getPaymentChannelId().equalsIgnoreCase(OneCheckoutPaymentChannel.MOTO.value())) {
                data += URLEncoder.encode("TRANSACTIONTYPE", "UTF-8") + "=" + URLEncoder.encode("M", "UTF-8") + "&";
            }

            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {
                /*
                String kam = "";
                if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.OneWay) {
                    kam = paymentRequest.getROUTE()[0];
                    data += URLEncoder.encode("decisionManager_travelData_completeRoute", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getROUTE()[0], "UTF-8") + "&";
                } else if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.Return) {
                    data += URLEncoder.encode("decisionManager_travelData_completeRoute", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getROUTE()[0] + " n " + paymentRequest.getROUTE()[1], "UTF-8") + "&";
                    kam = paymentRequest.getROUTE()[0] + " n " + paymentRequest.getROUTE()[1];
                }
                */
                String kam = "";
                if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.OneWay) {
                    if (paymentRequest != null && paymentRequest.getROUTE() != null && paymentRequest.getROUTE().length > 0) {
                        kam = paymentRequest.getROUTE()[0];
                         data += URLEncoder.encode("decisionManager_travelData_completeRoute", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getROUTE()[0], "UTF-8") + "&";
                    }
                    data += URLEncoder.encode("decisionManager_travelData_journeyType", "UTF-8") + "=" + URLEncoder.encode("one way", "UTF-8") + "&";
                } else if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.Return) {
                    if (paymentRequest != null && paymentRequest.getROUTE() != null && paymentRequest.getROUTE().length > 0) {
                        String temp = "";
                        if (paymentRequest.getROUTE().length > 1) {
                            temp = " n " + paymentRequest.getROUTE()[1];
                        }
                        kam = paymentRequest.getROUTE()[0] + temp;
                        data += URLEncoder.encode("decisionManager_travelData_completeRoute", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getROUTE()[0], "UTF-8") + "&";
                    }
                    data += URLEncoder.encode("decisionManager_travelData_journeyType", "UTF-8") + "=" + URLEncoder.encode("round trip", "UTF-8") + "&";
                }
                OneCheckoutLogger.log("decisionManager_travelData_completeRoute : %s ", kam);
                data += URLEncoder.encode("decisionManager_travelData_journeyType", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getFLIGHTTYPE().value(), "UTF-8") + "&";
                data += URLEncoder.encode("decisionManager_travelData_departureDateTime", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.cybersource_datetimeFormat.format(paymentRequest.getFLIGHTDATETIME()), "UTF-8") + "&";
                data += URLEncoder.encode("flightType", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getFLIGHT().value(), "UTF-8") + "&";
                data += URLEncoder.encode("ffNumber", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getFFNUMBER(), "UTF-8") + "&";
                if (paymentRequest.getFLIGHTNUMBER() != null && paymentRequest.getFLIGHTNUMBER().length > 0) {
                    data += URLEncoder.encode("flightNumber", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.parseDataAirLine(paymentRequest.getFLIGHTNUMBER()), "UTF-8") + "&";
                }
                if (paymentRequest.getFLIGHTDATE() != null && paymentRequest.getFLIGHTDATE().length > 0) {
                    data += URLEncoder.encode("flightDate", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.parseDataAirLine(paymentRequest.getFLIGHTDATE()), "UTF-8") + "&";
                }
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
                            data += URLEncoder.encode("item_" + i + "_passengerFirstName", "UTF-8") + "=" + URLEncoder.encode("" + firstName, "UTF-8") + "&";
                            data += URLEncoder.encode("item_" + i + "_passengerLastName", "UTF-8") + "=" + URLEncoder.encode("" + lastName, "UTF-8") + "&";
                            data += URLEncoder.encode("item_" + i + "_passengerType", "UTF-8") + "=" + URLEncoder.encode("" + type, "UTF-8") + "&";
                            data += URLEncoder.encode("item_" + i + "_passengerEmail", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEMAIL(), "UTF-8") + "&";
                            data += URLEncoder.encode("item_" + i + "_passengerPhone", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getMOBILEPHONE(), "UTF-8") + "&";
                            data += URLEncoder.encode("item_" + i + "_unitPrice", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdfnodecimal.format(paymentRequest.getAMOUNT()), "UTF-8") + "&";
                        }
                    }
                    data += URLEncoder.encode("item_count", "UTF-8") + "=" + URLEncoder.encode("" + count, "UTF-8") + "&";
                }
                data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("" + EDSAdditionalInformation.getAdditionalInfoForEdu(paymentRequest), "UTF-8") + "&";
            } else {
                data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getADDITIONALINFO(), "UTF-8") + "&";
            }

            // 3D SECURE DETAIL
            OneCheckoutLogger.log("paymentRequest.getECI[" + paymentRequest.getECI() + "]");
            if (paymentRequest.getECI()!=null)  {
                String xid = paymentRequest.getXID() != null ? paymentRequest.getXID().trim() : "";
                String authResponseCode = paymentRequest.getAUTHRESRESPONSECODE() != null ? paymentRequest.getAUTHRESRESPONSECODE().trim() : "";
                String authResStatus = paymentRequest.getAUTHRESSTATUS() != null ? paymentRequest.getAUTHRESSTATUS().trim() : "";
                String cavvAlgorithm = paymentRequest.getCAVVALGORITHM() != null ? paymentRequest.getCAVVALGORITHM().trim() : "";
                String cavv = paymentRequest.getCAVV() != null ? paymentRequest.getCAVV().trim() : "";
                if (authResponseCode.trim().equals("")) {
                    authResponseCode = "0";
                }
                OneCheckoutLogger.log("paymentRequest.getXID[" + xid + "]");
                OneCheckoutLogger.log("paymentRequest.getAUTHRESRESPONSECODE[" + authResponseCode + "]");
                OneCheckoutLogger.log("paymentRequest.getAUTHRESSTATUS[" + authResStatus + "]");
                OneCheckoutLogger.log("paymentRequest.getCAVVALGORITHM[" + cavvAlgorithm + "]");
                OneCheckoutLogger.log("paymentRequest.getCAVV[" + cavv + "]");
                data += URLEncoder.encode("ECI", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getECI(), "UTF-8") + "&";
                data += URLEncoder.encode("XID", "UTF-8") + "=" + URLEncoder.encode(xid, "UTF-8") + "&";
                data += URLEncoder.encode("AUTHRESRESPONSECODE", "UTF-8") + "=" + URLEncoder.encode(authResponseCode, "UTF-8") + "&";
                data += URLEncoder.encode("CAVVALGORITHM", "UTF-8") + "=" + URLEncoder.encode(cavvAlgorithm, "UTF-8") + "&";
                data += URLEncoder.encode("AUTHRESSTATUS", "UTF-8") + "=" + URLEncoder.encode(authResStatus, "UTF-8") + "&";
                data += URLEncoder.encode("CAVV", "UTF-8") + "=" + URLEncoder.encode(cavv, "UTF-8") + "&";
            }

            data += URLEncoder.encode("CCNAME", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCC_NAME(), "UTF-8") + "&";
            data += URLEncoder.encode("CCEMAIL", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEMAIL(), "UTF-8") + "&";
            data += URLEncoder.encode("CCCITY", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCITY(), "UTF-8") + "&";
            data += URLEncoder.encode("CCREGION", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getSTATE(), "UTF-8") + "&";
            String ccCountry = paymentRequest.getCOUNTRY() != null ? paymentRequest.getCOUNTRY().trim() : "";
            if (ccCountry != null && ccCountry.length() > 2) {
                OneCheckoutLogger.log("Checking country code[" + ccCountry + "] to database...");
                Country country = queryHelper.getCountryByNumericCode(ccCountry);
                if (country != null) {
                    ccCountry = country.getId();
                } else {
                    OneCheckoutLogger.log("Checking country code[" + ccCountry + "] NOT FOUND...");
                }
            }
            data += URLEncoder.encode("CCCOUNTRY", "UTF-8") + "=" + URLEncoder.encode("" + ccCountry, "UTF-8") + "&";
            data += URLEncoder.encode("CCPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getMOBILEPHONE(), "UTF-8") + "&";
            data += URLEncoder.encode("CCZIPCODE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getZIPCODE(), "UTF-8") + "&";


            //HashMap<String, String> data1 = new HashMap(data);
            String data1 = data;
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            // CARD DETAIL
            data1 += URLEncoder.encode("CARDNUMBER", "UTF-8") + "=" + URLEncoder.encode("" + base.maskingString(paymentRequest.getCARDNUMBER(), "PAN"), "UTF-8") + "&";
            data1 += URLEncoder.encode("EXPIRYMONTH", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(2,4), "UTF-8") + "&";
            data1 += URLEncoder.encode("EXPIRYYEAR", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(0,2), "UTF-8") + "&";
            data1 += URLEncoder.encode("CVV2", "UTF-8") + "=" + URLEncoder.encode("***", "UTF-8");

           OneCheckoutLogger.log("DATA to IPG => " + data1.toString());

            // CARD DETAIL
            data += URLEncoder.encode("CARDNUMBER", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCARDNUMBER(), "UTF-8") + "&";
            data += URLEncoder.encode("EXPIRYMONTH", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(2,4), "UTF-8") + "&";
            data += URLEncoder.encode("EXPIRYYEAR", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(0,2), "UTF-8") + "&";
            data += URLEncoder.encode("OCOID", "UTF-8") + "=" + URLEncoder.encode("" + ocoId, "UTF-8") + "&";
            data += URLEncoder.encode("CVV2", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCVV2(), "UTF-8");


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

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
