/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutVoidRequest;

/**
 *
 * @author hafizsjafioedin
 */
public class InvalidVoidRequestException   extends RuntimeException{
    
        private OneCheckoutVoidRequest oi;
        
        public InvalidVoidRequestException(String message){
                super(message);
        }

        public InvalidVoidRequestException(String message, OneCheckoutVoidRequest oi){
                super(message);
                setPaymentRequest(oi);
        }
         public InvalidVoidRequestException(String message, OneCheckoutVoidRequest oi, Throwable th){
                super(message);
                setPaymentRequest(oi);
                initCause(th);
        }
        private void setPaymentRequest(OneCheckoutVoidRequest pi){
            this.oi=pi;
        }
        public OneCheckoutVoidRequest getPaymentRequest(){
            return (OneCheckoutVoidRequest) oi;
        }
    
}
