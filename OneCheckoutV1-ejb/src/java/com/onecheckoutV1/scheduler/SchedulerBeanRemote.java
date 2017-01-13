/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.scheduler;

import javax.ejb.Remote;

/**
 *
 * @author aditya
 * 
 */

@Remote
public interface SchedulerBeanRemote {

    public Boolean executeMandiriVA();

    public Boolean executeNotify();
}