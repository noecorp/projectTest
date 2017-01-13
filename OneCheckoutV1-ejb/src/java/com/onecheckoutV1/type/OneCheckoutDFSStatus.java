/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutDFSStatus {
    
    APPROVE('A'),
    REVIEW('R'),
    REJECT('J'),
    HIGHRISK('H'),
    NA('U'),
    LOW_RISK('L'),
    MEDIUM_RISK('M'),
    HIGH_RISK('T'),
    PENDING('P');
    char type;

    OneCheckoutDFSStatus(char type) {
        this.type = type;
    }

    public char value() {
        return type;
    }

    
    //public static String getName(OneCheckoutDFSStatus status) {
        
//        if (status==ACCEPT)
//            return "APPROVE";
//        else
  //          return status.name();
  //  }
    
    public static OneCheckoutDFSStatus findType(char c) {
        OneCheckoutDFSStatus[] types = OneCheckoutDFSStatus.values();
        for (OneCheckoutDFSStatus ttype : types) {
            if (ttype.value()==c) {
                return ttype;
            }
        }
        return null;
    }    
            
}
