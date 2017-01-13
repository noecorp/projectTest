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
public enum EActivity {

    DO_RECUR_CUSTOMER_REGISTRATION_REQUEST(1),
    DO_RECUR_CUSTOMER_REGISTRATION_DATA(2),
    DO_RECUR_CUSTOMER_REGISTRATION_AUTHORIZE(3),
    DO_RECUR_CUSTOMER_REGISTRATION_THREE_D_SECURE(4),
    DO_RECUR_CUSTOMER_REGISTRATION_PAYMENT(5),
    DO_RECUR_CUSTOMER_REGISTRATION(6),
    DO_RECUR_CUSTOMER_UPDATE_REQUEST(7),
    DO_RECUR_CUSTOMER_UPDATE_DATA(8),
    DO_RECUR_CUSTOMER_UPDATE_AUTHORIZE(9),
    DO_RECUR_CUSTOMER_UPDATE_THREE_D_SECURE(10),
    DO_RECUR_CUSTOMER_UPDATE_PAYMENT(11),
    DO_RECUR_CUSTOMER_UPDATE(12),
    DO_TOKEN_PAYMENT_REQUEST(13),
    DO_TOKEN_PAYMENT_DATA(14),
    DO_TOKEN_PAYMENT(15),
    DO_TOKEN_PAYMENT_THREE_D_SECURE(16),
    DO_TOKEN_PAYMENT_THREE_D_SECURE_PROCESS(17),
    DO_TOKEN_UPDATE_CARD(18),
    DO_TOKEN_DELETE_CARD(19);
    
    private int code;
    private static final Map<Integer, EActivity> lookup = new HashMap<Integer, EActivity>();


    static {
        for (EActivity s : EnumSet.allOf(EActivity.class)) {
            getLookup().put(s.code(), s);
        }
    }

    public static Map<Integer, EActivity> getLookup() {
        return lookup;
    }

    EActivity(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
