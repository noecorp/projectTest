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
public interface MPGRecurLocal {

    public <T extends RequestHelper> boolean doRecurAuthPayment(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoRecurAuthPaymentRequest(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoRecurAuthPaymentResponse(T requestHelper);

    public <T extends RequestHelper> boolean doRecurCustomerRegistration(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoRecurCustomerRegistrationRequest(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoRecurCustomerRegistrationResponse(T requestHelper);

    public <T extends RequestHelper> boolean doRecurCustomerUpdate(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoRecurCustomerUpdateRequest(T requestHelper);

    public <T extends RequestHelper> boolean prepareDoRecurCustomerUpdateResponse(T requestHelper);

    public <T extends RequestHelper> boolean doNotify(T requestHelper);
}
