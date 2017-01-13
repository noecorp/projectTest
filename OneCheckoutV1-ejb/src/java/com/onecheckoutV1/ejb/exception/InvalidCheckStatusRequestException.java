/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutCheckStatusData;

/**
 *
 * @author aditya
 */

//xasxasxasx
public class InvalidCheckStatusRequestException extends RuntimeException {
    
        private OneCheckoutCheckStatusData oi;
        
        public InvalidCheckStatusRequestException(String message){
                super(message);
        }

        public InvalidCheckStatusRequestException(String message, OneCheckoutCheckStatusData oi){
                super(message);
                setPaymentRequest(oi);
        }
         public InvalidCheckStatusRequestException(String message, OneCheckoutCheckStatusData oi, Throwable th){
                super(message);
                setPaymentRequest(oi);
                initCause(th);
        }
        private void setPaymentRequest(OneCheckoutCheckStatusData pi){
            this.oi=pi;
        }
        public OneCheckoutCheckStatusData getPaymentRequest(){
            return (OneCheckoutCheckStatusData) oi;
        }
    
}
