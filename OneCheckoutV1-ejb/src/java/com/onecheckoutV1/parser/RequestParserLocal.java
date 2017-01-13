/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.parser;

import com.onecheckoutV1.ejb.helper.RequestHelper;
import javax.ejb.Local;

/**
 *
 * @author Opiks
 */
@Local
public interface RequestParserLocal {

    public <T extends RequestHelper> boolean doParsingRecurRegistrationCustomerRequest(T requestHelper);

    public <T extends RequestHelper> boolean doParsingRecurRegistrationCustomerData(T requestHelper);

    public <T extends RequestHelper> boolean doParsingThreeDSecure(T requestHelper);

    public <T extends RequestHelper> boolean doParsingRecurUpdateCustomerRequest(T requestHelper);

    public <T extends RequestHelper> boolean doParsingRecurUpdateCustomerData(T requestHelper);
    
    public <T extends RequestHelper> boolean doParsingTokenPaymentRequest(T requestHelper);

    public <T extends RequestHelper> boolean doParsingTokenPaymentData(T requestHelper);
    
    public <T extends RequestHelper> boolean doParsingTokenDeleteData(T requestHelper);
}
