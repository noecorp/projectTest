/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutThirdPartyStatus {
    

    TravelArrangerJointTheFlight('0'), TravelArrangerDoesNotJointTheFlight('1');
    char type;

    OneCheckoutThirdPartyStatus(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    public static OneCheckoutThirdPartyStatus findType(char c) {
        OneCheckoutThirdPartyStatus[] types = OneCheckoutThirdPartyStatus.values();
        for (OneCheckoutThirdPartyStatus ttype : types) {
            if (ttype.value()==c) {
                return ttype;
            }
        }
        return null;
    }            
}
