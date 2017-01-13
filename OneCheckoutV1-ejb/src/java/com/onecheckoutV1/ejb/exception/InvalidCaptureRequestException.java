/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutCaptureRequest;

/**
 *
 * @author hafizsjafioedin
 */

//syamsul
public class InvalidCaptureRequestException extends RuntimeException {

    private OneCheckoutCaptureRequest oneCheckoutCaptureRequest;

    public InvalidCaptureRequestException(String message) {
        super(message);
    }

    public InvalidCaptureRequestException(String message, OneCheckoutCaptureRequest oneCheckoutCaptureRequest) {
        super(message);
        setPaymentRequest(oneCheckoutCaptureRequest);
    }

    public InvalidCaptureRequestException(String message, OneCheckoutCaptureRequest oneCheckoutCaptureRequest, Throwable throwable) {
        super(message);
        setPaymentRequest(oneCheckoutCaptureRequest);
        initCause(throwable);
    }

    private void setPaymentRequest(OneCheckoutCaptureRequest oneCheckoutCaptureRequest) {
        this.oneCheckoutCaptureRequest = oneCheckoutCaptureRequest;
    }

    public OneCheckoutCaptureRequest getPaymentRequest() {
        return (OneCheckoutCaptureRequest) oneCheckoutCaptureRequest;
    }
}
