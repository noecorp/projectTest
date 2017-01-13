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
public enum OneCheckOutTransactionsType {

//    NEW('N'), SUCCESS('S'), FAILED('F'), REVERSED('R'), VOIDED('V'), REFUND('D'), UNPAID('U'), ONPROCESS('P'), ERROR('E'), CAPTURE('C');
    AUTHORIZE('A'),CAPTURE('C'), REFUND('R'), SETTLEMENT('T'), SALE('S');
    char type;
//    private static final long serialVersionUID = 1L;

    OneCheckOutTransactionsType(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    public static OneCheckOutTransactionsType findType(char c) {
        OneCheckOutTransactionsType[] types = OneCheckOutTransactionsType.values();
        for (OneCheckOutTransactionsType ttype : types) {
            if (ttype.value() == c) {
                return ttype;
            }
        }
        return null;
    }
}
