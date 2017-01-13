/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutDOKUVerifyData;

/**
 *
 * @author hafizsjafioedin
 */
public class InvalidDOKUVerifyDataException extends RuntimeException{
    
        private OneCheckoutDOKUVerifyData oi;
        
        public InvalidDOKUVerifyDataException(String message){
                super(message);
        }

        public InvalidDOKUVerifyDataException(String message, OneCheckoutDOKUVerifyData oi){
                super(message);
                setDOKUVerifyData(oi);
        }
         public InvalidDOKUVerifyDataException(String message, OneCheckoutDOKUVerifyData oi, Throwable th){
                super(message);
                setDOKUVerifyData(oi);
                initCause(th);
        }
        private void setDOKUVerifyData(OneCheckoutDOKUVerifyData pi){
            this.oi=pi;
        }
        public OneCheckoutDOKUVerifyData getVerifyData(){
            return (OneCheckoutDOKUVerifyData) oi;
        }
}
