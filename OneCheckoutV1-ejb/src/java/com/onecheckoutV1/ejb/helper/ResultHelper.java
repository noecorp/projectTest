/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.enums.EMerchantActivityErrorCode;
import com.onecheckoutV1.template.CustomerInfoObject;
import com.onechekoutv1.dto.Transactions;
import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author Opiks
 */
public class ResultHelper implements Serializable {

    private static final long serialVersionUID = 1L;
    private EMerchantActivityErrorCode eMerchantActivityErrorCode;
    private String displayMessage = "";
    private String responseMessage = "";
    private String systemMessage = "";
    private String responseCode = "";
    private String approvalCode = "";
    private String transactionCode = "";
    private String issuerName = "";
    private boolean recurAuthorizeStatus = false;
    private boolean recurRegistrationStatus = false;
    private boolean recurUpdateStatus = false;
    private Transactions transactions;
    private CustomerInfoObject customerInfoObject;
    private boolean tokenAuthorizeStatus = false;
    private boolean tokenizationRegistrationStatus = false;
    private boolean tokenUpdateStatus = false;
    private boolean tokenDeleteStatus = false;
    private HashMap<String, String> redirectParam = new HashMap<String, String>();

    public HashMap<String, String> getRedirectParam() {
        return redirectParam;
    }

    public void setRedirectParam(HashMap<String, String> redirectParam) {
        this.redirectParam = redirectParam;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }
    
    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getApprovalCode() {
        return approvalCode;
    }

    public void setApprovalCode(String approvalCode) {
        this.approvalCode = approvalCode;
    }
    
    public boolean isRecurAuthorizeStatus() {
        return recurAuthorizeStatus;
    }

    public void setRecurAuthorizeStatus(boolean recurAuthorizeStatus) {
        this.recurAuthorizeStatus = recurAuthorizeStatus;
    }

    public boolean isRecurRegistrationStatus() {
        return recurRegistrationStatus;
    }

    public void setRecurRegistrationStatus(boolean recurRegistrationStatus) {
        this.recurRegistrationStatus = recurRegistrationStatus;
    }

    public boolean isRecurUpdateStatus() {
        return recurUpdateStatus;
    }

    public void setRecurUpdateStatus(boolean recurUpdateStatus) {
        this.recurUpdateStatus = recurUpdateStatus;
    }

    public CustomerInfoObject getCustomerInfoObject() {
        return customerInfoObject;
    }

    public void setCustomerInfoObject(CustomerInfoObject customerInfoObject) {
        this.customerInfoObject = customerInfoObject;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public EMerchantActivityErrorCode getEMerchantActivityErrorCode() {
        return eMerchantActivityErrorCode;
    }

    public void setEMerchantActivityErrorCode(EMerchantActivityErrorCode eMerchantActivityErrorCode) {
        this.eMerchantActivityErrorCode = eMerchantActivityErrorCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public void setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
    }

    public Transactions getTransactions() {
        return transactions;
    }

    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }

    public boolean isTokenAuthorizeStatus() {
        return tokenAuthorizeStatus;
    }

    public void setTokenAuthorizeStatus(boolean tokenAuthorizeStatus) {
        this.tokenAuthorizeStatus = tokenAuthorizeStatus;
    }

    public boolean isTokenizationRegistrationStatus() {
        return tokenizationRegistrationStatus;
    }

    public void setTokenizationRegistrationStatus(boolean tokenizationRegistrationStatus) {
        this.tokenizationRegistrationStatus = tokenizationRegistrationStatus;
    }

    public EMerchantActivityErrorCode geteMerchantActivityErrorCode() {
        return eMerchantActivityErrorCode;
    }

    public void seteMerchantActivityErrorCode(EMerchantActivityErrorCode eMerchantActivityErrorCode) {
        this.eMerchantActivityErrorCode = eMerchantActivityErrorCode;
    }

    public boolean isTokenUpdateStatus() {
        return tokenUpdateStatus;
    }

    public void setTokenUpdateStatus(boolean tokenUpdateStatus) {
        this.tokenUpdateStatus = tokenUpdateStatus;
    }

    public boolean isTokenDeleteStatus() {
        return tokenDeleteStatus;
    }

    public void setTokenDeleteStatus(boolean tokenDeleteStatus) {
        this.tokenDeleteStatus = tokenDeleteStatus;
    }
    
}
