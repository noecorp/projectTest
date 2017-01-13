/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import javax.ejb.Local;

/**
 *
 * @author hafiz
 */
@Local
public interface OneCheckoutV1CreditCardDIRECTCOREBeanLocal extends OneCheckoutV1CreditCardPGBase {

//    public String getMOBTransactionData(OneCheckoutEDSGetData getData);

    public String getEDSDataEmptyACK();
}
