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
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.*;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMerchantCategory;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutReturnType;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import com.onechekoutv1.dto.TransactionsDataAirlines;
import com.onechekoutv1.dto.TransactionsDataNonAirlines;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author hafiz
 */
@Stateless
public class OneCheckoutV1SinarMasVAFullBean extends OneCheckoutChannelBase implements OneCheckoutV1SinarMasVAFullBeanLocal {

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

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));
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
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }

                //set data redirect yang akan di kirim ke merchant
                redirect.setPageTemplate("sinarmasvafull_input.html");
                //redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setUrlAction(config.getString("URL.GOBACK"));
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(statusCode);
                HashMap<String, String> data = redirect.getParameters();
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                data.put("WORDS", super.generateMerchantRedirectWordsBeforePayment(paymentRequest, m));
                //data.put("MERCHANTID", mpc.getMerchantPaymentChannelUid());
                //data.put("MALLID", m.getMerchantCode() + "");
                //data.put("MALLID", mpc.getPaymentChannelCode() + "");
                data.put("STATUSCODE", OneCheckoutErrorMessage.NOT_YET_PAID.value());
                data.put("PAYMENTCHANNEL", OneCheckoutPaymentChannel.SinarMasVAFull.value());
                data.put("SESSIONID", paymentRequest.getSESSIONID());
                data.put("PAYMENTCODE", paymentRequest.getPAYCODE());
                data.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());
//                String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                data.put("OCOID", trans.getOcoId());
                trxHelper.setMessage("VALID");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);
                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);
            } else {
                
                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);                   
            
            }
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doPayment - T2: %d Finish", (System.currentTimeMillis() - t1));
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
        OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid : Void is not permitted");
        return trxHelper;
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));
            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();
            String merchantCode = verifyRequest.getMALLID();
            MerchantPaymentChannel mpc = queryHelper.getMerchantPaymentChannel(merchantCode, acq);
            Merchants merchant = mpc.getMerchants();
            trxHelper.setMerchant(merchant);
            String payCode = verifyRequest.getVANUMBER();
            queryHelper.updateVirtualAccountExpiredTransactions(payCode, merchantCode, acq, mpc.getInvExpiredInMinutes());
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice - querying transaction");
            Transactions trans = null;
            if (merchant.getMerchantReusablePaycode() != null && merchant.getMerchantReusablePaycode() == Boolean.TRUE) {
                trans = queryHelper.getInitVATransactionBy(payCode, merchantCode, acq);
                verifyRequest.setBANK("SinarMas");
                trxHelper.setVerifyRequest(verifyRequest);
                trxHelper.setCIPMIP(OneCheckoutMethod.MIP);
                trxHelper.setPaymentChannel(OneCheckoutPaymentChannel.SinarMasVAFull);
                if (trans != null) {
                    trans = transacBean.createReuseVATransactions(trxHelper, mpc, trans);
                }
            } else if (merchant.getMerchantDirectInquiry() != null && merchant.getMerchantDirectInquiry()) {
                String transExist = super.doDirectInquiry(trxHelper, mpc, verifyRequest);
                if (transExist.equalsIgnoreCase("2") || transExist.equalsIgnoreCase("3") || transExist.equalsIgnoreCase("4") || transExist.equalsIgnoreCase("5")) {
                    OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice - T2: transaction already paid");
                    String ack = emptyInquiryData(verifyRequest, transExist);
                    verifyRequest.setACKNOWLEDGE(ack);
                    trxHelper.setVerifyRequest(verifyRequest);
                    return trxHelper;
                } else if (transExist.equalsIgnoreCase("0")) {
                    OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice - T2: transaction is not found");
                    String ack = emptyInquiryData(verifyRequest, transExist);
                    verifyRequest.setACKNOWLEDGE(ack);
                    trxHelper.setVerifyRequest(verifyRequest);
                    return trxHelper;
                } 
                else if (transExist.equalsIgnoreCase("EMPTY")) {
                    trans = null;
                } else {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                    trans.setMerchantPaymentChannel(mpc);
                    OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice : Inquiry Successfull : %s");
                }
            } else {
                trans = queryHelper.getInquiryVATransactionBy(payCode, merchantCode, acq);
            }

            if (trans == null) {
                OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice : Transaction is null");
                String ack = emptyInquiryData(verifyRequest, "0");
                verifyRequest.setACKNOWLEDGE(ack);
                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;
            }

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setDokuHostRefNum(verifyRequest.getTRACENO());
            trans.setTransactionsState(OneCheckoutTransactionState.INCOMING.value());
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice - T2: update transaction");
            String ack = createInquiryResponseXML(verifyRequest, trans);
            em.merge(trans);

            //OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice - Notify XML : " + ack);

            verifyRequest.setACKNOWLEDGE(ack);
            trxHelper.setVerifyRequest(verifyRequest);
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            String ack = emptyInquiryData(trxHelper.getVerifyRequest(), "0");
            trxHelper.getVerifyRequest().setACKNOWLEDGE(ack);
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getPermataVATransactionBy(notifyRequest.getSINARMAS_VANUMBER(), notifyRequest.getSINARMAS_MALLID(), notifyRequest.getTRANSIDMERCHANT(), acq, notifyRequest.getSINARMAS_STEP());
 //           OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
            if (trans == null) {
                OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;
            }

            String word = super.generateSinarMasNotifyRequestWords(notifyRequest, trans);
            OneCheckoutLogger.log("HASH PASSWORDS FROM ONECHECKOUT : %s", word);
            OneCheckoutLogger.log("HASH PASSWORDS FROM CHANNEL     : %s", notifyRequest.getWORDS());
            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {
                OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;
            }

            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.SUCCESS;
            //if (notifyRequest.getRESULTMSG() != null && notifyRequest.getRESULTMSG().toUpperCase().indexOf("SUCCESS") >= 0) {
            /*if (notifyRequest.getRESPONSECODE() != null && notifyRequest.getRESPONSECODE().toUpperCase().indexOf("00") >= 0) {
            status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
            status = OneCheckoutTransactionStatus.FAILED;
            }*/

            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuInvokeStatusDatetime(new Date());
            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            trans.setDokuResponseCode("0000");
            trans.setDokuHostRefNum(notifyRequest.getSINARMAS_TRACENO());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setIncAmount(BigDecimal.valueOf(notifyRequest.getAMOUNT()));

            
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus - %s", trans.getDokuResultMessage());


            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);

            params.put("PAYMENTCODE", trans.getAccountId());

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);            
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus : statusNotify : %s", resp);

            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));
            // proses parsing ack from merchant, then save it to database
            em.merge(trans);
            notifyRequest.setACKNOWLEDGE(resp);
            trxHelper.setNotifyRequest(notifyRequest);
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));
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
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();
            Transactions trans = queryHelper.getRedirectTransactionBy(redirectRequest.getTRANSIDMERCHANT(), redirectRequest.getSESSIONID(), redirectRequest.getAMOUNT(), redirectRequest.getPAYMENTCODE(), redirectRequest.getPAYMENTCHANNEL());
            //trans.setDokuResponseCode(trans.getDokuResponseCode() != null ? trans.getDokuResponseCode() : redirectRequest.getSTATUSCODE());
            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                if (m.getMerchantReusablePaycode() != null && m.getMerchantReusablePaycode() == Boolean.TRUE) {
                } else {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                    trans.setDokuResultMessage(OneCheckoutErrorMessage.NOT_YET_PAID.name());
                }
            } else {
                trans.setDokuResponseCode(trans.getDokuResponseCode() != null ? trans.getDokuResponseCode() : redirectRequest.getSTATUSCODE());
            }
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setPageTemplate("redirect.html");
            redirect.setUrlAction(m.getMerchantRedirectUrl());
  //          redirect.setAMOUNT(redirectRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
            redirect.setSTATUSCODE(trans.getDokuResponseCode());
            Map data = redirect.getParameters();
            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());

            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(redirectRequest.getAMOUNT()));
            data.put("TRANSIDMERCHANT", redirectRequest.getTRANSIDMERCHANT());
            data.put("STATUSCODE", trans.getDokuResponseCode());
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", redirectRequest.getSESSIONID());
            data.put("PAYMENTCODE", redirectRequest.getPAYMENTCODE());
            data.put("PURCHASECURRENCY", trans.getIncPurchasecurrency());//getPURCHASECURRENCY());

            if (trans.getRates() != null) {
                redirect.setAMOUNT(trans.getIncPurchaseamount().doubleValue());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
                data.put("CURRENCY", trans.getIncPurchasecurrency());//.getPURCHASECURRENCY());
            } else {
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
            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
        OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            String invoiceNo = statusRequest.getTRANSIDMERCHANT();
            String sessionId = statusRequest.getSESSIONID();
            oneCheckoutNotifyStatusRequest.setTRANSIDMERCHANT(invoiceNo);


            OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getCheckVAStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.INCOMING);

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
                sb.append("&").append("ACQUIRERID=").append("700");
                sb.append("&").append("VANUMBER=").append(trans.getAccountId());
                sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNo);
                sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));

               
                OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
                InternetResponse inetResp = super.doFetchHTTP(sb.toString(), trans.getMerchantPaymentChannel().getPaymentChannel().getCheckStatusUrl(),  m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                String words = "";
                String paymentStatus = "";
                String paymentResponseCode = "";
                String paymentRefNumber = "";
                String cancelResponseCode = "";
                String cancelRefNumber = "";
                String acquirerBank = "";
                String maskedCard = "";
                String paymentDateTime = "";
                String bank = "";
                if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                    xml.load(sr);
                    paymentStatus = xml.getString("STATUS") != null ? xml.getString("STATUS").trim() : "";
                    if (paymentStatus != null && !paymentStatus.trim().equalsIgnoreCase("") && !paymentStatus.trim().equalsIgnoreCase("INVALID_REQUEST")) {
                        if (!paymentStatus.trim().equalsIgnoreCase("TRX_NOT_FOUND")) {
                            paymentResponseCode = xml.getString("RESPONSECODE") != null ? xml.getString("RESPONSECODE").trim() : "";
                            paymentRefNumber = xml.getString("HOSTREFNUMBER") != null ? xml.getString("HOSTREFNUMBER").trim() : "";
                            cancelResponseCode = xml.getString("CANCELRESPONSECODE") != null ? xml.getString("CANCELRESPONSECODE").trim() : "";
                            cancelRefNumber = xml.getString("CANCELHOSTREFNUMBER") != null ? xml.getString("CANCELHOSTREFNUMBER").trim() : "";
                            acquirerBank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
                            maskedCard = xml.getString("CARDNUMBER") != null ? xml.getString("CARDNUMBER").trim() : "";
                            paymentDateTime = xml.getString("DATETIME") != null ? xml.getString("DATETIME").trim() : "";
                            bank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
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

                //     params.put("BANK", pChannelProc.name());
                params.put("PAYMENTCODE", trans.getAccountId());
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
    private String createInquiryResponseXML(OneCheckoutDOKUVerifyData verifyRequest, Transactions trans) {

        String ack = "";
        try {
            StringBuilder sb = new StringBuilder();
            if (trans != null) {
                String vaNumber = verifyRequest.getVANUMBER() != null ? StringEscapeUtils.escapeXml(verifyRequest.getVANUMBER().trim()) : "";
                String transIdMerchant = trans.getIncTransidmerchant() != null ? StringEscapeUtils.escapeXml(trans.getIncTransidmerchant().trim()) : "";
                String sessionId = trans.getIncSessionid() != null ? StringEscapeUtils.escapeXml(trans.getIncSessionid().trim()) : "";
                String customerName = trans.getIncName() != null ? StringEscapeUtils.escapeXml(trans.getIncName().trim()) : "";
                String customerEmail = trans.getIncEmail() != null ? StringEscapeUtils.escapeXml(trans.getIncEmail().trim()) : "";
                
                sb.append("<?xml version=\"1.0\"?>");
                sb.append("<transactionInfo>");
                sb.append("<isTransExist>").append("1").append("</isTransExist>");
                sb.append("<vaNumber>").append(vaNumber).append("</vaNumber>");
                sb.append("<transactionNo>").append(transIdMerchant).append("</transactionNo>");
                sb.append("<sessionId>").append(sessionId).append("</sessionId>");
                sb.append("<customerName>").append(customerName).append("</customerName>");
                sb.append("<customerPhone>").append(URLEncoder.encode("", "UTF-8")).append("</customerPhone>");     
                sb.append("<customerEmail>").append(customerEmail).append("</customerEmail>");
                sb.append("<transactionDate>").append(StringEscapeUtils.escapeXml(OneCheckoutVerifyFormatData.klikbca_datetimeFormat.format(trans.getIncRequestdatetime()))).append("</transactionDate>");
                sb.append("<amount>").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount())).append("</amount>");

                OneCheckoutMerchantCategory cat = OneCheckoutMerchantCategory.findType(trans.getMerchantPaymentChannel().getMerchants().getMerchantCategory());
                if (cat == OneCheckoutMerchantCategory.AIRLINE) {
                    TransactionsDataAirlines airlines = queryHelper.getTransactionAirlinesBy(trans);
                    OneCheckoutReturnType rtype = OneCheckoutReturnType.findType(airlines.getIncFlighttype());
                    String basket = "Booking Code - " + airlines.getIncBookingcode();
                    sb.append("<description>").append(StringEscapeUtils.escapeXml(basket)).append("</description>");
                } else if (cat == OneCheckoutMerchantCategory.NONAIRLINE) {
                    TransactionsDataNonAirlines nonAirlines = queryHelper.getTransactionNonAirlinesBy(trans);
                    sb.append("<description>").append(StringEscapeUtils.escapeXml(nonAirlines.getIncBasket())).append("</description>");
                } else {
                    sb.append("</description>");
                }

                if (trans.getIncAdditionalInformation() != null && !trans.getIncAdditionalInformation().isEmpty()) {
                    sb.append("<additionalInfo>").append(trans.getIncAdditionalInformation()).append("</additionalInfo>");
                } else {
                    sb.append("<additionalInfo>").append("").append("</additionalInfo>");
                }

                String name = trans.getIncName();
                if (name != null && name.length() > 20) {
                    name = name.substring(0, 20);
                }

                sb.append("<customerName>").append(StringEscapeUtils.escapeXml(name)).append("</customerName>");
                
//                String paymentChannelId = trans.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                sb.append("<ocoId>").append(StringEscapeUtils.escapeXml(trans.getOcoId())).append("</ocoId>");
                
                sb.append("</transactionInfo>");
                trans.setDokuInquiryInvoiceDatetime(new Date());
                em.merge(trans);
                ack = sb.toString();
            } else {
                ack = emptyInquiryData(verifyRequest, "0");
            }
            return ack;
        } catch (Throwable e) {
            e.printStackTrace();
            ack = emptyInquiryData(verifyRequest, "0");
            return ack;
        }
    }

    private String emptyInquiryData(OneCheckoutDOKUVerifyData verifyRequest, String errorCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<transactionInfo>");
        sb.append("<isTransExist>").append(errorCode).append("</isTransExist>");
        try {
            sb.append("<vaNumber>").append(URLEncoder.encode(verifyRequest.getVANUMBER(), "UTF-8")).append("</vaNumber>");
        } catch (UnsupportedEncodingException ex) {
            sb.append("<vaNumber>").append(verifyRequest.getVANUMBER()).append("</vaNumber>");
        }

        sb.append("<transactionNo>").append("</transactionNo>");
        sb.append("<transactionDate>").append("</transactionDate>");
        sb.append("<amount>").append("</amount>");
        sb.append("<description>").append("</description>");
        sb.append("<additionalInfo>").append("</additionalInfo>");
        sb.append("<customerName>").append("</customerName>");
        sb.append("</transactionInfo>");
        return sb.toString();
    }

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getPermataVATransactionBy(notifyRequest.getSINARMAS_VANUMBER(), notifyRequest.getSINARMAS_MALLID(), notifyRequest.getTRANSIDMERCHANT(), acq, notifyRequest.getSINARMAS_STEP());
            if (trans == null) {
                OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;
            }

            String word = super.generateSinarMasNotifyRequestWords(notifyRequest, trans);
            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {
                OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuVoidDatetime(new Date());
            trans.setDokuVoidResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuVoidApprovalCode(notifyRequest.getAPPROVALCODE());

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getRESULTMSG() != null && notifyRequest.getRESULTMSG().toUpperCase().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            //trans.setTransactionsStatus(status.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());


            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", trans.getAccountId());
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);            
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doReversal : statusNotify : %s", resp);
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doReversal - T4: %d update trx record", (System.currentTimeMillis() - t1));

            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);
            trxHelper.setNotifyRequest(notifyRequest);
            OneCheckoutLogger.log("OneCheckoutV1SinarMasVAFullBean.doVoid - T5: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
