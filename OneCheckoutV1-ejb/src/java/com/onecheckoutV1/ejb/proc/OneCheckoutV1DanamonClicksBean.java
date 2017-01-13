/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutCheckStatusData;
import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
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
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author iskandar
 */
@Stateless
public class OneCheckoutV1DanamonClicksBean extends OneCheckoutChannelBase implements OneCheckoutV1DanamonClicksBeanLocal{
    
    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    
    @PersistenceContext(unitName="ONECHECKOUTV1")
    protected EntityManager em;
    
    @Resource
    protected SessionContext ctx;
    
    private final static String mIDAcquirerId;
    private final static String serviceId;
    
    static{
         mIDAcquirerId = config.getString("ACQUIRERID.DANAMON.VALUE", "1006").trim();
         serviceId = config.getString("INTERNET.BANKING.SERVICE.ID","1").trim();
    }
    private boolean ableToReversal = true;

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        Transactions trans = null;
        OneCheckoutErrorMessage errorMsg = OneCheckoutErrorMessage.UNKNOWN;
        
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, acq, m);
            
            String statusCode = identrx.getStatusCode();
            boolean request_good = identrx.isRequestGood();
            boolean needNotify = identrx.isNeedNotify();
            MerchantPaymentChannel mpc = identrx.getMpc();
            
            if (request_good) {
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
                PaymentChannel pc = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.Danamon);
                PaymentChannel redirectPaymentChannel = new PaymentChannel();
                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                    String CoreUrl = pc.getRedirectPaymentUrlMip();
                    OneCheckoutLogger.log("URL PRE REDIRECT     : "+CoreUrl);
                    String xmls = preRedirect(trxHelper.getPaymentRequest(), CoreUrl, mpc, m, trans.getOcoId());
                    
                    if (!xmls.equals("")){
                        XMLConfiguration xml = new XMLConfiguration();
                        try {
                            StringReader sr = new StringReader(xmls);
                            xml.load(sr);
                            if (xml.getRootElementName().equals("MIBPaymentResponse") && xml.getString("result").equals("REDIRECT")){
                                redirectPaymentChannel.setRedirectPaymentUrlMip(xml.getString("redirectUrl"));
                                String param = xml.getString("redirectParameter");
                                OneCheckoutLogger.log("URL REDIRECT     : "+redirectPaymentChannel.getRedirectPaymentUrlMip());
                                OneCheckoutLogger.log("PARAM            : "+param);
                                
                                HashMap<String, String> parameter = parseParamRedirect(param);
                                String trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
                                trans.setDokuVoidApprovalCode(trxCode);
                                paymentRequest.setAllAdditionData(parameter);
                                redirect = createRedirectMIP(paymentRequest, redirectPaymentChannel, mpc, m, trans.getOcoId());
                                
                                trxHelper.setMessage("VALID");
                                trans.setRedirectDatetime(new Date());
                                trans.setSystemMessage(xmls);
                                trans.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());
                                em.merge(trans);
                            } else {
                                OneCheckoutLogger.log("::: FAILED PRE REDIRECT :::");
                                redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                                trxHelper.setMessage("FAILED");
                            }
                        } catch (Exception ex) {
                            OneCheckoutLogger.log("::: FAILED PRE REDIRECT :::");
                            redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                            trxHelper.setMessage("FAILED");
                            ex.printStackTrace();
                        }
                    } else {
                        OneCheckoutLogger.log("::: FAILED PRE REDIRECT :::");
                        redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                        trxHelper.setMessage("FAILED");
                    }
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                    String CoreUrl = pc.getRedirectPaymentUrlCip();
                    OneCheckoutLogger.log("URL PRE REDIRECT     : "+CoreUrl);
                    String xmls = preRedirect(trxHelper.getPaymentRequest(), CoreUrl, mpc, m, trans.getOcoId());
                    
                    if (!xmls.equals("")){
                        XMLConfiguration xml = new XMLConfiguration();
                        try {
                            StringReader sr = new StringReader(xmls);
                            xml.load(sr);
                            if (xml.getRootElementName().equals("MIBPaymentResponse") && xml.getString("result").equals("REDIRECT")){
                                redirectPaymentChannel.setRedirectPaymentUrlCip(xml.getString("redirectUrl"));
                                String param = xml.getString("redirectParameter");
                                
                                OneCheckoutLogger.log("URL REDIRECT DO  : "+redirectPaymentChannel.getRedirectPaymentUrlCip());
                                OneCheckoutLogger.log("PARAM            : ["+param+"]");
                                HashMap<String, String> parameter = parseParamRedirect(param);
                                
                                String trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
                                trans.setDokuVoidApprovalCode(trxCode);
                                paymentRequest.setAllAdditionData(parameter);
                                redirect = createRedirectCIP(paymentRequest, redirectPaymentChannel, mpc, m, trans.getOcoId());
                                
                                trxHelper.setMessage("VALID");
                                trans.setRedirectDatetime(new Date());
                                trans.setSystemMessage(xmls);
                                trans.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());
                                em.merge(trans);
                            } else {
                                OneCheckoutLogger.log("::: FAILED PRE REDIRECT :::");
                                redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                                trxHelper.setMessage("FAILED");
                            }
                        } catch (Exception ex) {
                            OneCheckoutLogger.log("::: FAILED PRE REDIRECT :::");
                            redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                            trxHelper.setMessage("FAILED");
                            ex.printStackTrace();
                        }
                    }else{
                        OneCheckoutLogger.log("::: FAILED PRE REDIRECT :::");
                        redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                        trxHelper.setMessage("FAILED");
                    }
                }
                trxHelper.setRedirect(redirect);
                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
            } else {
                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errorMsg, needNotify, trans);
            }
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doPayment - T2: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
          try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));
            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, paymentChannel);
            
            if (trans == null) {
                OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;
            }

            String word = super.generateMIBNotifyRequestWords(notifyRequest, trans);
            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {
                OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;
            }
            
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
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
            } else {
                trans.setVerifyId("");
                trans.setVerifyScore(0);
                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
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

            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
            HashMap<String, String> params = super.getData(trans);
            params.put("PAYMENTCODE", "");
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));
            em.merge(trans);
            notifyRequest.setACKNOWLEDGE(resp);
            trxHelper.setNotifyRequest(notifyRequest);
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));
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
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();
            String statusCode = "";
            String invoiceNo = null;
            String sessionId = null;
            double amount = 0.00;
            int attempts = 1;
            
            if (redirectRequest!=null){
                invoiceNo = redirectRequest.getTRANSIDMERCHANT();
                sessionId = redirectRequest.getSESSIONID();
                amount = redirectRequest.getAMOUNT();
                statusCode = redirectRequest.getSTATUSCODE();
            } else {
                invoiceNo = trxHelper.getTransactions().getIncTransidmerchant();
                sessionId = trxHelper.getTransactions().getIncSessionid();
                amount = trxHelper.getTransactions().getIncAmount().doubleValue();
            }

            Object[] obj = queryHelper.getRedirectTransactionWithoutStateNumber(invoiceNo, sessionId, amount);
            attempts = (Integer) obj[0];
            Transactions trans = (Transactions) obj[1];

            if (redirectRequest==null){
                trans = trxHelper.getTransactions();
                statusCode = trans.getDokuResponseCode();
            }

            OneCheckoutLogger.log("ATTEMPTS : " + attempts);
            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()){
                trans.setDokuResponseCode(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.name());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            }
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());

            if (redirectRequest!=null && redirectRequest.getFLAG().equalsIgnoreCase("1")){
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=1");
                if ((m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) && trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                    OneCheckoutLogger.log("FLAG=1 A");
                    if (attempts >= 3) {
                        OneCheckoutLogger.log("FLAG=1 A1");
                        redirect.setUrlAction(m.getMerchantRedirectUrl());
                        redirect.setPageTemplate("transactionFailed.html");
                    } else {
                        OneCheckoutLogger.log("FLAG=1 A2");
                        redirect.setUrlAction(config.getString("URL.RECEIVE"));
                        redirect.setPageTemplate("redirect.html");
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
            } else if (redirectRequest!=null && redirectRequest.getFLAG().equalsIgnoreCase("2")){
                statusCode = trans.getDokuResponseCode();
                redirectRequest.setSTATUSCODE(statusCode);
                trxHelper.setRedirectDoku(redirectRequest);
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
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
                data.put("CURRENCY", trans.getIncPurchasecurrency());
            }
            else {
                redirect.setAMOUNT(trans.getIncAmount().doubleValue());
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
            trxHelper.setRedirect(redirect);
            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            Transactions trans = trxHelper.getTransactions();
            
            if (trans != null) {
                String word = super.generateCheckStatusRequestWords(trans, m);
                
                if (!word.equalsIgnoreCase(checkStatusRequest.getWORDS())) {
                    OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
                    statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
                    return trxHelper;
                }
                
                OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREMIB(trans, Integer.parseInt(mIDAcquirerId));
                HashMap<String, String> params =  super.getData(trans);
                trxHelper.setMessage("VALID");
                params.put("PAYMENTCODE","");
                
                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params,m));
                trxHelper.setCheckStatusRequest(statusRequest);
                return trxHelper;
            }
            
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest,  trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");
            OneCheckoutLogger.log("OneCheckoutV1DanamonClicksBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            return trxHelper;
        }
    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        OneCheckoutLogger.log("============= createRedirectMIP =============");
        try {
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlMip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            redirect.setParameters(paymentRequest.getAllAdditionData());
            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        OneCheckoutLogger.log("============= createRedirectCIP =============");
        try {
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlCip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            redirect.setParameters(paymentRequest.getAllAdditionData());
            return redirect;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private HashMap<String, String> parseParamRedirect(String inParam) {
        try {
            HashMap<String, String> parameter = new HashMap<String, String>();           
            String[] params = inParam.trim().split(";;");
            for (String param : params) {
                String[] items = param.trim().split("\\|\\|");
                String paramName = items[0];
                String paramValue = "";
                if (items.length > 1) {
                    paramValue = !items[1].equals("null") ? items[1] : "";
                }
                OneCheckoutLogger.log(":: " + paramName + "[" + paramValue + "]");
                parameter.put(paramName, paramValue);
            }
            return parameter;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private String preRedirect(OneCheckoutPaymentRequest paymentRequest, String CoreUrl, MerchantPaymentChannel mpc, Merchants merchants, String ocoId) {
        OneCheckoutLogger.log("Get Redirect Param from Luna Service");
        String response = "";
        try {
            Integer MALLID = mpc.getPaymentChannelCode();
            String SHAREDKEY = merchants.getMerchantHashPassword();
            String WORDS = HashWithSHA1.doHashing(MALLID.toString() + SHAREDKEY, "SHA1", null);
            String CHAINMALLID = "";
            if (mpc.getPaymentChannelChainCode() != null && !"0".equals(mpc.getPaymentChannelChainCode().toString())) {
                CHAINMALLID = mpc.getPaymentChannelChainCode().toString();
            }
            String invoiceNumber = paymentRequest.getTRANSIDMERCHANT() != null ? paymentRequest.getTRANSIDMERCHANT().trim() : "";
            String currency = "IDR";
            Currency c = queryHelper.getCurrencyByCode(paymentRequest.getCURRENCY());
            if (c != null && c.getAlpha3Code() != null) {
                currency = c.getAlpha3Code();
            }
            
            String sessionId = paymentRequest.getSESSIONID() != null ? paymentRequest.getSESSIONID().trim() : "";
            String basket = paymentRequest.getBASKET() != null ? paymentRequest.getBASKET().trim() : "";
            String mobilePhone = paymentRequest.getMOBILEPHONE() != null ? paymentRequest.getMOBILEPHONE().trim() : "";
            String customerName = paymentRequest.getNAME() != null ? paymentRequest.getNAME().trim() : "";
            String customerEmail = paymentRequest.getEMAIL() != null ? paymentRequest.getEMAIL().trim() : "";
            
            String desc = "Payment Using Danamon Clicks";
            int pos = basket.indexOf(",");
            if (pos > 0) {
                desc = basket.substring(0, pos);
            }
            String[] strParam = {"UTF-8","&"};
            StringBuilder sb = new StringBuilder();
            sb.append("SERVICEID=").append(URLEncoder.encode(serviceId, strParam[0])).append(strParam[1]);
            sb.append("ACQUIRERID=").append(URLEncoder.encode(mIDAcquirerId, strParam[0])).append(strParam[1]);
            sb.append("MALLID=").append(URLEncoder.encode(MALLID.toString(), strParam[0])).append(strParam[1]);
            sb.append("CHAINMALLID=").append(URLEncoder.encode(CHAINMALLID, strParam[0])).append(strParam[1]);
            sb.append("WORDS=").append(URLEncoder.encode(WORDS, strParam[0])).append(strParam[1]);
            sb.append("INVOICENUMBER=").append(URLEncoder.encode(invoiceNumber, strParam[0])).append(strParam[1]);
            sb.append("CURRENCY=").append(URLEncoder.encode(currency, strParam[0])).append(strParam[1]);
            sb.append("AMOUNT=").append(URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()), strParam[0])).append(strParam[1]);
            sb.append("SESSIONID=").append(URLEncoder.encode(sessionId, strParam[0])).append(strParam[1]);
            sb.append("BASKET=").append(URLEncoder.encode(basket, strParam[0])).append(strParam[1]);
            sb.append("CUSTOMERMOBILEPHONE=").append(URLEncoder.encode(mobilePhone, strParam[0])).append(strParam[1]);
            sb.append("CUSTOMERNAME=").append(URLEncoder.encode(customerName, strParam[0])).append(strParam[1]);
            sb.append("CUSTOMEREMAIL=").append(URLEncoder.encode(customerEmail, strParam[0])).append(strParam[1]);
            sb.append("DESCRIPTION=").append(URLEncoder.encode(desc, strParam[0])).append(strParam[1]);
            String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            sb.append("SERVICETRANSACTIONID=").append(URLEncoder.encode(ocoId, strParam[0])).append(strParam[1]);
            
            OneCheckoutLogger.log("SEND PARAMETER " + sb.toString());
            
            InternetResponse internetResponse = super.doFetchHTTP(sb.toString(), CoreUrl, merchants.getMerchantReadTimeout(), merchants.getMerchantConnectionTimeout());
            response = internetResponse.getMsgResponse();
            OneCheckoutLogger.log("RESPONSE REDIRECT \n" + response);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return response;
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
    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}