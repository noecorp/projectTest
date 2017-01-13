/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutDataPGRedirect implements Serializable {

    private static final long serialVersionUID = 1L;
    private double AMOUNT;
    private String TRANSIDMERCHANT;
    private String STATUSCODE;
    private String PURCHASECURRENCY;
    private HashMap<String, String> parameters;
    private HashMap<String, String> retryData;
    private String urlAction;
    private String pageTemplate;
    private int enableButton;
    private String cookie;
    private String httpProtocol;
    private Boolean retry = Boolean.FALSE;

    //Only Virtual Account
    private String payCode = "";
    private String merchantRedirectUrl = "";
    private static String errorPage = "failed.html";
    private static String progressPage = "redirect.html";
    public static String HTTP_POST = "POST";
    public static String HTTP_GET = "GET";

    public OneCheckoutDataPGRedirect() {
        pageTemplate = errorPage;
        httpProtocol = OneCheckoutDataPGRedirect.HTTP_POST;
        parameters = new HashMap();
        retryData = new HashMap();
        enableButton = 1;
    }

    public String getProgressPage() {
        return progressPage;
    }

    /**
     * @return the parameters
     */
    public HashMap<String, String> getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the parameters
     */
    public HashMap<String, String> getRetryData() {
        return retryData;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setRetryData(HashMap<String, String> rd) {
        this.retryData = rd;
    }

    /**
     * @return the urlAction
     */
    public String getUrlAction() {
        return urlAction;
    }

    /**
     * @param urlAction the urlAction to set
     */
    public void setUrlAction(String urlAction) {
        this.urlAction = urlAction;
    }

    /**
     * @return the pageTemplate
     */
    public String getPageTemplate() {
        return pageTemplate;
    }

    /**
     * @param pageTemplate the pageTemplate to set
     */
    public void setPageTemplate(String pageTemplate) {
        this.pageTemplate = pageTemplate;
    }

    /**
     * @return the additionData
     */
    public String getCookie() {
        return cookie;
    }

    /**
     * @param additionData the additionData to set
     */
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * @return the AMOUNT
     */
    public double getAMOUNT() {
        return AMOUNT;
    }

    /**
     * @param AMOUNT the AMOUNT to set
     */
    public void setAMOUNT(double AMOUNT) {
        this.AMOUNT = AMOUNT;
    }

    /**
     * @return the TRANSIDMERCHANT
     */
    public String getTRANSIDMERCHANT() {
        return TRANSIDMERCHANT;
    }

    /**
     * @param TRANSIDMERCHANT the TRANSIDMERCHANT to set
     */
    public void setTRANSIDMERCHANT(String TRANSIDMERCHANT) {
        this.TRANSIDMERCHANT = TRANSIDMERCHANT;
    }

    /**
     * @return the STATUSCODE
     */
    public String getSTATUSCODE() {
        return STATUSCODE;
    }

    /**
     * @param STATUSCODE the STATUSCODE to set
     */
    public void setSTATUSCODE(String STATUSCODE) {
        this.STATUSCODE = STATUSCODE;
    }

    /**
     * @return the httpProtocol
     */
    public String getHttpProtocol() {
        return httpProtocol;
    }

    /**
     * @param httpProtocol the httpProtocol to set
     */
    public void setHttpProtocol(String httpProtocol) {
        this.httpProtocol = httpProtocol;
    }

    /**
     * @return the enableButton
     */
    public int getEnableButton() {
        return enableButton;
    }

    /**
     * @param enableButton the enableButton to set
     */
    public void setEnableButton(int enableButton) {
        this.enableButton = enableButton;
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
     * @return the merchantRedirectUrl
     */
    public String getMerchantRedirectUrl() {
        return merchantRedirectUrl;
    }

    /**
     * @param merchantRedirectUrl the merchantRedirectUrl to set
     */
    public void setMerchantRedirectUrl(String merchantRedirectUrl) {
        this.merchantRedirectUrl = merchantRedirectUrl;
    }

    /**
     * @return the retry
     */
    public Boolean getRetry() {
        return retry;
    }

    /**
     * @param retry the retry to set
     */
    public void setRetry(Boolean retry) {
        this.retry = retry;
    }

    /**
     * @return the PURCHASECURRENCY
     */
    public String getPURCHASECURRENCY() {
        return PURCHASECURRENCY;
    }

    /**
     * @param PURCHASECURRENCY the PURCHASECURRENCY to set
     */
    public void setPURCHASECURRENCY(String PURCHASECURRENCY) {
        this.PURCHASECURRENCY = PURCHASECURRENCY;
    }
}