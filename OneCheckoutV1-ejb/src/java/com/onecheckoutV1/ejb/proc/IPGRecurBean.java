/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.RecurHelper;
import com.onecheckoutV1.ejb.helper.RequestHelper;
import com.onecheckoutV1.ejb.util.DOKUVoidResponseXML;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.PaymentChannel;
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
public class IPGRecurBean extends OneCheckoutChannelBase implements IPGRecurLocal {

    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;

    @EJB
    private OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBeanLocal oneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBeanLocal; 
    
    public <T extends RequestHelper> boolean doRecurAuthPayment(T requestHelper) {
        boolean status = false;
        try {
            if (prepareDoRecurAuthPaymentRequest(requestHelper)) {

                //PERSISTING TRANSACTION
                if (requestHelper.getResultHelper().getTransactions() == null) {
                    Transactions trans = oneCheckoutV1QueryHelperBeanLocal.saveRecurTransactions(requestHelper);
                    requestHelper.getResultHelper().setTransactions(trans);
                }

                //OneCheckoutLogger.log("IPG_PAYMENT_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                String result = "";
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.IPG.MIP.PLUS", null);
                String resultPayment = super.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getRecurCorePaymentChannel().getPaymentChannel());
                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {
                    /*
                     <orderResponse>
                     <order-number>1401779925335</order-number>
                     <purchase-amount>10000.00</purchase-amount>
                     <responsecode>00</responsecode>
                     <status>Success</status>
                     <ApprovalCode>134803</ApprovalCode>
                     <HostReferenceNumber>920644467924</HostReferenceNumber>
                     <StatusCode/>
                     <installment>false</installment>
                     <OnUs>false</OnUs>
                     <IssuerBankName/>
                     <AcquirerCode>100</AcquirerCode>
                     <mallId>4140</mallId>
                     <verifyId>THJ201406030208</verifyId>
                     <verifyScore>50</verifyScore>
                     <verifyStatus>REVIEW</verifyStatus>
                     <systraceNumber>000415</systraceNumber>
                     <batchNumber>2</batchNumber>
                     </orderResponse>
                     */

                    OneCheckoutLogger.log("IPG_PAYMENT_RESPONSE :\n" + resultPayment);
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);
                    requestHelper.getResultHelper().setTransactions(parseXMLPayment(requestHelper.getResultHelper().getTransactions(), xml));

                    result = xml.getString("status") != null ? xml.getString("status").trim() : "";
                    String responseCode = xml.getString("responsecode") != null ? xml.getString("responsecode").trim() : "";
                    if (result.equalsIgnoreCase("Success") && responseCode.equalsIgnoreCase("00")) {
                        String approvalCode = xml.getString("ApprovalCode");
                        if (requestHelper.getThreeDSecureHelper() != null && requestHelper.getThreeDSecureHelper().isParseStatus()) {
                            requestHelper.getResultHelper().setRecurAuthorizeStatus(true);
                        } else {
                            // --------------------------------------
                            // NEED TO DO VOID TRANSACTION
                            // --------------------------------------
                            String voidUrl = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.IPG.VOID", null);
                            HashMap<String, String> voidparams = new HashMap<String, String>();
                            voidparams.put("MALLID", requestHelper.getRecurCorePaymentChannel().getPaymentChannelCode() + "");
                            voidparams.put("OrderNumber", requestHelper.getRecurHelper().getInvoiceNumber());
                            voidparams.put("ApprovalCode", approvalCode);
                            voidparams.put("MerchantID", requestHelper.getRecurCorePaymentChannel().getMerchantPaymentChannelUid());

                            if (requestHelper.getRecurHelper().getBillingRegisterAmount() != 0) {
                                voidparams.put("PurchaseAmt", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getRecurHelper().getBillingRegisterAmount()));
                            } else {
                                voidparams.put("PurchaseAmt", "10000.00");
                            }
                            voidparams.put("PurchaseMDR", "TEST");
                            //OneCheckoutLogger.log("IPG_VOID_REQUEST :\n" + voidparams.toString());
                            String resultVoid = super.postMIP(voidparams, voidUrl, requestHelper.getRecurCorePaymentChannel().getPaymentChannel());
                            OneCheckoutLogger.log("IPG_VOID_RESPONSE :\n" + resultVoid);

                            OneCheckoutVoidRequest oneCheckoutVoidRequest = new OneCheckoutVoidRequest(requestHelper.getRecurCorePaymentChannel().getPaymentChannelCode() + "");
                            oneCheckoutVoidRequest = DOKUVoidResponseXML.parseResponseXML(resultVoid, oneCheckoutVoidRequest);
                            OneCheckoutLogger.log("Void Status : %s", oneCheckoutVoidRequest.getVoidStatus());
                            if (oneCheckoutVoidRequest.getVoidStatus().equalsIgnoreCase("VOIDED")) {
                                requestHelper.getResultHelper().getTransactions().setDokuVoidApprovalCode(oneCheckoutVoidRequest.getApprovalCode());
                                requestHelper.getResultHelper().getTransactions().setDokuVoidDatetime(new Date());
                                requestHelper.getResultHelper().getTransactions().setDokuVoidResponseCode(oneCheckoutVoidRequest.getResponseCode());
                                requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                                requestHelper.getResultHelper().getTransactions().setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                            } else {
                                requestHelper.getResultHelper().getTransactions().setDokuVoidResponseCode(oneCheckoutVoidRequest.getResponseCode());
                                requestHelper.getResultHelper().getTransactions().setDokuVoidDatetime(new Date());
                            }
                            em.merge(requestHelper.getResultHelper().getTransactions());
                        }
                    } else if (result.equalsIgnoreCase("3DSECURE")) {
                        String secureData = xml.getString("verifyReason");
                        String[] param3DS = secureData.split("splitField");
                        String[] pareqs = param3DS[0].split("\\|");
                        String[] merchanData = param3DS[1].split("\\|");
                        String[] acsurl = param3DS[2].split("\\|");
                        String[] mpiPassword = param3DS[3].split("\\|");

                        String RedirectBackUrl = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.REDIRECT.ACS.URL", "");
                        requestHelper.setAcsUrl(acsurl[1]);
                        requestHelper.setThreeDSecureMerchantData(merchanData[1]);
                        requestHelper.setThreeDSecureParam(new HashMap<String, String>());
                        requestHelper.getThreeDSecureParam().put(pareqs[0], pareqs[1]);
                        requestHelper.getThreeDSecureParam().put(merchanData[0], merchanData[1]);
                        requestHelper.getThreeDSecureParam().put("TermUrl", RedirectBackUrl + "?" + "title=" + mpiPassword[1]);

                        OneCheckoutLogger.log("ACSURL :" + acsurl);
                        OneCheckoutLogger.log("THREEDSECURPARAM :\n" + requestHelper.getThreeDSecureParam().toString());

                        requestHelper.getResultHelper().getTransactions().setInc3dSecureStatus(xml.getString("md"));
                        requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());
                        em.merge(requestHelper.getResultHelper().getTransactions());
                    } else {
                        requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE.value());
                        requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                        em.merge(requestHelper.getResultHelper().getTransactions());
                        doNotify(requestHelper);
                    }
                    String issuerBankName = xml.getString("IssuerBankName");
                    String verifyStatus = xml.getString("verifyStatus");
                    String verifyScore = xml.getString("verifyScore");
                    String verifyReason = xml.getString("verifyReason");
                    String hostRefNum = xml.getString("HostReferenceNumber");
                } else {
                    requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE.value());
                    requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                    em.merge(requestHelper.getResultHelper().getTransactions());
                    doNotify(requestHelper);
                }

                if (prepareDoRecurAuthPaymentResponse(requestHelper)) {
                    status = true;
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoRecurAuthPaymentRequest(T requestHelper) {
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
                requestHelper.getPaymentParam().put("MALLID", requestHelper.getRecurCorePaymentChannel().getPaymentChannelCode() + "");
                if (requestHelper.getRecurCorePaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getRecurCorePaymentChannel().getPaymentChannelChainCode() > 0) {
                    requestHelper.getPaymentParam().put("CHAINNUM", requestHelper.getRecurCorePaymentChannel().getPaymentChannelChainCode() + "");
                } else {
                    requestHelper.getPaymentParam().put("CHAINNUM", "NA");
                }
                requestHelper.getPaymentParam().put("MerchantID", requestHelper.getRecurCorePaymentChannel().getMerchantPaymentChannelUid());
                requestHelper.getPaymentParam().put("MerchantName", requestHelper.getMerchantActivity().getMerchantIdx().getMerchantName());
                requestHelper.getPaymentParam().put("OrderNumber", requestHelper.getRecurHelper().getInvoiceNumber());
                String amountWord = "";
                String basket = "";
                if (requestHelper.getRecurHelper().getBillingRegisterAmount() != 0) {
//                    requestHelper.getPaymentParam().put("PurchaseAmt", String.valueOf(requestHelper.getRecurHelper().getBillingRegisterAmount()));
                    requestHelper.getPaymentParam().put("PurchaseAmt", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getRecurHelper().getBillingRegisterAmount()));
                    amountWord = String.valueOf(requestHelper.getRecurHelper().getBillingRegisterAmount());
                    basket = "Auth Recur Registration," + amountWord + ",1," + amountWord;
                } else {
                    requestHelper.getPaymentParam().put("PurchaseAmt", "10000.00");
                    amountWord = "10000.00";
                    basket = "Auth Recur Registration," + amountWord + ",1," + amountWord;
                }
//                requestHelper.getPaymentParam().put("PurchaseAmt", "10000.00");
                requestHelper.getPaymentParam().put("PurchaseCurrency", "360");
                StringBuilder word = new StringBuilder();
                word.append(amountWord);
                word.append(requestHelper.getRecurCorePaymentChannel().getMerchantPaymentChannelUid());
                word.append(requestHelper.getRecurCorePaymentChannel().getMerchantPaymentChannelHash());
                word.append(requestHelper.getRecurHelper().getInvoiceNumber());
                requestHelper.getPaymentParam().put("WORDS", HashWithSHA1.doHashing(word.toString(), null, null));
                requestHelper.getPaymentParam().put("SESSIONID", requestHelper.getRecurHelper().getSessionId());
                requestHelper.getPaymentParam().put("CardNumber", requestHelper.getCreditCardHelper().getCardNumber());
                requestHelper.getPaymentParam().put("EXPIRYDATE", requestHelper.getCreditCardHelper().getExpYear() + requestHelper.getCreditCardHelper().getExpMonth());
                requestHelper.getPaymentParam().put("CVV2", requestHelper.getCreditCardHelper().getCvv2());
                requestHelper.getPaymentParam().put("NAME", requestHelper.getCreditCardHelper().getCcName());
                requestHelper.getPaymentParam().put("CONTACTABLE_NAME", requestHelper.getRecurHelper().getCustomerName());
                requestHelper.getPaymentParam().put("CITY", requestHelper.getCreditCardHelper().getCcCity());
                requestHelper.getPaymentParam().put("STATE", requestHelper.getCreditCardHelper().getCcRegion());
                requestHelper.getPaymentParam().put("COUNTRY", requestHelper.getCreditCardHelper().getCcCountry());
                requestHelper.getPaymentParam().put("PHONE", requestHelper.getCreditCardHelper().getCcMobilePhone());
                requestHelper.getPaymentParam().put("HOMEPHONE", requestHelper.getCreditCardHelper().getCcHomePhone());
                requestHelper.getPaymentParam().put("OFFICEPHONE", requestHelper.getCreditCardHelper().getCcWorkPhone());
                requestHelper.getPaymentParam().put("EMAIL", requestHelper.getCreditCardHelper().getCcEmail());
                requestHelper.getPaymentParam().put("ZIP_CODE", requestHelper.getCreditCardHelper().getCcZipCode());
                requestHelper.getPaymentParam().put("ADDRESS", requestHelper.getCreditCardHelper().getCcAddress());

//                String basket = "Auth Recur Registration,10000.00,1,10000.00";
                requestHelper.getPaymentParam().put("PurchaseDesc", basket);
            }
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoRecurAuthPaymentResponse(T requestHelper) {
        boolean status = false;
        try {
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doRecurCustomerRegistration(T requestHelper) {
        boolean status = false;
        try {
            if (prepareDoRecurCustomerRegistrationRequest(requestHelper)) {
                OneCheckoutLogger.log("RECUR_REGISTRATION_REQUEST_IPG :\n" + requestHelper.getPaymentParam().toString());
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.RecurRegistration", null);
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

                    //OneCheckoutLogger.log("RECUR_REGISTRATION_RESPONSE :\n" + resultPayment);
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);

                    String result = xml.getString("RESULTMSG") != null ? xml.getString("RESULTMSG").trim() : "";
//                    String resultCode = xml.getString("ERRORCODE") != null ? xml.getString("ERRORCODE").trim() : "";
                    if (result.equalsIgnoreCase("SUCCESS")) {
                        requestHelper.getResultHelper().setRecurRegistrationStatus(true);
                    } 
//                        else if (resultCode.equalsIgnoreCase("XX16")) {
//                        //ini yang di lempar ke failPage
//                        requestHelper.setUseFailedPage(true);
//                        
//                        OneCheckoutDataHelper trxHelper = new OneCheckoutDataHelper();
//                        OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
//                        RecurHelper recur = requestHelper.getRecurHelper();
//                        notifyRequest.setAMOUNT(""+recur.getBillingAmount());
//                        notifyRequest.setSESSIONID(recur.getSessionId());
//                        notifyRequest.setTYPE("REVERSAL");
//                        notifyRequest.setWORDS(recur.getWords());
//                        notifyRequest.setTRANSIDMERCHANT(recur.getInvoiceNumber());
//                        notifyRequest.setCURRENCY(recur.getBillingCurrency());
//                        trxHelper.setNotifyRequest(notifyRequest);
//                        trxHelper.setMallId(recur.getMallId());
//                        trxHelper.setChainMerchantId(recur.getChainMallId());
//                        OneCheckoutPaymentChannel payChannel = OneCheckoutPaymentChannel.findType(recur.getPaymentChannelId());
//                        PaymentChannel paymentChannel = oneCheckoutV1QueryHelperBeanLocal.getPaymentChannelByChannel(payChannel);
//                        oneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBeanLocal.doMPGReversal(trxHelper, paymentChannel);
//                    }else{
//                        OneCheckoutDataHelper trxHelper = new OneCheckoutDataHelper();
//                        OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
//                        RecurHelper recur = requestHelper.getRecurHelper();
//                        notifyRequest.setAMOUNT(""+recur.getBillingAmount());
//                        notifyRequest.setSESSIONID(recur.getSessionId());
//                        notifyRequest.setTYPE("REVERSAL");
//                        notifyRequest.setWORDS(recur.getWords());
//                        notifyRequest.setTRANSIDMERCHANT(recur.getInvoiceNumber());
//                        notifyRequest.setCURRENCY(recur.getBillingCurrency());
//                        trxHelper.setNotifyRequest(notifyRequest);
//                        trxHelper.setMallId(recur.getMallId());
//                        trxHelper.setChainMerchantId(recur.getChainMallId());
//                        OneCheckoutPaymentChannel payChannel = OneCheckoutPaymentChannel.findType(recur.getPaymentChannelId());
//                        PaymentChannel paymentChannel = oneCheckoutV1QueryHelperBeanLocal.getPaymentChannelByChannel(payChannel);
//                        oneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBeanLocal.doMPGReversal(trxHelper, paymentChannel);
//                    }
                    doNotify(requestHelper);
                }

                if (prepareDoRecurCustomerRegistrationResponse(requestHelper)) {
                    status = true;
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoRecurCustomerRegistrationRequest(T requestHelper) {
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
            String words = HashWithSHA1.doHashing(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + requestHelper.getRecurHelper().getCustomerId() + requestHelper.getRecurHelper().getBillingNumber() + requestDateTime, null, null);
            requestHelper.getPaymentParam().put("WORDS", words);

            // CUSTOMER INFORMATION
            requestHelper.getPaymentParam().put("CUSTOMERID", requestHelper.getRecurHelper().getCustomerId());
            requestHelper.getPaymentParam().put("CUSTOMERNAME", requestHelper.getRecurHelper().getCustomerName());
            requestHelper.getPaymentParam().put("CUSTOMEREMAIL", requestHelper.getRecurHelper().getCustomerEmail());
            requestHelper.getPaymentParam().put("CUSTOMERADDRESS", requestHelper.getRecurHelper().getCustomerAddress());
            requestHelper.getPaymentParam().put("CUSTOMERCITY", requestHelper.getRecurHelper().getCustomerCity());
            requestHelper.getPaymentParam().put("CUSTOMERSTATE", requestHelper.getRecurHelper().getCustomerState());
            requestHelper.getPaymentParam().put("CUSTOMERCOUNTRY", requestHelper.getRecurHelper().getCustomerCountry());
            requestHelper.getPaymentParam().put("CUSTOMERZIPCODE", requestHelper.getRecurHelper().getCustomerZipCode());
            requestHelper.getPaymentParam().put("CUSTOMERMOBILEPHONE", requestHelper.getRecurHelper().getCustomerMobilePhone());
            requestHelper.getPaymentParam().put("CUSTOMERHOMEPHONE", requestHelper.getRecurHelper().getCustomerHomePhone());
            requestHelper.getPaymentParam().put("CUSTOMERWORKPHONE", requestHelper.getRecurHelper().getCustomerWorkPhone());

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

            // BILLING INFORMATION
            Currency currency = oneCheckoutV1QueryHelperBeanLocal.getCurrencyByCode(requestHelper.getRecurHelper().getBillingCurrency());
            String curr = "IDR";
            if (currency != null) {
                curr = currency.getAlpha3Code();
            }
            requestHelper.getPaymentParam().put("RECURRING", "R");
            requestHelper.getPaymentParam().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getRecurHelper().getBillingAmount()));
            requestHelper.getPaymentParam().put("PURCHASEAMOUNT", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getRecurHelper().getBillingPurchaseAmount()));
            requestHelper.getPaymentParam().put("CURRENCY", curr);
            requestHelper.getPaymentParam().put("PURCHASECURRENCY", curr);
            requestHelper.getPaymentParam().put("BILLNUMBER", requestHelper.getRecurHelper().getBillingNumber());
            requestHelper.getPaymentParam().put("BILLDETAIL", requestHelper.getRecurHelper().getBillingDetail());
            requestHelper.getPaymentParam().put("BILLTYPE", requestHelper.getRecurHelper().getBillingType());

            // SCHEDULE INFORMATION
            requestHelper.getPaymentParam().put("STARTDATE", requestHelper.getRecurHelper().getExecuteStartDate());
            requestHelper.getPaymentParam().put("ENDDATE", requestHelper.getRecurHelper().getExecuteEndDate());
            requestHelper.getPaymentParam().put("EXECUTETYPE", requestHelper.getRecurHelper().getExecuteType());
            requestHelper.getPaymentParam().put("EXECUTEDATE", requestHelper.getRecurHelper().getExecuteDate());
            requestHelper.getPaymentParam().put("EXECUTEMONTH", requestHelper.getRecurHelper().getExecuteMonth());
            requestHelper.getPaymentParam().put("FLATSTATUS", requestHelper.getRecurHelper().isFlatStatus() ? "TRUE" : "FALSE");
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoRecurCustomerRegistrationResponse(T requestHelper) {
        boolean status = false;
        try {
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doRecurCustomerUpdate(T requestHelper) {
        boolean status = false;
        try {
            if (prepareDoRecurCustomerUpdateRequest(requestHelper)) {
                //OneCheckoutLogger.log("RECUR_UPDATE_WITH_TOKEN_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.RecurUpdateBillingCard", null);
                OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
                String resultPayment = oneCheckoutChannelBase.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {
                    /*
                     <RESPONSE>
                     <RESULTMSG>SUCCESS</RESULTMSG>
                     <MALLID>2</MALLID>
                     <REQUESTDATETIME>20140605143725</REQUESTDATETIME>
                     <CUSTOMERID>TAUFIKISMAIL1234567890</CUSTOMERID>
                     <CUSTOMERNAME>Anggar Sasmito</CUSTOMERNAME>
                     <CUSTOMEREMAIL>anggar@nsiapay.net</CUSTOMEREMAIL>
                     <CUSTOMERADDRESS>Jakarta</CUSTOMERADDRESS>
                     <CUSTOMERCITY>Jakarta</CUSTOMERCITY>
                     <CUSTOMERSTATE>Jakarta</CUSTOMERSTATE>
                     <CUSTOMERCOUNTRY>ID</CUSTOMERCOUNTRY>
                     <CUSTOMERZIPCODE>13740</CUSTOMERZIPCODE>
                     <CUSTOMERMOBILEPHONE>6281808903132</CUSTOMERMOBILEPHONE>
                     <CUSTOMERHOMEPHONE>62215150555</CUSTOMERHOMEPHONE>
                     <CUSTOMERWORKPHONE>62215150555</CUSTOMERWORKPHONE>
                     <TOKENS>
                     <TOKEN>
                     <TOKENID>378051e63b069d440e82d434b38e017d3678189c</TOKENID>
                     <CARDNUMBER>451249******0591</CARDNUMBER>
                     <EXPIRYDATE>1805</EXPIRYDATE>
                     <CCNAME>Taufik Ismail</CCNAME>
                     <CCEMAIL>taufik@nsiapay.com</CCEMAIL>
                     <CCADDRESS>Jakarta</CCADDRESS>
                     <CCCITY>Jakarta</CCCITY>
                     <CCSTATE>Jakarta</CCSTATE>
                     <CCCOUNTRY>ID</CCCOUNTRY>
                     <CCZIPCODE>13223</CCZIPCODE>
                     <CCMOBILEPHONE>6281808903132</CCMOBILEPHONE>
                     <CCHOMEPHONE>62215150555</CCHOMEPHONE>
                     <CCWORKPHONE>62215150555</CCWORKPHONE>
                     <TOKENSTATUS>ACTIVE</TOKENSTATUS>
                     <BILLS>
                     <BILL>
                     <BILLNUMBER>1237974</BILLNUMBER>
                     <AMOUNT>100000.00</AMOUNT>
                     <PURCHASEAMOUNT>100000.00</PURCHASEAMOUNT>
                     <CURRENCY>360</CURRENCY>
                     <PURCHASECURRENCY>360</PURCHASECURRENCY>
                     <BILLDETAIL>test</BILLDETAIL>
                     <BILLTYPE>I</BILLTYPE>
                     <BILLSTATUS>ACTIVE</BILLSTATUS>
                     <STARTDATE>20140319</STARTDATE>
                     <ENDDATE>20150319</ENDDATE>
                     <EXECUTETYPE>D</EXECUTETYPE>
                     <EXECUTEDATE>04,11,18,25</EXECUTEDATE>
                     <EXECUTEMONTH>JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC</EXECUTEMONTH>
                     <FLATSTATUS>true</FLATSTATUS>
                     </BILL>
                     </BILLS>
                     </TOKEN>
                     </TOKENS>
                     </RESPONSE>
                     */
                    //OneCheckoutLogger.log("RECUR_UPDATE_WITH_TOKEN_RESPONSE :\n" + resultPayment);
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);
                    String result = xml.getString("RESULTMSG") != null ? xml.getString("RESULTMSG").trim() : "";
                    if (result.equalsIgnoreCase("SUCCESS")) {
                        String cardNumber = xml.getString("TOKENS.TOKEN.CARDNUMBER") != null ? xml.getString("TOKENS.TOKEN.CARDNUMBER").trim() : "";
                        requestHelper.getRecurHelper().setMaskingCard(cardNumber);
                        requestHelper.getResultHelper().setRecurUpdateStatus(true);
                    }
                    doNotify(requestHelper);
                }

                if (prepareDoRecurCustomerUpdateResponse(requestHelper)) {
                    status = true;
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoRecurCustomerUpdateRequest(T requestHelper) {
        boolean status = false;
        try {
            OneCheckoutLogger.log("RECUR UPDATE WITH TOKEN...");
            requestHelper.setPaymentParam(new HashMap<String, String>());
            // MERCHANT INFORMATION
            requestHelper.getPaymentParam().put("MALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "");
            if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
            }
            String requestDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            requestHelper.getPaymentParam().put("REQUESTDATETIME", requestDateTime);
            String words = HashWithSHA1.doHashing(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + requestHelper.getRecurHelper().getCustomerId() + requestHelper.getCreditCardHelper().getTokenNumber() + requestHelper.getRecurHelper().getBillingNumber() + requestDateTime, null, null);
            requestHelper.getPaymentParam().put("WORDS", words);

            // CUSTOMER INFORMATION
            requestHelper.getPaymentParam().put("CUSTOMERID", requestHelper.getRecurHelper().getCustomerId());

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

            // BILLING INFORMATION
            requestHelper.getPaymentParam().put("BILLNUMBER", requestHelper.getRecurHelper().getBillingNumber());
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean prepareDoRecurCustomerUpdateResponse(T requestHelper) {
        boolean status = false;
        try {
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doNotify(T requestHelper) {
        boolean status = false;
        try {
            String chainNumber = "NA";
            String url = requestHelper.getMerchantActivity().getMerchantIdx().getMerchantNotifyStatusUrl();
            HashMap<String, String> notifyParam = new HashMap<String, String>();
            notifyParam.put("MALLID", requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "");
            if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                chainNumber = requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantChainMerchantCode() + "";
            }
            notifyParam.put("CHAINMERCHANT", chainNumber);
            notifyParam.put("CUSTOMERID", requestHelper.getRecurHelper().getCustomerId());
            notifyParam.put("BILLNUMBER", requestHelper.getRecurHelper().getBillingNumber());
            String cardNumber = requestHelper.getRecurHelper().getMaskingCard();
            if (cardNumber == null || cardNumber.trim().equals("")) {
                cardNumber = requestHelper.getCreditCardHelper().getMaskCardNumber();
            }
            notifyParam.put("CARDNUMBER", cardNumber);
            notifyParam.put("ERRORCODE", "");
            if (requestHelper.getRecurHelper().isUpdateBillingStatus()) {
                notifyParam.put("STATUSTYPE", "T"); // P : NOTIFY PAYMENT, V : NOTIFY REVERSAL, G : NOTIFY RECUR REGISTRATION, T : NOTIFY RECUR UPDATE
                notifyParam.put("STATUS", requestHelper.getResultHelper().isRecurUpdateStatus() ? "SUCCESS" : "FAILED");
                notifyParam.put("MESSAGE", requestHelper.getResultHelper().isRecurUpdateStatus() ? "Update Success" : "Update Failed");
            } else {
                notifyParam.put("STATUSTYPE", "G"); // P : NOTIFY PAYMENT, V : NOTIFY REVERSAL, G : NOTIFY RECUR REGISTRATION, T : NOTIFY RECUR UPDATE
                notifyParam.put("STATUS", requestHelper.getResultHelper().isRecurRegistrationStatus() ? "SUCCESS" : "FAILED");
                notifyParam.put("MESSAGE", requestHelper.getResultHelper().isRecurRegistrationStatus() ? "Registration Success" : "Registration Failed");
            }

            /*
             WORDS --> MALLID + CHAINMERCHANT + BILLNUMBER + CUSTOMERID + STATUS + <shared key>
             */
            StringBuilder notifyWords = new StringBuilder();
            notifyWords.append(requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "");
            notifyWords.append(chainNumber);
            notifyWords.append(requestHelper.getRecurHelper().getBillingNumber());
            notifyWords.append(requestHelper.getRecurHelper().getCustomerId());
            notifyWords.append(requestHelper.getResultHelper().isRecurRegistrationStatus() ? "SUCCESS" : "FAILED");
            notifyWords.append(requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantHashPassword());
            String hashNotifyWords = HashWithSHA1.doHashing(notifyWords.toString(), requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantShaFormat(), requestHelper.getMerchantPaymentChannel().getMerchants().getMerchantHashPassword());
            OneCheckoutLogger.log("WORD in Clear Text : %s", hashNotifyWords.toString());
            notifyParam.put("WORDS", hashNotifyWords);

            OneCheckoutLogger.log("RECUR_NOTIFY_REGISTRATION_REQUEST :\n" + notifyParam.toString());
            String notifyResult = super.postMIP(notifyParam, url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
            OneCheckoutLogger.log("RECUR_NOTIFY_REGISTRATION_RESPONSE :\n" + notifyResult + "]");
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml) {
        try {
            String status = xml.getString("status");
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            if (status != null && status.equalsIgnoreCase("Success")) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            }
            String responseCode = xml.getString("responsecode");
            String approvalCode = xml.getString("ApprovalCode");
            String issuerBankName = xml.getString("IssuerBankName");
            String verifyStatus = xml.getString("verifyStatus");
            String verifyScore = xml.getString("verifyScore");
            String hostRefNum = xml.getString("HostReferenceNumber");
            if (responseCode != null && responseCode.length() != 4) {
                responseCode = "00" + responseCode;
            }
            trans.setDokuApprovalCode(approvalCode);
            trans.setDokuIssuerBank(issuerBankName);
            trans.setDokuResponseCode(responseCode);
            trans.setDokuHostRefNum(hostRefNum);
            try {
                if (verifyStatus != null) {
                    OneCheckoutDFSStatus vStatus = OneCheckoutDFSStatus.findType(verifyStatus.trim().charAt(0));
                    if (vStatus != null) {
                        trans.setVerifyStatus(vStatus.value());
                    } else {
                        trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    }
                    if (verifyScore != null) {
                        trans.setVerifyScore(Integer.parseInt(verifyScore));
                    } else {
                        trans.setVerifyScore(-1);
                    }
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
            //    trans.setEduStatus(trans.getVerifyStatus());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());
            return trans;
        } catch (Exception ex) {
            return trans;
        }
    }
}
