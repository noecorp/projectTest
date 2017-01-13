/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutFlightType {
    DOMESTIC("01"),
    INTERNATIONAL("02");
    String type;

    OneCheckoutFlightType(String type) {
        this.type = type;
    }

    public String value() {
        return type;
    }

    public static OneCheckoutFlightType findType(String c) {
        OneCheckoutFlightType[] types = OneCheckoutFlightType.values();
        for (OneCheckoutFlightType ttype : types) {
            if (ttype.value().equals(c)) {
                return ttype;
            }
        }
        return null;
    }    
        
}
