/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import javax.xml.xpath.XPathConstants;

/**
 *
 * @author hafizsjafioedin
 */
public class DOKUVoidResponseXML {
    
   private static final String ROOT = "/voidResponse";
   private static final String ONE_STATUS = "/status";
   private static final String ONE_RESPONSE_CODE = "/responsecode";
   private static final String ONE_APPROVAL_CODE = "/ApprovalCode";
   
   public DOKUVoidResponseXML() {
       
   }

   public static OneCheckoutVoidRequest parseResponseXML(String xml, OneCheckoutVoidRequest voidRequest) {

       try {

//            xml = xml.replace("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>", "");
           OneCheckoutXPathReader xPathReader = new OneCheckoutXPathReader(xml);


           String status = (String) xPathReader.query(ROOT + ONE_STATUS, XPathConstants.STRING);
           voidRequest.setVoidStatus(status);
           
           String responseCode = (String) xPathReader.query(ROOT + ONE_RESPONSE_CODE, XPathConstants.STRING);
           voidRequest.setResponseCode(responseCode);
           
           String approvalCode = (String) xPathReader.query(ROOT + ONE_APPROVAL_CODE, XPathConstants.STRING);
           voidRequest.setApprovalCode(approvalCode);
           
           OneCheckoutLogger.log("Status : %s, responseCode : %s, approvalCode : %s ", status,responseCode,approvalCode);
           
           return voidRequest;
       }catch(Exception iv){
           iv.printStackTrace();
           voidRequest.setVoidStatus("PROBLEM");
           return voidRequest;
       }
   }   
       
}
