/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
public enum OneCheckOutSettlementStatus {

    OPEN('O'), CLOSE('C');
    char type;

    OneCheckOutSettlementStatus(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    public static OneCheckOutSettlementStatus findType(char c) {
        OneCheckOutSettlementStatus[] types = OneCheckOutSettlementStatus.values();
        for (OneCheckOutSettlementStatus ttype : types) {
            if (ttype.value() == c) {
                return ttype;
            }
        }
        return null;
    }
}
