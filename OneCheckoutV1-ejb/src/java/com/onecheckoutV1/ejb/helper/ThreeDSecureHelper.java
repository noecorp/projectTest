/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author Opiks
 */
public class ThreeDSecureHelper implements Serializable {

    private static final long serialVersionUID = 1L;
    private String merchantData = "";
    private String paRes = "";
    private String title = "";
    private boolean parseStatus = false;
    private HashMap<String, String> additionalData = null;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getMerchantData() {
        return merchantData;
    }

    public void setMerchantData(String merchantData) {
        this.merchantData = merchantData;
    }

    public String getPaRes() {
        return paRes;
    }

    public void setPaRes(String paRes) {
        this.paRes = paRes;
    }
}
