/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.interfaces;

import com.onecheckoutV1.data.OneCheckoutDataHelper;

/**
 *
 * @author jauhaf
 */
public interface InterfacePluginPostParsePayment {

    public <T extends OneCheckoutDataHelper> boolean afterAuth(T helper);
}
