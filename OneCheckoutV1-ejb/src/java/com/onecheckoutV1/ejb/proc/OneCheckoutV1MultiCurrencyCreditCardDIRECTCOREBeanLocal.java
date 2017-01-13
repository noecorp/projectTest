/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onechekoutv1.dto.PaymentChannel;
import javax.ejb.Local;

/**
 *
 * @author hafiz
 */
@Local
public interface OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBeanLocal  extends OneCheckoutV1CreditCardPGBase {
    
    public boolean doMPGReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq);
    
}
