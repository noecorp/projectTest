/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.OneCheckoutTransactionProcessor;
import com.onechekoutv1.dto.PaymentChannel;
import javax.ejb.Local;

/**
 *
 * @author hafizsjafioedin
 */
@Local
public interface OneCheckoutV1KlikBCABeanLocal  extends OneCheckoutTransactionProcessor {

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq);
}
