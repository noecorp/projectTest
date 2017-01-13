/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author aditya
 */
public enum OneCheckoutEmailType {

    HTML('N'),
    TEXT('S'),
    HTML_ATTACHMENT('A'),
    TEXT_ATTACHMENT('B');
    char type;

    OneCheckoutEmailType(char type) {
        this.type = type;
    }

    public char code() {
        return type;
    }
}
