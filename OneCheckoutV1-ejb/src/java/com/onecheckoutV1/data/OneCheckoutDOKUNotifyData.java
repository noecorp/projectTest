/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidDOKUNotifyDataException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import java.text.ParseException;
import java.util.Date;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutDOKUNotifyData  extends OneCheckoutBaseRules{

    private String SESSIONID;
    private String TRANSIDMERCHANT;
    private double AMOUNT;
    private String WORDS;
    private String CARDNUMBER;
    private String BANK;
    private String RESPONSECODE;
    private String APPROVALCODE;
    private String RESULT;
    private String RESULTMSG;
    private String HOSTREFNUM;
    private String KLIKBCA_MERCHANTCODE;
    private String KLIKBCA_USERID;
    private String ACKNOWLEDGE;
    private OneCheckoutDFSStatus DFSStatus;
    private int DFSScore;
    private String DFSId;
    private Date REQUESPAYMENTDATETIME;
    private String MESSAGE;
    private String TRXCODE;

    private String DOKUPAYID;
    private String PAYCODE;

    // PERMATA VA
    private String PERMATA_VANUMBER;
    private String PERMATA_MALLID;
    private String PERMATA_TRACENO;
    private String PERMATA_STEP;
    private String VA_AGREGATOR_STEP;

    // SINAR MAS
    private String SINARMAS_VANUMBER;
    private String SINARMAS_MALLID;
    private String SINARMAS_TRACENO;
    private String SINARMAS_STEP;

    // Add Parameter for Void (BackOffice) / Reversal ()
    private String CURRENCY;
    private String TYPE;
    private int CHAINNUM;
    private int MALLID;

    // DOKU AGREGATOR
    private String VAID;
    private String MERCHANTCODE;
    private String USERACCOUNT;
    private String TRANSACTIONID;
    private String STEP;
    private String CHANNELID;
    private String COMPANYCODE;

    //MPG
    private String THREEDSECURESTATUS = "";
    private String LIABILITY = "NA";

    //RECUR AB TOKENIZATION
    private String STATUSTYPE = "";
    private String TOKENID = "";
    private String VERIFYID = "";
    private String VERIFYSCORE = "";
    private String VERIFYSTATUS = "";
    private String CUSTOMERID = "";
    
    // PERMATA VA
    private String BCA_VANUMBER;
    private String BCA_MALLID;
    private String BCA_TRACENO;
    private String BCA_STEP;
    
    public OneCheckoutDOKUNotifyData() {
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
            throw new InvalidDOKUNotifyDataException(iv.getMessage(), this, iv);
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
            throw new InvalidDOKUNotifyDataException(iv.getMessage(), this, iv);
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
     * @return the KLIKBCA_USERID
     */
    public String getKLIKBCA_USERID() {
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


/*
    private String paramTransIDMerchant = "transactionNo";
    private String paramUserId = "userId";
    private String paramTransactionDate = "transactionDate";
    private String paramAmount = "amount";
    private String paramStatus = "status";
    private String paramAdditionalInfo = "additionalInfo";

    private String paramOrdernumber = "OrderNumber";
    private String paramResponseCode = "RESPONSECODE";
    private String paramSessionId = "SESSIONID";
    private String paramCardNumber = "CARDNUMBER";
    private String paramBank = "BANK";
    private String paramApprovalCode = "APPROVALCODE";
    private String paramWords = "WORDS";
    private String paramResult = "RESULT";


    private String paramOrderNumber = "OrderNumber";
    private String paramResponseCode = "RESPONSECODE";
    private String paramHostRefNum = "HOSTREFNUM";
    private String paramResult = "RESULT";
    private String paramApprovalCode="APPROVALCODE";
    private String paramWords="WORDS";
    private String paramSessionId="SESSIONID";
    private String paramResultMsg="RESULTMSG";
*/

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
     * @return the RESPONSECODE
     */
    public String getRESPONSECODE() {
        return RESPONSECODE;
    }

    /**
     * @param RESPONSECODE the RESPONSECODE to set
     */
    public void setRESPONSECODE(String RESPONSECODE) {
        this.RESPONSECODE = RESPONSECODE;
    }

    /**
     * @return the APPROVALCODE
     */
    public String getAPPROVALCODE() {
        return APPROVALCODE;
    }

    /**
     * @param APPROVALCODE the APPROVALCODE to set
     */
    public void setAPPROVALCODE(String APPROVALCODE) {
        if (APPROVALCODE==null)
            this.APPROVALCODE = "";
        else
            this.APPROVALCODE = APPROVALCODE;
    }

    /**
     * @return the RESULT
     */
    public String getRESULT() {
        return RESULT;
    }

    /**
     * @param RESULT the RESULT to set
     */
    public void setRESULT(String RESULT) {
        this.RESULT = RESULT;
    }

    /**
     * @return the RESULTMSG
     */
    public String getRESULTMSG() {
        return RESULTMSG;
    }

    /**
     * @param RESULTMSG the RESULTMSG to set
     */
    public void setRESULTMSG(String RESULTMSG) {
        this.RESULTMSG = RESULTMSG;
    }

    /**
     * @return the HOSTREFNUM
     */
    public String getHOSTREFNUM() {
        return HOSTREFNUM;
    }

    /**
     * @param HOSTREFNUM the HOSTREFNUM to set
     */
    public void setHOSTREFNUM(String HOSTREFNUM) {
        this.HOSTREFNUM = HOSTREFNUM;
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
     * @return the cyberSourceStatus
     */
    public OneCheckoutDFSStatus getDFSStatus() {
        return DFSStatus;
    }

    /**
     * @param cyberSourceStatus the cyberSourceStatus to set
     */
    public void setDFSStatus(String dfsStatus) {
        if (dfsStatus == null || dfsStatus.isEmpty()) {
            this.DFSStatus = OneCheckoutDFSStatus.NA;
        } else if (dfsStatus.equalsIgnoreCase(OneCheckoutDFSStatus.APPROVE.name()) || dfsStatus.equalsIgnoreCase("ACCEPT")) {
            this.DFSStatus = OneCheckoutDFSStatus.APPROVE;
        } else if (dfsStatus.equalsIgnoreCase(OneCheckoutDFSStatus.REJECT.name())) {
            this.DFSStatus = OneCheckoutDFSStatus.REJECT;
        } else if (dfsStatus.equalsIgnoreCase(OneCheckoutDFSStatus.REVIEW.name())) {
            this.DFSStatus = OneCheckoutDFSStatus.REVIEW;
        } else if (dfsStatus.equalsIgnoreCase(OneCheckoutDFSStatus.HIGHRISK.name())) {
            this.DFSStatus = OneCheckoutDFSStatus.HIGHRISK;
        } else if (dfsStatus.equalsIgnoreCase(OneCheckoutDFSStatus.LOW_RISK.name())) {
            this.DFSStatus = OneCheckoutDFSStatus.APPROVE;
        } else if (dfsStatus.equalsIgnoreCase(OneCheckoutDFSStatus.MEDIUM_RISK.name())) {
            this.DFSStatus = OneCheckoutDFSStatus.REVIEW;
        } else if (dfsStatus.equalsIgnoreCase(OneCheckoutDFSStatus.HIGH_RISK.name())) {
            this.DFSStatus = OneCheckoutDFSStatus.REJECT;
        } else {
            this.DFSStatus = OneCheckoutDFSStatus.NA;
        }
    }
    /*
    public String aliasCyberStatus() {
        if (DFSStatus==OneCheckoutDFSStatus.ACCEPT)
            return "APPROVE";
        else
            return DFSStatus.name();
    }
*/
    /**
     * @return the cyberSourceScore
     */
    public int getDFSScore() {
        return DFSScore;
    }

    /**
     * @param cyberSourceScore the cyberSourceScore to set
     */
    public void setDFSScore(String score) {

        try {

            this.DFSScore = Integer.parseInt(score);

        } catch(Exception e) {
            this.DFSScore = -1;
        }

    }

    /**
     * @return the REQUESPAYMENTDATETIME
     */
    public Date getREQUESPAYMENTDATETIME() {
        return REQUESPAYMENTDATETIME;
    }


    /**
     * @return the cyberSourceRequestId
     */
    public String getDFSId() {
        return DFSId;
    }

    /**
     * @param cyberSourceRequestId the cyberSourceRequestId to set
     */
    public void setDFSIId(String DFSIId) {
        this.DFSId = DFSIId;
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
     * @return the PERMATA_VANUMBER
     */
    public String getPERMATA_VANUMBER() {
        return PERMATA_VANUMBER;
    }

    /**
     * @param PERMATA_VANUMBER the PERMATA_VANUMBER to set
     */
    public void setPERMATA_VANUMBER(String PERMATA_VANUMBER) {
        this.PERMATA_VANUMBER = PERMATA_VANUMBER;
    }

    /**
     * @return the PERMATA_MALLID
     */
    public String getPERMATA_MALLID() {
        return PERMATA_MALLID;
    }

    /**
     * @param PERMATA_MALLID the PERMATA_MALLID to set
     */
    public void setPERMATA_MALLID(String PERMATA_MALLID) {
        this.PERMATA_MALLID = PERMATA_MALLID;
    }

    /**
     * @return the PERMATA_TRACENO
     */
    public String getPERMATA_TRACENO() {
        return PERMATA_TRACENO;
    }

    /**
     * @param PERMATA_TRACENO the PERMATA_TRACENO to set
     */
    public void setPERMATA_TRACENO(String PERMATA_TRACENO) {
        this.PERMATA_TRACENO = PERMATA_TRACENO;
    }

    /**
     * @param REQUESPAYMENTDATETIME the REQUESPAYMENTDATETIME to set
     */
    public void setREQUESPAYMENTDATETIME(String dt)  {
        try {

            if(getPERMATA_VANUMBER() != null && getPERMATA_VANUMBER().length() > 0) {
                this.REQUESPAYMENTDATETIME = OneCheckoutVerifyFormatData.datetimeFormat.parse(dt);
            } else if(getSINARMAS_VANUMBER() != null && getSINARMAS_VANUMBER().length() > 0) {
                this.REQUESPAYMENTDATETIME = OneCheckoutVerifyFormatData.datetimeFormat.parse(dt);
            } else {
                this.REQUESPAYMENTDATETIME = OneCheckoutVerifyFormatData.klikbca_datetimeFormat.parse(dt);
            }

        } catch (ParseException iv) {
            throw new InvalidDOKUNotifyDataException(iv.getMessage(), this, iv);
        }

    }

    /**
     * @param REQUESPAYMENTDATETIME the REQUESPAYMENTDATETIME to set
     */
    public void setREQUESPAYMENTDATETIME(Date dt)  {
        this.REQUESPAYMENTDATETIME = dt;
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
        if(getPERMATA_VANUMBER() != null && getPERMATA_VANUMBER().length() > 0) {
            this.SESSIONID = sessionId;
        } else if(getSINARMAS_VANUMBER() != null && getSINARMAS_VANUMBER().length() > 0) {
            this.SESSIONID = sessionId;
        } else {
            this.SESSIONID = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 200,"SESSIONID", sessionId, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
        }
    }

    /**
     * @return the PERMATA_STEP
     */
    public String getPERMATA_STEP() {
        return PERMATA_STEP;
    }

    /**
     * @param PERMATA_STEP the PERMATA_STEP to set
     */
    public void setPERMATA_STEP(String PERMATA_STEP) {
        this.PERMATA_STEP = PERMATA_STEP;
    }

    /**
     * @return the CHAINNUM
     */
    public int getCHAINNUM() {
        return CHAINNUM;
    }

    /**
     * @param CHAINNUM the CHAINNUM to set
     */
    public void setCHAINNUM(String CHAINNUM) {
    // this.CHAINNUM = CHAINNUM;
        try {

            this.CHAINNUM = super.validateCHAINMERCHANT1(CHAINNUM);

        } catch (OneCheckoutInvalidInputException iv) {
            this.CHAINNUM = 0;
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
            throw new InvalidDOKUNotifyDataException(iv.getMessage(), this, iv);
        }

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
     * @return the MALLID
     */
    public int getMALLID() {
        return MALLID;
    }

    /**
     * @param MALLID the MALLID to set
     */
    public void setMALLID(String mallId) {

        try {

            this.MALLID = super.validateMALLID1(mallId);

        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidDOKUNotifyDataException(iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the VA_AGREGATOR_STEP
     */
    public String getVA_AGREGATOR_STEP() {
        return VA_AGREGATOR_STEP;
    }

    /**
     * @param VA_AGREGATOR_STEP the VA_AGREGATOR_STEP to set
     */
    public void setVA_AGREGATOR_STEP(String VA_AGREGATOR_STEP) {
        this.VA_AGREGATOR_STEP = VA_AGREGATOR_STEP;
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
     * @return the MERCHANTCODE
     */
    public String getMERCHANTCODE() {
        return MERCHANTCODE;
    }

    /**
     * @param MERCHANTCODE the MERCHANTCODE to set
     */
    public void setMERCHANTCODE(String MERCHANTCODE) {
        this.MERCHANTCODE = MERCHANTCODE;
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
     * @return the TRANSACTIONID
     */
    public String getTRANSACTIONID() {
        return TRANSACTIONID;
    }

    /**
     * @param TRANSACTIONID the TRANSACTIONID to set
     */
    public void setTRANSACTIONID(String TRANSACTIONID) {
        this.TRANSACTIONID = TRANSACTIONID;
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
     * @return the CHANNELID
     */
    public String getCHANNELID() {
        return CHANNELID;
    }

    /**
     * @param CHANNELID the CHANNELID to set
     */
    public void setCHANNELID(String CHANNELID) {
        this.CHANNELID = CHANNELID;
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
     * @return the THREEDSECURESTATUS
     */
    public String getTHREEDSECURESTATUS() {
        return THREEDSECURESTATUS;
    }

    /**
     * @param THREEDSECURESTATUS the THREEDSECURESTATUS to set
     */
    public void setTHREEDSECURESTATUS(String THREEDSECURESTATUS) {
        this.THREEDSECURESTATUS = THREEDSECURESTATUS;
    }

    /**
     * @return the LIABILITY
     */
    public String getLIABILITY() {
        return LIABILITY;
    }

    /**
     * @param LIABILITY the LIABILITY to set
     */
    public void setLIABILITY(String LIABILITY) {
        this.LIABILITY = LIABILITY;
    }

    public String getVERIFYSTATUS() {
        return VERIFYSTATUS;
    }

    public void setVERIFYSTATUS(String VERIFYSTATUS) {
        this.VERIFYSTATUS = VERIFYSTATUS;
    }

    public String getVERIFYID() {
        return VERIFYID;
    }

    public void setVERIFYID(String VERIFYID) {
        this.VERIFYID = VERIFYID;
    }

    public String getVERIFYSCORE() {
        return VERIFYSCORE;
    }

    public void setVERIFYSCORE(String VERIFYSCORE) {
        this.VERIFYSCORE = VERIFYSCORE;
    }

    public String getSTATUSTYPE() {
        return STATUSTYPE;
    }

    public void setSTATUSTYPE(String STATUSTYPE) {
        this.STATUSTYPE = STATUSTYPE;
    }

    public String getTOKENID() {
        return TOKENID;
    }

    public void setTOKENID(String TOKENID) {
        this.TOKENID = TOKENID;
    }

    public String getCUSTOMERID() {
        return CUSTOMERID;
    }

    public void setCUSTOMERID(String CUSTOMERID) {
        this.CUSTOMERID = CUSTOMERID;
    }

    /**
     * @return the SINARMAS_VANUMBER
     */
    public String getSINARMAS_VANUMBER() {
        return SINARMAS_VANUMBER;
    }

    /**
     * @param SINARMAS_VANUMBER the SINARMAS_VANUMBER to set
     */
    public void setSINARMAS_VANUMBER(String SINARMAS_VANUMBER) {
        this.SINARMAS_VANUMBER = SINARMAS_VANUMBER;
    }

    /**
     * @return the SINARMAS_MALLID
     */
    public String getSINARMAS_MALLID() {
        return SINARMAS_MALLID;
    }

    /**
     * @param SINARMAS_MALLID the SINARMAS_MALLID to set
     */
    public void setSINARMAS_MALLID(String SINARMAS_MALLID) {
        this.SINARMAS_MALLID = SINARMAS_MALLID;
    }

    /**
     * @return the SINARMAS_TRACENO
     */
    public String getSINARMAS_TRACENO() {
        return SINARMAS_TRACENO;
    }

    /**
     * @param SINARMAS_TRACENO the SINARMAS_TRACENO to set
     */
    public void setSINARMAS_TRACENO(String SINARMAS_TRACENO) {
        this.SINARMAS_TRACENO = SINARMAS_TRACENO;
    }

    /**
     * @return the SINARMAS_STEP
     */
    public String getSINARMAS_STEP() {
        return SINARMAS_STEP;
    }

    /**
     * @param SINARMAS_STEP the SINARMAS_STEP to set
     */
    public void setSINARMAS_STEP(String SINARMAS_STEP) {
        this.SINARMAS_STEP = SINARMAS_STEP;
    }

    public String getBCA_VANUMBER() {
        return BCA_VANUMBER;
    }

    public void setBCA_VANUMBER(String BCA_VANUMBER) {
        this.BCA_VANUMBER = BCA_VANUMBER;
    }

    public String getBCA_MALLID() {
        return BCA_MALLID;
    }

    public void setBCA_MALLID(String BCA_MALLID) {
        this.BCA_MALLID = BCA_MALLID;
    }

    public String getBCA_TRACENO() {
        return BCA_TRACENO;
    }

    public void setBCA_TRACENO(String BCA_TRACENO) {
        this.BCA_TRACENO = BCA_TRACENO;
    }

    public String getBCA_STEP() {
        return BCA_STEP;
    }

    public void setBCA_STEP(String BCA_STEP) {
        this.BCA_STEP = BCA_STEP;
    }
    
}
