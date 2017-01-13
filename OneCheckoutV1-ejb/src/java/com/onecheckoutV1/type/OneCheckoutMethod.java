/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutMethod {
    CIP('C'),MIP('M');
    char type;
    private static final long serialVersionUID = 1L;

    OneCheckoutMethod(char type){
        this.type=type;
    }
    public char value(){
        return type;
    }
    public static OneCheckoutMethod findType(char c){
        OneCheckoutMethod[] types = OneCheckoutMethod.values();
        for(OneCheckoutMethod ttype : types){
            if(ttype.value()==c)
                return ttype;
        }
        return null;
    }            
}
