/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutEDSGetData;
import com.onecheckoutV1.ejb.helper.OneCheckoutTransactionProcessor;
import javax.ejb.Local;

/**
 *
 * @author hafizsjafioedin
 */
@Local
public interface OneCheckoutV1CreditCardBeanLocal  extends OneCheckoutTransactionProcessor {

//    public String getMOBTransactionData(OneCheckoutEDSGetData getData);

    public String getEDSDataEmptyACK();
}
