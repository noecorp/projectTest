/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Transactions;

/**
 *
 * @author hafizsjafioedin
 */
public class IdentifyTrx {

    private boolean needNotify;
    private boolean requestGood;
    private OneCheckoutErrorMessage errorMsg;
    private String statusCode;
    private MerchantPaymentChannel mpc;
    private Transactions trans;

    public IdentifyTrx() {
        needNotify = false;
        requestGood = true;
        errorMsg = null;
        statusCode = "";
        trans = null;
    }

    /**
     * @return the needNotify
     */
    public boolean isNeedNotify() {
        return needNotify;
    }

    /**
     * @param needNotify the needNotify to set
     */
    public void setNeedNotify(boolean needNotify) {
        this.needNotify = needNotify;
    }

    /**
     * @return the requestGood
     */
    public boolean isRequestGood() {
        return requestGood;
    }

    /**
     * @param requestGood the requestGood to set
     */
    public void setRequestGood(boolean requestGood) {
        this.requestGood = requestGood;
    }

    /**
     * @return the errorMsg
     */
    public OneCheckoutErrorMessage getErrorMsg() {
        return errorMsg;
    }

    /**
     * @param errorMsg the errorMsg to set
     */
    public void setErrorMsg(OneCheckoutErrorMessage errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * @return the statusCode
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the mpc
     */
    public MerchantPaymentChannel getMpc() {
        return mpc;
    }

    /**
     * @param mpc the mpc to set
     */
    public void setMpc(MerchantPaymentChannel mpc) {
        this.mpc = mpc;
    }

    /**
     * @return the trans
     */
    public Transactions getTrans() {
        return trans;
    }

    /**
     * @param trans the trans to set
     */
    public void setTrans(Transactions trans) {
        this.trans = trans;
    }
}
