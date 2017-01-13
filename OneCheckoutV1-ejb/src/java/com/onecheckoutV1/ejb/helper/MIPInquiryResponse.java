/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author Opiks
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MIPInquiryResponse  implements Serializable {

    private static final long serialVersionUID = 1L;
    private String responseCode;
    private String responseMsg;
    private String dpMallId;
    private String transIdMerchant;
    private String inquiryCode;
    private String dokuId;
    private String customerName;
    private String customerEmail;
    private List<PaymentChannel> listPaymentChannel;
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

    public String getDpMallId() {
        return dpMallId;
    }

    public void setDpMallId(String dpMallId) {
        this.dpMallId = dpMallId;
    }

    public String getTransIdMerchant() {
        return transIdMerchant;
    }

    public void setTransIdMerchant(String transIdMerchant) {
        this.transIdMerchant = transIdMerchant;
    }

    public String getInquiryCode() {
        return inquiryCode;
    }

    public void setInquiryCode(String inquiryCode) {
        this.inquiryCode = inquiryCode;
    }

    public String getDokuId() {
        return dokuId;
    }

    public void setDokuId(String dokuId) {
        this.dokuId = dokuId;
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

    public List<PaymentChannel> getListPaymentChannel() {
        return listPaymentChannel;
    }

    public void setListPaymentChannel(List<PaymentChannel> listPaymentChannel) {
        this.listPaymentChannel = listPaymentChannel;
    }

    public String getCallBackData() {
        return callBackData;
    }

    public void setCallBackData(String callBackData) {
        this.callBackData = callBackData;
    }

    //:::: ------------------------------------------
    public static class PaymentChannel implements Serializable{

        private String channelCode;
        private String channelName;
        private Object details;

        public String getChannelCode() {
            return channelCode;
        }

        public void setChannelCode(String channelCode) {
            this.channelCode = channelCode;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        public Object getDetails() {
            return details;
        }

        public void setDetails(Object details) {
            this.details = details;
        }
    }

    public static class DetailsOfCash implements Serializable{

        private BigDecimal lastBalance;

        public DetailsOfCash() {
        }

        public DetailsOfCash(BigDecimal lastBalance) {
            this.lastBalance = lastBalance;
        }

        public BigDecimal getLastBalance() {
            return lastBalance;
        }

        public void setLastBalance(BigDecimal lastBalance) {
            this.lastBalance = lastBalance;
        }
    }

    public static class DetailsOfCC implements Serializable{

        private String linkId;
        private String cardNoMasked;
        private String cardName;
        private String cardCountry;
        private String cardPhone;
        private String cardEmail;
        private String cardCity;
        private String cardZipCode;
        private String cardNoEncrypt;
        private String cardExpiryDateEncrypt;

        public DetailsOfCC() {
        }

        public DetailsOfCC(String linkId, String cardNoMasked, String cardName, String cardCountry, String cardPhone, String cardEmail, String cardCity, String cardZipCode, String cardNoEncrypt, String cardExpiryDateEncrypt) {
            this.linkId = linkId;
            this.cardNoMasked = cardNoMasked;
            this.cardName = cardName;
            this.cardCountry = cardCountry;
            this.cardPhone = cardPhone;
            this.cardEmail = cardEmail;
            this.cardCity = cardCity;
            this.cardZipCode = cardZipCode;
            this.cardNoEncrypt = cardNoEncrypt;
            this.cardExpiryDateEncrypt = cardExpiryDateEncrypt;
        }

        public String getLinkId() {
            return linkId;
        }

        public void setLinkId(String linkId) {
            this.linkId = linkId;
        }

        public String getCardNoMasked() {
            return cardNoMasked;
        }

        public void setCardNoMasked(String cardNoMasked) {
            this.cardNoMasked = cardNoMasked;
        }

        public String getCardNoEncrypt() {
            return cardNoEncrypt;
        }

        public void setCardNoEncrypt(String cardNoEncrypt) {
            this.cardNoEncrypt = cardNoEncrypt;
        }

        public String getCardExpiryDateEncrypt() {
            return cardExpiryDateEncrypt;
        }

        public void setCardExpiryDateEncrypt(String cardExpiryDateEncrypt) {
            this.cardExpiryDateEncrypt = cardExpiryDateEncrypt;
        }

        public String getCardName() {
            return cardName;
        }

        public void setCardName(String cardName) {
            this.cardName = cardName;
        }

        public String getCardCountry() {
            return cardCountry;
        }

        public void setCardCountry(String cardCountry) {
            this.cardCountry = cardCountry;
        }

        public String getCardPhone() {
            return cardPhone;
        }

        public void setCardPhone(String cardPhone) {
            this.cardPhone = cardPhone;
        }

        public String getCardEmail() {
            return cardEmail;
        }

        public void setCardEmail(String cardEmail) {
            this.cardEmail = cardEmail;
        }

        public String getCardCity() {
            return cardCity;
        }

        public void setCardCity(String cardCity) {
            this.cardCity = cardCity;
        }

        public String getCardZipCode() {
            return cardZipCode;
        }

        public void setCardZipCode(String cardZipCode) {
            this.cardZipCode = cardZipCode;
        }
    }
}
