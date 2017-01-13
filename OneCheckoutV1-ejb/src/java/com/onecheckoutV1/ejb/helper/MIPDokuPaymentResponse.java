/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import java.io.Serializable;

/**
 *
 * @author iskandar
 */
public class MIPDokuPaymentResponse implements Serializable{
    
    private String status="";
    private String responseCode="";
    private String issuerBankName="";
    private String approvalCode="";

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getIssuerBankName() {
        return issuerBankName;
    }

    public void setIssuerBankName(String issuerBankName) {
        this.issuerBankName = issuerBankName;
    }

    public String getApprovalCode() {
        return approvalCode;
    }

    public void setApprovalCode(String approvalCode) {
        this.approvalCode = approvalCode;
    }
}
