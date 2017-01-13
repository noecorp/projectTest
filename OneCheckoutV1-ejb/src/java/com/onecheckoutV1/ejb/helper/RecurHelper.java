/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.exception.InvalidPaymentRequestException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author Opiks
 */
public class RecurHelper implements Serializable {

    private static final long serialVersionUID = 1L;
    private String button = "";
    private String paymentChannelId = "";
    private int mallId = 0;
    private int chainMallId = 0;
    private String customerId = "";
    private String customerName = "";
    private String customerEmail = "";
    private String customerAddress = "";
    private String customerCity = "";
    private String customerState = "";
    private String customerCountry = "";
    private String customerZipCode = "";
    private String customerHomePhone = "";
    private String customerMobilePhone = "";
    private String customerWorkPhone = "";
    private String billingNumber = "";
    private String billingType = "";        // D:DONATION, I:INSTALMENT
    private String billingDetail = "";
    private String billingCurrency = "";
    private String billingPurchaseCurrency = "";
    private double billingAmount = 0;
    private double billingRegisterAmount = 0;
    private double billingPurchaseAmount = 0;
    private String executeStartDate = "";
    private String executeEndDate = "";
    private String executeType = "";        // T:DATE, Y:DAY
    private String executeDate = "";
    private String executeMonth = "";
    private String words = "";
    private String sessionId = "";
    private String invoiceNumber = "";
    private boolean cashNowStatus = false;
    private boolean flatStatus = true;
    private String requestDateTime = "";
    private String clientIp = "";
    private String tokenId = "";
    private String updatedTokenId = "";
    private String maskingCard = "";
    private boolean parseStatus = false;
    private boolean updateBillingStatus = false;
    private HashMap<String, String> additionalData;
    private String message = "";

    public String getMaskingCard() {
        return maskingCard;
    }

    public void setMaskingCard(String maskingCard) {
        this.maskingCard = maskingCard;
    }

    public String getUpdatedTokenId() {
        return updatedTokenId;
    }

    public void setUpdatedTokenId(String updatedTokenId) {
        this.updatedTokenId = updatedTokenId;
    }

    public boolean isUpdateBillingStatus() {
        return updateBillingStatus;
    }

    public void setUpdateBillingStatus(boolean updateBillingStatus) {
        this.updateBillingStatus = updateBillingStatus;
    }

    public boolean isParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(boolean parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public HashMap<String, String> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(HashMap<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    public String getPaymentChannelId() {
        return paymentChannelId;
    }

    public void setPaymentChannelId(String paymentChannelId) {
        this.paymentChannelId = paymentChannelId;
    }

    public double getBillingAmount() {
        return billingAmount;
    }

    public void setBillingAmount(double billingAmount) {
        this.billingAmount = billingAmount;
    }

    public double getBillingRegisterAmount() {
        return billingRegisterAmount;
    }

    public void setBillingRegisterAmount(double billingRegisterAmount) {
        this.billingRegisterAmount = billingRegisterAmount;
    }

    public String getBillingCurrency() {
        return billingCurrency;
    }

    public void setBillingCurrency(String billingCurrency) {
        this.billingCurrency = billingCurrency;
    }

    public String getBillingDetail() {
        return billingDetail;
    }

    public void setBillingDetail(String billingDetail) {
        this.billingDetail = billingDetail;
    }

    public String getBillingNumber() {
        return billingNumber;
    }

    public void setBillingNumber(String billingNumber) {
        this.billingNumber = billingNumber;
    }

    public double getBillingPurchaseAmount() {
        return billingPurchaseAmount;
    }

    public void setBillingPurchaseAmount(double billingPurchaseAmount) {
        this.billingPurchaseAmount = billingPurchaseAmount;
    }

    public String getBillingPurchaseCurrency() {
        return billingPurchaseCurrency;
    }

    public void setBillingPurchaseCurrency(String billingPurchaseCurrency) {
        this.billingPurchaseCurrency = billingPurchaseCurrency;
    }

    public String getBillingType() {
        return billingType;
    }

    public void setBillingType(String billingType) {
        this.billingType = billingType;
    }

    public String getButton() {
        return button;
    }

    public void setButton(String button) {
        this.button = button;
    }

    public boolean isCashNowStatus() {
        return cashNowStatus;
    }

    public void setCashNowStatus(boolean cashNowStatus) {
        this.cashNowStatus = cashNowStatus;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity = customerCity;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerHomePhone() {
        return customerHomePhone;
    }

    public void setCustomerHomePhone(String customerHomePhone) {
        this.customerHomePhone = customerHomePhone;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerMobilePhone() {
        return customerMobilePhone;
    }

    public void setCustomerMobilePhone(String customerMobilePhone) {
        this.customerMobilePhone = customerMobilePhone;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerState() {
        return customerState;
    }

    public void setCustomerState(String customerState) {
        this.customerState = customerState;
    }

    public String getCustomerWorkPhone() {
        return customerWorkPhone;
    }

    public void setCustomerWorkPhone(String customerWorkPhone) {
        this.customerWorkPhone = customerWorkPhone;
    }

    public String getCustomerZipCode() {
        return customerZipCode;
    }

    public void setCustomerZipCode(String customerZipCode) {
        this.customerZipCode = customerZipCode;
    }

    public String getExecuteDate() {
        return executeDate;
    }

    public void setExecuteDate(String executeDate) {
        this.executeDate = executeDate;
    }

    public String getExecuteEndDate() {
        return executeEndDate;
    }

    public void setExecuteEndDate(String executeEndDate) {
        this.executeEndDate = executeEndDate;
    }

    public String getExecuteMonth() {
        return executeMonth;
    }

    public void setExecuteMonth(String executeMonth) {
        this.executeMonth = executeMonth;
    }

    public String getExecuteStartDate() {
        return executeStartDate;
    }

    public void setExecuteStartDate(String executeStartDate) {
        this.executeStartDate = executeStartDate;
    }

    public String getExecuteType() {
        return executeType;
    }

    public void setExecuteType(String executeType) {
        this.executeType = executeType;
    }

    public boolean isFlatStatus() {
        return flatStatus;
    }

    public void setFlatStatus(boolean flatStatus) {
        this.flatStatus = flatStatus;
    }

    public int getChainMallId() {
        return chainMallId;
    }

    public void setChainMallId(int chainMallId) {
        this.chainMallId = chainMallId;
    }

    public int getMallId() {
        return mallId;
    }

    public void setMallId(int mallId) {
        this.mallId = mallId;
    }

    public String getRequestDateTime() {
        return requestDateTime;
    }

    public void setRequestDateTime(String requestDateTime) {
        this.requestDateTime = requestDateTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        }

    public String getTokenId() {
        return tokenId;
    }
    
    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }
}
