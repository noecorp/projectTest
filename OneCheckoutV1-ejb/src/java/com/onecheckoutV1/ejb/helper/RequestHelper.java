/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onechekoutv1.dto.MerchantActivity;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import java.io.Serializable;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Opiks
 */
public class RequestHelper implements Serializable {

    private static final long serialVersionUID = 1L;
    private ResultHelper resultHelper;

    // REQUEST FROM MERCHANT
    private transient javax.servlet.http.HttpServletRequest filteredRequest;
    private transient javax.servlet.http.HttpServletRequest unfilterRequest;

    // TABLE IN DATABASE
    private MerchantActivity merchantActivity;
    private MerchantPaymentChannel merchantPaymentChannel;
    private MerchantPaymentChannel recurCorePaymentChannel;
    private MerchantPaymentChannel tokenizationCorePaymentChannel;

    // HELPER
    private RecurHelper recurHelper;
    private TokenizationHelper tokenizationHelper;
    private CreditCardHelper creditCardHelper;
    private ThreeDSecureHelper threeDSecureHelper;

    private HashMap<String, String> paymentParam;
    private HashMap<String, String> threeDSecureParam;

    private String acsUrl = "";
    private String threeDSecureMerchantData = "";
    private String templateFileName = "";
    
    private Boolean useFailedPage = false;

    private String errorCode = null;

    public Boolean getUseFailedPage() {
        return useFailedPage;
    }

    public void setUseFailedPage(Boolean useFailedPage) {
        this.useFailedPage = useFailedPage;
    }

    public RequestHelper() {
        setResultHelper(new ResultHelper());
    }

    public ThreeDSecureHelper getThreeDSecureHelper() {
        return threeDSecureHelper;
    }

    public void setThreeDSecureHelper(ThreeDSecureHelper threeDSecureHelper) {
        this.threeDSecureHelper = threeDSecureHelper;
    }

    public String getThreeDSecureMerchantData() {
        return threeDSecureMerchantData;
    }

    public void setThreeDSecureMerchantData(String threeDSecureMerchantData) {
        this.threeDSecureMerchantData = threeDSecureMerchantData;
    }

    public String getAcsUrl() {
        return acsUrl;
    }

    public void setAcsUrl(String acsUrl) {
        this.acsUrl = acsUrl;
    }

    public HashMap<String, String> getThreeDSecureParam() {
        return threeDSecureParam;
    }

    public void setThreeDSecureParam(HashMap<String, String> threeDSecureParam) {
        this.threeDSecureParam = threeDSecureParam;
    }

    public MerchantPaymentChannel getRecurCorePaymentChannel() {
        return recurCorePaymentChannel;
    }

    public void setRecurCorePaymentChannel(MerchantPaymentChannel recurCorePaymentChannel) {
        this.recurCorePaymentChannel = recurCorePaymentChannel;
    }

    public HashMap<String, String> getPaymentParam() {
        return paymentParam;
    }

    public void setPaymentParam(HashMap<String, String> paymentParam) {
        this.paymentParam = paymentParam;
    }

    public CreditCardHelper getCreditCardHelper() {
        return creditCardHelper;
    }

    public void setCreditCardHelper(CreditCardHelper creditCardHelper) {
        this.creditCardHelper = creditCardHelper;
    }

    public RecurHelper getRecurHelper() {
        return recurHelper;
    }

    public void setRecurHelper(RecurHelper recurHelper) {
        this.recurHelper = recurHelper;
    }

    public String getTemplateFileName() {
        return templateFileName;
    }

    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    public HttpServletRequest getFilteredRequest() {
        return filteredRequest;
    }

    public void setFilteredRequest(HttpServletRequest filteredRequest) {
        this.filteredRequest = filteredRequest;
    }

    public MerchantActivity getMerchantActivity() {
        return merchantActivity;
    }

    public void setMerchantActivity(MerchantActivity merchantActivity) {
        this.merchantActivity = merchantActivity;
    }

    public MerchantPaymentChannel getMerchantPaymentChannel() {
        return merchantPaymentChannel;
    }

    public void setMerchantPaymentChannel(MerchantPaymentChannel merchantPaymentChannel) {
        this.merchantPaymentChannel = merchantPaymentChannel;
    }

    public ResultHelper getResultHelper() {
        return resultHelper;
    }

    public void setResultHelper(ResultHelper resultHelper) {
        this.resultHelper = resultHelper;
    }

    public HttpServletRequest getUnfilterRequest() {
        return unfilterRequest;
    }

    public void setUnfilterRequest(HttpServletRequest unfilterRequest) {
        this.unfilterRequest = unfilterRequest;
    }

    public TokenizationHelper getTokenizationHelper() {
        return tokenizationHelper;
    }

    public void setTokenizationHelper(TokenizationHelper tokenizationHelper) {
        this.tokenizationHelper = tokenizationHelper;
    }

    public MerchantPaymentChannel getTokenizationCorePaymentChannel() {
        return tokenizationCorePaymentChannel;
    }

    public void setTokenizationCorePaymentChannel(MerchantPaymentChannel tokenizationCorePaymentChannel) {
        this.tokenizationCorePaymentChannel = tokenizationCorePaymentChannel;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
}
