/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;



/**
 *
 * @author hafizsjafioedin
 */
public class InvalidDOKUNotifyDataException extends RuntimeException{
    
        private OneCheckoutDOKUNotifyData oi;
        
        public InvalidDOKUNotifyDataException(String message){
                super(message);
        }

        public InvalidDOKUNotifyDataException(String message, OneCheckoutDOKUNotifyData oi){
                super(message);
                setDOKUNotifyData(oi);
        }
         public InvalidDOKUNotifyDataException(String message, OneCheckoutDOKUNotifyData oi, Throwable th){
                super(message);
                setDOKUNotifyData(oi);
                initCause(th);
        }
        private void setDOKUNotifyData(OneCheckoutDOKUNotifyData pi){
            this.oi=pi;
        }
        public OneCheckoutDOKUNotifyData getNotifyData(){
            return (OneCheckoutDOKUNotifyData) oi;
        }
}
