/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutPaymentRequest;

/**
 *
 * @author hafizsjafioedin
 */
public class InvalidPaymentRequestException   extends RuntimeException{
    
        private OneCheckoutPaymentRequest oi;
        
        public InvalidPaymentRequestException(String message){
                super(message);
        }

        public InvalidPaymentRequestException(String message, OneCheckoutPaymentRequest oi){
                super(message);
                setPaymentRequest(oi);
        }
         public InvalidPaymentRequestException(String message, OneCheckoutPaymentRequest oi, Throwable th){
                super(message);
                setPaymentRequest(oi);
                initCause(th);
        }
        private void setPaymentRequest(OneCheckoutPaymentRequest pi){
            this.oi=pi;
        }
        public OneCheckoutPaymentRequest getPaymentRequest(){
            return (OneCheckoutPaymentRequest) oi;
        }
}
