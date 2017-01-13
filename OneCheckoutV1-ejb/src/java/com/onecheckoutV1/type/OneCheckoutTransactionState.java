/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutTransactionState {

    INCOMING('I'), NOTIFYING('N'), PROCESS('P') ,DONE('D'), SETTLEMENT('S');
    char type;

    OneCheckoutTransactionState(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    public static OneCheckoutTransactionState findType(char c) {
        OneCheckoutTransactionState[] types = OneCheckoutTransactionState.values();
        for (OneCheckoutTransactionState ttype : types) {
            if (ttype.value() == c) {
                return ttype;
            }
        }
        return null;
    }
}