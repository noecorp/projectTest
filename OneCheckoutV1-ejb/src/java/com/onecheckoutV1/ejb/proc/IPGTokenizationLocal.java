/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.ejb.helper.RequestHelper;
import javax.ejb.Local;

/**
 *
 * @author Opiks
 */
@Local
public interface IPGTokenizationLocal {

    public <T extends RequestHelper> boolean doTokenizationAuthPayment(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoTokenizationAuthPaymentRequest(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoTokenizationAuthPaymentResponse(T requestHelper);

    public <T extends RequestHelper> boolean doTokenizationCustomerRegistration(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoTokenizationCustomerRegistrationRequest(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoTokenizationCustomerRegistrationResponse(T requestHelper);

}
