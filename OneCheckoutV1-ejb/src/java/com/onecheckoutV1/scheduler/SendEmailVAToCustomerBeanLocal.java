/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Transactions;
import javax.ejb.Local;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author hafiz
 */
@Local
public interface SendEmailVAToCustomerBeanLocal {
    public void sendEmailVAInfo(Transactions trans, PropertiesConfiguration config, Merchants merchant, OneCheckoutDataHelper dataHelper);
}
