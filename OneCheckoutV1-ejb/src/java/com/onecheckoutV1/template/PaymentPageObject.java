/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.template;

import com.onecheckoutV1.data.OneCheckoutBasket;
import com.onecheckoutV1.data.OneCheckoutInstallment;
import com.onecheckoutV1.data.OneCheckoutRewards;
//import com.onecheckoutV1.ejb.helper.MIPDokuInquiryResponse;
import com.onecheckoutV1.ejb.helper.MIPInquiryResponse;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onechekoutv1.dto.Country;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hafizsjafioedin
 */
public class PaymentPageObject implements Serializable {

    private static final long serialVersionUID = 1L;

    //Only For Credit Card
    private String name = "";
    private String email = "";
    private String address = "";
    private String city = "";
    private String state = "";
    private String country = "";
    private String zipcode = "";
    private String homephone = "";
    private String mobilephone = "";
    private String workphone = "";
    private String date = "";
    private String month = "";
    private String year = "";
    private String customerNumber = "";
    private CustomerInfoObject customerInfo;
    private String datenow = "";
    private String merchantName = "";
    private String merchantAddress = "";
    private String invoiceNo = "";
    private List<OneCheckoutBasket> basket;
    private ArrayList<String> channel;
    private String actionUrl = "";
    private String cancelUrl = "";

    // Mandiri
    private String challengeCode2 = "";
    private String challengeCode3 = "";

    // Permata VA
    private String payCode = "";
    private String permataFullPayCode = "";

    // Mandiri SOA
    private String mandiriSOALitePayCode = "";
    private String mandiriSOAFullPayCode = "";

    // Mandiri VA
    private String mandiriVALitePayCode = "";
    private String mandiriVAFullPayCode = "";

    // PTPos VA
    private String PTPOSPayCode = "";

    // Sinar Mas VA
    private String sinarMasLitePayCode = "";
    private String sinarMasFullPayCode = "";

    //Alfamart
    private String alfamartPayCode = "";
    private String purchaseCurrencyused = "";
    private String amount = "";
    private String purchaseCurrency = "";
    private String purchaseAmount = "";
    private List<Country> listCountry;

    private String danamonPayCode = "";

    private String briPayCode = "";

    //indomaret    
    private String indomaretPayCode = "";

    private String customerId = "";
    private String billingNumber = "";
    private String billingType = "";        // D:DONATION, I:INSTALMENT
    private String billingDetail = "";
    private String executeStartDate = "";
    private String executeEndDate = "";
    private String executeType = "";        // T:DATE, Y:DAY
    private String executeDate = "";
    private String executeMonth = "";
    private String cardNumber = "";
    private String tokenNumber = "";
    private boolean cashNowStatus = false;
    private boolean flatStatus = true;
    private boolean updateBillCardStatus = false;

    private String alfamartMVAPayCode = "";
    // DOKUWALLET
    private MIPInquiryResponse mIPInquiryResponse;

    private String message = "";
    private OneCheckoutRewards oneCheckoutRewards;

    private String CIMBVaPayCode = "";

    //OFF-US INSTALLMENT
    private List<OneCheckoutInstallment> listOneCheckoutInstallment = new ArrayList<OneCheckoutInstallment>();

//    private MIPDokuInquiryResponse mIPDokuInquiryResponse;
//
//    public MIPDokuInquiryResponse getmIPDokuInquiryResponse() {
//        return mIPDokuInquiryResponse;
//    }
//
//    public void setmIPDokuInquiryResponse(MIPDokuInquiryResponse mIPDokuInquiryResponse) {
//        this.mIPDokuInquiryResponse = mIPDokuInquiryResponse;
//    }
    private String vainitNumberFormat = "";
    private String permataMvaPaycode = "";
    private String BNIVaPaycode;
    private List<String> paymentTypeKredivo;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MIPInquiryResponse getMIPInquiryResponse() {
        return mIPInquiryResponse;
    }

    public void setMIPInquiryResponse(MIPInquiryResponse mIPInquiryResponse) {
        this.mIPInquiryResponse = mIPInquiryResponse;
    }

    public String getSinarMasFullPayCode() {
        return sinarMasFullPayCode;
    }

    public void setSinarMasFullPayCode(String sinarMasFullPayCode) {
        this.sinarMasFullPayCode = sinarMasFullPayCode;
    }

    public String getSinarMasLitePayCode() {
        return sinarMasLitePayCode;
    }

    public void setSinarMasLitePayCode(String sinarMasLitePayCode) {
        this.sinarMasLitePayCode = sinarMasLitePayCode;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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

    public String getBillingType() {
        return billingType;
    }

    public void setBillingType(String billingType) {
        this.billingType = billingType;
    }

    public boolean isCashNowStatus() {
        return cashNowStatus;
    }

    public void setCashNowStatus(boolean cashNowStatus) {
        this.cashNowStatus = cashNowStatus;
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

    /**
     * @return the datenow
     */
    public String getDatenow() {
        return datenow;
    }

    /**
     * @param datenow the datenow to set
     */
    public void setDatenow(String datenow) {
        this.datenow = datenow;
    }

    /**
     * @return the merchantName
     */
    public String getMerchantName() {
        return merchantName;
    }

    /**
     * @param merchantName the merchantName to set
     */
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    /**
     * @return the merchantAddress
     */
    public String getMerchantAddress() {
        return merchantAddress;
    }

    /**
     * @param merchantAddress the merchantAddress to set
     */
    public void setMerchantAddress(String merchantAddress) {
        this.merchantAddress = merchantAddress;
    }

    /**
     * @return the invoiceNo
     */
    public String getInvoiceNo() {
        return invoiceNo;
    }

    /**
     * @param invoiceNo the invoiceNo to set
     */
    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    /**
     * @return the purchaseCurrencyused
     */
    public String getPurchaseCurrencyused() {
        return purchaseCurrencyused;
    }

    /**
     * @param purchaseCurrencyused the purchaseCurrencyused to set
     */
    public void setPurchaseCurrencyused(String purchaseCurrencyused) {
        this.purchaseCurrencyused = purchaseCurrencyused;
    }

    /**
     * @return the amount
     */
    public String getAmount() {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(String amount) {
        this.amount = amount;
    }

    /**
     * @return the actionUrl
     */
    public String getActionUrl() {
        return actionUrl;
    }

    /**
     * @param actionUrl the actionUrl to set
     */
    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    /**
     * @return the basket
     */
    public List<OneCheckoutBasket> getBasket() {
        return basket;
    }

    /**
     * @param basket the basket to set
     */
    public void setBasket(List<OneCheckoutBasket> basket) {
        this.basket = basket;
    }

    /**
     * @return the channel
     */
    public ArrayList<String> getChannel() {
        return channel;
    }

    /**
     * @param channel the channel to set
     */
    public void setChannel(ArrayList<String> channel) {
        this.channel = channel;
    }

    /**
     * @return the cancelUrl
     */
    public String getCancelUrl() {
        return cancelUrl;
    }

    /**
     * @param cancelUrl the cancelUrl to set
     */
    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    /**
     * @return the challengeCode2
     */
    public String getChallengeCode2() {
        return challengeCode2;
    }

    /**
     * @param challengeCode2 the challengeCode2 to set
     */
    public void setChallengeCode2(String challengeCode2) {
        this.challengeCode2 = challengeCode2;
    }

    /**
     * @return the challengeCode3
     */
    public String getChallengeCode3() {
        return challengeCode3;
    }

    /**
     * @param challengeCode3 the challengeCode3 to set
     */
    public void setChallengeCode3(String challengeCode3) {
        this.challengeCode3 = challengeCode3;
    }

    /**
     * @return the permataFullPayCode
     */
    public String getPermataFullPayCode() {
        return permataFullPayCode;
    }

    /**
     * @param permataFullPayCode the permataFullPayCode to set
     */
    public void setPermataFullPayCode(String permataFullPayCode) {
        this.permataFullPayCode = permataFullPayCode;
    }

    /**
     * @return the mandiriSOALitePayCode
     */
    public String getMandiriSOALitePayCode() {
        return mandiriSOALitePayCode;
    }

    /**
     * @param mandiriSOALitePayCode the mandiriSOALitePayCode to set
     */
    public void setMandiriSOALitePayCode(String mandiriSOALitePayCode) {
        this.mandiriSOALitePayCode = mandiriSOALitePayCode;
    }

    /**
     * @return the mandiriSOAFullPayCode
     */
    public String getMandiriSOAFullPayCode() {
        return mandiriSOAFullPayCode;
    }

    /**
     * @param mandiriSOAFullPayCode the mandiriSOAFullPayCode to set
     */
    public void setMandiriSOAFullPayCode(String mandiriSOAFullPayCode) {
        this.mandiriSOAFullPayCode = mandiriSOAFullPayCode;
    }

    /**
     * @return the mandiriVALitePayCode
     */
    public String getMandiriVALitePayCode() {
        return mandiriVALitePayCode;
    }

    /**
     * @param mandiriVALitePayCode the mandiriVALitePayCode to set
     */
    public void setMandiriVALitePayCode(String mandiriVALitePayCode) {
        this.mandiriVALitePayCode = mandiriVALitePayCode;
    }

    /**
     * @return the mandiriVAFullPayCode
     */
    public String getMandiriVAFullPayCode() {
        return mandiriVAFullPayCode;
    }

    /**
     * @param mandiriVAFullPayCode the mandiriVAFullPayCode to set
     */
    public void setMandiriVAFullPayCode(String mandiriVAFullPayCode) {
        this.mandiriVAFullPayCode = mandiriVAFullPayCode;
    }

    /**
     * @return the payCode
     */
    public String getPayCode() {
        return payCode;
    }

    /**
     * @param payCode the payCode to set
     */
    public void setPayCode(String payCode) {
        this.payCode = payCode;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the zipcode
     */
    public String getZipcode() {
        return zipcode;
    }

    /**
     * @param zipcode the zipcode to set
     */
    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    /**
     * @return the homephone
     */
    public String getHomephone() {
        return homephone;
    }

    /**
     * @param homephone the homephone to set
     */
    public void setHomephone(String homephone) {
        this.homephone = homephone;
    }

    /**
     * @return the mobilephone
     */
    public String getMobilephone() {
        return mobilephone;
    }

    /**
     * @param mobilephone the mobilephone to set
     */
    public void setMobilephone(String mobilephone) {
        this.mobilephone = mobilephone;
    }

    /**
     * @return the workphone
     */
    public String getWorkphone() {
        return workphone;
    }

    /**
     * @param workphone the workphone to set
     */
    public void setWorkphone(String workphone) {
        this.workphone = workphone;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * @return the month
     */
    public String getMonth() {
        return month;
    }

    /**
     * @param month the month to set
     */
    public void setMonth(String month) {
        this.month = month;
    }

    /**
     * @return the year
     */
    public String getYear() {
        return year;
    }

    /**
     * @param year the year to set
     */
    public void setYear(String year) {
        this.year = year;
    }

    /**
     * @return the purchaseCurrency
     */
    public String getPurchaseCurrency() {
        return purchaseCurrency;
    }

    /**
     * @param purchaseCurrency the purchaseCurrency to set
     */
    public void setPurchaseCurrency(String purchaseCurrency) {
        this.purchaseCurrency = purchaseCurrency;
    }

    /**
     * @return the purchaseAmount
     */
    public String getPurchaseAmount() {
        return purchaseAmount;
    }

    /**
     * @param purchaseAmount the purchaseAmount to set
     */
    public void setPurchaseAmount(String purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    /**
     * @return the listCountry
     */
    public List<Country> getListCountry() {
        return listCountry;
    }

    /**
     * @param listCountry the listCountry to set
     */
    public void setListCountry(List<Country> listCountry) {
        this.listCountry = listCountry;
    }

    /**
     * @return the alfamartPayCode
     */
    public String getAlfamartPayCode() {
        return alfamartPayCode;
    }

    /**
     * @param alfamartPayCode the alfamartPayCode to set
     */
    public void setAlfamartPayCode(String alfamartPayCode) {
        this.alfamartPayCode = alfamartPayCode;
    }

    public String getIndomaretPayCode() {
        return indomaretPayCode;
    }

    public void setIndomaretPayCode(String indomaretPayCode) {
        this.indomaretPayCode = indomaretPayCode;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public CustomerInfoObject getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(CustomerInfoObject customerInfo) {
        this.customerInfo = customerInfo;
    }

    public boolean isUpdateBillCardStatus() {
        return updateBillCardStatus;
    }

    public void setUpdateBillCardStatus(boolean updateBillCardStatus) {
        this.updateBillCardStatus = updateBillCardStatus;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * @return the PTPOSPayCode
     */
    public String getPTPOSPayCode() {
        return PTPOSPayCode;
    }

    /**
     * @param PTPOSPayCode the PTPOSPayCode to set
     */
    public void setPTPOSPayCode(String PTPOSPayCode) {
        this.PTPOSPayCode = PTPOSPayCode;
    }

    public List<OneCheckoutInstallment> getListOneCheckoutInstallment() {
        return listOneCheckoutInstallment;
    }

    public void setListOneCheckoutInstallment(List<OneCheckoutInstallment> listOneCheckoutInstallment) {
        this.listOneCheckoutInstallment = listOneCheckoutInstallment;
    }

    public OneCheckoutRewards getOneCheckoutRewards() {
        return oneCheckoutRewards;
    }

    public void setOneCheckoutRewards(OneCheckoutRewards oneCheckoutRewards) {
        this.oneCheckoutRewards = oneCheckoutRewards;
    }

    public String getBriPayCode() {
        return briPayCode;
    }

    public void setBriPayCode(String briPayCode) {
        this.briPayCode = briPayCode;
    }

    public String getAlfamartMVAPayCode() {
        return alfamartMVAPayCode;
    }

    public void setAlfamartMVAPayCode(String alfamartMVAPayCode) {
        this.alfamartMVAPayCode = alfamartMVAPayCode;
    }

    public String getDanamonPayCode() {
        return danamonPayCode;
    }

    public void setDanamonPayCode(String danamonPayCode) {
        this.danamonPayCode = danamonPayCode;
    }

    public String getCIMBVaPayCode() {
        return CIMBVaPayCode;
    }

    public void setCIMBVaPayCode(String CIMBVaPayCode) {
        this.CIMBVaPayCode = CIMBVaPayCode;
    }

    public String getVainitNumberFormat() {
        return vainitNumberFormat;
    }

    public void setVainitNumberFormat(String vainitNumberFormat) {
        this.vainitNumberFormat = vainitNumberFormat;
    }

    public String getPermataMvaPaycode() {
        return permataMvaPaycode;
    }

    public void setPermataMvaPaycode(String permataMvaPaycode) {
        this.permataMvaPaycode = permataMvaPaycode;
    }

    public String getBNIVaPaycode() {
        return BNIVaPaycode;
    }

    public void setBNIVaPaycode(String BNIVaPaycode) {
        this.BNIVaPaycode = BNIVaPaycode;
    }

    public List<String> getPaymentTypeKredivo() {
        return paymentTypeKredivo;
    }

    public void setPaymentTypeKredivo(List<String> paymentTypeKredivo) {
        this.paymentTypeKredivo = paymentTypeKredivo;
    }

}
