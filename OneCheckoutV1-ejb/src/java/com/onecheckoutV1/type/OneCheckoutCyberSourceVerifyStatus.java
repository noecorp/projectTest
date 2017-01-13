/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.type;

/**
 *
 * @author aditya
 */
public enum OneCheckoutCyberSourceVerifyStatus {

    ACCEPT('A'),
    REVIEW('R'),
    REJECT('J'),
    UNDEFINED('U');
    char type;

    OneCheckoutCyberSourceVerifyStatus(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }


    public static String getName(OneCheckoutCyberSourceVerifyStatus status) {

        if (status==ACCEPT)
            return "APPROVE";
        else
            return status.name();
    }

    public static OneCheckoutCyberSourceVerifyStatus findType(char c) {
        OneCheckoutCyberSourceVerifyStatus[] types = OneCheckoutCyberSourceVerifyStatus.values();
        for (OneCheckoutCyberSourceVerifyStatus ttype : types) {
            if (ttype.value()==c) {
                return ttype;
            }
        }
        return null;
    }    

}
