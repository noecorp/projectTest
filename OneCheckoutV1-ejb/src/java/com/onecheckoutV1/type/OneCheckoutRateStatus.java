/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

/**
 *
 * @author jauhaf
 */
public enum OneCheckoutRateStatus {

    NEW('N'),
    OLD('O'),
    EXPIRED('E'),;
    private Character code;

    private OneCheckoutRateStatus(Character code) {
        this.code = code;
    }

    public Character code() {
        return code;
    }
}
