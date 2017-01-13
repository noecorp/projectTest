/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

import com.onecheckoutV1.data.OneCheckoutEDSUpdateStatusData;

/**
 *
 * @author hafizsjafioedin
 */
public class InvalidEDSUpdateStatusDataException  extends RuntimeException{

       private OneCheckoutEDSUpdateStatusData oi;
        
        public InvalidEDSUpdateStatusDataException(String message){
                super(message);
        }

        public InvalidEDSUpdateStatusDataException(String message, OneCheckoutEDSUpdateStatusData oi){
                super(message);
                setEDSUpdateStatusData(oi);
        }
         public InvalidEDSUpdateStatusDataException(String message, OneCheckoutEDSUpdateStatusData oi, Throwable th){
                super(message);
                setEDSUpdateStatusData(oi);
                initCause(th);
        }
        private void setEDSUpdateStatusData(OneCheckoutEDSUpdateStatusData pi){
            this.oi=pi;
        }
        public OneCheckoutEDSUpdateStatusData getEDSUpdateStatusData(){
            return (OneCheckoutEDSUpdateStatusData) oi;
        }   
}
