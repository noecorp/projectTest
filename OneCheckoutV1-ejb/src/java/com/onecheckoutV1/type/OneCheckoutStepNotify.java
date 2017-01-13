/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author aditya
 */
public enum OneCheckoutStepNotify {

    // OTHER
    CANCEL_BY_USER("CBU"),
    NOT_GOOD_REQUEST("NGR"),
    INVOKE_STATUS("IS"),
    EDS_UPDATE_STATUS("EUS"),

    // PAYMENT
    IDENTIFY_PAYMENT("IP"),
    VOID_PAYMENT("VP"),
    REVERSAL_PAYMENT("RP");
    
    String type;
    private static final long serialVersionUID = 1L;

    OneCheckoutStepNotify(String type) {
        this.type = type;
    }

    public String value() {
        return type;
    }

    public static OneCheckoutStepNotify findType(String c) {
        OneCheckoutStepNotify[] types = OneCheckoutStepNotify.values();
        for (OneCheckoutStepNotify ttype : types) {
            if (ttype.value().equals(c)) {
                return ttype;
            }
        }
        return null;
    }

}
