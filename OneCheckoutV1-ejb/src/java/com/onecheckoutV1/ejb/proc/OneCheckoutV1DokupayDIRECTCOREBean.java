/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.reflect.TypeToken;
import com.onecheckoutV1.data.OneCheckoutCheckStatusData;
import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDOKUVerifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;

//import com.onecheckoutV1.ejb.helper.MIPDokuInquiryResponse;
//import com.onecheckoutV1.ejb.helper.MIPDokuInquiryResponse.DokuPaymentChannel;
//import com.onecheckoutV1.ejb.helper.MIPDokuInquiryResponse.PromotionChannel;
//import com.onecheckoutV1.ejb.helper.MIPDokuPaymentResponse;
import com.onecheckoutV1.ejb.helper.MIPInquiryResponse;
import com.onecheckoutV1.ejb.helper.MIPPaymentResponse;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.AESTools;
import com.onecheckoutV1.ejb.util.EDSAdditionalInformation;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckOutBatchType;
import com.onecheckoutV1.type.OneCheckOutSettlementStatus;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMerchantCategory;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutReturnType;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Batch;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.SeqBatchIdNextval;
import com.onechekoutv1.dto.Transactions;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//import java.lang.reflect.Type;
//import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author hafiz
 */
@Stateless
public class OneCheckoutV1DokupayDIRECTCOREBean extends OneCheckoutChannelBase implements OneCheckoutV1DokupayDIRECTCOREBeanLocal {

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
    private String aesKey = "masMu45%4r&*uy^5";

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper oneCheckoutDataHelper, PaymentChannel paymentChannel) {
        Transactions transactions = null;
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));
            boolean needNotify = false;
            String paramString = "";
            OneCheckoutErrorMessage oneCheckoutErrorMessage = OneCheckoutErrorMessage.UNKNOWN;

//          MODIFIED VERSION
//            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment ", oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL());
            String url = "";
//          PREVIOUS VERSION
            if (oneCheckoutDataHelper.getPaymentPageObject() != null
                    && oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse() != null
                    && oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse().getResponseCode() != null
                    && oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse().getResponseCode().trim().equals("0000")) {

//          MODIFIED VERSION            
//            if (oneCheckoutDataHelper.getPaymentPageObject() != null &&
//                    oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse() != null &&
//                    oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse().getResponseCode() != null &&
//                    oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse().getResponseCode().trim().equals("0000")) {
                transactions = oneCheckoutDataHelper.getTransactions();
                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment incoming from OCO");
                OneCheckoutDataPGRedirect datatoCore = null;
                if (oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL() != null && oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL().equals("01")) {
                    // ===========================
                    //    C A S H   W A L L E T
                    // ===========================
                    transactions.setEduReason("CASHWALLET");
                    url = paymentChannel.getRedirectPaymentUrlMipXml();

//                  PREVIOUS VERSION
                    datatoCore = this.createPaymentCashWalletRequest(oneCheckoutDataHelper.getPaymentRequest(), transactions.getMerchantPaymentChannel(), oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse(), oneCheckoutDataHelper.getOcoId());
//                  MODIFIED VERSION                         
//                    datatoCore = this.createPaymentCashWalletRequest(oneCheckoutDataHelper.getPaymentRequest(), transactions.getMerchantPaymentChannel(), oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse());

                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - T1: %d Prepare Payment Request", (System.currentTimeMillis() - t1));
                    paramString = super.createParamsHTTP(datatoCore.getParameters());
                    String paymentResponse = "";
                    try {
                        paymentResponse = super.postMIP(paramString, url, paymentChannel);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - T2: %d Get Payment Response", (System.currentTimeMillis() - t1));
                    OneCheckoutLogger.log("PaymentResponse => " + paymentResponse);

//                  PREVIOUS VERSION
                    MIPPaymentResponse mIPPaymentResponse = null;
//                  MODIFIED VERSION
//                  MIPDokuPaymentResponse mIPDokuPaymentResponse = null;

                    try {
                        if (paymentResponse != null && !paymentResponse.trim().equals("")) {

//                            PREVIOUS VERSION
                            ObjectMapper mapper = new ObjectMapper();
                            mIPPaymentResponse = mapper.readValue(paymentResponse, MIPPaymentResponse.class);
//                            MODIFIED VERSION 
//                              mIPDokuPaymentResponse = getAllMipDokuPayment(paymentResponse);
                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }

//                  PREVIOUS VERSION
                    String status = mIPPaymentResponse.getStatus();
//                  MODIFIED VERSION                     
//                    String status = mIPDokuPaymentResponse.getStatus();

                    transactions.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    if (status != null && status.trim().equalsIgnoreCase("Success")) {
                        transactions.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                    }

//                  PREVIOUS VERSION
                    String responseCode = mIPPaymentResponse.getResponseCode();
                    String approvalCode = mIPPaymentResponse.getApprovalCode();
                    String issuerBankName = mIPPaymentResponse.getBank();
//                  MODIFIED VERSION  
//                    String responseCode = mIPDokuPaymentResponse.getResponseCode();
//                    String approvalCode = mIPDokuPaymentResponse.getApprovalCode();                      
//                    String issuerBankName = mIPDokuPaymentResponse.getIssuerBankName();

                    if (responseCode != null && responseCode.length() != 4) {
                        responseCode = "00" + responseCode;
                    }
                    transactions.setDokuApprovalCode(approvalCode);
                    transactions.setDokuIssuerBank(issuerBankName);
                    transactions.setDokuResponseCode(responseCode);
                    transactions.setVerifyId("");
                    transactions.setVerifyScore(-1);
                    transactions.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    transactions.setDokuInvokeStatusDatetime(new Date());
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                    HashMap<String, String> params = super.getData(transactions);
                    params.put("PAYMENTCODE", "");
                    String resp = super.notifyStatusMerchant(transactions, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doNotifyPayment - %s", resp);
                    if (status != null && status.trim().equalsIgnoreCase("Success") && transactions.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
//                      PREVIOUS VERSION
                        super.reversalToCOREDOKUWallet(oneCheckoutDataHelper.getPaymentRequest(), transactions, oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse());
//                     MODIFIED VERSION                      
//                        super.reversalToCOREDOKUWallet(oneCheckoutDataHelper.getPaymentRequest(), transactions,oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse());

                    }
                    transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    em.merge(transactions);
                } else if (oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL() != null && oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL().equals("02")) {
                    // ===========================
                    //    C R E D I T   C A R D
                    // ===========================
                    MerchantPaymentChannel merchantPaymentChannel = queryHelper.getMerchantPaymentChannel(oneCheckoutDataHelper.getMerchant(), OneCheckoutPaymentChannel.CreditCard);
                    if (merchantPaymentChannel != null) {
                        // ===========
                        //    I P G
                        // ===========
                        MerchantPaymentChannel dokuWalletMerchantPaymentChannel = transactions.getMerchantPaymentChannel();
                        transactions.setMerchantPaymentChannel(merchantPaymentChannel);
                        url = merchantPaymentChannel.getPaymentChannel().getRedirectPaymentUrlMipXml();
                        OneCheckoutErrorMessage errormsg = OneCheckoutErrorMessage.UNKNOWN;
                        if (oneCheckoutDataHelper.getMessage().equalsIgnoreCase("ACS")) {
                            transactions = oneCheckoutDataHelper.getTransactions();
                            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment incoming from ACS");
                            oneCheckoutDataHelper.getPaymentRequest().getAllAdditionData().put("FROM", "ACS");
//                            String paymentChannelId = merchantPaymentChannel.getPaymentChannel().getPaymentChannelId();
                            //String ocoId = this.generateOcoId(paymentChannelId);
                            oneCheckoutDataHelper.getPaymentRequest().getAllAdditionData().put("OCOID", oneCheckoutDataHelper.getOcoId());
                            paramString = super.createParamsHTTP(oneCheckoutDataHelper.getPaymentRequest().getAllAdditionData());
                            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment DATA => %s", paramString);
                        } else {
//                          PREVIOUS VERSION
                            datatoCore = this.createPaymentIPGRequest(oneCheckoutDataHelper.getPaymentRequest(), merchantPaymentChannel, oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse(), oneCheckoutDataHelper.getOcoId());
//                          MODIFIED VERSION
//                            datatoCore = this.createPaymentIPGRequest(oneCheckoutDataHelper.getPaymentRequest(), merchantPaymentChannel, oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse());
                            HashMap<String, String> params = datatoCore.getParameters();
                            paramString = super.createParamsHTTP(params);
                        }

                        boolean checkStatusToCore = false;
                        String resultPayment = "";
                        try {
                            resultPayment = super.postMIP(paramString, url, merchantPaymentChannel.getPaymentChannel());
                            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - RESULT[" + resultPayment + "]");
                            if (resultPayment == null || resultPayment.trim().equals("") || resultPayment.trim().equalsIgnoreCase("ERROR")) {
                                checkStatusToCore = true;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            checkStatusToCore = true;
                        }
                        if (checkStatusToCore) {
                            // CHECK STATUS TO CORE
                            Thread.sleep(5 * 1000);
                            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - Checking status to CORE", (System.currentTimeMillis() - t1));
                            transactions = super.CheckStatusCOREIPG(transactions, merchantPaymentChannel.getPaymentChannel());
                            if (transactions.getDokuResponseCode() != null && transactions.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value())) {
                                transactions = super.reversalToCOREIPG(transactions);
                                transactions.setDokuResponseCode(OneCheckoutErrorMessage.TRANSACTION_CAN_NOT_BE_PROCCED.value());
                            }
                            needNotify = true;

                            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                            HashMap<String, String> params = super.getData(transactions);
                            params.put("PAYMENTCODE", "");
                            transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                            String resp = super.notifyStatusMerchant(transactions, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

                            if (transactions.getDokuResponseCode() != null && transactions.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.NOTIFY_FAILED.value()) && transactions.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                                super.reversalToCOREIPG(transactions);
                            }
                            transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            em.merge(transactions);

                            // NOTIFY TO DOKU WALLET
                            notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                            oneCheckoutDataHelper.setTransactions(transactions);
                            oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                            oneCheckoutDataHelper.setMessage("VALID");
                            return oneCheckoutDataHelper;
                        }

                        XMLConfiguration xml = new XMLConfiguration();
                        try {
                            StringReader sr = new StringReader(resultPayment);
                            xml.load(sr);
                        } catch (Exception ex) {
                            needNotify = true;
                            errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                            transactions.setDokuResponseCode(errormsg.value());
                            transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            transactions.setDokuInvokeStatusDatetime(new Date());
                            transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                            em.merge(transactions);
                            super.reversalToCOREIPG(transactions);

                            // NOTIFY TO DOKU WALLET
                            notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                            oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                            oneCheckoutDataHelper.setMessage("VALID");
                            return oneCheckoutDataHelper;
                        }
                        String statusResp = xml.getString("status");
                        if (statusResp != null && (statusResp.equalsIgnoreCase("THREEDSECURE") || statusResp.equalsIgnoreCase("3DSECURE"))) {
                            try {
                                OneCheckoutDataPGRedirect redirect = this.parsingIPG3DSecureData(xml);
                                oneCheckoutDataHelper.setRedirect(redirect);//.setPayResponse(paymentResp);
                                transactions.setInc3dSecureStatus(redirect.getParameters().get("MD"));
                                transactions.setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());
                                transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                                em.merge(transactions);
                                return oneCheckoutDataHelper;
                            } catch (Exception ex) {
                                needNotify = true;
                                errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                                transactions.setDokuResponseCode(errormsg.value());
                                transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                                transactions.setDokuInvokeStatusDatetime(new Date());
                                transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                                em.merge(transactions);

                                // NOTIFY TO DOKU WALLET
                                notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                                oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                                oneCheckoutDataHelper.setMessage("VALID");
                                return oneCheckoutDataHelper;
                            }
                        } else {
                            transactions = this.parseIPGXMLPayment(transactions, xml);
                            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                            HashMap<String, String> params = super.getData(transactions);
                            params.put("PAYMENTCODE", "");
                            transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                            String resp = super.notifyStatusMerchant(transactions, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

                            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - %s", resp);
                            if (statusResp != null && statusResp.trim().equalsIgnoreCase("Success") && transactions.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                                super.reversalToCOREIPG(transactions);
                            }
                            transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            em.merge(transactions);

                            // NOTIFY TO DOKU WALLET
                            notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                            OneCheckoutRedirectData redirectRequest = null;
                            oneCheckoutDataHelper.setRedirectDoku(redirectRequest);
                            oneCheckoutDataHelper.setTransactions(transactions);
                            oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                            oneCheckoutDataHelper.setMessage("VALID");
                            return oneCheckoutDataHelper;
                        }
                    } else {
                        // ===========
                        //    M P G
                        // ===========
                        merchantPaymentChannel = queryHelper.getMerchantPaymentChannel(oneCheckoutDataHelper.getMerchant(), OneCheckoutPaymentChannel.BSP);
                        if (merchantPaymentChannel != null) {
                            MerchantPaymentChannel dokuWalletMerchantPaymentChannel = transactions.getMerchantPaymentChannel();
                            transactions.setMerchantPaymentChannel(merchantPaymentChannel);
                            OneCheckoutErrorMessage errormsg = OneCheckoutErrorMessage.UNKNOWN;
                            String CoreUrl = this.getCoreMPGMipURL(merchantPaymentChannel.getPaymentChannel(), "DEFAULT");//, CoreUrl)pChannel.getRedirectPaymentUrlMipXml();
                            if (oneCheckoutDataHelper.getMessage().equalsIgnoreCase("ACS")) {
                                transactions = oneCheckoutDataHelper.getTransactions();
                                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment incoming from ACS");
                                oneCheckoutDataHelper.getPaymentRequest().getAllAdditionData().put("FROM", "ACS");
                                String paymentChannelId = merchantPaymentChannel.getPaymentChannel().getPaymentChannelId();
                                //String ocoId = this.generateOcoId(paymentChannelId);
                                oneCheckoutDataHelper.getPaymentRequest().getAllAdditionData().put("SERVICETRANSACTIONID", oneCheckoutDataHelper.getOcoId());
                                paramString = super.createParamsHTTP(oneCheckoutDataHelper.getPaymentRequest().getAllAdditionData());
                                CoreUrl = this.getCoreMPGMipURL(merchantPaymentChannel.getPaymentChannel(), "ACS");//pChannel.getRedirectPaymentUrlMip();
                                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment DATA => %s", paramString);
                            } else {

//                              PREVIOUS VERSION
                                datatoCore = this.createPaymentMPGRequest(oneCheckoutDataHelper.getPaymentRequest(), merchantPaymentChannel, oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse(), oneCheckoutDataHelper.getOcoId());
//                              MODIFIED VERSION
//                                datatoCore = this.createPaymentMPGRequest(oneCheckoutDataHelper.getPaymentRequest(), merchantPaymentChannel,oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse());
                                HashMap<String, String> params = datatoCore.getParameters();
                                paramString = super.createParamsHTTP(params);

                            }

                            boolean checkStatusToCore = false;
                            String resultPayment = "";
                            try {
                                resultPayment = super.postMIP(paramString, CoreUrl, merchantPaymentChannel.getPaymentChannel());
                                if (resultPayment == null || resultPayment.trim().equals("") || resultPayment.trim().equalsIgnoreCase("ERROR")) {
                                    checkStatusToCore = true;
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                checkStatusToCore = true;
                            }
                            if (checkStatusToCore) {
                                // CHECK STATUS TO CORE
                                Thread.sleep(5 * 1000);
                                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - Checking status to CORE", (System.currentTimeMillis() - t1));
                                transactions = super.CheckStatusCOREMPG(transactions, merchantPaymentChannel.getPaymentChannel());
                                if (transactions.getDokuResponseCode() != null && transactions.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value())) {
                                    transactions = super.reversalToCOREMPG(transactions);
                                    transactions.setDokuResponseCode(OneCheckoutErrorMessage.TRANSACTION_CAN_NOT_BE_PROCCED.value());
                                }
                                needNotify = true;
                                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                                HashMap<String, String> params = super.getData(transactions);
                                params.put("PAYMENTCODE", "");
                                transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                                String resp = super.notifyStatusMerchant(transactions, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

                                if (transactions.getDokuResponseCode() != null && transactions.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.NOTIFY_FAILED.value()) && transactions.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                                    super.reversalToCOREMPG(transactions);
                                }
                                transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                                em.merge(transactions);

                                // NOTIFY TO DOKU WALLET
                                notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                                oneCheckoutDataHelper.setTransactions(transactions);
                                oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                                oneCheckoutDataHelper.setMessage("VALID");
                                return oneCheckoutDataHelper;
                            }

                            XMLConfiguration xml = new XMLConfiguration();
                            try {
                                StringReader sr = new StringReader(resultPayment);
                                xml.load(sr);
                            } catch (Exception ex) {
                                needNotify = true;
                                errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                                transactions.setDokuResponseCode(errormsg.value());
                                transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                                transactions.setDokuInvokeStatusDatetime(new Date());
                                transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                                em.merge(transactions);
                                transactions = super.reversalToCOREMPG(transactions);

                                // NOTIFY TO DOKU WALLET
                                notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                                oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                                oneCheckoutDataHelper.setMessage("VALID");
                                return oneCheckoutDataHelper;
                            }

                            String statusResp = xml.getString("result");
                            if (statusResp != null && (statusResp.equalsIgnoreCase("THREEDSECURE") || statusResp.equalsIgnoreCase("3DSECURE"))) {
                                try {
                                    OneCheckoutDataPGRedirect redirect = this.parsingMPG3DSecureData(xml);
                                    oneCheckoutDataHelper.setRedirect(redirect);//.setPayResponse(paymentResp);
                                    transactions.setInc3dSecureStatus(redirect.getParameters().get("MD"));
                                    transactions.setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());
                                    transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                                    em.merge(transactions);
                                    return oneCheckoutDataHelper;
                                } catch (Exception ex) {
                                    needNotify = true;
                                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                                    transactions.setDokuResponseCode(errormsg.value());
                                    transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                                    transactions.setDokuInvokeStatusDatetime(new Date());
                                    transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                                    em.merge(transactions);

                                    // NOTIFY TO DOKU WALLET
                                    notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                                    oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                                    oneCheckoutDataHelper.setMessage("VALID");
                                    return oneCheckoutDataHelper;
                                }
                            } else {
                                transactions = this.parseMPGXMLPayment(transactions, xml, merchantPaymentChannel.getMerchants());
                                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                                HashMap<String, String> params = super.getData(transactions);

                                params.put("PAYMENTCODE", "");
                                String resp = super.notifyStatusMerchant(transactions, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

                                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - %s", resp);
                                if (statusResp != null && statusResp.trim().equalsIgnoreCase("Success") && transactions.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                                    super.reversalToCOREMPG(transactions);
                                }
                                transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                                transactions.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                                em.merge(transactions);

                                // NOTIFY TO DOKU WALLET
                                notifyCCPaymentToDOKUWallet(oneCheckoutDataHelper, transactions);

                                oneCheckoutDataHelper.setTransactions(transactions);
                                oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                                oneCheckoutDataHelper.setMessage("VALID");
                                return oneCheckoutDataHelper;
                            }
                        }
                    }
                } else if (oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL() != null && oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL().equals("03")) {
                    // ===========================
                    //    T - C A S H 
                    // ===========================
                    transactions.setEduReason("TCASH");
                    url = paymentChannel.getRedirectPaymentUrlMipXml();

//                  PREVIOUS VERSION
                    datatoCore = this.createPaymentTCashRequest(oneCheckoutDataHelper.getPaymentRequest(), transactions.getMerchantPaymentChannel(), oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse(), oneCheckoutDataHelper.getOcoId());
//                  MODIFIED VERSION
//                    datatoCore = this.createPaymentTCashRequest(oneCheckoutDataHelper.getPaymentRequest(), transactions.getMerchantPaymentChannel(), oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse());

                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - T1: %d Prepare Payment Request", (System.currentTimeMillis() - t1));
                    paramString = super.createParamsHTTP(datatoCore.getParameters());
                    String paymentResponse = "";
                    try {
                        paymentResponse = super.postMIP(paramString, url, paymentChannel);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - T2: %d Get Payment Response", (System.currentTimeMillis() - t1));
                    OneCheckoutLogger.log("PaymentResponse => " + paymentResponse);

//                  PREVIOUS VERSION
                    MIPPaymentResponse mIPPaymentResponse = null;
//                  MODIFIED VERSION
//                  MIPDokuPaymentResponse mIPDokuPaymentResponse = null;

                    try {
                        if (paymentResponse != null && !paymentResponse.trim().equals("")) {

//                          PREVIOUS VERSION
                            ObjectMapper mapper = new ObjectMapper();
                            mIPPaymentResponse = mapper.readValue(paymentResponse, MIPPaymentResponse.class);
//                          MODIFIED VERSION                            
//                          mIPDokuPaymentResponse = getAllMipDokuPayment(paymentResponse);

                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
//                  PREVIOUS VERSION
                    String status = mIPPaymentResponse.getStatus();
//                  MODIFIED VERSION
//                  String status = mIPDokuPaymentResponse.getStatus();

                    transactions.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    if (status != null && status.trim().equalsIgnoreCase("Success")) {
                        transactions.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                    }

//                  PREVIOUS VERSION
                    String responseCode = mIPPaymentResponse.getResponseCode();
                    String approvalCode = mIPPaymentResponse.getApprovalCode();
                    String issuerBankName = mIPPaymentResponse.getBank();
//                    MODIFIED VERSION
//                    String responseCode = mIPDokuPaymentResponse.getResponseCode();
//                    String approvalCode = mIPDokuPaymentResponse.getApprovalCode();
//                    String issuerBankName = mIPDokuPaymentResponse.getIssuerBankName();

                    if (responseCode != null && responseCode.length() != 4) {
                        responseCode = "00" + responseCode;
                    }

                    transactions.setDokuApprovalCode(approvalCode);
                    transactions.setDokuIssuerBank(issuerBankName);
                    transactions.setDokuResponseCode(responseCode);
                    transactions.setVerifyId("");
                    transactions.setVerifyScore(-1);
                    transactions.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    transactions.setDokuInvokeStatusDatetime(new Date());
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                    HashMap<String, String> params = super.getData(transactions);
                    params.put("PAYMENTCODE", "");
                    String resp = super.notifyStatusMerchant(transactions, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doNotifyPayment - %s", resp);
                    if (status != null && status.trim().equalsIgnoreCase("Success") && transactions.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
//                      PREVIOUS VERSION                        
                        super.reversalToCOREDOKUWallet(oneCheckoutDataHelper.getPaymentRequest(), transactions, oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse());
//                        MODIFIED VERSION
//                        super.reversalToCOREDOKUWallet(oneCheckoutDataHelper.getPaymentRequest(), transactions, oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse());
                    }
                    transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    em.merge(transactions);
                }

                OneCheckoutRedirectData redirectRequest = null;
                oneCheckoutDataHelper.setRedirectDoku(redirectRequest);
                oneCheckoutDataHelper.setTransactions(transactions);
                oneCheckoutDataHelper = this.doShowResultPage(oneCheckoutDataHelper, paymentChannel);
                oneCheckoutDataHelper.setMessage("VALID");
                return oneCheckoutDataHelper;
            } else {
                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment incoming from Merchant...");
                IdentifyTrx identrx = super.getTransactionInfo(oneCheckoutDataHelper.getPaymentRequest(), paymentChannel, oneCheckoutDataHelper.getMerchant());
                boolean request_good = identrx.isRequestGood();
                MerchantPaymentChannel merchantPaymentChannel = identrx.getMpc();
                oneCheckoutErrorMessage = identrx.getErrorMsg();
                needNotify = identrx.isNeedNotify();
                if (!request_good) {
                    oneCheckoutDataHelper = super.createRedirectAndNotifyCaseFail(oneCheckoutDataHelper, oneCheckoutErrorMessage, needNotify, identrx.getTrans());
                    return oneCheckoutDataHelper;
                }
                if (oneCheckoutDataHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment Save Transaction...");
                    transactions = transacBean.saveTransactions(oneCheckoutDataHelper, merchantPaymentChannel);
                } else {
                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment Update Transaction...");
                    transactions = transacBean.updateTransactions(oneCheckoutDataHelper, merchantPaymentChannel);
                }
                url = paymentChannel.getRedirectPaymentUrlMip();
                OneCheckoutDataPGRedirect datatoCore = this.createInquiryRequest(oneCheckoutDataHelper.getPaymentRequest(), paymentChannel, merchantPaymentChannel, oneCheckoutDataHelper.getMerchant(), oneCheckoutDataHelper.getOcoId());
                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - T1: %d Prepare Inquiry Request", (System.currentTimeMillis() - t1));
                paramString = super.createParamsHTTP(datatoCore.getParameters());
                String inquiryResponse = "";
                try {
                    inquiryResponse = super.postMIP(paramString, url, paymentChannel);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doPayment - T2: %d Get Inquiry Response", (System.currentTimeMillis() - t1));
                OneCheckoutLogger.log("InquiryResponse => " + inquiryResponse);

//              PREVIOUS VERSION
                MIPInquiryResponse mIPInquiryResponse = null;
//              MODIFIED VERSION
//              MIPDokuInquiryResponse mIPDokuInquiryResponse = null;

                try {
                    if (inquiryResponse != null && !inquiryResponse.trim().equals("")) {

//                      PREVIOUS VERSION
                        ObjectMapper mapper = new ObjectMapper();
                        mIPInquiryResponse = mapper.readValue(inquiryResponse, MIPInquiryResponse.class);
                        if (mIPInquiryResponse != null && mIPInquiryResponse.getResponseCode().equals("0000")) {
                            oneCheckoutDataHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                            Boolean status = pluginExecutor.validationMerchantPlugins(oneCheckoutDataHelper);
                        }

//                      MODIFIED VERSION
//                          mIPDokuInquiryResponse = getAllMIPDokuInquiryResponse(inquiryResponse,oneCheckoutDataHelper);
//                          if(mIPDokuInquiryResponse != null && mIPDokuInquiryResponse.getResponseCode().equals("0000")){
//                              oneCheckoutDataHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
//                              Boolean status = pluginExecutor.validationMerchantPlugins(oneCheckoutDataHelper);
//                          }
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                }
                oneCheckoutDataHelper.setTransactions(transactions);

//              PREVIOUS VERSION
                oneCheckoutDataHelper.getPaymentPageObject().setMIPInquiryResponse(mIPInquiryResponse);
//              MODIFIED VERSION                
//              oneCheckoutDataHelper.getPaymentPageObject().setmIPDokuInquiryResponse(mIPDokuInquiryResponse);
            }
            return oneCheckoutDataHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            oneCheckoutDataHelper.setMessage(th.getMessage());
            return oneCheckoutDataHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    private void notifyCCPaymentToDOKUWallet(OneCheckoutDataHelper oneCheckoutDataHelper, Transactions transaction) {
        // NOTIFY TO DOKU WALLET
        String url = transaction.getMerchantPaymentChannel().getPaymentChannel().getRedirectPaymentUrlMipXml();

//              PREVIOUS VERSION
        OneCheckoutDataPGRedirect datatoCore = this.createPaymentCCWalletRequest(oneCheckoutDataHelper.getPaymentRequest(), oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse(), transaction);
//              MODIFIED VERSION       
//        OneCheckoutDataPGRedirect datatoCore = this.createPaymentCCWalletRequest(oneCheckoutDataHelper.getPaymentRequest(), transaction,oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse());

        String paramString = super.createParamsHTTP(datatoCore.getParameters());
        String paymentResponse = "";
        try {
            paymentResponse = super.postMIP(paramString, url, transaction.getMerchantPaymentChannel().getPaymentChannel());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        OneCheckoutLogger.log("Notify Payment To DOKU Wallet Response => " + paymentResponse);

//      PREVIOUS VERSION
        MIPPaymentResponse mIPPaymentResponse = null;
//        MODIFIED VERSION
//        MIPDokuPaymentResponse mIPDokuPaymentResponse = null;

        try {
            if (paymentResponse != null && !paymentResponse.trim().equals("")) {

//            PREVIOUS VERSION
                ObjectMapper mapper = new ObjectMapper();
                mIPPaymentResponse = mapper.readValue(paymentResponse, MIPPaymentResponse.class);
//            MODIFIED VERSION                
//            mIPDokuPaymentResponse = getAllMipDokuPayment(paymentResponse);

            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

//        PREVIOUS VERSION
        String status = mIPPaymentResponse.getStatus();
//        MODIFIED VERSION           
//        String status = mIPDokuPaymentResponse.getStatus();

    }

    private String getCoreMPGMipURL(PaymentChannel paymentChannel, String step) {
        String CoreUrl = paymentChannel.getRedirectPaymentUrlMipXml();
        if (step.equalsIgnoreCase("ACS")) {
            CoreUrl = paymentChannel.getRedirectPaymentUrlMip();
        }
        return CoreUrl;
    }

    private OneCheckoutDataPGRedirect createInquiryRequest(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        try {
            OneCheckoutDataPGRedirect inquiryRequest = new OneCheckoutDataPGRedirect();
            inquiryRequest.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            inquiryRequest.setAMOUNT(paymentRequest.getAMOUNT());
            inquiryRequest.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            inquiryRequest.getParameters().put("MALLID", mpc.getPaymentChannelCode() + "");
            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                inquiryRequest.getParameters().put("CHAINNUM", "NA");
            } else {
                inquiryRequest.getParameters().put("CHAINNUM", mpc.getPaymentChannelChainCode() + "");
            }
            inquiryRequest.getParameters().put("DPMALLID", mpc.getMerchantPaymentChannelPid() + "");
            inquiryRequest.getParameters().put("LOGINID", paymentRequest.getDOKUPAYID());
            inquiryRequest.getParameters().put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            inquiryRequest.getParameters().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            inquiryRequest.getParameters().put("BASKET", paymentRequest.getBASKET());
            inquiryRequest.getParameters().put("SESSIONID", paymentRequest.getSESSIONID());
            inquiryRequest.getParameters().put("REQUESTDATETIME", OneCheckoutVerifyFormatData.datetimeFormat.format(new Date()));

            StringBuilder word = new StringBuilder();
            word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            word.append(mpc.getMerchantPaymentChannelPid());
            word.append(mpc.getMerchantPaymentChannelHash());
            word.append(paymentRequest.getTRANSIDMERCHANT());
            OneCheckoutLogger.log("COMPONENT_WORDS_TO_DOKUWALLET[" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            OneCheckoutLogger.log("WORDS_TO_DOKUWALLET[" + hashwords + "]");
            inquiryRequest.getParameters().put("WORDS", hashwords);
            if (m.getMerchantCategory() != OneCheckoutMerchantCategory.NONAIRLINE.value()) {
                inquiryRequest.getParameters().put("ADDITIONALINFO", EDSAdditionalInformation.getAdditionalInfoForEdu(paymentRequest));
            } else {
                inquiryRequest.getParameters().put("ADDITIONALINFO", "");
            }
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            inquiryRequest.getParameters().put("PASSWORD", base.maskingString(paymentRequest.getSECURITYCODE(), null));
            OneCheckoutLogger.log("INQUIRY to DOKUPAY => " + inquiryRequest.getParameters().toString());
            inquiryRequest.getParameters().put("PASSWORD", paymentRequest.getSECURITYCODE());
            String paymentChannelId = pChannel.getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            inquiryRequest.getParameters().put("OCOID", ocoId);
            return inquiryRequest;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }
//    PREVIOUS VERSION    

    private OneCheckoutDataPGRedirect createPaymentCashWalletRequest(OneCheckoutPaymentRequest paymentRequest, MerchantPaymentChannel mpc, MIPInquiryResponse mIPInquiryResponse, String ocoId) {
//    MODIFIED VERSION
//    private OneCheckoutDataPGRedirect createPaymentCashWalletRequest(OneCheckoutPaymentRequest paymentRequest, MerchantPaymentChannel mpc, MIPDokuInquiryResponse mIPDokuInquiryResponse) {

        try {
            /*
             DPMALLID
             PAYMENTCHANNELCODE
             INQUIRYCODE
             TRANSIDMERCHANT
             AMOUNT
             SESSIONID
             DOKUID
             WORDS
             PIN
             CALLBACKDATA
             */
            String pin = paymentRequest.getCASHWALLETPIN() != null ? paymentRequest.getCASHWALLETPIN() : "";
            if (!pin.equals("")) {
                pin = AESTools.encryptInputKey(pin, aesKey);
            }
            OneCheckoutDataPGRedirect paymentCashWalletRequest = new OneCheckoutDataPGRedirect();
            paymentCashWalletRequest.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            paymentCashWalletRequest.getParameters().put("DPMALLID", mpc.getMerchantPaymentChannelPid() + "");
            paymentCashWalletRequest.getParameters().put("PAYMENTCHANNELCODE", paymentRequest.getDOKUWALLETCHANNEL());

//          PREVIOUS VERSION            
            paymentCashWalletRequest.getParameters().put("INQUIRYCODE", mIPInquiryResponse.getInquiryCode());
//          MODIFIED VERSION                   
//            paymentCashWalletRequest.getParameters().put("INQUIRYCODE", mIPDokuInquiryResponse.getInquiryCode());

            paymentCashWalletRequest.getParameters().put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            paymentCashWalletRequest.getParameters().put("SESSIONID", paymentRequest.getSESSIONID());

//          PREVIOUS VERSION
            paymentCashWalletRequest.getParameters().put("DOKUID", mIPInquiryResponse.getDokuId());
//          MODIFIED VERSION                   
//            paymentCashWalletRequest.getParameters().put("DOKUID", mIPDokuInquiryResponse.getDokuId());

            paymentCashWalletRequest.getParameters().put("PIN", pin);

//          MODIFIED VERSION            
//            String strPromotionId = paymentRequest.getPROMOTIONID() != null ? paymentRequest.getPROMOTIONID() : null;
//            String strFlagDoku = paymentRequest.getFLAGDOKU() != null ? paymentRequest.getFLAGDOKU() : null;
//          PREVIOUS VERSION            
            paymentCashWalletRequest.getParameters().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            paymentCashWalletRequest.getParameters().put("BASKET", paymentRequest.getBASKET());
            String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            paymentCashWalletRequest.getParameters().put("OCOID", ocoId);
            //add param Basket to payment wallet 

//          MODIFIED VERSION              
//            BigDecimal bdVal = BigDecimal.ZERO;
//            BigDecimal bdValMeasure = BigDecimal.ONE;
//            
//            if (strPromotionId != null && !strPromotionId.equals("") && strFlagDoku != null) {
//                paymentCashWalletRequest.getParameters().put("PROMOTIONID", strPromotionId);
//                for (PromotionChannel lstmIPDokuInquiryResponse : mIPDokuInquiryResponse.getListPromotion()) {
//                    OneCheckoutLogger.log("PROMOTION FLAG => " + strFlagDoku + " PROMOTION CHANNEL ID => " + lstmIPDokuInquiryResponse.getId());
//                    if (lstmIPDokuInquiryResponse.getId().equals(strPromotionId)) {
//                        bdVal = BigDecimal.valueOf(paymentRequest.getAMOUNT());
//                        bdVal = bdVal.subtract(lstmIPDokuInquiryResponse.getAmount());
//                        paymentCashWalletRequest.getParameters().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(bdVal));
//                    }
//                }
//            } else {
//                paymentCashWalletRequest.getParameters().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
//                bdValMeasure = BigDecimal.ZERO;
//            }
            StringBuilder word = new StringBuilder();

//          MODIFIED VERSION    
//            if (bdValMeasure.equals(BigDecimal.ZERO)) {
            word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            //          MODIFIED VERSION                  
//            } else {
//                word.append(OneCheckoutVerifyFormatData.sdf.format(bdVal));
//            }
            word.append(mpc.getMerchantPaymentChannelPid() + "");
            word.append(mpc.getMerchantPaymentChannelHash());

//          PREVIOUS VERSION            
            word.append(mIPInquiryResponse.getInquiryCode());
//          MODIFIED VERSION                
//          word.append(mIPDokuInquiryResponse.getInquiryCode());

            OneCheckoutLogger.log("COMPONENT_WORDS_TO_DOKUWALLET[" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            OneCheckoutLogger.log("WORDS_TO_DOKUWALLET[" + hashwords + "]");
            paymentCashWalletRequest.getParameters().put("WORDS", hashwords);
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            OneCheckoutLogger.log("PAYMENT CASH WALLET to DOKUPAY => " + paymentCashWalletRequest.getParameters().toString());
            return paymentCashWalletRequest;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

//  PREVIOUS VERSION
    private OneCheckoutDataPGRedirect createPaymentTCashRequest(OneCheckoutPaymentRequest paymentRequest, MerchantPaymentChannel mpc, MIPInquiryResponse mIPInquiryResponse, String ocoId) {
//  MODIFIED VERSION      
//    private OneCheckoutDataPGRedirect createPaymentTCashRequest(OneCheckoutPaymentRequest paymentRequest, MerchantPaymentChannel mpc,MIPDokuInquiryResponse mIPDokuInquiryResponse) {

        try {
            /*
             DPMALLID
             PAYMENTCHANNELCODE
             INQUIRYCODE
             TRANSIDMERCHANT
             AMOUNT
             SESSIONID
             DOKUID
             WORDS
             TOKEN
             LINKID
             ACCOUNTID
             REMARKS
             CALLBACKDATA
             */
            OneCheckoutDataPGRedirect paymentCashWalletRequest = new OneCheckoutDataPGRedirect();
            paymentCashWalletRequest.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            paymentCashWalletRequest.getParameters().put("DPMALLID", mpc.getMerchantPaymentChannelPid() + "");
            paymentCashWalletRequest.getParameters().put("PAYMENTCHANNELCODE", paymentRequest.getDOKUWALLETCHANNEL());

//          PREVIOUS VERSION            
            paymentCashWalletRequest.getParameters().put("INQUIRYCODE", mIPInquiryResponse.getInquiryCode());
//          MODIFIED VERSION            
//            paymentCashWalletRequest.getParameters().put("INQUIRYCODE",mIPDokuInquiryResponse.getInquiryCode());

            paymentCashWalletRequest.getParameters().put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            paymentCashWalletRequest.getParameters().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            paymentCashWalletRequest.getParameters().put("SESSIONID", paymentRequest.getSESSIONID());

//          PREVIOUS VERSION            
            paymentCashWalletRequest.getParameters().put("DOKUID", mIPInquiryResponse.getDokuId());
//          MODIFIED VERSION               
//          paymentCashWalletRequest.getParameters().put("DOKUID", mIPDokuInquiryResponse.getDokuId());
            paymentCashWalletRequest.getParameters().put("LINKID", paymentRequest.getDWTCASHACCOUNTNUMBER());
            paymentCashWalletRequest.getParameters().put("TOKEN", AESTools.encryptInputKey(paymentRequest.getDWTCASHTOKENNUMBER(), aesKey));
            String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            paymentCashWalletRequest.getParameters().put("OCOID", ocoId);
//            String strPromotionId = paymentRequest.getPROMOTIONID() != null ? paymentRequest.getPROMOTIONID() : null;
//            if(strPromotionId != null){
//                paymentCashWalletRequest.getParameters().put("PROMOTIONID",strPromotionId);
//            }

            StringBuilder word = new StringBuilder();
            word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            word.append(mpc.getMerchantPaymentChannelPid() + "");
            word.append(mpc.getMerchantPaymentChannelHash());

//          PREVIOUS VERSION            
            word.append(mIPInquiryResponse.getInquiryCode());
//          MODIFIED VERSION               
//            word.append(mIPDokuInquiryResponse.getInquiryCode());

            OneCheckoutLogger.log("COMPONENT_WORDS_TO_DOKUWALLET[" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            OneCheckoutLogger.log("WORDS_TO_DOKUWALLET[" + hashwords + "]");
            paymentCashWalletRequest.getParameters().put("WORDS", hashwords);
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            OneCheckoutLogger.log("PAYMENT T-CASH to DOKUPAY => " + paymentCashWalletRequest.getParameters().toString());
            return paymentCashWalletRequest;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

//  PREVIOUS VERSION
    private OneCheckoutDataPGRedirect createPaymentCCWalletRequest(OneCheckoutPaymentRequest paymentRequest, MIPInquiryResponse mIPInquiryResponse, Transactions transactions) {
//  MODIFIED VERSION    
//  private OneCheckoutDataPGRedirect createPaymentCCWalletRequest(OneCheckoutPaymentRequest paymentRequest, Transactions transactions,MIPDokuInquiryResponse mIPDokuInquiryResponse) {
        try {
            /*
             DPMALLID
             PAYMENTCHANNELCODE
             INQUIRYCODE
             TRANSIDMERCHANT
             AMOUNT
             SESSIONID
             DOKUID
             *
             RESPONSECODE
             RESULT
             BANK
             APPROVALCODE
             VERIFYID
             VERIFYSTATUS
             VERIFYSCORE
             WORDS
             LINKID
             CCNUMBER
             CCEXPIRYDATE
             CCNAME
             CCCOUNTRY
             CCPHONE
             CCEMAIL
             CCCITY
             CCZIPCODE
             */

            String responseCode = transactions.getDokuResponseCode() != null ? transactions.getDokuResponseCode().trim() : "";
            if (responseCode.length() == 2) {
            } else if (responseCode.length() == 4) {
                responseCode = responseCode.substring(2, 4);
            } else {
                responseCode = "99";
            }
            OneCheckoutDataPGRedirect paymentCashWalletRequest = new OneCheckoutDataPGRedirect();
            paymentCashWalletRequest.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            paymentCashWalletRequest.getParameters().put("DPMALLID", transactions.getMerchantPaymentChannel().getMerchantPaymentChannelPid() + "");
            paymentCashWalletRequest.getParameters().put("PAYMENTCHANNELCODE", paymentRequest.getDOKUWALLETCHANNEL());

//          PREVIOUS VERSION            
            paymentCashWalletRequest.getParameters().put("INQUIRYCODE", mIPInquiryResponse.getInquiryCode());
//          MODIFIED VERSION            
//            paymentCashWalletRequest.getParameters().put("INQUIRYCODE",mIPDokuInquiryResponse.getInquiryCode());

            paymentCashWalletRequest.getParameters().put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            paymentCashWalletRequest.getParameters().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            paymentCashWalletRequest.getParameters().put("SESSIONID", paymentRequest.getSESSIONID());

//          PREVIOUS VERSION            
            paymentCashWalletRequest.getParameters().put("DOKUID", mIPInquiryResponse.getDokuId());
//          MODIFIED VERSION               
//          paymentCashWalletRequest.getParameters().put("DOKUID",mIPDokuInquiryResponse.getDokuId());

            paymentCashWalletRequest.getParameters().put("LINKID", paymentRequest.getDWCREDITCARD());
            paymentCashWalletRequest.getParameters().put("BASKET", paymentRequest.getBASKET());
            paymentCashWalletRequest.getParameters().put("RESPONSECODE", responseCode);
            paymentCashWalletRequest.getParameters().put("RESULT", transactions.getTransactionsStatus() == 'S' ? "SUCCESS" : "FAILED");
            paymentCashWalletRequest.getParameters().put("BANK", transactions.getDokuIssuerBank() != null ? transactions.getDokuIssuerBank().trim() : "");
            paymentCashWalletRequest.getParameters().put("APPROVALCODE", transactions.getDokuApprovalCode() != null ? transactions.getDokuApprovalCode().trim() : "");
            paymentCashWalletRequest.getParameters().put("VERIFYID", transactions.getVerifyId() != null ? transactions.getVerifyId().trim() : "");
            paymentCashWalletRequest.getParameters().put("VERIFYSTATUS", transactions.getVerifyStatus() != null ? OneCheckoutDFSStatus.findType(transactions.getVerifyStatus()).name() : "");
            paymentCashWalletRequest.getParameters().put("VERIFYSCORE", transactions.getVerifyScore() + "");

            StringBuilder word = new StringBuilder();
            word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            word.append(transactions.getMerchantPaymentChannel().getMerchantPaymentChannelPid() + "");
            word.append(transactions.getMerchantPaymentChannel().getMerchantPaymentChannelHash());

//          PREVIOUS VERSION            
            word.append(mIPInquiryResponse.getInquiryCode());
//          MODIFIED VERSION            
//            word.append(mIPDokuInquiryResponse.getInquiryCode());

            OneCheckoutLogger.log("COMPONENT_WORDS_TO_DOKUWALLET[" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            OneCheckoutLogger.log("WORDS_TO_DOKUWALLET[" + hashwords + "]");
            paymentCashWalletRequest.getParameters().put("WORDS", hashwords);
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            OneCheckoutLogger.log("PAYMENT CC to DOKUPAY => " + paymentCashWalletRequest.getParameters().toString());
            return paymentCashWalletRequest;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

//  PREVIOUS VERSION
    private OneCheckoutDataPGRedirect createPaymentIPGRequest(OneCheckoutPaymentRequest oneCheckoutPaymentRequest, MerchantPaymentChannel mpc, MIPInquiryResponse mIPInquiryResponse, String ocoId) {
//  MODIFIED VERSION
//    private OneCheckoutDataPGRedirect createPaymentIPGRequest(OneCheckoutPaymentRequest oneCheckoutPaymentRequest, MerchantPaymentChannel mpc, MIPDokuInquiryResponse mIPDokuInquiryResponse) {

        try {
            OneCheckoutDataPGRedirect paymentIPGRequest = new OneCheckoutDataPGRedirect();
            paymentIPGRequest.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            paymentIPGRequest.getParameters().put("MerchantID", mpc.getMerchantPaymentChannelUid() + "");
            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                paymentIPGRequest.getParameters().put("CHAINNUM", "NA");
            } else {
                paymentIPGRequest.getParameters().put("CHAINNUM", mpc.getPaymentChannelChainCode() + "");
            }
            paymentIPGRequest.getParameters().put("TYPE", "IMMEDIATE");
            paymentIPGRequest.getParameters().put("OrderNumber", oneCheckoutPaymentRequest.getTRANSIDMERCHANT());
            paymentIPGRequest.getParameters().put("PurchaseAmt", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getAMOUNT()));
            paymentIPGRequest.getParameters().put("vat", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getVAT()));
            paymentIPGRequest.getParameters().put("insurance", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getINSURANCE()));
            paymentIPGRequest.getParameters().put("fuelSurcharge", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getFUELSURCHARGE()));
            paymentIPGRequest.getParameters().put("PurchaseCurrency", oneCheckoutPaymentRequest.getCURRENCY());
            paymentIPGRequest.getParameters().put("MerchantName", mpc.getMerchants().getMerchantName());
            paymentIPGRequest.getParameters().put("MALLID", mpc.getPaymentChannelCode() + "");
            paymentIPGRequest.getParameters().put("WORDS", super.generateDokuWords(oneCheckoutPaymentRequest, mpc));
            paymentIPGRequest.getParameters().put("SESSIONID", oneCheckoutPaymentRequest.getSESSIONID());
            paymentIPGRequest.getParameters().put("PurchaseDesc", oneCheckoutPaymentRequest.getBASKET());
            paymentIPGRequest.getParameters().put("ADDITIONALINFO", oneCheckoutPaymentRequest.getADDITIONALINFO());

            MIPInquiryResponse.DetailsOfCC cc = null;
//          MIPDokuInquiryResponse.DetailsOfCC cc = null;

//          PREVIOUS VERSION
            for (com.onecheckoutV1.ejb.helper.MIPInquiryResponse.PaymentChannel paymentChannel : mIPInquiryResponse.getListPaymentChannel()) {
//            MODIFIED VERSION
//            for (DokuPaymentChannel paymentChannel : mIPDokuInquiryResponse.getListPaymentChannel()) {
                if (paymentChannel.getChannelCode().equals("02") && paymentChannel.getDetails() != null) {
                    ArrayList<Map> lstDetils = (ArrayList<Map>) paymentChannel.getDetails();
                    for (Map hashMap : lstDetils) {
                        if (hashMap.get("linkId") != null && hashMap.get("linkId").toString().trim().equalsIgnoreCase(oneCheckoutPaymentRequest.getDWCREDITCARD())) {
//                            PREVIOUS VERSION
                            cc = new MIPInquiryResponse.DetailsOfCC();
//                            MODIFIED VERSION
//                            cc = new MIPDokuInquiryResponse.DetailsOfCC();
                            BeanUtils.populate(cc, hashMap);
                            break;
                        }
                    }
                }
            }

            paymentIPGRequest.getParameters().put("CardNumber", AESTools.decryptInputKey(cc.getCardNoEncrypt(), aesKey));
            paymentIPGRequest.getParameters().put("EXPIRYDATE", AESTools.decryptInputKey(cc.getCardExpiryDateEncrypt(), aesKey));
            paymentIPGRequest.getParameters().put("CVV2", oneCheckoutPaymentRequest.getDWCVV2());
            paymentIPGRequest.getParameters().put("NAME", cc.getCardName());
            paymentIPGRequest.getParameters().put("CONTACTABLE_NAME", cc.getCardName());
            paymentIPGRequest.getParameters().put("CITY", cc.getCardCity());
            paymentIPGRequest.getParameters().put("STATE", cc.getCardCity());
            paymentIPGRequest.getParameters().put("CustIP", oneCheckoutPaymentRequest.getCUSTIP());
            //String country = queryHelper.getCountryById(oneCheckoutPaymentRequest.getCOUNTRY());
            //paymentCashWalletRequest.getParameters().put("COUNTRY", country != null ? country : oneCheckoutPaymentRequest.getCOUNTRY());
            paymentIPGRequest.getParameters().put("COUNTRY", "ID");
            paymentIPGRequest.getParameters().put("PHONE", cc.getCardPhone());
            paymentIPGRequest.getParameters().put("HOMEPHONE", cc.getCardPhone());
            paymentIPGRequest.getParameters().put("OFFICEPHONE", cc.getCardPhone());
            paymentIPGRequest.getParameters().put("EMAIL", cc.getCardEmail());
            paymentIPGRequest.getParameters().put("ZIP_CODE", cc.getCardZipCode());
            paymentIPGRequest.getParameters().put("ADDRESS", cc.getCardCity());
//            String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            paymentIPGRequest.getParameters().put("OCOID", ocoId);
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            OneCheckoutLogger.log("PAYMENT to IPG => " + paymentIPGRequest.getParameters().toString());
            return paymentIPGRequest;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    private OneCheckoutDataPGRedirect parsingIPG3DSecureData(XMLConfiguration xml) {
        OneCheckoutDataPGRedirect redirect = null;
        try {
            String secureData = xml.getString("verifyReason");
            String[] param3DS = secureData.split("splitField");
            String pareq = param3DS[0];
            String MD = param3DS[1];
            String ACSURL = param3DS[2];
            String mpiPassword = param3DS[3];

            String[] pareqs = pareq.split("\\|");
            String[] mds = MD.split("\\|");
            String[] acsurl = ACSURL.split("\\|");
            String[] mpipassword = mpiPassword.split("\\|");

            redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setUrlAction(acsurl[1]);
            redirect.setPageTemplate(redirect.getProgressPage());

            Map data = redirect.getParameters();
            data.put(pareqs[0], pareqs[1]);
            data.put(mds[0], mds[1]);
            String RedirectBackUrl = config.getString("ONECHECKOUT.RedirectBackURLFromACS", "").trim() + "?" + mpipassword[0] + "=" + mpipassword[1];
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment RedirectBackUrl => %s", RedirectBackUrl);
            data.put("TermUrl", RedirectBackUrl); // ini url baliknya apa
            OneCheckoutLogger.log("ACSURL ==> [" + acsurl[0] + "]=[" + acsurl[1] + "]");
            OneCheckoutLogger.log("PAREQ ==> [" + pareqs[0] + "]=[" + pareqs[1] + "]");
            OneCheckoutLogger.log("XID ==> [" + mds[0] + "]=[" + mds[1] + "]");
            OneCheckoutLogger.log("TermUrl ==> [TermUrl]=[" + RedirectBackUrl + "]");
            redirect.setParameters(new HashMap(data));
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return redirect;
    }

    private Transactions parseIPGXMLPayment(Transactions trans, XMLConfiguration xml) {
        try {
            /*
             <orderResponse>
             <order-number></order-number>
             <purchase-amount></purchase-amount>
             <responsecode></responsecode>
             <status></status>
             <ApprovalCode></ApprovalCode>
             <HostReferenceNumber></HostReferenceNumber>
             <StatusCode></StatusCode>
             <installment></installment>
             <plan></plan>
             <tenor></tenor>
             <monthly></monthly>
             <InterestRate></InterestRate>
             <purchase-mdr></purchase-mdr>
             <OnUs></OnUs>
             <IssuerBankName></IssuerBankName>
             <AcquirerCode></AcquirerCode>
             <mallId></mallId>
             <chainNum></chainNum>
             <verifyId></verifyId>
             <verifyScore></verifyScore>
             <verifyStatus></verifyStatus>
             <verifyReason></verifyReason>
             <systraceNumber></systraceNumber>
             <batchNumber></batchNumber>
             </orderResponse>
             */

            String status = xml.getString("status");
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            if (status != null && status.equalsIgnoreCase("Success")) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            }
            String responseCode = xml.getString("responsecode");
            String approvalCode = xml.getString("ApprovalCode");
            String issuerBankName = xml.getString("IssuerBankName");
            String verifyStatus = xml.getString("verifyStatus");
            String verifyScore = xml.getString("verifyScore");
            //  String verifyReason = xml.getString("verifyReason");
            String hostRefNum = xml.getString("HostReferenceNumber");
            if (responseCode == null || responseCode.isEmpty()) {
                responseCode = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE.value();
            } else if (responseCode != null && responseCode.length() != 4) {
                responseCode = "00" + responseCode;
            }
            trans.setDokuApprovalCode(approvalCode);
            trans.setDokuIssuerBank(issuerBankName);
            trans.setDokuResponseCode(responseCode);
            trans.setDokuHostRefNum(hostRefNum);
            try {
                if (verifyStatus != null) {
                    OneCheckoutDFSStatus vStatus = OneCheckoutDFSStatus.findType(verifyStatus.trim().charAt(0));
                    if (vStatus != null) {
                        trans.setVerifyStatus(vStatus.value());
                    } else {
                        trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    }
                    if (verifyScore != null) {
                        trans.setVerifyScore(Integer.parseInt(verifyScore));
                    } else {
                        trans.setVerifyScore(-1);
                    }
                    trans.setVerifyId("");
                } else {
                    trans.setVerifyId("");
                    trans.setVerifyScore(-1);
                    trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                }
            } catch (Throwable t) {
                trans.setVerifyId("");
                trans.setVerifyScore(-1);
                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
            }
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return trans;
    }
//  PREVIOUS VERSION

    private OneCheckoutDataPGRedirect createPaymentMPGRequest(OneCheckoutPaymentRequest oneCheckoutPaymentRequest, MerchantPaymentChannel merchantPaymentChannel, MIPInquiryResponse mIPInquiryResponse, String ocoId) {
//  MODIFIED VERSION    
//    private OneCheckoutDataPGRedirect createPaymentMPGRequest(OneCheckoutPaymentRequest oneCheckoutPaymentRequest, MerchantPaymentChannel merchantPaymentChannel, MIPDokuInquiryResponse mIPDokuInquiryResponse) {
        try {
            OneCheckoutDataPGRedirect oneCheckoutDataPGRedirect = new OneCheckoutDataPGRedirect();
            oneCheckoutDataPGRedirect.setPageTemplate(oneCheckoutDataPGRedirect.getProgressPage());
            oneCheckoutDataPGRedirect.setUrlAction(merchantPaymentChannel.getPaymentChannel().getRedirectPaymentUrlCip());
            oneCheckoutDataPGRedirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            oneCheckoutDataPGRedirect.setAMOUNT(oneCheckoutPaymentRequest.getAMOUNT());
            oneCheckoutDataPGRedirect.setTRANSIDMERCHANT(oneCheckoutPaymentRequest.getTRANSIDMERCHANT());
            HashMap<String, String> data = oneCheckoutDataPGRedirect.getParameters();
            data.put("MALLID", merchantPaymentChannel.getPaymentChannelCode() + "");
            if (merchantPaymentChannel.getPaymentChannelChainCode() != null && merchantPaymentChannel.getPaymentChannelChainCode() > 0) {
                data.put("CHAINMALLID", merchantPaymentChannel.getPaymentChannelChainCode() + "");
            }
            data.put("INVOICENUMBER", oneCheckoutPaymentRequest.getTRANSIDMERCHANT());
            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getAMOUNT()));
            data.put("VAT", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getVAT()));
            data.put("INSURANCE", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getINSURANCE()));
            data.put("FUELCHARGE", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getFUELSURCHARGE()));
            OneCheckoutLogger.log("CURENCY " + oneCheckoutPaymentRequest.getCURRENCY());
            Currency currency = queryHelper.getCurrencyByCode(oneCheckoutPaymentRequest.getCURRENCY());
            data.put("CURRENCY", currency != null ? currency.getAlpha3Code() : "");
            data.put("SESSIONID", oneCheckoutPaymentRequest.getSESSIONID());
            data.put("WORDS", super.generateDokuMPGWords(oneCheckoutPaymentRequest, merchantPaymentChannel, currency.getAlpha3Code()));
            data.put("BASKET", oneCheckoutPaymentRequest.getBASKET());

//          PREVIOUS VERSION
            MIPInquiryResponse.DetailsOfCC cc = null;
//          MODIFIED VERSION            
//          MIPDokuInquiryResponse.DetailsOfCC cc = null;

//        PREVIOUS VERSION
            for (com.onecheckoutV1.ejb.helper.MIPInquiryResponse.PaymentChannel paymentChannel : mIPInquiryResponse.getListPaymentChannel()) {
//          MODIFIED VERSION
//            for(DokuPaymentChannel paymentChannel : mIPDokuInquiryResponse.getListPaymentChannel()){
                if (paymentChannel.getChannelCode().equals("02") && paymentChannel.getDetails() != null) {
                    ArrayList<Map> lstDetils = (ArrayList<Map>) paymentChannel.getDetails();
                    for (Map hashMap : lstDetils) {
                        if (hashMap.get("linkId") != null && hashMap.get("linkId").toString().trim().equalsIgnoreCase(oneCheckoutPaymentRequest.getDWCREDITCARD())) {
//                          PREVIOUS VERSION                            
                            cc = new MIPInquiryResponse.DetailsOfCC();
//                          MODIFIED VERSION
//                            cc = new MIPDokuInquiryResponse.DetailsOfCC();
                            BeanUtils.populate(cc, hashMap);
                            break;
                        }
                    }
                }
            }

            data.put("CCNAME", cc.getCardName());
            data.put("CCEMAIL", cc.getCardEmail());
            data.put("CCCITY", cc.getCardCity());
            data.put("CCREGION", cc.getCardCity());
            data.put("CCCOUNTRY", "ID");
            data.put("CCPHONE", cc.getCardPhone());
            data.put("CCZIPCODE", cc.getCardZipCode());
            data.put("SERVICEID", merchantPaymentChannel.getPaymentChannel().getServiceId());
            data.put("BILLINGADDRESS", cc.getCardCity());
            data.put("HOMEPHONE", cc.getCardPhone());
            data.put("CONTACTABLE_NAME", cc.getCardName());
            data.put("OFFICEPHONE", cc.getCardPhone());
            data.put("CustIP", oneCheckoutPaymentRequest.getCUSTIP());
            //DEVICEID RED
            data.put("DEVICEID", oneCheckoutPaymentRequest.getDEVICEID());
            if (merchantPaymentChannel.getMerchants().getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {
                String kam = "";
                if (oneCheckoutPaymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.OneWay) {
                    if (oneCheckoutPaymentRequest != null && oneCheckoutPaymentRequest.getROUTE() != null && oneCheckoutPaymentRequest.getROUTE().length > 0) {
                        kam = oneCheckoutPaymentRequest.getROUTE()[0];
                        data.put("decisionManager_travelData_completeRoute", oneCheckoutPaymentRequest.getROUTE()[0]);
                    }
                    data.put("decisionManager_travelData_journeyType", "one way");
                } else if (oneCheckoutPaymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.Return) {
                    if (oneCheckoutPaymentRequest != null && oneCheckoutPaymentRequest.getROUTE() != null && oneCheckoutPaymentRequest.getROUTE().length > 0) {
                        String temp = "";
                        if (oneCheckoutPaymentRequest.getROUTE().length > 1) {
                            temp = " n " + oneCheckoutPaymentRequest.getROUTE()[1];
                        }
                        kam = oneCheckoutPaymentRequest.getROUTE()[0] + temp;
                        data.put("decisionManager_travelData_completeRoute", oneCheckoutPaymentRequest.getROUTE()[0] + temp);
                    }
                    data.put("decisionManager_travelData_journeyType", "round trip");
                }
                OneCheckoutLogger.log("decisionManager_travelData_completeRoute : %s ", kam);
                data.put("decisionManager_travelData_journeyType", oneCheckoutPaymentRequest.getFLIGHTTYPE().value() + "");
                data.put("decisionManager_travelData_departureDateTime", OneCheckoutVerifyFormatData.cybersource_datetimeFormat.format(oneCheckoutPaymentRequest.getFLIGHTDATETIME()));
                data.put("flightType", oneCheckoutPaymentRequest.getFLIGHT().value());
                data.put("ffNumber", oneCheckoutPaymentRequest.getFFNUMBER());
                if (oneCheckoutPaymentRequest.getFLIGHTNUMBER() != null && oneCheckoutPaymentRequest.getFLIGHTNUMBER().length > 0) {
                    data.put("flightNumber", oneCheckoutPaymentRequest.parseDataAirLine(oneCheckoutPaymentRequest.getFLIGHTNUMBER()));
                }
                if (oneCheckoutPaymentRequest.getFLIGHTDATE() != null && oneCheckoutPaymentRequest.getFLIGHTDATE().length > 0) {
                    data.put("flightDate", oneCheckoutPaymentRequest.parseDataAirLine(oneCheckoutPaymentRequest.getFLIGHTDATE()));
                }
                if (oneCheckoutPaymentRequest.getPASSENGER_NAME() != null) {
                    String[] passengerName = oneCheckoutPaymentRequest.getPASSENGER_NAME();
                    String[] passengerType = oneCheckoutPaymentRequest.getPASSENGER_TYPE();
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
                            data.put("item_" + i + "_passengerEmail", oneCheckoutPaymentRequest.getEMAIL());
                            data.put("item_" + i + "_passengerPhone", oneCheckoutPaymentRequest.getMOBILEPHONE());
                            data.put("item_" + i + "_unitPrice", OneCheckoutVerifyFormatData.sdfnodecimal.format(oneCheckoutPaymentRequest.getAMOUNT()));
                        }
                    }
                    data.put("item_count", count + "");
                }
                data.put("ADDITIONALINFO", EDSAdditionalInformation.getAdditionalInfoForEdu(oneCheckoutPaymentRequest));
            } else {
                data.put("ADDITIONALINFO", oneCheckoutPaymentRequest.getADDITIONALINFO());
            }

            HashMap<String, String> data1 = new HashMap(data);
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            String creditCard = AESTools.decryptInputKey(cc.getCardNoEncrypt(), aesKey);
            String expDate = AESTools.decryptInputKey(cc.getCardExpiryDateEncrypt(), aesKey);
            data1.put("CARDNUMBER", base.maskingString(creditCard, "PAN"));
            data1.put("EXPIRYMONTH", expDate.substring(2, 4));
            data1.put("EXPIRYYEAR", expDate.substring(0, 2));
            OneCheckoutLogger.log("DATA to MPG => " + data1.toString());
            data.put("CARDNUMBER", creditCard);
            data.put("EXPIRYMONTH", expDate.substring(2, 4));
            data.put("EXPIRYYEAR", expDate.substring(0, 2));
            data.put("CVV2", oneCheckoutPaymentRequest.getDWCVV2());
//            String paymentChannelId = merchantPaymentChannel.getPaymentChannel().getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            data.put("SERVICETRANSACTIONID", ocoId);
            oneCheckoutDataPGRedirect.setParameters(data);
            return oneCheckoutDataPGRedirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataPGRedirect parsingMPG3DSecureData(XMLConfiguration xml) {
        OneCheckoutDataPGRedirect redirect = null;
        try {
            String pareq = xml.getString("paReq");
            String MD = xml.getString("md");
            String ACSURL = xml.getString("acsUrl");
            redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setUrlAction(ACSURL);
            redirect.setPageTemplate(redirect.getProgressPage());
            Map data = redirect.getParameters();
            data.put("PaReq", pareq);
            data.put("MD", MD);
            String RedirectBackUrl = config.getString("ONECHECKOUT.RedirectBackURLFromACS", "").trim();// + "?" + mpipassword[0] + "=" + mpipassword[1];
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment RedirectBackUrl => %s", RedirectBackUrl);
            data.put("TermUrl", RedirectBackUrl); // ini url baliknya apa
            OneCheckoutLogger.log("ACSURL ==> [acsUrl]=[" + ACSURL + "]");
            OneCheckoutLogger.log("PAREQ ==> [PaReq]=[" + pareq + "]");
            OneCheckoutLogger.log("XID ==> [md]=[" + MD + "]");
            OneCheckoutLogger.log("TermUrl ==> [TermUrl]=[" + RedirectBackUrl + "]");
            redirect.setParameters(new HashMap(data));
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return redirect;
    }

    public Transactions parseMPGXMLPayment(Transactions trans, XMLConfiguration xml, Merchants merchants) {
        try {
            String result = xml.getString("result") != null ? xml.getString("result").trim() : "";
            String responseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
            String approvalCode = xml.getString("approvalCode") != null ? xml.getString("approvalCode").trim() : "";
            String issuerBank = xml.getString("issuerBank") != null ? xml.getString("issuerBank").trim() : "";
            String verifyId = xml.getString("fraudScreeningId") != null ? xml.getString("fraudScreeningId").trim() : "";
            String verifyStatus = xml.getString("fraudScreeningStatus") != null ? xml.getString("fraudScreeningStatus").trim() : "NA";
            String verifyScore = xml.getString("fraudScreeningScore") != null ? xml.getString("fraudScreeningScore").trim() : "-1";
            String hostRefNum = xml.getString("hostReferenceNumber") != null ? xml.getString("hostReferenceNumber").trim() : "";
            String bank = xml.getString("bank") != null ? xml.getString("bank").trim() : "";
            String trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
            String errorCode = xml.getString("errorCode") != null ? xml.getString("errorCode").trim() : "";
            String liability = xml.getString("liability") != null ? xml.getString("liability").trim() : "";
            String threeDSecureStatus = xml.getString("threeDSecureStatus") != null ? xml.getString("threeDSecureStatus").trim() : "";
            //required batch settlement(start)
            String batchId = xml.getString("batchId") != null ? xml.getString("batchId").trim() : "";
            String batchNumber = xml.getString("batchNumber") != null ? xml.getString("batchNumber").trim() : "";
            String tid = xml.getString("tid") != null ? xml.getString("tid").trim() : "";
            String acquirerId = xml.getString("bankId") != null ? xml.getString("bankId").trim() : "";
            String settlementDate = xml.getString("settlementDate") != null ? xml.getString("settlementDate").trim() : "";
            String mid = xml.getString("mid") != null ? xml.getString("mid").trim() : "";
            //required batch settlement(end)

            trans.setInc3dSecureStatus(threeDSecureStatus);
            trans.setIncLiability(liability);

            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            if (result != null && result.equalsIgnoreCase("Success")) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            }

            if (responseCode != null && responseCode.length() != 4) {
                if (errorCode != null && !errorCode.trim().equals("")) {
                    if (errorCode.trim().equalsIgnoreCase("XX07")) {
                        responseCode = "BB";
                    } else {
                        responseCode = "99";
                    }
                }
                responseCode = "00" + responseCode;
            }
            trans.setDokuApprovalCode(approvalCode);
            trans.setDokuIssuerBank(issuerBank);
            trans.setDokuResponseCode(responseCode);
            trans.setDokuHostRefNum(hostRefNum);
            trans.setDokuVoidApprovalCode(trxCode);
            trans.setTrxCodeCore(trxCode);
//            trans.setDokuIssuerBank(bank);
            //required settlement and refund(start)
            
//            if (!batchId.equals("") && !batchNumber.equals("") && !mid.equals("") && !tid.equals("") && !acquirerId.equals("") && merchants.getMerchantEscrow() != null && !merchants.getMerchantEscrow()) {
//                Criterion c = Restrictions.eq("coreBatchId", batchId);
//                Criterion c1 = Restrictions.eq("coreBatchNumber", batchNumber);
//                Criterion c2 = Restrictions.eq("mid", mid);
//                Criterion c3 = Restrictions.eq("tid", tid);
//                Criterion c4 = Restrictions.eq("acquirerId", acquirerId);
////                Criterion c5 = Restrictions.eq("mall_id", trans.getIncMallid());
////                Criterion c6 = Restrictions.eq("chainMerchant", trans.getIncChainmerchant());
//
////                Batch batch = queryHelper.getBatchByCriterion(c, c1, c2, c3, c4, c5, c6);
//                Batch batch = queryHelper.getBatchByCriterion(c, c1, c2, c3, c4);
////                Batch batch1 = new Batch();
//                if (batch == null) {
//                    batch = new Batch();
//                    batch.setBatchId(nl.getNextVal(SeqBatchIdNextval.class).longValue());
//                    batch.setCoreBatchId(batchId);
//                    batch.setCoreBatchNumber(batchNumber);
//                    batch.setType(OneCheckOutBatchType.MPG.value());
//                    batch.setTid(tid);
//                    batch.setMid(mid);
//                    batch.setAcquirerId(acquirerId);
//                    batch.setStatus(OneCheckOutSettlementStatus.OPEN.value());
//                    batch.setMallId(trans.getIncMallid());
//                    batch.setChainMerchant(trans.getIncChainmerchant());
//                    if (!settlementDate.equals("")) {
//                        DateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH:MM:ss.SSS");
//                        Date d2 = dateFormat.parse(settlementDate);
//                        batch.setSettlementDate(d2);
//                    }
//                    queryHelper.saveBatch(batch);
//                }
//                trans.setBatchId((int) batch.getBatchId());
//            }

            //required settlement and refund(end)
            try {
                OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                if (verifyStatus != null) {
                    notifyRequest.setDFSStatus(verifyStatus);
                    notifyRequest.setDFSScore(verifyScore);
                    notifyRequest.setDFSIId(verifyId);
                    trans.setVerifyId(notifyRequest.getDFSId());
                    trans.setVerifyScore(notifyRequest.getDFSScore());
                    trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                } else {
                    notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                    notifyRequest.setDFSScore("-1");
                    notifyRequest.setDFSIId("");
                    trans.setVerifyId("");
                    trans.setVerifyScore(-1);
                    trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                }
            } catch (Throwable t) {
                trans.setVerifyScore(0);
            }
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());
            em.merge(trans);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return trans;
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doCCVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();

            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();
            //  double amount = voidRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doCCVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getVoidTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doCCVoid : Transaction is null");
                voidRequest.setVOIDRESPONSE(OneCheckoutTransactionStatus.SUCCESS.name());

                trxHelper.setVoidRequest(voidRequest);
                return trxHelper;

            }
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();//trxHelper.getMerchant();

            String word = super.generateVoidWords(voidRequest, m);//.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(voidRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doCCVoid : WORDS doesn't match !");
                voidRequest.setVOIDRESPONSE(OneCheckoutTransactionStatus.FAILED.name());

                trxHelper.setVoidRequest(voidRequest);
                return trxHelper;

            }

            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());

            if (status == OneCheckoutTransactionStatus.VOIDED) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doVoid : Transaction already voided");
                voidRequest.setVOIDRESPONSE(OneCheckoutTransactionStatus.SUCCESS.name());

                trxHelper.setVoidRequest(voidRequest);
                return trxHelper;

            } else if (status == OneCheckoutTransactionStatus.SUCCESS) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doVoid - ready to void");

                StringBuilder sb = new StringBuilder();

                //element = amount + merchant.getMid() + merchant.getWords() + invoiceNo;
                StringBuffer words = new StringBuffer();
                words.append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
                words.append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
                words.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
                words.append(invoiceNo);

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doVoid - HASH : [%s]", words.toString());
                String hash = HashWithSHA1.doHashing(words.toString(), "SHA1", null);
//                sb.append("DPMALLID", mpc.getMerchantPaymentChannelPid() + "");     
                sb.append("DPMALLID=").append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelPid() + "");
                sb.append("&").append("IPGMALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
                sb.append("&").append("IPGCHAINNUM=").append(trans.getMerchantPaymentChannel().getPaymentChannelChainCode());
                sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNo);
                sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
                sb.append("&").append("DOKUPAYID=").append(trans.getAccountId());
                sb.append("&").append("BASKET=").append("notsend");
                sb.append("&").append("APPROVALCODE=").append(trans.getDokuApprovalCode());
                sb.append("&").append("WORDS=").append(hash);

//                sb.append("&").append("USERNAME=").append(m.getMerchantName());
//                sb.append("&").append("IP").append(trans.getSystemSession());
//                sb.append("&").append("REASON").append(voidRequest.getVOIDREASON());
                OneCheckoutLogger.log("VOID PARAM " + sb.toString());
                InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doVoid - void Status : %s", inetResp.getMsgResponse());
                if (inetResp.getMsgResponse().equalsIgnoreCase("CONTINUE")) {

                    trans.setDokuVoidApprovalCode(voidRequest.getApprovalCode());
                    trans.setDokuVoidDatetime(new Date());
                    trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());

                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doVoid - update trx as voided");

                } else {

                    trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                    trans.setDokuVoidDatetime(new Date());
                    voidRequest.setVOIDRESPONSE("FAILED");

                    OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doVoid - update response code void");
                }

                em.merge(trans);

            } else {

                voidRequest.setVOIDRESPONSE("FAILED");
                OneCheckoutLogger.log("OneCheckoutCreditCardProcessorBean.doVoid - trx can't be voided");
            }

            trxHelper.setVoidRequest(voidRequest);

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doCCVoid - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getVoidRequest().setVOIDRESPONSE(OneCheckoutTransactionStatus.FAILED.name());
            //trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();

            String invoiceNo = verifyRequest.getTRANSIDMERCHANT();
            String sessionId = verifyRequest.getSESSIONID();
            double amount = verifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInquiryInvoice - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getVerifyTransactionBy(invoiceNo, sessionId, amount, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInquiryInvoice : Transaction is null");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUVerifyRequestWords(verifyRequest, trans);

            if (!word.equalsIgnoreCase(verifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInquiryInvoice : WORDS doesn't match !");
                verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setVerifyRequest(verifyRequest);
                return trxHelper;

            }

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInquiryInvoice - T2: %d update transaction", (System.currentTimeMillis() - t1));

            em.merge(trans);

            verifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);

            trxHelper.setVerifyRequest(verifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getVerifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuInvokeStatusDatetime(new Date());
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
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

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
//            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
            HashMap<String, String> params = super.getData(trans);

            params.put("PAYMENTCODE", "");

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));
            // proses parsing ack from merchant, then save it to database

            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper oneCheckoutDataHelper, PaymentChannel paymentChannel) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = oneCheckoutDataHelper.getMerchant();
            OneCheckoutRedirectData redirectRequest = oneCheckoutDataHelper.getRedirectDoku();
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
                invoiceNo = oneCheckoutDataHelper.getTransactions().getIncTransidmerchant();
                sessionId = oneCheckoutDataHelper.getTransactions().getIncSessionid();
                amount = oneCheckoutDataHelper.getTransactions().getIncAmount().doubleValue();
            }

            Object[] obj = queryHelper.getRedirectTransactionWithoutStateNumber(invoiceNo, sessionId, amount);//.getRedirectTransactionBy(invoiceNo, sessionId, amount);
            attempts = (Integer) obj[0];
            Transactions trans = (Transactions) obj[1];
            if (redirectRequest == null) {
                trans = oneCheckoutDataHelper.getTransactions();
                statusCode = trans.getDokuResponseCode();
            }
            OneCheckoutLogger.log("ATTEMPTS : " + attempts);
            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.value());
                trans.setDokuResultMessage(trans.getDokuResultMessage());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuResultMessage(statusCode);
                trans.setDokuInvokeStatusDatetime(new Date());
            }

            //set data redirect yang akan di kirim ke merchant
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();

            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
            if (trans.getIncPurchasecurrency() != null && !trans.getIncPurchasecurrency().equalsIgnoreCase("360")) {
                redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());
            }

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
                    redirect.setAMOUNT(redirectRequest.getAMOUNT());
                    redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
                    redirect.setSTATUSCODE(redirectRequest.getSTATUSCODE());
                    redirect.setUrlAction(m.getMerchantRedirectUrl());
                    redirect.setPageTemplate("redirect.html");
                }
            } else if (redirectRequest != null && (redirectRequest.getFLAG() == null || redirectRequest.getFLAG().equalsIgnoreCase("2"))) {
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(statusCode);
                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");
            } else {
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
                resultData.put("DISPLAYCHANNEL", oneCheckoutDataHelper.getPaymentChannel().value());
                OneCheckoutLogger.log("TRANSACTION STATUS [" + trans.getTransactionsStatus() + "]");
                if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");
                    redirect.setPageTemplate("transactionFailed.html");
                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A3");
                    redirect.setPageTemplate("transactionSuccess.html");
                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with " + m.getMerchantName());
                    resultData.put("APPROVAL", trans.getDokuApprovalCode());
                }
                redirect.setRetryData(resultData);
                HashMap<String, String> params = super.getData(trans);
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                trans = super.notifyOnRedirect(trans, params, channel);
            }

            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
            data.put("TRANSIDMERCHANT", invoiceNo);
            data.put("STATUSCODE", statusCode);
            data.put("PAYMENTCHANNEL", oneCheckoutDataHelper.getPaymentChannel().value());
            data.put("SESSIONID", sessionId);
            data.put("PAYMENTCODE", "");
            data.put("PURCHASECURRENCY", redirect.getPURCHASECURRENCY());
            if (trans.getRates() != null && redirect.getUrlAction().equals(m.getMerchantRedirectUrl())) {
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
            oneCheckoutDataHelper.setMessage("VALID");
            oneCheckoutDataHelper.setRedirect(redirect);//.setPayResponse(paymentResp);
            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1DokupayDIRECTCOREBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return oneCheckoutDataHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            oneCheckoutDataHelper.setMessage(th.getMessage());

            return oneCheckoutDataHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }


    /*
     public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
     OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
     //        OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
     try {
     long t1 = System.currentTimeMillis();
     OneCheckoutLogger.log("OneCheckoutV1BRIPayBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

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

     // KALAU MAU DIBUAT DISINI JALANIN CHECK STATUS KE CORE DOKUWALLET

     HashMap<String, String> params =  super.getData(trans);

     params.put("PAYMENTCODE","");
     OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();

     trxHelper.setMessage("VALID");
     statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params,m));
     trxHelper.setCheckStatusRequest(statusRequest);

     return trxHelper;

     }

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
     trxHelper.setMessage("VALID");
     statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
     trxHelper.setCheckStatusRequest(statusRequest);

     return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
     }
     }
     */
    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1DokupayBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
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

                OneCheckoutLogger.log("OneCheckoutV1DokupayBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - t1));
                if (trans.getEduReason() != null && (trans.getEduReason().equals("CASHWALLET") || trans.getEduReason().equals("TCASH"))) {
                    trans = super.CheckStatusCOREDokupayMIP(trans);
                } else {
                    MerchantPaymentChannel dokuWalletMerchantPaymentChannel = trans.getMerchantPaymentChannel();
                    MerchantPaymentChannel merchantPaymentChannel = queryHelper.getMerchantPaymentChannel(m, OneCheckoutPaymentChannel.CreditCard);
                    if (merchantPaymentChannel != null) {
                        // ===========
                        //    I P G
                        // ===========
                        trans.setMerchantPaymentChannel(merchantPaymentChannel);
                        trans = super.CheckStatusCOREIPG(trans, merchantPaymentChannel.getPaymentChannel());
                    } else {
                        merchantPaymentChannel = queryHelper.getMerchantPaymentChannel(m, OneCheckoutPaymentChannel.BSP);
                        if (merchantPaymentChannel != null) {
                            // ===========
                            //    M P G
                            // ===========
                            trans.setMerchantPaymentChannel(merchantPaymentChannel);
                            trans = super.CheckStatusCOREMPG(trans, merchantPaymentChannel.getPaymentChannel());
                        }
                    }
                    trans.setMerchantPaymentChannel(dokuWalletMerchantPaymentChannel);
                }
                HashMap<String, String> params = super.getData(trans);
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
            OneCheckoutLogger.log("OneCheckoutV1DokupayBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
//    MODIFIED VERSION
//    public MIPDokuPaymentResponse getAllMipDokuPayment(String paymentResponse){
//        try {
//            MIPDokuPaymentResponse mIPDokuPaymentResponse = new MIPDokuPaymentResponse();
//            Type stringObjectMap = new TypeToken<Map<String, Object>>() {
//            }.getType();
//            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
//            Map<String, Object> mapObject = gson.fromJson(paymentResponse, stringObjectMap);
//            BeanUtils.populate(mIPDokuPaymentResponse, mapObject);
//            return mIPDokuPaymentResponse;
//        } catch (Throwable th) {
//            th.printStackTrace();
//            return null;
//        }
//    }

//  MODIFIED VERSION
//    public MIPDokuInquiryResponse getAllMIPDokuInquiryResponse(String inquiryResponse,OneCheckoutDataHelper oneCheckoutDataHelper){
//        try {
//            MIPDokuInquiryResponse mIPDokuInquiryResponse = new MIPDokuInquiryResponse();
//            Type stringObjectMap = new TypeToken<Map<String, Object>>() {
//            }.getType();
//            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
//            Map<String, Object> mapObject = gson.fromJson(inquiryResponse, stringObjectMap);
//            BeanUtils.populate(mIPDokuInquiryResponse, mapObject);
//            
//            List<Map<String, DokuPaymentChannel>> mapListPayment = (List<Map<String, DokuPaymentChannel>>) mapObject.get("listPaymentChannel");
//            List<DokuPaymentChannel> lstPaymentChannel = new ArrayList<DokuPaymentChannel>();
//            for (Map<String, DokuPaymentChannel> payChannel : mapListPayment) {
//                DokuPaymentChannel dpc = new DokuPaymentChannel();
//                BeanUtils.populate(dpc, payChannel);
//                lstPaymentChannel.add(dpc);
//            }
//            
//            List<Map<String, PromotionChannel>> mapList = (List<Map<String, PromotionChannel>>) mapObject.get("listPromotion");
//            List<PromotionChannel> lstPromotinChannel = new ArrayList<PromotionChannel>();
//            if (mapList != null && mapList.size() > 0) {
//                for (Map<String, PromotionChannel> pchannel : mapList) {
//                    PromotionChannel pc = new PromotionChannel();
//                    pc.setName(String.valueOf(pchannel.get("name")) != null ? String.valueOf(pchannel.get("name")) : "");
//                    pc.setId(String.valueOf(pchannel.get("id")) != null ? String.valueOf(pchannel.get("id")) : "");
//                    pc.setAmount(new BigDecimal(String.format("%.2f", pchannel.get("amount"))));
//
//                    BigDecimal bdVal = BigDecimal.valueOf(oneCheckoutDataHelper.getPaymentRequest().getAMOUNT());
//                    bdVal = bdVal.subtract(pc.getAmount());
//                    pc.setAfterPromotionAmount(bdVal);
//                    lstPromotinChannel.add(pc);
//                }
//            }
////          INSANITY CHECK FOR CREDIT CARD / T CASH
////          for (DokuPaymentChannel paymentChannel : lstPaymentChannel) {
////                if (paymentChannel.getChannelCode().equals("03") && paymentChannel.getDetails() != null) {
////                    ArrayList<Map> lstDetils = (ArrayList<Map>) paymentChannel.getDetails();
////                    for (Map hashMap : lstDetils) {
////                        if (hashMap.get("linkId") != null ) {
////                             OneCheckoutLogger.log("TES "+hashMap.get("linkId").toString());
////                        }
////                    }
////                }
////          }
//            mIPDokuInquiryResponse.setListPaymentChannel(lstPaymentChannel);
//            mIPDokuInquiryResponse.setListPromotion(lstPromotinChannel);
//            return mIPDokuInquiryResponse;
//        } catch (Throwable th) {
//            th.printStackTrace();
//            return null;
//        }
//    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
