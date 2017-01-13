/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import javax.ejb.Stateless;

/**
 *
 * @author aditya
 */
@Stateless
public class OneCheckoutV1VCNBean extends OneCheckoutChannelBase implements OneCheckoutV1VCNBeanLocal {

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
