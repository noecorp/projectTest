/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutRedirectData;

/**
 *
 * @author hafizsjafioedin
 */
public class InvalidDOKURedirectDataException   extends RuntimeException{
    
        private OneCheckoutRedirectData oi;
        
        public InvalidDOKURedirectDataException(String message){
                super(message);
        }

        public InvalidDOKURedirectDataException(String message, OneCheckoutRedirectData oi){
                super(message);
                setRedirectData(oi);
        }
         public InvalidDOKURedirectDataException(String message, OneCheckoutRedirectData oi, Throwable th){
                super(message);
                setRedirectData(oi);
                initCause(th);
        }
        private void setRedirectData(OneCheckoutRedirectData pi){
            this.oi=pi;
        }
        public OneCheckoutRedirectData getRedirectData(){
            return (OneCheckoutRedirectData) oi;
        }
    
    
}
