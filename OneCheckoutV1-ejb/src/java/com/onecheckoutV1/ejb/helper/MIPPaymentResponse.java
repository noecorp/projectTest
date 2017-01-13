/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Opiks
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MIPPaymentResponse {

    private String responseCode;
    private String responseMsg;
    private String paymentChannelCode;
    private Integer dpMallId;
    private String transIdMerchant;
    private String bank;
    private String approvalCode;
    private String status;
    private String callBackData;

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMsg() {
        return responseMsg;
    }

    public void setResponseMsg(String responseMsg) {
        this.responseMsg = responseMsg;
    }

    public String getPaymentChannelCode() {
        return paymentChannelCode;
    }

    public void setPaymentChannelCode(String paymentChannelCode) {
        this.paymentChannelCode = paymentChannelCode;
    }

    public Integer getDpMallId() {
        return dpMallId;
    }

    public void setDpMallId(Integer dpMallId) {
        this.dpMallId = dpMallId;
    }

    public String getTransIdMerchant() {
        return transIdMerchant;
    }

    public void setTransIdMerchant(String transIdMerchant) {
        this.transIdMerchant = transIdMerchant;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getApprovalCode() {
        return approvalCode;
    }

    public void setApprovalCode(String approvalCode) {
        this.approvalCode = approvalCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCallBackData() {
        return callBackData;
    }

    public void setCallBackData(String callBackData) {
        this.callBackData = callBackData;
    }
}
