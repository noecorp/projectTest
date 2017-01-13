/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.ejb.helper.OneCheckoutTransactionProcessor;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author hafiz
 */
public interface OneCheckoutV1CreditCardPGBase extends OneCheckoutTransactionProcessor{

    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml, Merchants merchants);

    public OneCheckoutDataPGRedirect parsing3DSecureData(XMLConfiguration xml);
    public String getCoreMipURL(PaymentChannel pChannel, String step);
    
}
    

