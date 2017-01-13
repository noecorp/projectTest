/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.parser;

import com.google.gson.Gson;
import com.onecheckoutV1.ejb.helper.CreditCardHelper;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.RecurHelper;
import com.onecheckoutV1.ejb.helper.RequestHelper;
import com.onecheckoutV1.ejb.helper.ThreeDSecureHelper;
import com.onecheckoutV1.ejb.helper.TokenizationHelper;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.enums.EActivity;
import com.onecheckoutV1.enums.EMerchantActivityErrorCode;
import com.onecheckoutV1.enums.EParameterName;
import com.onechekoutv1.dto.Merchants;
import java.util.HashMap;
import java.util.Set;
import javax.ejb.Stateless;

/**
 *
 * @author Opiks
 */
@Stateless
public class RequestParserBean implements RequestParserLocal {

    public <T extends RequestHelper> boolean doParsingTokenPaymentRequest(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }

                // UNFILTERED REQUEST
                HashMap<String, String> unfilterParams = new HashMap<String, String>();
                Set m1 = requestHelper.getUnfilterRequest().getParameterMap().keySet();
                for (Object o : m1) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getUnfilterRequest().getParameter(key);
                    unfilterParams.put(key, val);
                }

                TokenizationHelper tokenizationHelper = new TokenizationHelper();

                // =============================================================
                String paymentChannelId = filteredParams.remove(EParameterName.PAYMENT_CHANNEL.code());
                if (paymentChannelId == null) {
                    paymentChannelId = "";
                }
                if (!paymentChannelId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PAYMENT_CHANNEL.code() + "[" + paymentChannelId.trim() + "]");
                    tokenizationHelper.setPaymentChannelId(paymentChannelId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.PAYMENT_CHANNEL.code() : ", " + EParameterName.PAYMENT_CHANNEL.code());
                }

                // =============================================================
                String mallId = filteredParams.remove(EParameterName.MALL_ID.code());
                if (mallId == null) {
                    mallId = "";
                }
                if (!mallId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.MALL_ID.code() + "[" + mallId.trim() + "]");
                    tokenizationHelper.setMallId(Integer.valueOf(mallId.trim()));
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.MALL_ID.code() : ", " + EParameterName.MALL_ID.code());
                }

                // =============================================================
                String chainMallId = filteredParams.remove(EParameterName.CHAIN_MALL_ID.code());
                if (chainMallId == null) {
                    chainMallId = "";
                }
                if (!chainMallId.trim().equals("") && !chainMallId.trim().equalsIgnoreCase("NA")) {
                    OneCheckoutLogger.log(EParameterName.CHAIN_MALL_ID.code() + "[" + chainMallId.trim() + "]");
                    tokenizationHelper.setChainMallId(Integer.valueOf(chainMallId.trim()));
                }

                // =============================================================
                String amount = filteredParams.remove(EParameterName.AMOUNT.code());
                if (amount == null) {
                    amount = "";
                }
                if (!amount.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.AMOUNT.code() + "[" + amount.trim() + "]");
                    tokenizationHelper.setAmount(Double.valueOf(amount.trim()));
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.AMOUNT.code() : ", " + EParameterName.AMOUNT.code());
                }

                // =============================================================
                String currency = filteredParams.remove(EParameterName.CURRENCY.code());
                if (currency == null) {
                    currency = "";
                }
                if (!currency.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CURRENCY.code() + "[" + currency.trim() + "]");
                    tokenizationHelper.setCurrency(currency.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.CURRENCY.code() : ", " + EParameterName.CURRENCY.code());
                }

                // =============================================================
                String purchaseAmount = filteredParams.remove(EParameterName.PURCHASE_AMOUNT.code());
                if (purchaseAmount == null) {
                    purchaseAmount = "";
                }
                if (!purchaseAmount.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PURCHASE_AMOUNT.code() + "[" + purchaseAmount.trim() + "]");
                    tokenizationHelper.setPurchaseAmount(Double.valueOf(purchaseAmount.trim()));
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.PURCHASE_AMOUNT.code() : ", " + EParameterName.PURCHASE_AMOUNT.code());
                }

                // =============================================================
                String purchaseCurrency = filteredParams.remove(EParameterName.PURCHASE_CURRENCY.code());
                if (purchaseCurrency == null) {
                    purchaseCurrency = "";
                }
                if (!purchaseCurrency.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PURCHASE_CURRENCY.code() + "[" + purchaseCurrency.trim() + "]");
                    tokenizationHelper.setPurchaseCurrency(purchaseCurrency.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.PURCHASE_CURRENCY.code() : ", " + EParameterName.PURCHASE_CURRENCY.code());
                }

                // =============================================================
                String sessionId = filteredParams.remove(EParameterName.SESSION_ID.code());
                if (sessionId == null) {
                    sessionId = "";
                }
                if (!sessionId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.SESSION_ID.code() + "[" + sessionId.trim() + "]");
                    tokenizationHelper.setSessionId(sessionId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.SESSION_ID.code() : ", " + EParameterName.SESSION_ID.code());
                }

                // =============================================================
                String words = filteredParams.remove(EParameterName.WORDS.code());
                if (words == null) {
                    words = "";
                }
                if (!words.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.WORDS.code() + "[" + words.trim() + "]");
                    tokenizationHelper.setWords(words.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.WORDS.code() : ", " + EParameterName.WORDS.code());
                }

                // =============================================================
                String requestDateTime = filteredParams.remove(EParameterName.REQUEST_DATE_TIME.code());
                if (requestDateTime == null) {
                    requestDateTime = "";
                }
                if (!requestDateTime.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.REQUEST_DATE_TIME.code() + "[" + requestDateTime.trim() + "]");
                    tokenizationHelper.setRequestDateTime(requestDateTime.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.REQUEST_DATE_TIME.code() : ", " + EParameterName.REQUEST_DATE_TIME.code());
                }

                // =============================================================
                String customerId = filteredParams.remove(EParameterName.CUSTOMER_ID.code());
                if (customerId == null) {
                    customerId = "";
                }
                if (!customerId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ID.code() + "[" + customerId.trim() + "]");
                    tokenizationHelper.setCustomerId(customerId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.CUSTOMER_ID.code() : ", " + EParameterName.CUSTOMER_ID.code());
                }

                // =============================================================
                String customerName = filteredParams.remove(EParameterName.CUSTOMER_NAME.code());
                if (customerName == null) {
                    customerName = "";
                }
                if (!customerName.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_NAME.code() + "[" + customerName.trim() + "]");
                    tokenizationHelper.setCustomerName(customerName.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.CUSTOMER_NAME.code() : ", " + EParameterName.CUSTOMER_NAME.code());
                }

                // =============================================================
                String customerEmail = unfilterParams.remove(EParameterName.CUSTOMER_EMAIL.code());
                if (customerEmail == null) {
                    customerEmail = "";
                }
                if (!customerEmail.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_EMAIL.code() + "[" + customerEmail.trim() + "]");
                    tokenizationHelper.setCustomerEmail(customerEmail.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.CUSTOMER_EMAIL.code() : ", " + EParameterName.CUSTOMER_EMAIL.code());
                }

                // =============================================================
                String customerAddress = filteredParams.remove(EParameterName.CUSTOMER_ADDRESS.code());
                if (customerAddress == null) {
                    customerAddress = "";
                }
                if (!customerAddress.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ADDRESS.code() + "[" + customerAddress.trim() + "]");
                    tokenizationHelper.setCustomerAddress(customerAddress.trim());
                }

                // =============================================================
                String customerCity = filteredParams.remove(EParameterName.CUSTOMER_CITY.code());
                if (customerCity == null) {
                    customerCity = "";
                }
                if (!customerCity.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_CITY.code() + "[" + customerCity.trim() + "]");
                    tokenizationHelper.setCustomerCity(customerCity.trim());
                }

                // =============================================================
                String customerState = filteredParams.remove(EParameterName.CUSTOMER_STATE.code());
                if (customerState == null) {
                    customerState = "";
                }
                if (!customerState.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_STATE.code() + "[" + customerState.trim() + "]");
                    tokenizationHelper.setCustomerState(customerState.trim());
                }

                // =============================================================
                String customerCountry = filteredParams.remove(EParameterName.CUSTOMER_COUNTRY.code());
                if (customerCountry == null) {
                    customerCountry = "";
                }
                if (!customerCountry.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_COUNTRY.code() + "[" + customerCountry.trim() + "]");
                    tokenizationHelper.setCustomerCountry(customerCountry.trim());
                }

                // =============================================================
                String customerZipCode = filteredParams.remove(EParameterName.CUSTOMER_ZIP_CODE.code());
                if (customerZipCode == null) {
                    customerZipCode = "";
                }
                if (!customerZipCode.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ZIP_CODE.code() + "[" + customerZipCode.trim() + "]");
                    tokenizationHelper.setCustomerZipCode(customerZipCode.trim());
                }

                // =============================================================
                String customerHomePhone = filteredParams.remove(EParameterName.CUSTOMER_HOME_PHONE.code());
                if (customerHomePhone == null) {
                    customerHomePhone = "";
                }
                if (!customerHomePhone.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_HOME_PHONE.code() + "[" + customerHomePhone.trim() + "]");
                    tokenizationHelper.setCustomerHomePhone(customerHomePhone.trim());
                }

                // =============================================================
                String customerMobilePhone = filteredParams.remove(EParameterName.CUSTOMER_MOBILE_PHONE.code());
                if (customerMobilePhone == null) {
                    customerMobilePhone = "";
                }
                if (!customerMobilePhone.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_MOBILE_PHONE.code() + "[" + customerMobilePhone.trim() + "]");
                    tokenizationHelper.setCustomerMobilePhone(customerMobilePhone.trim());
                }

                // =============================================================
                String customerWorkPhone = filteredParams.remove(EParameterName.CUSTOMER_WORK_PHONE.code());
                if (customerWorkPhone == null) {
                    customerWorkPhone = "";
                }
                if (!customerWorkPhone.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_WORK_PHONE.code() + "[" + customerWorkPhone.trim() + "]");
                    tokenizationHelper.setCustomerWorkPhone(customerWorkPhone.trim());
                }

                // =============================================================
                String clientIpAddress = filteredParams.remove(EParameterName.CUSTOMER_IP.code());
                if (clientIpAddress == null || clientIpAddress.trim().equals("")) {
                    clientIpAddress = requestHelper.getUnfilterRequest().getRemoteHost();
                    if (clientIpAddress == null) {
                        clientIpAddress = "";
                    }
                }
                if (!clientIpAddress.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_IP.code() + "[" + clientIpAddress.trim() + "]");
                    tokenizationHelper.setClientIp(clientIpAddress.trim());
                }

                // =============================================================
                String invoiceNo = unfilterParams.remove(EParameterName.INVOICE_NUMBER.code());
                if (invoiceNo == null) {
                    invoiceNo = "";
                }

                if (!invoiceNo.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.INVOICE_NUMBER.code() + "[" + invoiceNo.trim() + "]");
                    tokenizationHelper.setInvoiceNumber(invoiceNo.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Tokenization Get Info Customer Request : " + EParameterName.INVOICE_NUMBER.code() : ", " + EParameterName.INVOICE_NUMBER.code());
                }
                // =============================================================
                String basket = unfilterParams.remove(EParameterName.BASKET.code());
                if (basket == null) {
                    basket = "";
                }
                if (!basket.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.BASKET.code() + "[" + basket.trim() + "]");
                    boolean setBasketStatus = tokenizationHelper.setBasket(basket, Double.valueOf(amount), invoiceNo);
                    if (!setBasketStatus) {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Error Parsing Basket : " + EParameterName.BASKET.code() : ", " + EParameterName.BASKET.code());
                    }
                }

                // =============================================================
                if (filteredParams != null) {
                    tokenizationHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    tokenizationHelper.setParseStatus(true);
                }
                requestHelper.setTokenizationHelper(tokenizationHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_TOKEN_PAYMENT_REQUEST);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing token payment request from merchant");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_TOKEN_PAYMENT_REQUEST);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing token payment from merchant");
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doParsingTokenPaymentData(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }

                // UNFILTERED REQUEST
                HashMap<String, String> unfilterParams = new HashMap<String, String>();
                Set m1 = requestHelper.getUnfilterRequest().getParameterMap().keySet();
                for (Object o : m1) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getUnfilterRequest().getParameter(key);
                    unfilterParams.put(key, val);
                }

                CreditCardHelper creditCardHelper = new CreditCardHelper();

                // =============================================================
                String button = filteredParams.remove(EParameterName.BUTTON.code());
                if (button == null) {
                    button = "";
                }
                if (!button.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.BUTTON.code() + "[" + button.trim() + "]");
                    creditCardHelper.setButton(button.trim());
                }

                // =============================================================
                String tokenNumber = filteredParams.get(EParameterName.TOKEN_NUMBER.code());
                if (tokenNumber == null) {
                    tokenNumber = "";
                }
                if (!tokenNumber.trim().equals("") && requestHelper.getMerchantActivity().getActivityId().getId() != EActivity.DO_TOKEN_UPDATE_CARD.code()) {
                    OneCheckoutLogger.log("TOKEN...");
                    OneCheckoutLogger.log(EParameterName.TOKEN_NUMBER.code() + "[" + tokenNumber.trim() + "]");
                    creditCardHelper.setTokenNumber(tokenNumber.trim());
                } else {
                    if (!button.equalsIgnoreCase("CANCEL")) {
                        OneCheckoutLogger.log("NEW CARD...");
                        // =============================================================
                        String cardNumber = filteredParams.remove(EParameterName.CARD_NUMBER.code());
                        if (cardNumber == null) {
                            cardNumber = "";
                        }
                        if (!cardNumber.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CARD_NUMBER.code() + "[" + maskedCard(cardNumber.trim()) + "]");
                            creditCardHelper.setCardNumber(cardNumber.trim());
                        } else {
                            insufficientParamStatus = true;
                            insufficientParam += (insufficientParam.equals("") ? "Insufficient Token Payment Data : " + EParameterName.CARD_NUMBER.code() : ", " + EParameterName.CARD_NUMBER.code());
                        }

                        // =============================================================
                        String expiryMonth = filteredParams.remove(EParameterName.EXPIRY_MONTH.code());
                        if (expiryMonth == null) {
                            expiryMonth = "";
                        }
                        if (!expiryMonth.trim().equals("")) {
                            creditCardHelper.setExpMonth(expiryMonth.trim());
                        } else {
                            insufficientParamStatus = true;
                            insufficientParam += (insufficientParam.equals("") ? "Insufficient Token Payment Data : " + EParameterName.EXPIRY_MONTH.code() : ", " + EParameterName.EXPIRY_MONTH.code());
                        }

                        // =============================================================
                        String expiryYear = filteredParams.remove(EParameterName.EXPIRY_YEAR.code());
                        if (expiryYear == null) {
                            expiryYear = "";
                        }
                        if (!expiryYear.trim().equals("")) {
                            creditCardHelper.setExpYear(expiryYear.trim());
                        } else {
                            insufficientParamStatus = true;
                            insufficientParam += (insufficientParam.equals("") ? "Insufficient Token Payment Data : " + EParameterName.EXPIRY_YEAR.code() : ", " + EParameterName.EXPIRY_YEAR.code());
                        }

                        // =============================================================
                        String ccAddress = filteredParams.remove(EParameterName.CC_ADDRESS.code());
                        if (ccAddress == null) {
                            ccAddress = "";
                        }
                        if (!ccAddress.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_ADDRESS.code() + "[" + ccAddress.trim() + "]");
                            creditCardHelper.setCcAddress(ccAddress.trim());
                        }

                        // =============================================================
                        String ccName = filteredParams.remove(EParameterName.CC_NAME.code());
                        if (ccName == null) {
                            ccName = "";
                        }
                        if (!ccName.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_NAME.code() + "[" + ccName.trim() + "]");
                            creditCardHelper.setCcName(ccName.trim());
                        } else {
                            insufficientParamStatus = true;
                            insufficientParam += (insufficientParam.equals("") ? "Insufficient Token Payment Data : " + EParameterName.CC_NAME.code() : ", " + EParameterName.CC_NAME.code());
                        }

                        // =============================================================
                        String ccEmail = unfilterParams.remove(EParameterName.CC_EMAIL.code());
                        if (ccEmail == null) {
                            ccEmail = "";
                        }
                        if (!ccEmail.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_EMAIL.code() + "[" + ccEmail.trim() + "]");
                            creditCardHelper.setCcEmail(ccEmail.trim());
                        } else {
                            insufficientParamStatus = true;
                            insufficientParam += (insufficientParam.equals("") ? "Insufficient Token Payment Data : " + EParameterName.CC_EMAIL.code() : ", " + EParameterName.CC_EMAIL.code());
                        }

                        // =============================================================
                        String ccCity = filteredParams.remove(EParameterName.CC_CITY.code());
                        if (ccCity == null) {
                            ccCity = "";
                        }
                        if (!ccCity.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_CITY.code() + "[" + ccCity.trim() + "]");
                            creditCardHelper.setCcCity(ccCity.trim());
                        }

                        // =============================================================
                        String ccState = filteredParams.remove(EParameterName.CC_STATE_OR_REGION.code());
                        if (ccState == null) {
                            ccState = "";
                        }
                        if (!ccState.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_STATE_OR_REGION.code() + "[" + ccState.trim() + "]");
                            creditCardHelper.setCcRegion(ccState.trim());
                        }

                        // =============================================================
                        String ccCountry = filteredParams.remove(EParameterName.CC_COUNTRY.code());
                        if (ccCountry == null) {
                            ccCountry = "";
                        }
                        if (!ccCountry.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_COUNTRY.code() + "[" + ccCountry.trim() + "]");
                            creditCardHelper.setCcCountry(ccCountry.trim());
                        }

                        // =============================================================
                        String ccSaveStatus = filteredParams.remove(EParameterName.CCSAVESTATUS.code());
                        if (ccSaveStatus == null) {
                            ccCountry = "";
                        }
                        if (!ccCountry.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CCSAVESTATUS.code() + "[" + ccSaveStatus.trim() + "]");
                            creditCardHelper.setCcsaveStatus(ccSaveStatus.trim());
                        }

                        // =============================================================
                        String ccZipCode = filteredParams.remove(EParameterName.CC_ZIPCODE.code());
                        if (ccZipCode == null) {
                            ccZipCode = "";
                        }
                        if (!ccZipCode.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_ZIPCODE.code() + "[" + ccZipCode.trim() + "]");
                            creditCardHelper.setCcZipCode(ccZipCode.trim());
                        }

                        // =============================================================
                        String ccHomePhone = filteredParams.remove(EParameterName.CC_HOME_PHONE.code());
                        if (ccHomePhone == null) {
                            ccHomePhone = "";
                        }
                        if (!ccHomePhone.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_HOME_PHONE.code() + "[" + ccHomePhone.trim() + "]");
                            creditCardHelper.setCcHomePhone(ccHomePhone.trim());
                        }

                        // =============================================================
                        String ccMobilePhone = filteredParams.remove(EParameterName.CC_MOBILE_PHONE.code());
                        if (ccMobilePhone == null) {
                            ccMobilePhone = "";
                        }
                        if (!ccMobilePhone.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_MOBILE_PHONE.code() + "[" + ccMobilePhone.trim() + "]");
                            creditCardHelper.setCcMobilePhone(ccMobilePhone.trim());
                        }

                        // =============================================================
                        String ccWorkPhone = filteredParams.remove(EParameterName.CC_WORK_PHONE.code());
                        if (ccWorkPhone == null) {
                            ccWorkPhone = "";
                        }
                        if (!ccWorkPhone.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.CC_WORK_PHONE.code() + "[" + ccWorkPhone.trim() + "]");
                            creditCardHelper.setCcWorkPhone(ccWorkPhone.trim());
                        }

                        // =============================================================
                        String authStatus = filteredParams.remove(EParameterName.AUTH_STATUS.code());
                        if (authStatus == null) {
                            authStatus = "";
                        }
                        if (!authStatus.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.AUTH_STATUS.code() + "[" + authStatus.trim() + "]");
                            if (authStatus.trim().equalsIgnoreCase("true")) {
                                creditCardHelper.setAuthStatus(true);
                            }
                        }

                        // =============================================================
                        String tokenid = filteredParams.remove(EParameterName.TOKEN_NUMBER.code());
                        if (tokenid == null) {
                            tokenid = "";
                        }
                        if (!tokenid.trim().equals("")) {
                            OneCheckoutLogger.log(EParameterName.TOKEN_NUMBER.code() + "[" + tokenid.trim() + "]");
                            creditCardHelper.setTokenNumber(tokenid);
                        }
                    }
                }

                if (!button.equalsIgnoreCase("EDITCARD") && !button.equalsIgnoreCase("CANCEL")) {
                    String cvv2 = filteredParams.remove(EParameterName.CVV2.code());
                    if (cvv2 == null) {
                        cvv2 = "";
                    }
                    if (!cvv2.trim().equals("")) {
                        creditCardHelper.setCvv2(cvv2.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Token Payment Data : " + EParameterName.CVV2.code() : ", " + EParameterName.CVV2.code());
                    }
                }

                // =============================================================
                if (filteredParams != null) {
                    creditCardHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    creditCardHelper.setParseStatus(true);
                }
                requestHelper.setCreditCardHelper(creditCardHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_TOKEN_PAYMENT_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing Insufficient Token Payment Data : ");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_TOKEN_PAYMENT_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing Token Payment data");
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doParsingRecurRegistrationCustomerRequest(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }

                // UNFILTERED REQUEST
                HashMap<String, String> unfilterParams = new HashMap<String, String>();
                Set m1 = requestHelper.getUnfilterRequest().getParameterMap().keySet();
                for (Object o : m1) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getUnfilterRequest().getParameter(key);
                    unfilterParams.put(key, val);
                }

                RecurHelper recurHelper = new RecurHelper();

                // =============================================================
                String paymentChannelId = filteredParams.remove(EParameterName.PAYMENT_CHANNEL.code());
                if (paymentChannelId == null) {
                    paymentChannelId = "";
                }
                if (!paymentChannelId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PAYMENT_CHANNEL.code() + "[" + paymentChannelId.trim() + "]");
                    recurHelper.setPaymentChannelId(paymentChannelId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.PAYMENT_CHANNEL.code() : ", " + EParameterName.PAYMENT_CHANNEL.code());
                }

                // =============================================================
                String mallId = filteredParams.remove(EParameterName.MALL_ID.code());
                if (mallId == null) {
                    mallId = "";
                }
                if (!mallId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.MALL_ID.code() + "[" + mallId.trim() + "]");
                    recurHelper.setMallId(Integer.valueOf(mallId.trim()));
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.MALL_ID.code() : ", " + EParameterName.MALL_ID.code());
                }

                // =============================================================
                String chainMallId = filteredParams.remove(EParameterName.CHAIN_MALL_ID.code());
                if (chainMallId == null) {
                    chainMallId = "";
                }
                if (!chainMallId.trim().equals("") && !chainMallId.trim().equalsIgnoreCase("NA")) {
                    OneCheckoutLogger.log(EParameterName.CHAIN_MALL_ID.code() + "[" + chainMallId.trim() + "]");
                    recurHelper.setChainMallId(Integer.valueOf(chainMallId.trim()));
                }

                // =============================================================
                String invoiceNumber = filteredParams.remove(EParameterName.INVOICE_NUMBER.code());
                if (invoiceNumber == null) {
                    invoiceNumber = "";
                }
                if (!invoiceNumber.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.INVOICE_NUMBER.code() + "[" + invoiceNumber.trim() + "]");
                    recurHelper.setInvoiceNumber(invoiceNumber.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.INVOICE_NUMBER.code() : ", " + EParameterName.INVOICE_NUMBER.code());
                }

                // =============================================================
                String amount = filteredParams.remove(EParameterName.AMOUNT.code());
                if (amount == null) {
                    amount = "";
                }
                if (!amount.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.AMOUNT.code() + "[" + amount.trim() + "]");
                    recurHelper.setBillingAmount(Double.valueOf(amount.trim()));
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.AMOUNT.code() : ", " + EParameterName.AMOUNT.code());
                }

                // =============================================================
                String registerAmount = filteredParams.remove(EParameterName.REGISTERAMOUNT.code());
                if (registerAmount == null) {
                    registerAmount = "";
                }
                if (!registerAmount.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.REGISTERAMOUNT.code() + "[" + registerAmount.trim() + "]");
                    recurHelper.setBillingRegisterAmount(Double.valueOf(registerAmount.trim()));
                }
//                else {
//                    insufficientParamStatus = true;
//                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.REGISTERAMOUNT.code() : ", " + EParameterName.REGISTERAMOUNT.code());
//                }

                // =============================================================
                String currency = filteredParams.remove(EParameterName.CURRENCY.code());
                if (currency == null) {
                    currency = "";
                }
                if (!currency.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CURRENCY.code() + "[" + currency.trim() + "]");
                    recurHelper.setBillingCurrency(currency.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.CURRENCY.code() : ", " + EParameterName.CURRENCY.code());
                }

                // =============================================================
                String purchaseAmount = filteredParams.remove(EParameterName.PURCHASE_AMOUNT.code());
                if (purchaseAmount == null) {
                    purchaseAmount = "";
                }
                if (!purchaseAmount.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PURCHASE_AMOUNT.code() + "[" + purchaseAmount.trim() + "]");
                    recurHelper.setBillingPurchaseAmount(Double.valueOf(purchaseAmount.trim()));
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.PURCHASE_AMOUNT.code() : ", " + EParameterName.PURCHASE_AMOUNT.code());
                }

                // =============================================================
                String purchaseCurrency = filteredParams.remove(EParameterName.PURCHASE_CURRENCY.code());
                if (purchaseCurrency == null) {
                    purchaseCurrency = "";
                }
                if (!purchaseCurrency.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PURCHASE_CURRENCY.code() + "[" + purchaseCurrency.trim() + "]");
                    recurHelper.setBillingPurchaseCurrency(purchaseCurrency.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.PURCHASE_CURRENCY.code() : ", " + EParameterName.PURCHASE_CURRENCY.code());
                }

                // =============================================================
                String sessionId = filteredParams.remove(EParameterName.SESSION_ID.code());
                if (sessionId == null) {
                    sessionId = "";
                }
                if (!sessionId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.SESSION_ID.code() + "[" + sessionId.trim() + "]");
                    recurHelper.setSessionId(sessionId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.SESSION_ID.code() : ", " + EParameterName.SESSION_ID.code());
                }
//                if (sessionId.length() > 48) {
//                    insufficientParamStatus = true;
//                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.SESSION_ID.code() : " IS OVER 48 Character," + EParameterName.SESSION_ID.code());
//                }

                // =============================================================
                //outstandingAccountBilling(start)
//                String billingNumber = filteredParams.remove(EParameterName.BILLING_NUMBER.code());
//                if (billingNumber == null) {
//                    billingNumber = "";
//                }
//                if (!billingNumber.trim().equals("")) {
//                    OneCheckoutLogger.log(EParameterName.BILLING_NUMBER.code() + "[" + billingNumber.trim() + "]");
//                    recurHelper.setBillingNumber(billingNumber.trim());
//                } else {
//                    insufficientParamStatus = true;
//                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.BILLING_NUMBER.code() : ", " + EParameterName.BILLING_NUMBER.code());
//                }
//
//                // =============================================================
//                String customerId = filteredParams.remove(EParameterName.CUSTOMER_ID.code());
//                if (customerId == null) {
//                    customerId = "";
//                }
//                if (!customerId.trim().equals("")) {
//                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ID.code() + "[" + customerId.trim() + "]");
//                    recurHelper.setCustomerId(customerId.trim());
//                } else {
//                    insufficientParamStatus = true;
//                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.CUSTOMER_ID.code() : ", " + EParameterName.CUSTOMER_ID.code());
//                }
                //outstandingAccountBilling(end)
                // =============================================================
                String words = filteredParams.remove(EParameterName.WORDS.code());
                if (words == null) {
                    words = "";
                }
                if (!words.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.WORDS.code() + "[" + words.trim() + "]");
                    recurHelper.setWords(words.trim());
                    //outstandingAccountBilling(start)
//                    OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
//                    if (!mallId.equals("") && !chainMallId.equals("")) {
//                        Merchants merchant = queryHelper.getMerchantByMallId(Integer.parseInt(mallId), Integer.parseInt(chainMallId.equalsIgnoreCase("NA") ? "0" : chainMallId));
//                        if (merchant != null) {
//                            StringBuilder word = new StringBuilder();
//                            word.append(mallId);
//                            word.append(chainMallId);
//                            word.append(billingNumber);
//                            word.append(customerId);
//                            word.append(amount);
//                            word.append(merchant.getMerchantHashPassword());
//
//                            OneCheckoutLogger.log("ONECHECKOUT ELEMENT [" + word.toString() + "]");
//
//                            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
//                            if (!words.equals(hashwords)) {
//                                insufficientParamStatus = true;
//                                insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + "Words doesn't Match" : ", " + "Words doesn't Match");
//                            }
                    //outstandingAccountBilling(end)
                } else {
                    insufficientParamStatus = true;

                    //outstandingAccountBilling(start)
//                            insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + "Words doesn't Match" : ", " + "Words doesn't Match");
//                        }
//                    } else {
//                        insufficientParamStatus = true;
//                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + "Words doesn't Match" : ", " + "Words doesn't Match");
//                    }
//                } else {
//                    insufficientParamStatus = true;
                    //outstandingAccountBilling(end)
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.WORDS.code() : ", " + EParameterName.WORDS.code());
                }

                // =============================================================
                String requestDateTime = filteredParams.remove(EParameterName.REQUEST_DATE_TIME.code());
                if (requestDateTime == null) {
                    requestDateTime = "";
                }
                if (!requestDateTime.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.REQUEST_DATE_TIME.code() + "[" + requestDateTime.trim() + "]");
                    recurHelper.setRequestDateTime(requestDateTime.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.REQUEST_DATE_TIME.code() : ", " + EParameterName.REQUEST_DATE_TIME.code());
                }

                // =============================================================
                String customerId = filteredParams.remove(EParameterName.CUSTOMER_ID.code());
                if (customerId == null) {
                    customerId = "";
                }
                if (!customerId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ID.code() + "[" + customerId.trim() + "]");
                    recurHelper.setCustomerId(customerId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.CUSTOMER_ID.code() : ", " + EParameterName.CUSTOMER_ID.code());
                }

                // =============================================================
                String customerName = filteredParams.remove(EParameterName.CUSTOMER_NAME.code());
                if (customerName == null) {
                    customerName = "";
                }
                if (!customerName.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_NAME.code() + "[" + customerName.trim() + "]");
                    recurHelper.setCustomerName(customerName.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.CUSTOMER_NAME.code() : ", " + EParameterName.CUSTOMER_NAME.code());
                }

                // =============================================================
                String customerEmail = unfilterParams.remove(EParameterName.CUSTOMER_EMAIL.code());
                if (customerEmail == null) {
                    customerEmail = "";
                }
                if (!customerEmail.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_EMAIL.code() + "[" + customerEmail.trim() + "]");
                    recurHelper.setCustomerEmail(customerEmail.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.CUSTOMER_EMAIL.code() : ", " + EParameterName.CUSTOMER_EMAIL.code());
                }

                // =============================================================
                String customerAddress = filteredParams.remove(EParameterName.CUSTOMER_ADDRESS.code());
                if (customerAddress == null) {
                    customerAddress = "";
                }
                if (!customerAddress.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ADDRESS.code() + "[" + customerAddress.trim() + "]");
                    recurHelper.setCustomerAddress(customerAddress.trim());
                }

                // =============================================================
                String customerCity = filteredParams.remove(EParameterName.CUSTOMER_CITY.code());
                if (customerCity == null) {
                    customerCity = "";
                }
                if (!customerCity.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_CITY.code() + "[" + customerCity.trim() + "]");
                    recurHelper.setCustomerCity(customerCity.trim());
                }

                // =============================================================
                String customerState = filteredParams.remove(EParameterName.CUSTOMER_STATE.code());
                if (customerState == null) {
                    customerState = "";
                }
                if (!customerState.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_STATE.code() + "[" + customerState.trim() + "]");
                    recurHelper.setCustomerState(customerState.trim());
                }

                // =============================================================
                String customerCountry = filteredParams.remove(EParameterName.CUSTOMER_COUNTRY.code());
                if (customerCountry == null) {
                    customerCountry = "";
                }
                if (!customerCountry.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_COUNTRY.code() + "[" + customerCountry.trim() + "]");
                    recurHelper.setCustomerCountry(customerCountry.trim());
                }

                // =============================================================
                String customerZipCode = filteredParams.remove(EParameterName.CUSTOMER_ZIP_CODE.code());
                if (customerZipCode == null) {
                    customerZipCode = "";
                }
                if (!customerZipCode.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ZIP_CODE.code() + "[" + customerZipCode.trim() + "]");
                    recurHelper.setCustomerZipCode(customerZipCode.trim());
                }

                // =============================================================
                String customerHomePhone = filteredParams.remove(EParameterName.CUSTOMER_HOME_PHONE.code());
                if (customerHomePhone == null) {
                    customerHomePhone = "";
                }
                if (!customerHomePhone.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_HOME_PHONE.code() + "[" + customerHomePhone.trim() + "]");
                    recurHelper.setCustomerHomePhone(customerHomePhone.trim());
                }

                // =============================================================
                String customerMobilePhone = filteredParams.remove(EParameterName.CUSTOMER_MOBILE_PHONE.code());
                if (customerMobilePhone == null) {
                    customerMobilePhone = "";
                }
                if (!customerMobilePhone.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_MOBILE_PHONE.code() + "[" + customerMobilePhone.trim() + "]");
                    recurHelper.setCustomerMobilePhone(customerMobilePhone.trim());
                }

                // =============================================================
                String customerWorkPhone = filteredParams.remove(EParameterName.CUSTOMER_WORK_PHONE.code());
                if (customerWorkPhone == null) {
                    customerWorkPhone = "";
                }
                if (!customerWorkPhone.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_WORK_PHONE.code() + "[" + customerWorkPhone.trim() + "]");
                    recurHelper.setCustomerWorkPhone(customerWorkPhone.trim());
                }

                // =============================================================
                String billingType = filteredParams.remove(EParameterName.BILLING_TYPE.code());
                if (billingType == null) {
                    billingType = "";
                }
                if (!billingType.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.BILLING_TYPE.code() + "[" + billingType.trim() + "]");
                    recurHelper.setBillingType(billingType.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.BILLING_TYPE.code() : ", " + EParameterName.BILLING_TYPE.code());
                }

                // =============================================================
                String billingNumber = filteredParams.remove(EParameterName.BILLING_NUMBER.code());
                if (billingNumber == null) {
                    billingNumber = "";
                }
                if (!billingNumber.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.BILLING_NUMBER.code() + "[" + billingNumber.trim() + "]");
                    recurHelper.setBillingNumber(billingNumber.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.BILLING_NUMBER.code() : ", " + EParameterName.BILLING_NUMBER.code());
                }

                // =============================================================
                String billingDetail = filteredParams.remove(EParameterName.BILLING_DETAIL.code());
                if (billingDetail == null) {
                    billingDetail = "";
                }
                if (!billingDetail.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.BILLING_DETAIL.code() + "[" + billingDetail.trim() + "]");
                    recurHelper.setBillingDetail(billingDetail.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.BILLING_DETAIL.code() : ", " + EParameterName.BILLING_DETAIL.code());
                }

                // =============================================================
                String executeType = filteredParams.remove(EParameterName.EXECUTE_TYPE.code());
                if (executeType == null) {
                    executeType = "";
                }
                if (!executeType.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.EXECUTE_TYPE.code() + "[" + executeType.trim() + "]");
                    recurHelper.setExecuteType(executeType.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.EXECUTE_TYPE.code() : ", " + EParameterName.EXECUTE_TYPE.code());
                }

                // =============================================================
                String executeDate = filteredParams.remove(EParameterName.EXECUTE_DATE.code());
                if (executeDate == null) {
                    executeDate = "";
                }
                if (!executeDate.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.EXECUTE_DATE.code() + "[" + executeDate.trim() + "]");
                    recurHelper.setExecuteDate(executeDate.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.EXECUTE_DATE.code() : ", " + EParameterName.EXECUTE_DATE.code());
                }

                // =============================================================
                String executeMonth = filteredParams.remove(EParameterName.EXECUTE_MONTH.code());
                if (executeMonth == null) {
                    executeMonth = "";
                }
                if (!executeMonth.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.EXECUTE_MONTH.code() + "[" + executeMonth.trim() + "]");
                    recurHelper.setExecuteMonth(executeMonth.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.EXECUTE_MONTH.code() : ", " + EParameterName.EXECUTE_MONTH.code());
                }

                // =============================================================
                String executeStartDate = filteredParams.remove(EParameterName.EXECUTE_START_DATE.code());
                if (executeStartDate == null) {
                    executeStartDate = "";
                }
                if (!executeStartDate.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.EXECUTE_START_DATE.code() + "[" + executeStartDate.trim() + "]");
                    recurHelper.setExecuteStartDate(executeStartDate.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.EXECUTE_START_DATE.code() : ", " + EParameterName.EXECUTE_START_DATE.code());
                }

                // =============================================================
                String executeEndDate = filteredParams.remove(EParameterName.EXECUTE_END_DATE.code());
                if (executeEndDate == null) {
                    executeEndDate = "";
                }
                if (!executeEndDate.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.EXECUTE_END_DATE.code() + "[" + executeEndDate.trim() + "]");
                    recurHelper.setExecuteEndDate(executeEndDate.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Request : " + EParameterName.EXECUTE_END_DATE.code() : ", " + EParameterName.EXECUTE_END_DATE.code());
                }

                // =============================================================
                String flatStatus = filteredParams.remove(EParameterName.FLAT_STATUS.code());
                if (flatStatus == null) {
                    flatStatus = "";
                }
                if (!flatStatus.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.FLAT_STATUS.code() + "[" + flatStatus.trim() + "]");
                    if (flatStatus.trim().equalsIgnoreCase("FALSE")) {
                        OneCheckoutLogger.log(">>> RECUR FLAT STATUS FALSE");
                        recurHelper.setFlatStatus(false);
                    }
                }

                // =============================================================
                String cashNow = filteredParams.remove(EParameterName.CASH_NOW.code());
                if (cashNow == null) {
                    cashNow = "";
                }
                if (!cashNow.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CASH_NOW.code() + "[" + cashNow.trim() + "]");
                    if (cashNow.trim().equalsIgnoreCase("true")) {
                        recurHelper.setCashNowStatus(true);
                    }
                }

                // =============================================================
                String clientIpAddress = filteredParams.remove(EParameterName.CUSTOMER_IP.code());
                if (clientIpAddress == null || clientIpAddress.trim().equals("")) {
                    clientIpAddress = requestHelper.getUnfilterRequest().getRemoteHost();
                    if (clientIpAddress == null) {
                        clientIpAddress = "";
                    }
                }
                if (!clientIpAddress.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_IP.code() + "[" + clientIpAddress.trim() + "]");
                    recurHelper.setClientIp(clientIpAddress.trim());
                }

                // =============================================================
                if (filteredParams != null) {
                    recurHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    recurHelper.setParseStatus(true);
                }
                requestHelper.setRecurHelper(recurHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_REGISTRATION_REQUEST_FROM_MERCHANT);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing recur customer registration request from merchant");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_REGISTRATION_REQUEST_FROM_MERCHANT);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing recur customer registration request from merchant");
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doParsingRecurRegistrationCustomerData(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }

                // UNFILTERED REQUEST
                HashMap<String, String> unfilterParams = new HashMap<String, String>();
                Set m1 = requestHelper.getUnfilterRequest().getParameterMap().keySet();
                for (Object o : m1) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getUnfilterRequest().getParameter(key);
                    unfilterParams.put(key, val);
                }

                CreditCardHelper creditCardHelper = new CreditCardHelper();

                // =============================================================
                String tokenNumber = filteredParams.remove(EParameterName.TOKEN_NUMBER.code());
                if (tokenNumber == null) {
                    tokenNumber = "";
                }
                if (!tokenNumber.trim().equals("")) {
                    OneCheckoutLogger.log("REGISTER RECUR WITH TOKEN...");
                    OneCheckoutLogger.log(EParameterName.TOKEN_NUMBER.code() + "[" + tokenNumber.trim() + "]");
                    creditCardHelper.setTokenNumber(tokenNumber.trim());
                } else {
                    OneCheckoutLogger.log("REGISTER RECUR WITH NEW CREDIT CARD...");

                    // =============================================================
                    String cardNumber = filteredParams.remove(EParameterName.CARD_NUMBER.code());
                    if (cardNumber == null) {
                        cardNumber = "";
                    }
                    if (!cardNumber.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CARD_NUMBER.code() + "[" + maskedCard(cardNumber.trim()) + "]");
                        creditCardHelper.setCardNumber(cardNumber.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Data : " + EParameterName.CARD_NUMBER.code() : ", " + EParameterName.CARD_NUMBER.code());
                    }

                    // =============================================================
                    String expiryMonth = filteredParams.remove(EParameterName.EXPIRY_MONTH.code());
                    if (expiryMonth == null) {
                        expiryMonth = "";
                    }
                    if (!expiryMonth.trim().equals("")) {
                        creditCardHelper.setExpMonth(expiryMonth.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Data : " + EParameterName.EXPIRY_MONTH.code() : ", " + EParameterName.EXPIRY_MONTH.code());
                    }

                    // =============================================================
                    String expiryYear = filteredParams.remove(EParameterName.EXPIRY_YEAR.code());
                    if (expiryYear == null) {
                        expiryYear = "";
                    }
                    if (!expiryYear.trim().equals("")) {
                        creditCardHelper.setExpYear(expiryYear.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Data : " + EParameterName.EXPIRY_YEAR.code() : ", " + EParameterName.EXPIRY_YEAR.code());
                    }

                    // =============================================================
                    String cvv2 = filteredParams.remove(EParameterName.CVV2.code());
                    if (cvv2 == null) {
                        cvv2 = "";
                    }
                    if (!cvv2.trim().equals("")) {
                        creditCardHelper.setCvv2(cvv2.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Data : " + EParameterName.CVV2.code() : ", " + EParameterName.CVV2.code());
                    }

                    // =============================================================
                    String ccAddress = filteredParams.remove(EParameterName.CC_ADDRESS.code());
                    if (ccAddress == null) {
                        ccAddress = "";
                    }
                    if (!ccAddress.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_ADDRESS.code() + "[" + ccAddress.trim() + "]");
                        creditCardHelper.setCcAddress(ccAddress.trim());
                    }

                    // =============================================================
                    String ccName = filteredParams.remove(EParameterName.CC_NAME.code());
                    if (ccName == null) {
                        ccName = "";
                    }
                    if (!ccName.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_NAME.code() + "[" + ccName.trim() + "]");
                        creditCardHelper.setCcName(ccName.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Data : " + EParameterName.CC_NAME.code() : ", " + EParameterName.CC_NAME.code());
                    }

                    // =============================================================
                    String ccEmail = unfilterParams.remove(EParameterName.CC_EMAIL.code());
                    if (ccEmail == null) {
                        ccEmail = "";
                    }
                    if (!ccEmail.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_EMAIL.code() + "[" + ccEmail.trim() + "]");
                        creditCardHelper.setCcEmail(ccEmail.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Registration Customer Data : " + EParameterName.CC_EMAIL.code() : ", " + EParameterName.CC_EMAIL.code());
                    }

                    // =============================================================
                    String ccCity = filteredParams.remove(EParameterName.CC_CITY.code());
                    if (ccCity == null) {
                        ccCity = "";
                    }
                    if (!ccCity.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_CITY.code() + "[" + ccCity.trim() + "]");
                        creditCardHelper.setCcCity(ccCity.trim());
                    }

                    // =============================================================
                    String ccState = filteredParams.remove(EParameterName.CC_STATE_OR_REGION.code());
                    if (ccState == null) {
                        ccState = "";
                    }
                    if (!ccState.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_STATE_OR_REGION.code() + "[" + ccState.trim() + "]");
                        creditCardHelper.setCcRegion(ccState.trim());
                    }

                    // =============================================================
                    String ccCountry = filteredParams.remove(EParameterName.CC_COUNTRY.code());
                    if (ccCountry == null) {
                        ccCountry = "";
                    }
                    if (!ccCountry.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_COUNTRY.code() + "[" + ccCountry.trim() + "]");
                        creditCardHelper.setCcCountry(ccCountry.trim());
                    }

                    // =============================================================
                    String ccZipCode = filteredParams.remove(EParameterName.CC_ZIPCODE.code());
                    if (ccZipCode == null) {
                        ccZipCode = "";
                    }
                    if (!ccZipCode.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_ZIPCODE.code() + "[" + ccZipCode.trim() + "]");
                        creditCardHelper.setCcZipCode(ccZipCode.trim());
                    }

                    // =============================================================
                    String ccHomePhone = filteredParams.remove(EParameterName.CC_HOME_PHONE.code());
                    if (ccHomePhone == null) {
                        ccHomePhone = "";
                    }
                    if (!ccHomePhone.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_HOME_PHONE.code() + "[" + ccHomePhone.trim() + "]");
                        creditCardHelper.setCcHomePhone(ccHomePhone.trim());
                    }

                    // =============================================================
                    String ccMobilePhone = filteredParams.remove(EParameterName.CC_MOBILE_PHONE.code());
                    if (ccMobilePhone == null) {
                        ccMobilePhone = "";
                    }
                    if (!ccMobilePhone.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_MOBILE_PHONE.code() + "[" + ccMobilePhone.trim() + "]");
                        creditCardHelper.setCcMobilePhone(ccMobilePhone.trim());
                    }

                    // =============================================================
                    String ccWorkPhone = filteredParams.remove(EParameterName.CC_WORK_PHONE.code());
                    if (ccWorkPhone == null) {
                        ccWorkPhone = "";
                    }
                    if (!ccWorkPhone.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_WORK_PHONE.code() + "[" + ccWorkPhone.trim() + "]");
                        creditCardHelper.setCcWorkPhone(ccWorkPhone.trim());
                    }

                    // =============================================================
                    String authStatus = filteredParams.remove(EParameterName.AUTH_STATUS.code());
                    if (authStatus == null) {
                        authStatus = "";
                    }
                    if (!authStatus.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.AUTH_STATUS.code() + "[" + authStatus.trim() + "]");
                        if (authStatus.trim().equalsIgnoreCase("true")) {
                            creditCardHelper.setAuthStatus(true);
                        }
                    }
                }

                // =============================================================
                if (filteredParams != null) {
                    creditCardHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    creditCardHelper.setParseStatus(true);
                }
                requestHelper.setCreditCardHelper(creditCardHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_REGISTRATION_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing recur customer registration data");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_REGISTRATION_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing recur customer registration data");
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doParsingThreeDSecure(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }

                // UNFILTERED REQUEST
                HashMap<String, String> unfilterParams = new HashMap<String, String>();
                Set m1 = requestHelper.getUnfilterRequest().getParameterMap().keySet();
                for (Object o : m1) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getUnfilterRequest().getParameter(key);
                    unfilterParams.put(key, val);
                }

                ThreeDSecureHelper threeDSecureHelper = new ThreeDSecureHelper();

                // =============================================================
                String merchantData = unfilterParams.remove(EParameterName.MERCHANT_DATA.code());
                if (merchantData == null) {
                    merchantData = "";
                }
                if (!merchantData.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.MERCHANT_DATA.code() + "[" + merchantData.trim() + "]");
                    threeDSecureHelper.setMerchantData(merchantData.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Three D Secure Data : " + EParameterName.MERCHANT_DATA.code() : ", " + EParameterName.MERCHANT_DATA.code());
                }

                // =============================================================
                String paRes = unfilterParams.remove(EParameterName.PAYER_AUTHENTICATION_RESPONSE.code());
                if (paRes == null) {
                    paRes = "";
                }
                if (!paRes.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PAYER_AUTHENTICATION_RESPONSE.code() + "[" + paRes.trim() + "]");
                    threeDSecureHelper.setPaRes(paRes.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Three D Secure Data : " + EParameterName.PAYER_AUTHENTICATION_RESPONSE.code() : ", " + EParameterName.PAYER_AUTHENTICATION_RESPONSE.code());
                }

                // =============================================================
                String title = unfilterParams.remove(EParameterName.TITLE.code());
                if (title == null) {
                    title = "";
                }
                if (!title.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.TITLE.code() + "[" + title.trim() + "]");
                    threeDSecureHelper.setTitle(title.trim());
                }

                // =============================================================
                if (filteredParams != null) {
                    threeDSecureHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    threeDSecureHelper.setParseStatus(true);
                }
                requestHelper.setThreeDSecureHelper(threeDSecureHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_THREE_D_SECURE_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing three d secure data");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_THREE_D_SECURE_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing three d secure data");
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doParsingRecurUpdateCustomerRequest(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }

                // UNFILTERED REQUEST
                HashMap<String, String> unfilterParams = new HashMap<String, String>();
                Set m1 = requestHelper.getUnfilterRequest().getParameterMap().keySet();
                for (Object o : m1) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getUnfilterRequest().getParameter(key);
                    unfilterParams.put(key, val);
                }

                RecurHelper recurHelper = requestHelper.getRecurHelper();
                if (recurHelper == null) {
                    recurHelper = new RecurHelper();
                }
                recurHelper.setUpdateBillingStatus(true);

                // =============================================================
                String paymentChannelId = filteredParams.remove(EParameterName.PAYMENT_CHANNEL.code());
                if (paymentChannelId == null) {
                    paymentChannelId = "";
                }
                if (!paymentChannelId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.PAYMENT_CHANNEL.code() + "[" + paymentChannelId.trim() + "]");
                    recurHelper.setPaymentChannelId(paymentChannelId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.PAYMENT_CHANNEL.code() : ", " + EParameterName.PAYMENT_CHANNEL.code());
                }

                // =============================================================
                String mallId = filteredParams.remove(EParameterName.MALL_ID.code());
                if (mallId == null) {
                    mallId = "";
                }
                if (!mallId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.MALL_ID.code() + "[" + mallId.trim() + "]");
                    recurHelper.setMallId(Integer.valueOf(mallId.trim()));
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.MALL_ID.code() : ", " + EParameterName.MALL_ID.code());
                }

                // =============================================================
                String chainMallId = filteredParams.remove(EParameterName.CHAIN_MALL_ID.code());
                if (chainMallId == null) {
                    chainMallId = "";
                }
                if (!chainMallId.trim().equals("") && !chainMallId.trim().equalsIgnoreCase("NA")) {
                    OneCheckoutLogger.log(EParameterName.CHAIN_MALL_ID.code() + "[" + chainMallId.trim() + "]");
                    recurHelper.setChainMallId(Integer.valueOf(chainMallId.trim()));
                }

                // =============================================================
                String invoiceNumber = filteredParams.remove(EParameterName.INVOICE_NUMBER.code());
                if (invoiceNumber == null) {
                    invoiceNumber = "";
                }
                if (!invoiceNumber.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.INVOICE_NUMBER.code() + "[" + invoiceNumber.trim() + "]");
                    recurHelper.setInvoiceNumber(invoiceNumber.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.INVOICE_NUMBER.code() : ", " + EParameterName.INVOICE_NUMBER.code());
                }

                // =============================================================
                String sessionId = filteredParams.remove(EParameterName.SESSION_ID.code());
                if (sessionId == null) {
                    sessionId = "";
                }
                if (!sessionId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.SESSION_ID.code() + "[" + sessionId.trim() + "]");
                    recurHelper.setSessionId(sessionId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.SESSION_ID.code() : ", " + EParameterName.SESSION_ID.code());
                }

                // =============================================================
                String words = filteredParams.remove(EParameterName.WORDS.code());
                if (words == null) {
                    words = "";
                }
                if (!words.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.WORDS.code() + "[" + words.trim() + "]");
                    recurHelper.setWords(words.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.WORDS.code() : ", " + EParameterName.WORDS.code());
                }

                // =============================================================
                String requestDateTime = filteredParams.remove(EParameterName.REQUEST_DATE_TIME.code());
                if (requestDateTime == null) {
                    requestDateTime = "";
                }
                if (!requestDateTime.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.REQUEST_DATE_TIME.code() + "[" + requestDateTime.trim() + "]");
                    recurHelper.setRequestDateTime(requestDateTime.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.REQUEST_DATE_TIME.code() : ", " + EParameterName.REQUEST_DATE_TIME.code());
                }

                // =============================================================
                String customerId = filteredParams.remove(EParameterName.CUSTOMER_ID.code());
                if (customerId == null) {
                    customerId = "";
                }
                if (!customerId.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_ID.code() + "[" + customerId.trim() + "]");
                    recurHelper.setCustomerId(customerId.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.CUSTOMER_ID.code() : ", " + EParameterName.CUSTOMER_ID.code());
                }

                // =============================================================
                String billingNumber = filteredParams.remove(EParameterName.BILLING_NUMBER.code());
                if (billingNumber == null) {
                    billingNumber = "";
                }
                if (!billingNumber.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.BILLING_NUMBER.code() + "[" + billingNumber.trim() + "]");
                    recurHelper.setBillingNumber(billingNumber.trim());
                } else {
                    insufficientParamStatus = true;
                    insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Request : " + EParameterName.BILLING_NUMBER.code() : ", " + EParameterName.BILLING_NUMBER.code());
                }

                // =============================================================
                String clientIpAddress = filteredParams.remove(EParameterName.CUSTOMER_IP.code());
                if (clientIpAddress == null || clientIpAddress.trim().equals("")) {
                    clientIpAddress = requestHelper.getUnfilterRequest().getRemoteHost();
                    if (clientIpAddress == null) {
                        clientIpAddress = "";
                    }
                }
                if (!clientIpAddress.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.CUSTOMER_IP.code() + "[" + clientIpAddress.trim() + "]");
                    recurHelper.setClientIp(clientIpAddress.trim());
                }

                // =============================================================
                if (filteredParams != null) {
                    recurHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    recurHelper.setParseStatus(true);
                }
                requestHelper.setRecurHelper(recurHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_UPDATE_REQUEST_FROM_MERCHANT);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing Recur Customer Update Request From Merchant");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_UPDATE_REQUEST_FROM_MERCHANT);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing Recur Customer Update Request From Merchant");
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doParsingRecurUpdateCustomerData(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }

                // UNFILTERED REQUEST
                HashMap<String, String> unfilterParams = new HashMap<String, String>();
                Set m1 = requestHelper.getUnfilterRequest().getParameterMap().keySet();
                for (Object o : m1) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getUnfilterRequest().getParameter(key);
                    unfilterParams.put(key, val);
                }

                CreditCardHelper creditCardHelper = new CreditCardHelper();

                // =============================================================
                String tokenNumber = filteredParams.remove(EParameterName.TOKEN_NUMBER.code());
                if (tokenNumber == null) {
                    tokenNumber = "";
                }
                if (!tokenNumber.trim().equals("")) {
                    OneCheckoutLogger.log("UPDATE RECUR WITH TOKEN...");
                    OneCheckoutLogger.log(EParameterName.TOKEN_NUMBER.code() + "[" + tokenNumber.trim() + "]");
                    creditCardHelper.setTokenNumber(tokenNumber.trim());
                } else {
                    OneCheckoutLogger.log("UPDATE RECUR WITH NEW CREDIT CARD...");

                    // =============================================================
                    String cardNumber = filteredParams.remove(EParameterName.CARD_NUMBER.code());
                    if (cardNumber == null) {
                        cardNumber = "";
                    }
                    if (!cardNumber.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CARD_NUMBER.code() + "[" + maskedCard(cardNumber.trim()) + "]");
                        creditCardHelper.setCardNumber(cardNumber.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Data : " + EParameterName.CARD_NUMBER.code() : ", " + EParameterName.CARD_NUMBER.code());
                    }

                    // =============================================================
                    String expiryMonth = filteredParams.remove(EParameterName.EXPIRY_MONTH.code());
                    if (expiryMonth == null) {
                        expiryMonth = "";
                    }
                    if (!expiryMonth.trim().equals("")) {
                        creditCardHelper.setExpMonth(expiryMonth.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Data : " + EParameterName.EXPIRY_MONTH.code() : ", " + EParameterName.EXPIRY_MONTH.code());
                    }

                    // =============================================================
                    String expiryYear = filteredParams.remove(EParameterName.EXPIRY_YEAR.code());
                    if (expiryYear == null) {
                        expiryYear = "";
                    }
                    if (!expiryYear.trim().equals("")) {
                        creditCardHelper.setExpYear(expiryYear.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Data : " + EParameterName.EXPIRY_YEAR.code() : ", " + EParameterName.EXPIRY_YEAR.code());
                    }

                    // =============================================================
                    String cvv2 = filteredParams.remove(EParameterName.CVV2.code());
                    if (cvv2 == null) {
                        cvv2 = "";
                    }
                    if (!cvv2.trim().equals("")) {
                        creditCardHelper.setCvv2(cvv2.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Data : " + EParameterName.CVV2.code() : ", " + EParameterName.CVV2.code());
                    }

                    // =============================================================
                    String ccAddress = filteredParams.remove(EParameterName.CC_ADDRESS.code());
                    if (ccAddress == null) {
                        ccAddress = "";
                    }
                    if (!ccAddress.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_ADDRESS.code() + "[" + ccAddress.trim() + "]");
                        creditCardHelper.setCcAddress(ccAddress.trim());
                    }

                    // =============================================================
                    String ccName = filteredParams.remove(EParameterName.CC_NAME.code());
                    if (ccName == null) {
                        ccName = "";
                    }
                    if (!ccName.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_NAME.code() + "[" + ccName.trim() + "]");
                        creditCardHelper.setCcName(ccName.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Data : " + EParameterName.CC_NAME.code() : ", " + EParameterName.CC_NAME.code());
                    }

                    // =============================================================
                    String ccEmail = unfilterParams.remove(EParameterName.CC_EMAIL.code());
                    if (ccEmail == null) {
                        ccEmail = "";
                    }
                    if (!ccEmail.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_EMAIL.code() + "[" + ccEmail.trim() + "]");
                        creditCardHelper.setCcEmail(ccEmail.trim());
                    } else {
                        insufficientParamStatus = true;
                        insufficientParam += (insufficientParam.equals("") ? "Insufficient Recur Update Customer Data : " + EParameterName.CC_EMAIL.code() : ", " + EParameterName.CC_EMAIL.code());
                    }

                    // =============================================================
                    String ccCity = filteredParams.remove(EParameterName.CC_CITY.code());
                    if (ccCity == null) {
                        ccCity = "";
                    }
                    if (!ccCity.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_CITY.code() + "[" + ccCity.trim() + "]");
                        creditCardHelper.setCcCity(ccCity.trim());
                    }

                    // =============================================================
                    String ccState = filteredParams.remove(EParameterName.CC_STATE_OR_REGION.code());
                    if (ccState == null) {
                        ccState = "";
                    }
                    if (!ccState.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_STATE_OR_REGION.code() + "[" + ccState.trim() + "]");
                        creditCardHelper.setCcRegion(ccState.trim());
                    }

                    // =============================================================
                    String ccCountry = filteredParams.remove(EParameterName.CC_COUNTRY.code());
                    if (ccCountry == null) {
                        ccCountry = "";
                    }
                    if (!ccCountry.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_COUNTRY.code() + "[" + ccCountry.trim() + "]");
                        creditCardHelper.setCcCountry(ccCountry.trim());
                    }

                    // =============================================================
                    String ccZipCode = filteredParams.remove(EParameterName.CC_ZIPCODE.code());
                    if (ccZipCode == null) {
                        ccZipCode = "";
                    }
                    if (!ccZipCode.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_ZIPCODE.code() + "[" + ccZipCode.trim() + "]");
                        creditCardHelper.setCcZipCode(ccZipCode.trim());
                    }

                    // =============================================================
                    String ccHomePhone = filteredParams.remove(EParameterName.CC_HOME_PHONE.code());
                    if (ccHomePhone == null) {
                        ccHomePhone = "";
                    }
                    if (!ccHomePhone.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_HOME_PHONE.code() + "[" + ccHomePhone.trim() + "]");
                        creditCardHelper.setCcHomePhone(ccHomePhone.trim());
                    }

                    // =============================================================
                    String ccMobilePhone = filteredParams.remove(EParameterName.CC_MOBILE_PHONE.code());
                    if (ccMobilePhone == null) {
                        ccMobilePhone = "";
                    }
                    if (!ccMobilePhone.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_MOBILE_PHONE.code() + "[" + ccMobilePhone.trim() + "]");
                        creditCardHelper.setCcMobilePhone(ccMobilePhone.trim());
                    }

                    // =============================================================
                    String ccWorkPhone = filteredParams.remove(EParameterName.CC_WORK_PHONE.code());
                    if (ccWorkPhone == null) {
                        ccWorkPhone = "";
                    }
                    if (!ccWorkPhone.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.CC_WORK_PHONE.code() + "[" + ccWorkPhone.trim() + "]");
                        creditCardHelper.setCcWorkPhone(ccWorkPhone.trim());
                    }

                    // =============================================================
                    String authStatus = filteredParams.remove(EParameterName.AUTH_STATUS.code());
                    if (authStatus == null) {
                        authStatus = "";
                    }
                    if (!authStatus.trim().equals("")) {
                        OneCheckoutLogger.log(EParameterName.AUTH_STATUS.code() + "[" + authStatus.trim() + "]");
                        if (authStatus.trim().equalsIgnoreCase("true")) {
                            creditCardHelper.setAuthStatus(true);
                        }
                    }
                }

                // =============================================================
                if (filteredParams != null) {
                    creditCardHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    creditCardHelper.setParseStatus(true);
                }
                requestHelper.setCreditCardHelper(creditCardHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_UPDATE_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing recur customer update data");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_UPDATE_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing recur customer update data");
            th.printStackTrace();
        }
        return status;
    }

    public <T extends RequestHelper> boolean doParsingRecurUpdateThreeDSecure(T requestHelper) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T extends RequestHelper> boolean doParsingTokenDeleteData(T requestHelper) {
        boolean status = false;
        boolean insufficientParamStatus = false;
        boolean insufficientLengthParamStatus = false;
        String insufficientParam = "";
        String insufficientLengthParam = "";

        try {
            if (requestHelper.getFilteredRequest() != null) {
                // FILTERED REQUEST FROM ESSAPI
                HashMap<String, String> filteredParams = new HashMap<String, String>();
                Set m = requestHelper.getFilteredRequest().getParameterMap().keySet();
                for (Object o : m) {
                    String key = (String) o;
                    String val = null;
                    val = requestHelper.getFilteredRequest().getParameter(key);
                    filteredParams.put(key, val);
                }
                CreditCardHelper creditCardHelper = new CreditCardHelper();
                TokenizationHelper tokenizationHelper = requestHelper.getTokenizationHelper();
                if (tokenizationHelper == null) {
                    tokenizationHelper = new TokenizationHelper();
                }
                // =============================================================
                String button = filteredParams.remove(EParameterName.BUTTON.code());
                if (button == null) {
                    button = "";
                }
                if (!button.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.BUTTON.code() + "[" + button.trim() + "]");
                    creditCardHelper.setButton(button.trim());
                }
                // =============================================================
                String tokenNumber = filteredParams.remove(EParameterName.TOKEN_NUMBER.code());
                if (tokenNumber == null || tokenNumber.trim().equals("")) {
                    tokenNumber = requestHelper.getUnfilterRequest().getRemoteHost();
                    if (tokenNumber == null) {
                        tokenNumber = "";
                    }
                }
                if (!tokenNumber.trim().equals("")) {
                    OneCheckoutLogger.log(EParameterName.TOKEN_NUMBER.code() + "[" + tokenNumber.trim() + "]");
                    tokenizationHelper.setTokenId(tokenNumber);
                }

                // =============================================================
                if (filteredParams != null) {
                    tokenizationHelper.setAdditionalData(filteredParams);
                }
                if (!insufficientParamStatus && !insufficientLengthParamStatus) {
                    tokenizationHelper.setParseStatus(true);
                }
                requestHelper.setTokenizationHelper(tokenizationHelper);
                requestHelper.setCreditCardHelper(creditCardHelper);
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_TOKEN_PAYMENT_DATA);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing Token Customer Delete Request");
            th.printStackTrace();
        }

        try {
            if (insufficientParamStatus || insufficientLengthParamStatus) {
                String message = "";
                if (!insufficientParam.equals("") && insufficientLengthParam.equals("")) {
                    message = insufficientParam;
                } else if (insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientLengthParam;
                } else if (!insufficientParam.equals("") && !insufficientLengthParam.equals("")) {
                    message = insufficientParam + "; " + insufficientLengthParam;
                }
                requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.INSUFFICIENT_PARAM);
                requestHelper.getResultHelper().setSystemMessage(message);
            } else {
                status = true;
            }
        } catch (Throwable th) {
            requestHelper.getResultHelper().setEMerchantActivityErrorCode(EMerchantActivityErrorCode.FAILED_PARSING_RECUR_CUSTOMER_UPDATE_REQUEST_FROM_MERCHANT);
            requestHelper.getResultHelper().setSystemMessage("Failed Parsing Recur Customer Update Request From Merchant");
            th.printStackTrace();
        }
        return status;
    }

    private String maskedCard(String cardNumber) {
        String maskedCard = "";
        try {
            if (cardNumber != null && cardNumber.length() > 10) {
                String prefix = cardNumber.substring(0, 6);
                String postfix = cardNumber.substring(cardNumber.length() - 4);
                String masked = "";
                for (int i = 0; i < cardNumber.length() - 10; i++) {
                    masked += "*";
                }
                maskedCard = prefix + masked + postfix;
            } else {
                maskedCard = cardNumber;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return maskedCard;
    }
}
