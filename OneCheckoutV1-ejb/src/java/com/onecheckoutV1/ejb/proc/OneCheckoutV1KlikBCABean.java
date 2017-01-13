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
import com.onechekoutv1.dto.*;
import java.io.StringReader;
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
public class OneCheckoutV1KlikBCABean extends OneCheckoutChannelBase implements OneCheckoutV1KlikBCABeanLocal {

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

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

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

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }

                //set data redirect yang akan di kirim ke merchant                
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

                redirect.setPageTemplate("klikbca_input.html");
                redirect.setUrlAction(config.getString("URL.THANKYOU"));
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(statusCode);
                HashMap<String, String> data = redirect.getParameters();

                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                data.put("WORDS", super.generateMerchantRedirectWordsBeforePayment(paymentRequest, m));
                
                data.put("STATUSCODE", OneCheckoutErrorMessage.NOT_YET_PAID.value());
                data.put("PAYMENTCHANNEL", OneCheckoutPaymentChannel.KlikBCA.value());
                data.put("SESSIONID", paymentRequest.getSESSIONID());
                data.put("PAYMENTCODE", paymentRequest.getPAYCODE());

                data.put("MERCHANTID", mpc.getMerchantPaymentChannelUid());
                data.put("MALLID", m.getMerchantCode() + "");
                //data.put("MALLID", mpc.getPaymentChannelCode() + "");
//                String paymentChannelId = pChannel.getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                data.put("OCOID", trans.getOcoId());
                trxHelper.setMessage("VALID");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

                OneCheckoutNotifyStatusResponse ack = trxHelper.getNotifyResponse();

            } else {

                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans); 

            }

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doPayment - T2: %d Finish", (System.currentTimeMillis() - t1));

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
             OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doVoid : Void is not permitted");

            return trxHelper;
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();


            String userId = verifyRequest.getKlikBCA_USERID();
            String merchantCode = verifyRequest.getKLIKBCA_MERCHANTCODE();

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInquiryInvoice - update expired transaction");
            MerchantPaymentChannel mpc = queryHelper.getMerchantPaymentChannel(merchantCode, acq);

            queryHelper.updateKlikBCAExpiredTransactions(userId, merchantCode, acq, mpc.getInvExpiredInMinutes());

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInquiryInvoice - querying transaction");

            List<Transactions> transList = queryHelper.getKlikBCATransactionBy(userId, merchantCode, acq);

            if (transList == null) {

                OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInquiryInvoice : Transaction is null");
                String ack = emptyInquiryData(verifyRequest.getKlikBCA_USERID());
                verifyRequest.setACKNOWLEDGE(ack);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInquiryInvoice - T2: update transaction");
            String ack = createInquiryResponseXML(verifyRequest, transList);

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInquiryInvoice - Notify XML : " + ack);

            verifyRequest.setACKNOWLEDGE(ack);

            trxHelper.setVerifyRequest(verifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            String ack = emptyInquiryData(trxHelper.getVerifyRequest().getKlikBCA_USERID());
            trxHelper.getVerifyRequest().setACKNOWLEDGE(ack);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));


            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String userId = notifyRequest.getKLIKBCA_USERID();
            double amount = notifyRequest.getAMOUNT();
            Date payDate = notifyRequest.getREQUESPAYMENTDATETIME();

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getKlikBCATransactionBy(invoiceNo, userId, amount, payDate, acq);

            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus : Transaction is null");
                String ack = emptyNotifyData(notifyRequest.getKLIKBCA_USERID(), notifyRequest.getTRANSIDMERCHANT(), "Failed");
                trxHelper.getNotifyRequest().setACKNOWLEDGE(ack);


                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));

            //Request Garuda, cuman dipake untuk semuanya
            notifyRequest.setAPPROVALCODE(OneCheckoutVerifyFormatData.tenGenerateApprovalCode.format(trans.getTransactionsId()));

            trans.setDokuInvokeStatusDatetime(new Date());
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
            trans.setVerifyId(notifyRequest.getDFSId());
            trans.setVerifyScore(notifyRequest.getDFSScore());
            trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
        //    trans.setEduStatus(notifyRequest.getDFSStatus().value());


            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getRESULT() != null && notifyRequest.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus - %s", trans.getDokuResultMessage());


            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
//            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);              
            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));


            // proses parsing ack from merchant, then save it to database

            em.merge(trans);
            String errMsg = null;
            String bca = this.createNotifyResponseXML(notifyRequest, resp, errMsg);

            notifyRequest.setACKNOWLEDGE(bca);

            trxHelper.setNotifyRequest(notifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            String ack = emptyNotifyData(notifyRequest.getKLIKBCA_USERID(), notifyRequest.getTRANSIDMERCHANT(), "Failed");
            trxHelper.getNotifyRequest().setACKNOWLEDGE(ack);
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
            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();

            String invoiceNo = redirectRequest.getTRANSIDMERCHANT();
            String sessionId = redirectRequest.getSESSIONID();
            double amount = redirectRequest.getAMOUNT();

            Transactions trans = queryHelper.getKlikBCARedirectTransactionBy(invoiceNo, sessionId, amount);

            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {

                //trans.setDokuResponseCode(redirectRequest.getSTATUSCODE() != null ? redirectRequest.getSTATUSCODE() : OneCheckoutErrorMessage.NOT_YET_PAID.value());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.NOT_YET_PAID.name());
                //trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            } else {
                trans.setRedirectDatetime(new Date());
                trans.setDokuResponseCode(trans.getDokuResponseCode() != null ? trans.getDokuResponseCode() : redirectRequest.getSTATUSCODE());
            }

            //set data redirect yang akan di kirim ke merchant                
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

//            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setPageTemplate("redirect.html");
            redirect.setUrlAction(m.getMerchantRedirectUrl());
            redirect.setAMOUNT(redirectRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
            redirect.setSTATUSCODE(trans.getDokuResponseCode());
            Map data = redirect.getParameters();

            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());


            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(redirectRequest.getAMOUNT()));
            data.put("TRANSIDMERCHANT", redirectRequest.getTRANSIDMERCHANT());


            data.put("STATUSCODE", trans.getDokuResponseCode());
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", redirectRequest.getSESSIONID());
            data.put("PAYMENTCODE", "");
            data.put("PURCHASECURRENCY", trans.getIncPurchasecurrency());//getPURCHASECURRENCY());            
            if (trans.getRates()!=null) {
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



            //    redirect.setCookie(trans.getIncCookies());
            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);   

            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1KlikBCABean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            String invoiceNo = statusRequest.getTRANSIDMERCHANT();
            String sessionId = statusRequest.getSESSIONID();



            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.INCOMING);
            if (trans == null) {
                trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);
            }
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
                sb.append("&").append("ACQUIRERID=").append("400");
                sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNo);
                sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
                OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
                InternetResponse inetResp = super.doFetchHTTP(sb.toString(), trans.getMerchantPaymentChannel().getPaymentChannel().getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                    xml.load(sr);
                    String paymentStatus = xml.getString("STATUS") != null ? xml.getString("STATUS").trim() : "";
                    if (paymentStatus != null && !paymentStatus.trim().equalsIgnoreCase("") && !paymentStatus.trim().equalsIgnoreCase("INVALID_REQUEST")) {
                        if (!paymentStatus.trim().equalsIgnoreCase("TRX_NOT_FOUND")) {
                            String paymentResponseCode = xml.getString("RESPONSECODE") != null ? xml.getString("RESPONSECODE").trim() : "";
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
                       //         trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
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
                            Date tpayment = new Date();                            
                            try {
                               tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                            } catch (Exception ex) {

                            }                                 
                            
                            trans.setDokuInvokeStatusDatetime(tpayment);                            

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

                // create NotificationRequest;
                HashMap<String, String> params = super.getData(trans);

                //        params.put("BANK", pChannelProc.name());
                params.put("PAYMENTCODE", "");
                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();


                trxHelper.setMessage("VALID");
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params, m));
                trxHelper.setCheckStatusRequest(statusRequest);

                return trxHelper;

            }


            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);


            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());

            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");


            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));

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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private String createInquiryResponseXML(OneCheckoutDOKUVerifyData verifyRequest, List<Transactions> transList) {

        try {

            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\"?>");
            sb.append("<saleInfoResponse>");
            sb.append("<userId>").append(verifyRequest.getKlikBCA_USERID()).append("</userId>");

            if (transList != null) {
                sb.append("<totalSale>").append(transList.size()).append("</totalSale>");
                sb.append("<transactionList>");
                for (Transactions trans : transList) {
                    sb.append("<transactionInfo>");
                    sb.append("<transactionNo>").append(trans.getIncTransidmerchant()).append("</transactionNo>");
                    sb.append("<transactionDate>").append(OneCheckoutVerifyFormatData.klikbca_datetimeFormat.format(trans.getIncRequestdatetime())).append("</transactionDate>");
                    sb.append("<amount>IDR").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue())).append("</amount>");

                    OneCheckoutMerchantCategory cat = OneCheckoutMerchantCategory.findType(trans.getMerchantPaymentChannel().getMerchants().getMerchantCategory());
                    if (cat == OneCheckoutMerchantCategory.AIRLINE) {

                        TransactionsDataAirlines airlines = queryHelper.getTransactionAirlinesBy(trans);

                        OneCheckoutReturnType rtype = OneCheckoutReturnType.findType(airlines.getIncFlighttype());


                        String basket = "Booking Code - " + airlines.getIncBookingcode();
                        sb.append("<description>").append(basket).append("</description>");
                    } else if (cat == OneCheckoutMerchantCategory.NONAIRLINE) {

                        TransactionsDataNonAirlines nonAirlines = queryHelper.getTransactionNonAirlinesBy(trans);
                        sb.append("<description>").append(nonAirlines.getIncBasket()).append("</description>");
                    }

                    sb.append("<additionalInfo>").append("").append("</additionalInfo>");
                    sb.append("</transactionInfo>");
                    trans.setDokuInquiryInvoiceDatetime(new Date());
                    em.merge(trans);
                }
            } else {
                sb.append("<totalSale>").append("0").append("</totalSale>");
                sb.append("<transactionList>");
            }

            sb.append("</transactionList>");
            sb.append("</saleInfoResponse>");


            return sb.toString();
        } catch (Exception e) {


            e.printStackTrace();
            String ack = emptyInquiryData(verifyRequest.getKlikBCA_USERID());
            return ack;

        }

    }

    private String emptyInquiryData(String userId) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<saleInfoResponse>");
        sb.append("<userId>").append(userId).append("</userId>");
        sb.append("<totalSale>").append("0").append("</totalSale>");
        sb.append("<transactionList>");
        sb.append("</transactionList>");
        sb.append("</saleInfoResponse>");

        return sb.toString();

    }

    private String createNotifyResponseXML(OneCheckoutDOKUNotifyData notifyRequest, String status, String errMsg) {


        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>");
            sb.append("<paymentresultResponse>");
            sb.append("<userId>").append(notifyRequest.getKLIKBCA_USERID() == null ? "" : notifyRequest.getKLIKBCA_USERID()).append("</userId>");
            sb.append("<transactionInfo>");
            sb.append("<transactionNo>").append(notifyRequest.getTRANSIDMERCHANT() == null ? "" : notifyRequest.getTRANSIDMERCHANT()).append("</transactionNo>");
            sb.append("<transactionDate>").append(OneCheckoutVerifyFormatData.klikbca_datetimeFormat.format(notifyRequest.getREQUESPAYMENTDATETIME())).append("</transactionDate>");
            sb.append("<status>").append(status.equalsIgnoreCase(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE) ? "00" : "01").append("</status>");
            sb.append("<reason>").append(errMsg == null ? "OK" : errMsg).append("</reason>");
            sb.append("</transactionInfo>");
            sb.append("</paymentresultResponse>");

            return sb.toString();
        } catch (Exception e) {


            e.printStackTrace();
            String ack = emptyNotifyData(notifyRequest.getKLIKBCA_USERID(), notifyRequest.getTRANSIDMERCHANT(), "Failed");
            return ack;

        }
    }

    private String emptyNotifyData(String userId, String invoiceNo, String errMsg) {

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>");
        sb.append("<paymentresultResponse>");
        sb.append("<userId>").append(userId).append("</userId>");
        sb.append("<transactionInfo>");
        sb.append("<transactionNo>").append(invoiceNo == null ? "denied" : invoiceNo).append("</transactionNo>");
        sb.append("<transactionDate>").append(OneCheckoutVerifyFormatData.klikbca_datetimeFormat.format(new Date())).append("</transactionDate>");
        sb.append("<status>").append("01").append("</status>");
        sb.append("<reason>").append(errMsg == null ? "denied" : errMsg).append("</reason>");
        sb.append("</transactionInfo>");
        sb.append("</paymentresultResponse>");

        return sb.toString();

    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutKlikBCABean.doRetryCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();

            String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
            String userId = paymentRequest.getUSERIDKLIKBCA();
            double amount = paymentRequest.getAMOUNT();
            Date payDate = paymentRequest.getREQUESTDATETIME();

            //    OneCheckoutLogger.log("InvoiceNo : (%s) , UserId : (%s) , Amount : (%s), payDate : (%s)",invoiceNo,userId,OneCheckoutVerifyFormatData.sdf.format(amount),OneCheckoutVerifyFormatData.datetimeFormat.format(payDate));

            OneCheckoutLogger.log("OneCheckoutKlikBCABean.doRetryCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            //getKlikBCATransactionBy(String invoiceNo, String userId, double amount, Date requestPayment)
            Transactions trans = queryHelper.getKlikBCATransactionBy(invoiceNo, userId, amount, payDate, pChannel);

            if (trans != null) {

                MerchantPaymentChannel mpc = queryHelper.getMerchantPaymentChannel(m, pChannel);

                //set data redirect yang akan di kirim ke merchant
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

                redirect.setPageTemplate("klikbca_input.html");
                redirect.setUrlAction(config.getString("URL.THANKYOU"));
                //redirect.setUrlAction(pChannel.getRedirectPaymentUrlMip());
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(trans.getDokuResponseCode());
                HashMap<String, String> data = redirect.getParameters();

                data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                data.put("USERID", paymentRequest.getUSERIDKLIKBCA());
                data.put("MERCHANTID", mpc.getMerchantPaymentChannelUid());
                //data.put("MALLID", m.getMerchantCode() + "");
                data.put("MALLID", mpc.getPaymentChannelCode() + "");
                data.put("SESSIONID", paymentRequest.getSESSIONID());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));

                trxHelper.setMessage("RETRY");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            } else {

                trans = queryHelper.getRedirectTransactionBy(invoiceNo, paymentRequest.getSESSIONID(), amount);

                //set data redirect yang akan di kirim ke merchant
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

                redirect.setPageTemplate(redirect.getProgressPage());
                //redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setUrlAction(config.getString("URL.GOBACK"));
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(trans.getDokuResponseCode());
                Map data = redirect.getParameters();

                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                data.put("WORDS", super.generateRedirectWords(redirect, m));

                data.put("STATUSCODE", trans.getDokuResponseCode());
                data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
                data.put("SESSIONID", paymentRequest.getSESSIONID());
                                
                redirect.setCookie(paymentRequest.getCookie());
                trxHelper.setMessage("BACK_TO_MERCHANT");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            }

            OneCheckoutLogger.log("OneCheckoutKlikBCABean.doRetryCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}