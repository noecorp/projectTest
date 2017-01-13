/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutMerchantCategory {
    
    AIRLINE('0'), NONAIRLINE('1');
    char type;
    private static final long serialVersionUID = 1L;

    OneCheckoutMerchantCategory(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    public static OneCheckoutMerchantCategory findType(char c) {
        OneCheckoutMerchantCategory[] types = OneCheckoutMerchantCategory.values();
        for (OneCheckoutMerchantCategory ttype : types) {
            if (ttype.value()==c) {
                return ttype;
            }
        }
        return null;
    }        
}
