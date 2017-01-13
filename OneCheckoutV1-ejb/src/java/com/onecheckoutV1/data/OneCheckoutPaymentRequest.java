/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import com.onecheckoutV1.ejb.exception.InvalidPaymentRequestException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutFlightType;
import com.onecheckoutV1.type.OneCheckoutReturnType;
import com.onecheckoutV1.type.OneCheckoutThirdPartyStatus;
import com.onechekoutv1.dto.Rates;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutPaymentRequest extends OneCheckoutBaseRules {

    private int MALLID;
    private int CHAINMERCHANT;
    private double AMOUNT;
    private double REGISTERAMOUNT;
    private double PURCHASEAMOUNT;
    private double VAT;
    private double INSURANCE;
    private double FUELSURCHARGE;
    private String TRANSIDMERCHANT;
    private String WORDS;
    private Date REQUESTDATETIME;
    private String CURRENCY;
    private String PURCHASECURRENCY;
    private String SESSIONID;
    private String EMAIL;
    private String BILLINGDESCRIPTION;
    //    private OneCheckoutPaymentChannel PAYMENTCHANNEL;
    // Payment Account Information
    private String CARDNUMBER;
    private String EXPIRYDATE;
    private String CVV2;
    private String INSTALLMENT_ACQUIRER;
    private String TENOR;
    private String PROMOID;
    private String CHALLENGE_CODE_1;
    private String CHALLENGE_CODE_2;
    private String CHALLENGE_CODE_3;
    private String RESPONSE_TOKEN;
    private String USERIDKLIKBCA;
    private String PAYCODE;
    private String CUSTIP;
    private String PAYMENTTYPE;

    //DOKUPAY
    private String DOKUPAYID;
    private String SECURITYCODE;
    private String DOKUWALLETCHANNEL;
    private String CASHWALLETPIN;
    private String DWCREDITCARD;
    private String DWCVV2;
    private String DWTCASHACCOUNTNUMBER;
    private String DWTCASHTOKENNUMBER;

    //Token
    private String CUSTOMERTYPE;
    private String CUSTOMERID;
    private String TOKENID;

    //RECUR Info
    private String BILLNUMBER;
    private String BILLDETAIL;
    private String BILLTYPE;
    private Date STARTDATE;
    private Date ENDDATE;
    private String EXECUTETYPE;
    private String EXECUTEDATE;
    private String EXECUTEMONTH;
    private boolean CASHNOW = false;
    private boolean FLATSTATUS = false;
    private boolean UPDATEBILLSTATUS = false;

    // CH Information
    private String CC_NAME;
    private String ADDRESS;
    private String CITY;
    private String STATE;
    private String COUNTRY;
    private String ZIPCODE;
    private String HOMEPHONE;
    private String MOBILEPHONE;
    private String WORKPHONE;
    private String BIRTHDATE;

    // Non Airlines Information
    private String BASKET;
    private String SHIPPING_ADDRESS;
    private String SHIPPING_CITY;
    private String SHIPPING_STATE;
    private String SHIPPING_COUNTRY;
    private String SHIPPING_ZIPCODE;
    // Airlines Information
    private String NAME;
    private OneCheckoutFlightType FLIGHT;
    private OneCheckoutReturnType FLIGHTTYPE;
    private String BOOKINGCODE;
    private String[] ROUTE;
    private String[] FLIGHTDATE;
    private String[] FLIGHTTIME;
    private Date FLIGHTDATETIME;
    private String[] FLIGHTNUMBER;
    private String[] PASSENGER_NAME;
    private String[] PASSENGER_TYPE;
    private String FFNUMBER;
    private OneCheckoutThirdPartyStatus THIRDPARTY_STATUS;
    private String ECI = "";
    private String XID = "";
    private String VERESSTATUS = "";
    private String AUTHRESRESPONSECODE = "";//AuthResResponseCode;
    private String AUTHRESSTATUS = "";//AuthResStatus;
    private String AUTHRESVENDORCODE = "";//AuthResVendorCode;
    private String CAVVALGORITHM = "";//cavvAlgorithm;
    private String CAVV = "";//cavv;
    private HashMap<String, String> additionData;
    // private HashMap<String, String> cookies;
    private String cookie;
    private String message = "";
    private String ADDITIONALINFO;

    //KlikPayBCA
    private String INVOICENUMBER = "";
    private String DESCRIPTION = "";
    private String ACQUIRERID = "";

    //DEVICEID RED
    private String DEVICEID = "";

    private Rates RATE;
    private double CONVERTEDAMOUNT;

    //Off Us Installment
    private String INSTALLMENTCODE = "";
    private String INSTALLMENTSTATUS = "";
    private String TRXCODE = "";

    private String LOGINID = "";
    private String PROMOTIONID = "";
    private String FLAGDOKU = "";
    private String AMOUNTAFTERPROMOTION = "";

    private String MSISDN = "";
    private String PINDOMPETKU = "";
    private String INITIATORDOMPETKU = "";
    private String SIGNATURE = "";
    private String USERID = "";

    private String CUSTOMERNAME = "";
    private String PAYREASON = "";
    private String USERNAME = "";
    private String REDEMPTIONSTATUS = "";

    //kredivo
    private String paymentTypeKredivo = "";

    private Boolean VAPartialAmount = false;
    private Boolean VAOpenAmount = false;
    private String VAinitNumberFormat;

    public String getREDEMPTIONSTATUS() {
        return REDEMPTIONSTATUS;
    }

    public void setREDEMPTIONSTATUS(String REDEMPTIONSTATUS) {
        try {
            String rewardsStatus = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, OneCheckoutVerifyFormatData.ONE, "REDEMPTIONSTATUS", REDEMPTIONSTATUS, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            if (rewardsStatus.equals("1")) {
                this.REDEMPTIONSTATUS = "TRUE";
            } else {
                this.REDEMPTIONSTATUS = "FALSE";
            }
        } catch (OneCheckoutInvalidInputException ociie) {
            throw new InvalidPaymentRequestException("10100|" + ociie.getMessage(), this, ociie);
        }
    }

    public String getUSERNAME() {
        return USERNAME;
    }

    public void setUSERNAME(String USERNAME) {
        this.USERNAME = USERNAME;
    }

    public String getPAYREASON() {
        return PAYREASON;
    }

    public void setPAYREASON(String PAYREASON) {
        this.PAYREASON = PAYREASON;
    }

    public String getCUSTOMERNAME() {
        return CUSTOMERNAME;
    }

    public void setCUSTOMERNAME(String CUSTOMERNAME) {
        this.CUSTOMERNAME = CUSTOMERNAME;
    }

    public String getUSERID() {
        return USERID;
    }

    public void setUSERID(String USERID) {
        this.USERID = USERID;
    }

    public String getPINDOMPETKU() {
        return PINDOMPETKU;
    }

    public void setPINDOMPETKU(String PINDOMPETKU) {
        this.PINDOMPETKU = PINDOMPETKU;
    }

    public String getINITIATORDOMPETKU() {
        return INITIATORDOMPETKU;
    }

    public void setINITIATORDOMPETKU(String INITIATORDOMPETKU) {
        this.INITIATORDOMPETKU = INITIATORDOMPETKU;
    }

    public String getSIGNATURE() {
        return SIGNATURE;
    }

    public void setSIGNATURE(String SIGNATURE) {
        this.SIGNATURE = SIGNATURE;
    }

    public String getMSISDN() {
        return MSISDN;
    }

    public void setMSISDN(String MSISDN) {
        this.MSISDN = MSISDN;
    }

    public String getAMOUNTAFTERPROMOTION() {
        return AMOUNTAFTERPROMOTION;
    }

    public void setAMOUNTAFTERPROMOTION(String AMOUNTAFTERPROMOTION) {
        this.AMOUNTAFTERPROMOTION = AMOUNTAFTERPROMOTION;
    }

    public String getFLAGDOKU() {
        return FLAGDOKU;
    }

    public void setFLAGDOKU(String FLAGDOKU) {
        this.FLAGDOKU = FLAGDOKU;
    }

    public String getPROMOTIONID() {
        return PROMOTIONID;
    }

    public void setPROMOTIONID(String PROMOTIONID) {
        this.PROMOTIONID = PROMOTIONID;
    }

    public String getLOGINID() {
        return LOGINID;
    }

    public void setLOGINID(String LOGINID) {
        this.LOGINID = LOGINID;
    }

    public String getINSTALLMENTSTATUS() {
        return INSTALLMENTSTATUS;
    }

    public void setINSTALLMENTSTATUS(String INSTALLMENTSTATUS) {

        try {
            String installStatus = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, OneCheckoutVerifyFormatData.ONE, "INSTALLMENTSTATUS", INSTALLMENTSTATUS, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            if (installStatus.equals("1")) {
                this.INSTALLMENTSTATUS = "TRUE";
            } else {
                this.INSTALLMENTSTATUS = "FALSE";
            }
        } catch (OneCheckoutInvalidInputException ociie) {
            throw new InvalidPaymentRequestException("10063|" + ociie.getMessage(), this, ociie);
        }
    }

    public String getTRXCODE() {
        return TRXCODE;
    }

    public void setTRXCODE(String TRXCODE) {
        this.TRXCODE = TRXCODE;
    }

    public String getINSTALLMENTCODE() {
        return INSTALLMENTCODE;
    }

    public void setINSTALLMENTCODE(String INSTALLMENTCODE) {
        this.INSTALLMENTCODE = INSTALLMENTCODE;
    }

    public String getVERESSTATUS() {
        return VERESSTATUS;
    }

    public void setVERESSTATUS(String VERESSTATUS) {
        this.VERESSTATUS = VERESSTATUS;
    }

    public String getPAYMENTTYPE() {
        return PAYMENTTYPE;
    }

    public void setPAYMENTTYPE(String PAYMENTTYPE) {
        this.PAYMENTTYPE = PAYMENTTYPE;
    }

    public String getBILLINGDESCRIPTION() {
        return BILLINGDESCRIPTION;
    }

    public void setBILLINGDESCRIPTION(String BILLINGDESCRIPTION) {
        this.BILLINGDESCRIPTION = BILLINGDESCRIPTION;
    }

    public OneCheckoutPaymentRequest() {
    }

    public String getCASHWALLETPIN() {
        return CASHWALLETPIN;
    }

    public void setCASHWALLETPIN(String CASHWALLETPIN) {
        this.CASHWALLETPIN = CASHWALLETPIN;
    }

    public String getDOKUWALLETCHANNEL() {
        return DOKUWALLETCHANNEL;
    }

    public void setDOKUWALLETCHANNEL(String DOKUWALLETCHANNEL) {
        this.DOKUWALLETCHANNEL = DOKUWALLETCHANNEL;
    }

    public String getDWCREDITCARD() {
        return DWCREDITCARD;
    }

    public void setDWCREDITCARD(String DWCREDITCARD) {
        this.DWCREDITCARD = DWCREDITCARD;
    }

    public String getDWCVV2() {
        return DWCVV2;
    }

    public void setDWCVV2(String DWCVV2) {
        this.DWCVV2 = DWCVV2;
    }

    public String getDWTCASHACCOUNTNUMBER() {
        return DWTCASHACCOUNTNUMBER;
    }

    public void setDWTCASHACCOUNTNUMBER(String DWTCASHACCOUNTNUMBER) {
        this.DWTCASHACCOUNTNUMBER = DWTCASHACCOUNTNUMBER;
    }

    public String getDWTCASHTOKENNUMBER() {
        return DWTCASHTOKENNUMBER;
    }

    public void setDWTCASHTOKENNUMBER(String DWTCASHTOKENNUMBER) {
        this.DWTCASHTOKENNUMBER = DWTCASHTOKENNUMBER;
    }

    public boolean isUPDATEBILLSTATUS() {
        return UPDATEBILLSTATUS;
    }

    public void setUPDATEBILLSTATUS(boolean UPDATEBILLSTATUS) {
        this.UPDATEBILLSTATUS = UPDATEBILLSTATUS;
    }

    public boolean isCASHNOW() {
        return CASHNOW;
    }

    public void setCASHNOW(boolean CASHNOW) {
        this.CASHNOW = CASHNOW;
    }

    public boolean isFLATSTATUS() {
        return FLATSTATUS;
    }

    public void setFLATSTATUS(boolean FLATSTATUS) {
        this.FLATSTATUS = FLATSTATUS;
    }

    public OneCheckoutPaymentRequest(String mallId) {

        try {

            //  this.CHAINMERCHANT = 0;
            setMALLID(mallId);
            //this.PAYMENTCHANNEL = channel;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException(iv.getMessage(), this, iv);
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
    private void setMALLID(String mallId) {

        try {

            this.MALLID = super.validateMALLID1(mallId);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10001|" + iv.getMessage(), this, iv);
        }

    }

    public void setMALLID(int MALLID) {
        this.MALLID = MALLID;
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
    public void setCHAINMERCHANT(String CHAINMERCHANT) {
        //     this.CHAINMERCHANT = CHAINMERCHANT;
        try {

            this.CHAINMERCHANT = super.validateCHAINMERCHANT1(CHAINMERCHANT);

        } catch (OneCheckoutInvalidInputException iv) {
            this.CHAINMERCHANT = 0;
            //  throw new InvalidPaymentRequestException(iv.getMessage(), this, iv);
        }

    }

    public void setCHAINMERCHANT(int CHAINMERCHANT) {
        this.CHAINMERCHANT = CHAINMERCHANT;
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

            if (amount != null && amount.contains(",")) {
                throw new OneCheckoutInvalidInputException(String.format("Invalid AMOUNT Value because contains [,] value"));
            } /* else if (amount != null && !amount.contains(".00")) {
             throw new OneCheckoutInvalidInputException(String.format("Invalid AMOUNT Value because is not contains [.00] value"));
             } else */ {
                this.AMOUNT = super.validateAmount(amount, "AMOUNT");
            }

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10002|" + iv.getMessage(), this, iv);
        }
    }

    public void setAMOUNT(double amount) {
        this.AMOUNT = amount;
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
    public void setPURCHASEAMOUNT(String purchaseAmount) {

        try {

            if (purchaseAmount != null && purchaseAmount.contains(",")) {
                throw new OneCheckoutInvalidInputException(String.format("Invalid AMOUNT Value because contains [,] value"));
            } /*else if (purchaseAmount != null && !purchaseAmount.contains(".00")) {
             throw new OneCheckoutInvalidInputException(String.format("Invalid AMOUNT Value because is not contains [.00] value"));
             }*/

            this.PURCHASEAMOUNT = super.validateAmount(purchaseAmount, "PURCHASEAMOUNT");

            String zero = "0.00";
            double zeron = Double.parseDouble(zero);
            if (this.PURCHASEAMOUNT <= zeron) {
                throw new InvalidPaymentRequestException("10003|" + "PURCHASEAMOUNT must larger than 0.00", this);
            }

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10003|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the VAT
     */
    public double getVAT() {
        return VAT;
    }

    /**
     * @param VAT the VAT to set
     */
    public void setVAT(String vat) {

        try {
            this.VAT = super.validateAmount(vat, "VAT");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10004|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the INSURANCE
     */
    public double getINSURANCE() {
        return INSURANCE;
    }

    /**
     * @param INSURANCE the INSURANCE to set
     */
    public void setINSURANCE(String insurance) {

        try {
            this.INSURANCE = super.validateAmount(insurance, "INSURANCE");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10005|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the FUELSURCHARGE
     */
    public double getFUELSURCHARGE() {
        return FUELSURCHARGE;
    }

    /**
     * @param FUELSURCHARGE the FUELSURCHARGE to set
     */
    public void setFUELSURCHARGE(String fuelSurcharge) {

        try {
            this.FUELSURCHARGE = super.validateAmount(fuelSurcharge, "FUELSURCHARGE");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10006|" + iv.getMessage(), this, iv);
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
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10007|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the REQUESTDATETIME
     */
    public Date getREQUESTDATETIME() {
        return REQUESTDATETIME;
    }

    /**
     * @param REQUESTDATETIME the REQUESTDATETIME to set
     */
    public void setREQUESTDATETIME(String requestDatetime) {
        try {
            //   this.REQUESTDATETIME = super.validateDATETIME(requestDatetime,"REQUESTDATETIME");

            String procdtime = OneCheckoutVerifyFormatData.validateString(14, 14, "REQUESTDATETIME", requestDatetime, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

            this.REQUESTDATETIME = df.parse(procdtime);

            OneCheckoutLogger.log("REQUESTDATETIME valid format");
        } catch (ParseException iv) {
            message = iv.getMessage();
            OneCheckoutLogger.log("REQUESTDATETIME invalid format");
            iv.printStackTrace();
            throw new InvalidPaymentRequestException("10008|" + iv.getMessage(), this, iv);
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
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10009|" + iv.getMessage(), this, iv);
        }

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
    public void setPURCHASECURRENCY(String purchaseCurrency) {

        try {

            PURCHASECURRENCY = OneCheckoutVerifyFormatData.validateString(3, 3, "PURCHASECURRENCY", purchaseCurrency, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10010|" + iv.getMessage(), this, iv);
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
        try {

            this.SESSIONID = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 48, "SESSIONID", sessionId, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10011|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the EMAIL
     */
    public String getEMAIL() {
        return EMAIL;
    }

    /**
     * @param EMAIL the EMAIL to set
     */
    public void setEMAIL(String email) {
        String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
        if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
            if (email == null || email.trim().equals("")) {
                email = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.EMAIL");
            }
        }

        boolean valid = super.validateEmail(email);
        if (valid) {
            this.EMAIL = email;
        } else {
            message = "INVALID EMAIL ADDRESS";
            throw new InvalidPaymentRequestException("10012|" + "INVALID EMAIL ADDRESS");
        }

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
    public void setCARDNUMBER(String cardNumber) {
        try {
            if (cardNumber != null && cardNumber.trim().length() > 10) {
                String firstDigit = cardNumber.substring(0, 1);
                if (firstDigit.equals("3")) {
                    this.CARDNUMBER = super.validateAmexCardNumber(cardNumber, "CARDNUMBER");
                } else if (firstDigit.equals("1")) {
                    this.CARDNUMBER = super.validateUatpCardNumber(cardNumber, "CARDNUMBER");
                } else {
                    this.CARDNUMBER = super.validateCardNumber(cardNumber, "CARDNUMBER");
                }
            }
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10013|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the EXPIRYDATE
     */
    public String getEXPIRYDATE() {
        return EXPIRYDATE;
    }

    /**
     * @param EXPIRYDATE the EXPIRYDATE to set
     */
    public void setEXPIRYDATE(String expiryDate) {
        try {

            String expdate = OneCheckoutVerifyFormatData.validateString(4, 4, "EXPDATE", expiryDate, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            String expiryYear = expdate.substring(0, 2);
            String expiryMonth = expdate.substring(2);

            String mm = super.setExpiryMonth(expiryMonth);
            String yy = super.setExpiryYear(expiryYear);

//            OneCheckoutLogger.log("::MMYY" + mm + yy);
            boolean ok = super.validExpNumber(mm, yy);

            if (!ok) {
                message = "Expired Date is not valid";
                throw new InvalidPaymentRequestException("Expired Date is not valid");
            }

            this.EXPIRYDATE = yy + mm;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10014|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the CVV2
     */
    public String getCVV2() {
        return CVV2;
    }

    /**
     * @param CVV2 the CVV2 to set
     */
    public void setCVV2(String cvv2) {

        try {
            this.CVV2 = super.setCvv2(cvv2);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10015|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @param CVV2 the CVV2 to set
     */
    public void setCVV2withEmpty() {

        try {
            this.CVV2 = "";

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10015|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the INSTALLMENT_ACQUIRER
     */
    public String getINSTALLMENT_ACQUIRER() {
        return INSTALLMENT_ACQUIRER;
    }

    /**
     * @param INSTALLMENT_ACQUIRER the INSTALLMENT_ACQUIRER to set
     */
    public void setINSTALLMENT_ACQUIRER(String installmentAcquirer) {
        try {
            this.INSTALLMENT_ACQUIRER = OneCheckoutVerifyFormatData.validateString(3, 3, "INSTALLMENT_ACQUIRER", installmentAcquirer, OneCheckoutVerifyFormatData.NUMERIC_VALUE, "");
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10016|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the TENOR
     */
    public String getTENOR() {
        return TENOR;
    }

    /**
     * @param TENOR the TENOR to set
     */
    public void setTENOR(String tenor) {
        try {
//            this.TENOR = OneCheckoutVerifyFormatData.validateString(2, 2, "TENOR", tenor, OneCheckoutVerifyFormatData.NUMERIC_VALUE, "");
            this.TENOR = OneCheckoutVerifyFormatData.validateString(1, 2, "TENOR", tenor, OneCheckoutVerifyFormatData.NUMERIC_VALUE, "");
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10017|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the PROMOID
     */
    public String getPROMOID() {
        return PROMOID;
    }

    /**
     * @param PROMOID the PROMOID to set
     */
    public void setPROMOID(String promoId) {

        try {
            this.PROMOID = OneCheckoutVerifyFormatData.validateString(3, 3, "PROMOID", promoId, OneCheckoutVerifyFormatData.NUMERIC_VALUE, "");
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10018|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the CHALLENGE_CODE_1
     */
    public String getCHALLENGE_CODE_1() {
        return CHALLENGE_CODE_1;
    }

    /**
     * @param CHALLENGE_CODE_1 the CHALLENGE_CODE_1 to set
     */
    public void setCHALLENGE_CODE_1(String challengeCode1) {

        try {
            this.CHALLENGE_CODE_1 = OneCheckoutVerifyFormatData.validateString(10, 10, "CHALLENGE_CODE_1", challengeCode1, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10019|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the CHALLENGE_CODE_2
     */
    public String getCHALLENGE_CODE_2() {
        return CHALLENGE_CODE_2;
    }

    /**
     * @param CHALLENGE_CODE_2 the CHALLENGE_CODE_2 to set
     */
    public void setCHALLENGE_CODE_2(String challengeCode2) {

        try {
            this.CHALLENGE_CODE_2 = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 10, "CHALLENGE_CODE_2", challengeCode2, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10020|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the CHALLENGE_CODE_3
     */
    public String getCHALLENGE_CODE_3() {
        return CHALLENGE_CODE_3;
    }

    /**
     * @param CHALLENGE_CODE_3 the CHALLENGE_CODE_3 to set
     */
    public void setCHALLENGE_CODE_3(String challengeCode3) {

        try {
            this.CHALLENGE_CODE_3 = OneCheckoutVerifyFormatData.validateString(8, 9, "CHALLENGE_CODE_3", challengeCode3, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10021|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the RESPONSE_TOKEN
     */
    public String getRESPONSE_TOKEN() {
        return RESPONSE_TOKEN;
    }

    /**
     * @param RESPONSE_TOKEN the RESPONSE_TOKEN to set
     */
    public void setRESPONSE_TOKEN(String responseToken) {
        try {
            this.RESPONSE_TOKEN = OneCheckoutVerifyFormatData.validateString(6, 6, "RESPONSE_TOKEN", responseToken, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10022|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the USERIDKLIKBCA
     */
    public String getUSERIDKLIKBCA() {
        return USERIDKLIKBCA;
    }

    /**
     * @param USERIDKLIKBCA the USERIDKLIKBCA to set
     */
    public void setUSERIDKLIKBCA(String userIDKlikBCA) {

        try {
            this.USERIDKLIKBCA = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 12, "USERIDKLIKBCA", userIDKlikBCA, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10023|" + iv.getMessage(), this, iv);
        }
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
    public void setDOKUPAYID(String dokupayID) {
        try {
            //this.DOKUPAYID = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 10, "DOKUPAYID", dokupayID, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            this.DOKUPAYID = dokupayID;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10024|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the CC_NAME
     */
    public String getCC_NAME() {
        return CC_NAME;
    }

    /**
     * @param CC_NAME the CC_NAME to set
     */
    public void setCC_NAME(String name) {
        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (name == null || name.trim().equals("")) {
                    name = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.CC_NAME");
                }
            }
            CC_NAME = super.validateName(name, "CC_NAME");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10025|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the ADDRESS
     */
    public String getADDRESS() {
        return ADDRESS;
    }

    /**
     * @param ADDRESS the ADDRESS to set
     */
    public void setADDRESS(String address) {

        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (address == null || address.trim().equals("")) {
                    address = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.ADDRESS");
                }
            }
            address = address.replaceAll("(\\r|\\n)", "");
            this.ADDRESS = super.setAddress(address);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10026|" + iv.getMessage(), this, iv);
        }
    }

    public void setEmptyADDRESS() {
        this.ADDRESS = "";

    }

    /**
     * @return the CITY
     */
    public String getCITY() {
        return CITY;
    }

    /**
     * @param CITY the CITY to set
     */
    public void setCITY(String city) {
        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (city == null || city.trim().equals("")) {
                    city = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.CITY");
                }
            }
            this.CITY = super.validateCity(city);
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10027|" + iv.getMessage(), this, iv);
        }

    }

    public void setEmptyCITY() {
        this.CITY = "";

    }

    /**
     * @return the STATE
     */
    public String getSTATE() {
        return STATE;
    }

    /**
     * @param STATE the STATE to set
     */
    public void setSTATE(String state) {
        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (state == null || state.trim().equals("")) {
                    state = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.STATE");
                }
            }
            this.STATE = super.validateState(state);
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10028|" + iv.getMessage(), this, iv);
        }
    }

    public void setEmptySTATE() {
        this.STATE = "";

    }

    /**
     * @return the COUNTRY
     */
    public String getCOUNTRY() {
        return COUNTRY;
    }

    /**
     * @param COUNTRY the COUNTRY to set
     */
    public void setCOUNTRY(String country) {
        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (country == null || country.trim().equals("")) {
                    country = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.COUNTRY");
                }
            }
            this.COUNTRY = super.validateCountry(country);
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10029|" + iv.getMessage(), this, iv);
        }

    }

    public void setEmptyCOUNTRY() {
        this.COUNTRY = "";

    }

    /**
     * @return the ZIPCODE
     */
    public String getZIPCODE() {
        return ZIPCODE;
    }

    /**
     * @param ZIPCODE the ZIPCODE to set
     */
    public void setZIPCODE(String zipCode) {
        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (zipCode == null || zipCode.trim().equals("")) {
                    zipCode = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.ZIPCODE");
                }
            }
            this.ZIPCODE = super.validateZipCode(zipCode);
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10030|" + iv.getMessage(), this, iv);
        }

    }

    public void setEmptyZIPCODE() {
        this.ZIPCODE = "";

    }

    /**
     * @return the HOMEPHONE
     */
    public String getHOMEPHONE() {
        return HOMEPHONE;
    }

    /**
     * @param HOMEPHONE the HOMEPHONE to set
     */
    public void setHOMEPHONE(String homePhone) {
        String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
        if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
            if (homePhone == null || homePhone.trim().equals("")) {
                homePhone = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.HOMEPHONE");
            }
        }
        this.HOMEPHONE = homePhone;
        /*try {
         this.HOMEPHONE = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 20, "HOMEPHONE", homePhone, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);

         } catch (OneCheckoutInvalidInputException iv) {
         throw new InvalidPaymentRequestException(iv.getMessage(), this, iv);
         }*/
    }

    /**
     * @return the MOBILEPHONE
     */
    public String getMOBILEPHONE() {
        return MOBILEPHONE;
    }

    /**
     * @param MOBILEPHONE the MOBILEPHONE to set
     */
    public void setMOBILEPHONE(String mobilePhone) {
        // this.MOBILEPHONE = mobilePhone;

        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (mobilePhone == null || mobilePhone.trim().equals("")) {
                    mobilePhone = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.MOBILEPHONE");
                }
            }
            this.MOBILEPHONE = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 15, "MOBILEPHONE", mobilePhone, OneCheckoutVerifyFormatData.NUMERIC_SYMBOL_VALUE);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10031|" + iv.getMessage(), this, iv);
        }

    }

    public void setEmptyMOBILEPHONE() {
        this.MOBILEPHONE = "";

    }

    /**
     * @return the WORKPHONE
     */
    public String getWORKPHONE() {
        return WORKPHONE;
    }

    /**
     * @param WORKPHONE the WORKPHONE to set
     */
    public void setWORKPHONE(String workPhone) {

        String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
        if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
            if (workPhone == null || workPhone.trim().equals("")) {
                workPhone = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.WORKPHONE");
            }
        }
        //this.WORKPHONE = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 20, "WORKPHONE", workPhone, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM,"");
        this.WORKPHONE = workPhone;
    }

    /**
     * @return the BIRTHDATE
     */
    public String getBIRTHDATE() {
        return BIRTHDATE;
    }

    /**
     * @param BIRTHDATE the BIRTHDATE to set
     */
    public void setBIRTHDATE(String birthDate) {

        /*String bDate = OneCheckoutVerifyFormatData.validateString(8, 8, "BIRTHDATE", birthDate, OneCheckoutVerifyFormatData.NUMERIC_VALUE,"");

         if (!bDate.isEmpty()) {
         DateFormat df = new SimpleDateFormat("yyyyMMdd");
         try {
         Date odate = df.parse(bDate);
         } catch (ParseException ex) {
         bDate = "";
         message = ex.getMessage();
         Logger.getLogger(OneCheckoutPaymentRequest.class.getName()).log(Level.SEVERE, null, ex);
         }
         }    
         this.BIRTHDATE = bDate;*/
        String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
        if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
            if (birthDate == null || birthDate.trim().equals("")) {
                birthDate = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.BIRTHDATE");
            }
        }
        this.BIRTHDATE = birthDate;
    }

    /**
     * @return the BASKET
     */
    public String getBASKET() {
        return BASKET;
    }

    /**
     * @param BASKET the BASKET to set
     */
    public void setBASKET(String basket, double amount, String invoiceNo) {
        try {
            this.BASKET = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 1024, "BASKET", basket, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);

            List<OneCheckoutBasket> basketList = OneCheckoutVerifyFormatData.GetValidBasket(basket, amount);

        } catch (OneCheckoutInvalidInputException iv) {

            StringBuilder basketStr = new StringBuilder();
            basketStr.append(invoiceNo);//getMerchant().getMerchantName());
            String amountStr = OneCheckoutVerifyFormatData.sdf.format(amount);
            basketStr.append(",").append(amountStr).append(",").append("1").append(",").append(amountStr);

            this.BASKET = basketStr.toString();

//            message = iv.getMessage();
//            throw new InvalidPaymentRequestException("10032|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the SHIPPING_ADDRESS
     */
    public String getSHIPPING_ADDRESS() {
        return SHIPPING_ADDRESS;
    }

    /**
     * @param SHIPPING_ADDRESS the SHIPPING_ADDRESS to set
     */
    public void setSHIPPING_ADDRESS(String SHIPPING_ADDRESS) {
        this.SHIPPING_ADDRESS = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 100, "SHIPPING_ADDRESS", SHIPPING_ADDRESS, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");
    }

    /**
     * @return the SHIPPING_CITY
     */
    public String getSHIPPING_CITY() {
        return SHIPPING_CITY;
    }

    /**
     * @param SHIPPING_CITY the SHIPPING_CITY to set
     */
    public void setSHIPPING_CITY(String SHIPPING_CITY) {
//        this.SHIPPING_CITY = SHIPPING_CITY;
        this.SHIPPING_CITY = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 100, "SHIPPING_CITY", SHIPPING_CITY, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");

    }

    /**
     * @return the SHIPPING_STATE
     */
    public String getSHIPPING_STATE() {
        return SHIPPING_STATE;
    }

    /**
     * @param SHIPPING_STATE the SHIPPING_STATE to set
     */
    public void setSHIPPING_STATE(String SHIPPING_STATE) {

        this.SHIPPING_STATE = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 100, "SHIPPING_STATE", SHIPPING_STATE, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");

    }

    /**
     * @return the SHIPPING_COUNTRY
     */
    public String getSHIPPING_COUNTRY() {
        return SHIPPING_COUNTRY;
    }

    /**
     * @param SHIPPING_COUNTRY the SHIPPING_COUNTRY to set
     */
    public void setSHIPPING_COUNTRY(String shippingCountry) {

        try {

            this.SHIPPING_COUNTRY = super.validateCountry(shippingCountry);
        } catch (OneCheckoutInvalidInputException iv) {
            this.SHIPPING_COUNTRY = "";
            // throw new InvalidPaymentRequestException(iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the SHIPPING_ZIPCODE
     */
    public String getSHIPPING_ZIPCODE() {
        return SHIPPING_ZIPCODE;
    }

    /**
     * @param SHIPPING_ZIPCODE the SHIPPING_ZIPCODE to set
     */
    public void setSHIPPING_ZIPCODE(String shippingZipCode) {

        try {
            this.SHIPPING_ZIPCODE = super.validateZipCode(shippingZipCode);
        } catch (OneCheckoutInvalidInputException iv) {

            this.SHIPPING_ZIPCODE = "";
            //throw new InvalidPaymentRequestException(iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the NAME
     */
    public String getNAME() {
        return NAME;
    }

    /**
     * @param NAME the NAME to set
     */
    public void setNAME(String name) {

        try {
            String listMallId = OneCheckoutProperties.getOneCheckoutConfig().getString("USE.DEFAULT.PARAM.MALLID");
            if (listMallId != null && listMallId.trim().contains(getMALLID() + "")) {
                if (name == null || name.trim().equals("")) {
                    name = OneCheckoutProperties.getOneCheckoutConfig().getString("PARAM.DEFAULT.NAME");
                }
            }
            this.NAME = super.validateName(name, "NAME");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10033|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the FLIGHT
     */
    public OneCheckoutFlightType getFLIGHT() {
        return FLIGHT;
    }

    /**
     * @param FLIGHT the FLIGHT to set
     */
    public void setFLIGHT(String flight) {
        //this.FLIGHT = FLIGHT;

        try {
            String flightId = OneCheckoutVerifyFormatData.validateString(2, 2, "FLIGHT", flight, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

            this.FLIGHT = OneCheckoutFlightType.findType(flightId);
            if (FLIGHT == null) {
                message = "INVALID FLIGHT TYPE";
                throw new InvalidPaymentRequestException("INVALID FLIGHT TYPE");
            }

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10034|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the FLIGHTTYPE
     */
    public OneCheckoutReturnType getFLIGHTTYPE() {
        return FLIGHTTYPE;
    }

    /**
     * @param FLIGHTTYPE the FLIGHTTYPE to set
     */
    public void setFLIGHTTYPE(String returni) {

        try {
            String returnId = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, OneCheckoutVerifyFormatData.ONE, "FLIGHTTYPE", returni, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

            char returnID = returnId.charAt(0);
            this.FLIGHTTYPE = OneCheckoutReturnType.findType(returnID);
            if (FLIGHTTYPE == null) {
                message = "INVALID RETURN TYPE";
                throw new InvalidPaymentRequestException("INVALID RETURN TYPE");
            }

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10035|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the BOOKINGCODE
     */
    public String getBOOKINGCODE() {
        return BOOKINGCODE;
    }

    /**
     * @param BOOKINGCODE the BOOKINGCODE to set
     */
    public void setBOOKINGCODE(String bookingCode) {

        try {
            this.BOOKINGCODE = super.setBookingCode(bookingCode);
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10036|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the ROUTE
     */
    public String[] getROUTE() {
        return ROUTE;
    }

    /**
     * @param ROUTE the ROUTE to set
     */
    public void setROUTE(String[] route) {

        try {

            int count = route.length;
            for (int i = 0; i < count; i++) {

                String temp = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 50, "ROUTE", route[i], OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);

            }

            this.ROUTE = route;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10037|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the FLIGHTDATE
     */
    public String[] getFLIGHTDATE() {
        return FLIGHTDATE;
    }

    /**
     * @param FLIGHTDATE the FLIGHTDATE to set
     */
    public void setFLIGHTDATE(String[] flightDate) {
        // this.FLIGHTDATE = FLIGHTDATE;
        try {
            //   this.FLIGHTDATE =

            int count = flightDate.length;
            for (int i = 0; i < count; i++) {

                String temp = OneCheckoutVerifyFormatData.validateString(8, 8, "FLIGHTDATE", flightDate[i], OneCheckoutVerifyFormatData.NUMERIC_VALUE);

            }

            this.FLIGHTDATE = flightDate;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10038|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the FLIGHTTIME
     */
    public String[] getFLIGHTTIME() {
        return FLIGHTTIME;
    }

    /**
     * @param FLIGHTTIME the FLIGHTTIME to set
     */
    public void setFLIGHTTIME(String[] flightTime) {

        try {

            int count = flightTime.length;
            for (int i = 0; i < count; i++) {

                String temp = OneCheckoutVerifyFormatData.validateString(6, 6, "FLIGHTTIME", flightTime[i], OneCheckoutVerifyFormatData.NUMERIC_VALUE);

            }

            this.FLIGHTTIME = flightTime;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10039|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the FLIGHTDATETIME
     */
    public Date getFLIGHTDATETIME() {
        return FLIGHTDATETIME;
    }

    /**
     * @param FLIGHTDATETIME the FLIGHTDATETIME to set
     */
    public void setFLIGHTDATETIME() {
        try {

            Date flightDate = null;
            try {
                flightDate = OneCheckoutVerifyFormatData.datetimeFormat.parse(FLIGHTDATE[0] + FLIGHTTIME[0]);
            } catch (ParseException ex) {
                // Logger.getLogger(OneCheckoutPaymentRequest.class.getName()).log(Level.SEVERE, null, ex);
                message = ex.getMessage();
                throw new InvalidPaymentRequestException(ex.getMessage(), this, ex);
            }
            this.FLIGHTDATETIME = flightDate;
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10040|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the FLIGHTNUMBER
     */
    public String[] getFLIGHTNUMBER() {
        return FLIGHTNUMBER;
    }

    /**
     * @param FLIGHTNUMBER the FLIGHTNUMBER to set
     */
    public void setFLIGHTNUMBER(String[] flightNumber) {
        try {

            int count = flightNumber.length;
            for (int i = 0; i < count; i++) {

                String temp = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 30, "FLIGHTNUMBER", flightNumber[i], OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            }

            this.FLIGHTNUMBER = flightNumber;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10041|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the PASSENGER_NAME
     */
    public String[] getPASSENGER_NAME() {
        return PASSENGER_NAME;
    }

    /**
     * @param PASSENGER_NAME the PASSENGER_NAME to set
     */
    public void setPASSENGER_NAME(String[] passengerName) {

        try {
            int count = passengerName.length;
            for (int i = 0; i < count; i++) {

                String temp = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 50, "PASSENGER_NAME", passengerName[i], OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            }

            this.PASSENGER_NAME = passengerName;
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10042|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the PASSENGER_TYPE
     */
    public String[] getPASSENGER_TYPE() {
        return PASSENGER_TYPE;
    }

    /**
     * @param PASSENGER_TYPE the PASSENGER_TYPE to set
     */
    public void setPASSENGER_TYPE(String[] passengerType) {

        try {
            int count = passengerType.length;
            for (int i = 0; i < count; i++) {

                String temp = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 1, "PASSENGER_TYPE", passengerType[i], OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            }

            this.PASSENGER_TYPE = passengerType;

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10043|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the THIRDPARTY_STATUS
     */
    public OneCheckoutThirdPartyStatus getTHIRDPARTY_STATUS() {
        return THIRDPARTY_STATUS;
    }

    /**
     * @param THIRDPARTY_STATUS the THIRDPARTY_STATUS to set
     */
    public void setTHIRDPARTY_STATUS(char THIRDPARTY_STATUS) {

        this.THIRDPARTY_STATUS = OneCheckoutThirdPartyStatus.findType(THIRDPARTY_STATUS);
    }

    public HashMap<String, String> getAllAdditionData() {
        return additionData;
    }

    /**
     * @param additionData the additionData to set
     */
    public void setAllAdditionData(HashMap<String, String> additionData) {
        this.additionData = additionData;
    }

    public void addAdditionaData(String key, String value) {
        if (this.additionData == null) {
            additionData = new HashMap<String, String>();
        }
        additionData.put(key, value);
    }

    public String getAdditionalData(String key) {
        return additionData.get(key);
    }

    public String parseArray(String[] array) {

        StringBuilder result = new StringBuilder();

        int len = array.length;
        for (int k = 0; k < len; k++) {

            if (k == 0) {
                result.append(array[k]);
            } else {
                result.append("|").append(array[k]);
            }

        }

        return result.toString();
    }

    public String parseDataAirLine(String[] array) {
        StringBuilder result = new StringBuilder();
        int len = array.length;
        for (int k = 0; k < len; k++) {
            if (k == 0) {
                result.append(array[k]);
            } else {
                result.append(";").append(array[k]);
            }
        }
        return result.toString();
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
    public void setPAYCODE(String payCode) {
        try {
            this.PAYCODE = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 16, "PAYCODE", payCode, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10044|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the cookie
     */
    public String getCookie() {
        return cookie;
    }

    /**
     * @param cookie the cookie to set
     */
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the ADDITIONALINFO
     */
    public String getADDITIONALINFO() {
        return ADDITIONALINFO;
    }

    /**
     * @param ADDITIONALINFO the ADDITIONALINFO to set
     */
    public void setADDITIONALINFO(String ADDITIONALINFO) {
        this.ADDITIONALINFO = ADDITIONALINFO;
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

//            boolean checkWords = validateWords(this, this.MALLID, this.CHAINMERCHANT);
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10045|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the SECURITYCODE
     */
    public String getSECURITYCODE() {
        return SECURITYCODE;
    }

    /**
     * @param SECURITYCODE the SECURITYCODE to set
     */
    public void setSECURITYCODE(String SECURITYCODE) {
        this.SECURITYCODE = SECURITYCODE;
    }

    /**
     * @return the FFNUMBER
     */
    public String getFFNUMBER() {
        return FFNUMBER;
    }

    /**
     * @param FFNUMBER the FFNUMBER to set
     */
    public void setFFNUMBER(String FFNUMBER) {
        this.FFNUMBER = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 1024, "FFNUMBER", FFNUMBER, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    public String getCUSTOMERID() {
        return CUSTOMERID;
    }

    public void setCUSTOMERID(String CUSTOMERID) {
        this.CUSTOMERID = CUSTOMERID;
    }

    public String getCUSTOMERTYPE() {
        return CUSTOMERTYPE;
    }

    public void setCUSTOMERTYPE(String CUSTOMERTYPE) {
        this.CUSTOMERTYPE = CUSTOMERTYPE;
    }

    public String getTOKENID() {
        return TOKENID;
    }

    public void setTOKENID(String TOKENID) {
        this.TOKENID = TOKENID;
    }

    /**
     * @return the ECI
     */
    public String getECI() {
        return ECI;
    }

    /**
     * @param ECI the ECI to set
     */
    public void setECI(String ECI) {

        try {
            this.ECI = OneCheckoutVerifyFormatData.validateString(2, 2, "ECI", ECI, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");

        } catch (OneCheckoutInvalidInputException iv) {

            this.ECI = "";
        }

    }

    /**
     * @return the XID
     */
    public String getXID() {
        return XID;
    }

    /**
     * @param XID the XID to set
     */
    public void setXID(String XID) {
        try {
            this.XID = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 28, "XID", XID, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10046|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the AUTHRESRESPONSECODE
     */
    public String getAUTHRESRESPONSECODE() {
        return AUTHRESRESPONSECODE;
    }

    /**
     * @param AUTHRESRESPONSECODE the AUTHRESRESPONSECODE to set
     */
    public void setAUTHRESRESPONSECODE(String AUTHRESRESPONSECODE) {
        try {
            this.AUTHRESRESPONSECODE = OneCheckoutVerifyFormatData.validateString(1, 1, "AUTHRESRESPONSECODE", AUTHRESRESPONSECODE, OneCheckoutVerifyFormatData.NUMERIC_VALUE, "");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10047|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the AUTHRESSTATUS
     */
    public String getAUTHRESSTATUS() {
        return AUTHRESSTATUS;
    }

    /**
     * @param AUTHRESSTATUS the AUTHRESSTATUS to set
     */
    public void setAUTHRESSTATUS(String AUTHRESSTATUS) {

        try {
            this.AUTHRESSTATUS = OneCheckoutVerifyFormatData.validateString(1, 1, "AUTHRESSTATUS", AUTHRESSTATUS, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10048|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the AUTHRESVENDORCODE
     */
    public String getAUTHRESVENDORCODE() {
        return AUTHRESVENDORCODE;
    }

    /**
     * @param AUTHRESVENDORCODE the AUTHRESVENDORCODE to set
     */
    public void setAUTHRESVENDORCODE(String AUTHRESVENDORCODE) {

        try {
            this.AUTHRESVENDORCODE = OneCheckoutVerifyFormatData.validateString(1, 30, "AUTHRESVENDORCODE", AUTHRESVENDORCODE, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10049|" + iv.getMessage(), this, iv);
        }

    }

    /**
     * @return the CAVVALGORITHM
     */
    public String getCAVVALGORITHM() {
        return CAVVALGORITHM;
    }

    /**
     * @param CAVVALGORITHM the CAVVALGORITHM to set
     */
    public void setCAVVALGORITHM(String CAVVALGORITHM) {

        try {
            this.CAVVALGORITHM = OneCheckoutVerifyFormatData.validateString(1, 1, "CAVVALGORITHM", CAVVALGORITHM, OneCheckoutVerifyFormatData.NUMERIC_VALUE, "");

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10050|" + iv.getMessage(), this, iv);
        }
    }

    /**
     * @return the CAVV
     */
    public String getCAVV() {
        return CAVV;
    }

    /**
     * @param CAVV the CAVV to set
     */
    public void setCAVV(String CAVV) {

        try {
            this.CAVV = OneCheckoutVerifyFormatData.validateString(1, 50, "CAVV", CAVV, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10051|" + iv.getMessage(), this, iv);
        }

    }

    public String getBILLDETAIL() {

        return BILLDETAIL;
    }

    public void setBILLDETAIL(String BILLDETAIL) {
        try {
            String temp = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 256, "BILLDETAIL", BILLDETAIL, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            this.BILLDETAIL = temp;
            OneCheckoutLogger.log("BILLDETAIL valid format");
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10052|" + iv.getMessage(), this, iv);
        }
    }

    public String getBILLNUMBER() {
        return BILLNUMBER;
    }

    public void setBILLNUMBER(String BILLNUMBER) {
        try {
            String temp = OneCheckoutVerifyFormatData.validateString(1, 16, "BILLNUMBER", BILLNUMBER, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            OneCheckoutLogger.log("BILLNUMBER valid format");
            this.BILLNUMBER = temp;
        } catch (OneCheckoutInvalidInputException ociie) {
            throw new InvalidPaymentRequestException("10053|" + ociie.getMessage(), this, ociie);
        }
    }

    public String getBILLTYPE() {
        return BILLTYPE;
    }

    public void setBILLTYPE(String BILLTYPE) {
        try {
            this.BILLTYPE = OneCheckoutVerifyFormatData.validateString(1, 1, "BILLTYPE", BILLTYPE.toUpperCase(), OneCheckoutVerifyFormatData.BILLTYPES_REGEX, "");
            OneCheckoutLogger.log("BILLTYPE valid format");
        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10054|" + iv.getMessage(), this, iv);
        }
    }

    public Date getENDDATE() {
        return ENDDATE;
    }

    public void setENDDATE(String ENDDATE) {
        try {
            String procdtime = OneCheckoutVerifyFormatData.validateString(8, 8, "ENDDATE", ENDDATE, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            this.ENDDATE = OneCheckoutVerifyFormatData.dateFormat.parse(procdtime);
            OneCheckoutLogger.log("ENDDATE valid format");
        } catch (ParseException iv) {
            iv.printStackTrace();
            throw new InvalidPaymentRequestException("10055|" + iv.getMessage(), this, iv);
        }
    }

    public String getEXECUTEDATE() {
        return EXECUTEDATE;
    }

    public void setEXECUTEDATE(String EXECUTEDATE) {
        try {
            if (this.EXECUTETYPE.equals("DAY")) {
                this.EXECUTEDATE = OneCheckoutVerifyFormatData.validateString(3, 64, "EXECUTEDATE", EXECUTEDATE.toUpperCase(), OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");
            } else if (this.EXECUTETYPE.equals("DATE")) {
                this.EXECUTEDATE = OneCheckoutVerifyFormatData.validateString(1, 128, "EXECUTEDATE", EXECUTEDATE, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");
            } else if (this.EXECUTETYPE.equals("FULLDATE")) {
                String procdtime = OneCheckoutVerifyFormatData.validateString(8, 4096, "EXECUTEDATE", EXECUTEDATE, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");
                Date temp = OneCheckoutVerifyFormatData.dateFormat.parse(procdtime);
                this.EXECUTEDATE = OneCheckoutVerifyFormatData.dateFormat.format(temp);
            }
            OneCheckoutLogger.log("EXECUTEDATE valid format");
        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidPaymentRequestException("10056|" + iv.getMessage(), this, iv);
        } catch (ParseException pe) {
            throw new InvalidPaymentRequestException("10056|" + pe.getMessage(), this, pe);
        }
    }

    public String getEXECUTEMONTH() {
        return EXECUTEMONTH;
    }

    public void setEXECUTEMONTH(String EXECUTEMONTH) {
        try {
            this.EXECUTEMONTH = OneCheckoutVerifyFormatData.validateString(3, 64, "EXECUTEMONTH", EXECUTEMONTH.toUpperCase(), OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM, "");
            OneCheckoutLogger.log("EXECUTEMONTH valid format");
        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidPaymentRequestException("10057|" + iv.getMessage(), this, iv);
        }
    }

    public String getEXECUTETYPE() {
        return EXECUTETYPE;
    }

    public void setEXECUTETYPE(String EXECUTETYPE) {
        try {
            this.EXECUTETYPE = OneCheckoutVerifyFormatData.validateString(3, 8, "EXECUTETYPE", EXECUTETYPE.toUpperCase(), OneCheckoutVerifyFormatData.EXECUTETYPE_REGEX);
            OneCheckoutLogger.log("EXECUTETYPE valid format");
        } catch (OneCheckoutInvalidInputException iv) {
            throw new InvalidPaymentRequestException("10058|" + iv.getMessage(), this, iv);
        }
    }

    public Date getSTARTDATE() {
        return STARTDATE;
    }

    public void setSTARTDATE(String STARTDATE) {
        try {
            String procdtime = OneCheckoutVerifyFormatData.validateString(8, 8, "STARTDATE", STARTDATE, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            this.STARTDATE = OneCheckoutVerifyFormatData.dateFormat.parse(procdtime);
            OneCheckoutLogger.log("STARTDATE valid format");
        } catch (ParseException iv) {
            iv.printStackTrace();
            throw new InvalidPaymentRequestException("10059|" + iv.getMessage(), this, iv);
        }
    }

    public String getINVOICENUMBER() {
        return INVOICENUMBER;
    }

    public void setINVOICENUMBER(String INVOICENUMBER) {
        try {
            String temp = OneCheckoutVerifyFormatData.validateString(1, 50, "INVOICENUMBER", INVOICENUMBER, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            OneCheckoutLogger.log("INVOICENUMBER valid format");
            this.INVOICENUMBER = temp;
        } catch (OneCheckoutInvalidInputException ociie) {
            throw new InvalidPaymentRequestException("10060|" + ociie.getMessage(), this, ociie);
        }
    }

    public String getDESCRIPTION() {
        return DESCRIPTION;
    }

    public void setDESCRIPTION(String DESCRIPTION) {
        try {
            String temp = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 256, "DESCRIPTION", DESCRIPTION, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
            this.DESCRIPTION = temp;
            OneCheckoutLogger.log("DESCRIPTION valid format");
        } catch (OneCheckoutInvalidInputException ociie) {
            message = ociie.getMessage();
            throw new InvalidPaymentRequestException("10061|" + ociie.getMessage(), this, ociie);
        }
    }

    public String getACQUIRERID() {
        return ACQUIRERID;
    }

    public void setACQUIRERID(String ACQUIRERID) {
        try {
            String temp = OneCheckoutVerifyFormatData.validateString(1, 16, "ACQUIRERID", ACQUIRERID, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            OneCheckoutLogger.log("ACQUIRERID valid format");
            this.ACQUIRERID = temp;
        } catch (OneCheckoutInvalidInputException ociie) {
            throw new InvalidPaymentRequestException("10062|" + ociie.getMessage(), this, ociie);
        }
    }

    public String getDEVICEID() {
        return DEVICEID;
    }

    public void setDEVICEID(String DEVICEID) {
        this.DEVICEID = DEVICEID;
    }

    /**
     * @return the CUSTIP
     */
    public String getCUSTIP() {
        return CUSTIP;
    }

    /**
     * @param CUSTIP the CUSTIP to set
     */
    public void setCUSTIP(String CUSTIP) {
        this.CUSTIP = CUSTIP;
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

    public double getCONVERTEDAMOUNT() {
        return CONVERTEDAMOUNT;
    }

    public void setCONVERTEDAMOUNT(double CONVERTEDAMOUNT) {
        this.CONVERTEDAMOUNT = CONVERTEDAMOUNT;
    }

    public double getREGISTERAMOUNT() {
        return REGISTERAMOUNT;
    }

    public void setRegisterAMOUNT(String RegisterAmount) {

        try {

            if (RegisterAmount != null && RegisterAmount.contains(",")) {
                throw new OneCheckoutInvalidInputException(String.format("Invalid REGISTERAMOUNT Value because contains [,] value"));
            } /* else if (amount != null && !amount.contains(".00")) {
             throw new OneCheckoutInvalidInputException(String.format("Invalid AMOUNT Value because is not contains [.00] value"));
             } else */ {
                this.REGISTERAMOUNT = super.validateRegisterAmount(RegisterAmount, "REGISTERAMOUNT");
            }

        } catch (OneCheckoutInvalidInputException iv) {
            message = iv.getMessage();
            throw new InvalidPaymentRequestException("10002|" + iv.getMessage(), this, iv);
        }
    }

    public void setREGISTERAMOUNT(double REGISTERAMOUNT) {
        this.REGISTERAMOUNT = REGISTERAMOUNT;
    }

    public String getPaymentTypeKredivo() {
        return paymentTypeKredivo;
    }

    public void setPaymentTypeKredivo(String paymentTypeKredivo) {
        this.paymentTypeKredivo = paymentTypeKredivo;
    }

    public Boolean getVAPartialAmount() {
        return VAPartialAmount;
    }

    public void setVAPartialAmount(String VAPartialAmount) {
        if (VAPartialAmount == null) {
            this.VAPartialAmount = false;
        } else if (VAPartialAmount.equals("1")) {
            this.VAPartialAmount = true;
        }

    }

    public Boolean getVAOpenAmount() {
        return VAOpenAmount;
    }

    public void setVAOpenAmount(String VACloseOpenAmount) {
        if (VACloseOpenAmount == null) {
            this.VAOpenAmount = false;
        } else if (VACloseOpenAmount.equalsIgnoreCase("O")) {
            this.VAOpenAmount = true;
        } else {
            this.VAOpenAmount = false;
        }

    }

    public String getVAinitNumberFormat() {
        return VAinitNumberFormat;
    }

    public void setVAinitNumberFormat(String VAinitNumberFormat) {
        this.VAinitNumberFormat = VAinitNumberFormat;
    }

}
