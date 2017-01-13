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
import com.onecheckoutV1.ejb.util.MPGVoidReponseXML;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Country;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author Opiks
 */
@Stateless
public class MPGRecurBean extends OneCheckoutChannelBase implements MPGRecurLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

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

                //OneCheckoutLogger.log("MPG_PAYMENT_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                String result = "";
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.MPG.MIP.PLUS", null);
                if (requestHelper.getThreeDSecureHelper() != null && requestHelper.getThreeDSecureHelper().isParseStatus()) {
                    url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.MPG.MIP.THREEDSECURE", null);
                }
                String resultPayment = super.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getRecurCorePaymentChannel().getPaymentChannel());
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
                        String issuerBankName = xml.getString("bank");
                        String verifyStatus = xml.getString("fraudScreeningStatus");
                        String verifyScore = xml.getString("fraudScreeningScore");
                        String verifyReason = xml.getString("fraudScreeningReason");
                        requestHelper.getResultHelper().setResponseCode(responseCode);
                        requestHelper.getResultHelper().setTransactionCode(transactionCode);
                        if (result.equalsIgnoreCase("SUCCESS") && responseCode.equalsIgnoreCase("00")) {
                            String approvalCode = xml.getString("approvalCode");
                            requestHelper.getResultHelper().setApprovalCode(approvalCode);
                            requestHelper.getResultHelper().setTransactions(parseXMLPayment(requestHelper.getResultHelper().getTransactions(), xml));
                            if (requestHelper.getThreeDSecureHelper() != null && requestHelper.getThreeDSecureHelper().isParseStatus()) {
                                requestHelper.getResultHelper().setRecurAuthorizeStatus(true);
                            } else {
                                // --------------------------------------
                                // NEED TO DO VOID TRANSACTION
                                // --------------------------------------
                                StringBuilder word = new StringBuilder();
                                word.append(transactionCode);
                                word.append(requestHelper.getRecurCorePaymentChannel().getMerchantPaymentChannelHash());
                                word.append(approvalCode);
                                String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

                                String urlVoid = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.MPG.VOID", null);
                                HashMap<String, String> voidParam = new HashMap<String, String>();
                                voidParam.put("SERVICEID", requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getServiceId());
                                voidParam.put("TRXCODE", transactionCode);
                                voidParam.put("PAYMENTAPPROVALCODE", requestHelper.getResultHelper().getApprovalCode());
                                voidParam.put("WORDS", hashwords);

                                //OneCheckoutLogger.log("RECUR_VOID_REQUEST :\n" + voidParam.toString());
                                String voidResult = super.postMIP(voidParam, urlVoid, requestHelper.getRecurCorePaymentChannel().getPaymentChannel());
                                OneCheckoutLogger.log("RECUR_VOID_RESPONSE :\n" + voidResult);
                            }
                            em.merge(requestHelper.getResultHelper().getTransactions());
                        } else if (result.equalsIgnoreCase("THREEDSECURE")) {
                            String paReq = xml.getString("paReq");
                            String MD = xml.getString("md");
                            String acsUrl = xml.getString("acsUrl");

                            String RedirectBackUrl = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.REDIRECT.ACS.URL", "");
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
                            requestHelper.setErrorCode("00" + responseCode);
                            requestHelper.getResultHelper().getTransactions().setDokuResponseCode("00" + responseCode);
                            requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                            em.merge(requestHelper.getResultHelper().getTransactions());
                            doNotify(requestHelper);
                        }
                    } else {
                        requestHelper.setErrorCode(OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE.value());
                        requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE.value());
                        requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                        em.merge(requestHelper.getResultHelper().getTransactions());
                        doNotify(requestHelper);
                    }
                } else {
                    requestHelper.setErrorCode(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                    requestHelper.getResultHelper().getTransactions().setDokuResponseCode(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                    requestHelper.getResultHelper().getTransactions().setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    requestHelper.getResultHelper().getTransactions().setDokuInvokeStatusDatetime(new Date());
                    em.merge(requestHelper.getResultHelper().getTransactions());
                    doNotify(requestHelper);
                }

                if (prepareDoRecurAuthPaymentResponse(requestHelper)) {
                    if (!result.trim().equalsIgnoreCase("THREEDSECURE")) {
                    }
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
                requestHelper.setPaymentParam(new HashMap<String, String>());
                requestHelper.getPaymentParam().put("PaRes", requestHelper.getThreeDSecureHelper().getPaRes());
                requestHelper.getPaymentParam().put("MD", requestHelper.getThreeDSecureHelper().getMerchantData());
            } else {
                String curr = "IDR";
                Currency currency = oneCheckoutV1QueryHelperBeanLocal.getCurrencyByCode(requestHelper.getRecurHelper().getBillingPurchaseCurrency());
                if (currency != null) {
                    curr = currency.getAlpha3Code();
                }

                StringBuilder word = new StringBuilder();
                word.append(OneCheckoutVerifyFormatData.sdf.format(requestHelper.getRecurHelper().getBillingAmount()));
                word.append(requestHelper.getRecurCorePaymentChannel().getPaymentChannelCode());
                word.append(requestHelper.getRecurHelper().getInvoiceNumber());
                word.append(requestHelper.getRecurCorePaymentChannel().getMerchantPaymentChannelHash());
                word.append(curr);
                OneCheckoutLogger.log("WORD in Clear Text : %s", word.toString());
                String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

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
                requestHelper.getPaymentParam().put("SERVICEID", requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getServiceId());
                requestHelper.getPaymentParam().put("MALLID", requestHelper.getRecurCorePaymentChannel().getPaymentChannelCode() + "");
                if (requestHelper.getRecurCorePaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getRecurCorePaymentChannel().getPaymentChannelChainCode() > 0) {
                    requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getRecurCorePaymentChannel().getPaymentChannelChainCode() + "");
                }
                requestHelper.getPaymentParam().put("INVOICENUMBER", requestHelper.getRecurHelper().getInvoiceNumber());
                //requestHelper.getPaymentParam().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getRecurHelper().getBillingAmount()));
                String basket = "";
                if (requestHelper.getRecurHelper().getBillingRegisterAmount() != 0) {
                    requestHelper.getPaymentParam().put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(requestHelper.getRecurHelper().getBillingRegisterAmount()));
                    basket = "Auth Recur Registration," + String.valueOf(requestHelper.getRecurHelper().getBillingRegisterAmount()) + ",1," + String.valueOf(requestHelper.getRecurHelper().getBillingRegisterAmount());
                } else {
                    requestHelper.getPaymentParam().put("AMOUNT", "10000.00");
                    basket = "Auth Recur Registration,10000.00,1,10000.00";
                }
//                requestHelper.getPaymentParam().put("AMOUNT", "10000.00");

                requestHelper.getPaymentParam().put("CURRENCY", curr);
                requestHelper.getPaymentParam().put("WORDS", hashwords);
                requestHelper.getPaymentParam().put("SESSIONID", requestHelper.getRecurHelper().getSessionId());

//                String basket = "Auth Recur Registration,10000.00,1,10000.00";
                requestHelper.getPaymentParam().put("BASKET", basket);

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
                //OneCheckoutLogger.log("RECUR_REGISTRATION_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                String url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.RecurRegistration", null);
                OneCheckoutLogger.log("PARAM RECUR REGISTER MPG : " + requestHelper.getPaymentParam().toString());
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

//                    response failed 
//<RESPONSE>
//  <RESULTMSG>FAILED</RESULTMSG>
//  <ERRORCODE>XX16</ERRORCODE>
//  <MESSAGE>Billing Number was registered before</MESSAGE>
//  <MALLID>2</MALLID>
//  <REQUESTDATETIME>20151016101229</REQUESTDATETIME>
//  <CUSTOMERID>6814258</CUSTOMERID>
//</RESPONSE>
                    //OneCheckoutLogger.log("RECUR_REGISTRATION_RESPONSE :\n" + resultPayment);
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);

                    String result = xml.getString("RESULTMSG") != null ? xml.getString("RESULTMSG").trim() : "";
                    String resultCode = xml.getString("ERRORCODE") != null ? xml.getString("ERRORCODE").trim() : "";
                    requestHelper.getResultHelper().setRecurRegistrationStatus(false);
                    if (result.equalsIgnoreCase("SUCCESS")) {
                        requestHelper.getResultHelper().setRecurRegistrationStatus(true);
                    } else if (!result.equalsIgnoreCase("SUCCESS") && !resultCode.equals("")) {
//                        OneCheckoutLogger.log("errorCode From MPG =* " + resultCode);
                        if (resultCode.length() == 4) {
                            requestHelper.setErrorCode(resultCode);
                        } else if (resultCode.length() < 4) {
                            for (int i = 0; i < (4 - resultCode.length()); i++) {
                                resultCode = "0" + resultCode;
                            }
                            requestHelper.setErrorCode(resultCode);
                        }

                    }
//                    else {
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
//                        
//                        OneCheckoutPaymentChannel payChannel = OneCheckoutPaymentChannel.findType(recur.getPaymentChannelId());
//                        PaymentChannel paymentChannel = oneCheckoutV1QueryHelperBeanLocal.getPaymentChannelByChannel(payChannel);
//                        oneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBeanLocal.doMPGReversal(trxHelper, paymentChannel);
//                        
//                        Transactions trans = requestHelper.getResultHelper().getTransactions();
//                        Merchants merchant = oneCheckoutV1QueryHelperBeanLocal.getMerchantByMallId(recur.getMallId(), recur.getChainMallId());
//                        MerchantPaymentChannel mpc = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(merchant, paymentChannel);
//                        this.doVoidMpg(trans.getDokuVoidApprovalCode(), trans.getDokuApprovalCode(), mpc.getMerchantPaymentChannelHash(), paymentChannel);
//                    }

//                    else{
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

    private void doVoidMpg(String trxCode, String paymentApprovalCode, String sharedKey, PaymentChannel paymentChannel) {
        String serviceId = "1";
        StringBuilder word = new StringBuilder();
        word.append(trxCode);
        word.append(sharedKey);
        word.append(paymentApprovalCode);
        String hashWords = HashWithSHA1.doHashing(word.toString(), null, null);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("SERVICEID", serviceId);
        params.put("TRXCODE", trxCode);
        params.put("PAYMENTAPPROVALCODE", paymentApprovalCode);
        params.put("WORDS", hashWords);
        String mpgVoidUrl = config.getString("RECUR.URL.VOID", "").trim();
        String data_encode = this.createParamsHTTP(params);

        OneCheckoutLogger.log("Fetch URL     : %s", mpgVoidUrl);
        OneCheckoutLogger.log("Merchant Data : %s", data_encode);

        OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
        String voidResponse = oneCheckoutChannelBase.postMIP(params, mpgVoidUrl, paymentChannel);
        OneCheckoutLogger.log("Onecheckoutv1.doVoidMPG response : " + voidResponse);
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
                    String resultCode = xml.getString("ERRORCODE") != null ? xml.getString("ERRORCODE").trim() : "";
                    if (result.equalsIgnoreCase("SUCCESS")) {
                        String cardNumber = xml.getString("TOKENS.TOKEN.CARDNUMBER") != null ? xml.getString("TOKENS.TOKEN.CARDNUMBER").trim() : "";
                        requestHelper.getRecurHelper().setMaskingCard(cardNumber);
                        requestHelper.getResultHelper().setRecurUpdateStatus(true);
                    } else if (!result.equalsIgnoreCase("SUCCESS") && !resultCode.equals("")) {
//                        OneCheckoutLogger.log("errorCode From MPG =* " + resultCode);
                        if (resultCode.length() == 4) {
                            requestHelper.setErrorCode(resultCode);
                        } else if (resultCode.length() < 4) {
                            for (int i = 0; i < (4 - resultCode.length()); i++) {
                                resultCode = "0" + resultCode;
                            }
                            requestHelper.setErrorCode(resultCode);
                        }

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

    private Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml) {
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
            if (result != null && result.equalsIgnoreCase("Success")) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            }
            if (responseCode != null && responseCode.length() != 4) {
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
                    //  trans.setEduStatus(dfsStatus.value());
                } else {
                    trans.setVerifyId("");
                    trans.setVerifyScore(-1);
                    trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    //  trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                }
            } catch (Throwable t) {
                trans.setVerifyScore(0);
            }
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());
            em.merge(trans);
            return trans;
        } catch (Exception ex) {
            return trans;
        }
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
            if (requestHelper.getErrorCode() != null) {
                notifyParam.put("ERRORCODE", requestHelper.getErrorCode());
            } else {
                notifyParam.put("ERRORCODE", "");
            }
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
}
