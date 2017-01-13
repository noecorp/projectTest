/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidEDSGetDataException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutEDSGetData extends OneCheckoutBaseRules {
    
    
    private String TRANSIDMERCHANT;
    private double AMOUNT;
    private String WORDS;
    private String ACKNOWLEDGE;
    private boolean NULLTRANSACTION = true;
    

    public OneCheckoutEDSGetData() {
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
            throw new InvalidEDSGetDataException(iv.getMessage(), this, iv);
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
            throw new InvalidEDSGetDataException(iv.getMessage(), this, iv);
        }                           
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
     * @return the NULLTRANSACTION
     */
    public boolean isNULLTRANSACTION() {
        return NULLTRANSACTION;
    }

    /**
     * @param NULLTRANSACTION the NULLTRANSACTION to set
     */
    public void setNULLTRANSACTION(boolean NULLTRANSACTION) {
        this.NULLTRANSACTION = NULLTRANSACTION;
    }
        
    
}