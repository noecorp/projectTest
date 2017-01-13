/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Opiks
 */
public enum OneCheckoutVAMerchantResponseCode {

    INVALID_ACCOUNT_NUMBER("3000"),
    INVALID_AMOUNT("3001"),
    TRANSACTION_HAS_BEEN_PAID("3002"),
    ACCOUNT_NUMBER_WAS_EXPIRED("3004"),
    SUCCESS("0000"),
    FAILED("9999");
    private String oneCheckoutVAMerchantResponseCode;
    private static final Map<String, OneCheckoutVAMerchantResponseCode> lookup = new HashMap<String, OneCheckoutVAMerchantResponseCode>();


    static {
        for (OneCheckoutVAMerchantResponseCode s : EnumSet.allOf(OneCheckoutVAMerchantResponseCode.class)) {
            getLookup().put(s.code(), s);
        }
    }

    OneCheckoutVAMerchantResponseCode(String oneCheckoutVAMerchantResponseCode) {
        this.oneCheckoutVAMerchantResponseCode = oneCheckoutVAMerchantResponseCode;
    }

    public static Map<String, OneCheckoutVAMerchantResponseCode> getLookup() {
        return lookup;
    }

    public String code() {
        return oneCheckoutVAMerchantResponseCode;
    }
}
