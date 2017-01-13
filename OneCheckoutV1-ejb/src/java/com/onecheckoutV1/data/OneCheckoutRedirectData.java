/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidDOKURedirectDataException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutRedirectData  extends OneCheckoutBaseRules {
    
    
    private String TRANSIDMERCHANT;
    private double AMOUNT;
    private String STATUSCODE;
    private String SESSIONID;
    private String PAYMENTCHANNEL;
    private String WORDS;
    private String PAYMENTCODE;

    //CUSTOMIZE TRANSACTION RESULT & 3 ATTEMPTS
    private String FLAG = "0";

    private Date DATENOW;
    private String DISPLAYNAME;
    private String DISPLAYADDRESS;

    private String MSG;
    private String MSGDETAIL;

    private String APRROVALCODE;
    private String TYPE;
    private String BRANDID;
    private String CREDITCARD;
    
    private Boolean REWARDSSTATUS = false;
    private String CURRENCY;
    private double NETAMOUNT;
    private String POINTSREDEEMED;
    private double AMOUNTREDEEMED;
    private String MERCHANTNUM;

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
        } catch (InvalidDOKURedirectDataException iv) {
            throw new InvalidDOKURedirectDataException(iv.getMessage(), this, iv);
        }     
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

        } catch (InvalidDOKURedirectDataException iv) {
            throw new InvalidDOKURedirectDataException(iv.getMessage(), this, iv);
        }           
        
    }

    /**
     * @return the STATUSCODE
     */
    public String getSTATUSCODE() {
        return STATUSCODE;
    }

    /**
     * @param STATUSCODE the STATUSCODE to set
     */
    public void setSTATUSCODE(String statusCode) {
        this.STATUSCODE = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 10,"STATUSCODE", statusCode, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM,"");

       
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
     * @return the PAYMENTCHANNEL
     */
    public String getPAYMENTCHANNEL() {
        return PAYMENTCHANNEL;
    }

    /**
     * @param PAYMENTCHANNEL the PAYMENTCHANNEL to set
     */
    public void setPAYMENTCHANNEL(String PAYMENTCHANNEL) {
        this.PAYMENTCHANNEL = PAYMENTCHANNEL;
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
     * @return the PAYMENTCODE
     */
    public String getPAYMENTCODE() {
        return PAYMENTCODE;
    }

    /**
     * @param PAYMENTCODE the PAYMENTCODE to set
     */
    public void setPAYMENTCODE(String PAYMENTCODE) {
        this.PAYMENTCODE = PAYMENTCODE;
    }

    /**
     * @return the FLAG
     */
    public String getFLAG() {
        return FLAG;
    }

    /**
     * @param FLAG the FLAG to set
     */
    public void setFLAG(String FLAG) {
        if(FLAG == null) {
            this.FLAG = "0";
        } else {
            this.FLAG = FLAG;
        }
    }

    /**
     * @return the DATENOW
     */
    public Date getDATENOW() {
        return DATENOW;
    }

    /**
     * @param DATENOW the DATENOW to set
     */
    public void setDATENOW(String DATENOW) {

        try {
         //   this.REQUESTDATETIME = super.validateDATETIME(requestDatetime,"REQUESTDATETIME");

            String procdtime = OneCheckoutVerifyFormatData.validateString(14, 14, "DATENOW", DATENOW, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

            this.DATENOW = df.parse(procdtime);

            OneCheckoutLogger.log("DATENOW valid format");
        } catch (Throwable iv) {
            OneCheckoutLogger.log("DATENOW invalid format");
            this.DATENOW = new Date();
        }
    }

    /**
     * @return the DISPLAYNAME
     */
    public String getDISPLAYNAME() {
        return DISPLAYNAME;
    }

    /**
     * @param DISPLAYNAME the DISPLAYNAME to set
     */
    public void setDISPLAYNAME(String DISPLAYNAME) {
        this.DISPLAYNAME = DISPLAYNAME;
    }

    /**
     * @return the DISPLAYADDRESS
     */
    public String getDISPLAYADDRESS() {
        return DISPLAYADDRESS;
    }

    /**
     * @param DISPLAYADDRESS the DISPLAYADDRESS to set
     */
    public void setDISPLAYADDRESS(String DISPLAYADDRESS) {
        this.DISPLAYADDRESS = DISPLAYADDRESS;
    }

    /**
     * @return the MSG
     */
    public String getMSG() {
        return MSG;
    }

    /**
     * @param MSG the MSG to set
     */
    public void setMSG(String MSG) {
        this.MSG = MSG;
    }

    /**
     * @return the MSGDETAIL
     */
    public String getMSGDETAIL() {
        return MSGDETAIL;
    }

    /**
     * @param MSGDETAIL the MSGDETAIL to set
     */
    public void setMSGDETAIL(String MSGDETAIL) {
        this.MSGDETAIL = MSGDETAIL;
    }

    /**
     * @return the APRROVALCODE
     */
    public String getAPRROVALCODE() {
        return APRROVALCODE;
    }

    /**
     * @param APRROVALCODE the APRROVALCODE to set
     */
    public void setAPRROVALCODE(String APRROVALCODE) {
        this.APRROVALCODE = APRROVALCODE;
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

    /**
     * @return the BRANDID
     */
    public String getBRANDID() {
        return BRANDID;
    }

    /**
     * @param BRANDID the BRANDID to set
     */
    public void setBRANDID(String BRANDID) {
        this.BRANDID = BRANDID;
    }

    /**
     * @return the CREDITCARD
     */
    public String getCREDITCARD() {
        return CREDITCARD;
    }

    /**
     * @param CREDITCARD the CREDITCARD to set
     */
    public void setCREDITCARD(String CREDITCARD) {
        this.CREDITCARD = CREDITCARD;
    }

    /**
     * @return the REWARDSSTATUS
     */
    public Boolean getREWARDSSTATUS() {
        return REWARDSSTATUS;
    }

    /**
     * @param REWARDSSTATUS the REWARDSSTATUS to set
     */
    public void setREWARDSSTATUS(String REWARDSSTATUS) {
        //this.REWARDSSTATUS = REWARDSSTATUS;
        try {
            this.REWARDSSTATUS = Boolean.valueOf(REWARDSSTATUS);
        } catch (Throwable t) {
            this.REWARDSSTATUS = Boolean.FALSE;
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
    public void setCURRENCY(String CURRENCY) {
        this.CURRENCY = CURRENCY;
    }

    /**
     * @return the NETAMOUNT
     */
    public double getNETAMOUNT() {
        return NETAMOUNT;
    }

    /**
     * @param NETAMOUNT the NETAMOUNT to set
     */
    public void setNETAMOUNT(String NETAMOUNT) {

        try {

            this.NETAMOUNT = super.validateAmount(NETAMOUNT,"NETAMOUNT");

        } catch (InvalidDOKURedirectDataException iv) {
            throw new InvalidDOKURedirectDataException(iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the POINTSREDEEMED
     */
    public String getPOINTSREDEEMED() {
        return POINTSREDEEMED;
    }

    /**
     * @param POINTSREDEEMED the POINTSREDEEMED to set
     */
    public void setPOINTSREDEEMED(String POINTSREDEEMED) {
        this.POINTSREDEEMED = POINTSREDEEMED;
    }

    /**
     * @return the AMOUNTREDEEMED
     */
    public double getAMOUNTREDEEMED() {
        return AMOUNTREDEEMED;
    }

    /**
     * @param AMOUNTREDEEMED the AMOUNTREDEEMED to set
     */
    public void setAMOUNTREDEEMED(String AMOUNTREDEEMED) {
        try {

            this.AMOUNTREDEEMED = super.validateAmount(AMOUNTREDEEMED,"AMOUNTREDEEMED");

        } catch (InvalidDOKURedirectDataException iv) {
            throw new InvalidDOKURedirectDataException(iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the MERCHANTNUM
     */
    public String getMERCHANTNUM() {
        return MERCHANTNUM;
    }

    /**
     * @param MERCHANTNUM the MERCHANTNUM to set
     */
    public void setMERCHANTNUM(String MERCHANTNUM) {
        this.MERCHANTNUM = MERCHANTNUM;
    }
    
}