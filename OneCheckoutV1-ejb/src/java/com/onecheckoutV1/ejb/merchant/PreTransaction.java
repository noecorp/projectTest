/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.merchant;

import com.onecheckoutV1.data.OneCheckoutDataHelper;

/**
 *
 * @author aditya
 */
public interface PreTransaction {

    public <T extends OneCheckoutDataHelper> boolean beforeTrans(T request);

}
