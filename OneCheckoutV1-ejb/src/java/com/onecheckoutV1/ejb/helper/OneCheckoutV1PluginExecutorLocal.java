/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import javax.ejb.Local;

/**
 *
 * @author aditya
 */
@Local
public interface OneCheckoutV1PluginExecutorLocal {

    public <T extends OneCheckoutDataHelper> boolean validationMerchantPlugins(T request);
}
