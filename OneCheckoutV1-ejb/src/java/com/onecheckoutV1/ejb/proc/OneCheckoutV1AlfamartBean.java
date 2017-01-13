 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

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
import com.onecheckoutV1.scheduler.SendEmailVAToCustomerBean;
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
import doku.virtualaccount.wsdl.MerchantVA;
import doku.virtualaccount.xml.VARegistrationParamDocument;
import doku.virtualaccount.xml.VARegistrationParamDocument.VARegistrationParam;
import doku.virtualaccount.xml.VARegistrationResponseDocument;
import doku.virtualaccount.xml.VARegistrationResponseDocument.VARegistrationResponse;
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
import org.apache.commons.configuration.PropertiesConfiguration;
import javax.persistence.PersistenceContext;
import javax.xml.ws.BindingProvider;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author hafizsjafioedin
 */
@Stateless
public class OneCheckoutV1AlfamartBean extends OneCheckoutChannelBase implements OneCheckoutV1AlfamartBeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    private OneCheckoutPaymentChannel pChannelProc = OneCheckoutPaymentChannel.Alfamart;

    private boolean ableToReversal = true;

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

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
                try {
                    // Register To Virtual Account Before Save Databases
                    VARegistrationResponse vaRegistrationResponse = registerVa(trxHelper);
                    if (vaRegistrationResponse != null) {
                        trxHelper.setSystemMessage(vaRegistrationResponse.getResponseCode());
                    }

                } catch (Throwable t) {
                }
                
                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }

                //set data redirect yang akan di kirim ke merchant
                redirect.setPageTemplate("alfamartvalite_input.html");

                redirect.setUrlAction(config.getString("URL.GOBACK"));
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(statusCode);
                HashMap<String, String> data = redirect.getParameters();

                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                data.put("WORDS", super.generateMerchantRedirectWordsBeforePayment(paymentRequest, m));
                data.put("STATUSCODE", OneCheckoutErrorMessage.NOT_YET_PAID.value());
                data.put("PAYMENTCHANNEL", OneCheckoutPaymentChannel.Alfamart.value());
                data.put("SESSIONID", paymentRequest.getSESSIONID());
                data.put("PAYMENTCODE", paymentRequest.getPAYCODE());
                data.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());
                data.put("EMAIL", trans.getIncEmail());
                //data.put("OCOID", String.valueOf(trans.getTransactionsId()));
                data.put("DEVICEID", paymentRequest.getDEVICEID() != null ? paymentRequest.getDEVICEID() : "");
//                data.put("UA_BROWSER", paymentRequest.getUA_BROWSER() != null ? paymentRequest.getUA_BROWSER() : "");
//                data.put("UA_OS", paymentRequest.getUA_OS() != null ? paymentRequest.getUA_OS() : "");
//                data.put("UA_OSVERSION", paymentRequest.getUA_OSVERSION() != null ? paymentRequest.getUA_OSVERSION() : "");
//                data.put("UA_SCREENPRINT", paymentRequest.getUA_SCREENPRINT() != null ? paymentRequest.getUA_SCREENPRINT() : "");
//                data.put("UA_LOWERCASE", paymentRequest.getUA_LOWERCASE() != null ? paymentRequest.getUA_LOWERCASE() : "");
                data.put("USERNAME", paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");
                data.put("CUSTIP", paymentRequest.getCUSTIP());
                data.put("PAYREASON", paymentRequest.getPAYREASON());
                //String ocoId = this.generateOcoId(pChannel.getPaymentChannelId());
                data.put("OCOID", trxHelper.getOcoId());
                trxHelper.setMessage("VALID");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

                SendEmailVAToCustomerBean sendEmailVAToCustomerBean = new SendEmailVAToCustomerBean();
                sendEmailVAToCustomerBean.sendEmailVAInfo(trans, config, m, trxHelper);

            } else {

                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);

            }

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doPayment - T2: %d Finish", (System.currentTimeMillis() - t1));

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
        OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doVoid : Void is not permitted");

        return trxHelper;

    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();

            //    PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            //    Merchants merchant = trxHelper.getMerchant();
            String merchantCode = verifyRequest.getMALLID();
            MerchantPaymentChannel mpc = queryHelper.getMerchantPaymentChannel(merchantCode, acq);
            Merchants merchant = mpc.getMerchants();
            trxHelper.setMerchant(merchant);
            //String payCode = config.getInt("ALFAMART.INIT.NUMBER") + verifyRequest.getCOMPANYCODE() + OneCheckoutVerifyFormatData.fourDigitMalldId.format(Integer.parseInt(merchantCode)) + OneCheckoutVerifyFormatData.traceNo.format(Integer.parseInt(verifyRequest.getUSERACCOUNT()));//String payCode = verifyRequest.getPERMATA_VANUMBER();
            String payCode = verifyRequest.getCOMPANYCODE() + OneCheckoutVerifyFormatData.threeDigitMalldId.format(Integer.parseInt(merchantCode)) + OneCheckoutVerifyFormatData.traceNo.format(Integer.parseInt(verifyRequest.getUSERACCOUNT()));//String payCode = verifyRequest.getPERMATA_VANUMBER();
            verifyRequest.setVANUMBER(payCode);
            verifyRequest.setPAYCODE(payCode);

            queryHelper.updateVirtualAccountExpiredTransactions(payCode, merchantCode, acq, mpc.getInvExpiredInMinutes());

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - querying transaction");

            Transactions trans = null;

            trans = queryHelper.getInquiryVATransactionBy(payCode, merchantCode, acq);

            Boolean stopReponse = false;
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - trans : " + trans);

            if (trans == null && merchant.getMerchantDirectInquiry() != null && merchant.getMerchantDirectInquiry()) {

                String transExist = super.doDirectInquiry(trxHelper, mpc, verifyRequest);

                if (transExist.equalsIgnoreCase("2")) {

                    OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - T2: transaction already paid");

                    String ack = createInquiryResponseXML(verifyRequest, trans, transExist);

                    verifyRequest.setACKNOWLEDGE(ack);

                    trxHelper.setVerifyRequest(verifyRequest);

                    return trxHelper;

                } else if (transExist.equalsIgnoreCase("0")) {

                    OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - T2: transaction is not found");

                    String ack = createInquiryResponseXML(verifyRequest, trans, transExist);

                    verifyRequest.setACKNOWLEDGE(ack);

                    trxHelper.setVerifyRequest(verifyRequest);

                    return trxHelper;

                } else if (transExist.equalsIgnoreCase("EMPTY")) {
                    trans = null;
                    stopReponse = true;

                } else {

                    trans = transacBean.saveTransactions(trxHelper, mpc);
                    trans.setMerchantPaymentChannel(mpc);
                    OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice : Inquiry Successfull : %s");
                }

            } else if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - merchant.getMerchantName : " + merchant.getMerchantName());
                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - merchant.getMerchantReusablePaycode : " + merchant.getMerchantReusablePaycode());
                if (merchant.getMerchantReusablePaycode() != null && merchant.getMerchantReusablePaycode() == Boolean.FALSE) {
                    trans = queryHelper.getVirtualTransactionBy(payCode, merchantCode, acq);

                    if (trans != null) {

                        if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value()) {

                            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - T2: transaction already paid");
                            String transExist = "2";
                            String ack = createInquiryResponseXML(verifyRequest, trans, transExist);

                            //OneCheckoutLogger.log("OneCheckoutV1PermataVALiteBean.doInquiryInvoice - Notify XML : " + ack);
                            verifyRequest.setACKNOWLEDGE(ack);

                            trxHelper.setVerifyRequest(verifyRequest);

                            return trxHelper;
                        } else {
                            stopReponse = true;
                        }
                    }

                } else {
                    stopReponse = true;
                }

            }

            if (stopReponse) {

                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice : Transaction is null");
                String ack = emptyInquiryData(verifyRequest, "0");
                verifyRequest.setACKNOWLEDGE(ack);

                trxHelper.setVerifyRequest(verifyRequest);

                return trxHelper;
            }

            String word = super.generateDOKUAgregatorVerifyRequestWords(verifyRequest, trans);

            if (!word.equalsIgnoreCase(verifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice : WORDS doesn't match !");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setDokuHostRefNum(verifyRequest.getTRACENO());

            trans = nl.refreshAmountBaseNewRate(trans);

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - T2: update transaction");
            String transExist = "1";
            String ack = createInquiryResponseXML(verifyRequest, trans, transExist);

            em.merge(trans);

            //OneCheckoutLogger.log("OneCheckoutV1PermataVALiteBean.doInquiryInvoice - Notify XML : " + ack);
            verifyRequest.setACKNOWLEDGE(ack);

            trxHelper.setVerifyRequest(verifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            //String payCode = config.getInt("ALFAMART.INIT.NUMBER") + notifyRequest.getCOMPANYCODE() + OneCheckoutVerifyFormatData.fourDigitMalldId.format(Integer.parseInt(notifyRequest.getMERCHANTCODE())) + OneCheckoutVerifyFormatData.traceNo.format(Integer.parseInt(notifyRequest.getUSERACCOUNT()));//String payCode = verifyRequest.getPERMATA_VANUMBER();
            String payCode = notifyRequest.getCOMPANYCODE() + OneCheckoutVerifyFormatData.threeDigitMalldId.format(Integer.parseInt(notifyRequest.getMERCHANTCODE())) + OneCheckoutVerifyFormatData.traceNo.format(Integer.parseInt(notifyRequest.getUSERACCOUNT()));//String payCode = verifyRequest.getPERMATA_VANUMBER();

            notifyRequest.setPERMATA_VANUMBER(payCode);

            Transactions trans = queryHelper.getPermataVATransactionByWithAmount(payCode, new BigDecimal(notifyRequest.getAMOUNT()), notifyRequest.getMERCHANTCODE(), acq, notifyRequest.getSTEP());
//            OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();

            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUAgregatorNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.SUCCESS;

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());

            trans.setDokuInvokeStatusDatetime(new Date());

            trans.setDokuResponseCode("0000");
            trans.setDokuHostRefNum(notifyRequest.getHOSTREFNUM());
            trans.setDokuApprovalCode(notifyRequest.getHOSTREFNUM());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - %s", trans.getDokuResultMessage());
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - %s", trans.getDokuHostRefNum());

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);

            params.put("PAYMENTCODE", trans.getAccountId());

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));

            // proses parsing ack from merchant, then save it to database
            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();
            Transactions trans = queryHelper.getRedirectTransactionBy(redirectRequest.getTRANSIDMERCHANT(), redirectRequest.getSESSIONID(), redirectRequest.getAMOUNT(), redirectRequest.getPAYMENTCODE(), redirectRequest.getPAYMENTCHANNEL());
            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.NOT_YET_PAID.name());
            } else {
                trans.setDokuResponseCode(trans.getDokuResponseCode() != null ? trans.getDokuResponseCode() : redirectRequest.getSTATUSCODE());
            }

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setUrlAction(m.getMerchantRedirectUrl());
            redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
            redirect.setSTATUSCODE(trans.getDokuResponseCode());
            Map data = redirect.getParameters();
            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());
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
            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);
            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doRedirectToMerchant - T1: %d Register DOKUWallet", (System.currentTimeMillis() - t1));
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
            redirect = super.addDWRegistrationPage(trxHelper, redirect, channel, trans.getMerchantPaymentChannel(), trans);
            redirect.setPageTemplate("redirect.html");
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    /*
     public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
     OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
     //      OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
     try {
     long t1 = System.currentTimeMillis();
     OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

     OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
     Merchants m = trxHelper.getMerchant();

     String invoiceNo = statusRequest.getTRANSIDMERCHANT();
     String sessionId = statusRequest.getSESSIONID();


     OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
     Transactions trans = queryHelper.getCheckVAStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.INCOMING);

     if (trans == null) {
     trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);
     }

     if (trans != null) {


     String word = super.generateCheckStatusRequestWords(trans, m);

     if (!word.equalsIgnoreCase(checkStatusRequest.getWORDS())) {
     // Create Empty Notify
     OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
     statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
     return trxHelper;
     }

     // create NotificationRequest;

     HashMap<String, String> params = super.getData(trans);

     params.put("PAYMENTCODE", trans.getAccountId());
     OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();



     trxHelper.setMessage("VALID");
     statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params, m));
     trxHelper.setCheckStatusRequest(statusRequest);
     return trxHelper;

     }


     // Create Empty Notify
     OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);

     statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());

     trxHelper.setCheckStatusRequest(statusRequest);
     trxHelper.setMessage("VALID");


     OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));

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
     */
    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            String invoiceNo = statusRequest.getTRANSIDMERCHANT();
            String sessionId = statusRequest.getSESSIONID();
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getCheckVAStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.INCOMING);
            if (trans == null) {
                trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);
            }
            if (trans != null) {
                String word = super.generateCheckStatusRequestWords(trans, m);
                if (!word.equalsIgnoreCase(checkStatusRequest.getWORDS())) {
                    // Create Empty Notify
                    OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
                    statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
                    return trxHelper;
                }

                OneCheckoutLogger.log("OneCheckoutV1DokupayBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREAlfa(trans);
                HashMap<String, String> params = super.getData(trans);
                params.put("PAYMENTCODE", trans.getAccountId());
                trxHelper.setMessage("VALID");
                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params, m));//.toMPGString());
                trxHelper.setCheckStatusRequest(statusRequest);
                return trxHelper;
            }

            // Create Empty Notify
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));
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

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal - T0: %d Start process", (System.currentTimeMillis() - t1));

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            String payCode = config.getInt("ALFAMART.INIT.NUMBER") + notifyRequest.getCOMPANYCODE() + OneCheckoutVerifyFormatData.threeDigitMalldId.format(Integer.parseInt(notifyRequest.getMERCHANTCODE())) + OneCheckoutVerifyFormatData.traceNo.format(Integer.parseInt(notifyRequest.getUSERACCOUNT()));//String payCode = verifyRequest.getPERMATA_VANUMBER();
            notifyRequest.setPERMATA_VANUMBER(payCode);

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getPermataVATransactionByWithAmount(payCode, new BigDecimal(notifyRequest.getAMOUNT()), notifyRequest.getMERCHANTCODE(), acq, notifyRequest.getSTEP());

            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUAgregatorNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());
            trans.setDokuVoidDatetime(new Date());
            trans.setDokuVoidResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuVoidApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setVerifyId("");
            trans.setVerifyScore(-1);
            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getRESULTMSG() != null && notifyRequest.getRESULTMSG().toUpperCase().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            //trans.setTransactionsStatus(status.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);

            params.put("PAYMENTCODE", trans.getAccountId());

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);
            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal - T4: %d update trx record", (System.currentTimeMillis() - t1));

            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1AlfamartBean.doReversal - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }

    }

    public VARegistrationResponse registerVa(OneCheckoutDataHelper trxHelper) {

        try {

            StringBuilder sb = new StringBuilder();

            doku.virtualaccount.wsdl.MerchantVAService service = new doku.virtualaccount.wsdl.MerchantVAService();
            MerchantVA port = service.getMerchantVAPort();

            //BindingProvider bp = (BindingProvider) port;
            //bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, config.getString("onecheckout.wsdlurl", "http://localhost:8080/VA/MerchantVAService"));

            VARegistrationParamDocument registrationParamDoc = VARegistrationParamDocument.Factory.newInstance();
            VARegistrationParam registrationParam = registrationParamDoc.addNewVARegistrationParam();

            String payCode = trxHelper.getPaymentRequest().getPAYCODE();
            payCode = payCode.substring((payCode.length() - 8), payCode.length());

            VARegistrationParam.VirtualAccount va = registrationParam.addNewVirtualAccount();
            va.setVirtualAccountId(payCode);
            va.setCurrency(trxHelper.getPaymentRequest().getPURCHASECURRENCY());
            va.addNewInfo();

            // trxHelper.getMerchant().getMerchantName()
            String customerName = trxHelper.getPaymentRequest().getNAME();

            if (customerName != null && customerName.length() > 19) {
                customerName = customerName.substring(0, 19);
            }

            va.getInfo().addDetail(customerName);

            //registrationParam.setVirtualAccountArray(new VARegistrationParam.VirtualAccount[1]);
            registrationParam.setMerchantId(String.valueOf(trxHelper.getPaymentChannelCode()));
            registrationParam.setUniqueId(payCode);

            /*registrationParam.getVirtualAccountArray()[1].setVirtualAccountId(trxHelper.getPaymentRequest().getPAYCODE());
             registrationParam.getVirtualAccountArray()[1].setCurrency(trxHelper.getPaymentRequest().getPURCHASECURRENCY());
             registrationParam.getVirtualAccountArray()[1].addNewInfo();
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail(trxHelper.getMerchant().getMerchantName());
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");*/
            OneCheckoutLogger.log("::before paymentResponse");
            OneCheckoutLogger.log(":: Value Of = " + registrationParamDoc.toString());
            String paymentResponse = port.registerVirtualAccount(registrationParamDoc.toString());
            

            VARegistrationResponseDocument vaRegistrationResponseDoc = VARegistrationResponseDocument.Factory.parse(paymentResponse);
            VARegistrationResponse vaRegistrationResponse = vaRegistrationResponseDoc.getVARegistrationResponse();

            return vaRegistrationResponse;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    private String emptyInquiryData(OneCheckoutDOKUVerifyData verifyRequest, String transExist) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<transactionInfo>");
        sb.append("<isTransExist>").append(transExist).append("</isTransExist>");

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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private String createInquiryResponseXML(OneCheckoutDOKUVerifyData verifyRequest, Transactions trans, String transExist) {

        String ack = "";
        try {

            StringBuilder sb = new StringBuilder();

            if (trans != null) {
                String vaNumber = verifyRequest.getVANUMBER() != null ? StringEscapeUtils.escapeXml(verifyRequest.getVANUMBER().trim()) : "";
                String transIdMerchant = trans.getIncTransidmerchant() != null ? StringEscapeUtils.escapeXml(trans.getIncTransidmerchant().trim()): "";
                String sessionId = trans.getIncSessionid() != null ? StringEscapeUtils.escapeXml(trans.getIncSessionid().trim()) : "";
                String customerName = trans.getIncName() != null ? StringEscapeUtils.escapeXml(trans.getIncName().trim()) : "";
                String customerEmail = trans.getIncEmail() != null ? StringEscapeUtils.escapeXml(trans.getIncEmail().trim()) : "";
                sb.append("<?xml version=\"1.0\"?>");
                sb.append("<transactionInfo>");
                sb.append("<isTransExist>").append(transExist).append("</isTransExist>");
                sb.append("<vaNumber>").append(vaNumber).append("</vaNumber>");
                sb.append("<transactionNo>").append(transIdMerchant).append("</transactionNo>");
                sb.append("<sessionId>").append(sessionId).append("</sessionId>");
                sb.append("<customerName>").append(customerName).append("</customerName>");
                sb.append("<customerPhone>").append(StringEscapeUtils.unescapeXml("")).append("</customerPhone>");
                sb.append("<customerEmail>").append(customerEmail).append("</customerEmail>");
                sb.append("<transactionDate>").append(StringEscapeUtils.escapeXml(OneCheckoutVerifyFormatData.klikbca_datetimeFormat.format(trans.getIncRequestdatetime()))).append("</transactionDate>");
                sb.append("<amount>").append(StringEscapeUtils.escapeXml(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()))).append("</amount>");
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
                    sb.append("<description>").append("</description>");
                }
                sb.append("<additionalInfo>").append("").append("</additionalInfo>");
                String name = trans.getIncName();
                if (name != null && name.length() > 20) {
                    name = name.substring(0, 20);
                }
                sb.append("<customerName>").append(StringEscapeUtils.escapeXml(name)).append("</customerName>");
                sb.append("</transactionInfo>");
                trans.setDokuInquiryInvoiceDatetime(new Date());
                em.merge(trans);
                ack = sb.toString();
            } else {
                ack = emptyInquiryData(verifyRequest, transExist);
            }

            return ack;
        } catch (Throwable e) {

            e.printStackTrace();
            ack = emptyInquiryData(verifyRequest, transExist);
            return ack;

        }

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
