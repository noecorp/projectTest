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
public enum EMerchantActivityStatus {

    START('N'),
    DONE_FAIL('F'),
    DONE_SUCCESS('S'),
    FINISH('D');
    private char eMerchantActivityStatus;
    private static final Map<Character, EMerchantActivityStatus> lookup = new HashMap<Character, EMerchantActivityStatus>();


    static {
        for (EMerchantActivityStatus s : EnumSet.allOf(EMerchantActivityStatus.class)) {
            getLookup().put(s.code(), s);
        }
    }

    EMerchantActivityStatus(char eMerchantActivityStatus) {
        this.eMerchantActivityStatus = eMerchantActivityStatus;
    }

    public static Map<Character, EMerchantActivityStatus> getLookup() {
        return lookup;
    }

    public char code() {
        return eMerchantActivityStatus;
    }
}
