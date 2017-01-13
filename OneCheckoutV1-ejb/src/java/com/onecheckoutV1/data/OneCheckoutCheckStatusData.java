/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidCheckStatusRequestException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;

/**
 *
 * @author aditya
 */
public class OneCheckoutCheckStatusData extends OneCheckoutBaseRules {

    private int MALLID;
    private String TRANSIDMERCHANT;
    private String SESSIONID;
    private String ACKNOWLEDGE;
    private String WORDS;

    public OneCheckoutCheckStatusData(String mallId) {

        try {

            this.MALLID = super.validateMALLID1(mallId);

        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidCheckStatusRequestException(iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the MALLID
     */
    public int getMALLID() {
        return MALLID;
    }

    /**
     * @param MALLID the MALLID to set
     */
    public void setMALLID(int MALLID) {
        this.MALLID = MALLID;
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
        try {
            this.TRANSIDMERCHANT = super.setTransIdMerchant(TRANSIDMERCHANT);
        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidCheckStatusRequestException(iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the SESSIONID
     */
    public String getSESSIONID() {
        return SESSIONID;
    }

    /**
     * @param SESSIONID the SESSIONID to set
     */
    public void setSESSIONID(String SESSIONID) {
        this.SESSIONID = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 48,"SESSIONID", SESSIONID, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    /**
     * @return the ACKNOWLEDGE
     */
    public String getACKNOWLEDGE() {
        return ACKNOWLEDGE;
    }

    /**
     * @param ACKNOWLEDGE the ACKNOWLEDGE to set
     */
    public void setACKNOWLEDGE(String ACKNOWLEDGE) {
        this.ACKNOWLEDGE = ACKNOWLEDGE;
    }

    /**
     * @return the WORDS
     */
    public String getWORDS() {
        return WORDS;
    }

    /**
     * @param WORDS the WORDS to set
     */
    public void setWORDS(String WORDS) {
        this.WORDS = WORDS;
    }
    
}