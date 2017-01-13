/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import javax.ejb.Local;

/**
 *
 * @author hafiz
 */
@Local
public interface SchedulerNotifyToMerchantV2BeanLocal {

    public String getTransactionList(int reminder, int quotient);

    public void doNotify(String transactionId);
    
}
