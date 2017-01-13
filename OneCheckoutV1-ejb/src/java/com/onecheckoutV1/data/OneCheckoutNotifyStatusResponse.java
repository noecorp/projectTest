/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import java.io.Serializable;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutNotifyStatusResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private String ACKNOWLEDGE; 
    private boolean STATUS;
    private boolean NOTIFYEMAIL;
    private int HTTP_RESPONSE_CODE;
    
    public OneCheckoutNotifyStatusResponse() {
 //       TRANS = trans;
        this.ACKNOWLEDGE = null;
        this.STATUS = false;
        this.HTTP_RESPONSE_CODE = -10;
        this.NOTIFYEMAIL = false;
    }
    
    /**
     * @return the RESPONSE_STATUS
     */
    public boolean getSTATUS() {
        return STATUS;
    }

    /**
     * @param RESPONSE_STATUS the RESPONSE_STATUS to set
     */
    public void setSTATUS(String ackowledge,boolean continueForTimeout) {
    
        if (ackowledge != null && ackowledge.toUpperCase().indexOf("CONTINUE") >= 0) {
            STATUS = true;
        }  else if (continueForTimeout) {
            STATUS = true;
            this.NOTIFYEMAIL = true;            
        } else if (ackowledge != null && ackowledge.toUpperCase().indexOf("STOP") >= 0) {
            STATUS = false;

        } else {
            STATUS = true;
            this.NOTIFYEMAIL = true;
        }

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
     * @return the HTTP_RESPONSE_CODE
     */
    public int getHTTP_RESPONSE_CODE() {
        return HTTP_RESPONSE_CODE;
    }

    /**
     * @param HTTP_RESPONSE_CODE the HTTP_RESPONSE_CODE to set
     */
    public void setHTTP_RESPONSE_CODE(int HTTP_RESPONSE_CODE) {
        this.HTTP_RESPONSE_CODE = HTTP_RESPONSE_CODE;
    }

    /**
     * @return the NOTIFYEMAIL
     */
    public boolean isNOTIFYEMAIL() {
        return NOTIFYEMAIL;
    }

    public String getMsgFlagEmail() {
        if (this.NOTIFYEMAIL)
            return "NOTIFYMERCHANT1";
        else
            return "";
    }
 
            
}
