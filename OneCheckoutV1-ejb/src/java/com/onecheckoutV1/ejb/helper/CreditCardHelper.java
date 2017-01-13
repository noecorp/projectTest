/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Opiks
 */
public class CreditCardHelper implements Serializable {

    private static final long serialVersionUID = 1L;
    private String maskCardNumber = "";
    private String cardNumber = "";
    private String expMonth = "";
    private String expYear = "";
    private String cvv2 = "";
    private String ccAddress = "";
    private String ccName = "";
    private String ccEmail = "";
    private String ccCity = "";
    private String ccRegion = "";
    private String ccCountry = "";
    private String ccMobilePhone = "";
    private String ccHomePhone = "";
    private String ccWorkPhone = "";
    private String ccZipCode = "";
    private String tokenNumber = "";
    private String button = "";
    private boolean authStatus = false;
    private boolean parseStatus = false;
    private HashMap<String, String> additionalData;
    private String ccsaveStatus = null;
    

    public String getMaskCardNumber() {
        String maskedCard = "";
        try {
            if (this.cardNumber != null) {
                maskedCard = StringUtils.overlay(this.cardNumber, StringUtils.repeat("*", this.cardNumber.length()-10), 6, this.cardNumber.length()-4);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return maskedCard;
    }

    public void setMaskCardNumber(String maskCardNumber) {
        this.maskCardNumber = maskCardNumber;
    }

    public String getButton() {
        return button;
    }

    public void setButton(String button) {
        this.button = button;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getCcAddress() {
        return ccAddress;
    }

    public void setCcAddress(String ccAddress) {
        this.ccAddress = ccAddress;
    }

    public String getCvv2() {
        return cvv2;
    }

    public void setCvv2(String cvv2) {
        this.cvv2 = cvv2;
    }

    public HashMap<String, String> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(HashMap<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    public boolean isAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(boolean authStatus) {
        this.authStatus = authStatus;
    }

    public boolean isParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(boolean parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCcCity() {
        return ccCity;
    }

    public void setCcCity(String ccCity) {
        this.ccCity = ccCity;
    }

    public String getCcCountry() {
        return ccCountry;
    }

    public void setCcCountry(String ccCountry) {
        this.ccCountry = ccCountry;
    }

    public String getCcEmail() {
        return ccEmail;
    }

    public void setCcEmail(String ccEmail) {
        this.ccEmail = ccEmail;
    }

    public String getCcHomePhone() {
        return ccHomePhone;
    }

    public void setCcHomePhone(String ccHomePhone) {
        this.ccHomePhone = ccHomePhone;
    }

    public String getCcMobilePhone() {
        return ccMobilePhone;
    }

    public void setCcMobilePhone(String ccMobilePhone) {
        this.ccMobilePhone = ccMobilePhone;
    }

    public String getCcName() {
        return ccName;
    }

    public void setCcName(String ccName) {
        this.ccName = ccName;
    }

    public String getCcRegion() {
        return ccRegion;
    }

    public void setCcRegion(String ccRegion) {
        this.ccRegion = ccRegion;
    }

    public String getCcWorkPhone() {
        return ccWorkPhone;
    }

    public void setCcWorkPhone(String ccWorkPhone) {
        this.ccWorkPhone = ccWorkPhone;
    }

    public String getCcZipCode() {
        return ccZipCode;
    }

    public void setCcZipCode(String ccZipCode) {
        this.ccZipCode = ccZipCode;
    }

    public String getExpMonth() {
        return expMonth;
    }

    public void setExpMonth(String expMonth) {
        this.expMonth = expMonth;
    }

    public String getExpYear() {
        return expYear;
    }

    public void setExpYear(String expYear) {
        this.expYear = expYear;
    }

    public String getCcsaveStatus() {
        return ccsaveStatus;
    }

    public void setCcsaveStatus(String ccsaveStatus) {
        this.ccsaveStatus = ccsaveStatus;
    }
    
}
