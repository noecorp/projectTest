/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.manage;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.ejb.helper.CreditCardHelper;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.RecurHelper;
import com.onecheckoutV1.ejb.helper.RequestHelper;
import com.onecheckoutV1.ejb.helper.ResultHelper;
import com.onecheckoutV1.ejb.helper.TokenizationHelper;
import com.onecheckoutV1.ejb.proc.IPGRecurLocal;
import com.onecheckoutV1.ejb.proc.IPGTokenizationLocal;
import com.onecheckoutV1.ejb.proc.MPGRecurLocal;
import com.onecheckoutV1.ejb.proc.MPGTokenizationLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.enums.EActivity;
import com.onecheckoutV1.enums.EMerchantActivityStatus;
import com.onecheckoutV1.enums.EParameterName;
import com.onecheckoutV1.parser.RequestParserLocal;
import com.onecheckoutV1.template.CustomerBillObject;
import com.onecheckoutV1.template.CustomerInfoObject;
import com.onecheckoutV1.template.CustomerTokenObject;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onechekoutv1.dto.Activity;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantActivity;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Session;
import doku.accountbilling.xml.RESPONSEDocument;
import doku.accountbilling.xml.RESPONSEDocument.RESPONSE.TOKENS.TOKEN;
import doku.accountbilling.xml.RESPONSEDocument.RESPONSE.TOKENS.TOKEN.BILLS.BILL;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author Opiks
 */
@Stateless
public class ManageBean implements ManageLocal {

    @Resource
    SessionContext context;
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    private EntityManager em;
    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;
    @EJB
    private RequestParserLocal requestParserLocal;
    @EJB
    private IPGRecurLocal iPGRecurLocal;
    @EJB
    private MPGRecurLocal mPGRecurLocal;
    @EJB
    private IPGTokenizationLocal iPGTokenizationLocal;
    @EJB
    private MPGTokenizationLocal mPGTokenizationLocal;

    public <T extends RequestHelper> boolean manageMerchantActivity(T requestHelper, MerchantActivity merchantActivity) {
        boolean status = false;
        try {
            if (merchantActivity != null && merchantActivity.getActivityId() != null) {
                requestHelper.setTemplateFileName("error.html");
                requestHelper.setMerchantActivity(merchantActivity);
                if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_REGISTRATION_REQUEST.code()) {
                    if (requestParserLocal.doParsingRecurRegistrationCustomerRequest(requestHelper)) {
                        status = true;
                        CustomerInfoObject customerInfoObject = getAccountBillingCustomerInfo(requestHelper);
                        if (customerInfoObject != null) {
                            requestHelper.getResultHelper().setCustomerInfoObject(customerInfoObject);
                            for (CustomerTokenObject customerTokenObject : customerInfoObject.getTokens()) {
                                for (CustomerBillObject customerBillObject : customerTokenObject.getBills()) {
                                    if (customerBillObject.getBillstatus() != null && customerBillObject.getBillstatus().trim().equalsIgnoreCase("ACTIVE") && requestHelper.getRecurHelper().getBillingNumber().equals(customerBillObject.getBillnumber().trim())) {
//                                        requestHelper.setTemplateFileName("recurResult.html");
                                        OneCheckoutLogger.log("BILL NUMBER HAS BEEN REGISTERED...");
                                        status = false;
                                    }
                                }
                            }
                            if (!status) {
                                MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.CreditCard);
                                if (merchantPaymentChannel != null) {
                                    iPGRecurLocal.doNotify(requestHelper);
                                } else {
                                    merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.BSP);
                                    if (merchantPaymentChannel != null) {
                                        mPGRecurLocal.doNotify(requestHelper);
                                    }
                                }
                            }
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_REGISTRATION_DATA.code()) {
                    if (requestParserLocal.doParsingRecurRegistrationCustomerData(requestHelper)) {
                        status = true;
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_REGISTRATION_AUTHORIZE.code()) {
                    OneCheckoutLogger.log("TOKEN_NUMBER[" + requestHelper.getCreditCardHelper().getTokenNumber() + "]");
                    if (!requestHelper.getCreditCardHelper().getTokenNumber().equals("")) {
                        OneCheckoutLogger.log("RECUR REGISTRATION WITH TOKEN...");
                        requestHelper.setPaymentParam(new HashMap<String, String>());
                        // MERCHANT INFORMATION
                        requestHelper.getPaymentParam().put("MALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "");
                        if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                            requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
                        }
                        String requestDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
                        requestHelper.getPaymentParam().put("REQUESTDATETIME", requestDateTime);
                        String words = HashWithSHA1.doHashing(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + requestHelper.getRecurHelper().getCustomerId() + requestHelper.getCreditCardHelper().getTokenNumber() + requestHelper.getRecurHelper().getBillingNumber() + requestDateTime, "SHA1", null);
                        requestHelper.getPaymentParam().put("WORDS", words);

                        // CUSTOMER INFORMATION
                        requestHelper.getPaymentParam().put("CUSTOMERID", requestHelper.getRecurHelper().getCustomerId());

                        // CARD INFORMATION
                        requestHelper.getPaymentParam().put("TOKENID", requestHelper.getCreditCardHelper().getTokenNumber());

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
                        String flatStatus = requestHelper.getRecurHelper().isFlatStatus() ? "TRUE" : "FALSE";
                        requestHelper.getPaymentParam().put("FLATSTATUS", flatStatus);

                        //OneCheckoutLogger.log("RECUR_REGISTRATION_WITH_TOKEN_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                        String url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.RecurRegistrationWithToken", null);
                        OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
                        String resultPayment = oneCheckoutChannelBase.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
                        if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {
                            //OneCheckoutLogger.log("RECUR_REGISTRATION_WITH_TOKEN_RESPONSE :\n" + resultPayment);
                            XMLConfiguration xml = new XMLConfiguration();
                            StringReader sr = new StringReader(resultPayment);
                            xml.load(sr);
                            String result = xml.getString("RESULTMSG") != null ? xml.getString("RESULTMSG").trim() : "";
                            if (result.equalsIgnoreCase("SUCCESS")) {
                                String cardNumber = xml.getString("TOKENS.TOKEN.CARDNUMBER") != null ? xml.getString("TOKENS.TOKEN.CARDNUMBER").trim() : "";
                                requestHelper.getRecurHelper().setMaskingCard(cardNumber);
                                requestHelper.getResultHelper().setRecurRegistrationStatus(true);
                            }
                        }
                        iPGRecurLocal.doNotify(requestHelper);
                        status = true;
                    } else {
                        OneCheckoutLogger.log("RECUR REGISTRATION WITH CREDIT CARD...");
                        MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.CreditCard);
                        if (merchantPaymentChannel != null) {
                            requestHelper.setRecurCorePaymentChannel(merchantPaymentChannel);
                            if (iPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                                status = true;
                            }
                        } else {
                            OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT IPG");
                            merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.BSP);
                            if (merchantPaymentChannel != null) {
                                requestHelper.setRecurCorePaymentChannel(merchantPaymentChannel);
                                if (mPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                                    status = true;
                                }
                            } else {
                                OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT MPG");
                            }
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_REGISTRATION_THREE_D_SECURE.code()) {
                    if (requestParserLocal.doParsingThreeDSecure(requestHelper)) {
                        status = true;
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_REGISTRATION_PAYMENT.code()) {
                    if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.CreditCard.value())) {
                        if (iPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                            status = true;
                        }
                    } else if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.BSP.value())) {
                        if (mPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                            status = true;
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_REGISTRATION.code()) {
                    if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.CreditCard.value())) {
                        if (iPGRecurLocal.doRecurCustomerRegistration(requestHelper)) {
                            status = true;
                        }
                    } else if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.BSP.value())) {
                        if (mPGRecurLocal.doRecurCustomerRegistration(requestHelper)) {
                            status = true;
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_UPDATE_REQUEST.code()) {
                    if (requestParserLocal.doParsingRecurUpdateCustomerRequest(requestHelper)) {
                        CustomerInfoObject customerInfoObject = getAccountBillingCustomerInfo(requestHelper);
                        if (customerInfoObject != null) {
                            for (CustomerTokenObject customerTokenObject : customerInfoObject.getTokens()) {
                                for (CustomerBillObject customerBillObject : customerTokenObject.getBills()) {
                                    if (customerBillObject.getBillstatus() != null && customerBillObject.getBillstatus().trim().equalsIgnoreCase("ACTIVE") && requestHelper.getRecurHelper().getBillingNumber().equals(customerBillObject.getBillnumber().trim())) {
                                        requestHelper.getRecurHelper().setCustomerName(customerInfoObject.getCustomername());
                                        requestHelper.getRecurHelper().setCustomerAddress(customerInfoObject.getCustomeraddress());
                                        requestHelper.getRecurHelper().setCustomerCity(customerInfoObject.getCustomercity());
                                        requestHelper.getRecurHelper().setCustomerCountry(customerInfoObject.getCustomercountry());
                                        requestHelper.getRecurHelper().setCustomerEmail(customerInfoObject.getCustomeremail());
                                        requestHelper.getRecurHelper().setCustomerHomePhone(customerInfoObject.getCustomerhomephone());
                                        requestHelper.getRecurHelper().setCustomerMobilePhone(customerInfoObject.getCustomermobilephone());
                                        requestHelper.getRecurHelper().setCustomerState(customerInfoObject.getCustomerstate());
                                        requestHelper.getRecurHelper().setCustomerWorkPhone(customerInfoObject.getCustomerworkphone());
                                        requestHelper.getRecurHelper().setCustomerZipCode(customerInfoObject.getCustomerzipcode());
                                        requestHelper.getRecurHelper().setBillingNumber(customerBillObject.getBillnumber());
                                        requestHelper.getRecurHelper().setBillingType(customerBillObject.getBilltype());
                                        requestHelper.getRecurHelper().setBillingDetail(customerBillObject.getBilldetail());
                                        requestHelper.getRecurHelper().setExecuteDate(customerBillObject.getExecutedate());
                                        requestHelper.getRecurHelper().setExecuteMonth(customerBillObject.getExecutemonth());
                                        requestHelper.getRecurHelper().setExecuteStartDate(customerBillObject.getStartdate());
                                        requestHelper.getRecurHelper().setExecuteEndDate(customerBillObject.getEnddate());
                                        requestHelper.getRecurHelper().setMaskingCard(customerTokenObject.getCardnumber());
                                        requestHelper.getRecurHelper().setUpdatedTokenId(customerTokenObject.getTokenid());
                                        requestHelper.getResultHelper().setCustomerInfoObject(customerInfoObject);
                                        status = true;
                                        break;
                                    }
                                }
                            }
                            OneCheckoutLogger.log("BILL NUMBER NOT IN LIST...");
                        } else {
                            OneCheckoutLogger.log("INVALID BILL NUMBER...");
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_UPDATE_DATA.code()) {
                    if (requestParserLocal.doParsingRecurUpdateCustomerData(requestHelper)) {
                        status = true;
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_UPDATE_AUTHORIZE.code()) {
                    OneCheckoutLogger.log("TOKEN_NUMBER[" + requestHelper.getCreditCardHelper().getTokenNumber() + "]");
                    if (!requestHelper.getCreditCardHelper().getTokenNumber().equals("")) {
                        OneCheckoutLogger.log("RECUR UPDATE WITH TOKEN...");
                        requestHelper.setPaymentParam(new HashMap<String, String>());
                        // MERCHANT INFORMATION
                        requestHelper.getPaymentParam().put("MALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "");
                        if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                            requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
                        }
                        String requestDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
                        requestHelper.getPaymentParam().put("REQUESTDATETIME", requestDateTime);
                        String words = HashWithSHA1.doHashing(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + requestHelper.getRecurHelper().getCustomerId() + requestHelper.getCreditCardHelper().getTokenNumber() + requestHelper.getRecurHelper().getBillingNumber() + requestDateTime, "SHA1", null);
                        requestHelper.getPaymentParam().put("WORDS", words);

                        // CUSTOMER INFORMATION
                        requestHelper.getPaymentParam().put("CUSTOMERID", requestHelper.getRecurHelper().getCustomerId());

                        // CARD INFORMATION
                        requestHelper.getPaymentParam().put("TOKENID", requestHelper.getCreditCardHelper().getTokenNumber());

                        // BILLING INFORMATION
                        requestHelper.getPaymentParam().put("BILLNUMBER", requestHelper.getRecurHelper().getBillingNumber());

                        //OneCheckoutLogger.log("RECUR_UPDATE_WITH_TOKEN_REQUEST :\n" + requestHelper.getPaymentParam().toString());
                        String url = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.RecurUpdateBillingCard", null);
                        OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
                        String resultPayment = oneCheckoutChannelBase.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
                        if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {
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
                        }
                        iPGRecurLocal.doNotify(requestHelper);
                        status = true;
                    } else {
                        OneCheckoutLogger.log("RECUR REGISTRATION WITH CREDIT CARD...");
                        MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.CreditCard);
                        if (merchantPaymentChannel != null) {
                            requestHelper.setRecurCorePaymentChannel(merchantPaymentChannel);
                            if (iPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                                status = true;
                            }
                        } else {
                            OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT IPG");
                            merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.BSP);
                            if (merchantPaymentChannel != null) {
                                requestHelper.setRecurCorePaymentChannel(merchantPaymentChannel);
                                if (mPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                                    status = true;
                                }
                            } else {
                                OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT MPG");
                            }
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_UPDATE_THREE_D_SECURE.code()) {
                    if (requestParserLocal.doParsingThreeDSecure(requestHelper)) {
                        status = true;
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_UPDATE_PAYMENT.code()) {
                    if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.CreditCard.value())) {
                        if (iPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                            status = true;
                        }
                    } else if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.BSP.value())) {
                        if (mPGRecurLocal.doRecurAuthPayment(requestHelper)) {
                            status = true;
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_UPDATE.code()) {
                    if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.CreditCard.value())) {
                        if (iPGRecurLocal.doRecurCustomerUpdate(requestHelper)) {
                            status = true;
                        }
                    } else if (requestHelper.getRecurCorePaymentChannel().getPaymentChannel().getPaymentChannelId().equals(OneCheckoutPaymentChannel.BSP.value())) {
                        if (mPGRecurLocal.doRecurCustomerUpdate(requestHelper)) {
                            status = true;
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_REQUEST.code()) {
                    if (requestParserLocal.doParsingTokenPaymentRequest(requestHelper)) {
                        status = true;
                        CustomerInfoObject customerInfoObject = getAccountBillingCustomerInfo(requestHelper);
                        if (customerInfoObject != null) {
                            requestHelper.getResultHelper().setCustomerInfoObject(customerInfoObject);
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_DATA.code()) {
                    if (requestParserLocal.doParsingTokenPaymentData(requestHelper)) {
                        if (requestHelper.getCreditCardHelper().getButton().equalsIgnoreCase("EDITCARD")) {
                            CustomerInfoObject customerInfoObject = getInfoCustomerRecurbyToken(requestHelper);
                            if (customerInfoObject != null) {
                                requestHelper.getCreditCardHelper().setCardNumber(customerInfoObject.getTokens().get(0).getCardnumber());
                                requestHelper.getCreditCardHelper().setExpYear(customerInfoObject.getTokens().get(0).getExpirydate().substring(0, 2));
                                requestHelper.getCreditCardHelper().setExpMonth(customerInfoObject.getTokens().get(0).getExpirydate().substring(2));
                                requestHelper.getCreditCardHelper().setCcAddress(customerInfoObject.getTokens().get(0).getCcaddress());
                                requestHelper.getCreditCardHelper().setCcCity(customerInfoObject.getTokens().get(0).getCccity());
                                requestHelper.getCreditCardHelper().setCcCountry(customerInfoObject.getTokens().get(0).getCccountry());
                                requestHelper.getCreditCardHelper().setCcEmail(customerInfoObject.getTokens().get(0).getCcemail());
                                requestHelper.getCreditCardHelper().setCcHomePhone(customerInfoObject.getTokens().get(0).getCchomephone());
                                requestHelper.getCreditCardHelper().setCcMobilePhone(customerInfoObject.getTokens().get(0).getCcmobilephone());
                                requestHelper.getCreditCardHelper().setCcName(customerInfoObject.getTokens().get(0).getCcname());
                                requestHelper.getCreditCardHelper().setCcRegion(customerInfoObject.getTokens().get(0).getCcstate());
                                requestHelper.getCreditCardHelper().setCcWorkPhone(customerInfoObject.getTokens().get(0).getCcworkphone());
                                requestHelper.getCreditCardHelper().setCcZipCode(customerInfoObject.getTokens().get(0).getCczipcode());
                                requestHelper.getCreditCardHelper().setTokenNumber(customerInfoObject.getTokens().get(0).getTokenid());
                            }
                        }
                        status = true;
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT.code()) {
                    boolean continueStatus = true;
                    if (!requestHelper.getCreditCardHelper().getTokenNumber().equals("")) {
                        CustomerInfoObject customerInfoObject = getInfoCustomerRecurbyToken(requestHelper);
                        if (customerInfoObject != null) {
                            requestHelper.getCreditCardHelper().setCardNumber(customerInfoObject.getTokens().get(0).getCardnumber());
                            requestHelper.getCreditCardHelper().setExpYear(customerInfoObject.getTokens().get(0).getExpirydate().substring(0, 2));
                            requestHelper.getCreditCardHelper().setExpMonth(customerInfoObject.getTokens().get(0).getExpirydate().substring(2));
                            requestHelper.getCreditCardHelper().setCcAddress(customerInfoObject.getTokens().get(0).getCcaddress());
                            requestHelper.getCreditCardHelper().setCcCity(customerInfoObject.getTokens().get(0).getCccity());
                            requestHelper.getCreditCardHelper().setCcCountry(customerInfoObject.getTokens().get(0).getCccountry());
                            requestHelper.getCreditCardHelper().setCcEmail(customerInfoObject.getTokens().get(0).getCcemail());
                            requestHelper.getCreditCardHelper().setCcHomePhone(customerInfoObject.getTokens().get(0).getCchomephone());
                            requestHelper.getCreditCardHelper().setCcMobilePhone(customerInfoObject.getTokens().get(0).getCcmobilephone());
                            requestHelper.getCreditCardHelper().setCcName(customerInfoObject.getTokens().get(0).getCcname());
                            requestHelper.getCreditCardHelper().setCcRegion(customerInfoObject.getTokens().get(0).getCcstate());
                            requestHelper.getCreditCardHelper().setCcWorkPhone(customerInfoObject.getTokens().get(0).getCcworkphone());
                            requestHelper.getCreditCardHelper().setCcZipCode(customerInfoObject.getTokens().get(0).getCczipcode());
                        } else {
                            continueStatus = false;
                        }
                    }

                    if (continueStatus) {
                        MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.CreditCard);
                        if (merchantPaymentChannel != null) {
                            requestHelper.setTokenizationCorePaymentChannel(merchantPaymentChannel);
                            if (iPGTokenizationLocal.doTokenizationAuthPayment(requestHelper)) {
                                if (requestHelper.getCreditCardHelper().getTokenNumber() == null || requestHelper.getCreditCardHelper().getTokenNumber().trim().equals("")) {
                                    if (requestHelper.getResultHelper().isTokenAuthorizeStatus() && requestHelper.getCreditCardHelper().getCcsaveStatus() != null && requestHelper.getCreditCardHelper().getCcsaveStatus().equalsIgnoreCase("yes")) {
                                        OneCheckoutLogger.log(":: REGISTER TO ACCOUNTBILLING ::");
                                        iPGTokenizationLocal.doTokenizationCustomerRegistration(requestHelper);
                                    }
                                }
                            }
                            status = true;
                        } else {
                            OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT IPG");
                            merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.BSP);
                            if (merchantPaymentChannel != null) {
                                requestHelper.setTokenizationCorePaymentChannel(merchantPaymentChannel);
                                if (mPGTokenizationLocal.doTokenizationAuthPayment(requestHelper)) {
                                    if (requestHelper.getCreditCardHelper().getTokenNumber() == null || requestHelper.getCreditCardHelper().getTokenNumber().trim().equals("")) {

                                        if (requestHelper.getResultHelper().isTokenAuthorizeStatus() && requestHelper.getCreditCardHelper().getCcsaveStatus() != null && requestHelper.getCreditCardHelper().getCcsaveStatus().equalsIgnoreCase("yes")) {
                                            OneCheckoutLogger.log(":: REGISTER TO ACCOUNTBILLING ::");
                                            mPGTokenizationLocal.doTokenizationCustomerRegistration(requestHelper);
                                        }
                                    }
                                }
                                status = true;
                            } else {
                                OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT MPG");
                            }
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_THREE_D_SECURE.code()) {
                    if (requestParserLocal.doParsingThreeDSecure(requestHelper)) {
                        status = true;
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_THREE_D_SECURE_PROCESS.code()) {
                    MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.CreditCard);
                    if (merchantPaymentChannel != null) {
                        requestHelper.setTokenizationCorePaymentChannel(merchantPaymentChannel);
                        if (iPGTokenizationLocal.doTokenizationAuthPayment(requestHelper)) {
                            if (requestHelper.getCreditCardHelper().getTokenNumber() == null || requestHelper.getCreditCardHelper().getTokenNumber().trim().equals("")) {
                                if (requestHelper.getResultHelper().isTokenAuthorizeStatus() && requestHelper.getCreditCardHelper().getCcsaveStatus() != null && requestHelper.getCreditCardHelper().getCcsaveStatus().equalsIgnoreCase("yes")) {
                                    OneCheckoutLogger.log(":: REGISTER TO ACCOUNTBILLING AFTER 3D ::");
                                    iPGTokenizationLocal.doTokenizationCustomerRegistration(requestHelper);
                                }
                            }
                        }
                        status = true;
                    } else {
                        OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT IPG");
                        merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(requestHelper.getMerchantActivity().getMerchantIdx(), OneCheckoutPaymentChannel.BSP);
                        if (merchantPaymentChannel != null) {
                            requestHelper.setTokenizationCorePaymentChannel(merchantPaymentChannel);
                            if (mPGTokenizationLocal.doTokenizationAuthPayment(requestHelper)) {
                                if (requestHelper.getCreditCardHelper().getTokenNumber() == null || requestHelper.getCreditCardHelper().getTokenNumber().trim().equals("")) {
                                    if (requestHelper.getResultHelper().isTokenAuthorizeStatus() && requestHelper.getCreditCardHelper().getCcsaveStatus() != null && requestHelper.getCreditCardHelper().getCcsaveStatus().equalsIgnoreCase("yes")) {
                                        OneCheckoutLogger.log(":: REGISTER TO ACCOUNTBILLING AFTER 3D ::");
                                        mPGTokenizationLocal.doTokenizationCustomerRegistration(requestHelper);
                                    }
                                }
                            }
                            status = true;
                        } else {
                            OneCheckoutLogger.log("MERCHANT DOESN'T SUPPORT MPG");
                        }
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_TOKEN_UPDATE_CARD.code()) {
                    String button = requestHelper.getFilteredRequest().getParameter(EParameterName.BUTTON.code());
                    if (!button.equalsIgnoreCase("CANCEL")) {
                        if (requestParserLocal.doParsingTokenPaymentData(requestHelper)) {
                            updateTokenData(requestHelper);
                            requestHelper.getResultHelper().setCustomerInfoObject(getAccountBillingCustomerInfo(requestHelper));
                            status = true;
                        }
                    } else {
                        status = true;
                    }
                } else if (merchantActivity.getActivityId().getId() == EActivity.DO_TOKEN_DELETE_CARD.code()) {
                    if (requestParserLocal.doParsingTokenDeleteData(requestHelper)) {
                        deleteTokenData(requestHelper);
                        requestHelper.getResultHelper().setCustomerInfoObject(getAccountBillingCustomerInfo(requestHelper));
                        status = true;
                    }
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return status;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public MerchantActivity startMerchantActivity(RequestHelper requestHelper, Merchants merchants, EActivity eActivity, HashMap<String, String> data) {
        try {
            if (requestHelper != null && requestHelper.getMerchantActivity() != null) {
                requestHelper.getMerchantActivity().setStatus(EMerchantActivityStatus.FINISH.code());
                em.merge(requestHelper.getMerchantActivity());
            }
            if (eActivity != null) {
                OneCheckoutLogger.log("Starting Activity[" + eActivity.name() + "], code[" + eActivity.code() + "]");
                Activity activity = em.find(Activity.class, eActivity.code());
                MerchantActivity merchantActivity = new MerchantActivity();
                merchantActivity.setId(oneCheckoutV1QueryHelperBeanLocal.getNextValue(MerchantActivity.class));
                merchantActivity.setActivityId(activity);
                merchantActivity.setMerchantIdx(merchants);
                merchantActivity.setStartTime(new Date());
                merchantActivity.setStatus(EMerchantActivityStatus.START.code());
                merchantActivity.setData(data.toString());
                em.persist(merchantActivity);
                return merchantActivity;
            } else {
                return requestHelper.getMerchantActivity();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void endMerchantActtivity(RequestHelper requestHelper, boolean status) {
        MerchantActivity merchantActivity = requestHelper.getMerchantActivity();
        if (merchantActivity == null) {
            OneCheckoutLogger.log(": : : : : ACTIVITY NOT STARTED");
        } else {
            merchantActivity.setEndTime(new Date());
            merchantActivity.setStatus(status ? EMerchantActivityStatus.DONE_SUCCESS.code() : EMerchantActivityStatus.DONE_FAIL.code());
            if (requestHelper.getResultHelper().getSystemMessage() != null && !requestHelper.getResultHelper().getSystemMessage().trim().equals("")) {
                merchantActivity.setMessage(requestHelper.getResultHelper().getSystemMessage());
            }
            if (requestHelper.getResultHelper().getEMerchantActivityErrorCode() != null) {
                merchantActivity.setErrorCode(requestHelper.getResultHelper().getEMerchantActivityErrorCode().code());
            }
            em.merge(merchantActivity);
        }
    }

    public void processTemplate(RequestHelper requestHelper, Writer out) {
        try {
            Map root = new HashMap();
            String templatePath = OneCheckoutProperties.getOneCheckoutConfig().getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");
            String templateFileName = "error.html";
            if (requestHelper != null) {

//                if (requestHelper.getUseFailedPage()) {
//                    templateFileName = "failedPage.html";
//                }
                String subPath = getSubPath(requestHelper.getUnfilterRequest().getHeader("User-Agent").toUpperCase());
                templateFileName = subPath + requestHelper.getTemplateFileName();

                String custom = "";
                if (requestHelper.getMerchantActivity().getMerchantIdx().getMerchantChainMerchantCode() == 0) {
                    custom = requestHelper.getMerchantActivity().getMerchantIdx().getMerchantCode() + "";
                } else {
                    custom = requestHelper.getMerchantActivity().getMerchantIdx().getMerchantCode() + "_" + requestHelper.getMerchantActivity().getMerchantIdx().getMerchantChainMerchantCode();
                }

                File file = new File(templatePath + custom + "/" + templateFileName);
                boolean exists = file.exists();
                if (exists) {
                    templatePath = templatePath + custom;
                    OneCheckoutLogger.log(templatePath + "/" + templateFileName + " is exist");
                } else {
                    OneCheckoutLogger.log(templatePath + custom + "/" + templateFileName + " does not exist, so use default template");
                }

                templateFileName = requestHelper.getTemplateFileName();
                if (requestHelper.getMerchantActivity() != null) {
                    if (requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_UPDATE_CARD.code()
                            || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_REGISTRATION_REQUEST.code() || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_RECUR_CUSTOMER_UPDATE_REQUEST.code()
                            || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_REQUEST.code()
                            || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_DATA.code()
                            || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_DELETE_CARD.code()) {
                        root.put("listCountry", oneCheckoutV1QueryHelperBeanLocal.getListCountry());
                    }
                }
                root.put("requestHelper", requestHelper);
            }
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());
            Template temp = cfg.getTemplate(templateFileName);
            root.put("currentDate", OneCheckoutVerifyFormatData.recur_dateFormat.format(new Date()));
            temp.process(root, out);
            out.flush();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private static String getSubPath(String userAgent) {
//        if (userAgent == null) {
//            return "";
//        } else if (userAgent.indexOf("BLACKBERRY") >= 0) {
//            return "mob_";
//        } else if (userAgent.indexOf("IPHONE") >= 0) {
//            return "mob_";
//        } else if (userAgent.indexOf("ANDROID") >= 0) {
//            return "mob_";
//        } else {
//            return "";
//        }
        String mobVersion = OneCheckoutProperties.getOneCheckoutConfig().getString("ONECHECKOUTV1.MOBILE.VERSION");
        
        mobVersion = mobVersion.toUpperCase();
//        OneCheckoutLogger.log("mobversion ManageBean =* " + mobVersion);
//        OneCheckoutLogger.log("userAgent ManageBean =* " + userAgent);

        if (userAgent == null) {
            return "";
        }
        boolean mobOS = false;
        String[] listMob = mobVersion.split(";");
        for (String listMob1 : listMob) {
            if (userAgent.indexOf(listMob1) >= 0) {
                mobOS = true;
                break;
            }
        }
        if (mobOS) {
            return "mob_";
        } else {
            return "";
        }

    }

    public RequestHelper readRequestHelperFile(String fullPathFileName) {
        //ObjectInputStream inputStream = null;
        RequestHelper requestHelper = null;
        /*
         try {
         //Construct the ObjectInputStream object
         inputStream = new ObjectInputStream(new FileInputStream(fullPathFileName));
         Map<String, Object> datas = (Map<String, Object>) inputStream.readObject();
         if (datas != null && datas.size() > 0) {
         requestHelper = new RequestHelper();
         requestHelper.setMerchantActivity((MerchantActivity) datas.get("merchantActivity"));
         requestHelper.setRecurCorePaymentChannel((MerchantPaymentChannel) datas.get("recurCorePaymentChannel"));
         requestHelper.setMerchantPaymentChannel((MerchantPaymentChannel) datas.get("merchantPaymentChannel"));
         requestHelper.setRecurHelper((RecurHelper) datas.get("recurHelper"));
         requestHelper.setTokenizationHelper((TokenizationHelper) datas.get("tokenizationHelper"));
         requestHelper.setCreditCardHelper((CreditCardHelper) datas.get("creditCardHelper"));
         requestHelper.setResultHelper((ResultHelper) datas.get("resultHelper"));
         requestHelper.setPaymentParam((HashMap<String, String>) datas.get("paymentParam"));
         }
         } catch (Throwable ex) {
         ex.printStackTrace();
         } finally {
         //Close the ObjectInputStream
         try {
         if (inputStream != null) {
         inputStream.close();
         }
         } catch (Throwable ex) {
         ex.printStackTrace();
         }
         }

         try {
         File file = new File(fullPathFileName);
         file.delete();
         } catch (Throwable th) {
         th.printStackTrace();
         }
         */
        try {
            Session session = oneCheckoutV1QueryHelperBeanLocal.getSessionByRequestCode(fullPathFileName);
            if (session != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(session.getSession());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Map<String, Object> datas = (Map<String, Object>) ois.readObject();
                if (datas != null && datas.size() > 0) {
                    requestHelper = new RequestHelper();
                    requestHelper.setMerchantActivity((MerchantActivity) datas.get("merchantActivity"));
                    requestHelper.setRecurCorePaymentChannel((MerchantPaymentChannel) datas.get("recurCorePaymentChannel"));
                    requestHelper.setMerchantPaymentChannel((MerchantPaymentChannel) datas.get("merchantPaymentChannel"));
                    requestHelper.setRecurHelper((RecurHelper) datas.get("recurHelper"));
                    requestHelper.setTokenizationHelper((TokenizationHelper) datas.get("tokenizationHelper"));
                    requestHelper.setCreditCardHelper((CreditCardHelper) datas.get("creditCardHelper"));
                    requestHelper.setResultHelper((ResultHelper) datas.get("resultHelper"));
                    requestHelper.setPaymentParam((HashMap<String, String>) datas.get("paymentParam"));
                }
                oneCheckoutV1QueryHelperBeanLocal.deleteSessionById(session.getId());
            }
        } catch (Throwable th) {
        }

        return requestHelper;
    }

    public <T extends RequestHelper> boolean createRequestHelperFile(String fullPathFileName, T requestHelper) {
        boolean status = false;
        //ObjectOutputStream outputStream = null;
        try {
            Map<String, Object> datas = new HashMap<String, Object>();
            datas.put("merchantActivity", requestHelper.getMerchantActivity());
            datas.put("recurCorePaymentChannel", requestHelper.getRecurCorePaymentChannel());
            datas.put("merchantPaymentChannel", requestHelper.getMerchantPaymentChannel());
            datas.put("recurHelper", requestHelper.getRecurHelper());
            datas.put("tokenizationHelper", requestHelper.getTokenizationHelper());
            datas.put("creditCardHelper", requestHelper.getCreditCardHelper());
            datas.put("resultHelper", requestHelper.getResultHelper());
            datas.put("paymentParam", requestHelper.getPaymentParam());

            //Construct the LineNumberReader object
            //outputStream = new ObjectOutputStream(new FileOutputStream(fullPathFileName));
            //outputStream.writeObject(datas);
            OneCheckoutLogger.log("INSERT SESSION DATA...");
            Session session = new Session();
            session.setId(oneCheckoutV1QueryHelperBeanLocal.getNextValue(Session.class));
            session.setMerchantIdx(requestHelper.getMerchantActivity().getMerchantIdx());
            session.setRequestCode(fullPathFileName);
            session.setCreateDate(new Date());
            session.setStatus('A');
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(datas);
            oos.close();
            session.setSession(baos.toByteArray());
            em.persist(session);
            OneCheckoutLogger.log("SUCCESS INSERT SESSION DATA...");
            status = true;
        } catch (Throwable ex) {
            ex.printStackTrace();
        } /*finally {
         //Close the ObjectOutputStream
         try {
         if (outputStream != null) {
         outputStream.flush();
         outputStream.close();
         }
         } catch (Throwable ex) {
         ex.printStackTrace();
         }
         }*/

        return status;
    }

    private void deleteTokenData(RequestHelper requestHelper) {
        try {
            requestHelper.setPaymentParam(new HashMap<String, String>());
            // MERCHANT INFORMATION
            requestHelper.getPaymentParam().put("MALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "");
            if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
            }
            // CUSTOMER INFORMATION
            requestHelper.getPaymentParam().put("CUSTOMERID", requestHelper.getTokenizationHelper().getCustomerId());
            requestHelper.getPaymentParam().put("IDENTIFIERTYPE", "T");
            requestHelper.getPaymentParam().put("IDENTIFIERNO", requestHelper.getTokenizationHelper().getTokenId());
            requestHelper.getPaymentParam().put("REQUESTTYPE", "C");
            //REQUEST DATE TIME
            String reqDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            requestHelper.getPaymentParam().put("REQUESTDATETIME", reqDateTime);
            //WORDS
            StringBuilder sbWords = new StringBuilder();
            sbWords.append(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode());
            sbWords.append(requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            sbWords.append(requestHelper.getTokenizationHelper().getCustomerId());
            sbWords.append("T");
            sbWords.append(requestHelper.getTokenizationHelper().getTokenId());
            sbWords.append("C");
            sbWords.append(reqDateTime);
            String hashwords = HashWithSHA1.doHashing(sbWords.toString(), "SHA1", null);
            requestHelper.getPaymentParam().put("WORDS", hashwords);

            //OneCheckoutLogger.log("TOKENIZATION_DELETE_TOKEN_REQUEST :\n" + requestHelper.getPaymentParam().toString());
            String url = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.URL.DeleteTokenData", null);
            OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
            String resultUpdate = oneCheckoutChannelBase.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
            if (resultUpdate != null && resultUpdate.length() > 0 && !resultUpdate.equalsIgnoreCase("ERROR")) {
                //OneCheckoutLogger.log("TOKENIZATION_DELETE_TOKEN_REQUEST :\n" + resultUpdate);
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(resultUpdate);
                xml.load(sr);
                String result = xml.getString("RESULTMSG") != null ? xml.getString("RESULTMSG").trim() : "";
                if (result.equalsIgnoreCase("SUCCESS")) {
                    requestHelper.getResultHelper().setTokenDeleteStatus(true);
                    requestHelper.getResultHelper().setSystemMessage("Delete Card Success");
                } else {
                    requestHelper.getResultHelper().setSystemMessage("Delete Card Failed");
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private void updateTokenData(RequestHelper requestHelper) {
        try {
            requestHelper.setPaymentParam(new HashMap<String, String>());
            // MERCHANT INFORMATION
            requestHelper.getPaymentParam().put("MALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "");
            if (requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                requestHelper.getPaymentParam().put("CHAINMALLID", requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
            }
            // CUSTOMER INFORMATION
            requestHelper.getPaymentParam().put("CUSTOMERID", requestHelper.getTokenizationHelper().getCustomerId());
            // CARD INFORMATION
            requestHelper.getPaymentParam().put("TOKENID", requestHelper.getCreditCardHelper().getTokenNumber());
            requestHelper.getPaymentParam().put("CARDNUMBER", requestHelper.getCreditCardHelper().getCardNumber());
            requestHelper.getPaymentParam().put("EXPIRYDATE", requestHelper.getCreditCardHelper().getExpYear() + requestHelper.getCreditCardHelper().getExpMonth());
            requestHelper.getPaymentParam().put("CVV2", requestHelper.getCreditCardHelper().getCvv2());
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
            //REQUEST DATE TIME
            String reqDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            requestHelper.getPaymentParam().put("REQUESTDATETIME", reqDateTime);
            //WORDS
            StringBuilder sbWords = new StringBuilder();
            sbWords.append(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode());
            sbWords.append(requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            sbWords.append(requestHelper.getTokenizationHelper().getCustomerId());
            sbWords.append(requestHelper.getTokenizationHelper().getTokenId());
            sbWords.append(reqDateTime);
            String hashwords = HashWithSHA1.doHashing(sbWords.toString(), "SHA1", null);
            requestHelper.getPaymentParam().put("WORDS", hashwords);

            //OneCheckoutLogger.log("TOKENIZATION_UPDATE_TOKEN_REQUEST :\n" + requestHelper.getPaymentParam().toString());
            String url = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.URL.UpdateTokenData", null);
            OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
            String resultUpdate = oneCheckoutChannelBase.postMIP(requestHelper.getPaymentParam(), url, requestHelper.getMerchantPaymentChannel().getPaymentChannel());
            if (resultUpdate != null && resultUpdate.length() > 0 && !resultUpdate.equalsIgnoreCase("ERROR")) {
                //OneCheckoutLogger.log("TOKENIZATION_UPDATE_TOKEN_RESPONSE :\n" + resultUpdate);
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(resultUpdate);
                xml.load(sr);
                String result = xml.getString("RESULTMSG") != null ? xml.getString("RESULTMSG").trim() : "";
                if (result.equalsIgnoreCase("SUCCESS")) {
                    requestHelper.getResultHelper().setTokenUpdateStatus(true);
                    requestHelper.getResultHelper().setSystemMessage("Update Card Success");
                } else {
                    requestHelper.getResultHelper().setSystemMessage("Update Card Failed");
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private CustomerInfoObject getAccountBillingCustomerInfo(RequestHelper requestHelper) {
        CustomerInfoObject customerInfoObject = null;
        try {
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            HashMap<String, String> params = new HashMap<String, String>();
            String reqDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            StringBuffer sbWords = new StringBuffer();
            sbWords.append(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "");
            sbWords.append(requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            sbWords.append("C");
            String customerId = "";
            if (requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_REQUEST.code() || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_UPDATE_CARD.code() || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_DELETE_CARD.code()) {
                customerId = requestHelper.getTokenizationHelper().getCustomerId();
            } else {
                customerId = requestHelper.getRecurHelper().getCustomerId();
            }
            sbWords.append("Q");
            sbWords.append(reqDateTime);
            String hashwords = HashWithSHA1.doHashing(sbWords.toString(), "SHA1", null);
            StringBuilder sb = new StringBuilder("");
            sb.append("MALLID=").append(URLEncoder.encode(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode() + "", "UTF-8"));
            sb.append("&CHAINMALLID=").append(URLEncoder.encode(requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode() + "", "UTF-8"));
            sb.append("&IDENTIFIERTYPE=").append(URLEncoder.encode("C", "UTF-8"));
            sb.append("&IDENTIFIERNO=").append(URLEncoder.encode(customerId, "UTF-8"));
            sb.append("&REQUESTTYPE=").append(URLEncoder.encode("Q", "UTF-8"));
            sb.append("&REQUESTDATETIME=").append(URLEncoder.encode(reqDateTime, "UTF-8"));
            sb.append("&WORDS=").append(URLEncoder.encode(hashwords, "UTF-8"));
            String urlGetInfo = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.GETINFO", null);
            OneCheckoutLogger.log("::: ACCOUNT BILLING GET CUSTOMER INFO :::");
            OneCheckoutLogger.log("Parameter    : \n" + sb.toString());
            OneCheckoutLogger.log("send to      : \n" + urlGetInfo);
            InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo, 30, 30);
            String resp = intResp.getMsgResponse();
            OneCheckoutLogger.log("Response AB  : \n%s", resp);
            if (resp.startsWith("<RESPONSE")) {
                RESPONSEDocument rESPONSEDocument = RESPONSEDocument.Factory.parse(resp);
                if (rESPONSEDocument != null) {
                    customerInfoObject = new CustomerInfoObject();
                    customerInfoObject.setCustomeraddress(rESPONSEDocument.getRESPONSE().getCUSTOMERADDRESS() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERADDRESS().trim() : "");
                    customerInfoObject.setCustomercity(rESPONSEDocument.getRESPONSE().getCUSTOMERCITY() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERCITY().trim() : "");
                    customerInfoObject.setCustomercountry(rESPONSEDocument.getRESPONSE().getCUSTOMERCOUNTRY() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERCOUNTRY().trim() : "");
                    customerInfoObject.setCustomeremail(rESPONSEDocument.getRESPONSE().getCUSTOMEREMAIL() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMEREMAIL().trim() : "");
                    customerInfoObject.setCustomerhomephone(rESPONSEDocument.getRESPONSE().getCUSTOMERHOMEPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERHOMEPHONE().trim() : "");
                    customerInfoObject.setCustomerid(rESPONSEDocument.getRESPONSE().getCUSTOMERID() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERID().trim() : "");
                    customerInfoObject.setCustomermobilephone(rESPONSEDocument.getRESPONSE().getCUSTOMERMOBILEPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERMOBILEPHONE().trim() : "");
                    customerInfoObject.setCustomername(rESPONSEDocument.getRESPONSE().getCUSTOMERNAME() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERNAME().trim() : "");
                    customerInfoObject.setCustomerstate(rESPONSEDocument.getRESPONSE().getCUSTOMERSTATE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERSTATE().trim() : "");
                    customerInfoObject.setCustomerworkphone(rESPONSEDocument.getRESPONSE().getCUSTOMERWORKPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERWORKPHONE().trim() : "");
                    customerInfoObject.setCustomerzipcode(rESPONSEDocument.getRESPONSE().getCUSTOMERZIPCODE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERZIPCODE().trim() : "");
                    customerInfoObject.setIdentifiertype(rESPONSEDocument.getRESPONSE().getIDENTIFIERTYPE() != null ? rESPONSEDocument.getRESPONSE().getIDENTIFIERTYPE().trim() : "");
                    customerInfoObject.setMallid(rESPONSEDocument.getRESPONSE().getMALLID() != null ? rESPONSEDocument.getRESPONSE().getMALLID().trim() : "");
                    customerInfoObject.setRequestdatetime(rESPONSEDocument.getRESPONSE().getREQUESTDATETIME() != null ? rESPONSEDocument.getRESPONSE().getREQUESTDATETIME().trim() : "");

                    CustomerTokenObject customerTokenObject = null;
                    CustomerBillObject customerBillObject = null;
                    if (rESPONSEDocument.getRESPONSE().getTOKENS() != null && rESPONSEDocument.getRESPONSE().getTOKENS().getTOKENArray() != null && rESPONSEDocument.getRESPONSE().getTOKENS().getTOKENArray().length > 0) {
                        for (TOKEN token : rESPONSEDocument.getRESPONSE().getTOKENS().getTOKENArray()) {
                            if (token.getTOKENSTATUS() != null && token.getTOKENSTATUS().trim().equalsIgnoreCase("ACTIVE")) {
                                customerTokenObject = new CustomerTokenObject();
                                customerTokenObject.setCardnumber(token.getCARDNUMBER() != null ? token.getCARDNUMBER().trim() : "");
                                customerTokenObject.setCcaddress(token.getCCADDRESS() != null ? token.getCCADDRESS().trim() : "");
                                customerTokenObject.setCccity(token.getCCCITY() != null ? token.getCCCITY().trim() : "");
                                customerTokenObject.setCccountry(token.getCCCOUNTRY() != null ? token.getCCCOUNTRY().trim() : "");
                                customerTokenObject.setCcemail(token.getCCEMAIL() != null ? token.getCCEMAIL().trim() : "");
                                customerTokenObject.setCchomephone(token.getCCHOMEPHONE() != null ? token.getCCHOMEPHONE().trim() : "");
                                customerTokenObject.setCcmobilephone(token.getCCMOBILEPHONE() != null ? token.getCCMOBILEPHONE().trim() : "");
                                customerTokenObject.setCcname(token.getCCNAME() != null ? token.getCCNAME().trim() : "");
                                customerTokenObject.setCcstate(token.getCCSTATE() != null ? token.getCCSTATE().trim() : "");
                                customerTokenObject.setCcworkphone(token.getCCWORKPHONE() != null ? token.getCCWORKPHONE().trim() : "");
                                customerTokenObject.setCczipcode(token.getCCZIPCODE() != null ? token.getCCZIPCODE().trim() : "");
                                customerTokenObject.setExpirydate(token.getEXPIRYDATE() != null ? token.getEXPIRYDATE().trim() : "");
                                customerTokenObject.setTokenid(token.getTOKENID() != null ? token.getTOKENID().trim() : "");
                                customerTokenObject.setTokenstatus(token.getTOKENSTATUS() != null ? token.getTOKENSTATUS().trim() : "");

                                if (token.getBILLS() != null && token.getBILLS().getBILLArray() != null && token.getBILLS().getBILLArray().length > 0) {
                                    for (BILL bill : token.getBILLS().getBILLArray()) {
                                        customerBillObject = new CustomerBillObject();
                                        customerBillObject.setAmount(bill.getAMOUNT() != null ? bill.getAMOUNT().trim() : "");
                                        customerBillObject.setBilldetail(bill.getBILLDETAIL() != null ? bill.getBILLDETAIL().trim() : "");
                                        customerBillObject.setBillnumber(bill.getBILLNUMBER() != null ? bill.getBILLNUMBER().trim() : "");
                                        customerBillObject.setBillstatus(bill.getBILLSTATUS() != null ? bill.getBILLSTATUS().trim() : "");
                                        customerBillObject.setBilltype(bill.getBILLTYPE() != null ? bill.getBILLTYPE().trim() : "");
                                        customerBillObject.setCurrency(bill.getCURRENCY() != null ? bill.getCURRENCY().trim() : "");
                                        customerBillObject.setEnddate(bill.getENDDATE() != null ? bill.getENDDATE().trim() : "");
                                        customerBillObject.setExecutedate(bill.getEXECUTEDATE() != null ? bill.getEXECUTEDATE().trim() : "");
                                        customerBillObject.setExecutemonth(bill.getEXECUTEMONTH() != null ? bill.getEXECUTEMONTH().trim() : "");
                                        customerBillObject.setExecutetype(bill.getEXECUTETYPE() != null ? bill.getEXECUTETYPE().trim() : "");
                                        customerBillObject.setFlatstatus(bill.getFLATSTATUS() != null ? bill.getFLATSTATUS().trim() : "");
                                        customerBillObject.setStartdate(bill.getSTARTDATE() != null ? bill.getSTARTDATE().trim() : "");
                                        customerTokenObject.getBills().add(customerBillObject);
                                    }
                                }
                                customerInfoObject.getTokens().add(customerTokenObject);
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return customerInfoObject;
    }

    private CustomerInfoObject getInfoCustomerRecurbyToken(RequestHelper requestHelper) {
        CustomerInfoObject customerInfoObject = null;
        try {
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            HashMap<String, String> params = new HashMap<String, String>();
            String reqDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            StringBuilder sbWords = new StringBuilder();
            sbWords.append(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode());
            sbWords.append(requestHelper.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            sbWords.append("C");
            sbWords.append(requestHelper.getTokenizationHelper().getCustomerId());
            sbWords.append("Q");
            sbWords.append(reqDateTime);
            String hashwords = HashWithSHA1.doHashing(sbWords.toString(), "SHA1", null);
            StringBuilder sb = new StringBuilder("");
            sb.append("MALLID=").append(requestHelper.getMerchantPaymentChannel().getPaymentChannelCode());
            sb.append("&CHAINMALLID=").append(requestHelper.getMerchantPaymentChannel().getPaymentChannelChainCode());
            sb.append("&IDENTIFIERTYPE=").append("T");
            sb.append("&IDENTIFIERNO=").append(requestHelper.getCreditCardHelper().getTokenNumber());
            sb.append("&REQUESTTYPE=").append("Q");
            sb.append("&REQUESTDATETIME=").append(reqDateTime);
            sb.append("&WORDS=").append(hashwords);

            String urlGetInfo = OneCheckoutProperties.getOneCheckoutConfig().getString("RECUR.URL.GETINFObyTOKEN", null);
            //OneCheckoutLogger.log("::: ACCOUNT BILLING DoServiceGetInfo :::");
            //OneCheckoutLogger.log("Parameter                    : \n" + sb.toString());
            //OneCheckoutLogger.log("send to                      : \n" + urlGetInfo);
            InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo, 30, 30);
            String resp = intResp.getMsgResponse();
            //OneCheckoutLogger.log("Response DoServiceGetInfo    : \n%s", resp);
            if (resp.startsWith("<RESPONSE")) {
                RESPONSEDocument rESPONSEDocument = RESPONSEDocument.Factory.parse(resp);
                if (rESPONSEDocument != null) {
                    customerInfoObject = new CustomerInfoObject();
                    customerInfoObject.setCustomeraddress(rESPONSEDocument.getRESPONSE().getCUSTOMERADDRESS() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERADDRESS().trim() : "");
                    customerInfoObject.setCustomercity(rESPONSEDocument.getRESPONSE().getCUSTOMERCITY() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERCITY().trim() : "");
                    customerInfoObject.setCustomercountry(rESPONSEDocument.getRESPONSE().getCUSTOMERCOUNTRY() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERCOUNTRY().trim() : "");
                    customerInfoObject.setCustomeremail(rESPONSEDocument.getRESPONSE().getCUSTOMEREMAIL() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMEREMAIL().trim() : "");
                    customerInfoObject.setCustomerhomephone(rESPONSEDocument.getRESPONSE().getCUSTOMERHOMEPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERHOMEPHONE().trim() : "");
                    customerInfoObject.setCustomerid(rESPONSEDocument.getRESPONSE().getCUSTOMERID() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERID().trim() : "");
                    customerInfoObject.setCustomermobilephone(rESPONSEDocument.getRESPONSE().getCUSTOMERMOBILEPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERMOBILEPHONE().trim() : "");
                    customerInfoObject.setCustomername(rESPONSEDocument.getRESPONSE().getCUSTOMERNAME() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERNAME().trim() : "");
                    customerInfoObject.setCustomerstate(rESPONSEDocument.getRESPONSE().getCUSTOMERSTATE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERSTATE().trim() : "");
                    customerInfoObject.setCustomerworkphone(rESPONSEDocument.getRESPONSE().getCUSTOMERWORKPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERWORKPHONE().trim() : "");
                    customerInfoObject.setCustomerzipcode(rESPONSEDocument.getRESPONSE().getCUSTOMERZIPCODE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERZIPCODE().trim() : "");
                    customerInfoObject.setIdentifiertype(rESPONSEDocument.getRESPONSE().getIDENTIFIERTYPE() != null ? rESPONSEDocument.getRESPONSE().getIDENTIFIERTYPE().trim() : "");
                    customerInfoObject.setMallid(rESPONSEDocument.getRESPONSE().getMALLID() != null ? rESPONSEDocument.getRESPONSE().getMALLID().trim() : "");
                    customerInfoObject.setRequestdatetime(rESPONSEDocument.getRESPONSE().getREQUESTDATETIME() != null ? rESPONSEDocument.getRESPONSE().getREQUESTDATETIME().trim() : "");

                    CustomerTokenObject customerTokenObject = null;
                    CustomerBillObject customerBillObject = null;
                    if (rESPONSEDocument.getRESPONSE().getTOKENS() != null && rESPONSEDocument.getRESPONSE().getTOKENS().getTOKENArray() != null && rESPONSEDocument.getRESPONSE().getTOKENS().getTOKENArray().length > 0) {
                        for (TOKEN token : rESPONSEDocument.getRESPONSE().getTOKENS().getTOKENArray()) {
                            if (token.getTOKENSTATUS() != null && token.getTOKENSTATUS().trim().equalsIgnoreCase("ACTIVE")) {
                                customerTokenObject = new CustomerTokenObject();
                                customerTokenObject.setCardnumber(token.getCARDNUMBER() != null ? token.getCARDNUMBER().trim() : "");
                                customerTokenObject.setCcaddress(token.getCCADDRESS() != null ? token.getCCADDRESS().trim() : "");
                                customerTokenObject.setCccity(token.getCCCITY() != null ? token.getCCCITY().trim() : "");
                                customerTokenObject.setCccountry(token.getCCCOUNTRY() != null ? token.getCCCOUNTRY().trim() : "");
                                customerTokenObject.setCcemail(token.getCCEMAIL() != null ? token.getCCEMAIL().trim() : "");
                                customerTokenObject.setCchomephone(token.getCCHOMEPHONE() != null ? token.getCCHOMEPHONE().trim() : "");
                                customerTokenObject.setCcmobilephone(token.getCCMOBILEPHONE() != null ? token.getCCMOBILEPHONE().trim() : "");
                                customerTokenObject.setCcname(token.getCCNAME() != null ? token.getCCNAME().trim() : "");
                                customerTokenObject.setCcstate(token.getCCSTATE() != null ? token.getCCSTATE().trim() : "");
                                customerTokenObject.setCcworkphone(token.getCCWORKPHONE() != null ? token.getCCWORKPHONE().trim() : "");
                                customerTokenObject.setCczipcode(token.getCCZIPCODE() != null ? token.getCCZIPCODE().trim() : "");
                                customerTokenObject.setExpirydate(token.getEXPIRYDATE() != null ? token.getEXPIRYDATE().trim() : "");
                                customerTokenObject.setTokenid(token.getTOKENID() != null ? token.getTOKENID().trim() : "");
                                customerTokenObject.setTokenstatus(token.getTOKENSTATUS() != null ? token.getTOKENSTATUS().trim() : "");

                                if (token.getBILLS() != null && token.getBILLS().getBILLArray() != null && token.getBILLS().getBILLArray().length > 0) {
                                    for (BILL bill : token.getBILLS().getBILLArray()) {
                                        customerBillObject = new CustomerBillObject();
                                        customerBillObject.setAmount(bill.getAMOUNT() != null ? bill.getAMOUNT().trim() : "");
                                        customerBillObject.setBilldetail(bill.getBILLDETAIL() != null ? bill.getBILLDETAIL().trim() : "");
                                        customerBillObject.setBillnumber(bill.getBILLNUMBER() != null ? bill.getBILLNUMBER().trim() : "");
                                        customerBillObject.setBillstatus(bill.getBILLSTATUS() != null ? bill.getBILLSTATUS().trim() : "");
                                        customerBillObject.setBilltype(bill.getBILLTYPE() != null ? bill.getBILLTYPE().trim() : "");
                                        customerBillObject.setCurrency(bill.getCURRENCY() != null ? bill.getCURRENCY().trim() : "");
                                        customerBillObject.setEnddate(bill.getENDDATE() != null ? bill.getENDDATE().trim() : "");
                                        customerBillObject.setExecutedate(bill.getEXECUTEDATE() != null ? bill.getEXECUTEDATE().trim() : "");
                                        customerBillObject.setExecutemonth(bill.getEXECUTEMONTH() != null ? bill.getEXECUTEMONTH().trim() : "");
                                        customerBillObject.setExecutetype(bill.getEXECUTETYPE() != null ? bill.getEXECUTETYPE().trim() : "");
                                        customerBillObject.setFlatstatus(bill.getFLATSTATUS() != null ? bill.getFLATSTATUS().trim() : "");
                                        customerBillObject.setStartdate(bill.getSTARTDATE() != null ? bill.getSTARTDATE().trim() : "");
                                        customerTokenObject.getBills().add(customerBillObject);
                                    }
                                }
                                customerInfoObject.getTokens().add(customerTokenObject);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return customerInfoObject;
    }
}
