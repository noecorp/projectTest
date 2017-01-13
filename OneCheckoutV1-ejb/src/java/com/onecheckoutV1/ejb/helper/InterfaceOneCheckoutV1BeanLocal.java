/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import java.util.HashMap;
import javax.ejb.Local;

/**
 *
 * @author hafizsjafioedin
 */
@Local
public interface InterfaceOneCheckoutV1BeanLocal {

    public OneCheckoutDataHelper ProcessCheckStatus(OneCheckoutDataHelper onecheckout);

    public OneCheckoutDataHelper ProcessGetTodayTransactions(OneCheckoutDataHelper onecheckout);

    public OneCheckoutDataHelper ProcessPayment(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessRetryCheckStatus(OneCheckoutDataHelper onecheckout);

    public OneCheckoutDataHelper ProcessVoid(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessInquiryInvoice(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessInvokeStatus(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessGetEDSData(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessUpdateEDSStatus(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessRedirectToMerchant(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessMIPPayment(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessCCVoid(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessReversal(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessReconcile(OneCheckoutDataHelper trxHelper);

    public OneCheckoutDataHelper ProcessCyberSourceReview(OneCheckoutDataHelper trxHelper);
    
    public OneCheckoutDataHelper ProcessRefund(OneCheckoutDataHelper checkoutDataHelper);
}
