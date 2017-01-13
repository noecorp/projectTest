/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Opiks
 */
public enum EParameterName {

    PAYMENT_CHANNEL("PAYMENTCHANNEL"),
    MALL_ID("MALLID"),
    CHAIN_MALL_ID("CHAINMERCHANT"),
    INVOICE_NUMBER("TRANSIDMERCHANT"),
    CURRENCY("CURRENCY"),
    AMOUNT("AMOUNT"),
    REGISTERAMOUNT("REGISTERAMOUNT"),
    PURCHASE_CURRENCY("PURCHASECURRENCY"),
    PURCHASE_AMOUNT("PURCHASEAMOUNT"),
    SESSION_ID("SESSIONID"),
    REQUEST_DATE_TIME("REQUESTDATETIME"),
    WORDS("WORDS"),
    BUTTON("BUTTON"),
    CUSTOMER_ID("CUSTOMERID"),
    CUSTOMER_NAME("NAME"),
    CUSTOMER_EMAIL("EMAIL"),
    CUSTOMER_ADDRESS("ADDRESS"),
    CUSTOMER_CITY("CITY"),
    CUSTOMER_STATE("STATE"),
    CUSTOMER_COUNTRY("COUNTRY"),
    CUSTOMER_ZIP_CODE("ZIPCODE"),
    CUSTOMER_HOME_PHONE("HOMEPHONE"),
    CUSTOMER_WORK_PHONE("WORKPHONE"),
    CUSTOMER_MOBILE_PHONE("MOBILEPHONE"),
    BILLING_TYPE("BILLTYPE"),
    BILLING_NUMBER("BILLNUMBER"),
    BILLING_DETAIL("BILLDETAIL"),
    EXECUTE_TYPE("EXECUTETYPE"),
    EXECUTE_DATE("EXECUTEDATE"),
    EXECUTE_MONTH("EXECUTEMONTH"),
    EXECUTE_START_DATE("STARTDATE"),
    EXECUTE_END_DATE("ENDDATE"),
    FLAT_STATUS("FLATSTATUS"),
    CASH_NOW("CASHNOW"),
    CARD_NUMBER("CARDNUMBER"),
    EXPIRY_MONTH("EXPIRYMONTH"),
    EXPIRY_DATE("EXPIRYDATE"),
    EXPIRY_YEAR("EXPIRYYEAR"),
    CVV2("CVV2"),
    CC_ADDRESS("CCADDRESS"),
    CC_NAME("CCNAME"),
    CC_EMAIL("CCEMAIL"),
    CC_CITY("CCCITY"),
    CC_STATE_OR_REGION("CCSTATE"),
    CC_COUNTRY("CCCOUNTRY"),
    CC_MOBILE_PHONE("CCMOBILEPHONE"),
    CC_HOME_PHONE("CCHOMEPHONE"),
    CC_WORK_PHONE("CCWORKPHONE"),
    CC_ZIPCODE("CCZIPCODE"),
    TOKEN_NUMBER("TOKENID"),
    AUTH_STATUS("AUTHSTATUS"),
    CUSTOMER_IP("CUSTOMERIP"),
    RESULT_MESSAGE("RESULTMSG"),
    ACTIVITY("ACTIVITY"),
    RESPONSE_DATE_TIME("RESPONSEDATETIME"),
    RESPONSE_CODE("RESPONSECODE"),
    APPROVAL_CODE("APPROVALCODE"),
    BANK("BANK"),
    VERIFY_ID("VERIFYID"),
    VERIFY_SCORE("VERIFYSCORE"),
    VERIFY_STATUS("VERIFYSTATUS"),
    MERCHANT_DATA("MD"),
    PAYER_AUTHENTICATION_RESPONSE("PaRes"),
    TITLE("title"),
    BASKET("BASKET"),
    CCSAVESTATUS("CCSAVESTATUS");
    private String eParameterName;
    private static final Map<String, EParameterName> lookup = new HashMap<String, EParameterName>();


    static {
        for (EParameterName s : EnumSet.allOf(EParameterName.class)) {
            getLookup().put(s.code(), s);
        }
    }

    EParameterName(String eParameterName) {
        this.eParameterName = eParameterName;
    }

    public static Map<String, EParameterName> getLookup() {
        return lookup;
    }

    public String code() {
        return eParameterName;
    }
}
