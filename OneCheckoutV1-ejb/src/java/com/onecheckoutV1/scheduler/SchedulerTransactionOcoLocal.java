/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import javax.ejb.Local;

/**
 *
 * @author iskandar
 */
@Local
public interface SchedulerTransactionOcoLocal {
    public void startSchedulerDaily();
}
