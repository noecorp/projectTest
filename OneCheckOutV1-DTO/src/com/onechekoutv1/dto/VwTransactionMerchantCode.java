/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onechekoutv1.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
@Entity
@Table(name = "vw_transaction_merchant_code")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "VwTransactionMerchantCode.findAll", query = "SELECT v FROM VwTransactionMerchantCode v"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByMerchantCode", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.merchantCode = :merchantCode"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByTransactionsId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.transactionsId = :transactionsId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncMallid", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incMallid = :incMallid"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncChainmerchant", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incChainmerchant = :incChainmerchant"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncAmount", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incAmount = :incAmount"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncPurchaseamount", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incPurchaseamount = :incPurchaseamount"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncTransidmerchant", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incTransidmerchant = :incTransidmerchant"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncWords", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incWords = :incWords"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncRequestdatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incRequestdatetime = :incRequestdatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncCurrency", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incCurrency = :incCurrency"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncPurchasecurrency", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incPurchasecurrency = :incPurchasecurrency"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncSessionid", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incSessionid = :incSessionid"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncEmail", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incEmail = :incEmail"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncPaymentchannel", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incPaymentchannel = :incPaymentchannel"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncInstallmentAcquirer", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incInstallmentAcquirer = :incInstallmentAcquirer"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncTenor", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incTenor = :incTenor"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncPromoid", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incPromoid = :incPromoid"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncAdditionalInformation", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incAdditionalInformation = :incAdditionalInformation"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByAccountId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.accountId = :accountId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findBySystemSession", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.systemSession = :systemSession"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByTransactionsStatus", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.transactionsStatus = :transactionsStatus"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByTransactionsState", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.transactionsState = :transactionsState"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByTransactionsDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.transactionsDatetime = :transactionsDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuInquiryInvoiceDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuInquiryInvoiceDatetime = :dokuInquiryInvoiceDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuInvokeStatusDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuInvokeStatusDatetime = :dokuInvokeStatusDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuResponseCode", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuResponseCode = :dokuResponseCode"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuApprovalCode", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuApprovalCode = :dokuApprovalCode"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuResultMessage", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuResultMessage = :dokuResultMessage"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuIssuerBank", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuIssuerBank = :dokuIssuerBank"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuResult", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuResult = :dokuResult"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByEduGetdataDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.eduGetdataDatetime = :eduGetdataDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByEduPassVoidDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.eduPassVoidDatetime = :eduPassVoidDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByEduStatus", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.eduStatus = :eduStatus"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByEduReason", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.eduReason = :eduReason"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByRedirectDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.redirectDatetime = :redirectDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findBySystemMessage", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.systemMessage = :systemMessage"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByVerifyScore", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.verifyScore = :verifyScore"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByVerifyStatus", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.verifyStatus = :verifyStatus"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByVerifyId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.verifyId = :verifyId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByVerifyDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.verifyDatetime = :verifyDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuVoidResponseCode", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuVoidResponseCode = :dokuVoidResponseCode"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuVoidApprovalCode", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuVoidApprovalCode = :dokuVoidApprovalCode"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuVoidDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuVoidDatetime = :dokuVoidDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncName", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incName = :incName"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByMerchantPaymentChannelId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.merchantPaymentChannelId = :merchantPaymentChannelId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDokuHostRefNum", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.dokuHostRefNum = :dokuHostRefNum"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByReconcileDatetime", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.reconcileDatetime = :reconcileDatetime"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByInc3dsecStatus", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.inc3dsecStatus = :inc3dsecStatus"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncLiability", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incLiability = :incLiability"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByIncCustomerId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.incCustomerId = :incCustomerId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByTokenId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.tokenId = :tokenId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByDeviceId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.deviceId = :deviceId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByRateId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.rateId = :rateId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByConvertedAmount", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.convertedAmount = :convertedAmount"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByMerchantNotificationResponse", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.merchantNotificationResponse = :merchantNotificationResponse"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByPaymentChannelId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.paymentChannelId = :paymentChannelId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByPaymentChannelName", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.paymentChannelName = :paymentChannelName"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByPaymentChannelProcessor", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.paymentChannelProcessor = :paymentChannelProcessor"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByPaymentChannelStatus", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.paymentChannelStatus = :paymentChannelStatus"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByRedirectPaymentUrlMip", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.redirectPaymentUrlMip = :redirectPaymentUrlMip"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByVoidUrl", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.voidUrl = :voidUrl"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByRedirectPaymentUrlCip", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.redirectPaymentUrlCip = :redirectPaymentUrlCip"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByRedirectPaymentUrlMipXml", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.redirectPaymentUrlMipXml = :redirectPaymentUrlMipXml"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByServiceId", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.serviceId = :serviceId"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByCheckStatusUrl", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.checkStatusUrl = :checkStatusUrl"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByPaymentChannelConnectionTimeout", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.paymentChannelConnectionTimeout = :paymentChannelConnectionTimeout"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByPaymentChannelReadTimeout", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.paymentChannelReadTimeout = :paymentChannelReadTimeout"),
    @NamedQuery(name = "VwTransactionMerchantCode.findByReversalPaymentUrl", query = "SELECT v FROM VwTransactionMerchantCode v WHERE v.reversalPaymentUrl = :reversalPaymentUrl")})
public class VwTransactionMerchantCode implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column(name = "merchant_code")
    @Id
    private Integer merchantCode;
    @Column(name = "transactions_id")
    private BigInteger transactionsId;
    @Column(name = "inc_mallid")
    private Integer incMallid;
    @Column(name = "inc_chainmerchant")
    private Integer incChainmerchant;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "inc_amount")
    private BigDecimal incAmount;
    @Column(name = "inc_purchaseamount")
    private BigDecimal incPurchaseamount;
    @Column(name = "inc_transidmerchant")
    private String incTransidmerchant;
    @Column(name = "inc_words")
    private String incWords;
    @Column(name = "inc_requestdatetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date incRequestdatetime;
    @Column(name = "inc_currency")
    private String incCurrency;
    @Column(name = "inc_purchasecurrency")
    private String incPurchasecurrency;
    @Column(name = "inc_sessionid")
    private String incSessionid;
    @Column(name = "inc_email")
    private String incEmail;
    @Column(name = "inc_paymentchannel")
    private String incPaymentchannel;
    @Column(name = "inc_installment_acquirer")
    private String incInstallmentAcquirer;
    @Column(name = "inc_tenor")
    private String incTenor;
    @Column(name = "inc_promoid")
    private String incPromoid;
    @Column(name = "inc_additional_information")
    private String incAdditionalInformation;
    @Column(name = "account_id")
    private String accountId;
    @Column(name = "system_session")
    private String systemSession;
    @Column(name = "transactions_status")
    private Character transactionsStatus;
    @Column(name = "transactions_state")
    private Character transactionsState;
    @Column(name = "transactions_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date transactionsDatetime;
    @Column(name = "doku_inquiry_invoice_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dokuInquiryInvoiceDatetime;
    @Column(name = "doku_invoke_status_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dokuInvokeStatusDatetime;
    @Column(name = "doku_response_code")
    private String dokuResponseCode;
    @Column(name = "doku_approval_code")
    private String dokuApprovalCode;
    @Column(name = "doku_result_message")
    private String dokuResultMessage;
    @Column(name = "doku_issuer_bank")
    private String dokuIssuerBank;
    @Column(name = "doku_result")
    private String dokuResult;
    @Column(name = "edu_getdata_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eduGetdataDatetime;
    @Column(name = "edu_pass_void_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eduPassVoidDatetime;
    @Column(name = "edu_status")
    private Character eduStatus;
    @Column(name = "edu_reason")
    private String eduReason;
    @Column(name = "redirect_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date redirectDatetime;
    @Column(name = "system_message")
    private String systemMessage;
    @Column(name = "verify_score")
    private Integer verifyScore;
    @Column(name = "verify_status")
    private Character verifyStatus;
    @Column(name = "verify_id")
    private String verifyId;
    @Column(name = "verify_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date verifyDatetime;
    @Column(name = "doku_void_response_code")
    private String dokuVoidResponseCode;
    @Column(name = "doku_void_approval_code")
    private String dokuVoidApprovalCode;
    @Column(name = "doku_void_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dokuVoidDatetime;
    @Column(name = "inc_name")
    private String incName;
    @Column(name = "merchant_payment_channel_id")
    private Integer merchantPaymentChannelId;
    @Column(name = "doku_host_ref_num")
    private String dokuHostRefNum;
    @Column(name = "reconcile_datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reconcileDatetime;
    @Column(name = "inc_3dsec_status")
    private String inc3dsecStatus;
    @Column(name = "inc_liability")
    private String incLiability;
    @Column(name = "inc_customer_id")
    private String incCustomerId;
    @Column(name = "token_id")
    private String tokenId;
    @Column(name = "device_id")
    private String deviceId;
    @Column(name = "rate_id")
    private Integer rateId;
    @Column(name = "converted_amount")
    private BigDecimal convertedAmount;
    @Column(name = "merchant_notification_response")
    private String merchantNotificationResponse;
    @Column(name = "payment_channel_id")
    private String paymentChannelId;
    @Column(name = "payment_channel_name")
    private String paymentChannelName;
    @Column(name = "payment_channel_processor")
    private String paymentChannelProcessor;
    @Column(name = "payment_channel_status")
    private Boolean paymentChannelStatus;
    @Column(name = "redirect_payment_url_mip")
    private String redirectPaymentUrlMip;
    @Column(name = "void_url")
    private String voidUrl;
    @Column(name = "redirect_payment_url_cip")
    private String redirectPaymentUrlCip;
    @Column(name = "redirect_payment_url_mip_xml")
    private String redirectPaymentUrlMipXml;
    @Column(name = "service_id")
    private String serviceId;
    @Column(name = "check_status_url")
    private String checkStatusUrl;
    @Column(name = "payment_channel_connection_timeout")
    private Integer paymentChannelConnectionTimeout;
    @Column(name = "payment_channel_read_timeout")
    private Integer paymentChannelReadTimeout;
    @Column(name = "reversal_payment_url")
    private String reversalPaymentUrl;

    public VwTransactionMerchantCode() {
    }

    public Integer getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(Integer merchantCode) {
        this.merchantCode = merchantCode;
    }

    public BigInteger getTransactionsId() {
        return transactionsId;
    }

    public void setTransactionsId(BigInteger transactionsId) {
        this.transactionsId = transactionsId;
    }

    public Integer getIncMallid() {
        return incMallid;
    }

    public void setIncMallid(Integer incMallid) {
        this.incMallid = incMallid;
    }

    public Integer getIncChainmerchant() {
        return incChainmerchant;
    }

    public void setIncChainmerchant(Integer incChainmerchant) {
        this.incChainmerchant = incChainmerchant;
    }

    public BigDecimal getIncAmount() {
        return incAmount;
    }

    public void setIncAmount(BigDecimal incAmount) {
        this.incAmount = incAmount;
    }

    public BigDecimal getIncPurchaseamount() {
        return incPurchaseamount;
    }

    public void setIncPurchaseamount(BigDecimal incPurchaseamount) {
        this.incPurchaseamount = incPurchaseamount;
    }

    public String getIncTransidmerchant() {
        return incTransidmerchant;
    }

    public void setIncTransidmerchant(String incTransidmerchant) {
        this.incTransidmerchant = incTransidmerchant;
    }

    public String getIncWords() {
        return incWords;
    }

    public void setIncWords(String incWords) {
        this.incWords = incWords;
    }

    public Date getIncRequestdatetime() {
        return incRequestdatetime;
    }

    public void setIncRequestdatetime(Date incRequestdatetime) {
        this.incRequestdatetime = incRequestdatetime;
    }

    public String getIncCurrency() {
        return incCurrency;
    }

    public void setIncCurrency(String incCurrency) {
        this.incCurrency = incCurrency;
    }

    public String getIncPurchasecurrency() {
        return incPurchasecurrency;
    }

    public void setIncPurchasecurrency(String incPurchasecurrency) {
        this.incPurchasecurrency = incPurchasecurrency;
    }

    public String getIncSessionid() {
        return incSessionid;
    }

    public void setIncSessionid(String incSessionid) {
        this.incSessionid = incSessionid;
    }

    public String getIncEmail() {
        return incEmail;
    }

    public void setIncEmail(String incEmail) {
        this.incEmail = incEmail;
    }

    public String getIncPaymentchannel() {
        return incPaymentchannel;
    }

    public void setIncPaymentchannel(String incPaymentchannel) {
        this.incPaymentchannel = incPaymentchannel;
    }

    public String getIncInstallmentAcquirer() {
        return incInstallmentAcquirer;
    }

    public void setIncInstallmentAcquirer(String incInstallmentAcquirer) {
        this.incInstallmentAcquirer = incInstallmentAcquirer;
    }

    public String getIncTenor() {
        return incTenor;
    }

    public void setIncTenor(String incTenor) {
        this.incTenor = incTenor;
    }

    public String getIncPromoid() {
        return incPromoid;
    }

    public void setIncPromoid(String incPromoid) {
        this.incPromoid = incPromoid;
    }

    public String getIncAdditionalInformation() {
        return incAdditionalInformation;
    }

    public void setIncAdditionalInformation(String incAdditionalInformation) {
        this.incAdditionalInformation = incAdditionalInformation;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSystemSession() {
        return systemSession;
    }

    public void setSystemSession(String systemSession) {
        this.systemSession = systemSession;
    }

    public Character getTransactionsStatus() {
        return transactionsStatus;
    }

    public void setTransactionsStatus(Character transactionsStatus) {
        this.transactionsStatus = transactionsStatus;
    }

    public Character getTransactionsState() {
        return transactionsState;
    }

    public void setTransactionsState(Character transactionsState) {
        this.transactionsState = transactionsState;
    }

    public Date getTransactionsDatetime() {
        return transactionsDatetime;
    }

    public void setTransactionsDatetime(Date transactionsDatetime) {
        this.transactionsDatetime = transactionsDatetime;
    }

    public Date getDokuInquiryInvoiceDatetime() {
        return dokuInquiryInvoiceDatetime;
    }

    public void setDokuInquiryInvoiceDatetime(Date dokuInquiryInvoiceDatetime) {
        this.dokuInquiryInvoiceDatetime = dokuInquiryInvoiceDatetime;
    }

    public Date getDokuInvokeStatusDatetime() {
        return dokuInvokeStatusDatetime;
    }

    public void setDokuInvokeStatusDatetime(Date dokuInvokeStatusDatetime) {
        this.dokuInvokeStatusDatetime = dokuInvokeStatusDatetime;
    }

    public String getDokuResponseCode() {
        return dokuResponseCode;
    }

    public void setDokuResponseCode(String dokuResponseCode) {
        this.dokuResponseCode = dokuResponseCode;
    }

    public String getDokuApprovalCode() {
        return dokuApprovalCode;
    }

    public void setDokuApprovalCode(String dokuApprovalCode) {
        this.dokuApprovalCode = dokuApprovalCode;
    }

    public String getDokuResultMessage() {
        return dokuResultMessage;
    }

    public void setDokuResultMessage(String dokuResultMessage) {
        this.dokuResultMessage = dokuResultMessage;
    }

    public String getDokuIssuerBank() {
        return dokuIssuerBank;
    }

    public void setDokuIssuerBank(String dokuIssuerBank) {
        this.dokuIssuerBank = dokuIssuerBank;
    }

    public String getDokuResult() {
        return dokuResult;
    }

    public void setDokuResult(String dokuResult) {
        this.dokuResult = dokuResult;
    }

    public Date getEduGetdataDatetime() {
        return eduGetdataDatetime;
    }

    public void setEduGetdataDatetime(Date eduGetdataDatetime) {
        this.eduGetdataDatetime = eduGetdataDatetime;
    }

    public Date getEduPassVoidDatetime() {
        return eduPassVoidDatetime;
    }

    public void setEduPassVoidDatetime(Date eduPassVoidDatetime) {
        this.eduPassVoidDatetime = eduPassVoidDatetime;
    }

    public Character getEduStatus() {
        return eduStatus;
    }

    public void setEduStatus(Character eduStatus) {
        this.eduStatus = eduStatus;
    }

    public String getEduReason() {
        return eduReason;
    }

    public void setEduReason(String eduReason) {
        this.eduReason = eduReason;
    }

    public Date getRedirectDatetime() {
        return redirectDatetime;
    }

    public void setRedirectDatetime(Date redirectDatetime) {
        this.redirectDatetime = redirectDatetime;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }

    public Integer getVerifyScore() {
        return verifyScore;
    }

    public void setVerifyScore(Integer verifyScore) {
        this.verifyScore = verifyScore;
    }

    public Character getVerifyStatus() {
        return verifyStatus;
    }

    public void setVerifyStatus(Character verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public String getVerifyId() {
        return verifyId;
    }

    public void setVerifyId(String verifyId) {
        this.verifyId = verifyId;
    }

    public Date getVerifyDatetime() {
        return verifyDatetime;
    }

    public void setVerifyDatetime(Date verifyDatetime) {
        this.verifyDatetime = verifyDatetime;
    }

    public String getDokuVoidResponseCode() {
        return dokuVoidResponseCode;
    }

    public void setDokuVoidResponseCode(String dokuVoidResponseCode) {
        this.dokuVoidResponseCode = dokuVoidResponseCode;
    }

    public String getDokuVoidApprovalCode() {
        return dokuVoidApprovalCode;
    }

    public void setDokuVoidApprovalCode(String dokuVoidApprovalCode) {
        this.dokuVoidApprovalCode = dokuVoidApprovalCode;
    }

    public Date getDokuVoidDatetime() {
        return dokuVoidDatetime;
    }

    public void setDokuVoidDatetime(Date dokuVoidDatetime) {
        this.dokuVoidDatetime = dokuVoidDatetime;
    }

    public String getIncName() {
        return incName;
    }

    public void setIncName(String incName) {
        this.incName = incName;
    }

    public Integer getMerchantPaymentChannelId() {
        return merchantPaymentChannelId;
    }

    public void setMerchantPaymentChannelId(Integer merchantPaymentChannelId) {
        this.merchantPaymentChannelId = merchantPaymentChannelId;
    }

    public String getDokuHostRefNum() {
        return dokuHostRefNum;
    }

    public void setDokuHostRefNum(String dokuHostRefNum) {
        this.dokuHostRefNum = dokuHostRefNum;
    }

    public Date getReconcileDatetime() {
        return reconcileDatetime;
    }

    public void setReconcileDatetime(Date reconcileDatetime) {
        this.reconcileDatetime = reconcileDatetime;
    }

    public String getInc3dsecStatus() {
        return inc3dsecStatus;
    }

    public void setInc3dsecStatus(String inc3dsecStatus) {
        this.inc3dsecStatus = inc3dsecStatus;
    }

    public String getIncLiability() {
        return incLiability;
    }

    public void setIncLiability(String incLiability) {
        this.incLiability = incLiability;
    }

    public String getIncCustomerId() {
        return incCustomerId;
    }

    public void setIncCustomerId(String incCustomerId) {
        this.incCustomerId = incCustomerId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getRateId() {
        return rateId;
    }

    public void setRateId(Integer rateId) {
        this.rateId = rateId;
    }

    public BigDecimal getConvertedAmount() {
        return convertedAmount;
    }

    public void setConvertedAmount(BigDecimal convertedAmount) {
        this.convertedAmount = convertedAmount;
    }

    public String getMerchantNotificationResponse() {
        return merchantNotificationResponse;
    }

    public void setMerchantNotificationResponse(String merchantNotificationResponse) {
        this.merchantNotificationResponse = merchantNotificationResponse;
    }

    public String getPaymentChannelId() {
        return paymentChannelId;
    }

    public void setPaymentChannelId(String paymentChannelId) {
        this.paymentChannelId = paymentChannelId;
    }

    public String getPaymentChannelName() {
        return paymentChannelName;
    }

    public void setPaymentChannelName(String paymentChannelName) {
        this.paymentChannelName = paymentChannelName;
    }

    public String getPaymentChannelProcessor() {
        return paymentChannelProcessor;
    }

    public void setPaymentChannelProcessor(String paymentChannelProcessor) {
        this.paymentChannelProcessor = paymentChannelProcessor;
    }

    public Boolean getPaymentChannelStatus() {
        return paymentChannelStatus;
    }

    public void setPaymentChannelStatus(Boolean paymentChannelStatus) {
        this.paymentChannelStatus = paymentChannelStatus;
    }

    public String getRedirectPaymentUrlMip() {
        return redirectPaymentUrlMip;
    }

    public void setRedirectPaymentUrlMip(String redirectPaymentUrlMip) {
        this.redirectPaymentUrlMip = redirectPaymentUrlMip;
    }

    public String getVoidUrl() {
        return voidUrl;
    }

    public void setVoidUrl(String voidUrl) {
        this.voidUrl = voidUrl;
    }

    public String getRedirectPaymentUrlCip() {
        return redirectPaymentUrlCip;
    }

    public void setRedirectPaymentUrlCip(String redirectPaymentUrlCip) {
        this.redirectPaymentUrlCip = redirectPaymentUrlCip;
    }

    public String getRedirectPaymentUrlMipXml() {
        return redirectPaymentUrlMipXml;
    }

    public void setRedirectPaymentUrlMipXml(String redirectPaymentUrlMipXml) {
        this.redirectPaymentUrlMipXml = redirectPaymentUrlMipXml;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCheckStatusUrl() {
        return checkStatusUrl;
    }

    public void setCheckStatusUrl(String checkStatusUrl) {
        this.checkStatusUrl = checkStatusUrl;
    }

    public Integer getPaymentChannelConnectionTimeout() {
        return paymentChannelConnectionTimeout;
    }

    public void setPaymentChannelConnectionTimeout(Integer paymentChannelConnectionTimeout) {
        this.paymentChannelConnectionTimeout = paymentChannelConnectionTimeout;
    }

    public Integer getPaymentChannelReadTimeout() {
        return paymentChannelReadTimeout;
    }

    public void setPaymentChannelReadTimeout(Integer paymentChannelReadTimeout) {
        this.paymentChannelReadTimeout = paymentChannelReadTimeout;
    }

    public String getReversalPaymentUrl() {
        return reversalPaymentUrl;
    }

    public void setReversalPaymentUrl(String reversalPaymentUrl) {
        this.reversalPaymentUrl = reversalPaymentUrl;
    }
    
}
