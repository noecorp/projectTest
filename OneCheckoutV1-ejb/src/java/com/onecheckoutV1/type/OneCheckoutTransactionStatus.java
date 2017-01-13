/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutTransactionStatus {

    NEW('N'),SUCCESS('S'),FAILED('F'),REVERSED('R'),VOIDED('V'),REFUND('D'), UNPAID('U'), ONPROCESS('P'), ERROR('E');
    char type;
    private static final long serialVersionUID = 1L;

    OneCheckoutTransactionStatus(char type){
        this.type=type;
    }
    public char value(){
        return type;
    }
    public static OneCheckoutTransactionStatus findType(char c){
        OneCheckoutTransactionStatus[] types =OneCheckoutTransactionStatus.values();
        for(OneCheckoutTransactionStatus ttype : types){
            if(ttype.value()==c)
                return ttype;
        }
        return null;
    }        
}
