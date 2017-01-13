/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidEDSUpdateStatusDataException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutEDSUpdateStatusData  extends OneCheckoutBaseRules {

    private String TRANSIDMERCHANT;
 //   private double AMOUNT;
    private String WORDS;
    private OneCheckoutDFSStatus STATUS;
    private String REASON;
    private String ACKNOWLEDGE;    
    
    public OneCheckoutEDSUpdateStatusData() {
        ACKNOWLEDGE = "";
    }
    
    /**
     * @return the AMOUNT
     */
//    public double getAMOUNT() {
//        return AMOUNT;
//    }

    /**
     * @param AMOUNT the AMOUNT to set
     */
//    public void setAMOUNT(String amount) {
        
//        try {
//            this.AMOUNT = super.validateAmount(amount,"AMOUNT");

//        } catch (OneCheckoutInvalidInputException iv) {
//            throw new InvalidEDSUpdateStatusRequestException(iv.getMessage(), this, iv);
//        }   
 //   }
 
    
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
            throw new InvalidEDSUpdateStatusDataException(iv.getMessage(), this, iv);
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
     * @return the STATUS
     */
    public OneCheckoutDFSStatus getSTATUS() {
        return STATUS;
    }

    /**
     * @param STATUS the STATUS to set
     */
    public void setSTATUS(String status) {

        
        if (status!=null && status.toUpperCase().indexOf("PASS") >= 0)            
            this.STATUS = OneCheckoutDFSStatus.APPROVE;
        else if (status!=null && status.toUpperCase().indexOf("VOID") >= 0)
            this.STATUS = OneCheckoutDFSStatus.REJECT;        
        
    }

    /**
     * @return the REASON
     */
    public String getREASON() {
        return REASON;
    }

    /**
     * @param REASON the REASON to set
     */
    public void setREASON(String REASON) {
        this.REASON = REASON;
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
            
    
}
