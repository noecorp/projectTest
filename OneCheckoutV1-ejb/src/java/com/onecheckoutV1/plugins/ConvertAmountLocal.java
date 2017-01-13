/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.plugins;

import com.onecheckoutV1.ejb.interfaces.InterfacePluginPostParsePayment;
import javax.ejb.Local;

/**
 *
 * @author jauhaf
 */
@Local
public interface ConvertAmountLocal extends InterfacePluginPostParsePayment {
    
}
