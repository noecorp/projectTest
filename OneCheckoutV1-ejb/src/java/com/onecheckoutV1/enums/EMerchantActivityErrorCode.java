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
public enum EMerchantActivityErrorCode {

    INSUFFICIENT_PARAM("XX01"),
    INVALID_WORDS("XX02"),
    FAILED_PARSING_RECUR_CUSTOMER_REGISTRATION_REQUEST_FROM_MERCHANT("XX03"),
    FAILED_PARSING_RECUR_CUSTOMER_REGISTRATION_DATA("XX04"),
    FAILED_PARSING_RECUR_THREE_D_SECURE_DATA("XX05"),
    FAILED_PARSING_RECUR_CUSTOMER_UPDATE_REQUEST_FROM_MERCHANT("XX06"),
    FAILED_PARSING_RECUR_CUSTOMER_UPDATE_DATA("XX07"),
    FAILED_PARSING_TOKEN_PAYMENT_REQUEST("XX08"),
    FAILED_PARSING_TOKEN_PAYMENT_DATA("XX09");
     
    private String eMerchantActivityErrorCode;
    private static final Map<String, EMerchantActivityErrorCode> lookup = new HashMap<String, EMerchantActivityErrorCode>();


    static {
        for (EMerchantActivityErrorCode s : EnumSet.allOf(EMerchantActivityErrorCode.class)) {
            getLookup().put(s.code(), s);
        }
    }

    EMerchantActivityErrorCode(String eMerchantActivityErrorCode) {
        this.eMerchantActivityErrorCode = eMerchantActivityErrorCode;
    }

    public static Map<String, EMerchantActivityErrorCode> getLookup() {
        return lookup;
    }

    public String code() {
        return eMerchantActivityErrorCode;
    }
}
