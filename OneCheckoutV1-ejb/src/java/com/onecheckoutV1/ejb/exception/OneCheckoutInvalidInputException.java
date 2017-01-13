/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.exception;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutInvalidInputException   extends RuntimeException {
        protected Object inputObject;
        public OneCheckoutInvalidInputException(String message){
                super(message);
        }
}
