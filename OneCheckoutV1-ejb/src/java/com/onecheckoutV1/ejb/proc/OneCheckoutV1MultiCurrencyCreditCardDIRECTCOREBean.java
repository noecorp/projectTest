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
import com.onecheckoutV1.data.OneCheckoutInstallment;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutRewards;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.exception.InvalidPaymentRequestException;
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
import com.onechekoutv1.dto.Country;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.SeqBatchIdNextval;
import com.onechekoutv1.dto.Transactions;
import com.sun.xml.ws.resources.XmlmessageMessages;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author hafiz
 */
@Stateless
public class OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean extends OneCheckoutChannelBase implements OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBeanLocal {

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
        //MIPDokuSuitePaymentResponse paymentResp = trxHelper.getPaymentResponse();

        Transactions trans = null;
        Merchants m = null;
        OneCheckoutPaymentRequest paymentRequest = null;
        String paramString = "";
        String CoreUrl = "";
        String resultPayment = "";

        try {
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.dopayment test logger");;
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));
            m = trxHelper.getMerchant();
            paymentRequest = trxHelper.getPaymentRequest();
            boolean needNotify = false;
            OneCheckoutErrorMessage errormsg = OneCheckoutErrorMessage.UNKNOWN;
            // check disini
            CoreUrl = this.getCoreMipURL(pChannel, "DEFAULT");//, CoreUrl)pChannel.getRedirectPaymentUrlMipXml();
            boolean isResult = false;
            OneCheckoutLogger.log("trxHelper.getMessage() : " + trxHelper.getMessage());
            if (trxHelper.getMessage().equalsIgnoreCase("ACS")) {
                trans = trxHelper.getTransactions();
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment incoming from ACS");
                trxHelper.getPaymentRequest().getAllAdditionData().put("FROM", "ACS");
//                String paymentChannelId = pChannel.getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                trxHelper.getPaymentRequest().getAllAdditionData().put("SERVICETRANSACTIONID", trxHelper.getOcoId());
                paramString = super.createParamsHTTP(trxHelper.getPaymentRequest().getAllAdditionData());
                CoreUrl = this.getCoreMipURL(pChannel, "ACS");//pChannel.getRedirectPaymentUrlMip();
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment DATA => %s", paramString);
            } else if (trxHelper.getMessage().equalsIgnoreCase("REDIRECTACQUIRER")) {
                trans = trxHelper.getTransactions();
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment incoming from REDIRECTACQUIRER");
                //trxHelper.getPaymentRequest().getAllAdditionData().put("FROM", "REDIRECTACQUIRER");
                Map map = new HashedMap(trxHelper.getPaymentRequest().getAllAdditionData());
                Iterator i = map.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key != null && !key.startsWith("vpc_")) {
                        trxHelper.getPaymentRequest().getAllAdditionData().remove(key);
                    }
                }
//                String paymentChannelId = pChannel.getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                trxHelper.getPaymentRequest().getAllAdditionData().put("SERVICETRANSACTIONID", trxHelper.getOcoId());
                paramString = super.createParamsHTTP(trxHelper.getPaymentRequest().getAllAdditionData());
                CoreUrl = this.getCoreMipURL(pChannel, "REDIRECTACQUIRER");
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment DATA => %s", paramString);
            } else if (trxHelper.getMessage().equalsIgnoreCase("INS")) {
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment incoming from INSTALLMENT PAYMENT");
                if (trans == null) {
                    String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
                    String sessionId = paymentRequest.getSESSIONID();
                    trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, pChannel, OneCheckoutTransactionState.INCOMING);
                }
                OneCheckoutDataPGRedirect data = dataInstallmentRequest(paymentRequest, pChannel, trans.getOcoId());
                CoreUrl = this.getCoreMipURL(pChannel, "INS");
                paramString = super.createParamsHTTP(data.getParameters());
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment DATA => %s" + paramString);
                isResult = true;
            } else if (trxHelper.getMessage().equalsIgnoreCase("INSANDREWARDS")) {
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment incoming from INSTALLMENT AND REWARD PAYMENT");
                if (trans == null) {
                    String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
                    String sessionId = paymentRequest.getSESSIONID();
                    trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, pChannel, OneCheckoutTransactionState.INCOMING);
                }
                OneCheckoutDataPGRedirect data = dataInstallmentAndRewardsRequest(paymentRequest, pChannel, trans.getOcoId());
                CoreUrl = this.getCoreMipURL(pChannel, "INSANDREWARDS");
                paramString = super.createParamsHTTP(data.getParameters());
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment DATA => %s" + paramString);
                isResult = true;
            } else if (trxHelper.getMessage().equalsIgnoreCase("REWARDS")) {
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment incoming from REWARD PAYMENT");
                if (trans == null) {
                    String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
                    String sessionId = paymentRequest.getSESSIONID();
                    trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, pChannel, OneCheckoutTransactionState.INCOMING);
                }
                OneCheckoutDataPGRedirect data = dataRewardsRequest(paymentRequest, pChannel, trans.getOcoId());
                CoreUrl = this.getCoreMipURL(pChannel, "REWARDS");
                paramString = super.createParamsHTTP(data.getParameters());
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment DATA => %s" + paramString);
                isResult = true;
            } else {
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment incoming from Merchant");
                IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
                boolean request_good = identrx.isRequestGood();
                MerchantPaymentChannel mpc = identrx.getMpc();
                errormsg = identrx.getErrorMsg();
                needNotify = identrx.isNeedNotify();
                if (!request_good) {
                    trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, identrx.getTrans());
                    return trxHelper;
                }
                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }

                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

                OneCheckoutDataPGRedirect datatoCore = this.createRedirectMIP(paymentRequest, pChannel, mpc, m, trxHelper.getOcoId());
                HashMap<String, String> params = datatoCore.getParameters();
                paramString = super.createParamsHTTP(params);
            }

            boolean checkStatusToCore = false;
            try {
                resultPayment = super.postMIP(paramString, CoreUrl, pChannel);
//                resultPayment = this.getResultPayment(paymentRequest.getMALLID(), paymentRequest.getCHAINMERCHANT(), paymentRequest.getTRXCODE(), paymentRequest.getCARDNUMBER(), paymentRequest.getINVOICENUMBER(), paymentRequest.getAMOUNT(), new Date(), isResult);
                OneCheckoutLogger.log("CORE URL " + CoreUrl);
                if (resultPayment == null || resultPayment.trim().equals("") || resultPayment.trim().equalsIgnoreCase("ERROR")) {
                    checkStatusToCore = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                checkStatusToCore = true;
                /*
                 needNotify = true;
                 errormsg = OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE;
                 trans.setDokuResponseCode(errormsg.value());
                 trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                 trans.setDokuInvokeStatusDatetime(new Date());
                 em.merge(trans);
                 //trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                 trxHelper = this.doShowResultPage(trxHelper, pChannel);
                 trxHelper.setMessage("VALID");
                 return trxHelper;
                 */
            }
            String resultPaymentOriginal = resultPayment;
            if (resultPayment.contains("vpc_CardNum") && resultPayment.contains("vpc_CardExp") && resultPayment.contains("vpc_CardSecurityCode")) {
                OneCheckoutBaseRules base = new OneCheckoutBaseRules();

                String card = resultPayment.substring(resultPayment.indexOf("vpc_CardNum") + "vpc_CardNum".length() + 2, resultPayment.indexOf("vpc_CardNum") + "vpc_CardNum".length() + 2 + 16);
                String maskingCard = base.maskingString(card, "PAN");
                String expiry = resultPayment.substring(resultPayment.indexOf("vpc_CardExp") + "vpc_CardExp".length() + 2, resultPayment.indexOf("vpc_CardExp") + "vpc_CardExp".length() + 2 + 4);
                String expiryMasking = base.maskingString(expiry, "EXPIRYDATE");
                String cvv = resultPayment.substring(resultPayment.indexOf("vpc_CardSecurityCode") + "vpc_CardSecurityCode".length() + 2, resultPayment.indexOf("vpc_CardSecurityCode") + "vpc_CardSecurityCode".length() + 2 + 3);
                String cvvMasking = base.maskingString(cvv, "CVV");
                resultPayment = resultPayment.replace(card, maskingCard);
                resultPayment = resultPayment.replace(expiry, expiryMasking);
                resultPayment = resultPayment.replace(cvv, cvvMasking);
            }

            OneCheckoutLogger.log("RESULT PAYMENT : " + resultPayment);
            if (checkStatusToCore) {
                // CHECK STATUS TO CORE
                Thread.sleep(5 * 1000);
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREMPG(trans, pChannel);
                if (trans.getDokuResponseCode() != null && trans.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value())) {
                    trans = super.reversalToCOREMPG(trans);
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.TRANSACTION_CAN_NOT_BE_PROCCED.value());
                }
                needNotify = true;
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
                HashMap<String, String> params = super.getData(trans);
                params.put("PAYMENTCODE", "");
                String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
                //trans = super.notifyStatusMerchant(trans,trxHelper.getPaymentChannel());
                if (trans.getDokuResponseCode() != null && trans.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.NOTIFY_FAILED.value()) && trans.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                    super.reversalToCOREMPG(trans);
                }
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                em.merge(trans);
                trxHelper.setTransactions(trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
            }

            XMLConfiguration xml = new XMLConfiguration();
            try {
                StringReader sr = new StringReader(resultPaymentOriginal);
                xml.load(sr);
//                OneCheckoutLogger.log(resultPayment);
            } catch (Exception ex) {
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                trans.setDokuResponseCode(errormsg.value());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
                trans = super.reversalToCOREMPG(trans);
//                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
            }

            String statusResp = xml.getString("result");
            if (statusResp != null && (statusResp.equalsIgnoreCase("THREEDSECURE") || statusResp.equalsIgnoreCase("3DSECURE"))) {
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
                try {
                    redirect = this.parsing3DSecureData(xml);
                    trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);
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
                    return trxHelper;
                } finally {
                    redirect = null;
                }
            } else if (statusResp != null && statusResp.equalsIgnoreCase("REDIRECTTOACQUIRER")) {
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
                try {
                    redirect = this.parsingRedirectToAcquirerData(xml);
                    trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);
                    trans.setInc3dSecureStatus(redirect.getParameters().get("vpc_Merchant") + redirect.getParameters().get("vpc_MerchTxnRef"));
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACQUIRER.value());
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
                    return trxHelper;
                } finally {
                    redirect = null;
                }
            } else if (statusResp != null && statusResp.equalsIgnoreCase("INSTALLMENT")) {
                String trxCode = "";
                List<OneCheckoutInstallment> lstInstallmentStr = new ArrayList<OneCheckoutInstallment>();

                try {
                    trxHelper.setRedirect(new OneCheckoutDataPGRedirect());
                    trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode") : "";
                    trans.setDokuVoidApprovalCode(trxCode);
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.OFF_US_INSTALLMENT_PROCESS.value());
                    trxHelper.getPaymentRequest().setTRXCODE(trxCode);

                    lstInstallmentStr = this.parsingInstallmentData(xml, trxHelper);
                    trxHelper.getPaymentPageObject().setListOneCheckoutInstallment(lstInstallmentStr);
                    trxHelper.getPaymentPageObject().setMessage("INSTALLMENT");
                    em.merge(trans);
                    return trxHelper;
                } catch (Exception ex) {
                    needNotify = true;
                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                    trans.setDokuResponseCode(errormsg.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
                    trxHelper.setMessage("VALID");
                    return trxHelper;
                } finally {
                    trxCode = null;
                    lstInstallmentStr = null;
                }
            } else if (statusResp != null && statusResp.equalsIgnoreCase("INSTALLMENTANDREWARDS")) {
                String trxCode = "";
                List<OneCheckoutInstallment> lstInstallmentStr = new ArrayList<OneCheckoutInstallment>();
                OneCheckoutRewards rewards = new OneCheckoutRewards();
                try {
                    OneCheckoutLogger.log("PROCESSS INSTALLMENTANDREWARDS");
                    trxHelper.setRedirect(new OneCheckoutDataPGRedirect());
                    trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode") : "";
                    trans.setDokuVoidApprovalCode(trxCode);
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.OFF_US_INSTALLMENT_AND_REWARDS_PROCESS.value());
                    trxHelper.getPaymentRequest().setTRXCODE(trxCode);

                    lstInstallmentStr = this.parsingInstallmentData(xml, trxHelper);
                    trxHelper.getPaymentPageObject().setListOneCheckoutInstallment(lstInstallmentStr);
                    rewards = this.parsingRewardsData(xml);
                    trxHelper.getPaymentPageObject().setOneCheckoutRewards(rewards);
                    //trxHelper.getPaymentPageObject().setOneCheckoutRewards(new OneCheckoutRewards(xml.getInt("availablePoint"), xml.getInt("pointRedeemed"), xml.getDouble("amountRedeemed")));
                    trxHelper.getPaymentPageObject().setMessage("INSTALLMENTANDREWARDS");
                    em.merge(trans);
                    return trxHelper;
                } catch (Exception ex) {
                    OneCheckoutLogger.log("Error in INSTALLMENTANDREWARDS : %s", ex.getMessage());
                    needNotify = true;
                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                    trans.setDokuResponseCode(errormsg.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
                    trxHelper.setMessage("VALID");
                    return trxHelper;
                } finally {
                    trxCode = null;
                    lstInstallmentStr = null;
                    rewards = null;
                }
            } else if (statusResp != null && statusResp.equalsIgnoreCase("REWARDS")) {
                String trxCode = "";
                OneCheckoutRewards rewards = new OneCheckoutRewards();
                try {
                    trxHelper.setRedirect(new OneCheckoutDataPGRedirect());
                    trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode") : "";
                    trans.setDokuVoidApprovalCode(trxCode);
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.OFF_US_REWARDS_PROCESS.value());
                    trxHelper.getPaymentRequest().setTRXCODE(trxCode);

                    rewards = this.parsingRewardsData(xml);
                    trxHelper.getPaymentPageObject().setOneCheckoutRewards(rewards);
                    trxHelper.getPaymentPageObject().setMessage("REWARDS");
                    em.merge(trans);
                    return trxHelper;
                } catch (Exception ex) {
                    needNotify = true;
                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                    trans.setDokuResponseCode(errormsg.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
                    trxHelper.setMessage("VALID");
                    return trxHelper;
                } finally {
                    trxCode = null;
                    rewards = null;
                }
            } else {
                trans = this.parseXMLPayment(trans, xml, m);
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
                HashMap<String, String> params = super.getData(trans);

//              MODIFIED FOR INSTALLMENT
                String installmentCode = trxHelper.getPaymentRequest().getINSTALLMENTCODE();
                if (installmentCode != null && !installmentCode.equals("0") && !installmentCode.isEmpty()) {
                    List<OneCheckoutInstallment> lstOnecheckoutInstallment = trxHelper.getPaymentPageObject().getListOneCheckoutInstallment();
                    if (lstOnecheckoutInstallment != null && lstOnecheckoutInstallment.size() > 0) {
                        for (OneCheckoutInstallment installment : lstOnecheckoutInstallment) {
                            if (installmentCode.equals(installment.getCode())) {
                                trxHelper.setTenorResponse(installment.getTenor());
                                trxHelper.setInterestResponse(installment.getInterest());
                                trxHelper.setPlanIdResponse(installment.getPlan());
                                break;
                            }
                        }
                    }
                    trans.setIncTenor(trxHelper.getTenorResponse());
                    trans.setIncInstallmentAcquirer(trxHelper.getInterestResponse());
                    trans.setIncPromoid(trxHelper.getPlanIdResponse());
                }

                if (trxHelper.getPaymentRequest().getREDEMPTIONSTATUS().equals("TRUE")) {
                    OneCheckoutRewards rewards = trxHelper.getPaymentPageObject().getOneCheckoutRewards();
                    if (rewards != null) {
                        trans.setIncRewards(rewards.getPointRedeemed());
                    }
                }
//                trxHelper = this.doInstallmentDataPage(trxHelper, xml);
                params.put("PAYMENTCODE", "");
                String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment - %s", resp);
                if (statusResp != null && statusResp.trim().equalsIgnoreCase("Success") && trans.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                    super.reversalToCOREMPG(trans);
                }
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                em.merge(trans);
                trxHelper.setTransactions(trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
            }
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        } finally {
            trans = null;
            m = null;
            paymentRequest = null;
            paramString = null;
            CoreUrl = null;
            resultPayment = null;
        }
    }

    public OneCheckoutDataPGRedirect dataInstallmentAndRewardsRequest(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, String ocoId) {
        OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
        HashMap<String, String> data = new HashMap<String, String>();
        try {

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            data = redirect.getParameters();
            data.put("TRXCODE", paymentRequest.getTRXCODE());
            data.put("INSTALLMENTCODE", paymentRequest.getINSTALLMENTCODE());
            data.put("INSTALLMENTSTATUS", paymentRequest.getINSTALLMENTSTATUS());
            data.put("REDEMPTIONSTATUS", paymentRequest.getREDEMPTIONSTATUS());
//            String paymentChannelId = pChannel.getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            data.put("SERVICETRANSACTIONID", ocoId);
            OneCheckoutLogger.log("::DATA to DoProcessInstallmentAndReward => " + data);
            redirect.setParameters(data);
            return redirect;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            redirect = null;
            data = null;
        }
    }

    public OneCheckoutDataPGRedirect dataRewardsRequest(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, String ocoId) {
        OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
        HashMap<String, String> data = new HashMap<String, String>();
        try {
            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            data = redirect.getParameters();
            data.put("TRXCODE", paymentRequest.getTRXCODE());
            data.put("REDEMPTIONSTATUS", paymentRequest.getREDEMPTIONSTATUS());
//            String paymentChannelId = pChannel.getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            data.put("SERVICETRANSACTIONID", ocoId);
            OneCheckoutLogger.log("::DATA to DoProcessReward => " + data);
            redirect.setParameters(data);
            return redirect;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            redirect = null;
            data = null;
        }
    }

    private String getResultPayment(int mallId, int chainMallId, String trxCode, String cardNumber, String invoiceNumber, double amount, Date date, boolean isResult) {
        String xml = "<MPGPaymentResponse>\n"
                + "" + mallId + "\n"
                + "<chainMallId>" + chainMallId + "</chainMallId>\n"
                + "<trxCode>" + trxCode + "</trxCode>\n"
                + "<cardNumber>" + cardNumber.replaceAll(cardNumber.substring(1, 11), "xxxxxxxxxxx") + "</cardNumber>\n"
                + "<invoiceNumber>" + invoiceNumber + "</invoiceNumber>\n"
                + "<amount>" + amount + "</amount>\n"
                + "<currency>IDR</currency>\n"
                + "<sessionId></sessionId>\n"
                + "<paymentDate>" + date + "</paymentDate>\n"
                + "<result>" + ((isResult) ? "SUCCESS" : (cardNumber.contains("5105") ? "REWARDS" : (cardNumber.contains("4024") ? "INSTALLMENT" : "INSTALLMENTANDREWARDS"))) + "</result>\n"
                + "\n";
        if (!isResult) {
            xml += "<acsUrl></acsUrl>\n"
                    + "<paReq></paReq>\n"
                    + "\n"
                    + "<rewards>\n"
                    + "<availablePoint>87735</availablePoint>\n"
                    + "<pointRedeemed>15</pointRedeemed>\n"
                    + "<amountRedeemed>75.00</amountRedeemed>\n"
                    + "</rewards>\n";
        } else {
            xml += "<responseCode></responseCode>\n"
                    + "<approvalCode></approvalCode>\n"
                    + "<bank></bank>\n"
                    + "\n"
                    + "<tid></tid>\n"
                    + "<liability></liability>\n"
                    + "<threeDSecureStatus></threeDSecureStatus>\n"
                    + "<fraudScreeningId></fraudScreeningId>\n"
                    + "<fraudScreeningScore></fraudScreeningScore>\n"
                    + "<fraudScreeningStatus></fraudScreeningStatus>\n"
                    + "<fraudScreeningReason></fraudScreeningReason>";
        }

        if ((cardNumber.contains("4111") || cardNumber.contains("4024")) && !isResult) {
            xml += "<installments>\n"
                    + "<installment>\n"
                    + "<code>FjzlwF9MXIRINiCljfavLQ==</code>\n"
                    + "<description>CICILAN 12 BULAN</description>\n"
                    + "<tenor>12</tenor>\n"
                    + "<plan>002</plan>\n"
                    + "<interest>0.50</interest>\n"
                    + "</installment>\n"
                    + "<installment>\n"
                    + "<code>cD+45PMsY0RXBL3CNdJD2w==</code>\n"
                    + "<description>CICILAN 3 BULAN</description>\n"
                    + "<tenor>3</tenor>\n"
                    + "<plan>003</plan> <interest>1.50</interest>\n"
                    + "</installment>\n"
                    + "<installment>\n"
                    + "<code>cD+45PMsY0RXBL3CNdJD2w==</code>\n"
                    + "<description>CICILAN 6 BULAN</description>\n"
                    + "<tenor>6</tenor>\n"
                    + "<plan>001</plan> <interest>1.00</interest>\n"
                    + "</installment>\n"
                    + "</installments>\n";
        }

        if ((cardNumber.contains("4111") || cardNumber.contains("4024")) && !isResult) {
            xml += "<installments>\n"
                    + "<installment>\n"
                    + "<code>FjzlwF9MXIRINiCljfavLQ==</code>\n"
                    + "<description>CICILAN 12 BULAN</description>\n"
                    + "<tenor>12</tenor>\n"
                    + "<plan>002</plan>\n"
                    + "<interest>0.50</interest>\n"
                    + "</installment>\n"
                    + "<installment>\n"
                    + "<code>cD+45PMsY0RXBL3CNdJD2w==</code>\n"
                    + "<description>CICILAN 3 BULAN</description>\n"
                    + "<tenor>3</tenor>\n"
                    + "<plan>003</plan> <interest>1.50</interest>\n"
                    + "</installment>\n"
                    + "<installment>\n"
                    + "<code>cD+45PMsY0RXBL3CNdJD2w==</code>\n"
                    + "<description>CICILAN 6 BULAN</description>\n"
                    + "<tenor>6</tenor>\n"
                    + "<plan>001</plan> <interest>1.00</interest>\n"
                    + "</installment>\n"
                    + "</installments>\n";
        }
        xml += "</MPGPaymentResponse>";

        return xml;
    }

    public OneCheckoutRewards parsingRewardsData(XMLConfiguration xml) {
        //List<OneCheckoutRewards> lstRewards = new ArrayList<OneCheckoutRewards>();
        OneCheckoutRewards reward = new OneCheckoutRewards();
        try {
            Document doc = xml.getDocument();
            NodeList nList = doc.getElementsByTagName("rewards");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                Element eElement = (Element) nNode;
                int availablePoint = Integer.parseInt(eElement.getElementsByTagName("availablePoint").item(0).getTextContent());
                int pointRedeemed = Integer.parseInt(eElement.getElementsByTagName("pointRedeemed").item(0).getTextContent());
                double amountRedeemed = Double.parseDouble(eElement.getElementsByTagName("amountRedeemed").item(0).getTextContent());
                System.out.println("availablePoint : " + availablePoint);
                System.out.println("pointRedeemed : " + pointRedeemed);
                System.out.println("amountRedeemed : " + amountRedeemed);
                reward.setAmountRedeemed(amountRedeemed);
                reward.setAvailablePoint(availablePoint);
                reward.setPointRedeemed(pointRedeemed);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reward;
    }

    public OneCheckoutDataPGRedirect dataInstallmentRequest(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, String ocoId) {
        HashMap<String, String> data = new HashMap<String, String>();
        OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
        try {
            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            data = redirect.getParameters();
            data.put("TRXCODE", paymentRequest.getTRXCODE());
            data.put("INSTALLMENTCODE", paymentRequest.getINSTALLMENTCODE());
            data.put("INSTALLMENTSTATUS", paymentRequest.getINSTALLMENTSTATUS());
//            String paymentChannelId = pChannel.getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            data.put("SERVICETRANSACTIONID", ocoId);
            OneCheckoutLogger.log("::DATA to DoProcessInstallment => " + data);
            redirect.setParameters(data);
            return redirect;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            data = null;
            redirect = null;
        }
    }

    public String getCoreMipURL(PaymentChannel pChannel, String step) {

        String CoreUrl = pChannel.getRedirectPaymentUrlMipXml();
        if (step.equalsIgnoreCase("ACS")) {
            CoreUrl = pChannel.getRedirectPaymentUrlMip();
        } else if (step.equalsIgnoreCase("INS") || step.equalsIgnoreCase("INSANDREWARDS") || step.equalsIgnoreCase("REWARDS")) {
            CoreUrl = pChannel.getRedirectPaymentUrlMip();
            if (CoreUrl != null) {
                CoreUrl = CoreUrl.replace("DoThreeDSecure", "DoProcessInstallment");
            }
        } else if (step.equalsIgnoreCase("REDIRECTACQUIRER")) {
            CoreUrl = pChannel.getRedirectPaymentUrlMip();
            if (CoreUrl != null) {
                CoreUrl = CoreUrl.replace("DoThreeDSecure", "BCARedirect");
            }
        }

//        else if(step.equalsIgnoreCase("INSANDREWARDS")){
//            CoreUrl = pChannel.getRedirectPaymentUrlMip();
//        }else if(step.equalsIgnoreCase("REWARDS")){
//            CoreUrl = pChannel.getRedirectPaymentUrlMip();
//        }
        return CoreUrl;
    }

    public OneCheckoutDataPGRedirect parsing3DSecureData(XMLConfiguration xml) {
        OneCheckoutDataPGRedirect redirect = null;
        try {
            String pareq = xml.getString("paReq");
            String MD = xml.getString("md");
            String[] ACS = xml.getStringArray("acsUrl");
            String ACSURL = StringUtils.join(ACS, ",");

            //set data redirect yang akan di kirim ke merchant
            // OneCheckoutDataPGRedirect redirect = null;
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
            return redirect;
        } catch (Throwable th) {
            return redirect;
        }
    }

    public OneCheckoutDataPGRedirect parsingRedirectToAcquirerData(XMLConfiguration xml) {
        OneCheckoutDataPGRedirect redirect = null;
        try {
            /*
             <MPGPaymentResponse>
             <mallId>20184</mallId>
             <chainMallId/>
             <trxCode>57c9d3ccc07de6d3e6e0f2279c6b2c6f6096d2a1</trxCode>
             <cardNumber>5***********8754</cardNumber>
             <invoiceNumber>sndbox_qrHrgLvTr4</invoiceNumber>
             <amount>75000.00</amount>
             <currency>IDR</currency>
             <sessionId>234asdf234</sessionId>
             <paymentDate>20150515073505</paymentDate>
             <result>REDIRECTTOACQUIRER</result>
             <responseCode>98</responseCode>
             <issuerBank>Bank BNI</issuerBank>
             <bank>BCA</bank>
             <redirectUrl>https://migs.mastercard.com.au/vpcpay</redirectUrl>
             <redirectParameter>vpc_AccessCode||DBCA1B06;;vpc_Command||pay;;vpc_OrderInfo||sndbox_qrHrgLvTr4;;vpc_Gateway||ssl;;vpc_CardSecurityCode||869;;vpc_CardExp||1604;;vpc_CardNum||5426400030108754;;vpc_ReturnURL||http://staging.doku.com/MPG/BCARedirect;;vpc_Version||1;;vpc_Card||Mastercard;;vpc_Locale||id;;vpc_Merchant||TEST000294583;;vpc_Amount||75000;;vpc_SecureHash||2E306F2AFF4F4E2E79B635D96E744C63A94007BA18F17D137772824F80640CE3;;vpc_Currency||IDR;;vpc_SecureHashType||SHA256;;vpc_MerchTxnRef||000001</redirectParameter>
             </MPGPaymentResponse>
             */

            String redirectParameter = xml.getString("redirectParameter");
            String[] ACS = xml.getStringArray("redirectUrl");
            String url = StringUtils.join(ACS, ",");

            redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setUrlAction(url);
            redirect.setPageTemplate(redirect.getProgressPage());
            if (redirectParameter != null && !redirectParameter.trim().equals("")) {
                String[] row = redirectParameter.split(";;");
                if (row != null && row.length > 0) {
                    for (String col : row) {
                        String[] val = col.split("\\|\\|");
                        if (val != null && val.length == 2) {
                            redirect.getParameters().put(val[0].trim(), val[1].trim());
                        }
                    }
                }
            }

            //String RedirectBackUrl = config.getString("ONECHECKOUT.RedirectBackURLFromACS", "").trim();// + "?" + mpipassword[0] + "=" + mpipassword[1];
            //OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doPayment RedirectBackUrl => %s", RedirectBackUrl);
            //data.put("TermUrl", RedirectBackUrl); // ini url baliknya apa
            OneCheckoutLogger.log("ACQ URL ==> [redirectUrl]=[" + url + "]");
//            OneCheckoutLogger.log("PARAM ==> [redirectParameter]=[" + redirectParameter + "]");
//            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
//            String card = redirectParameter.substring(redirectParameter.indexOf("vpc_CardNum") + "vpc_CardNum".length() + 2, redirectParameter.indexOf("vpc_CardNum") + "vpc_CardNum".length() + 2 + 16);
//            String maskingCard = base.maskingString(card, "PAN");
//            String expiry = redirectParameter.substring(redirectParameter.indexOf("vpc_CardExp") + "vpc_CardExp".length() + 2, redirectParameter.indexOf("vpc_CardExp") + "vpc_CardExp".length() + 2 + 4);
//            String expiryMasking = base.maskingString(expiry, "EXPIRYDATE");
//            String cvv = redirectParameter.substring(redirectParameter.indexOf("vpc_CardSecurityCode") + "vpc_CardSecurityCode".length() + 2, redirectParameter.indexOf("vpc_CardSecurityCode") + "vpc_CardSecurityCode".length() + 2 + 3);
//            String cvvMasking = base.maskingString(cvv, "CVV");
//            redirectParameter = redirectParameter.replace(card, maskingCard);
//            redirectParameter = redirectParameter.replace(expiry, expiryMasking);
//            redirectParameter = redirectParameter.replace(cvv, cvvMasking);
//            OneCheckoutLogger.log("PARAM ==> [redirectParameter]=[" + redirectParameter + "]");

//            OneCheckoutLogger.log("DATA ==> " + redirect.getParameters().toString());
            return redirect;
        } catch (Throwable th) {
            return redirect;
        }
    }

    public List<OneCheckoutInstallment> parsingInstallmentData(XMLConfiguration xml, OneCheckoutDataHelper oneCheckoutData) {

        List<OneCheckoutInstallment> lstInstallmentStr = new ArrayList<OneCheckoutInstallment>();
        try {
            List<HierarchicalConfiguration> listInstallments = xml.configurationsAt("installments.installment");
            for (HierarchicalConfiguration ins : listInstallments) {

                OneCheckoutInstallment installment = new OneCheckoutInstallment();
                installment.setTrxCode(xml.getString("trxCode") != null ? xml.getString("trxCode") : "");
                installment.setCode(ins.getString("code") != null ? ins.getString("code") : "");
                installment.setDescription(ins.getString("description") != null ? ins.getString("description").replaceAll("\\D+", "") : "");
                installment.setPlan(ins.getString("plan") != null ? ins.getString("plan") : "");
                installment.setInterest(ins.getString("interest") != null ? ins.getString("interest") : "");
                installment.setTenor(ins.getString("tenor") != null ? ins.getString("tenor") : "");
                OneCheckoutLogger.log("Description : " + installment.getDescription());
                lstInstallmentStr.add(installment);
            }

            Collections.sort(lstInstallmentStr, new Comparator<OneCheckoutInstallment>() {
                public int compare(OneCheckoutInstallment o1, OneCheckoutInstallment o2) {
                    Integer desc1 = Integer.parseInt(o1.getDescription());
                    Integer desc2 = Integer.parseInt(o2.getDescription());
                    return desc1.compareTo(desc2);
                }

            });
            return lstInstallmentStr;
        } catch (Throwable th) {
            th.printStackTrace();
            return lstInstallmentStr;
        }
    }

    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml, Merchants merchants) {

        try {

            String result = xml.getString("result") != null ? xml.getString("result").trim() : "";
            String responseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
            String approvalCode = xml.getString("approvalCode") != null ? xml.getString("approvalCode").trim() : "";
//            String issuerBank = xml.getString("issuerBank") != null ? xml.getString("issuerBank").trim() : "";
            String issuerBank = xml.getString("issuerBank") != null ? xml.getString("issuerBank").trim() : xml.getString("bank") != null ? xml.getString("bank") : "";
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
                if (errorCode != null && !errorCode.trim().equals("") && responseCode.equals("")) {
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
////                Batch batch = nl.getBatchByCriterion(c, c1, c2, c3, c4, c5, c6);
//                Batch batch = nl.getBatchByCriterion(c, c1, c2, c3, c4);
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
//                    nl.saveBatch(batch);
//                }
//                trans.setBatchId((int) batch.getBatchId());
//            }
            //required settlement and refund(end)
//            trans.setDokuIssuerBank(bank);
            try {
                OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                if (verifyStatus != null) {
                    notifyRequest.setDFSStatus(verifyStatus);
                    notifyRequest.setDFSScore(verifyScore);
                    notifyRequest.setDFSIId(verifyId);
                    trans.setVerifyId(notifyRequest.getDFSId());
                    trans.setVerifyScore(notifyRequest.getDFSScore());
                    trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                    //              trans.setEduStatus(notifyRequest.getDFSStatus().value());
                } else {
                    notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                    notifyRequest.setDFSScore("-1");
                    notifyRequest.setDFSIId("");
                    trans.setVerifyId("");
                    trans.setVerifyScore(-1);
                    trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    //              trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                }
            } catch (Throwable t) {
                trans.setVerifyScore(0);
            }
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());
            em.merge(trans);
            return trans;
        } catch (Exception ex) {
            return trans;
        }

    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();
            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();

            String wordDoku = super.generateVoidWords(voidRequest, m);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - Checking Hash WORDS");

            if (!wordDoku.equalsIgnoreCase(voidRequest.getWORDS())) {
                voidRequest.setVOIDRESPONSE("FAILED");
                trxHelper.setMessage("VALID");
                trxHelper.setVoidRequest(voidRequest);
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - Hash WORDS doesn't match");

                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - Hash WORDS match");
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);
            if (trans != null) {

                MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());

                if (status == OneCheckoutTransactionStatus.SUCCESS) {

                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - ready to void");

                    StringBuilder sb = new StringBuilder();

                    voidRequest.setApprovalCode(trans.getDokuApprovalCode());
                    //sb.append("TRXCODE=").append(voidRequest.getTRANSIDMERCHANT());
                    sb.append("TRXCODE=").append(trans.getDokuVoidApprovalCode());
                    sb.append("&").append("SERVICEID=").append(mpc.getPaymentChannel().getServiceId());
                    sb.append("&").append("PAYMENTAPPROVALCODE=").append(trans.getDokuApprovalCode());
                    sb.append("&").append("WORDS=").append(super.generateMPGVoidWords(voidRequest, mpc));

//                    sb.append("&").append("USERNAME=").append(m.getMerchantName());
//                    sb.append("&").append("IP=").append(trans.getSystemSession());
//                    sb.append("&").append("REASON=").append(voidRequest.getVOIDREASON());
                    OneCheckoutLogger.log("VOID PARAM : " + sb.toString());
                    InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                    // 
                    OneCheckoutLogger.log("XML response FROM MPG = > " + inetResp.getMsgResponse());
                    voidRequest = MPGVoidReponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
                    if (voidRequest.getVoidStatus().equalsIgnoreCase("SUCCESS")) {

//                        trans.setDokuVoidApprovalCode(voidRequest.getApprovalCode());
                        trans.setDokuVoidDatetime(new Date());
                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("SUCCESS");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - update trx as voided");

                    } else {

                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setDokuVoidDatetime(new Date());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse() + ";" + voidRequest.getMESSAGE());
                        } else {
                            voidRequest.setVOIDRESPONSE("FAILED" + ";" + voidRequest.getMESSAGE());
                        }

                        OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - update response code void");
                    }

                    em.merge(trans);

                } else if (status == OneCheckoutTransactionStatus.VOIDED) {

                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(super.generateVoidResponse(trans, "VOIDED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - trx already voided");

                } else {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(super.generateVoidResponse(trans, "FAILED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("FAILED" + ";" + voidRequest.getMESSAGE());
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - trx can't be voided");
                }

            } else {

                if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                    voidRequest.setVOIDRESPONSE(super.generateVoidResponse(null, ""));
                } else {
                    voidRequest.setVOIDRESPONSE("FAILED" + ";" + "Transaction Not Found");
                }

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - trx isn't found");
            }

            trxHelper.setMessage("VALID");
            trxHelper.setVoidRequest(voidRequest);

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doVoid - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;

        } catch (Throwable th) {
            th.printStackTrace();
            voidRequest.setVOIDRESPONSE("FAILED");
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

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
            Transactions trans = (Transactions) obj[1];

            if (redirectRequest == null) {
                trans = trxHelper.getTransactions();
                statusCode = trans.getDokuResponseCode();
            }

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
                    redirect.setAMOUNT(amount);
                    redirect.setTRANSIDMERCHANT(invoiceNo);
                    redirect.setSTATUSCODE(statusCode);
                    redirect.setUrlAction(m.getMerchantRedirectUrl());
                    redirect.setPageTemplate("redirect.html");
                }
                trxHelper.setRedirectDoku(redirectRequest);
            } else if (redirectRequest != null && redirectRequest.getFLAG().equalsIgnoreCase("2")) {
                statusCode = trans.getDokuResponseCode();
                redirectRequest.setSTATUSCODE(statusCode);
                trxHelper.setRedirectDoku(redirectRequest);

                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
                // redirect = redirectToMerchant(redirect, redirectRequest, trans, m, trxHelper, data);
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
//                BigDecimal displayAmount = trans.getIncAmount() - BigDecimal.valueOf(trxHelper.getAmountRedeemed());
                Double incAmount = Double.parseDouble("" + trans.getIncAmount());
                Double displayAmount = incAmount;
                //block pointRedemmed
//                if (trxHelper.getAmountRedeemed() != null && trxHelper.getAmountRedeemed() > 0) {
//                    incAmount = Double.parseDouble("" + trans.getIncAmount());
//                    displayAmount = incAmount - trxHelper.getAmountRedeemed();
//                    resultData.put("PURCHASEAMOUNT", OneCheckoutVerifyFormatData.moneyFormat.format(incAmount));
//                }
                resultData.put("DISPLAYAMOUNT", OneCheckoutVerifyFormatData.moneyFormat.format(displayAmount));

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

//                    OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
//                    
//                    if(paymentRequest != null && paymentRequest.getREDEMPTIONSTATUS().equals("TRUE")){
//                        OneCheckoutRewards rewards = trxHelper.getPaymentPageObject().getOneCheckoutRewards();
//                        OneCheckoutLogger.log("redemption true");
//                        if(rewards != null){
//                            int point = rewards.getPointRedeemed();
//                            double amounts = rewards.getAmountRedeemed();
//                            OneCheckoutLogger.log("GoBack point redeemed := "+point);
//                            OneCheckoutLogger.log("GoBack amount redeemed := "+amounts);
//                            resultData.put("REDEMPTIONSTATUS", Boolean.TRUE);
//                            resultData.put("POINTREDEEMED", point);
//                            resultData.put("AMOUNTREDEEMED", amounts);
//                            OneCheckoutLogger.log("include point");
//                        }
//                    }
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
                    redirect = super.addDWRegistrationPage(trxHelper, redirect, channel, trans.getMerchantPaymentChannel(), trans);
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

            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        return super.doGetEDSDataBase(trxHelper, acq);
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        return super.doUpdateEDSStatusBase(trxHelper, acq);

    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
//        OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

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

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREMPG(trans, paymentChannel);

                HashMap<String, String> params = super.getData(trans);
                trxHelper.setMessage("VALID");
                params.put("PAYMENTCODE", "");
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

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));

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
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal - T2: %d update transaction", (System.currentTimeMillis() - t1));

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getTYPE().equalsIgnoreCase("REVERSAL")) {
                status = OneCheckoutTransactionStatus.REVERSED;
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal - %s", trans.getDokuResultMessage());
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
//            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
            HashMap<String, String> params = super.getData(trans);
            params.put("PAYMENTCODE", "");

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal : statusNotify : %s", resp);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal - T4: %d update trx record", (System.currentTimeMillis() - t1));

            // proses parsing ack from merchant, then save it to database
            em.merge(trans);
            notifyRequest.setACKNOWLEDGE(resp);
            trxHelper.setNotifyRequest(notifyRequest);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doReversal - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean doMPGReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal invoice number : " + invoiceNo + "   sessionId : " + sessionId);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal : Transaction is null");
//                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
//
//                trxHelper.setNotifyRequest(notifyRequest);
                return false;

            }

            String word = super.generateMPGReversalWords(trxHelper, trans);
            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal : WORDS doesn't match !");
//                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
//
//                trxHelper.setNotifyRequest(notifyRequest);
                return false;
            }

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getTYPE().equalsIgnoreCase("REVERSAL")) {
                status = OneCheckoutTransactionStatus.REVERSED;
            }

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal - %s", trans.getDokuResultMessage());

            HashMap<String, String> params = new HashMap<String, String>();

            params.put("SERVICEID", "1");
            params.put("MALLID", "" + trxHelper.getMallId());
            params.put("CHAINMALLID", "" + trxHelper.getChainMerchantId());
            params.put("INVOICENUMBER", "" + trans.getTransactionsId());
            params.put("WORDS", "" + notifyRequest.getWORDS());
            if (trans.getRates() != null) {
                params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
                params.put("CURRENCY", trans.getIncPurchasecurrency());
            } else {
                params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
                params.put("CURRENCY", trans.getIncCurrency());
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal - T2: %d update trx record", (System.currentTimeMillis() - t1));

            String mpgReversalUrl = config.getString("RECUR.URL.REVERSAL", "").trim();
            String data_encode = this.createParamsHTTP(params);

            OneCheckoutLogger.log("Fetch URL     : %s", mpgReversalUrl);
            OneCheckoutLogger.log("Merchant Data : %s", data_encode);

            OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
            String MPGResponse = oneCheckoutChannelBase.postMIP(params, mpgReversalUrl, acq);

            OneCheckoutLogger.log("Response MPG  : \n%s", MPGResponse);

            em.merge(trans);

            params = super.getData(trans);
            params.put("PAYMENTCODE", "");

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal : statusNotify : %s", resp);

//            notifyRequest.setACKNOWLEDGE(MPGResponse);
//            trxHelper.setNotifyRequest(notifyRequest);
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMPGReversal - T3: %d Finish process", (System.currentTimeMillis() - t1));
            return true;
        } catch (Exception ex) {
            return false;
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
            data.put("FUELSURCHARGE", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()));

            OneCheckoutLogger.log("CURENCY " + paymentRequest.getCURRENCY());
            Currency currency = queryHelper.getCurrencyByCode(paymentRequest.getCURRENCY());

            data.put("CURRENCY", currency != null ? currency.getAlpha3Code() : "");
            data.put("SESSIONID", paymentRequest.getSESSIONID());
            data.put("WORDS", super.generateDokuMPGWords(paymentRequest, mpc, currency.getAlpha3Code()));
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
            data.put("CustIP", paymentRequest.getCUSTIP());
            data.put("CUSTOMERIP", paymentRequest.getCUSTIP());
            //DEVICEID RED
            data.put("DEVICEID", paymentRequest.getDEVICEID());

//            data.put("IP",paymentRequest.getCUSTIP() != null ?  paymentRequest.getCUSTIP() : "");
//            data.put("USERNAME",paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");
//            data.put("USERNAME",mpc.getMerchants().getMerchantName() != null ? mpc.getMerchants().getMerchantName() : "");            
//            data.put("REASON",paymentRequest.getPAYREASON() != null ? paymentRequest.getPAYREASON() : "");
            if (paymentRequest.getPAYMENTTYPE() != null && paymentRequest.getPAYMENTTYPE().trim().equalsIgnoreCase("AUTHORIZATION")) {
                data.put("TRANSACTIONTYPE", "A");
            } else {
                if (pChannel.getPaymentChannelId().equalsIgnoreCase(OneCheckoutPaymentChannel.MOTO.value())) {
                    data.put("TRANSACTIONTYPE", "M");
                }

                String acquirerCode = paymentRequest.getINSTALLMENT_ACQUIRER() != null ? paymentRequest.getINSTALLMENT_ACQUIRER().trim() : "";
                String planId = paymentRequest.getPROMOID() != null ? paymentRequest.getPROMOID().trim() : "";
                String tenor = paymentRequest.getTENOR() != null ? paymentRequest.getTENOR().trim() : "";
                if (!acquirerCode.equals("") && !planId.equals("") && !tenor.equals("")) {
                    data.put("INSTALLMENTACQUIRERID", paymentRequest.getINSTALLMENT_ACQUIRER());
                    data.put("INSTALLMENTPLANID", paymentRequest.getPROMOID());
                    data.put("INSTALLMENTTENOR", paymentRequest.getTENOR());
                    if (pChannel.getPaymentChannelId().equalsIgnoreCase(OneCheckoutPaymentChannel.MOTO.value())) {
                        data.put("TRANSACTIONTYPE", "L");
                    } else {
                        if (paymentRequest.getPAYMENTTYPE() != null && paymentRequest.getPAYMENTTYPE().trim().equalsIgnoreCase("OFFUSINSTALLMENT")) {
                            data.put("TRANSACTIONTYPE", "O");
                        } else {
                            data.put("TRANSACTIONTYPE", "I");
                        }
                    }
                }
            }

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
                data.put("bookingCode", paymentRequest.getBOOKINGCODE());
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
                                    for (int j = 0; j < names.length; j++) {
                                        if (j > 0) {
                                            if (lastName.equals("")) {
                                                lastName = names[j];
                                            } else {
                                                lastName += " " + names[j];
                                            }
                                        }
                                    }
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
            data.put("CARDNUMBER", base.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
            data1.put("EXPIRYMONTH", paymentRequest.getEXPIRYDATE().substring(0, 2));
            data1.put("EXPIRYYEAR", paymentRequest.getEXPIRYDATE().substring(2, 4));
            data.put("SERVICETRANSACTIONID", ocoId);
            OneCheckoutLogger.log("DATA to IPG CIP => " + data.toString());

            data.put("CARDNUMBER", paymentRequest.getCARDNUMBER());
            data.put("EXPIRYMONTH", paymentRequest.getEXPIRYDATE().substring(2, 4));
            data.put("EXPIRYYEAR", paymentRequest.getEXPIRYDATE().substring(0, 2));
            data.put("CVV2", paymentRequest.getCVV2());
//            String paymentChannelId = pChannel.getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);

            redirect.setParameters(data);

            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMIPPayment - T0: %d Start", (System.currentTimeMillis() - t1));
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
                boolean checkStatusToCore = false;
                String resultPayment = "";
                OneCheckoutDataPGRedirect redirect = null;
                trans = transacBean.saveTransactions(trxHelper, mpc);
                trxHelper.setOcoId(trans.getOcoId());
                String data = createPostMIP(paymentRequest, pChannel, mpc, merchant, trans.getOcoId());
                try {
                    resultPayment = super.postMIP(data, pChannel.getRedirectPaymentUrlMipXml(), pChannel);
                    if (resultPayment == null || resultPayment.trim().equals("") || resultPayment.trim().equalsIgnoreCase("ERROR")) {
                        checkStatusToCore = true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    checkStatusToCore = true;
                }
                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMIPPayment - T1: %d Start", (System.currentTimeMillis() - t1));
                if (checkStatusToCore) {
                    // CHECK STATUS TO CORE
                    Thread.sleep(5 * 1000);
                    trans = super.CheckStatusCOREMPG(trans, pChannel);
                    if (trans.getDokuResponseCode() != null && trans.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value())) {
                        trans = super.reversalToCOREMPG(trans);
                    }
                    OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMIPPayment - Checking status to CORE", (System.currentTimeMillis() - t1));
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
                    HashMap<String, String> params = super.getData(trans);
                    params.put("PAYMENTCODE", "");
                    String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
                    //trans = super.notifyStatusMerchant(trans,trxHelper.getPaymentChannel());

                    em.merge(trans);
                    trxHelper.setTransactions(trans);
                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
                    trxHelper.setMessage("VALID");
                    return trxHelper;
                }

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
                String verifyStatusRiskEngine = "";
                String eduStatus = "";
                String batchId = "";
                String batchNumber = "";
                String tid = "";
                String settlementDate = "";
                String acquirerId = "";
//                String eduStatus = "";
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

                    //required batch settlement(start)
                    batchId = xml.getString("batchId") != null ? xml.getString("batchId").trim() : "";
                    batchNumber = xml.getString("batchNumber") != null ? xml.getString("batchNumber").trim() : "";
                    tid = xml.getString("tid") != null ? xml.getString("tid").trim() : "";
                    acquirerId = xml.getString("bankId") != null ? xml.getString("bankId").trim() : "";
                    settlementDate = xml.getString("settlementDate") != null ? xml.getString("settlementDate").trim() : "";
                    //required batch settlement(end)

                    if (responseCode == null || responseCode.trim().equals("")) {
                        if (errorCode != null && !errorCode.trim().equals("")) {
                            if (errorCode.trim().equalsIgnoreCase("XX07")) {
                                responseCode = "BB";
                            } else {
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
                    trans.setTrxCodeCore(trxCode);

                    //required settlement and refund(start)
//                    if (!batchId.equals("") && !batchNumber.equals("") && !mid.equals("") && !tid.equals("") && !acquirerId.equals("") && merchant.getMerchantEscrow() != null && !merchant.getMerchantEscrow()) {
//                        Criterion c = Restrictions.eq("coreBatchId", batchId);
//                        Criterion c1 = Restrictions.eq("coreBatchNumber", batchNumber);
//                        Criterion c2 = Restrictions.eq("mid", mid);
//                        Criterion c3 = Restrictions.eq("tid", tid);
//                        Criterion c4 = Restrictions.eq("acquirerId", acquirerId);
////                        Criterion c5 = Restrictions.eq("mall_id", trans.getIncMallid());
////                        Criterion c6 = Restrictions.eq("chainMerchant", trans.getIncChainmerchant());
//
//                        Batch batch = queryHelper.getBatchByCriterion(c, c1, c2, c3, c4);
////                        Batch batch = queryHelper.getBatchByCriterion(c, c1, c2, c3, c4);
////                        Batch batch1 = new Batch();
//                        if (batch == null) {
//                            batch = new Batch();
//                            batch.setBatchId(nl.getNextVal(SeqBatchIdNextval.class).longValue());
//                            batch.setCoreBatchId(batchId);
//                            batch.setCoreBatchNumber(batchNumber);
//                            batch.setType(OneCheckOutBatchType.MPG.value());
//                            batch.setTid(tid);
//                            batch.setMid(mid);
//                            batch.setAcquirerId(acquirerId);
//                            batch.setStatus(OneCheckOutSettlementStatus.OPEN.value());
//                            batch.setMallId(trans.getIncMallid());
//                            batch.setChainMerchant(trans.getIncChainmerchant());
//                            if (!settlementDate.equals("")) {
//                                DateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH:MM:ss.SSS");
//                                Date d2 = dateFormat.parse(settlementDate);
//                                batch.setSettlementDate(d2);
//                            }
//                            queryHelper.saveBatch(batch);
//                        }
//                        trans.setBatchId((int) batch.getBatchId());
//                    }
                    //required settlement and refund(end)
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
                                    eduStatus = OneCheckoutDFSStatus.REJECT.name();
                                } else if (trans.getVerifyStatus() == OneCheckoutDFSStatus.MEDIUM_RISK.value()) {
                                    verifyStatusRiskEngine = OneCheckoutDFSStatus.REVIEW.name();
                                    eduStatus = OneCheckoutDFSStatus.PENDING.name();
                                } else if (trans.getVerifyStatus() == OneCheckoutDFSStatus.LOW_RISK.value()) {
                                    verifyStatusRiskEngine = OneCheckoutDFSStatus.APPROVE.name();
                                    eduStatus = OneCheckoutDFSStatus.APPROVE.name();
                                } else {
                                    verifyStatusRiskEngine = OneCheckoutDFSStatus.NA.name();
                                }
                            }
                            //     trans.setEduStatus(notifyRequest.getDFSStatus().value());
                        } else {
                            notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                            notifyRequest.setDFSScore("-1");
                            notifyRequest.setDFSIId("");
                            trans.setVerifyId("");
                            trans.setVerifyScore(-1);
                            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                            //      trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                        }
                    } catch (Throwable t) {
                        trans.setVerifyScore(0);
                    }

                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    if (result != null && result.equalsIgnoreCase("Success")) {
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                    }

                } else {
                    trans.setDokuResponseCode("00TO");
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                }
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);

                String hashFormat = merchant.getMerchantShaFormat();
                if (hashFormat == null || hashFormat.equals("") || !hashFormat.equalsIgnoreCase("SHA2") || !hashFormat.equalsIgnoreCase("HMACSHA256") || !hashFormat.equalsIgnoreCase("HMACSHA1")) {
                    hashFormat = null;
                }
                words = HashWithSHA1.doHashing(amount + paymentRequest.getMALLID() + trxHelper.getMerchant().getMerchantHashPassword() + trans.getIncTransidmerchant() + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + verifyStatus, "SHA2", null);
//                words = HashWithSHA1.doHashing(amount + paymentRequest.getMALLID() + trxHelper.getMerchant().getMerchantHashPassword() + trans.getIncTransidmerchant() + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + verifyStatus, hashFormat, null);
                OneCheckoutLogger.log("COMPONENT WORDS TO MERCHANT [ " + amount + " " + paymentRequest.getMALLID() + " " + trxHelper.getMerchant().getMerchantHashPassword() + " " + trans.getIncTransidmerchant() + " " + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + " " + verifyStatus);
                String xmlMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<PAYMENT_STATUS>"
                        + "<AMOUNT>" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()) + "</AMOUNT>"
                        + "<TRANSIDMERCHANT>" + trans.getIncTransidmerchant() + "</TRANSIDMERCHANT>"
                        + "<WORDS>" + words + "</WORDS>"
                        + "<RESPONSECODE>" + trans.getDokuResponseCode() + "</RESPONSECODE>"
                        + "<APPROVALCODE>" + approvalCode + "</APPROVALCODE>"
                        + "<RESULTMSG>" + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + "</RESULTMSG>"
                        + "<PAYMENTCHANNEL>" + pChannel.getPaymentChannelId() + "</PAYMENTCHANNEL>"
                        + "<PAYMENTCODE></PAYMENTCODE>"
                        + "<SESSIONID>" + paymentRequest.getSESSIONID() + "</SESSIONID>"
                        + "<BANK>" + issuerBank + "</BANK>"
                        + "<MID>" + mid + "</MID>"
                        + "<MCN>" + maskedCard + "</MCN>"
                        + "<PAYMENTDATETIME>" + paymentDateTime + "</PAYMENTDATETIME>"
                        + "<VERIFYID>" + verifyId + "</VERIFYID>"
                        + "<VERIFYSCORE>" + verifyScore + "</VERIFYSCORE>"
                        + //                  "<VERIFYSTATUS>" + verifyStatus + "</VERIFYSTATUS>" +
                        "<VERIFYSTATUS>" + verifyStatusRiskEngine + "</VERIFYSTATUS>"
                        + "<EDUSTATUS>" + eduStatus + "</EDUSTATUS>"
                        + "</PAYMENT_STATUS>";
                trxHelper.setMessage(xmlMessage);

                OneCheckoutLogger.log("OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean.doMIPPayment - T2: %d Start", (System.currentTimeMillis() - t1));
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
            data += URLEncoder.encode("FUELSURCHARGE", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()), "UTF-8") + "&";
            Currency currency = queryHelper.getCurrencyByCode(paymentRequest.getCURRENCY());
            data += URLEncoder.encode("CURRENCY", "UTF-8") + "=" + URLEncoder.encode(currency != null ? currency.getAlpha3Code() : "", "UTF-8") + "&";
            data += URLEncoder.encode("SESSIONID", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getSESSIONID(), "UTF-8") + "&";
            data += URLEncoder.encode("WORDS", "UTF-8") + "=" + URLEncoder.encode(super.generateDokuMPGWords(paymentRequest, mpc, currency.getAlpha3Code()), "UTF-8") + "&";
            data += URLEncoder.encode("BASKET", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBASKET(), "UTF-8") + "&";
            data += URLEncoder.encode("BILLINGADDRESS", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getADDRESS(), "UTF-8") + "&";
            data += URLEncoder.encode("HOMEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getHOMEPHONE(), "UTF-8") + "&";
            data += URLEncoder.encode("CONTACTABLENAME", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getNAME(), "UTF-8") + "&";
            data += URLEncoder.encode("OFFICEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getMOBILEPHONE(), "UTF-8") + "&";
            data += URLEncoder.encode("BIRTHDATE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBIRTHDATE(), "UTF-8") + "&";
            if (paymentRequest.getBILLINGDESCRIPTION() != null && !paymentRequest.getBILLINGDESCRIPTION().trim().equals("")) {
                data += URLEncoder.encode("BILLINGSTATEMENTDESCRIPTION", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBILLINGDESCRIPTION().trim(), "UTF-8") + "&";
            }

            String transactionType = "S";
            if (pChannel.getPaymentChannelId().equalsIgnoreCase(OneCheckoutPaymentChannel.MOTO.value())) {
                transactionType = "M";
            }
            // INSTALLMENT DETAIL
            String acquirerCode = paymentRequest.getINSTALLMENT_ACQUIRER() != null ? paymentRequest.getINSTALLMENT_ACQUIRER().trim() : "";
            String planId = paymentRequest.getPROMOID() != null ? paymentRequest.getPROMOID().trim() : "";
            String tenor = paymentRequest.getTENOR() != null ? paymentRequest.getTENOR().trim() : "";
            if (!acquirerCode.equals("") && !planId.equals("") && !tenor.equals("")) {
                data += URLEncoder.encode("INSTALLMENTACQUIRERID", "UTF-8") + "=" + URLEncoder.encode(acquirerCode, "UTF-8") + "&";
                data += URLEncoder.encode("INSTALLMENTPLANID", "UTF-8") + "=" + URLEncoder.encode(planId, "UTF-8") + "&";
                data += URLEncoder.encode("INSTALLMENTTENOR", "UTF-8") + "=" + URLEncoder.encode(tenor, "UTF-8") + "&";
                if (pChannel.getPaymentChannelId().equalsIgnoreCase(OneCheckoutPaymentChannel.MOTO.value())) {
                    transactionType = "L";
                } else {
                    transactionType = "I";
                    if (paymentRequest.getPAYMENTTYPE() != null && paymentRequest.getPAYMENTTYPE().trim().equalsIgnoreCase("OFFUSINSTALLMENT")) {
                        transactionType = "O";
                    }
                }
            }
            if (paymentRequest.getPAYMENTTYPE() != null && paymentRequest.getPAYMENTTYPE().equals("AUTHORIZATION")) {
                transactionType = "A";
            }
            data += URLEncoder.encode("TRANSACTIONTYPE", "UTF-8") + "=" + URLEncoder.encode(transactionType, "UTF-8") + "&";

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
            String vEResStatus = paymentRequest.getVERESSTATUS() != null ? paymentRequest.getVERESSTATUS().trim() : "";
            OneCheckoutLogger.log("paymentRequest.getVERESSTATUS[" + vEResStatus + "]");
            data += URLEncoder.encode("VERESSTATUS", "UTF-8") + "=" + URLEncoder.encode(vEResStatus, "UTF-8") + "&";
            OneCheckoutLogger.log("paymentRequest.getECI[" + paymentRequest.getECI() + "]");
            if (paymentRequest.getECI() != null) {
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
            data1 += URLEncoder.encode("EXPIRYMONTH", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(2, 4), "UTF-8") + "&";
            data1 += URLEncoder.encode("EXPIRYYEAR", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(0, 2), "UTF-8") + "&";
            data1 += URLEncoder.encode("CVV2", "UTF-8") + "=" + URLEncoder.encode("***", "UTF-8");
            data += URLEncoder.encode("SERVICETRANSACTIONID", "UTF-8") + "=" + URLEncoder.encode("" + ocoId, "UTF-8") + "&";

            OneCheckoutLogger.log("DATA to IPG MIP=> " + data.toString());

            // CARD DETAIL
            data += URLEncoder.encode("CARDNUMBER", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCARDNUMBER(), "UTF-8") + "&";
            data += URLEncoder.encode("EXPIRYMONTH", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(2, 4), "UTF-8") + "&";
            data += URLEncoder.encode("EXPIRYYEAR", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE().substring(0, 2), "UTF-8") + "&";
            data += URLEncoder.encode("CVV2", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCVV2(), "UTF-8");

            return data;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataHelper doInstallmentDataPage(OneCheckoutDataHelper trxHelper, XMLConfiguration xml) {
        String tenorResponse = xml.getString("tenor") != null ? xml.getString("tenor") : "";
        String interestResponse = xml.getString("interest") != null ? xml.getString("interest") : "";
        String planIdResponse = xml.getString("planId") != null ? xml.getString("planId") : "";

        trxHelper.setTenorResponse(tenorResponse);
        trxHelper.setInterestResponse(interestResponse);
        trxHelper.setPlanIdResponse(planIdResponse);

        return trxHelper;
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

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        HashMap<String, Object> responseError = new HashMap<String, Object>();
        String resultRefund = "";
        OneCheckoutDataHelper checkoutDataHelper = new OneCheckoutDataHelper();

        try {

////            <MPGRefundResponse>
////  <mallId>1</mallId>
////  <trxCode>d38fde95d1a1c826d905b6310af6f5a9e427674c</trxCode>
////  <cardNumber>4***********3142</cardNumber>
////  <invoiceNumber>D1ET4VC2163005</invoiceNumber>
////  <transactionAmount>1452.10</transactionAmount>
////  <refundAmount>1452.10</refundAmount>
////  <currency>PGK</currency>
////  <result>SUCCESS</result>
////  <responseCode>00</responseCode>
////  <approvalCode>050815</approvalCode>
////  <message>REFUND APPROVED</message>
////  <bank>BSP</bank>
////  <mid>4001PGK00000005</mid>
////  <tid>401PGK05</tid>
////</MPGRefundResponse>
            OneCheckoutLogger.log(":::: Process Do refund To Core :::::(Start)");

            Transactions transactions = refundHelper.getTransactions();
            String param = createPostRefund(transactions, merchantPaymentChannel, refundHelper);
////            String Url = "http://luna2.nsiapay.com/MPG/ServiceRequestRefund";

            String Url = OneCheckoutProperties.getOneCheckoutConfig().getString("onecheckoutv2.url.refund.core.mpg" + refundHelper.getRefundType());
            resultRefund = super.PostTOMerchant(param, Url, merchantPaymentChannel);

            if (resultRefund == null || resultRefund.trim().equals("") || resultRefund.trim().equalsIgnoreCase("ERROR")) {

                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.TIMEOUT.value());
                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.TIMEOUT.name());
                refundHelper.setMapResultRefund(responseError);
                return checkoutDataHelper;
            }

            if (resultRefund != null && resultRefund.length() > 0 && !resultRefund.equalsIgnoreCase("ERROR")) {
                OneCheckoutLogger.log("Response refund from MPG = " + resultRefund);

                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(resultRefund);
                xml.load(sr);

                String status = xml.getString("result") != null ? xml.getString("result") : "";
                if (status.equalsIgnoreCase("SUCCESS")) {

                    transacBean.saveTrannsactionRefund(transactions, refundHelper.getAmount());

//                    result.put("res_response_code", OneCheckoutErrorMessage.SUCCESS.value());
//                    result.put("res_response_msg", OneCheckoutErrorMessage.SUCCESS.name());
                    resultRefund = parsingResponseMPGToEscrow(resultRefund, refundHelper);
                    checkoutDataHelper.setMessage(resultRefund);
                    return checkoutDataHelper;
                } else {
//                    result.put("RESPONSECODE", OneCheckoutErrorMessage.FAILED_REFUND.value());
//                    result.put("RESPONSEMSG", OneCheckoutErrorMessage.FAILED_REFUND.name());
//                    return result;
                    resultRefund = parsingResponseMPGToEscrow(resultRefund, refundHelper);
                    checkoutDataHelper.setMessage(resultRefund);
                    return checkoutDataHelper;
                }
            } else {
                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.FAILED_REFUND.value());
                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.FAILED_REFUND.name());
                refundHelper.setMapResultRefund(responseError);
                checkoutDataHelper.setRefundHelper(refundHelper);
                return checkoutDataHelper;
            }

        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("Error in -doRefund- ==> " + th.getMessage());
            responseError.put("RESPONSECODE", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.value());
            responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.name());
            refundHelper.setMapResultRefund(responseError);
            checkoutDataHelper.setRefundHelper(refundHelper);
        }
        return checkoutDataHelper;

    }

    public String parsingResponseMPGToEscrow(String responseMPG, RefundHelper refundHelper) {
        String result = null;
        try {
            StringBuilder builder = new StringBuilder();
            StringReader reader = new StringReader(responseMPG);
            XMLConfiguration xml = new XMLConfiguration();
            xml.load(reader);
            String responseCode = xml.getString("responseCode");
            String responseMSG = xml.getString("result");

            if (responseCode != null && responseCode.equals("00") && responseMSG != null && responseMSG.equalsIgnoreCase("SUCCESS")) {
                Transactions transactions = refundHelper.getTransactions();
                builder.append("<?xml version=\"1.0\"?>");
                builder.append("<REFUND_RESPONSE>");
                builder.append("<RESPONSECODE>0000</RESPONSECODE>");
                builder.append("<RESPONSEMSG>SUCCESS</RESPONSEMSG>");
                builder.append("<TRANSIDMERCHANT>" + transactions.getIncTransidmerchant() + "</TRANSIDMERCHANT>");
                builder.append("<REFNUM>" + transactions.getDokuApprovalCode() + "</REFNUM>");
                builder.append("<SESSIONID>" + transactions.getIncSessionid() + "</SESSIONID>");
                builder.append("<REFIDMERCHANT>" + refundHelper.getRefIdMerchant() + "</REFIDMERCHANT>");
                builder.append("</REFUND_RESPONSE>");

            } else {
                Transactions transactions = refundHelper.getTransactions();
                builder.append("<?xml version=\"1.0\"?>");
                builder.append("<REFUND_RESPONSE>");
                builder.append("<RESPONSECODE>"+OneCheckoutErrorMessage.FAILED_REFUND.value()+"</RESPONSECODE>");
                builder.append("<RESPONSEMSG>FAILED</RESPONSEMSG>");
                builder.append("<TRANSIDMERCHANT>" + transactions.getIncTransidmerchant() + "</TRANSIDMERCHANT>");
                builder.append("<REFNUM>" + transactions.getDokuApprovalCode() + "</REFNUM>");
                builder.append("<SESSIONID>" + transactions.getIncSessionid() + "</SESSIONID>");
                builder.append("<REFIDMERCHANT>" + refundHelper.getRefIdMerchant() + "</REFIDMERCHANT>");
                builder.append("</REFUND_RESPONSE>");
            }

//            builder.append("<?xml version=\"1.0\"?>");
//            builder.append("<").append(processName.toUpperCase()).append(">");
//            for (int i = 0; i < params.size(); i++) {
//                String key = params.keySet().toArray()[i] + "";
//                String value = params.get(key) + "";
//                builder.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
//
//            }
//            builder.append("</").append(processName.toUpperCase()).append(">");
            result = builder + "";

        } catch (Exception e) {
        }
        return result;
    }

    public String createPostRefund(Transactions transactions, MerchantPaymentChannel merchantPaymentChannel, RefundHelper helper) {

        String data = "";
        try {

            data += URLEncoder.encode("SERVICEID", "UTF-8") + "=" + URLEncoder.encode("" + "1", "UTF-8") + "&";
            data += URLEncoder.encode("SERVICETRANSACTIONID", "UTF-8") + "=" + URLEncoder.encode(transactions.getOcoId(), "UTF-8") + "&";
            data += URLEncoder.encode("TRXCODE", "UTF-8") + "=" + URLEncoder.encode(transactions.getTrxCodeCore(), "UTF-8") + "&";
            if (helper.getRefundType().equals("PD") || helper.getRefundType().equals("PC")) {
                data += URLEncoder.encode("AMOUNT", "UTF-8") + "=" + URLEncoder.encode(helper.getAmount(), "UTF-8") + "&";
            }
            data += URLEncoder.encode("PAYMENTAPPROVALCODE", "UTF-8") + "=" + URLEncoder.encode(transactions.getDokuApprovalCode(), "UTF-8") + "&";
            data += URLEncoder.encode("WORDS", "UTF-8") + "=" + URLEncoder.encode(generateDokuRefundMPGWords(merchantPaymentChannel, transactions, helper), "UTF-8") + "&";
            data += URLEncoder.encode("REASON", "UTF-8") + "=" + URLEncoder.encode(helper.getReason(), "UTF-8") + "&";
            OneCheckoutLogger.log("PARAM REFUND TO CORE => " + data);
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("error in createPostSettlementMPG = " + th.getMessage());

        }

        return data;
    }

    public String generateDokuRefundMPGWords(MerchantPaymentChannel mpc, Transactions transactions, RefundHelper helper) {

        String sharedKey = null;
        String sharedKeyMasking = null;
        OneCheckoutBaseRules baseRules = new OneCheckoutBaseRules();
        try {

            StringBuilder word = new StringBuilder();

            if (helper.getRefundType().equalsIgnoreCase("FULL")) {
                word.append(transactions.getTrxCodeCore());
                word.append(transactions.getOcoId());
                word.append(mpc.getMerchantPaymentChannelHash());
                word.append(transactions.getDokuApprovalCode());
            } else if (helper.getRefundType().equalsIgnoreCase("PC")) {
                word.append(transactions.getTrxCodeCore());
                word.append(transactions.getOcoId());
                word.append(mpc.getMerchantPaymentChannelHash());
                word.append(helper.getAmount());
                word.append(transactions.getDokuApprovalCode());
            } else if (helper.getRefundType().equals("PD")) {
                word.append(transactions.getTrxCodeCore());
                word.append(transactions.getOcoId());
                word.append(mpc.getMerchantPaymentChannelHash());
                word.append(transactions.getDokuApprovalCode());
                word.append(helper.getAmount());
            }

            sharedKey = mpc.getMerchantPaymentChannelHash();
            sharedKeyMasking = baseRules.maskingString(sharedKey, "SHAREDKEY");

////            OneCheckoutLogger.log("WORD in Clear Text : %s", word.toString());
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            OneCheckoutLogger.log("WORD in Clear Text :" + word.replace(word.indexOf(sharedKey), word.indexOf(sharedKey) + sharedKey.length(), sharedKeyMasking) + "]");
            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }
}
