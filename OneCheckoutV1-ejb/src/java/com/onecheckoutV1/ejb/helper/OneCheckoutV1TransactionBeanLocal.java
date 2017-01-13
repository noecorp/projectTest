/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Transactions;
import javax.ejb.Local;

/**
 *
 * @author hafizsjafioedin
 */
@Local
public interface OneCheckoutV1TransactionBeanLocal {

    public Transactions saveTransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc);

    public Transactions updateTransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc);

    public Transactions saveSettlementData(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc);
    
    public Transactions saveTrannsactionRefund(Transactions transactions, String amount);

    public boolean saveInvalidParamsTransactions(OneCheckoutDataHelper trxHelper);

    public Transactions saveNotFoundInquiryTransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc);

    public Transactions createReuseVATransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc, Transactions transInit);
    
}
