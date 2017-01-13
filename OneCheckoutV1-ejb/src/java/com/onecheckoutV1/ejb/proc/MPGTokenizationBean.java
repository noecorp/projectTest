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
import com.onechekoutv1.dto.Country;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.Transactions;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author Opiks
 */
@Stateless
public class MPGTokenizationBean extends OneCheckoutChannelBase  implements MPGTokenizationLocal {

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
                
                //OneCheckoutLogger.log("MPG_PAYMENT_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                String result = "";
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.MPG.MIP.PLUS", null);
                if (requestHelper.getThreeDSecureHelper() != null && requestHelper.getThreeDSecureHelper().isParseStatus()) {
                    url = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.MPG.MIP.THREEDSECURE", null);
                }
                String resultPayment = super.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getTokenizationCorePaymentChannel().getPaymentChannel());
                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {

                    OneCheckoutLogger.log("MPG_PAYMENT_RESPONSE :\n" + resultPayment);
                    XMLConfiguration xml = null;
                    try {
                        xml = new XMLConfiguration();
                        StringReader sr = new StringReader(resultPayment);
                        xml.load(sr);
                    } catch (Throwable th) {
                    }
                    if (xml != null) {
                        result = xml.getString("result") != null ? xml.getString("result").trim() : "";
                        String responseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
                        String transactionCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
                        String acquirerBankName = xml.getString("bank");
                        String issuerBankName = xml.getString("issuerBank");
                        String verifyStatus = xml.getString("fraudScreeningStatus");
                        String verifyScore = xml.getString("fraudScreeningScore");
                        String verifyReason = xml.getString("fraudScreeningReason");
                        requestHelper.getResultHelper().setTransactionCode(transactionCode);
                        if (result.equalsIgnoreCase("THREEDSECURE")) {
                            String paReq =  xml.getString("paReq");
                            String MD =  xml.getString("md");
                            String acsUrl =  xml.getString("acsUrl");

                            String RedirectBackUrl = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.REDIRECT.ACS.URL", "");
                            requestHelper.setAcsUrl(acsUrl);
                            requestHelper.setThreeDSecureMerchantData(MD);
                            requestHelper.setThreeDSecureParam(new HashMap<String, String>());
                            requestHelper.getThreeDSecureParam().put("PaReq", paReq);
                            requestHelper.getThreeDSecureParam().put("MD", MD);
                            requestHelper.getThreeDSecureParam().put("TermUrl", RedirectBackUrl);

                            OneCheckoutLogger.log("ACSURL :" + acsUrl);
                            OneCheckoutLogger.log("THREEDSECURPARAM :\n" + requestHelper.getThreeDSecureParam().toString());

                            requestHelper.getResultHelper().getTransactions().setInc3dSecureStatus(requestHelper.getThreeDSecureParam().get("MD"));
                            requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());
                            em.merge(requestHelper.getResultHelper().getTransactions());
                        } else {
                            requestHelper.getResultHelper().setIssuerName(issuerBankName);
                            requestHelper.getResultHelper().setTransactions(parseXMLPayment(requestHelper.getResultHelper().getTransactions(), xml));
                            if (result.equalsIgnoreCase("SUCCESS") && responseCode.equalsIgnoreCase("00")) {
                                requestHelper.getResultHelper().setTokenizationRegistrationStatus(true);
                                String approvalCode = xml.getString("approvalCode");
                                requestHelper.getResultHelper().setApprovalCode(approvalCode);
                                requestHelper.getResultHelper().setTokenAuthorizeStatus(true);
                            }                             
                            
                      //      requestHelper.getResultHelper().getTransactions().setDokuResponseCode("00" + responseCode);
                   //         requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                 //           requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                            em.merge(requestHelper.getResultHelper().getTransactions());
                        }
                    } else {
                        requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE.value());
                        requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                        em.merge(requestHelper.getResultHelper().getTransactions());
                    }
                } else {
                    requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());  
                    requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                    em.merge(requestHelper.getResultHelper().getTransactions());
                }

                em.merge(requestHelper.getResultHelper().getTransactions());
                requestHelper.getResultHelper().setResponseCode(requestHelper.getResultHelper().getTransactions().getDokuResponseCode());                
                if (prepareDoTokenizationAuthPaymentResponse(requestHelper)) {
                    if (!result.trim().equalsIgnoreCase("THREEDSECURE")) {
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
            String result = xml.getString("result") != null ? xml.getString("result").trim() : "";
            String responseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
            String approvalCode = xml.getString("approvalCode") != null ? xml.getString("approvalCode").trim() : "";
            String issuerBank = xml.getString("issuerBank") != null ? xml.getString("issuerBank").trim() : "";
            String verifyId = xml.getString("fraudScreeningId") != null ? xml.getString("fraudScreeningId").trim() : "";
            String verifyStatus = xml.getString("fraudScreeningStatus") != null ? xml.getString("fraudScreeningStatus").trim() : "NA";
            String verifyScore = xml.getString("fraudScreeningScore") != null ? xml.getString("fraudScreeningScore").trim() : "-1";
            //String verifyReason = xml.getString("fraudScreeningReason") != null ? xml.getString("fraudScreeningReason").trim() : "";
            String hostRefNum = xml.getString("hostReferenceNumber") != null ? xml.getString("hostReferenceNumber").trim() : "";
            String bank = xml.getString("bank") != null ? xml.getString("bank").trim() : "";
            //String maskedCard = xml.getString("cardNumber") != null ? xml.getString("cardNumber").trim() : "";
            //String paymentDateTime = xml.getString("paymentDate") != null ? xml.getString("paymentDate").trim() : "";
            String trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
            String errorCode = xml.getString("errorCode") != null ? xml.getString("errorCode").trim() : "";
            String liability = xml.getString("liability") != null ? xml.getString("liability").trim() : "";
            String threeDSecureStatus = xml.getString("threeDSecureStatus") != null ? xml.getString("threeDSecureStatus").trim() : "";
            //String errorCode = xml.getString("errorCode") != null ? xml.getString("errorCode").trim() : "";
                        
            trans.setInc3dSecureStatus(threeDSecureStatus);
            trans.setIncLiability(liability);
            
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            if (result!=null && result.equalsIgnoreCase("Success")) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            }                     
           // if (responseCode != null && responseCode.length() != 4) {
            if (responseCode.length() != 4) {
                if (errorCode != null) {
                    if (errorCode.trim().equalsIgnoreCase("XX07")) {
                        responseCode = "BB";
                    }
                }
                responseCode = "00" + responseCode;
            }
                        
            trans.setDokuApprovalCode(approvalCode);
            trans.setDokuIssuerBank(issuerBank);
            trans.setDokuResponseCode(responseCode);
            trans.setDokuHostRefNum(hostRefNum);
            trans.setDokuVoidApprovalCode(trxCode);
            trans.setDokuIssuerBank(bank);
            try {
                if (verifyStatus != null) {
                    OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.valueOf(verifyStatus);
                    trans.setVerifyId(verifyId);
                    trans.setVerifyScore(Integer.parseInt(verifyScore));
                    trans.setVerifyStatus(dfsStatus.value());
             //       trans.setEduStatus(dfsStatus.value());
                } else {
                    trans.setVerifyId("");
                    trans.setVerifyScore(-1);
                    trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
               //     trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                }
            } catch (Throwable t) {
                trans.setVerifyScore(0);
            }
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());
            em.merge(trans);
            return trans;
        } catch(Exception ex) {
            return trans;
        }
    }
    
    public <T extends RequestHelper> boolean prepareDoTokenizationAuthPaymentRequest(T requestHelper) {
        boolean status = false;
        try {
            if (requestHelper.getThreeDSecureHelper() != null && requestHelper.getPaymentParam() != null && requestHelper.getPaymentParam().size() > 0) {
                requestHelper.setPaymentParam(new HashMap<String, String>());
                requestHelper.getPaymentParam().put("PaRes", requestHelper.getThreeDSecureHelper().getPaRes());
                requestHelper.getPaymentParam().put("MD", requestHelper.getThreeDSecureHelper().getMerchantData());
            } else {
                String curr = "IDR";
                Currency currency = oneCheckoutV1QueryHelperBeanLocal.getCurrencyByCode(requestHelper.getTokenizationHelper().getPurchaseCurrency());
                if (currency != null) {
                    curr = currency.getAlpha3Code();
                }
                
                StringBuilder word = new StringBuilder();
                word.append(OneCheckoutVerifyFormatData.sdf.format(requestHelper.getTokenizationHelper().getAmount()));
                word.append(requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelCode());
                word.append(requestHelper.getTokenizationHelper().getInvoiceNumber());
                word.append(requestHelper.getTokenizationCorePaymentChannel().getMerchantPaymentChannelHash());
                word.append(curr);
                OneCheckoutLogger.log("WORD in Clear Text : %s", word.toString());
                String hashwords = HashWithSHA1.doHashing(word.toString(),null,null);

                String ccCountry = "ID";
                if (ccCountry != null && ccCountry.length() > 2) {
                    OneCheckoutLogger.log("Checking country code[" + ccCountry + "] to database...");
                    Country country = oneCheckoutV1QueryHelperBeanLocal.getCountryByNumericCode(ccCountry);
                    if (country != null) {
                        ccCountry = country.getId();
                    } else {
                        OneCheckoutLogger.log("Checking country code[" + ccCountry + "] NOT FOUND...");
                    }
                }

                requestHelper.setPaymentParam(new HashMap<String, String>());
                requestHelper.getPaymentParam().put("SERVICEID", requestHelper.getTokenizationCorePaymentChannel().getPaymentChannel().getServiceId());
                requestHelper.getPaymentParam().put("MALLID", requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelCode() + "");
                if (requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelChainCode() > 0) {
                    requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getTokenizationCorePaymentChannel().getPaymentChannelChainCode() + "");
                }
                requestHelper.getPaymentParam().put("INVOICENUMBER", requestHelper.getTokenizationHelper().getInvoiceNumber());
                requestHelper.getPaymentParam().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getTokenizationHelper().getAmount()));
                requestHelper.getPaymentParam().put("CURRENCY", curr);
                requestHelper.getPaymentParam().put("WORDS", hashwords);
                requestHelper.getPaymentParam().put("SESSIONID", requestHelper.getTokenizationHelper().getSessionId());
                requestHelper.getPaymentParam().put("BASKET", requestHelper.getTokenizationHelper().getBasket());
                
                requestHelper.getPaymentParam().put("CARDNUMBER", requestHelper.getCreditCardHelper().getCardNumber());
                requestHelper.getPaymentParam().put("EXPIRYMONTH", requestHelper.getCreditCardHelper().getExpMonth());
                requestHelper.getPaymentParam().put("EXPIRYYEAR", requestHelper.getCreditCardHelper().getExpYear());
                requestHelper.getPaymentParam().put("CVV2", requestHelper.getCreditCardHelper().getCvv2());
                
                requestHelper.getPaymentParam().put("CCNAME", requestHelper.getCreditCardHelper().getCcName());
                requestHelper.getPaymentParam().put("CCEMAIL", requestHelper.getCreditCardHelper().getCcEmail());
                requestHelper.getPaymentParam().put("CCCITY", requestHelper.getCreditCardHelper().getCcCity());
                requestHelper.getPaymentParam().put("CCREGION", requestHelper.getCreditCardHelper().getCcRegion());
                requestHelper.getPaymentParam().put("CCCOUNTRY", ccCountry);
                requestHelper.getPaymentParam().put("CCPHONE", requestHelper.getCreditCardHelper().getCcMobilePhone());
                requestHelper.getPaymentParam().put("CCZIPCODE", requestHelper.getCreditCardHelper().getCcZipCode());
                requestHelper.getPaymentParam().put("BILLINGADDRESS", requestHelper.getCreditCardHelper().getCcAddress());
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

            //AMOUNT + MALLID + <shared key> + TRANSIDMERCHANT + RESULTMSG + VERIFYSTATUS
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
//        if (notifyResult == null || (!notifyResult.trim().equalsIgnoreCase("CONTINUE") && notifyResult.trim().equalsIgnoreCase("STOP"))) {
        if (notifyResult == null || notifyResult.trim().equalsIgnoreCase("STOP")) {
                if (requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantNotifyTimeout() == null || !requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantNotifyTimeout()) {
                    // --------------------------------------
                    // NEED TO DO VOID TRANSACTION
                    // --------------------------------------
                    requestHelper.getResultHelper().setTokenAuthorizeStatus(false);
                    StringBuilder word = new StringBuilder();
                    word.append(requestHelper.getResultHelper().getTransactionCode());
                    word.append(requestHelper.getTokenizationCorePaymentChannel().getMerchantPaymentChannelHash());
                    word.append(requestHelper.getResultHelper().getApprovalCode());
                    String hashwords = HashWithSHA1.doHashing(word.toString(),null,null);
                    String urlVoid = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.MPG.VOID", null);
                    
                    HashMap<String, String> voidParam = new HashMap<String, String>();
                    voidParam.put("SERVICEID", requestHelper.getTokenizationCorePaymentChannel().getPaymentChannel().getServiceId());
                    voidParam.put("TRXCODE", requestHelper.getResultHelper().getTransactionCode());
                    voidParam.put("PAYMENTAPPROVALCODE", requestHelper.getResultHelper().getApprovalCode());
                    voidParam.put("WORDS", hashwords);

                    OneCheckoutLogger.log("TOKEN_VOID_REQUEST :\n" + voidParam.toString());
                    String voidResult = super.postMIP(voidParam, urlVoid, requestHelper.getTokenizationCorePaymentChannel().getPaymentChannel());
                    OneCheckoutLogger.log("TOKEN_VOID_RESPONSE :\n" + voidResult);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }
 
}
