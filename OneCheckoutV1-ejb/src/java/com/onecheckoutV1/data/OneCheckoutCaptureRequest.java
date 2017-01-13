/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidCaptureRequestException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;

/**
 *
 * @author Opiks
 */
public class OneCheckoutCaptureRequest extends OneCheckoutBaseRules {
    private int MALLID;//X
    private String TRANSIDMERCHANT;//X
    private String SESSIONID;//X
    private String WORDS;//X

    private String VOIDRESPONSE;

    private String orderNumber;
    private String voidStatus;
    private String responseCode;
    private String approvalCode;
    private Double amount;
    private Double amountMDR;
    private String statusCode;
    private String type;

    //TAMBAHAN MPG
    private int CHAINMERCHANT;//X
    private String MID;
    private String TID;
    private String BANK;
    private String CURRENCY;
    private String TRXCODE;
    private String CARDNUMBER;
    private String MESSAGE;

    public OneCheckoutCaptureRequest(String mallId) {

        try {

            this.MALLID = super.validateMALLID1(mallId);

        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidCaptureRequestException(iv.getMessage(), this, iv);
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
    public void setTRANSIDMERCHANT(String transidmerchant) {
        try {
            this.TRANSIDMERCHANT = super.setTransIdMerchant(transidmerchant);
        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidCaptureRequestException(iv.getMessage(), this, iv);
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
    public void setWORDS(String words) {
       // this.WORDS = WORDS;
        try {
            this.WORDS = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 300, "WORDS", words, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);

        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidCaptureRequestException(iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the VOIDDATETIME
     */
//    public Date getVOIDDATETIME() {
//        return VOIDDATETIME;
//    }

    /**
     * @param VOIDDATETIME the VOIDDATETIME to set
     */
//    public void setVOIDDATETIME(String requestDatetime) {
//        try {
 //           this.VOIDDATETIME = super.validateDATETIME(requestDatetime,"VOIDDATETIME");
//        } catch (OneCheckoutInvalidInputException iv) {
//            throw new InvalidVoidRequestException(iv.getMessage(), this, iv);
//        }
//    }


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
        this.SESSIONID = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 48,"SESSIONID", sessionId, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    /**
     * @return the VOIDRESPONSE
     */
    public String getVOIDRESPONSE() {
        return VOIDRESPONSE;
    }

    /**
     * @param VOIDRESPONSE the VOIDRESPONSE to set
     */
    public void setVOIDRESPONSE(String VOIDRESPONSE) {
        this.VOIDRESPONSE = VOIDRESPONSE;
    }

    /**
     * @return the responseCode
     */
    public String getResponseCode() {
        return responseCode;
    }

    /**
     * @param responseCode the responseCode to set
     */
    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * @return the approvalCode
     */
    public String getApprovalCode() {
        return approvalCode;
    }

    /**
     * @param approvalCode the approvalCode to set
     */
    public void setApprovalCode(String approvalCode) {
        this.approvalCode = approvalCode;
    }

    /**
     * @return the voidStatus
     */
    public String getVoidStatus() {
        return voidStatus;
    }

    /**
     * @param voidStatus the voidStatus to set
     */
    public void setVoidStatus(String voidStatus) {
        this.voidStatus = voidStatus;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the orderNumber
     */
    public String getOrderNumber() {
        return orderNumber;
    }

    /**
     * @param orderNumber the orderNumber to set
     */
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    /**
     * @return the statusCode
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the amount
     */
    public Double getAmount() {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(String amount) {
        try {
            this.amount = super.validateAmount(amount,"AMOUNT");

        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidCaptureRequestException(iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the amountMDR
     */
    public Double getAmountMDR() {
        return amountMDR;
    }

    /**
     * @param amountMDR the amountMDR to set
     */
    public void setAmountMDR(String amountMDR) {
         try {
            this.amountMDR = super.validateAmount(amountMDR,"AMOUNT");

        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidCaptureRequestException(iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the CHAINMERCHANT
     */
    public int getCHAINMERCHANT() {
        return CHAINMERCHANT;
    }

    /**
     * @param CHAINMERCHANT the CHAINMERCHANT to set
     */
    public void setCHAINMERCHANT(int CHAINMERCHANT) {
        this.CHAINMERCHANT = CHAINMERCHANT;
    }

    /**
     * @return the MID
     */
    public String getMID() {
        return MID;
    }

    /**
     * @param MID the MID to set
     */
    public void setMID(String MID) {
        this.MID = MID;
    }

    /**
     * @return the TID
     */
    public String getTID() {
        return TID;
    }

    /**
     * @param TID the TID to set
     */
    public void setTID(String TID) {
        this.TID = TID;
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
     * @return the TRXCODE
     */
    public String getTRXCODE() {
        return TRXCODE;
    }

    /**
     * @param TRXCODE the TRXCODE to set
     */
    public void setTRXCODE(String TRXCODE) {
        this.TRXCODE = TRXCODE;
    }

    /**
     * @return the CARDNUMBER
     */
    public String getCARDNUMBER() {
        return CARDNUMBER;
    }

    /**
     * @param CARDNUMBER the CARDNUMBER to set
     */
    public void setCARDNUMBER(String CARDNUMBER) {
        this.CARDNUMBER = CARDNUMBER;
    }

    /**
     * @return the MESSAGE
     */
    public String getMESSAGE() {
        return MESSAGE;
    }

    /**
     * @param MESSAGE the MESSAGE to set
     */
    public void setMESSAGE(String MESSAGE) {
        this.MESSAGE = MESSAGE;
    }
}
