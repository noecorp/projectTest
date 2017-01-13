/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutBasket;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Opiks
 */
public class TokenizationHelper implements Serializable {

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
    private String currency = "";
    private String purchaseCurrency = "";
    private double amount = 0;
    private double purchaseAmount = 0;
    private String words = "";
    private String sessionId = "";
    private String requestDateTime = "";
    private String clientIp = "";
    private String tokenId = "";
    private boolean parseStatus = false;
    private String basket = "";
    private String invoiceNumber = "";

    private List<OneCheckoutBasket> basketView = new ArrayList<OneCheckoutBasket>();

    private HashMap<String, String> additionalData;

    public String getButton() {
        return button;
    }

    public void setButton(String button) {
        this.button = button;
    }

    public String getPaymentChannelId() {
        return paymentChannelId;
    }

    public void setPaymentChannelId(String paymentChannelId) {
        this.paymentChannelId = paymentChannelId;
    }

    public int getMallId() {
        return mallId;
    }

    public void setMallId(int mallId) {
        this.mallId = mallId;
    }

    public int getChainMallId() {
        return chainMallId;
    }

    public void setChainMallId(int chainMallId) {
        this.chainMallId = chainMallId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
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

    public String getCustomerState() {
        return customerState;
    }

    public void setCustomerState(String customerState) {
        this.customerState = customerState;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }

    public String getCustomerZipCode() {
        return customerZipCode;
    }

    public void setCustomerZipCode(String customerZipCode) {
        this.customerZipCode = customerZipCode;
    }

    public String getCustomerHomePhone() {
        return customerHomePhone;
    }

    public void setCustomerHomePhone(String customerHomePhone) {
        this.customerHomePhone = customerHomePhone;
    }

    public String getCustomerMobilePhone() {
        return customerMobilePhone;
    }

    public void setCustomerMobilePhone(String customerMobilePhone) {
        this.customerMobilePhone = customerMobilePhone;
    }

    public String getCustomerWorkPhone() {
        return customerWorkPhone;
    }

    public void setCustomerWorkPhone(String customerWorkPhone) {
        this.customerWorkPhone = customerWorkPhone;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestDateTime() {
        return requestDateTime;
    }

    public void setRequestDateTime(String requestDateTime) {
        this.requestDateTime = requestDateTime;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public boolean isParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(boolean parseStatus) {
        this.parseStatus = parseStatus;
    }

    public HashMap<String, String> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(HashMap<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPurchaseCurrency() {
        return purchaseCurrency;
    }

    public void setPurchaseCurrency(String purchaseCurrency) {
        this.purchaseCurrency = purchaseCurrency;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(double purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getBasket() {
        return basket;
    }

    public boolean setBasket(String basket,double amount, String invoiceNo) {
        try {
            this.basket = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 1024, "BASKET", basket, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            setBasketView(OneCheckoutVerifyFormatData.GetValidBasketTokenization(basket, amount));
            return true;
        } catch (OneCheckoutInvalidInputException iv) {
            StringBuilder basketStr = new StringBuilder();
            basketStr.append(invoiceNo);//getMerchant().getMerchantName());
            String amountStr = OneCheckoutVerifyFormatData.sdf.format(amount);
            basketStr.append(",").append(amountStr).append(",").append("1").append(",").append(amountStr);

            this.basket = basketStr.toString();
            return true;
        }
    }

    public List<OneCheckoutBasket> getBasketView() {
        return basketView;
    }

    public void setBasketView(List<OneCheckoutBasket> basketView) {
        this.basketView = basketView;
    }

}
