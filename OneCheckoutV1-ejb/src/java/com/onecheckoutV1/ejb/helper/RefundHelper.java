/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import java.util.HashMap;

/**
 *
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
public class RefundHelper {

    private Transactions transactions;
    private String sessionId;
    private String reason;
    private String refIdMerchant;
    private HashMap<String, Object> mapResultRefund;
    private String resurltRefund;
    private PaymentChannel paymentChannel;
    private String refundType;
    private String amount = null;
    private String currency;
    private String bankData;
    private String data1;
    private String data2;
    private String data3;
    private String data4;

    public Transactions getTransactions() {
        return transactions;
    }

    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRefIdMerchant() {
        return refIdMerchant;
    }

    public void setRefIdMerchant(String refIdMerchant) {
        this.refIdMerchant = refIdMerchant;
    }

    public HashMap<String, Object> getMapResultRefund() {
        return mapResultRefund;
    }

    public void setMapResultRefund(HashMap<String, Object> mapResultRefund) {
        this.mapResultRefund = mapResultRefund;
    }

    public PaymentChannel getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(PaymentChannel paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public String getRefundType() {
        return refundType;
    }

    public void setRefundType(String refundType) {
        this.refundType = refundType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBankData() {
        return bankData;
    }

    public void setBankData(String bankData) {
        this.bankData = bankData;
    }

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public String getData2() {
        return data2;
    }

    public void setData2(String data2) {
        this.data2 = data2;
    }

    public String getData3() {
        return data3;
    }

    public void setData3(String data3) {
        this.data3 = data3;
    }

    public String getData4() {
        return data4;
    }

    public void setData4(String data4) {
        this.data4 = data4;
    }

    
}
