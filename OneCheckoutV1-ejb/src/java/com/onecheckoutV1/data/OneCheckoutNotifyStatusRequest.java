/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Rates;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutNotifyStatusRequest {
    
    private double AMOUNT;
    private double PURCHASEAMOUNT;
    private Rates RATE;
    private String STATUSTYPE;
    private String TRANSIDMERCHANT;
    private String WORDS;    
    private String RESPONSECODE;
    private String APPROVALCODE;
    private String RESULTMSG;
    private String PAYMENTCHANNEL;
    private String PAYMENTCODE;
    private String SESSIONID;
    private String BANK;
    private String MCN;
    private String PAYMENTDATETIME;
    private String VERIFYSCORE;
    private String VERIFYSTATUS;
    private String VERIFYID;
    private String CHNAME;
    private String THREEDSECURESTATUS = "";
    private String LIABILITY = "NA";
    private String CURRENCY;
    private String PURCHASECURRENCY;
    
    public OneCheckoutNotifyStatusRequest() {
        
        
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
    public void setAMOUNT(double AMOUNT) {
        this.AMOUNT = AMOUNT;
    }

    /**
     * @return the STATUSTYPE
     */
    public String getSTATUSTYPE() {
        return STATUSTYPE;
    }

    /**
     * @param STATUSTYPE the STATUSTYPE to set
     */
    public void setSTATUSTYPE(String STATUSTYPE) {
        this.STATUSTYPE = STATUSTYPE;
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
        this.TRANSIDMERCHANT = TRANSIDMERCHANT;
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
        this.APPROVALCODE = APPROVALCODE;
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
     * @return the SESSIONID
     */
    public String getSESSIONID() {
        return SESSIONID;
    }

    /**
     * @param SESSIONID the SESSIONID to set
     */
    public void setSESSIONID(String SESSIONID) {
        this.SESSIONID = SESSIONID;
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
     * @return the MCN
     */
    public String getMCN() {
        return MCN;
    }

    /**
     * @param MCN the MCN to set
     */
    public void setMCN(String MCN) {
        this.MCN = MCN;
    }

    /**
     * @return the PAYMENTDATETIME
     */
    public String getPAYMENTDATETIME() {
        return PAYMENTDATETIME;
    }

    /**
     * @param PAYMENTDATETIME the PAYMENTDATETIME to set
     */
    public void setPAYMENTDATETIME(String PAYMENTDATETIME) {
        this.PAYMENTDATETIME = PAYMENTDATETIME;
    }

    /**
     * @return the VERIFYSCORE
     */
    public String getVERIFYSCORE() {
        return VERIFYSCORE;
    }

    /**
     * @param VERIFYSCORE the VERIFYSCORE to set
     */
    public void setVERIFYSCORE(String VERIFYSCORE) {
        this.VERIFYSCORE = VERIFYSCORE;
    }

    /**
     * @return the VERIFYSTATUS
     */
    public String getVERIFYSTATUS() {
        return VERIFYSTATUS;
    }

    /**
     * @param VERIFYSTATUS the VERIFYSTATUS to set
     */
    public void setVERIFYSTATUS(String VERIFYSTATUS) {
        this.VERIFYSTATUS = VERIFYSTATUS;
    }
    
    

    public String toCheckStatusStringFailed() {
        StringBuilder sb=new StringBuilder();
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
            DecimalFormat df2=new DecimalFormat("##########.00");

            sb.append("<?xml version=\"1.0\"?>");
            //sb.append("<NOTIFY_STATUS>");
            sb.append("<PAYMENT_STATUS>");
            sb.append("<AMOUNT>").append(df2.format(getAMOUNT())).append("</AMOUNT>");
  //          sb.append("<STATUSTYPE>").append(getSTATUSTYPE()).append("</STATUSTYPE>");
            sb.append("<TRANSIDMERCHANT>").append(getTRANSIDMERCHANT() == null ? "" : getTRANSIDMERCHANT()).append("</TRANSIDMERCHANT>");
            sb.append("<WORDS>").append(getWORDS() == null ? "" : getWORDS()).append("</WORDS>");
            sb.append("<RESPONSECODE>").append(getRESPONSECODE() == null ? "" : getRESPONSECODE()).append("</RESPONSECODE>");
            sb.append("<APPROVALCODE>").append(getAPPROVALCODE() == null ? "" : getAPPROVALCODE()).append("</APPROVALCODE>");
            sb.append("<RESULTMSG>").append(getRESULTMSG() == null ? "" : getRESULTMSG().toUpperCase()).append("</RESULTMSG>");
            sb.append("<PAYMENTCHANNEL>").append(getPAYMENTCHANNEL() == null ? "" : getPAYMENTCHANNEL()).append("</PAYMENTCHANNEL>");
            sb.append("<PAYMENTCODE>").append(getPAYMENTCODE() == null ? "" : getPAYMENTCODE()).append("</PAYMENTCODE>");
            sb.append("<SESSIONID>").append(getSESSIONID() == null ? "" : getSESSIONID()).append("</SESSIONID>");
            sb.append("<BANK>").append(getBANK() == null ? "" : getBANK()).append("</BANK>");
            sb.append("<MCN>").append(getMCN() == null ? "" : getMCN()).append("</MCN>");
            sb.append("<PAYMENTDATETIME>").append(getPAYMENTDATETIME() == null ? "" : getPAYMENTDATETIME()).append("</PAYMENTDATETIME>");
            sb.append("<VERIFYID>").append(getVERIFYID() == null ? "" : getVERIFYID()).append("</VERIFYID>");
            sb.append("<VERIFYSCORE>").append(getVERIFYSCORE() == null ? "" : getVERIFYSCORE()).append("</VERIFYSCORE>");
            sb.append("<VERIFYSTATUS>").append(getVERIFYSTATUS() == null ? "" : getVERIFYSTATUS()).append("</VERIFYSTATUS>");
            sb.append("</PAYMENT_STATUS>");

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    public String toCheckStatusString(HashMap<String, String> data, Merchants m) {
        StringBuilder sb=new StringBuilder();
        try {
            
            OneCheckoutLogger.log("OneCheckoutNotifyStatusRequest.toCheckStatusString : Convert HashMap to XML String ");            
            sb.append("<?xml version=\"1.0\"?>");
            sb.append("<PAYMENT_STATUS>");
            for (String key : data.keySet()) {
                String value = (String) data.get(key);

                if (value==null)
                    value = "";
                
                sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
            }            
                       
            sb.append("</PAYMENT_STATUS>");
            OneCheckoutLogger.log("OneCheckoutNotifyStatusRequest.toCheckStatusString : XML =" + sb.toString());            
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

//    public String toCheckStatusString(HashMap<String, String> data, Merchants m) {
//        StringBuilder sb = new StringBuilder();
//        try {
//            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
//            DecimalFormat df2 = new DecimalFormat("##########.00");
//            sb.append("<?xml version=\"1.0\"?>");
//            sb.append("<PAYMENT_STATUS>");
//            Iterator i = data.keySet().iterator();
//            while (i.hasNext()) {
//                String key = (String) i.next();
//                String value = (String) data.get(key);
//                if (value == null) {
//                    value = "";
//                }
//                sb.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
//            }
//            sb.append("</PAYMENT_STATUS>");
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return null;
//        }
//        return sb.toString();
//    }
    
    public String toErrorCheckStatusString(String message, String responseCode) {
        StringBuilder sb=new StringBuilder();
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
            DecimalFormat df2=new DecimalFormat("##########.00");

            sb.append("<?xml version=\"1.0\"?>");
            //sb.append("<NOTIFY_STATUS>");
            sb.append("<PAYMENT_STATUS>");
            sb.append("<AMOUNT>").append("</AMOUNT>");
     //       sb.append("<STATUSTYPE>").append("</STATUSTYPE>");
            sb.append("<TRANSIDMERCHANT>").append("</TRANSIDMERCHANT>");
            sb.append("<WORDS>").append("</WORDS>");
            sb.append("<RESPONSECODE>").append(responseCode).append("</RESPONSECODE>");
            sb.append("<APPROVALCODE>").append("</APPROVALCODE>");
            sb.append("<RESULTMSG>").append(message).append("</RESULTMSG>");
            sb.append("<PAYMENTCHANNEL>").append("</PAYMENTCHANNEL>");
            sb.append("<PAYMENTCODE>").append("</PAYMENTCODE>");
            sb.append("<SESSIONID>").append("</SESSIONID>");
            sb.append("<BANK>").append("</BANK>");
            sb.append("<MCN>").append("</MCN>");
            sb.append("<PAYMENTDATETIME>").append("</PAYMENTDATETIME>");
            sb.append("<VERIFYID>").append("</VERIFYID>");
            sb.append("<VERIFYSCORE>").append("</VERIFYSCORE>");
            sb.append("<VERIFYSTATUS>").append("</VERIFYSTATUS>");
            sb.append("</PAYMENT_STATUS>");
            //sb.append("</NOTIFY_STATUS>");
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }

    
    /**
     * @return the VERIFYID
     */
    public String getVERIFYID() {
        return VERIFYID;
    }

    /**
     * @param VERIFYID the VERIFYID to set
     */
    public void setVERIFYID(String VERIFYID) {
        this.VERIFYID = VERIFYID;
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
     * @return the CHNAME
     */
    public String getCHNAME() {
        return CHNAME;
    }

    /**
     * @param CHNAME the CHNAME to set
     */
    public void setCHNAME(String CHNAME) {
        this.CHNAME = CHNAME;
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

    /**
     * @return the PURCHASECURRENCY
     */
    public String getPURCHASECURRENCY() {
        return PURCHASECURRENCY;
    }

    /**
     * @param PURCHASECURRENCY the PURCHASECURRENCY to set
     */
    public void setPURCHASECURRENCY(String PURCHASECURRENCY) {
        this.PURCHASECURRENCY = PURCHASECURRENCY;
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
     * @return the RATE
     */
    public Rates getRATE() {
        return RATE;
    }

    /**
     * @param RATE the RATE to set
     */
    public void setRATE(Rates RATE) {
        this.RATE = RATE;
    }

    /**
     * @return the PURCHASEAMOUNT
     */
    public double getPURCHASEAMOUNT() {
        return PURCHASEAMOUNT;
    }

    /**
     * @param PURCHASEAMOUNT the PURCHASEAMOUNT to set
     */
    public void setPURCHASEAMOUNT(double PURCHASEAMOUNT) {
        this.PURCHASEAMOUNT = PURCHASEAMOUNT;
    }
    
}
