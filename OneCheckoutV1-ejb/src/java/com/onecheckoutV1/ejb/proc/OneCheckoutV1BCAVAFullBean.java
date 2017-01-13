/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDOKUVerifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMerchantCategory;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutReturnType;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import com.onechekoutv1.dto.TransactionsDataAirlines;
import com.onechekoutv1.dto.TransactionsDataNonAirlines;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author margono
 */
@Stateless
public class OneCheckoutV1BCAVAFullBean extends OneCheckoutChannelBase implements OneCheckoutV1BCAVAFullBeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;

    private boolean ableToReversal = false;

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();

            String merchantCode = verifyRequest.getMALLID();
            MerchantPaymentChannel mpc = queryHelper.getMerchantPaymentChannel(merchantCode, acq);
            Merchants merchant = mpc.getMerchants();
            trxHelper.setMerchant(merchant);
            String payCode = verifyRequest.getVANUMBER();

            queryHelper.updateVirtualAccountExpiredTransactions(payCode, merchantCode, acq, mpc.getInvExpiredInMinutes());

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice - querying transaction");
            Transactions trans = null; //queryHelper.getPermataVATransactionBy(payCode, merchantCode, acq);

            if (merchant.getMerchantReusablePaycode() != null && merchant.getMerchantReusablePaycode() == Boolean.TRUE) {

                trans = queryHelper.getInitVATransactionBy(payCode, merchantCode, acq);
                verifyRequest.setBANK("BCA");
                trxHelper.setVerifyRequest(verifyRequest);

                trxHelper.setCIPMIP(OneCheckoutMethod.MIP);
                trxHelper.setPaymentChannel(OneCheckoutPaymentChannel.BCAVAFull);

                if (trans != null) {
                    trans = transacBean.createReuseVATransactions(trxHelper, mpc, trans);
                }

            } else if (merchant.getMerchantDirectInquiry() != null && merchant.getMerchantDirectInquiry()) {

                String transExist = super.doDirectInquiry(trxHelper, mpc, verifyRequest);
                OneCheckoutLogger.log("value from directInquiry = " + transExist);
                if (transExist.equalsIgnoreCase("2")) {

                    OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice - T2: transaction already paid");

                    String ack = emptyInquiryData(verifyRequest, transExist);

                    verifyRequest.setACKNOWLEDGE(ack);

                    trxHelper.setVerifyRequest(verifyRequest);

                    return trxHelper;

                } else if (transExist.equalsIgnoreCase("0")) {

                    OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice - T2: transaction is not found");

                    String ack = emptyInquiryData(verifyRequest, transExist);

                    verifyRequest.setACKNOWLEDGE(ack);

                    trxHelper.setVerifyRequest(verifyRequest);

                    return trxHelper;

                } else if (transExist.equalsIgnoreCase("EMPTY")) {
                    trans = null;

                } else {
                    // harusnya masuk disini
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                    trans.setMerchantPaymentChannel(mpc);
                    OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice : Inquiry Successfull : %s");
                }

            } else {
                trans = queryHelper.getInquiryVATransactionBy(payCode, merchantCode, acq);
            }

            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice : Transaction is null");
                String ack = emptyInquiryData(verifyRequest, "0");
                verifyRequest.setACKNOWLEDGE(ack);

                trxHelper.setVerifyRequest(verifyRequest);

                return trxHelper;
            }
            String currency = null;
            if (trans.getIncCurrency() != null) {
                currency = trans.getIncCurrency();
            } else if (trans.getIncPurchasecurrency() != null) {
                currency = trans.getIncPurchasecurrency();
            } else {
                currency = "360";
            }
            Currency resultCurrency = queryHelper.getCurrencyByCode(currency);

            trans.setDokuInquiryInvoiceDatetime(new Date());
            trans.setDokuHostRefNum(verifyRequest.getTRACENO());
            trans.setTransactionsState(OneCheckoutTransactionState.INCOMING.value());
            trans = nl.refreshAmountBaseNewRate(trans);
            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice - T2: update transaction");
            String ack = createInquiryResponseXML(verifyRequest, trans, resultCurrency);

            em.merge(trans);

            //OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice - Notify XML : " + ack);
            verifyRequest.setACKNOWLEDGE(ack);

            trxHelper.setVerifyRequest(verifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInquiryInvoice - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            String ack = emptyInquiryData(trxHelper.getVerifyRequest(), "0");
            trxHelper.getVerifyRequest().setACKNOWLEDGE(ack);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    private String emptyInquiryData(OneCheckoutDOKUVerifyData verifyRequest, String errorCode) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<transactionInfo>");
        sb.append("<isTransExist>").append("0").append("</isTransExist>");

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
    private String createInquiryResponseXML(OneCheckoutDOKUVerifyData verifyRequest, Transactions trans, Currency currency) {

        String ack = "";
        try {

            StringBuilder sb = new StringBuilder();

            if (trans != null) {
                String vaNumber = verifyRequest.getVANUMBER() != null ? verifyRequest.getVANUMBER().trim() : "";
                String transIdMerchant = trans.getIncTransidmerchant() != null ? trans.getIncTransidmerchant().trim() : "";
                String sessionId = trans.getIncSessionid() != null ? trans.getIncSessionid().trim() : "";
                String customerName = trans.getIncName() != null ? trans.getIncName().trim() : "";
                String customerEmail = trans.getIncEmail() != null ? trans.getIncEmail().trim() : "";

                sb.append("<?xml version=\"1.0\"?>");
                sb.append("<transactionInfo>");
                sb.append("<isTransExist>").append("1").append("</isTransExist>");
                sb.append("<vaNumber>").append(URLEncoder.encode(vaNumber, "UTF-8")).append("</vaNumber>");
                sb.append("<transactionNo>").append(URLEncoder.encode(transIdMerchant, "UTF-8")).append("</transactionNo>");
                sb.append("<sessionId>").append(URLEncoder.encode(sessionId, "UTF-8")).append("</sessionId>");
                sb.append("<customerName>").append(URLEncoder.encode(customerName, "UTF-8")).append("</customerName>");
                sb.append("<customerPhone>").append(URLEncoder.encode("", "UTF-8")).append("</customerPhone>");
                sb.append("<customerEmail>").append(URLEncoder.encode(customerEmail, "UTF-8")).append("</customerEmail>");
                sb.append("<transactionDate>").append(URLEncoder.encode(OneCheckoutVerifyFormatData.klikbca_datetimeFormat.format(trans.getIncRequestdatetime()), "UTF-8")).append("</transactionDate>");
                sb.append("<amount>").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount())).append("</amount>");
                sb.append("<currency>").append(URLEncoder.encode(currency.getAlpha3Code(), "UTF-8")).append("</currency>");

                OneCheckoutMerchantCategory cat = OneCheckoutMerchantCategory.findType(trans.getMerchantPaymentChannel().getMerchants().getMerchantCategory());
                if (cat == OneCheckoutMerchantCategory.AIRLINE) {

                    TransactionsDataAirlines airlines = queryHelper.getTransactionAirlinesBy(trans);

                    OneCheckoutReturnType rtype = OneCheckoutReturnType.findType(airlines.getIncFlighttype());

                    String basket = "Booking Code - " + airlines.getIncBookingcode();
                    sb.append("<description>").append(URLEncoder.encode(basket, "UTF-8")).append("</description>");

                } else if (cat == OneCheckoutMerchantCategory.NONAIRLINE) {

                    TransactionsDataNonAirlines nonAirlines = queryHelper.getTransactionNonAirlinesBy(trans);
                    sb.append("<description>").append(URLEncoder.encode(nonAirlines.getIncBasket(), "UTF-8")).append("</description>");
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

                sb.append("<customerName>").append(URLEncoder.encode(name, "UTF-8")).append("</customerName>");

                String paymentChannelId = trans.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId();
                //String ocoId = this.generateOcoId(paymentChannelId);
                String ocoId = trans.getOcoId();
                sb.append("<ocoId>").append(URLEncoder.encode(ocoId, "UTF-8")).append("</ocoId>");
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

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getPermataVATransactionBy(notifyRequest.getBCA_VANUMBER(), notifyRequest.getBCA_MALLID(), notifyRequest.getTRANSIDMERCHANT(), acq, notifyRequest.getBCA_STEP());
//            OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();

            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateBCANotifyRequestWords(notifyRequest, trans);

            OneCheckoutLogger.log("HASH PASSWORDS FROM ONECHECKOUT : %s", word);
            OneCheckoutLogger.log("HASH PASSWORDS FROM CHANNEL     : %s", notifyRequest.getWORDS());
            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.SUCCESS;

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - t1));
            trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
            trans.setDokuIssuerBank(notifyRequest.getBANK());

            trans.setDokuInvokeStatusDatetime(new Date());
            //String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
//            trans.setDokuResponseCode(notifyRequest.getRESPONSECODE());
            trans.setDokuResponseCode(OneCheckoutErrorMessage.SUCCESS.value());
            trans.setDokuHostRefNum(notifyRequest.getHOSTREFNUM());
            trans.setDokuResult(notifyRequest.getRESULT());
            trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setIncAmount(BigDecimal.valueOf(notifyRequest.getAMOUNT()));

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);

            params.put("PAYMENTCODE", trans.getAccountId());

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus : statusNotify : %s", resp);

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));

            // proses parsing ack from merchant, then save it to database
            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = queryHelper.getPermataVATransactionBy(notifyRequest.getBCA_VANUMBER(), notifyRequest.getBCA_MALLID(), notifyRequest.getTRANSIDMERCHANT(), acq, notifyRequest.getBCA_STEP());

            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateBCANotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid - T2: %d update transaction", (System.currentTimeMillis() - t1));
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
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

            HashMap<String, String> params = super.getData(trans);

            params.put("PAYMENTCODE", trans.getAccountId());

            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);
            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid : statusNotify : %s", resp);

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid - T4: %d update trx record", (System.currentTimeMillis() - t1));

            em.merge(trans);

            notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1BCAVAFullBean.doVoid - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
