/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.ejb.helper.OneCheckoutTransactionProcessor;
import javax.ejb.Local;

/**
 *
 * @author aditya
 */
@Local
public interface OneCheckoutV1PermataVALiteBeanLocal extends OneCheckoutTransactionProcessor {

    public doku.virtualaccount.xml.VARegistrationResponseDocument.VARegistrationResponse registerVa(com.onecheckoutV1.data.OneCheckoutDataHelper trxHelper);
}
