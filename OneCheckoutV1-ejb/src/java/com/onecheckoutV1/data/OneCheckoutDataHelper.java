/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidDOKURedirectDataException;
import com.onecheckoutV1.ejb.helper.MIPInquiryResponse;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.template.CustomerInfoEmailObject;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMerchantCategory;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Rates;
import com.onechekoutv1.dto.Transactions;
import java.io.Serializable;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutDataHelper implements Serializable {

    private static final long serialVersionUID = 1L;
    private OneCheckoutPaymentChannel paymentChannel;
    private OneCheckoutMerchantCategory merchantCategory;
    private Merchants merchant;
    private String message;
    private int mallId;
    private int paymentChannelCode;
    private int chainMerchantId;
    private Long trxId;
    private OneCheckoutMethod CIPMIP;
    private String systemSession;
    private String systemMessage;
    private String dwRegName = "";
    private String dwRegEmail = "";
    private String dwRegPhone = "";
    private boolean dwRegStatus = false;
    
    private OneCheckoutPaymentRequest paymentRequest;
    private OneCheckoutPaymentResponse paymentResponse;
    private OneCheckoutDataPGRedirect redirect;
    private OneCheckoutDOKUVerifyData verifyRequest;
    private OneCheckoutDOKUNotifyData notifyRequest;
    private OneCheckoutRedirectData redirectDoku;
    
    private OneCheckoutVoidRequest voidRequest;
    private OneCheckoutCheckStatusData checkStatusRequest;
    private OneCheckoutEDSGetData edsGetData;
    private OneCheckoutEDSUpdateStatusData edsUpdateStatus;

    private OneCheckoutTodayKlikBCARequest todayKlikBCA;
    private OneCheckoutDataTodayTransactions trxDataDisplay;

    //DOKUPAY
    private MIPInquiryResponse mIPInquiryResponse;

    private PaymentPageObject paymentPageObject;
    private CustomerInfoEmailObject customerInfoEmailObject;
    private String flag = "";

    private OneCheckoutCyberSourceData CyberSourceData;

    // Plugin oh Plugin -_____-"
    private Transactions transactions;
    private Rates rates;
    private OneCheckoutTransactionStatus status;
    private OneCheckoutNotifyStatusResponse notifyResponse;
    private OneCheckoutStepNotify stepNotify;
    private OneCheckoutErrorMessage errorMessage;
    
    private String tenorResponse;
    private String interestResponse;
    private String planIdResponse;
    
    private String ocoId;
    private RefundHelper refundHelper;
    
//    private Double amountRedeemed;
    
    public OneCheckoutDataHelper() {
        paymentChannel = null;
        merchantCategory = null;
        merchant = null;
        message = null;
        mallId = 0;
        chainMerchantId = 0;
        CIPMIP = null;
        systemSession = null;
        systemMessage = null;

        paymentRequest = null;
        paymentResponse = null;
        redirect = null;
//        verifyRequest = null;
        notifyRequest = null;
        voidRequest = null;
        checkStatusRequest = null;
        edsGetData = null;
        edsUpdateStatus = null;
        dwRegEmail = "";
        dwRegName = "";
        dwRegPhone = "";
        dwRegStatus = false;
    }

    public String getTenorResponse() {
        return tenorResponse;
    }

    public void setTenorResponse(String tenorResponse) {
        this.tenorResponse = tenorResponse;
    }

    public String getInterestResponse() {
        return interestResponse;
    }

    public void setInterestResponse(String interestResponse) {
        this.interestResponse = interestResponse;
    }

    public String getPlanIdResponse() {
        return planIdResponse;
    }

    public void setPlanIdResponse(String planIdResponse) {
        this.planIdResponse = planIdResponse;
    }
    
    public boolean isDwRegStatus() {
        return dwRegStatus;
    }

    public void setDwRegStatus(boolean dwRegStatus) {
        this.dwRegStatus = dwRegStatus;
    }

    public String getDwRegEmail() {
        return dwRegEmail;
    }

    public void setDwRegEmail(String dwRegEmail) {
        this.dwRegEmail = dwRegEmail;
    }

    public String getDwRegName() {
        return dwRegName;
    }

    public void setDwRegName(String dwRegName) {
        this.dwRegName = dwRegName;
    }

    public String getDwRegPhone() {
        return dwRegPhone;
    }

    public void setDwRegPhone(String dwRegPhone) {
        this.dwRegPhone = dwRegPhone;
    }

    public MIPInquiryResponse getMIPInquiryResponse() {
        return mIPInquiryResponse;
    }

    public void setMIPInquiryResponse(MIPInquiryResponse mIPInquiryResponse) {
        this.mIPInquiryResponse = mIPInquiryResponse;
    }

    /**
     * @return the paymentChannel
     */
    public OneCheckoutPaymentChannel getPaymentChannel() {
        return paymentChannel;
    }

    /**
     * @param paymentChannel the paymentChannel to set
     */
    public void setPaymentChannel(OneCheckoutPaymentChannel paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    /**
     * @return the merchantCategory
     */
    public OneCheckoutMerchantCategory getMerchantCategory() {
        return merchantCategory;
    }

    /**
     * @param merchantCategory the merchantCategory to set
     */
    public void setMerchantCategory(OneCheckoutMerchantCategory merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    /**
     * @return the merchant
     */
    public Merchants getMerchant() {
        return merchant;
    }

    /**
     * @param merchant the merchant to set
     */
    public void setMerchant(Merchants merchant) {
        this.merchant = merchant;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the mallId
     */
    public int getMallId() {
        return mallId;
    }

    /**
     * @param mallId the mallId to set
     */
    public void setMallId(int mallId) {
        this.mallId = mallId;
    }

    /**
     * @return the ChainMerchantId
     */
    public int getChainMerchantId() {
        return chainMerchantId;
    }

    /**
     * @param ChainMerchantId the ChainMerchantId to set
     */
    public void setChainMerchantId(int ChainMerchantId) {
        this.chainMerchantId = ChainMerchantId;
    }

    /**
     * @return the CIPMIP
     */
    public OneCheckoutMethod getCIPMIP() {
        return CIPMIP;
    }

    /**
     * @param CIPMIP the CIPMIP to set
     */
    public void setCIPMIP(OneCheckoutMethod CIPMIP) {
        this.CIPMIP = CIPMIP;
    }

    /**
     * @return the systemSession
     */
    public String getSystemSession() {
        return systemSession;
    }

    /**
     * @param systemSession the systemSession to set
     */
    public void setSystemSession(String systemSession) {
        this.systemSession = systemSession;
    }

    /**
     * @return the paymentRequest
     */
    public OneCheckoutPaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    /**
     * @param paymentRequest the paymentRequest to set
     */
    public void setPaymentRequest(OneCheckoutPaymentRequest paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    /**
     * @return the paymentResponse
     */
    public OneCheckoutPaymentResponse getPaymentResponse() {
        return paymentResponse;
    }

    /**
     * @param paymentResponse the paymentResponse to set
     */
    public void setPaymentResponse(OneCheckoutPaymentResponse paymentResponse) {
        this.paymentResponse = paymentResponse;
    }

    /**
     * @return the redirect
     */
    public OneCheckoutDataPGRedirect getRedirect() {
        return redirect;
    }

    /**
     * @param redirect the redirect to set
     */
    public void setRedirect(OneCheckoutDataPGRedirect redirect) {
        this.redirect = redirect;
    }


    /**
     * @return the notifyRequest
     */
    public OneCheckoutDOKUNotifyData getNotifyRequest() {
        return notifyRequest;
    }

    /**
     * @param notifyRequest the notifyRequest to set
     */
    public void setNotifyRequest(OneCheckoutDOKUNotifyData notifyRequest) {
        this.notifyRequest = notifyRequest;
    }

    /**
     * @return the voidRequest
     */
    public OneCheckoutVoidRequest getVoidRequest() {
        return voidRequest;
    }

    /**
     * @param voidRequest the voidRequest to set
     */
    public void setVoidRequest(OneCheckoutVoidRequest voidRequest) {
        this.voidRequest = voidRequest;
    }

    /**
     * @return the checkStatusRequest
     */
    public OneCheckoutCheckStatusData getCheckStatusRequest() {
        return checkStatusRequest;
    }

    /**
     * @param checkStatusRequest the checkStatusRequest to set
     */
    public void setCheckStatusRequest(OneCheckoutCheckStatusData checkStatusRequest) {
        this.checkStatusRequest = checkStatusRequest;
    }

    /**
     * @return the edsGetData
     */
    public OneCheckoutEDSGetData getEdsGetData() {
        return edsGetData;
    }

    /**
     * @param edsGetData the edsGetData to set
     */
    public void setEdsGetData(OneCheckoutEDSGetData edsGetData) {
        this.edsGetData = edsGetData;
    }

    /**
     * @return the edsUpdateStatus
     */
    public OneCheckoutEDSUpdateStatusData getEdsUpdateStatus() {
        return edsUpdateStatus;
    }

    /**
     * @param edsUpdateStatus the edsUpdateStatus to set
     */
    public void setEdsUpdateStatus(OneCheckoutEDSUpdateStatusData edsUpdateStatus) {
        this.edsUpdateStatus = edsUpdateStatus;
    }

    /**
     * @return the verifyRequest
     */
    public OneCheckoutDOKUVerifyData getVerifyRequest() {
        return verifyRequest;
    }

    /**
     * @param verifyRequest the verifyRequest to set
     */
    public void setVerifyRequest(OneCheckoutDOKUVerifyData verifyRequest) {
        this.verifyRequest = verifyRequest;
    }

    /**
     * @return the redirectDoku
     */
    public OneCheckoutRedirectData getRedirectDoku() {
        return redirectDoku;
    }

    /**
     * @param redirectDoku the redirectDoku to set
     */
    public void setRedirectDoku(OneCheckoutRedirectData redirectDoku) {
        this.redirectDoku = redirectDoku;
    }

    /**
     * @return the paymentPageObject
     */
    public PaymentPageObject getPaymentPageObject() {
        return paymentPageObject;
    }

    /**
     * @param paymentPageObject the paymentPageObject to set
     */
    public void setPaymentPageObject(PaymentPageObject paymentPageObject) {
        this.paymentPageObject = paymentPageObject;
    }

    /**
     * @return the trxId
     */
    public Long getTrxId() {
        return trxId;
    }

    /**
     * @param trxId the trxId to set
     */
    public void setTrxId(Long trxId) {
        this.trxId = trxId;
    }

    /**
     * @return the flag
     */
    public String getFlag() {
        return flag;
    }

    /**
     * @param flag the flag to set
     */
    public void setFlag(String flag) {
        this.flag = flag;
    }

    /**
     * @return the todayKlikBCA
     */
    public OneCheckoutTodayKlikBCARequest getTodayKlikBCA() {
        return todayKlikBCA;
    }

    /**
     * @param todayKlikBCA the todayKlikBCA to set
     */
    public void setTodayKlikBCA(OneCheckoutTodayKlikBCARequest todayKlikBCA) {
        this.todayKlikBCA = todayKlikBCA;
    }

    /**
     * @return the trxDataDisplay
     */
    public OneCheckoutDataTodayTransactions getTrxDataDisplay() {
        return trxDataDisplay;
    }

    /**
     * @param trxDataDisplay the trxDataDisplay to set
     */
    public void setTrxDataDisplay(OneCheckoutDataTodayTransactions trxDataDisplay) {
        this.trxDataDisplay = trxDataDisplay;
    }

    /**
     * @return the systemMessage
     */
    public String getSystemMessage() {
        return systemMessage;
    }

    /**
     * @param systemMessage the systemMessage to set
     */
    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }

    /**
     * @return the paymentChannelCode
     */
    public int getPaymentChannelCode() {
        return paymentChannelCode;
    }

    /**
     * @param paymentChannelCode the paymentChannelCode to set
     */
    public void setPaymentChannelCode(int paymentChannelCode) {
        this.paymentChannelCode = paymentChannelCode;
    }

    /**
     * @return the transactions
     */
    public Transactions getTransactions() {
        return transactions;
    }

    /**
     * @param transactions the transactions to set
     */
    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }

    /**
     * @return the notifyResponse
     */
    public OneCheckoutNotifyStatusResponse getNotifyResponse() {
        return notifyResponse;
    }

    /**
     * @param notifyResponse the notifyResponse to set
     */
    public void setNotifyResponse(OneCheckoutNotifyStatusResponse notifyResponse) {
        this.notifyResponse = notifyResponse;
    }

    /**
     * @return the stepNotify
     */
    public OneCheckoutStepNotify getStepNotify() {
        return stepNotify;
    }

    /**
     * @param stepNotify the stepNotify to set
     */
    public void setStepNotify(OneCheckoutStepNotify stepNotify) {
        this.stepNotify = stepNotify;
    }

    /**
     * @return the errorMessage
     */
    public OneCheckoutErrorMessage getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(OneCheckoutErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the status
     */
    public OneCheckoutTransactionStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(OneCheckoutTransactionStatus status) {
        this.status = status;
    }

    /**
     * @return the CyberSourceData
     */
    public OneCheckoutCyberSourceData getCyberSourceData() {
        return CyberSourceData;
    }

    /**
     * @param CyberSourceData the CyberSourceData to set
     */
    public void setCyberSourceData(OneCheckoutCyberSourceData CyberSourceData) {
        this.CyberSourceData = CyberSourceData;
    }

    public Rates getRates() {
        return rates;
    }

    public void setRates(Rates rates) {
        this.rates = rates;
    }
    
    public CustomerInfoEmailObject getCustomerInfoEmailObject() {
        return customerInfoEmailObject;
    }
    public void setCustomerInfoEmailObject(CustomerInfoEmailObject customerInfoEmailObject) {
        this.customerInfoEmailObject = customerInfoEmailObject;
    }

    public String getOcoId() {
        return ocoId;
    }

    public void setOcoId(String ocoId) {
        this.ocoId = ocoId;
    }

//    public Double getAmountRedeemed() {
//        return amountRedeemed;
//    }
//
//    public void setAmountRedeemed(String amountRedeemed) {
//        try {
//            this.amountRedeemed = this.validateAmount(amountRedeemed,"AMOUNTREDEEMED");
//        } catch (InvalidDOKURedirectDataException iv) {
//            throw new InvalidDOKURedirectDataException(iv.getMessage());
//        }
//    }
    
    private double validateAmount(String amount, String description) {
        double val = (double) OneCheckoutVerifyFormatData.validateDouble(OneCheckoutVerifyFormatData.ZERO, Double.MAX_VALUE, description, "###,###,##0.00", amount);
        return val;
    }

    public RefundHelper getRefundHelper() {
        return refundHelper;
    }

    public void setRefundHelper(RefundHelper refundHelper) {
        this.refundHelper = refundHelper;
    }

    
}
