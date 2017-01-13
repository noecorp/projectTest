/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutReturnType {

    OneWay('0'), Return('1'), Transit('2'), TransitAndReturn('3'), MultiCity('4');
    char type;

    OneCheckoutReturnType(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    public static OneCheckoutReturnType findType(char c) {
        OneCheckoutReturnType[] types = OneCheckoutReturnType.values();
        for (OneCheckoutReturnType ttype : types) {
            if (ttype.value()==c) {
                return ttype;
            }
        }
        return null;
    }        
}
