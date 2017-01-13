/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutEDSGetData;

/**
 *
 * @author hafizsjafioedin
 */
public class InvalidEDSGetDataException  extends RuntimeException{
    
        private OneCheckoutEDSGetData oi;
        
        public InvalidEDSGetDataException(String message){
                super(message);
        }

        public InvalidEDSGetDataException(String message, OneCheckoutEDSGetData oi){
                super(message);
                setEDSGetData(oi);
        }
         public InvalidEDSGetDataException(String message, OneCheckoutEDSGetData oi, Throwable th){
                super(message);
                setEDSGetData(oi);
                initCause(th);
        }
        private void setEDSGetData(OneCheckoutEDSGetData pi){
            this.oi=pi;
        }
        public OneCheckoutEDSGetData getEDSGetData(){
            return (OneCheckoutEDSGetData) oi;
        }
}
