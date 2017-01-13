/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.RequestHelper;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Transactions;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author diasnurularifin
 */
@Stateless
public class IPGTokenizationBean extends OneCheckoutChannelBase implements IPGTokenizationLocal {
    
    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;

    public <T extends RequestHelper> boolean doTokenizationAuthPayment(T requestHelper) {
        boolean status = false;
        try {
            if (prepareDoTokenizationAuthPaymentRequest(requestHelper)) {
                
                //PERSISTING TRANSACTION
                if(requestHelper.getResultHelper().getTransactions() == null){
                    Transactions trans = oneCheckoutV1QueryHelperBeanLocal.saveTokenizationTransactions(requestHelper);
                    requestHelper.getResultHelper().setTransactions(trans);
                }
                
                //OneCheckoutLogger.log("IPG_PAYMENT_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                String result = "";
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.IPG.MIP.PLUS", null);
                String resultPayment = super.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getTokenizationCorePaymentChannel().getPaymentChannel());
                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {

                    OneCheckoutLogger.log("IPG_PAYMENT_RESPONSE :\n" + resultPayment);
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);

                    result = xml.getString("status") != null ? xml.getString("status").trim() : "";
                    String responseCode = xml.getString("responsecode") != null ? xml.getString("responsecode").trim() : "";
                    String issuerName = xml.getString("IssuerBankName") != null ? xml.getString("IssuerBankName").trim() : "";
                    requestHelper.getResultHelper().setIssuerName(issuerName);
                    if (result.equalsIgnoreCase("3DSECURE")) {
                        String secureData = xml.getString("verifyReason");
                        String[] param3DS = secureData.split("splitField");
                        String[] pareqs = param3DS[0].split("\\|");
                        String[] merchanData = param3DS[1].split("\\|");
                        String[] acsurl = param3DS[2].split("\\|");
                        String[] mpiPassword = param3DS[3].split("\\|");

                        String RedirectBackUrl = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.REDIRECT.ACS.URL", "");
                        requestHelper.setAcsUrl(acsurl[1]);
                        requestHelper.setThreeDSecureMerchantData(merchanData[1]);
                        requestHelper.setThreeDSecureParam(new HashMap<String, String>());
                        requestHelper.getThreeDSecureParam().put(pareqs[0], pareqs[1]);
                        requestHelper.getThreeDSecureParam().put(merchanData[0], merchanData[1]);
                        requestHelper.getThreeDSecureParam().put("TermUrl", RedirectBackUrl + "?" + "title=" + mpiPassword[1]);

                        OneCheckoutLogger.log("ACSURL :" + acsurl);
                        OneCheckoutLogger.log("THREEDSECURPARAM :\n" + requestHelper.getThreeDSecureParam().toString());
                        
                        requestHelper.getResultHelper().getTransactions().setInc3dSecureStatus(requestHelper.getThreeDSecureParam().get("MD"));
                        requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());                    
                 //       em.merge(requestHelper.getResultHelper().getTransactions());
                    }
                    else { //
                        requestHelper.getResultHelper().setTransactions(parseXMLPayment(requestHelper.getResultHelper().getTransactions(), xml));
                   //     em.merge(requestHelper.getResultHelper().getTransactions());
                        
                        if (result.equalsIgnoreCase("Success") && responseCode.equalsIgnoreCase("00")) {
                            requestHelper.getResultHelper().setTokenizationRegistrationStatus(true);
                            String approvalCode = xml.getString("ApprovalCode") != null ? xml.getString("ApprovalCode").trim() : "";
                            requestHelper.getResultHelper().setApprovalCode(approvalCode);
                            requestHelper.getResultHelper().setTokenAuthorizeStatus(true);
                        }    
                    } 
                } else {
                    requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());  
                    requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                //    em.merge(requestHelper.getResultHelper().getTransactions());
                }
                
                em.merge(requestHelper.getResultHelper().getTransactions());
                requestHelper.getResultHelper().setResponseCode(requestHelper.getResultHelper().getTransactions().getDokuResponseCode());
                if (prepareDoTokenizationAuthPaymentResponse(requestHelper)) {
                    if (!result.trim().equalsIgnoreCase("3DSECURE")) {
                        if (doNotifyPayment(requestHelper)) {
                        }
                    }
                    status = true;
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml) {
        try {
            String status = xml.getString("status");
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            if (status!=null && status.equalsIgnoreCase("Success")) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            }
            String responseCode = xml.getString("responsecode");
            String approvalCode = xml.getString("ApprovalCode");
            String issuerBankName = xml.getString("IssuerBankName");
            String verifyStatus = xml.getString("verifyStatus");
            String verifyScore = xml.getString("verifyScore");
            String hostRefNum = xml.getString("HostReferenceNumber");
//            if (responseCode != null && responseCode.length() != 4) {
//                responseCode = "00" + responseCode;
//            }
            
            if (responseCode == null || responseCode.trim().equals("")) {
                    responseCode = "99";
            }
            responseCode = "00" + responseCode;            
            trans.setDokuApprovalCode(approvalCode);
            trans.setDokuIssuerBank(issuerBankName);
            trans.setDokuResponseCode(responseCode);
            trans.setDokuHostRefNum(hostRefNum);
            try {
                if (verifyStatus != null) {
                    OneCheckoutDFSStatus vStatus = OneCheckoutDFSStatus.findType(verifyStatus.trim().charAt(0));
                    if (vStatus!=null) 
                        trans.setVerifyStatus(vStatus.value());
                    else
                        trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    if (verifyScore!=null)
                        trans.setVerifyScore(Integer.parseInt(verifyScore));
                    else
                        trans.setVerifyScore(-1);
                    trans.setVerifyId("");
                } else {
                    trans.setVerifyId("");
                    trans.setVerifyScore(-1);
                    trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                }
            } catch (Throwable t) {
                trans.setVerifyId("");
                trans.setVerifyScore(-1);
                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
            }
         //   trans.setEduStatus(trans.getVerifyStatus());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());
            return trans;
        } catch(Exception ex) {        
            return trans;
        }
    }
    
    public <T extends RequestHelper> boolean prepareDoTokenizationAuthPaymentRequest(T requestHelper) {
        boolean status = false;
        try {
            if (requestHelper.getThreeDSecureHelper() != null && requestHelper.getPaymentParam() != null && requestHelper.getPaymentParam().size() > 0) {
                requestHelper.getPaymentParam().put("FROM", "ACS");
                requestHelper.getPaymentParam().put("title", requestHelper.getThreeDSecureHelper().getTitle());
                requestHelper.getPaymentParam().put("PaRes", requestHelper.getThreeDSecureHelper().getPaRes());
                requestHelper.getPaymentParam().put("MD", requestHelper.getThreeDSecureHelper().getMerchantData());
            } else {
                requestHelper.setPaymentParam(new HashMap<String, String>());
                requestHelper.getPaymentParam().put("TYPE", "IMMEDIATE");
                requestHelper.getPaymentParam().put("MALLID", requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelCode() + "");
                if (requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelChainCode() > 0) {
                    requestHelper.getPaymentParam().put("CHAINNUM", requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelChainCode() + "");
                } else {
                    requestHelper.getPaymentParam().put("CHAINNUM", "NA");
                }
                requestHelper.getPaymentParam().put("MerchantID", requestHelper.getTokenizationCorePaymentChannel().getMerchantPaymentChannelUid());
                requestHelper.getPaymentParam().put("MerchantName", requestHelper.getMerchantActivity().getMerchantIdx().getMerchantName());
                requestHelper.getPaymentParam().put("OrderNumber", requestHelper.getTokenizationHelper().getInvoiceNumber());
                requestHelper.getPaymentParam().put("PurchaseAmt", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getTokenizationHelper().getAmount()));
                requestHelper.getPaymentParam().put("PurchaseCurrency", requestHelper.getTokenizationHelper().getPurchaseCurrency());
                StringBuilder word = new StringBuilder();
                word.append(OneCheckoutVerifyFormatData.sdf.format(requestHelper.getTokenizationHelper().getAmount()));
                word.append(requestHelper.getTokenizationCorePaymentChannel().getMerchantPaymentChannelUid());
                word.append(requestHelper.getTokenizationCorePaymentChannel().getMerchantPaymentChannelHash());
                word.append(requestHelper.getTokenizationHelper().getInvoiceNumber());
                requestHelper.getPaymentParam().put("WORDS", HashWithSHA1.doHashing(word.toString(),null,null));
                requestHelper.getPaymentParam().put("SESSIONID", requestHelper.getTokenizationHelper().getSessionId());
                requestHelper.getPaymentParam().put("CardNumber", requestHelper.getCreditCardHelper().getCardNumber());
                requestHelper.getPaymentParam().put("EXPIRYDATE", requestHelper.getCreditCardHelper().getExpYear() + requestHelper.getCreditCardHelper().getExpMonth());
                requestHelper.getPaymentParam().put("CVV2", requestHelper.getCreditCardHelper().getCvv2());
                requestHelper.getPaymentParam().put("NAME", requestHelper.getCreditCardHelper().getCcName());
                requestHelper.getPaymentParam().put("CONTACTABLE_NAME", requestHelper.getTokenizationHelper().getCustomerName());
                requestHelper.getPaymentParam().put("CITY", requestHelper.getCreditCardHelper().getCcCity());
                requestHelper.getPaymentParam().put("STATE", requestHelper.getCreditCardHelper().getCcRegion());
                requestHelper.getPaymentParam().put("COUNTRY", requestHelper.getCreditCardHelper().getCcCountry());
                requestHelper.getPaymentParam().put("PHONE", requestHelper.getCreditCardHelper().getCcMobilePhone());
                requestHelper.getPaymentParam().put("HOMEPHONE", requestHelper.getCreditCardHelper().getCcHomePhone());
                requestHelper.getPaymentParam().put("OFFICEPHONE", requestHelper.getCreditCardHelper().getCcWorkPhone());
                requestHelper.getPaymentParam().put("EMAIL", requestHelper.getCreditCardHelper().getCcEmail());
                requestHelper.getPaymentParam().put("ZIP_CODE", requestHelper.getCreditCardHelper().getCcZipCode());
                requestHelper.getPaymentParam().put("ADDRESS", requestHelper.getCreditCardHelper().getCcAddress());
                requestHelper.getPaymentParam().put("PurchaseDesc", requestHelper.getTokenizationHelper().getBasket());
            }
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoTokenizationAuthPaymentResponse(T requestHelper) {
        boolean status = false;
        try {
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doTokenizationCustomerRegistration(T requestHelper) {
        boolean status = false;
        try {
            if (prepareDoTokenizationCustomerRegistrationRequest(requestHelper)) {
                //OneCheckoutLogger.log("TOKEN_REGISTRATION_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.URL.Registration", null);
                String resultPayment = super.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {
                    /*
                    <RESPONSE>
                       <RESULTMSG>SUCCESS</RESULTMSG>
                       <MALLID>1030</MALLID>
                       <REQUESTDATETIME>20140326103951</REQUESTDATETIME>
                       <CUSTOMERID>141</CUSTOMERID>
                       <CUSTOMERNAME>OCO_Testing</CUSTOMERNAME>
                       <CUSTOMEREMAIL>anggar@nsiapay.net</CUSTOMEREMAIL>
                       <CUSTOMERADDRESS>Alamat Billing Address</CUSTOMERADDRESS>
                       <CUSTOMERCITY>Kota Billing Address</CUSTOMERCITY>
                       <CUSTOMERSTATE>Propinsi Billing Address</CUSTOMERSTATE>
                       <CUSTOMERCOUNTRY>ID</CUSTOMERCOUNTRY>
                       <CUSTOMERZIPCODE>12345</CUSTOMERZIPCODE>
                       <CUSTOMERMOBILEPHONE>0987654321</CUSTOMERMOBILEPHONE>
                       <CUSTOMERHOMEPHONE>0123456789</CUSTOMERHOMEPHONE>
                       <CUSTOMERWORKPHONE>0123456789</CUSTOMERWORKPHONE>
                       <TOKENS>
                          <TOKEN>
                             <TOKENID>3a15cf8be72b1ed36ffb3ec555ba3c97dc43f475</TOKENID>
                             <CARDNUMBER>5***********1111</CARDNUMBER>
                             <EXPIRYDATE>1505</EXPIRYDATE>
                             <CCNAME>Anggar Keren</CCNAME>
                             <CCEMAIL>anggar@nsiapay.net</CCEMAIL>
                             <CCADDRESS>Alamat Billing Address</CCADDRESS>
                             <CCCITY>Kota Billing Address</CCCITY>
                             <CCSTATE />
                             <CCCOUNTRY>ID</CCCOUNTRY>
                             <CCZIPCODE>12345</CCZIPCODE>
                             <CCMOBILEPHONE />
                             <CCHOMEPHONE>0123456789</CCHOMEPHONE>
                             <CCWORKPHONE>0123456789</CCWORKPHONE>
                             <TOKENSTATUS>ACTIVE</TOKENSTATUS>
                             <BILLS>
                                <BILL>
                                   <BILLNUMBER>1237974</BILLNUMBER>
                                   <AMOUNT>100000.00</AMOUNT>
                                   <PURCHASEAMOUNT>100000.00</PURCHASEAMOUNT>
                                   <CURRENCY>360</CURRENCY>
                                   <PURCHASECURRENCY>360</PURCHASECURRENCY>
                                   <BILLDETAIL>test</BILLDETAIL>
                                   <BILLTYPE>S</BILLTYPE>
                                   <BILLSTATUS>ACTIVE</BILLSTATUS>
                                   <STARTDATE>20140319</STARTDATE>
                                   <ENDDATE>20150319</ENDDATE>
                                   <EXECUTETYPE>DAY</EXECUTETYPE>
                                   <EXECUTEDATE>WED</EXECUTEDATE>
                                   <EXECUTEMONTH>MAR</EXECUTEMONTH>
                                   <FLATSTATUS>true</FLATSTATUS>
                                </BILL>
                             </BILLS>
                          </TOKEN>
                       </TOKENS>
                    </RESPONSE>
                     */

                    //OneCheckoutLogger.log("TOKEN_REGISTRATION_RESPONSE :\n" + resultPayment);
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);

                    String result = xml.getString("RESULTMSG") != null ? xml.getString("RESULTMSG").trim() : "";
                    if (result.equalsIgnoreCase("SUCCESS")) {
                    }
                }

                if (prepareDoTokenizationCustomerRegistrationResponse(requestHelper)) {
                    status = true;
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoTokenizationCustomerRegistrationRequest(T requestHelper) {
        boolean status = false;
        try {
            requestHelper.setPaymentParam(new HashMap<String, String>());
            // MERCHANT INFORMATION
            requestHelper.getPaymentParam().put("MALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "");
            if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
            }
            String requestDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            requestHelper.getPaymentParam().put("REQUESTDATETIME", requestDateTime);
            String words = HashWithSHA1.doHashing(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + requestHelper.getTokenizationHelper().getCustomerId() + requestDateTime,null,null);
            requestHelper.getPaymentParam().put("WORDS", words);

            // CUSTOMER INFORMATION
            requestHelper.getPaymentParam().put("CUSTOMERID", requestHelper.getTokenizationHelper().getCustomerId());
            requestHelper.getPaymentParam().put("CUSTOMERNAME", requestHelper.getTokenizationHelper().getCustomerName());
            requestHelper.getPaymentParam().put("CUSTOMEREMAIL", requestHelper.getTokenizationHelper().getCustomerEmail());
            requestHelper.getPaymentParam().put("CUSTOMERADDRESS", requestHelper.getTokenizationHelper().getCustomerAddress());
            requestHelper.getPaymentParam().put("CUSTOMERCITY", requestHelper.getTokenizationHelper().getCustomerCity());
            requestHelper.getPaymentParam().put("CUSTOMERSTATE", requestHelper.getTokenizationHelper().getCustomerState());
            requestHelper.getPaymentParam().put("CUSTOMERCOUNTRY", requestHelper.getTokenizationHelper().getCustomerCountry());
            requestHelper.getPaymentParam().put("CUSTOMERZIPCODE", requestHelper.getTokenizationHelper().getCustomerZipCode());
            requestHelper.getPaymentParam().put("CUSTOMERMOBILEPHONE", requestHelper.getTokenizationHelper().getCustomerMobilePhone());
            requestHelper.getPaymentParam().put("CUSTOMERHOMEPHONE", requestHelper.getTokenizationHelper().getCustomerHomePhone());
            requestHelper.getPaymentParam().put("CUSTOMERWORKPHONE", requestHelper.getTokenizationHelper().getCustomerWorkPhone());

            // CARD INFORMATION
            requestHelper.getPaymentParam().put("CARDNUMBER", requestHelper.getCreditCardHelper().getCardNumber());
            requestHelper.getPaymentParam().put("EXPIRYDATE", requestHelper.getCreditCardHelper().getExpYear() + requestHelper.getCreditCardHelper().getExpMonth());
            requestHelper.getPaymentParam().put("CCNAME", requestHelper.getCreditCardHelper().getCcName());
            requestHelper.getPaymentParam().put("CCEMAIL", requestHelper.getCreditCardHelper().getCcEmail());
            requestHelper.getPaymentParam().put("CCADDRESS", requestHelper.getCreditCardHelper().getCcAddress());
            requestHelper.getPaymentParam().put("CCCITY", requestHelper.getCreditCardHelper().getCcCity());
            requestHelper.getPaymentParam().put("CCSTATE", requestHelper.getCreditCardHelper().getCcRegion());
            requestHelper.getPaymentParam().put("CCCOUNTRY", requestHelper.getCreditCardHelper().getCcCountry());
            requestHelper.getPaymentParam().put("CCZIPCODE", requestHelper.getCreditCardHelper().getCcZipCode());
            requestHelper.getPaymentParam().put("CCMOBILEPHONE", requestHelper.getCreditCardHelper().getCcMobilePhone());
            requestHelper.getPaymentParam().put("CCHOMEPHONE", requestHelper.getCreditCardHelper().getCcHomePhone());
            requestHelper.getPaymentParam().put("CCWORKPHONE", requestHelper.getCreditCardHelper().getCcWorkPhone());
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoTokenizationCustomerRegistrationResponse(T requestHelper) {
        boolean status = false;
        try {
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doNotifyPayment(T requestHelper) {
        boolean status = false;
        try {
            String verifyStatus = "NA";
            String chainMallId = "NA";
            String url = requestHelper.getMerchantActivity().getMerchantIdx().getMerchantNotifyStatusUrl();
            HashMap<String, String> notifyParam = new HashMap<String, String>();
            notifyParam.put("MALLID", requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "");
            if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                chainMallId = requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantChainMerchantCode() + "";
            }
            notifyParam.put("CHAINMERCHANT", chainMallId);
            notifyParam.put("CURRENCY", requestHelper.getTokenizationHelper().getCurrency());
            notifyParam.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getTokenizationHelper().getAmount()));
            notifyParam.put("TRANSIDMERCHANT", requestHelper.getTokenizationHelper().getInvoiceNumber());
            notifyParam.put("STATUSTYPE", "P");
            notifyParam.put("RESPONSECODE", requestHelper.getResultHelper().getResponseCode());
            notifyParam.put("APPROVALCODE", requestHelper.getResultHelper().getApprovalCode());
            notifyParam.put("RESULTMSG", requestHelper.getResultHelper().isTokenAuthorizeStatus() ? "SUCCESS" : "FAILED");
            notifyParam.put("PAYMENTCHANNEL", "16");
            notifyParam.put("SESSIONID", requestHelper.getTokenizationHelper().getSessionId());
            notifyParam.put("MCN", requestHelper.getCreditCardHelper().getMaskCardNumber());
            notifyParam.put("PAYMENTDATETIME", OneCheckoutVerifyFormatData.datetimeFormat.format(new Date()));
            notifyParam.put("CUSTOMERID", requestHelper.getTokenizationHelper().getCustomerId());
            notifyParam.put("BANK", requestHelper.getResultHelper().getIssuerName());
            notifyParam.put("TOKENID", "");
            notifyParam.put("PAYMENTCODE", "");
            notifyParam.put("VERIFYID", "");
            notifyParam.put("VERIFYSCORE", "-1");
            notifyParam.put("VERIFYSTATUS", verifyStatus);

            // AMOUNT + MALLID + <shared key> + TRANSIDMERCHANT + RESULTMSG + VERIFYSTATUS
            StringBuilder wordsNotify = new StringBuilder();
            wordsNotify.append(OneCheckoutVerifyFormatData.sdf.format(requestHelper.getTokenizationHelper().getAmount()));
            wordsNotify.append(requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "");
            wordsNotify.append(requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantHashPassword());
            wordsNotify.append(requestHelper.getTokenizationHelper().getInvoiceNumber());
            wordsNotify.append(requestHelper.getResultHelper().isTokenAuthorizeStatus() ? "SUCCESS" : "FAILED");
            wordsNotify.append(verifyStatus);
            OneCheckoutLogger.log("WORD in Clear Text : %s", wordsNotify.toString());
            String hashWordsNotify = HashWithSHA1.doHashing(wordsNotify.toString(),requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantShaFormat(),requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantHashPassword());
            notifyParam.put("WORDS", hashWordsNotify);

            OneCheckoutLogger.log("TOKEN_NOTIFY_PAYMENT :\n" + notifyParam.toString());
            String notifyResult = super.postMIP(notifyParam, url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
            OneCheckoutLogger.log("TOKEN_NOTIFY_PAYMENT RESULT[" + notifyResult + "]");
//            if (notifyResult == null || !notifyResult.trim().equalsIgnoreCase("CONTINUE")) {
            if (notifyResult == null || notifyResult.trim().equalsIgnoreCase("STOP")) {
                if (requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantNotifyTimeout() == null || !requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantNotifyTimeout()) {
                    // --------------------------------------
                    // NEED TO DO VOID TRANSACTION
                    // --------------------------------------
                    requestHelper.getResultHelper().setTokenAuthorizeStatus(false);
                    HashMap<String, String> voidparams = new HashMap<String, String>();
                    voidparams.put("MALLID", requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelCode() + "");
                    voidparams.put("OrderNumber", requestHelper.getTokenizationHelper().getInvoiceNumber());
                    voidparams.put("ApprovalCode", requestHelper.getResultHelper().getApprovalCode());
                    voidparams.put("MerchantID", requestHelper.getTokenizationCorePaymentChannel().getMerchantPaymentChannelUid());
                    voidparams.put("PurchaseAmt", "10000.00");
                    voidparams.put("PurchaseMDR", "TEST");
                    //OneCheckoutLogger.log("TOKEN_VOID_REQUEST :\n" + voidparams.toString());
                    String voidUrl = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.IPG.VOID", null);
                    String resultVoid = super.postMIP(voidparams, voidUrl, requestHelper.getTokenizationCorePaymentChannel().getPaymentChannel());
                    OneCheckoutLogger.log("TOKEN_VOID_RESPONSE :\n" + resultVoid);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }
}
