/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.sa;

import java.util.Date;
import javax.ejb.Local;

/**
 *
 * @author ahmadfirdaus
 */
@Local
public interface TransactionSandboxReportBeanLocal {
    public String getTransaction(int merchantCode, Date day);
}
