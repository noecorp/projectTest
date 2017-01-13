/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.merchant;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;

/**
 *
 * @author hafiz
 */
public interface OneCheckoutV1MerchantProcessor {


    public OneCheckoutNotifyStatusResponse sendNotifyChannel(OneCheckoutDataHelper trxHelper);// String data, String url, HashMap<String, String> params, Merchants merchant);



}
