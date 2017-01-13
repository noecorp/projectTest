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
public class MPGVoidReponseXML {

    private static final String ROOT = "/MPGVoidResponse";
    private static final String ONE_MALLID = "/mallId";
    private static final String ONE_CHAINMALLID = "/chainMallId";
    private static final String ONE_TRXCODE = "/trxCode";
    private static final String ONE_CARDNUMBER = "/cardNumber";
    private static final String ONE_INVOICENUMBER = "/invoiceNumber";
    private static final String ONE_AMOUNT = "/amount";
    private static final String ONE_CURRENCY = "/currency";
    private static final String ONE_RESULT = "/result";
    private static final String ONE_ERRORCODE = "/errorCode";
    private static final String ONE_RESPONSECODE = "/responseCode";
    private static final String ONE_APPROVALCODE = "/approvalCode";
    private static final String ONE_MESSAGE = "/message";
    private static final String ONE_BANK = "/bank";
    private static final String ONE_MID = "/mid";
    private static final String ONE_TID = "/tid";

    public MPGVoidReponseXML() {
    }

    public static OneCheckoutVoidRequest parseResponseXML(String xml, OneCheckoutVoidRequest voidRequest) {

        try {

            OneCheckoutXPathReader xPathReader = new OneCheckoutXPathReader(xml);
            if (xPathReader.query(ROOT + ONE_MALLID, XPathConstants.NUMBER) != null) {
                String mallId = (String) xPathReader.query(ROOT + ONE_MALLID, XPathConstants.STRING);
                if (mallId != null && !mallId.trim().endsWith("")) {
                    voidRequest.setMALLID(Integer.valueOf(mallId.trim()));
                }
            }

            if (xPathReader.query(ROOT + ONE_CHAINMALLID, XPathConstants.NUMBER) != null) {
                String chainMallId = (String) xPathReader.query(ROOT + ONE_CHAINMALLID, XPathConstants.STRING);
                if (chainMallId != null && !chainMallId.trim().endsWith("")) {
                    voidRequest.setCHAINMERCHANT(Integer.valueOf(chainMallId.trim()));
                }
            }

            String trxCode = (String) xPathReader.query(ROOT + ONE_TRXCODE, XPathConstants.STRING);
            voidRequest.setTRXCODE(trxCode);

            String cardNumber = (String) xPathReader.query(ROOT + ONE_CARDNUMBER, XPathConstants.STRING);
            voidRequest.setCARDNUMBER(cardNumber);

            String transIdMerchant = (String) xPathReader.query(ROOT + ONE_INVOICENUMBER, XPathConstants.STRING);
            voidRequest.setTRANSIDMERCHANT(transIdMerchant);

            String amount = (String) xPathReader.query(ROOT + ONE_AMOUNT, XPathConstants.STRING);
            voidRequest.setAmount(amount);

            String currency = (String) xPathReader.query(ROOT + ONE_CURRENCY, XPathConstants.STRING);
            voidRequest.setCURRENCY(currency);

            String result = (String) xPathReader.query(ROOT + ONE_RESULT, XPathConstants.STRING);
            voidRequest.setVoidStatus(result);

            String errorCode = (String) xPathReader.query(ROOT + ONE_ERRORCODE, XPathConstants.STRING);
            voidRequest.setStatusCode(errorCode);

            String responseCode = (String) xPathReader.query(ROOT + ONE_RESPONSECODE, XPathConstants.STRING);
            voidRequest.setResponseCode(responseCode);

            String approvalCode = (String) xPathReader.query(ROOT + ONE_APPROVALCODE, XPathConstants.STRING);
            voidRequest.setApprovalCode(approvalCode);

            String message = (String) xPathReader.query(ROOT + ONE_MESSAGE, XPathConstants.STRING);
            voidRequest.setMESSAGE(message);

            String bank = (String) xPathReader.query(ROOT + ONE_BANK, XPathConstants.STRING);
            voidRequest.setBANK(bank);

            String mid = (String) xPathReader.query(ROOT + ONE_MID, XPathConstants.STRING);
            voidRequest.setMID(mid);

            String tid = (String) xPathReader.query(ROOT + ONE_TID, XPathConstants.STRING);
            voidRequest.setTID(tid);

            OneCheckoutLogger.log("Status : %s, responseCode : %s, approvalCode : %s ", result, responseCode, approvalCode);

            /*
             <MPGVoidResponse>
             <mallId>8</mallId>
             <trxCode>b5cf26ca3c5195346ebeb030c53fbc62e97dcb89</trxCode>
             <cardNumber>5***********0940</cardNumber>
             <invoiceNumber>1386744018840</invoiceNumber>
             <amount>10000.00</amount>
             <currency>IDR</currency>
             <result>SUCCESS</result>
             <responseCode>00</responseCode>
             <approvalCode>568975</approvalCode>
             <message>PAYMENT HAS BEEN VOID</message>
             <bank>MANDIRI</bank>
             <mid>000000020020022</mid>
             <tid>20020022</tid>
             </MPGVoidResponse>
             */
            return voidRequest;
        } catch (Exception iv) {
            iv.printStackTrace();
            voidRequest.setVoidStatus("PROBLEM");
            return voidRequest;
        }
    }
}
