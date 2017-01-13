/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.merchant;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import javax.ejb.Stateless;

/**
 *
 * @author hafiz
 */
@Stateless
public class OneCheckoutV1MerchantDefaultBean implements OneCheckoutV1MerchantDefaultLocal {

    public OneCheckoutNotifyStatusResponse sendNotifyChannel(OneCheckoutDataHelper trxHelper) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method" or "Web Service > Add Operation")
 
}
