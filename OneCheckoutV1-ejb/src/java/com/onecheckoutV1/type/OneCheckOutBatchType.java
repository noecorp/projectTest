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
public enum OneCheckOutBatchType {

    MPG('M'), ESCROW('E');
    char type;

    OneCheckOutBatchType(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    public static OneCheckOutBatchType findType(char c) {
        OneCheckOutBatchType[] types = OneCheckOutBatchType.values();
        for (OneCheckOutBatchType ttype : types) {
            if (ttype.value() == c) {
                return ttype;
            }
        }
        return null;
    }
}
