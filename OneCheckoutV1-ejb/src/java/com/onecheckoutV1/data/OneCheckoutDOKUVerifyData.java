/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidDOKUVerifyDataException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutDOKUVerifyData extends OneCheckoutBaseRules {

    
    private String SESSIONID;
    private String TRANSIDMERCHANT;
    private double AMOUNT;
    private String CURRENCY;
    private String WORDS;
    private String ACKNOWLEDGE;
    private String MALLID;

    //Nambah Karena BSP
    private String CHAINMERCHANT;
    private String TYPE;

    // BCA
    private String KLIKBCA_USERID;
    private String KLIKBCA_MERCHANTCODE;

    // DOKUPAY
    private String DOKUPAYID;

    // PERMATA VA
    private String VANUMBER;// aka va number
 //   private String BANK;
    private String TRACENO;

    //DOKU AGREGATOR
    private String VAID;
    private String USERACCOUNT;
    private String COMPANYCODE;
    private String BANK;
    private String STEP;


    private String PAYCODE;
    
    public OneCheckoutDOKUVerifyData() {
        ACKNOWLEDGE = "";
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
    public void setAMOUNT(String amount) {
        
        try {
            this.AMOUNT = super.validateAmount(amount,"AMOUNT");

        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage(), this, iv);
        }   
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
    public void setTRANSIDMERCHANT(String transidmerchant) {
        try {        
            this.TRANSIDMERCHANT = super.setTransIdMerchant(transidmerchant); 
        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage(), this, iv);
        }                           
    }
    
    
    /**
     * @return the CURRENCY
     */
    public String getCURRENCY() {
        return CURRENCY;
    }

    /**
     * @param CURRENCY the CURRENCY to set
     */
    public void setCURRENCY(String currency) {

        try {
            
            CURRENCY = OneCheckoutVerifyFormatData.validateString(3, 3, "CURRENCY", currency, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            
        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage(), this, iv);
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
    public void setSESSIONID(String sessionId) {
        this.SESSIONID = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 200,"SESSIONID", sessionId, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
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

    /**
     * @return the KlikBCA_UserId
     */
    public String getKlikBCA_USERID() {
        return KLIKBCA_USERID;
    }

    /**
     * @param KlikBCA_UserId the KlikBCA_UserId to set
     */
    public void setKLIKBCA_USERID(String KlikBCA_UserId) {
        this.KLIKBCA_USERID = KlikBCA_UserId;
    }

    /**
     * @return the KlikBCA_MerchantCode
     */
    public String getKLIKBCA_MERCHANTCODE() {
        return KLIKBCA_MERCHANTCODE;
    }

    /**
     * @param KlikBCA_MerchantCode the KlikBCA_MerchantCode to set
     */
    public void setKLIKBCA_MERCHANTCODE(String KlikBCA_MerchantCode) {
        this.KLIKBCA_MERCHANTCODE = KlikBCA_MerchantCode;
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
     * @return the DOKUPAYID
     */
    public String getDOKUPAYID() {
        return DOKUPAYID;
    }

    /**
     * @param DOKUPAYID the DOKUPAYID to set
     */
    public void setDOKUPAYID(String DOKUPAYID) {
        this.DOKUPAYID = DOKUPAYID;
    }

    /**
     * @return the PERMATA_VANUMBER
     */
    public String getVANUMBER() {
        return VANUMBER;
    }

    /**
     * @param PERMATA_VANUMBER the PERMATA_VANUMBER to set
     */
    public void setVANUMBER(String VANUMBER) {
        this.VANUMBER = VANUMBER;
    }

//    /**
//     * @return the PERMATA_BANK
//     */
//    public String getPERMATA_BANK() {
//        return PERMATA_BANK;
//    }
//
//    /**
//     * @param PERMATA_BANK the PERMATA_BANK to set
//     */
//    public void setPERMATA_BANK(String PERMATA_BANK) {
//        this.PERMATA_BANK = PERMATA_BANK;
//    }

    /**
     * @return the PERMATA_TRACENO
     */
    public String getTRACENO() {
        return TRACENO;
    }

    /**
     * @param PERMATA_TRACENO the PERMATA_TRACENO to set
     */
    public void setTRACENO(String TRACENO) {
        this.TRACENO = TRACENO;
    }

    /**
     * @return the MALLID
     */
    public String getMALLID() {
        return MALLID;
    }

    /**
     * @param MALLID the MALLID to set
     */
    public void setMALLID(String MALLID) {
        this.MALLID = MALLID;
    }

    /**
     * @return the VAID
     */
    public String getVAID() {
        return VAID;
    }

    /**
     * @param VAID the VAID to set
     */
    public void setVAID(String VAID) {
        this.VAID = VAID;
    }

    /**
     * @return the USERACCOUNT
     */
    public String getUSERACCOUNT() {
        return USERACCOUNT;
    }

    /**
     * @param USERACCOUNT the USERACCOUNT to set
     */
    public void setUSERACCOUNT(String USERACCOUNT) {
        this.USERACCOUNT = USERACCOUNT;
    }

    /**
     * @return the COMPANYCODE
     */
    public String getCOMPANYCODE() {
        return COMPANYCODE;
    }

    /**
     * @param COMPANYCODE the COMPANYCODE to set
     */
    public void setCOMPANYCODE(String COMPANYCODE) {
        this.COMPANYCODE = COMPANYCODE;
    }

    /**
     * @return the BANK
     */
    public String getBANK() {
        return BANK;
    }

    /**
     * @param BANK the BANK to set
     */
    public void setBANK(String BANK) {
        this.BANK = BANK;
    }

    /**
     * @return the STEP
     */
    public String getSTEP() {
        return STEP;
    }

    /**
     * @param STEP the STEP to set
     */
    public void setSTEP(String STEP) {
        this.STEP = STEP;
    }

    /**
     * @return the PAYCODE
     */
    public String getPAYCODE() {
        return PAYCODE;
    }

    /**
     * @param PAYCODE the PAYCODE to set
     */
    public void setPAYCODE(String PAYCODE) {
        this.PAYCODE = PAYCODE;
    }

    /**
     * @return the CHAINMERCHANT
     */
    public String getCHAINMERCHANT() {
        return CHAINMERCHANT;
    }

    /**
     * @param CHAINMERCHANT the CHAINMERCHANT to set
     */
    public void setCHAINMERCHANT(String CHAINMERCHANT) {
        this.CHAINMERCHANT = CHAINMERCHANT;
    }

    /**
     * @return the TYPE
     */
    public String getTYPE() {
        return TYPE;
    }

    /**
     * @param TYPE the TYPE to set
     */
    public void setTYPE(String TYPE) {
        this.TYPE = TYPE;
    }

}