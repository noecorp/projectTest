/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.util;

import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import javax.xml.xpath.XPathConstants;

/**
 *
 * @author aditya
 */
public class MandiriClickPayResponseXML {
    
   private static final String ROOT = "/voidResponse";

   private static final String ONE_ORDER_NUMBER = "/OrderNumber";
   private static final String ONE_STATUS = "/status";
   private static final String ONE_RESPONSE_CODE = "/responsecode";
   private static final String ONE_APPROVAL_CODE = "/ApprovalCode";
   private static final String ONE_AMOUNT = "/Amount";
   private static final String ONE_AMOUNT_MDR = "/AmountMDR";
   private static final String ONE_STATUS_CODE = "/StatusCode";

   public MandiriClickPayResponseXML() {

   }

   public static OneCheckoutVoidRequest parseResponseXML(String xml, OneCheckoutVoidRequest voidRequest) {

       try {

           // xml = xml.replace("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>", "");
           OneCheckoutXPathReader xPathReader = new OneCheckoutXPathReader(xml);

           String orderNumber = (String) xPathReader.query(ROOT + ONE_ORDER_NUMBER, XPathConstants.STRING);
           voidRequest.setOrderNumber(orderNumber);

           String status = (String) xPathReader.query(ROOT + ONE_STATUS, XPathConstants.STRING);
           voidRequest.setVoidStatus(status);

           String responseCode = (String) xPathReader.query(ROOT + ONE_RESPONSE_CODE, XPathConstants.STRING);
           voidRequest.setResponseCode(responseCode);

           String approvalCode = (String) xPathReader.query(ROOT + ONE_APPROVAL_CODE, XPathConstants.STRING);
           voidRequest.setApprovalCode(approvalCode);

           String amount = (String) xPathReader.query(ROOT + ONE_AMOUNT, XPathConstants.STRING);
           voidRequest.setAmount(amount);

           String amountMDR = (String) xPathReader.query(ROOT + ONE_AMOUNT_MDR, XPathConstants.STRING);
           voidRequest.setAmountMDR(amountMDR);

           String statusCode = (String) xPathReader.query(ROOT + ONE_STATUS_CODE, XPathConstants.STRING);
           voidRequest.setStatusCode(statusCode);

           OneCheckoutLogger.log("Status : %s, responseCode : %s, approvalCode : %s ", status,responseCode,approvalCode);

           return voidRequest;
       }catch(Exception iv){
           iv.printStackTrace();
           voidRequest.setVoidStatus("PROBLEM");
           return voidRequest;
       }
   }

}