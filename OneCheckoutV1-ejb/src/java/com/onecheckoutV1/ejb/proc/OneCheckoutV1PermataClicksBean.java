/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutCheckStatusData;
import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBean;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
@Stateless
public class OneCheckoutV1PermataClicksBean extends OneCheckoutChannelBase implements OneCheckoutV1PermataClicksBeanLocal {

    private static PropertiesConfiguration configuration = OneCheckoutProperties.getOneCheckoutConfig();

    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;

    @Resource
    protected SessionContext ctx;

    private final static String mIDAcquirerId;
    private final static String serviceId;

    static {
        mIDAcquirerId = configuration.getString("ACQUIRERID.PERMATA.VALUE", "500").trim();
        serviceId = configuration.getString("INTERNET.BANKING.SERVICE.ID", "1").trim();
    }

    private boolean ableToReversal = true;

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        Transactions transactions = null;
        OneCheckoutErrorMessage errorMessage = OneCheckoutErrorMessage.UNKNOWN;

        try {
            long timeStart = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doPayment - T0: %d Start", (System.currentTimeMillis() - timeStart));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
            IdentifyTrx identifyTrx = super.getTransactionInfo(paymentRequest, acq, m);

            String statusCode = identifyTrx.getStatusCode();
            boolean request_good = identifyTrx.isRequestGood();
            boolean needNotify = identifyTrx.isNeedNotify();
            MerchantPaymentChannel mpc = identifyTrx.getMpc();

            if (request_good) {
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
                PaymentChannel pc = queryHelper.getPaymentChannelByChannel(OneCheckoutPaymentChannel.Permata);
                PaymentChannel redirectPaymentChannel = new PaymentChannel();

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    OneCheckoutLogger.log("Onecheckout Method MIP");
                    transactions = transacBean.saveTransactions(trxHelper, mpc);
                    String CoreUrl = pc.getRedirectPaymentUrlMip();
                    OneCheckoutLogger.log("URL PRE DIRECT MIP : " + CoreUrl);
                    String xmls = reDirectToCore(trxHelper.getPaymentRequest(), CoreUrl, mpc, m, transactions, transactions.getOcoId());

                    if (!xmls.equals("")) {
                        XMLConfiguration xml = new XMLConfiguration();
                        try {
                            StringReader sr = new StringReader(xmls);
                            xml.load(sr);
                            if (xml.getRootElementName().equals("MIBPaymentResponse") && xml.getString("result").equals("REDIRECT")) {
                                redirectPaymentChannel.setRedirectPaymentUrlMip(xml.getString("redirectUrl"));
                                String param = xml.getString("redirectParameter");
                                OneCheckoutLogger.log("URL REDIRECT  : " + redirectPaymentChannel.getRedirectPaymentUrlMip());
                                OneCheckoutLogger.log("PARAM REDIRECT : " + param);

                                HashMap<String, String> parameter = parseParamRedirectToCore(param);
                                String trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode") : "";
                                transactions.setDokuVoidApprovalCode(trxCode);
                                paymentRequest.setAllAdditionData(parameter);
                                redirect = createRedirectMIP(paymentRequest, redirectPaymentChannel, mpc, m, transactions.getOcoId());

                                trxHelper.setMessage("VALID");
                                transactions.setRedirectDatetime(new Date());
                                transactions.setSystemMessage(xmls);
                                transactions.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());
                                em.merge(transactions);

                            } else {
                                OneCheckoutLogger.log("::: FAILED REDIRECT TO CORE :::");
                                redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                                trxHelper.setMessage("FAILED");
                            }
                        } catch (Exception e) {
                            OneCheckoutLogger.log("::: FAILED REDIRECT TO CORE :::");
                            redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                            trxHelper.setMessage("FAILED");
                        }
                    } else {
                        OneCheckoutLogger.log("::: FAILED REDIRECT TO CORE :::");
                        redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                        trxHelper.setMessage("FAILED");
                    }
                } else {
                    //case ocoMethod as CIP
                    OneCheckoutLogger.log("Onecheckout Method CIP");
                    transactions = transacBean.updateTransactions(trxHelper, mpc);
                    String CoreUrl = pc.getRedirectPaymentUrlCip();
                    OneCheckoutLogger.log("URL PRE DIRECT CIP");
                    String xmls = reDirectToCore(trxHelper.getPaymentRequest(), CoreUrl, mpc, m, transactions, transactions.getOcoId());

                    if (!xmls.equals("")) {
                        XMLConfiguration xmlc = new XMLConfiguration();
                        try {
                            StringReader sr = new StringReader(xmls);
                            xmlc.load(sr);
                            if (xmlc.getRootElementName().equals("MIBPaymentResponse") && xmlc.getString("result").equals("REDIRECT")) {
                                redirectPaymentChannel.setRedirectPaymentUrlCip(xmlc.getString("redirectUrl"));
//                                String param = xmlc.getString("redirectParameter");
                                String paramRemoveFirst = xmlc.getProperty("redirectParameter").toString().substring(1);
                                String paramRemoveLast = paramRemoveFirst.substring(0, paramRemoveFirst.length() - 1);

                                String param = paramRemoveLast;

                                OneCheckoutLogger.log("URL REDIRECT CIP TO : " + redirectPaymentChannel.getRedirectPaymentUrlCip());
                                OneCheckoutLogger.log("PARAM               : [" + param + "]");

                                HashMap<String, String> parameter = parseParamRedirectToCore(param);

                                String trxCode = xmlc.getString("trxCode");
                                transactions.setDokuVoidApprovalCode(trxCode);
                                paymentRequest.setAllAdditionData(parameter);
                                redirect = createRedirectCIP(paymentRequest, redirectPaymentChannel, mpc, m, trxHelper.getOcoId());

                                trxHelper.setMessage("VALID");
                                transactions.setRedirectDatetime(new Date());
                                transactions.setSystemMessage(xmls);
                                transactions.setTransactionsState(OneCheckoutTransactionState.NOTIFYING.value());

                                em.merge(transactions);

                            } else {
                                OneCheckoutLogger.log("::: FAILED,RESULT TAG IN RESPONSE NOT REDIRECT :::");
                                redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                                trxHelper.setMessage("FAILED");
                            }

                        } catch (Exception e) {
                            OneCheckoutLogger.log("::: FAILED REDIRECT TO CORE :::");
                            redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                            trxHelper.setMessage("FAILED");
                            e.printStackTrace();
                        }
                    } else {
                        OneCheckoutLogger.log("::: FAILED REDIRECT TO CORE :::");
                        redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE.value());
                        trxHelper.setMessage("FAILED");
                    }

                }

                trxHelper.setRedirect(redirect);
                trxHelper.setTransactions(transactions);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);

            } else {
                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errorMessage, needNotify, transactions);
            }
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doPayment - T2: %d Finish process", (System.currentTimeMillis() - timeStart));
            return trxHelper;
        } catch (Throwable t) {
            t.printStackTrace();
            trxHelper.setMessage(t.getMessage());
            return trxHelper;
        }

    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel paymentChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        OneCheckoutLogger.log("============= Create Redirect MIP =============");
        try {
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(paymentChannel.getRedirectPaymentUrlMip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            redirect.setParameters(paymentRequest.getAllAdditionData());
            return redirect;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private String reDirectToCore(OneCheckoutPaymentRequest paymentRequest, String CoreUrl, MerchantPaymentChannel mpc, Merchants merchants, Transactions transactions, String ocoId) {
        OneCheckoutLogger.log(": : : Get Redirect Param From Luna Service : : : ");
        String response = "";
        try {
            Integer MALLID = mpc.getPaymentChannelCode();
            String SHAREDKEY = merchants.getMerchantHashPassword();
            String WORDS = HashWithSHA1.doHashing(MALLID.toString() + SHAREDKEY, "SHA1", null);
            String CHAINMALLID = "";

            if (mpc.getPaymentChannelChainCode() != null && mpc.getPaymentChannelChainCode() > 0) {
                CHAINMALLID = mpc.getPaymentChannelChainCode().toString();
            }
            String invoiceNumber = paymentRequest.getTRANSIDMERCHANT() != null ? paymentRequest.getTRANSIDMERCHANT().trim() : "";
            String currency = "IDR";
            Currency c = queryHelper.getCurrencyByCode(paymentRequest.getCURRENCY());

            if (c != null && c.getAlpha3Code() != null) {
                currency = c.getAlpha3Code();
            }

            String sessionId = paymentRequest.getSESSIONID() != null ? paymentRequest.getSESSIONID().trim() : "";
            String basket = paymentRequest.getBASKET() != null ? paymentRequest.getBASKET().trim() : "";
            String mobilePhone = paymentRequest.getMOBILEPHONE() != null ? paymentRequest.getMOBILEPHONE() : "";
            String customerName = paymentRequest.getNAME() != null ? paymentRequest.getNAME() : "";
            String customerEmail = paymentRequest.getEMAIL() != null ? paymentRequest.getEMAIL() : "";
            String deviceId = paymentRequest.getDEVICEID() != null ? paymentRequest.getDEVICEID() : "";
//            String ua_browser = paymentRequest.getUA_BROWSER() != null ? paymentRequest.getUA_BROWSER() : "";
//            String ua_os = paymentRequest.getUA_OS() != null ? paymentRequest.getUA_OS() : "";
//            String ua_osVersion = paymentRequest.getUA_OSVERSION() != null ? paymentRequest.getUA_OSVERSION() : "";
//            String ua_screenprint = paymentRequest.getUA_SCREENPRINT() != null ? paymentRequest.getUA_SCREENPRINT() : "";
//            String ualowercase = paymentRequest.getUA_LOWERCASE() != null ? paymentRequest.getUA_LOWERCASE() : "";
            String username = paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "";
            String CustIp = paymentRequest.getCUSTIP() != null ? paymentRequest.getCUSTIP() : "";
            String PayReason = paymentRequest.getPAYREASON() != null ? paymentRequest.getPAYREASON() : "";

//            String desc = "Payment Using Permata Clicks";
//            int pos = basket.indexOf(",");
//            if (pos > 0) {
//                desc = basket.substring(0, pos);
//            }
            
//            int lengthBasket = basket.split("\\;").length;
//            String arrBask[] = basket.split("\\;");
//            String resDesc = "";
//
//            for (int i = 0; i < lengthBasket; i++) {
//                String inArrBask[] = arrBask[i].split("\\,");
//                resDesc += inArrBask[0];
//                
//                if (i != lengthBasket-1) {
//                    resDesc += ",";
//                }
//            }
            

            String desc = "Pembayaran di " + merchants.getMerchantName(); //+ " berupa " + resDesc;

            String[] strParam = {"UTF-8", "&"};

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SERVICEID=").append(URLEncoder.encode(serviceId, strParam[0])).append(strParam[1]);
            stringBuilder.append("ACQUIRERID=").append(URLEncoder.encode(mIDAcquirerId, strParam[0])).append(strParam[1]);
            stringBuilder.append("MALLID=").append(URLEncoder.encode(MALLID.toString(), strParam[0])).append(strParam[1]);
            stringBuilder.append("CHAINMALLID=").append(URLEncoder.encode(CHAINMALLID, strParam[0])).append(strParam[1]);
            stringBuilder.append("WORDS=").append(URLEncoder.encode(WORDS, strParam[0])).append(strParam[1]);
            stringBuilder.append("INVOICENUMBER=").append(URLEncoder.encode(invoiceNumber, strParam[0])).append(strParam[1]);
            stringBuilder.append("CURRENCY=").append(URLEncoder.encode(currency, strParam[0])).append(strParam[1]);
            stringBuilder.append("AMOUNT=").append(URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()), strParam[0])).append(strParam[1]);
            stringBuilder.append("SESSIONID=").append(URLEncoder.encode(sessionId, strParam[0])).append(strParam[1]);
            stringBuilder.append("BASKET=").append(URLEncoder.encode(basket, strParam[0])).append(strParam[1]);
            stringBuilder.append("CUSTOMERMOBILEPHONE=").append(URLEncoder.encode(mobilePhone, strParam[0])).append(strParam[1]);
            stringBuilder.append("CUSTOMERNAME=").append(URLEncoder.encode(customerName, strParam[0])).append(strParam[1]);
            stringBuilder.append("CUSTOMEREMAIL=").append(URLEncoder.encode(customerEmail, strParam[0])).append(strParam[1]);
            stringBuilder.append("DESCRIPTION=").append(URLEncoder.encode(desc, strParam[0])).append(strParam[1]);
//            stringBuilder.append("SERVICETRANSACTIONID=").append(URLEncoder.encode(String.valueOf(transactions.getTransactionsId()), strParam[0])).append(strParam[1]);
            stringBuilder.append("DEVICEID=").append(URLEncoder.encode(deviceId, strParam[0])).append(strParam[1]);
//            stringBuilder.append("UA_BROWSER=").append(URLEncoder.encode(ua_browser, strParam[0])).append(strParam[1]);
//            stringBuilder.append("UA_OS=").append(URLEncoder.encode(ua_os, strParam[0])).append(strParam[1]);
//            stringBuilder.append("UA_OSVERSION=").append(URLEncoder.encode(ua_osVersion, strParam[0])).append(strParam[1]);
//            stringBuilder.append("UA_SCREENPRINT=").append(URLEncoder.encode(ua_screenprint, strParam[0])).append(strParam[1]);
//            stringBuilder.append("UA_LOWERCASE=").append(URLEncoder.encode(ualowercase, strParam[0])).append(strParam[1]);
            stringBuilder.append("USERNAME=").append(URLEncoder.encode(username, strParam[0])).append(strParam[1]);
            stringBuilder.append("CUSTIP=").append(URLEncoder.encode(CustIp, strParam[0])).append(strParam[1]);
            stringBuilder.append("PAYREASON=").append(URLEncoder.encode(PayReason, strParam[0])).append(strParam[1]);
//            String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            stringBuilder.append("SERVICETRANSACTIONID=").append(URLEncoder.encode(ocoId, strParam[0])).append(strParam[1]);
            
            System.out.println("SEND PARAMETER =  " + stringBuilder.toString());

            InternetResponse internetResponse = super.doFetchHTTP(stringBuilder.toString(), CoreUrl, merchants.getMerchantReadTimeout(), merchants.getMerchantConnectionTimeout());
            response = internetResponse.getMsgResponse();
            System.out.println("RESPONSE REDIRECT = \n" + response);
//            OneCheckoutLogger.log("RESPONSE REDIRECT = \n" + response);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return response;
    }

    private HashMap<String, String> parseParamRedirectToCore(String params) {
        try {
//            HashMap<String, String> parameter = new HashMap<String, String>();
//            String[] param = params.split("\\|\\|");
//            JSONObject jSONObject = new JSONObject(param[1].trim());
//            Iterator<?> keys = jSONObject.keys();
//            
//            while (keys.hasNext()) {
//                String key = (String) keys.next();
//                String value = jSONObject.getString(key);
//                OneCheckoutLogger.log(":: " + key + "[" + value + "]");
//                parameter.put(key, value);
//            }
//            return parameter;

            HashMap<String, String> parameter = new HashMap<String, String>();
            String[] param = params.split("\\|\\|");

            parameter.put("data", param[1]);

            return parameter;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {

        try {

            long startTime = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - startTime));
            OneCheckoutDOKUNotifyData notifyRequestData = trxHelper.getNotifyRequest();
            String invoiceNo = notifyRequestData.getTRANSIDMERCHANT();
            String sessionId = notifyRequestData.getSESSIONID();
            double amount = notifyRequestData.getAMOUNT();
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - startTime));
            Transactions transactions = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, paymentChannel);

            if (transactions == null) {
                OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus : Transaction is null");
                notifyRequestData.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequestData);
                return trxHelper;
            }

            String Words = super.generateMIBNotifyRequestWords(notifyRequestData, transactions);
            if (!Words.equalsIgnoreCase(notifyRequestData.getWORDS())) {
                OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus : WORDS doesn't match !");
                notifyRequestData.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
                trxHelper.setNotifyRequest(notifyRequestData);
                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus - T2: %d update transaction", (System.currentTimeMillis() - startTime));
            transactions.setDokuInvokeStatusDatetime(notifyRequestData.getREQUESPAYMENTDATETIME());
            transactions.setDokuApprovalCode(notifyRequestData.getAPPROVALCODE());
            transactions.setDokuIssuerBank(notifyRequestData.getBANK());
            transactions.setDokuResponseCode(notifyRequestData.getRESPONSECODE());
            transactions.setDokuHostRefNum(notifyRequestData.getHOSTREFNUM());
            transactions.setDokuResult(notifyRequestData.getRESULT());
            transactions.setDokuResultMessage(notifyRequestData.getRESULTMSG());

            if (notifyRequestData.getDFSStatus() != null || notifyRequestData.getDFSId() != null || notifyRequestData.getDFSScore() != 0) {
                transactions.setVerifyId(notifyRequestData.getDFSId());
                transactions.setVerifyStatus(notifyRequestData.getDFSStatus().value());
                transactions.setVerifyScore(notifyRequestData.getDFSScore());
            } else {
                transactions.setVerifyId("");
                transactions.setVerifyScore(0);
                transactions.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
            }

            OneCheckoutTransactionStatus status = null;
            if (notifyRequestData.getRESULT() != null && notifyRequestData.getRESULT().indexOf("SUCCESS") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            transactions.setInc3dSecureStatus(notifyRequestData.getTHREEDSECURESTATUS());
            transactions.setIncLiability(notifyRequestData.getLIABILITY());
            transactions.setTransactionsStatus(status.value());
            transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - startTime));
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
            HashMap<String, String> params = super.getData(transactions);
            params.put("PAYMENTCODE", "");
            String resp = super.notifyStatusMerchant(transactions, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);

            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - startTime));
            em.merge(transactions);
            notifyRequestData.setACKNOWLEDGE(resp);
            trxHelper.setNotifyRequest(notifyRequestData);
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - startTime));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }

    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        try {
            long startTime = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - startTime));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutRedirectData redirectDataRequest = trxHelper.getRedirectDoku();
            String statusCode = "";
            String invoiceNo = null;
            String sessionId = null;
            double amount = 0.0;
            int attempts = 1;

            if (redirectDataRequest != null) {
//                OneCheckoutLogger.log("test case =* 1");
                invoiceNo = redirectDataRequest.getTRANSIDMERCHANT();
                statusCode = redirectDataRequest.getSTATUSCODE();
                amount = redirectDataRequest.getAMOUNT();
                sessionId = redirectDataRequest.getSESSIONID();
            } else {
//                OneCheckoutLogger.log("test case =* 2");
                invoiceNo = trxHelper.getTransactions().getIncTransidmerchant();
                sessionId = trxHelper.getTransactions().getIncSessionid();
                amount = trxHelper.getTransactions().getIncAmount().doubleValue();
            }

//            OneCheckoutLogger.log("invoiceNo =* " + invoiceNo);
//            OneCheckoutLogger.log("sessionId =* " + sessionId);
//            OneCheckoutLogger.log("amount =* " + amount);
            Object[] objects = queryHelper.getRedirectTransactionWithoutStateNumber(invoiceNo, sessionId, amount);
            attempts = (Integer) objects[0];
            Transactions transactions = (Transactions) objects[1];

            if (redirectDataRequest == null) {
                transactions = trxHelper.getTransactions();
                statusCode = transactions.getDokuResponseCode();
            }

            OneCheckoutLogger.log("ATTEMPTS : " + attempts);
//            OneCheckoutLogger.log("TRANSACTIOONID =* " + transactions.getTransactionsId());
            if (transactions.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                transactions.setDokuResponseCode(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.value());
                transactions.setDokuResultMessage(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.name());
                transactions.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            }

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();
            redirect.setPURCHASECURRENCY(transactions.getIncPurchasecurrency());

            if (redirectDataRequest != null && redirectDataRequest.getFLAG().equalsIgnoreCase("1")) {
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=1");
                if ((m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) && transactions.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                    OneCheckoutLogger.log("FLAG=1 A");
                    if (attempts >= 3) {
                        OneCheckoutLogger.log("FLAG=1 A1");
                        redirect.setUrlAction(m.getMerchantRedirectUrl());
                        redirect.setPageTemplate("redirect.html");
                    } else {
                        OneCheckoutLogger.log("FLAG=1 A2");
                        redirect.setUrlAction(configuration.getString("URL.RECEIVE"));
                        redirect.setPageTemplate("redirect.html");
                        data = super.generatePaymentRequest(transactions);
                        redirect.setParameters(new HashMap(data));
                    }
                } else {
                    OneCheckoutLogger.log("FLAG=1 A1");
                    redirect.setAMOUNT(amount);
                    redirect.setTRANSIDMERCHANT(invoiceNo);
                    redirect.setSTATUSCODE(statusCode);
                    redirect.setUrlAction(m.getMerchantRedirectUrl());
                    redirect.setPageTemplate("redirect.html");
                }
                trxHelper.setRedirectDoku(redirectDataRequest);
            } else if (redirectDataRequest != null && redirectDataRequest.getFLAG().equalsIgnoreCase("2")) {
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
                statusCode = transactions.getDokuResponseCode();
                redirectDataRequest.setSTATUSCODE(statusCode);
                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(redirectDataRequest.getSTATUSCODE());
                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");

            } else {
                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(statusCode);
                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0");
                HashMap resultData = redirect.getRetryData();
                resultData.put("DATENOW", OneCheckoutVerifyFormatData.detailDateFormat.format(new Date()));
                resultData.put("DISPLAYNAME", m.getMerchantName());
                resultData.put("DISPLAYADDRESS", m.getMerchantAddress());
                resultData.put("CREDIT_CARD", transactions.getAccountId());

                Currency currency = queryHelper.getCurrencyByCode(transactions.getIncCurrency());
                resultData.put("CURRENCY", currency.getAlpha3Code());
                resultData.put("INVOICE", invoiceNo);
                resultData.put("DISPLAYAMOUNT", OneCheckoutVerifyFormatData.moneyFormat.format(transactions.getIncAmount()));

                if (m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A1");
                    redirect.setRetry(Boolean.TRUE);
                    resultData.put("ATTEMPTS", String.valueOf(attempts));

                }
                redirect.setUrlAction(configuration.getString("URL.GOBACK"));
                resultData.put("DISPLAYCHANNEL", trxHelper.getPaymentChannel().value());
                OneCheckoutLogger.log("TRANSACTION STATUS [" + transactions.getTransactionsStatus() + "]");

                if (transactions.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                    if (transactions.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()) {
                        redirect.setRetry(Boolean.FALSE);
                    }
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");
                    redirect.setPageTemplate("transactionFailed.html");
                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A4");
                    redirect.setPageTemplate("transactionSuccess.html");
                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with " + m.getMerchantName());
                    resultData.put("APPROVAL", transactions.getDokuApprovalCode());
                }
                redirect.setRetryData(resultData);
                HashMap<String, String> params = super.getData(transactions);
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(paymentChannel.getPaymentChannelId());
                transactions = super.notifyOnRedirect(transactions, params, channel);
            }

            data.put("TRANSIDMERCHANT", invoiceNo);
            data.put("STATUSCODE", statusCode);
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", sessionId);
            data.put("PAYMENTCODE", "");
            data.put("PURCHASECURRENCY", redirect.getPURCHASECURRENCY());

            if (transactions.getRates() != null && redirect.getUrlAction().equals(m.getMerchantRedirectUrl())) {
                redirect.setAMOUNT(transactions.getIncPurchaseamount().doubleValue());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(transactions.getIncPurchaseamount().doubleValue()));
                data.put("CURRENCY", transactions.getIncPurchasecurrency());
            } else {
                redirect.setAMOUNT(transactions.getIncAmount().doubleValue());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(transactions.getIncAmount().doubleValue()));
                data.put("CURRENCY", transactions.getIncCurrency());
            }
            data.put("WORDS", super.generateRedirectWords(redirect, m));
            OneCheckoutLogger.log("============== START REDIRECT PARAMETER ==============");
            OneCheckoutLogger.log("MERCHANT URL         [" + m.getMerchantRedirectUrl() + "]");
            OneCheckoutLogger.log("AMOUNT               [" + data.get("AMOUNT") + "]");
            OneCheckoutLogger.log("TRANSIDMERCHANT      [" + data.get("TRANSIDMERCHANT") + "]");
            OneCheckoutLogger.log("WORDS                [" + data.get("WORDS") + "]");
            OneCheckoutLogger.log("STATUSCODE           [" + data.get("STATUSCODE") + "]");
            OneCheckoutLogger.log("PAYMENTCHANNEL       [" + data.get("PAYMENTCHANNEL") + "]");
            OneCheckoutLogger.log("SESSIONID            [" + data.get("SESSIONID") + "]");
            OneCheckoutLogger.log("PAYMENTCODE          [" + data.get("PAYMENTCODE") + "]");
            OneCheckoutLogger.log("CURRENCY             [" + data.get("CURRENCY") + "]");
            OneCheckoutLogger.log("PURCHASECURRENCY     [" + data.get("PURCHASECURRENCY") + "]");
            OneCheckoutLogger.log("============== END  REDIRECT  PARAMETER ==============");

            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);
            transactions.setRedirectDatetime(new Date());
            em.merge(transactions);
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - startTime));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }

    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
        try {
            long startTime = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - startTime));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            Transactions trans = trxHelper.getTransactions();

            if (trans != null) {
                String word = super.generateCheckStatusRequestWords(trans, m);

                if (!word.equalsIgnoreCase(checkStatusRequest.getWORDS())) {
                    OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
                    statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
                    return trxHelper;
                }

                OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - startTime));
                trans = super.CheckStatusCOREMIB(trans, Integer.parseInt(mIDAcquirerId));
                HashMap<String, String> params = super.getData(trans);
                trxHelper.setMessage("VALID");
                params.put("PAYMENTCODE", "");

                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params, m));
                trxHelper.setCheckStatusRequest(statusRequest);
                return trxHelper;
            }

            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");
            OneCheckoutLogger.log("OneCheckoutV1PermataClicksBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - startTime));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            return trxHelper;
        }
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        OneCheckoutLogger.log("============ CREATE REDIRECT CIP ============");
        try {
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlCip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            redirect.setParameters(paymentRequest.getAllAdditionData());
            return redirect;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

//    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
