/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.*;
import com.onecheckoutV1.data.*;
import com.onecheckoutV1.ejb.exception.InvalidDOKUNotifyDataException;
import com.onecheckoutV1.ejb.exception.InvalidDOKUVerifyDataException;
import com.onecheckoutV1.ejb.exception.InvalidPaymentRequestException;
//import com.onecheckoutV1.ejb.helper.MIPDokuInquiryResponse;
import com.onecheckoutV1.ejb.helper.MIPInquiryResponse;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBean;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckOutBatchType;
import com.onecheckoutV1.type.OneCheckOutSettlementStatus;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMerchantCategory;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onecheckoutV1.type.OneCheckoutVAMerchantResponseCode;
import com.onechekoutv1.dto.Batch;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.SeqBatchIdNextval;
import com.onechekoutv1.dto.Transactions;
import com.onechekoutv1.dto.TransactionsDataAirlines;
import com.onechekoutv1.dto.TransactionsDataCardholder;
import com.onechekoutv1.dto.TransactionsDataNonAirlines;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
//import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
//import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutChannelBase implements Serializable {

    public static final long serialVersionUID = 1L;

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    @EJB
    OneCheckoutV1QueryHelperBeanLocal nl;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    public static final int MAX_FAILED_TRANS = 3;

    private String[] tembakHttp(String web, String param, int conTimeout, int readTimeout) {
        String hasil = "";
        OneCheckoutLogger.log("------------------------------------------------------------------------");
        String[] res = new String[]{"-1", ""};
        try {
            //BUAT KONEKSI HTTP
            URL url = new URL(web);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(conTimeout * 1000);
            conn.setReadTimeout(readTimeout * 1000);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            // send post data to merchant
            wr.write(param);
            wr.flush();

            int responseCode = conn.getResponseCode();

            OneCheckoutLogger.log("Response Code Http Connection[" + responseCode + "]");
            // check responce code from merchant
            if (responseCode == 200) {

                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    hasil = hasil + line;
                }

            }
            res = new String[]{responseCode + "", hasil};
            conn.disconnect();
        } catch (Throwable th) {
            th.printStackTrace();
        }
        //OneCheckoutLogger.log("Result Http Connections[" + hasil + "]");
        OneCheckoutLogger.log("------------------------------------------------------------------------");
        return res;
    }

    public InternetResponse doFetchHTTP(String data, String url, int conTimeout, int readTimeout) {

        InternetResponse inetResp = null;
        try {
            String resp = "";

            OneCheckoutLogger.log("URL => %s", url);

            if (conTimeout <= 0 || readTimeout <= 0) {
                OneCheckoutLogger.log("connectionTimeout=%d, connectionTimeout=%d", conTimeout, readTimeout);
                inetResp = new InternetResponse();
                inetResp.setMsgResponse("cancelling do http connection");
                inetResp.setHTTPrespCode(-2);

            } else if (url.toLowerCase().contains("http://dokulocal_")) {
                OneCheckoutLogger.log("Direct to jboss 8080");
                String[] result = tembakHttp(url, data, conTimeout, readTimeout);
                inetResp = new InternetResponse();
                inetResp.setHTTPrespCode(Integer.parseInt(result[0]));
                inetResp.setMsgResponse(result[1]);
                return inetResp;
            } else {
                OneCheckoutLogger.log("Direct to goinet");
                InternetRequest iRequest = new InternetRequest();
                iRequest.setURLAddress(url);
                iRequest.setConnectionTimeout(conTimeout * 1000);
                iRequest.setReadTimeout(readTimeout * 1000);
                //iRequest.setMessageData("");
                iRequest.setRequest(RequestType.HTTP_HTTPS);

                if (url.toLowerCase().startsWith("https")) {
                    iRequest.setProtocol(DOKUInternetProtocol.HTTPS_POST);
                } else {
                    iRequest.setProtocol(DOKUInternetProtocol.HTTP_POST);
                }

                //iRequest.setProtocol(DOKUInternetProtocol.HTTP_POST);
                //iRequest.setParameterProperties(params);
                //  String dataencode = URLEncoder.encode(data, "UTF-8");
                iRequest.setMessageData(data);
                inetResp = DokuIntConnection.connect(iRequest);

                OneCheckoutLogger.log("Response Code Http Connection => %d  URL => %s", inetResp.getHTTPrespCode(), url);

//                OneCheckoutLogger.log("Response Message " + inetResp.getMsgResponse());
                if (inetResp.getHTTPrespCode() == HttpURLConnection.HTTP_OK) {
                    OneCheckoutLogger.log("Reading response from merchant");

                } else {
                    OneCheckoutLogger.log("Got HTTP RESPONSE CODE = %d", inetResp.getHTTPrespCode());
                }
            }

            return inetResp;

        } catch (Exception ex) {
            ex.printStackTrace();

            return inetResp;
        }

    }

    public OneCheckoutNotifyStatusResponse notifyPaymentChannel(String url, HashMap<String, String> params, Merchants merchant) {

        OneCheckoutNotifyStatusResponse notifyResp = new OneCheckoutNotifyStatusResponse();

        try {
            OneCheckoutLogger.log("OneCheckoutChannelBase.notifyChannel do Notify Channel to Merchant");

            String data_encode = this.createParamsHTTP(params);

            OneCheckoutLogger.log("OneCheckoutChannelBase.notifyChannel : Post Params" + params.toString());

            OneCheckoutLogger.log("Fetch URL     : %s", url);
            OneCheckoutLogger.log("Merchant Data : %s", data_encode);

            InternetResponse inetResp = this.doFetchHTTP(data_encode, url, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout());

            notifyResp.setACKNOWLEDGE(inetResp.getMsgResponse());
            notifyResp.setHTTP_RESPONSE_CODE(inetResp.getHTTPrespCode());
            notifyResp.setSTATUS(inetResp.getMsgResponse(), merchant.getMerchantNotifyTimeout());

            return notifyResp;

        } catch (Exception ex) {
            ex.printStackTrace();

            return notifyResp;
        }

    }

    public OneCheckoutNotifyStatusResponse sendToMerchant(String url, HashMap<String, String> params, int conTimeout, int readTimeout, Boolean notifyTimeOut) { //, Rates rate1, double purchaseAmount1, String purchaseCurrency1) {
        //public OneCheckoutNotifyStatusResponse sendToMerchantnotifyMerchant(String url, HashMap<String, String> params, int conTimeout, int readTimeout, Boolean notifyTimeOut, Rates rate, double purchaseAmount, String purchaseCurrency) {
        OneCheckoutNotifyStatusResponse notifyResp = new OneCheckoutNotifyStatusResponse();

        try {
            OneCheckoutLogger.log("OneCheckoutChannelBase.notifyMerchant do Notify Status Merchant");

            OneCheckoutLogger.log("OneCheckoutChannelBase.sendToMerchant : Post Params" + params.toString());

            String data_encode = this.createParamsHTTP(params);

            OneCheckoutLogger.log("Fetch URL     : %s", url);
            OneCheckoutLogger.log("Merchant Data : %s", data_encode);

            InternetResponse inetResp = this.doFetchHTTP(data_encode, url, conTimeout, readTimeout);

            notifyResp.setACKNOWLEDGE(inetResp.getMsgResponse());
            notifyResp.setHTTP_RESPONSE_CODE(inetResp.getHTTPrespCode());
            notifyResp.setSTATUS(inetResp.getMsgResponse(), notifyTimeOut);

            return notifyResp;

        } catch (Exception ex) {
            ex.printStackTrace();

            return notifyResp;
        }

    }

    public String generatePaymentRequestWords(OneCheckoutPaymentRequest paymentRequest, Merchants m) {

        try {
            // AMOUNT+MALLID+<password>+TRANSIDMERCHANT
            StringBuilder word = new StringBuilder();

            if (paymentRequest.getRATE() != null) {
                word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getPURCHASEAMOUNT()));
            } else {
                word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            }

            //     word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            word.append(m.getMerchantCode());
            word.append(m.getMerchantHashPassword());
            word.append(paymentRequest.getTRANSIDMERCHANT());

            if (paymentRequest.getRATE() != null) {
                word.append(paymentRequest.getPURCHASECURRENCY());
            } else if (paymentRequest.getPURCHASECURRENCY() != null && !paymentRequest.getPURCHASECURRENCY().equalsIgnoreCase("360")) {
                word.append(paymentRequest.getPURCHASECURRENCY());
            }

            OneCheckoutLogger.log("ONECHECKOUT ELEMENT [" + word.toString() + "]");

            String hashwords = HashWithSHA1.doHashing(word.toString(), m.getMerchantShaFormat(), m.getMerchantHashPassword());

            return hashwords;
        } catch (Throwable iv) {
            iv.printStackTrace();
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }

    public String generateNotifyWords(Transactions trans, Merchants merchant, OneCheckoutDFSStatus dfsStatus, OneCheckoutTransactionStatus status) {

        try {
            //AMOUNT+MALLID+<password>+TRANSIDMERCHANT+RESULTMSG+VERIFYSTATUS

            StringBuilder word = new StringBuilder();

            if (trans.getRates() != null) {
                word.append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
            } else {
                word.append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            }

            word.append(merchant.getMerchantCode());
            word.append(merchant.getMerchantHashPassword());
            word.append(trans.getIncTransidmerchant());
            //word.append(notify.getRESPONSECODE());
            word.append(status.name());
            word.append(dfsStatus.name());

            if (trans.getRates() != null) {
                word.append(trans.getIncPurchasecurrency());
            } else if (!trans.getIncCurrency().equalsIgnoreCase("360")) {
                word.append(trans.getIncCurrency());
            }

            OneCheckoutLogger.log("WORD FOR MERCHANT in Clear Text : %s", word.toString());
            String hashwords = HashWithSHA1.doHashing(word.toString(), merchant.getMerchantShaFormat(), merchant.getMerchantHashPassword());
            // String hashwords = HashWithSHA1.SHA1(word.toString());

            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }

    }

    public String generateNotifyWords(OneCheckoutPaymentRequest payReq, Merchants merchant, OneCheckoutDFSStatus dfsStatus, OneCheckoutTransactionStatus status) {

        try {
            //AMOUNT+MALLID+<password>+TRANSIDMERCHANT+RESULTMSG+VERIFYSTATUS

            StringBuilder word = new StringBuilder();

            if (payReq.getRATE() != null) {
                word.append(OneCheckoutVerifyFormatData.sdf.format(payReq.getPURCHASEAMOUNT()));
            } else {
                word.append(OneCheckoutVerifyFormatData.sdf.format(payReq.getAMOUNT()));
            }

            word.append(merchant.getMerchantCode());
            word.append(merchant.getMerchantHashPassword());
            word.append(payReq.getTRANSIDMERCHANT());
            //word.append(notify.getRESPONSECODE());
            word.append(status.name());
            word.append(dfsStatus.name());

            if (payReq.getRATE() != null) {
                word.append(payReq.getPURCHASECURRENCY());
            } else if (!payReq.getCURRENCY().equalsIgnoreCase("360")) {
                word.append(payReq.getCURRENCY());
            }

            OneCheckoutLogger.log("WORD FOR MERCHANT in Clear Text : %s", word.toString());
            String hashwords = HashWithSHA1.doHashing(word.toString(), merchant.getMerchantShaFormat(), merchant.getMerchantHashPassword());

            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }

    }

    public String generateCheckStatusRequestWords(Transactions trans, Merchants merchant) {
        try {
            //MALLID+<password>+TRANSIDMERCHANT

            StringBuilder word = new StringBuilder();
            //   word.append(OneCheckoutVerifyFormatData.sdf.format(notify.getAMOUNT()));
            word.append(merchant.getMerchantCode());
            word.append(merchant.getMerchantHashPassword());
            word.append(trans.getIncTransidmerchant());

            if (trans.getRates() != null) {
                word.append(trans.getIncPurchasecurrency());//.getPURCHASECURRENCY());
            } else if (trans.getIncPurchasecurrency() != null && !trans.getIncPurchasecurrency().equals("360")) {
                word.append(trans.getIncPurchasecurrency());
            }

            String hashwords = HashWithSHA1.doHashing(word.toString(), merchant.getMerchantShaFormat(), merchant.getMerchantHashPassword());

            OneCheckoutLogger.log("WORD FOR MERCHANT in Clear Text : %s", word);
            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }

    public String generateRedirectWords(OneCheckoutDataPGRedirect redirect, Merchants merchant) {

        try {

            //AMOUNT+<password>+TRANSIDMERCHANT+STATUSCODE
            StringBuilder word = new StringBuilder();
            word.append(OneCheckoutVerifyFormatData.sdf.format(redirect.getAMOUNT()));
            word.append(merchant.getMerchantHashPassword());
            word.append(redirect.getTRANSIDMERCHANT());
            word.append(redirect.getSTATUSCODE());

            if (redirect.getPURCHASECURRENCY() != null && !redirect.getPURCHASECURRENCY().equalsIgnoreCase("360")) {
                word.append(redirect.getPURCHASECURRENCY());
            }

            OneCheckoutLogger.log("WORDS REDIRECT in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), merchant.getMerchantShaFormat(), merchant.getMerchantHashPassword());

            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }

    }

    public IdentifyTrx getTransactionInfo(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, Merchants m) {

        IdentifyTrx identrx = new IdentifyTrx();
        try {

            String invoiceNo = paymentRequest.getTRANSIDMERCHANT();

            String wordDoku = generatePaymentRequestWords(paymentRequest, m);

            OneCheckoutLogger.log("OneCheckoutChannelBase.getTransactionInfo MERCHANT WORDS   [" + paymentRequest.getWORDS() + "]");

            OneCheckoutLogger.log("OneCheckoutChannelBase.getTransactionInfo ONECHECKOUT WORD [" + wordDoku + "]");

            if (!wordDoku.equalsIgnoreCase(paymentRequest.getWORDS())) {
                identrx.setNeedNotify(true);
                identrx.setRequestGood(false);
                identrx.setErrorMsg(OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
                identrx.setStatusCode(identrx.getErrorMsg().value());
            }

            MerchantPaymentChannel mpc = null;
            if (paymentRequest.getVAOpenAmount() != null && paymentRequest.getVAOpenAmount()) {
                mpc = nl.getMerchantPaymentChannelOpenAmount(m, pChannel);
                if (mpc == null) {
                    mpc = nl.getMerchantPaymentChannel(m, pChannel);
                }
            } else {
                mpc = nl.getMerchantPaymentChannel(m, pChannel);
            }

            identrx.setMpc(mpc);
            if (mpc == null) {
                identrx.setErrorMsg(OneCheckoutErrorMessage.PAYMENTCHANNEL_NOT_REGISTERED);
                identrx.setNeedNotify(true);
                identrx.setRequestGood(false);
                identrx.setStatusCode(identrx.getErrorMsg().value());
            } else if (mpc.getMerchantPaymentChannelStatus() == false) {
                identrx.setErrorMsg(OneCheckoutErrorMessage.PAYMENTCHANNEL_MERCHANT_DISABLED);
                identrx.setNeedNotify(true);
                identrx.setRequestGood(false);
                identrx.setStatusCode(identrx.getErrorMsg().value());
            }

            Transactions trans = null;
            if (identrx.isRequestGood()) {
                trans = invoiceValidator(invoiceNo, mpc, paymentRequest.getAMOUNT(), paymentRequest.getSESSIONID());
            }

            if (identrx.isRequestGood() && trans != null) {
                identrx.setStatusCode(trans.getDokuResponseCode());
                if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()
                        || trans.getTransactionsStatus() == OneCheckoutTransactionStatus.REVERSED.value()) {
                    identrx.setErrorMsg(OneCheckoutErrorMessage.MAXIMUM_3_TIMES_ATTEMPT);
                    identrx.setNeedNotify(true);
                    identrx.setRequestGood(false);
                    identrx.setStatusCode(identrx.getErrorMsg().value());
                } else if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value()
                        || trans.getTransactionsStatus() == OneCheckoutTransactionStatus.VOIDED.value()) {
                    identrx.setErrorMsg(OneCheckoutErrorMessage.RE_ENTER_TRANSACTION);
                    identrx.setNeedNotify(false);
                    identrx.setRequestGood(false);
                }
            }

            if (identrx.isRequestGood() && mpc.getMerchants().getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {

                OneCheckoutLogger.log("OneCheckoutChannelBase.getTransactionInfo : PNR %s", paymentRequest.getBOOKINGCODE());
                String pnr = paymentRequest.getBOOKINGCODE();//invoiceNo.substring(0,6);
                OneCheckoutLogger.log("OneCheckoutChannelBase.getTransactionInfo : PNR %s", pnr);
                Transactions trxisFound = PNRValidator(pnr, mpc);

                if (trxisFound != null) {

                    identrx.setNeedNotify(false);
                    identrx.setRequestGood(false);
                    identrx.setErrorMsg(OneCheckoutErrorMessage.DUPLICATE_PNR);
                    identrx.setTrans(trxisFound);
                    identrx.setStatusCode(identrx.getErrorMsg().value());
                }
            }

            return identrx;
        } catch (Exception ex) {

            OneCheckoutLogger.log("OneCheckoutChannelBase.getTransactionInfo Exception %s", ex.getMessage());
            ex.printStackTrace();
            return identrx;
        }

    }

    protected Transactions PNRValidator(String PNR, MerchantPaymentChannel mpc) {

        // Transactions trans = null;
        try {

            List<TransactionsDataAirlines> transList = nl.getTransactionsByPNR(PNR, mpc);//.getTransactionsBy(invoiceNo, mpc);

            if (transList == null) {
                OneCheckoutLogger.log("OneCheckoutChannelBase.PNRValidator : PNR %s never uses before", PNR);
                return null;
            } else {
                if (transList.size() > 0) {

                    OneCheckoutLogger.log("OneCheckoutChannelBase.PNRValidator : PNR %s has been used", PNR);
                    return transList.get(0).getTransactions();
                } else {
                    return null;
                }
            }

        } catch (Exception iv) {
            OneCheckoutLogger.log("OneCheckoutChannelBase.PNRValidator Exception %s", iv.getMessage());
            iv.printStackTrace();
            return null;
        }

    }

    protected Transactions invoiceValidator(String invoiceNo, MerchantPaymentChannel mpc, double amount, String sessionID) {

        // Transactions trans = null;
        try {

            List<Transactions> transList = nl.getTransactionsBy(invoiceNo, mpc);

            if (transList == null) {
                OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator : invoice number %s never uses before", invoiceNo);
                return null;
            }

            int failedCount = 0;
            for (Transactions trans : transList) {

                if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value()
                        && trans.getTransactionsState() == OneCheckoutTransactionState.DONE.value()) {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator : transaction already success, show the transactions");
                    return trans;
                } else if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.VOIDED.value()
                        && trans.getTransactionsState() == OneCheckoutTransactionState.DONE.value()) {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator : transaction already success, show the transactions");
                    return trans;
                } else if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()
                        && trans.getTransactionsState() == OneCheckoutTransactionState.DONE.value()) {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator : invoice number:%s responsecode_FAILED:%s ", invoiceNo, trans.getDokuResponseCode());
                    failedCount++;
                } else if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.REVERSED.value()
                        && trans.getTransactionsState() == OneCheckoutTransactionState.DONE.value()) {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator : invoice number:%s responsecode_REVERSED:%s ", invoiceNo, trans.getDokuResponseCode());
                    failedCount++;
                } else if (trans.getTransactionsState() == OneCheckoutTransactionState.INCOMING.value() || trans.getTransactionsState() == OneCheckoutTransactionState.NOTIFYING.value()) {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator : invoice number:%s responsecode_INCOMING:%s ", invoiceNo, trans.getDokuResponseCode());
                    failedCount++;

                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
//                    trans.setDokuResponseCode(OneCheckoutErrorMessage.RE_ENTER_TRANSACTION.value());
//                    trans.setDokuResultMessage(OneCheckoutErrorMessage.RE_ENTER_TRANSACTION.name());
                    em.merge(trans);
                }

//                if (failedCount > MAX_FAILED_TRANS - 1) {
//                    OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator : transaction more than 3 times attempt");
//                    return trans;
//                }
            }

            return null;
        } catch (Exception iv) {
            iv.getStackTrace();
            OneCheckoutLogger.log("OneCheckoutChannelBase.invoiceValidator Exception");

            return null;
        }

    }

    public String generateDokuWords(OneCheckoutPaymentRequest paymentRequest, MerchantPaymentChannel mpc) {

        try {
            //String WORDS = HashWithSHA1.SHA1(PurchaseAmt + MerchantID + mpc.getMerchantPaymentChannelHash() + OrderNumber);

            StringBuilder word = new StringBuilder();
            word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            word.append(mpc.getMerchantPaymentChannelUid());
            word.append(mpc.getMerchantPaymentChannelHash());
            word.append(paymentRequest.getTRANSIDMERCHANT());
            OneCheckoutLogger.log("COMPONENT_WORDS_TO_DOKUWALLET[" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            OneCheckoutLogger.log("WORDS_TO_DOKUWALLET[" + hashwords + "]");
            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }

    public String generateDokuMPGWords(OneCheckoutPaymentRequest paymentRequest, MerchantPaymentChannel mpc, String currency_code) {

        try {

            StringBuilder word = new StringBuilder();
            word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));

            word.append(mpc.getPaymentChannelCode());
//            if (mpc.getPaymentChannelChainCode() != null || mpc.getPaymentChannelChainCode() > 0) {
//                word.append(mpc.getPaymentChannelChainCode() + "");
//            }

            word.append(paymentRequest.getTRANSIDMERCHANT());
            word.append(mpc.getMerchantPaymentChannelHash());
            word.append(currency_code);
            OneCheckoutLogger.log("WORD in Clear Text : %s", word.toString());
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }

    public String generateDOKUVerifyRequestWords(OneCheckoutDOKUVerifyData verifyRequest, Transactions trans) {

        try {
            //sdf.format(oi.getPurchaseAmount()) + oi.getMid() + hashPassword + oi.getTransIdMerchant()
            StringBuilder word = new StringBuilder();

            word.append(OneCheckoutVerifyFormatData.sdf.format(verifyRequest.getAMOUNT()));
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(verifyRequest.getTRANSIDMERCHANT());

            OneCheckoutLogger.log(" AMOUNT : " + OneCheckoutVerifyFormatData.sdf.format(verifyRequest.getAMOUNT()));
            OneCheckoutLogger.log(" MID : " + trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
            OneCheckoutLogger.log(" HASHPWD : " + trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            OneCheckoutLogger.log(" TRANSIDMERCHANT : " + verifyRequest.getTRANSIDMERCHANT());

            OneCheckoutLogger.log("WORD in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            iv.printStackTrace();
            throw new InvalidDOKUVerifyDataException(iv.getMessage());
        }

    }

    public String generateMPGVerifyRequestWords(OneCheckoutDOKUVerifyData verifyRequest, Transactions trans) {

        try {
            //sdf.format(oi.getPurchaseAmount()) + oi.getMid() + hashPassword + oi.getTransIdMerchant()
            StringBuilder word = new StringBuilder();

            OneCheckoutLogger.log(" AMOUNT          : " + verifyRequest.getAMOUNT());
            OneCheckoutLogger.log(" HASHPWD         : " + trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            OneCheckoutLogger.log(" TRANSIDMERCHANT : " + verifyRequest.getTRANSIDMERCHANT());
            OneCheckoutLogger.log(" MALLID          : " + trans.getMerchantPaymentChannel().getPaymentChannelCode());

            word.append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            word.append(verifyRequest.getTRANSIDMERCHANT());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(trans.getMerchantPaymentChannel().getPaymentChannelCode());

            if (trans.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && trans.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                word.append(trans.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
            }

            Currency currency = nl.getCurrencyByCode(trans.getIncCurrency());
            word.append(currency.getAlpha3Code());
            OneCheckoutLogger.log("WORD in Clear Text : %s", word);

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            iv.printStackTrace();
            throw new InvalidDOKUVerifyDataException(iv.getMessage());
        }

    }

    public String generateDOKUNotifyRequestWords(OneCheckoutDOKUNotifyData notifyRequest, Transactions trans) {

        try {
            //sdf.format(oi.getPurchaseAmount()) + oi.getMid() + hashPassword + oi.getTransIdMerchant() + result
            StringBuilder word = new StringBuilder();

            word.append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(notifyRequest.getTRANSIDMERCHANT());
            word.append(notifyRequest.getRESULT());

            OneCheckoutLogger.log(" =================================================================== ");
            OneCheckoutLogger.log(" AMOUNT : " + OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
            OneCheckoutLogger.log(" MID : " + trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
            OneCheckoutLogger.log(" HASHPWD : " + trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            OneCheckoutLogger.log(" TRANSIDMERCHANT : " + notifyRequest.getTRANSIDMERCHANT());
            OneCheckoutLogger.log(" RESULT : " + notifyRequest.getRESULT());

            OneCheckoutLogger.log(" WORD FROM PAYMENTCHANNEL in Clear Text : %s", word.toString());
            OneCheckoutLogger.log(" =================================================================== ");

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            throw new InvalidDOKUNotifyDataException(iv.getMessage());
        }

    }

    public String generateMPGNotifyRequestWords(OneCheckoutDOKUNotifyData notifyRequest, Transactions trans) {

        try {
            //sdf.format(oi.getPurchaseAmount()) + oi.getMid() + hashPassword + oi.getTransIdMerchant() + result
            StringBuilder word = new StringBuilder();

            OneCheckoutLogger.log(" =================================================================== ");
            OneCheckoutLogger.log(" TRANSIDMERCHANT : " + notifyRequest.getTRANSIDMERCHANT());
            OneCheckoutLogger.log(" HASHPWD         : " + trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            OneCheckoutLogger.log(" MALLID          : " + trans.getMerchantPaymentChannel().getPaymentChannelCode());

            word.append(notifyRequest.getTRANSIDMERCHANT());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());

            word.append(trans.getMerchantPaymentChannel().getPaymentChannelCode());

            if (trans.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && trans.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                word.append(trans.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
                OneCheckoutLogger.log(" CHAINMALLID     : " + trans.getMerchantPaymentChannel().getPaymentChannelChainCode());
            }

            word.append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
            Currency currency = nl.getCurrencyByCode(trans.getIncCurrency());
            word.append(currency.getAlpha3Code());

            OneCheckoutLogger.log(" =================================================================== ");

            OneCheckoutLogger.log(" WORD FROM PAYMENTCHANNEL in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            throw new InvalidDOKUNotifyDataException(iv.getMessage());
        }

    }

    public String generateMIBNotifyRequestWords(OneCheckoutDOKUNotifyData notifyRequest, Transactions trans) {

        try {
            //transaction.getInvoiceNumber() + shareKey.trim() + mallId + chainMallId + amount + transaction.getCurrencyId().getId();
            StringBuilder words = new StringBuilder();
            OneCheckoutLogger.log(" =================================================================== ");
            OneCheckoutLogger.log(" TRANSIDMERCHANT : " + notifyRequest.getTRANSIDMERCHANT());
            OneCheckoutLogger.log(" HASHPWD         : " + trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            OneCheckoutLogger.log(" MALLID          : " + trans.getMerchantPaymentChannel().getPaymentChannelCode());
            words.append(notifyRequest.getTRANSIDMERCHANT());
            words.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            words.append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
            if (trans.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && trans.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                words.append(trans.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
                OneCheckoutLogger.log(" CHAINMALLID     : " + trans.getMerchantPaymentChannel().getPaymentChannelChainCode());
            }
            words.append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
            Currency currency = nl.getCurrencyByCode(trans.getIncCurrency());
            words.append(currency.getAlpha3Code());
            OneCheckoutLogger.log(" =================================================================== ");
            OneCheckoutLogger.log(" WORDS FROM PAYMENTCHANNEL in Clear Text : %s", words.toString());
            String hashwords = HashWithSHA1.doHashing(words.toString(), null, null);
            return hashwords;
        } catch (Exception iv) {
            throw new InvalidDOKUNotifyDataException(iv.getMessage());
        }

    }

    protected OneCheckoutNotifyStatusRequest createEmptyNotify(OneCheckoutCheckStatusData statusRequest, OneCheckoutPaymentChannel pChannel, OneCheckoutErrorMessage oneCheckoutErrorMessage) {

        OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
        notify.setAMOUNT(BigDecimal.ZERO.doubleValue());
        notify.setAPPROVALCODE("");
        notify.setBANK("");
        notify.setMCN("");
        notify.setPAYMENTCHANNEL(pChannel.value());
        notify.setPAYMENTDATETIME("");
        notify.setRESPONSECODE(oneCheckoutErrorMessage.value());
        notify.setRESULTMSG(oneCheckoutErrorMessage.name());
        notify.setSESSIONID(statusRequest.getSESSIONID());
        notify.setSTATUSTYPE("P");
        notify.setTRANSIDMERCHANT(statusRequest.getTRANSIDMERCHANT());
        notify.setVERIFYID("");
        notify.setVERIFYSCORE("-1");
        notify.setVERIFYSTATUS(OneCheckoutDFSStatus.NA.name());

        return notify;
    }

    public String generateVoidWords(OneCheckoutVoidRequest voidRequest, Merchants m) {

        try {

            //MALLID+<password>+TRANSIDMERCHANT+SESSIONID
            StringBuilder word = new StringBuilder();
            word.append(m.getMerchantCode());
            word.append(m.getMerchantHashPassword());
            word.append(voidRequest.getTRANSIDMERCHANT());
            word.append(voidRequest.getSESSIONID());

            OneCheckoutLogger.log("ONECHECKOUT ELEMENT [" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), m.getMerchantShaFormat(), m.getMerchantHashPassword());

            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }

    }

    public String generateMPGVoidWords(OneCheckoutVoidRequest voidRequest, MerchantPaymentChannel mpc) {

        try {

            //MALLID+<password>+TRANSIDMERCHANT+SESSIONID
            StringBuilder word = new StringBuilder();
            word.append(voidRequest.getTRANSIDMERCHANT());
            word.append(mpc.getMerchantPaymentChannelHash());
            word.append(voidRequest.getApprovalCode());
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }

    }

    public String postMIP(String data, String url, PaymentChannel pc) {

        try {

            InternetResponse inetResp = this.doFetchHTTP(data, url, pc.getPaymentChannelConnectionTimeout(), pc.getPaymentChannelReadTimeout());

            return inetResp.getMsgResponse() != null ? !inetResp.getMsgResponse().equals("") ? inetResp.getMsgResponse() : "ERROR" : "ERROR";

        } catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }
    }
    
    public String PostTOMerchant(String data, String url, MerchantPaymentChannel merchantPaymentChannel) {

        try {

//            OneCheckoutLogger.log("OneCheckoutChannelBase.CyberSourceNotifyMerchant do notify CyberSource Review to Merchant");
            OneCheckoutLogger.log("OneCheckoutChannelBase.PostTOMerchant: " + data);

            InternetResponse inetResp = this.doFetchHTTP(data, url, merchantPaymentChannel.getPaymentChannel().getPaymentChannelConnectionTimeout(), merchantPaymentChannel.getPaymentChannel().getPaymentChannelReadTimeout());

            return inetResp.getMsgResponse();

        } catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }

    }

    public String postMIP(HashMap<String, String> data, String url, PaymentChannel pc) {
        try {
            String request = "";
            Iterator i = data.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                String value = (String) (data.get(key) != null ? data.get(key) : "");
                if (request.equals("")) {
                    request = URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
                } else {
                    request += "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
                }
            }
            InternetResponse inetResp = this.doFetchHTTP(request, url, pc.getPaymentChannelConnectionTimeout(), pc.getPaymentChannelReadTimeout());
            return inetResp.getMsgResponse() != null ? !inetResp.getMsgResponse().equals("") ? inetResp.getMsgResponse() : "ERROR" : "ERROR";
        } catch (Throwable ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public String generateDOKUAgregatorVerifyRequestWords(OneCheckoutDOKUVerifyData verifyRequest, Transactions trans) {

        try {
            StringBuilder word = new StringBuilder();

            word.append(verifyRequest.getCOMPANYCODE());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(verifyRequest.getMALLID() + verifyRequest.getUSERACCOUNT());

            OneCheckoutLogger.log("WORD FROM CHANNEL in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage());
        }
    }

    public String generateDOKUAgregatorNotifyRequestWords(OneCheckoutDOKUNotifyData notifyRequest, Transactions trans) {

        try {
            StringBuilder word = new StringBuilder();

            word.append(OneCheckoutVerifyFormatData.sdf.format(notifyRequest.getAMOUNT()));
            word.append(notifyRequest.getCOMPANYCODE());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(notifyRequest.getMERCHANTCODE() + notifyRequest.getUSERACCOUNT());

            OneCheckoutLogger.log("WORD FROM CHANNEL in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage());
        }
    }

    public String generatePermataNotifyRequestWords(OneCheckoutDOKUNotifyData notifyRequest, Transactions trans) {

        try {
            StringBuilder word = new StringBuilder();

            word.append(OneCheckoutVerifyFormatData.sdf.format(notifyRequest.getAMOUNT()));
            word.append(notifyRequest.getPERMATA_MALLID());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(notifyRequest.getTRANSIDMERCHANT());
            word.append(notifyRequest.getRESULTMSG());

            OneCheckoutLogger.log("WORDS FROM CHANNEL in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage());
        }
    }

    public String generateBCANotifyRequestWords(OneCheckoutDOKUNotifyData notifyRequest, Transactions trans) {

        try {
            StringBuilder word = new StringBuilder();

//            word.append(OneCheckoutVerifyFormatData.sdf.format(notifyRequest.getAMOUNT()));
//            word.append(notifyRequest.getBCA_MALLID());
//            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
//            word.append(notifyRequest.getTRANSIDMERCHANT());
//            word.append(notifyRequest.getRESULTMSG());
            word.append(OneCheckoutVerifyFormatData.sdf.format(notifyRequest.getAMOUNT()));
            word.append(notifyRequest.getCOMPANYCODE());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());

            OneCheckoutLogger.log("WORDS FROM CHANNEL in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;

        } catch (Exception iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage());
        }
    }

    public String generateSinarMasNotifyRequestWords(OneCheckoutDOKUNotifyData notifyRequest, Transactions trans) {

        try {
            StringBuilder word = new StringBuilder();

            word.append(OneCheckoutVerifyFormatData.sdf.format(notifyRequest.getAMOUNT()));
            word.append(notifyRequest.getSINARMAS_MALLID());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(notifyRequest.getCOMPANYCODE());
            word.append(notifyRequest.getSINARMAS_VANUMBER());

            OneCheckoutLogger.log("WORDS FROM CHANNEL in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), "SHA1", null);

            return hashwords;

        } catch (Exception iv) {
            throw new InvalidDOKUVerifyDataException(iv.getMessage());
        }
    }

    public String generateMerchantRedirectWordsBeforePayment(OneCheckoutPaymentRequest paymentRequest, Merchants merchant) {

        try {
            //String WORDS = HashWithSHA1.SHA1(PurchaseAmt + MerchantID + mpc.getMerchantPaymentChannelHash() + OrderNumber);

            StringBuilder word = new StringBuilder();
            word.append(OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            word.append(merchant.getMerchantHashPassword());
            word.append(paymentRequest.getTRANSIDMERCHANT());
            word.append(OneCheckoutErrorMessage.NOT_YET_PAID.value());//

            OneCheckoutLogger.log("WORDS NOT YET PAID BEFORE REDIRECT PAYMENT in Clear Text : %s", word.toString());

            String hashwords = HashWithSHA1.doHashing(word.toString(), merchant.getMerchantShaFormat(), merchant.getMerchantHashPassword());
            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }

    public HashMap<String, String> stringToHashMap(String hasmap) {
        try {
            Properties props = new Properties();
            props.load(new StringReader(hasmap.substring(1, hasmap.length() - 1).replace(", ", "\n")));
            HashMap<String, String> hashMap = new HashMap<String, String>();
            for (Map.Entry<Object, Object> e : props.entrySet()) {
                String key = (String) e.getKey();
                String val = (String) e.getValue();
                hashMap.put((String) e.getKey(), (String) e.getValue());
            }
            return hashMap;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public String CyberSourceNotifyMerchant(String data, String url, Merchants merchant) {

        try {

            OneCheckoutLogger.log("OneCheckoutChannelBase.CyberSourceNotifyMerchant do notify CyberSource Review to Merchant");
            OneCheckoutLogger.log("OneCheckoutChannelBase.CyberSourceNotifyMerchant : " + data);
            String data_encode = URLEncoder.encode("content", "UTF-8") + "=" + URLEncoder.encode(data, "UTF-8");

            InternetResponse inetResp = this.doFetchHTTP(data_encode, url, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout());

            return inetResp.getMsgResponse();

        } catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }

    }

    protected HashMap<String, String> getData(Transactions trans) {

        try {
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();

            HashMap<String, String> params = new HashMap<String, String>();

            if (trans.getRates() != null) {

                params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
                params.put("CURRENCY", trans.getIncPurchasecurrency());
            } else {

                params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
                params.put("CURRENCY", trans.getIncCurrency());

            }
            params.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());

            if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.REVERSED.value() || trans.getTransactionsStatus() == OneCheckoutTransactionStatus.VOIDED.value()) {
                params.put("STATUSTYPE", "V");
            } else {
                params.put("STATUSTYPE", "P");
            }

            params.put("RESPONSECODE", trans.getDokuResponseCode());
            params.put("APPROVALCODE", trans.getDokuApprovalCode());
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.FAILED;
            OneCheckoutLogger.log("OneCheckoutChannelBase.getData TRANSACTIONID[" + trans.getTransactionsId() + "]");
            OneCheckoutLogger.log("OneCheckoutChannelBase.getData DOKU RESPONSE CODE[" + trans.getDokuResponseCode() + "]");
            OneCheckoutLogger.log("OneCheckoutChannelBase.getData TRANSACTION STATE[" + trans.getTransactionsState() + "]");
            if (trans.getDokuResponseCode() != null && trans.getDokuResponseCode().equals(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value())) {
                if (trans.getTransactionsState() == null || trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                    status = OneCheckoutTransactionStatus.ONPROCESS;
                } else {
                    status = OneCheckoutTransactionStatus.ERROR;
                }
            } else {
                if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value()) {
                    status = OneCheckoutTransactionStatus.SUCCESS;
                } else if (trans.getTransactionsState() != null && trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                    status = OneCheckoutTransactionStatus.UNPAID;
                    params.put("RESPONSECODE", OneCheckoutErrorMessage.NOT_YET_PAID.value());
                }
            }
            params.put("RESULTMSG", status.name());

            OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.NA;
            try {
                dfsStatus = OneCheckoutDFSStatus.findType(trans.getVerifyStatus());
            } catch (Exception ex) {

            }

            OneCheckoutDFSStatus eduStatus = OneCheckoutDFSStatus.NA;
            try {
                eduStatus = OneCheckoutDFSStatus.findType(trans.getEduStatus());
            } catch (Exception ex) {

            }

            String mword = base.generateNotifyWords(trans, trans.getMerchantPaymentChannel().getMerchants(), dfsStatus, status);
            String paymentDate = "";
            if (trans.getDokuInvokeStatusDatetime() != null) {
                paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            }

            params.put("WORDS", mword);

            params.put("PAYMENTCHANNEL", trans.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());

            String channelList = config.getString("ONECHECKOUT.LIST.CHANNEL.VA", "_05 _07 _08 _09 _10 _11 _14 _20 _21 _22").trim();
            if (channelList.toUpperCase().indexOf(trans.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId()) >= 0) {
                params.put("PAYMENTCODE", trans.getAccountId());
            } else {
                params.put("PAYMENTCODE", "");
            }
            params.put("SESSIONID", trans.getIncSessionid());
            params.put("BANK", trans.getDokuIssuerBank());
            params.put("MCN", this.createMCN(trans));
            params.put("PAYMENTDATETIME", paymentDate);

            params.put("VERIFYID", trans.getVerifyId() != null ? trans.getVerifyId() : "");
            params.put("VERIFYSCORE", trans.getVerifyScore() != null ? trans.getVerifyScore() + "" : "-1");
            params.put("VERIFYSTATUS", dfsStatus.name());

            String edustatus1 = null;
            if (dfsStatus.name().equals(OneCheckoutDFSStatus.APPROVE.name())) {
                edustatus1 = OneCheckoutDFSStatus.APPROVE.name();
            } else if (dfsStatus.name().equals(OneCheckoutDFSStatus.REVIEW.name())) {
                edustatus1 = OneCheckoutDFSStatus.PENDING.name();
            } else if (dfsStatus.name().equals(OneCheckoutDFSStatus.REJECT.name())) {
                edustatus1 = OneCheckoutDFSStatus.REJECT.name();
            } else {
                edustatus1 = OneCheckoutDFSStatus.NA.name();
            }
            params.put("EDUSTATUS", edustatus1);
            params.put("PURCHASECURRENCY", trans.getIncPurchasecurrency());

            Merchants m = trans.getMerchantPaymentChannel().getMerchants();
            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {
                try {
                    if (trans.getTransactionsDataAirlineses() != null && trans.getTransactionsDataAirlineses().size() > 0) {
                        TransactionsDataAirlines AI = trans.getTransactionsDataAirlineses().iterator().next();
                        params.put("BOOKINGCODE", AI.getIncBookingcode());
                    }
                } catch (org.hibernate.LazyInitializationException le) {
                    OneCheckoutLogger.log(le.getMessage());
                    TransactionsDataAirlines AI = nl.getPassengerData(trans);
                    if (AI != null) {
                        params.put("BOOKINGCODE", AI.getIncBookingcode());
                    }
                } catch (Throwable ex) {
                    TransactionsDataAirlines AI = nl.getPassengerData(trans);
                    if (AI != null) {
                        params.put("BOOKINGCODE", AI.getIncBookingcode());
                    }
                }
            }

            // === ONLY BSP / MPG === //
            PaymentChannel pc = trans.getMerchantPaymentChannel().getPaymentChannel();//.getMerchants();
            OneCheckoutPaymentChannel opc = OneCheckoutPaymentChannel.findType(pc.getPaymentChannelId());
            if (opc == OneCheckoutPaymentChannel.BSP || opc == OneCheckoutPaymentChannel.CreditCard || opc == OneCheckoutPaymentChannel.BNIDebitOnline) {
                try {
                    if (trans.getTransactionsDataCardholders() != null && trans.getTransactionsDataCardholders().size() > 0) {
                        TransactionsDataCardholder cardholder = trans.getTransactionsDataCardholders().iterator().next();
                        params.put("CHNAME", cardholder.getIncCcName() != null ? cardholder.getIncCcName() : "");
                    }
                } catch (org.hibernate.LazyInitializationException le) {
                    OneCheckoutLogger.log(le.getMessage());
                    TransactionsDataCardholder ch = nl.getCH(trans);
                    if (ch != null) {
                        params.put("CHNAME", ch.getIncCcName() != null ? ch.getIncCcName() : "");
                    }
                } catch (Throwable ex) {
                    TransactionsDataCardholder ch = nl.getCH(trans);
                    if (ch != null) {
                        params.put("CHNAME", ch.getIncCcName() != null ? ch.getIncCcName() : "");
                    }

                }
            }

            params.put("THREEDSECURESTATUS", "");
            if (trans.getInc3dSecureStatus() != null && trans.getInc3dSecureStatus().equalsIgnoreCase("TRUE")) {
                params.put("THREEDSECURESTATUS", "TRUE");
            } else if (trans.getInc3dSecureStatus() != null && trans.getInc3dSecureStatus().equalsIgnoreCase("FALSE")) {
                params.put("THREEDSECURESTATUS", "FALSE");
            }

            params.put("LIABILITY", trans.getIncLiability() != null ? trans.getIncLiability() : "");
            params.put("PURCHASECURRENCY", trans.getIncPurchasecurrency());

            //FOR BSP / MPG
            try {

                //if (!params.get("THREEDSECURESTATUS").isEmpty()) {
                if (opc == OneCheckoutPaymentChannel.BSP || opc == OneCheckoutPaymentChannel.CreditCard || opc == OneCheckoutPaymentChannel.BNIDebitOnline) {
                    char PAN1 = trans.getAccountId().charAt(0);
                    if (PAN1 == '4') {
                        params.put("BRAND", "VISA");
                    } else if (PAN1 == '5') {
                        params.put("BRAND", "MASTERCARD");
                    } else if (PAN1 == '3') {
                        params.put("BRAND", "AMEX");
                    } else if (PAN1 == '1' && params.get("BANK") != null && params.get("BANK").toUpperCase().indexOf("UATP") >= 0) {
                        params.put("BRAND", "UATP");
                    }

                }

            } catch (Throwable t) {
                OneCheckoutLogger.log("OneCheckoutChannelBase.getData : ADDITIONALDATA is NPE, This is SWIPER MERCHANT");
            }

            String exeptionalMallId = "";
            String grepMallId = "";

            try {
                exeptionalMallId = config.getString("ONECHECKOUT.SWIPER.ADDITIONALDATA");
                if (exeptionalMallId != null && !exeptionalMallId.trim().equals("")) {
                    grepMallId = m.getMerchantCode() + "";
                    OneCheckoutLogger.log("OneCheckoutChannelBase.getData ONECHECKOUT.SWIPER.ADDITIONALDATA = %s", exeptionalMallId);
                    if (m.getMerchantChainMerchantCode() != null && m.getMerchantChainMerchantCode() > 0) {
                        grepMallId = m.getMerchantCode() + "_" + m.getMerchantChainMerchantCode();
                    }

                    OneCheckoutLogger.log("OneCheckoutChannelBase.getData MERCHANTID_CHAINMERCHANTID = %s", grepMallId);

                    //trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    if (exeptionalMallId != null && exeptionalMallId.contains(grepMallId)) {
                        OneCheckoutLogger.log("OneCheckoutChannelBase.getData - This merchant is SWIPER ADDITIONAL DATA");
                        //trans.setTransactionsState(OneCheckoutTransactionState.PROCESS.value());
                        params.put("ADDITIONALDATA", trans.getIncAdditionalInformation());
                        OneCheckoutLogger.log("OneCheckoutChannelBase.getData Add Parameter ADDITIONAL_DATA = %s", trans.getIncAdditionalInformation());

                    } else {
                        OneCheckoutLogger.log("OneCheckoutChannelBase.getData - This merchant is NOT SWIPER ADDITIONAL DATA");
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                exeptionalMallId = null;
                OneCheckoutLogger.log("OneCheckoutChannelBase.getData ERROR : " + t.getMessage());
                OneCheckoutLogger.log("OneCheckoutChannelBase.getData Can't Find Properti in Apps Config");
            }

            OneCheckoutLogger.log("OneCheckoutChannelBase.getData : Param List " + params.toString());

            return params;//new Object[]{notify, params};

        } catch (Exception ex) {

            ex.printStackTrace();
            return null;//new Object[]{0, null};

        }

    }

    protected boolean sendEmail(String bodyemail, String to, String subject, PropertiesConfiguration config) {

        //  String to = config.getString("EMAIL.TO." + mallId,"hafiz@nsiapay.com");
        String from = config.getString("mail.account", "system-noreply@doku.com");
        String cc = config.getString("mail.cc", "rudy.thong@doku.com");
        String host = config.getString("mail.host", "smtp.gmail.com");
        String password = config.getString("mail.password", "n51aJKT!@#");

        InternetRequest iRequest = new InternetRequest();
        iRequest.setRequest((RequestType.EMAIL));
        iRequest.setEmailTo(to);
        iRequest.setEmailFrom(from);
        iRequest.setEmailCc(cc);
        iRequest.setEmailHost(host);
        iRequest.setEmailMessageBody(bodyemail);
        iRequest.setEmailMessageHeaderType("text/html");
        iRequest.setEmailPassword(password);
        iRequest.setEmailSubject(subject);

        InternetResponse inetResp = DokuIntConnection.connect(iRequest);

        if (inetResp.isStatusEmail()) {
            OneCheckoutLogger.log("sending email succeded");
        } else {

            OneCheckoutLogger.log("sending email failed");
        }

        if (inetResp.isStatusEmail()) {
            OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
            return true;
        } else {
            OneCheckoutLogger.log("FAILED SENDING EMAIL...");
            return false;

        }

    }

    protected void doEmail(Transactions trans, PropertiesConfiguration config) {
///apps/onecheckoutv1/email/email_notify_status.html

        try {
            String templatePath = config.getString("EMAIL.ALERT.TEMPLATE", "/apps/onecheckoutv1/email/");

            String templateFileName = "email_notify_status.html";
            File file = new File(templatePath + templateFileName);
            boolean exists = file.exists();

            if (!exists) {
                OneCheckoutLogger.log(templateFileName + " does not exist");
                return;
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);

            Map root = new HashMap();

            HashMap<String, String> params = getData(trans);
            ArrayList paramList = convertHashMap(params);

            root.put("itemlist", paramList);

            root.put("merchantName", trans.getMerchantPaymentChannel().getMerchants().getMerchantName());
            root.put("MALLID", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode());
            root.put("invoiceNo", trans.getIncTransidmerchant());

            StringWriter writer = new StringWriter();
            temp.process(root, writer);

            String subject = "[" + trans.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "-" + trans.getIncTransidmerchant() + "] Notify Timeout Alert " + OneCheckoutVerifyFormatData.email_datetimeFormat.format(new Date());
            String to = trans.getMerchantPaymentChannel().getMerchants().getMerchantEmail();
            boolean result = sendEmail(writer.toString(), to, subject, config);//sendEmail(,email,config);

            if (result) {
                trans.setSystemMessage("EMAILSUCCESS");
                em.merge(trans);
            }
            //   return sendEmail(writer.toString(),email,config);

        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("ERROR : %s", ex.getMessage());
            //return false;
        }

    }

    public ArrayList convertHashMap(HashMap<String, String> params) {
        ArrayList list = new ArrayList();
        if (params.isEmpty()) {
            return list;
        } else {
//            OneCheckoutLogger.log("Number of Items : %d", params.size());
            Set m = params.keySet();
            int i = 0;
            for (Object o : m) {
                String[] param = new String[2];
                String key = (String) o;
                String val = null;
                val = params.get(key);
                param[0] = key;

                if (val == null) {
                    param[1] = "";
                } else {
                    param[1] = val;
                }

                list.add(i, param);
                i++;
            }

        }

        return list;

    }

    public String createParamsHTTP(HashMap<String, String> params) {

        String res = "";
        try {
            Set m = params.keySet();
            StringBuilder sb = new StringBuilder();
            for (Object o : m) {
                String[] param = new String[2];
                String key = (String) o;
                String val = params.get(key);
                if (val == null || val.isEmpty()) {
                    val = "";
                }

                if (sb.length() == 0) {
                    sb.append(key).append("=").append(URLEncoder.encode(val, "UTF-8"));
                } else {
                    sb.append("&").append(key).append("=").append(URLEncoder.encode(val, "UTF-8"));
                }

            }
            return sb.toString();
        } catch (Exception ex) {
            return res;
        }
    }

    public String createMCN(Transactions trans) {

        String publicKeyString = config.getString("ONECHECKOUT.PUBLICKEY." + trans.getIncMallid(), "NOKEY").trim();
        try {
            if (!publicKeyString.equalsIgnoreCase("NOKEY")) {

                String text = trans.getSystemSession();

                String[] sess = text.split("\\|");

                return sess[3];
            } else {
                return trans.getAccountId();
            }

        } catch (Exception ex) {
            OneCheckoutLogger.log("OneCheckoutChannelBase.createMCN : Exception " + ex.getMessage());
            return trans.getAccountId();
        }
    }

    // dibawah ini, penambahan berkenaan dengan MIP ke masing-masing CORE
    protected OneCheckoutDataHelper createRedirectAndNotifyCaseFail(OneCheckoutDataHelper trxHelper, OneCheckoutErrorMessage errormsg, boolean needNotify, Transactions trans) {

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutChannelBase.createRedirectAndNotifyCaseFail - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();

            String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
            String statusCode = errormsg.value();

            if (needNotify) {

                String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
                OneCheckoutLogger.log("OneCheckoutChannelBase.createRedirectAndNotifyCaseFail - %s", errormsg.name());

                OneCheckoutLogger.log("OneCheckoutChannelBase.createRedirectAndNotifyCaseFail - T1.1: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.FAILED;//OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());            
                OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.NA;//OneCheckoutDFSStatus.findType(trans.getVerifyStatus());       

                String word = generateNotifyWords(paymentRequest, m, dfsStatus, status);

                HashMap<String, String> params = new HashMap<String, String>();
                params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                params.put("CURRENCY", paymentRequest.getCURRENCY());

                if (paymentRequest.getRATE() != null) {

                    params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getPURCHASEAMOUNT()));
                    params.put("CURRENCY", paymentRequest.getPURCHASECURRENCY());
                }

                params.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                params.put("WORDS", word);
                params.put("STATUSTYPE", "P");
                params.put("RESPONSECODE", errormsg.value());
                params.put("APPROVALCODE", "");
                params.put("RESULTMSG", "FAILED");
                params.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());

                if (paymentRequest.getPAYCODE() != null) {
                    params.put("PAYMENTCODE", paymentRequest.getPAYCODE());
                } else {
                    params.put("PAYMENTCODE", "");
                }

                params.put("SESSIONID", paymentRequest.getSESSIONID());
                params.put("BANK", "");
                params.put("MCN", paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
                params.put("PAYMENTDATETIME", paymentDate);
                params.put("VERIFYID", "");
                params.put("VERIFYSCORE", "-1");
                params.put("VERIFYSTATUS", OneCheckoutDFSStatus.NA.name());
                params.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());

                OneCheckoutNotifyStatusResponse ack = sendToMerchant(m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());//, paymentRequest.getRATE(), paymentRequest.getPURCHASEAMOUNT(), paymentRequest.getPURCHASECURRENCY());

                OneCheckoutLogger.log("OneCheckoutChannelBase.createRedirectAndNotifyCaseFail - T1.2: %d finish Notify to Merchant", (System.currentTimeMillis() - t1));
            }

            //set data redirect yang akan di kirim ke merchant
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setPageTemplate(redirect.getProgressPage());

            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            redirect.setSTATUSCODE(statusCode);
            redirect.setPURCHASECURRENCY(paymentRequest.getPURCHASECURRENCY());

            Map data = redirect.getParameters();

            if (paymentRequest.getRATE() != null) {
                redirect.setAMOUNT(paymentRequest.getPURCHASEAMOUNT());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getPURCHASEAMOUNT()));
                data.put("CURRENCY", paymentRequest.getPURCHASECURRENCY());
            } else {
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                data.put("CURRENCY", paymentRequest.getCURRENCY());

            }
            data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            data.put("WORDS", generateRedirectWords(redirect, m));
            data.put("STATUSCODE", statusCode);
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", paymentRequest.getSESSIONID());
            data.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());

            data.put("PAYMENTCODE", "");
            redirect.setParameters(new HashMap(data));

            OneCheckoutLogger.log("============== START REDIRECT PARAMETER ==============");
//            OneCheckoutLogger.log("MERCHANT URL         [" + m.getMerchantRedirectUrl() + "]");
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

            HashMap resultData = redirect.getRetryData();

            Currency currency = nl.getCurrencyByCode(paymentRequest.getPURCHASECURRENCY());

            if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur && trans != null) {
                resultData.put("CUSTOMERNAME", trans.getIncName());
                resultData.put("BILLINGNUMBER", trans.getTransactionsDataRecur().getIncBillNumber());
                resultData.put("BILLINGDETAIL", trans.getTransactionsDataRecur().getIncBillDetail());
                resultData.put("STARTDATE", trans.getTransactionsDataRecur().getIncStartDate());
                resultData.put("ENDDATE", trans.getTransactionsDataRecur().getIncEndDate());
                resultData.put("EXECUTEDATE", trans.getTransactionsDataRecur().getIncExecuteDate());
                resultData.put("EXECUTEMONTH", trans.getTransactionsDataRecur().getIncExecuteMonth());
            }
            resultData.put("DATENOW", OneCheckoutVerifyFormatData.detailDateFormat.format(new Date()));
            resultData.put("DISPLAYNAME", m.getMerchantName());
            resultData.put("DISPLAYADDRESS", m.getMerchantAddress());
            resultData.put("CREDIT_CARD", paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
            resultData.put("CURRENCY", currency.getAlpha3Code());
            resultData.put("INVOICE", invoiceNo);
            resultData.put("DISPLAYAMOUNT", OneCheckoutVerifyFormatData.moneyFormat.format(paymentRequest.getAMOUNT()));
            resultData.put("DISPLAYCHANNEL", trxHelper.getPaymentChannel().value());

            if (errormsg == OneCheckoutErrorMessage.DUPLICATE_PNR) {

                Transactions duplicatePNRTrans = trans;
                redirect.setUrlAction("");
                redirect.setPageTemplate("transactionSuccess.html");
                resultData.put("MSG", "Approved");
                resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with " + m.getMerchantName());
                resultData.put("APPROVAL", duplicatePNRTrans.getDokuApprovalCode());
            } else {
                redirect.setUrlAction(m.getMerchantRedirectUrl());
                resultData.put("MSG", "Failed");
                resultData.put("MSGDETAIL", "Please try again or contact your merchant");

                redirect.setPageTemplate("transactionFailed.html");
            }
            //        redirect.setCookie(paymentRequest.getCookie());
            trxHelper.setMessage("FAILED");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            return trxHelper;

        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }

    }

    protected Transactions notifyOnRedirect(Transactions trans, HashMap<String, String> params, OneCheckoutPaymentChannel pChannel) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutChannelBase.notifyOnRedirect - T0: %d Start", (System.currentTimeMillis() - t1));
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();

            boolean needNotify = false;
            if (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value()) {

                String mustSuccess = config.getString("ONECHECKOUT.NOTIFYALWAYSOK.MALLID", "").trim();

                String merchantCode = "_" + m.getMerchantCode();

                if (!mustSuccess.isEmpty() && mustSuccess.toUpperCase().indexOf(merchantCode) >= 0) {
                    needNotify = true;
                }
            }

            if (needNotify) {

                int conTimeout = config.getInt("ONECHECKOUT.NOTIFY.CONNECTIONTIMEOUT.SECOND", m.getMerchantConnectionTimeout());
                int readTimeout = config.getInt("ONECHECKOUT.NOTIFY.READTIMEOUT.SECOND", m.getMerchantReadTimeout());

                trans.setReconcileDateTime(trans.getDokuInvokeStatusDatetime());
                trans.setDokuInvokeStatusDatetime(new Date());
                String url = m.getMerchantNotifyStatusUrl();
                OneCheckoutLogger.log("OneCheckoutChannelBase.notifyOnRedirect - Start notify", (System.currentTimeMillis() - t1));
                long t2 = System.currentTimeMillis();

                OneCheckoutNotifyStatusResponse ack = sendToMerchant(m.getMerchantNotifyStatusUrl(), params, conTimeout, readTimeout, m.getMerchantNotifyTimeout());

                OneCheckoutLogger.log("OneCheckoutChannelBase.notifyOnRedirect - T1: notify takes time %d", (System.currentTimeMillis() - t2));

                OneCheckoutLogger.log("OneCheckoutChannelBase.notifyOnRedirect : statusNotify : %s", ack.getACKNOWLEDGE());

                String res = ack.getACKNOWLEDGE();
                if (res != null && res.toUpperCase().indexOf("CONTINUE") >= 0) {
                    trans.setSystemMessage("NOTIFYINREDIRECTSUCCESS");
                } else {
                    trans.setSystemMessage("NOTIFYINREDIRECTFAILED");
                    doEmail(trans, config);
                }

                OneCheckoutLogger.log("OneCheckoutChannelBase.notifyOnRedirect : update trans.setSystemMessage= %s", trans.getSystemMessage());
         //       em.merge(trans);

                //                    staticResponse = true;
            }

            return trans;
        } catch (Exception ex) {
            ex.printStackTrace();

            return trans;
        }
    }

    protected <T extends Transactions> String notifyStatusMerchant(T trans, HashMap<String, String> params, OneCheckoutPaymentChannel pChannel, boolean reversal, OneCheckoutStepNotify step) {
        try {
            OneCheckoutV1QueryHelperBeanLocal helperLocal = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBean.class);
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant - T0: %d Start", (System.currentTimeMillis() - t1));
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());

            String mustSuccess = config.getString("ONECHECKOUT.NOTIFYALWAYSOK.MALLID", "").trim();
            OneCheckoutNotifyStatusResponse ack = null;
            String merchantCode = "_" + m.getMerchantCode();
            boolean staticResponse = false;

            String resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE;
            if (step == OneCheckoutStepNotify.INVOKE_STATUS) {
                if (!mustSuccess.isEmpty() && mustSuccess.toUpperCase().indexOf(merchantCode) >= 0) {
                    OneCheckoutLogger.log("MerchantCode %s in Config %s", merchantCode, mustSuccess);
                    OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant : statusNotify : NO NEED NOTIFY TO MERCHANT, ALWAYS CONTINUE");
                    staticResponse = true;
                } else {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant - T2: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
                    OneCheckoutLogger.log("MerchantCode %s not in Config %s", merchantCode, mustSuccess);

                    // OneCheckoutLogger.log("OneCheckoutChannelBase.notifyMerchant : Post Params" + params.toString());            
                    ack = sendToMerchant(m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());//,trans.getRates(),trans.getIncPurchaseamount().doubleValue(),trans.getIncPurchasecurrency());
                    OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant : statusNotify : %s", ack.getACKNOWLEDGE());
                    OneCheckoutLogger.log(": : : : : ACK STATUS " + ack.getSTATUS());
                    OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant - T3: %d finish Notify to Merchant", (System.currentTimeMillis() - t1));
                }

                if (!staticResponse && status == OneCheckoutTransactionStatus.SUCCESS) {
                    if (!ack.getSTATUS() && reversal) {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                        trans.setDokuVoidApprovalCode(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                        trans.setDokuVoidDatetime(new Date());
                        trans.setDokuVoidResponseCode(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                        resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP;
                        OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant : do reversal");
                    }

                    if (ack.isNOTIFYEMAIL()) {
                        trans.setSystemMessage(ack.getMsgFlagEmail());
                    }
                }
            } else if (step == OneCheckoutStepNotify.REVERSAL_PAYMENT) {
                ack = sendToMerchant(m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());
                if (!ack.getSTATUS()) {
                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP;
                    OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant failed notify or response stop or merchant");
                }
            } else if (step == OneCheckoutStepNotify.EDS_UPDATE_STATUS) {

                ack = sendToMerchant(m.getMerchantReviewUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), false);
                OneCheckoutLogger.log("EDS_UPDATE_STATUS response merchant url : " + ack);
                //                if(ack == null){
//                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_TIMEOUT;
//                }else{
                OneCheckoutLogger.log("EDS_UPDATE_STATUS response getACKNOWLEDGE : " + ack.getACKNOWLEDGE());
                if (ack.getACKNOWLEDGE() == null || ack.getACKNOWLEDGE().equals("")) {
                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_TIMEOUT;
                } else if (ack.getACKNOWLEDGE() != null && ack.getACKNOWLEDGE().toUpperCase().indexOf("CONTINUE") >= 0) {
                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE;
                } else if (ack.getACKNOWLEDGE() != null && ack.getACKNOWLEDGE().toUpperCase().indexOf("VOID") >= 0) {
                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_VOID;
                } else {
                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP;
                }
            }
//            if (ack != null && !ack.getACKNOWLEDGE().equals("")) {
//                if (ack.getACKNOWLEDGE().getBytes("UTF8").length > 250) {
//                    int beforeDeduct = ack.getACKNOWLEDGE().getBytes("UTF8").length;
//                    int afterDeduct = beforeDeduct - 250;
//                    OneCheckoutLogger.log("::DEDUCT VALUE :: " + beforeDeduct + " :: " + afterDeduct);
//                    trans.setMerchantNotificationResponse(ack.getACKNOWLEDGE().substring(afterDeduct, beforeDeduct));
//                } else {
//                    trans.setMerchantNotificationResponse(ack.getACKNOWLEDGE());
//                }
//            }
            if (ack != null && !ack.getACKNOWLEDGE().equals("")) {

                Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
                int loopingLuar = 0;
//                for (; n.hasMoreElements();) {
                String ipAddress = null;
                for (; n.hasMoreElements();) {
                    NetworkInterface e = n.nextElement();
                    loopingLuar = loopingLuar + 1;
//                    OneCheckoutLogger.log("looping luar =* " + loopingLuar);

                    Enumeration<InetAddress> a = e.getInetAddresses();
                    int loopingDalam = 0;
                    for (; a.hasMoreElements();) {
                        loopingDalam = loopingDalam + 1;
//                        OneCheckoutLogger.log("looping dalam =* " + loopingDalam);
                        InetAddress addr = a.nextElement();
                        if (loopingLuar == 2) {
//                            System.out.println("resultNetwork =*  " + addr.getHostAddress());
                            ipAddress = addr.getHostAddress() + "";
                        }

                    }
                }

//                String IpAddress = InetAddress.getLocalHost().getHostAddress();
                String HostName = InetAddress.getLocalHost().getHostName();
                SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
                String messageConnection = OneCheckoutProperties.getOneCheckoutConfig().getString("ONECHECKOUTV2.ERROR.CONNECTION." + ack.getHTTP_RESPONSE_CODE(), "ERROR_ON_CALL_URL");
//                OneCheckoutLogger.log("message =* " + messageConnection);
                if (ack.getHTTP_RESPONSE_CODE() == 200) {
                    if (ack.getACKNOWLEDGE().getBytes("UTF8").length > 165) {

                        int beforeDeduct = ack.getACKNOWLEDGE().getBytes("UTF8").length;
                        int afterDeduct = beforeDeduct - 165;
                        OneCheckoutLogger.log("::DEDUCT VALUE :: " + beforeDeduct + " :: " + afterDeduct);

                        trans.setMerchantNotificationResponse(ipAddress + "|||" + HostName + "|||" + dateFormat.format(new Date()) + "|||" + ack.getHTTP_RESPONSE_CODE() + "|||" + ack.getACKNOWLEDGE().substring(afterDeduct, beforeDeduct));

                    } else {
                        trans.setMerchantNotificationResponse(ipAddress + "|||" + HostName + "|||" + dateFormat.format(new Date()) + "|||" + ack.getHTTP_RESPONSE_CODE() + "|||" + ack.getACKNOWLEDGE());
                    }
                } else {
                    trans.setMerchantNotificationResponse(ipAddress + "|||" + HostName + "|||" + dateFormat.format(new Date()) + "|||" + ack.getHTTP_RESPONSE_CODE() + "|||" + messageConnection);
                }

                helperLocal.updateTransaction(trans);
            }

            OneCheckoutLogger.log("OneCheckoutChannelBase.notifyStatusMerchant - T4: %d Finish process", (System.currentTimeMillis() - t1));
            return resp;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected String generateVoidResponse(Transactions trans, String status) {

        StringBuilder sb = new StringBuilder();
        sb.append("<voidResponse>");

        if (trans != null) {
            sb.append("<OrderNumber>").append(trans.getIncTransidmerchant()).append("</OrderNumber>");
        } else {
            sb.append("<OrderNumber>").append("</OrderNumber>");
        }

        sb.append("<status>").append(status).append("</status>");

        if (trans != null) {
            sb.append("<responsecode>").append(trans.getDokuResponseCode()).append("</responsecode>");
            sb.append("<ApprovalCode>").append(trans.getDokuApprovalCode()).append("</ApprovalCode>");
            sb.append("<HostReferenceNumber>").append(trans.getDokuHostRefNum()).append("</HostReferenceNumber>");
            sb.append("<Amount>").append(trans.getIncTransidmerchant()).append("</Amount>");
            sb.append("<AmountMDR></AmountMDR>");
            sb.append("<StatusCode>").append(trans.getIncTransidmerchant()).append("</StatusCode>");

        } else {
            sb.append("<responsecode></responsecode>");
            sb.append("<ApprovalCode></ApprovalCode>");
            sb.append("<HostReferenceNumber></HostReferenceNumber>");
            sb.append("<Amount></Amount>");
            sb.append("<AmountMDR></AmountMDR>");
            sb.append("<StatusCode></StatusCode>");
        }

        sb.append("</voidResponse>");

        return sb.toString();
    }

    private String getEDSDataEmptyACK() {

        StringBuilder sb = new StringBuilder();
        sb.append("<information>");
        sb.append("<personal>");
        sb.append("<full_name></full_name>");
        sb.append("<address></address>");
        sb.append("<zip_code></zip_code>");
        sb.append("<home_phone></home_phone>");
        sb.append("<work_phone></work_phone>");
        sb.append("<mobile_phone></mobile_phone>");
        sb.append("<contactable_phone></contactable_phone>");
        sb.append("<email></email>");
        sb.append("<birth_date></birth_date>");
        sb.append("<billing_address></billing_address>");
        sb.append("<name_on_card></name_on_card>");
        sb.append("</personal>");
        sb.append("</information>");

        return sb.toString();
    }

    private String getEDSDataACK(Transactions trans, Merchants m) {

        try {

            TransactionsDataCardholder ch = nl.getTransactionCHBy(trans);

            StringBuilder sb = new StringBuilder();
            sb.append("<information>");
            sb.append("<personal>");
            sb.append("<full_name>").append((trans.getIncName() != null) ? trans.getIncName() : "").append("</full_name>");
            sb.append("<address>").append((ch.getIncAddress() != null) ? ch.getIncAddress() : "").append("</address>");
            sb.append("<zip_code>").append((ch.getIncZipcode() != null) ? ch.getIncZipcode() : "").append("</zip_code>");
            sb.append("<home_phone>").append((ch.getIncHomephone() != null) ? ch.getIncHomephone() : "").append("</home_phone>");
            sb.append("<work_phone>").append((ch.getIncWorkphone() != null) ? ch.getIncWorkphone() : "").append("</work_phone>");
            sb.append("<mobile_phone>").append((ch.getIncMobilephone() != null) ? ch.getIncMobilephone() : "").append("</mobile_phone>");
            sb.append("<contactable_phone>").append((ch.getIncMobilephone() != null) ? ch.getIncMobilephone() : "").append("</contactable_phone>");
            sb.append("<email>").append((trans.getIncEmail() != null) ? trans.getIncEmail() : "").append("</email>");
            sb.append("<birth_date>").append((ch.getIncBirthdate() != null) ? ch.getIncBirthdate() : "").append("</birth_date>");
            sb.append("<billing_address>").append((ch.getIncAddress() != null) ? ch.getIncAddress() : "").append("</billing_address>");
            sb.append("<name_on_card>").append((ch.getIncCcName() != null) ? ch.getIncCcName() : "").append("</name_on_card>");
            sb.append("</personal>");

            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {

                TransactionsDataAirlines airlines = nl.getTransactionAirlinesBy(trans);

                sb.append("<detail>");
                sb.append("<booking_code>").append(airlines.getIncBookingcode()).append("</booking_code>");
                String route = airlines.getIncRoute();

                //        String flightdatetime = trans.getAirlinesFlightdate() + "" + trans.getAirlinesFlighttime();
                //        Date dflight = OneCheckoutVerifyFormatData.datetimeFormat.parse(flightdatetime);
                String fdate = "";
                String ftime = "";
                try {
                    OneCheckoutBaseRules baseRules = new OneCheckoutBaseRules();
                    //20141128       | 070700
                    String ddatetime = airlines.getIncFlightdate() + airlines.getIncFlighttime();

                    Date t = baseRules.validateDATETIME(ddatetime, "Flight Date Time");
                    SimpleDateFormat flightDateFormat = new SimpleDateFormat("dd-MM-yyyy");
                    SimpleDateFormat flightTimeFormat = new SimpleDateFormat("HH:mm:ss");

                    fdate = flightDateFormat.format(t);
                    ftime = flightTimeFormat.format(t);

                } catch (Exception ex) {
                    fdate = airlines.getIncFlightdate();
                    ftime = airlines.getIncFlighttime();
                    OneCheckoutLogger.log("failed to create valid format, continue with values , Flight Date : " + fdate + " , " + "Flight Time : " + ftime);
                }

                sb.append("<route>").append(route).append("</route>");
                sb.append("<flight_date>").append(fdate).append("</flight_date>");
                sb.append("<flight_time>").append(ftime).append("</flight_time>");
                sb.append("<flight_number>").append(airlines.getIncFlightnumber()).append("</flight_number>");
                sb.append("<ticket_number>").append("").append("</ticket_number>");

                String pnames = "";
                int plen = 0;
                if (airlines.getIncPassengerName() != null) {
                    pnames = airlines.getIncPassengerName().replace('|', ';');
                    String[] p = airlines.getIncPassengerName().split("\\|");
                    plen = p.length;
                }
                sb.append("<passenger_name>").append(pnames).append("</passenger_name>");
                sb.append("<passenger_count>").append(plen).append("</passenger_count>");
                sb.append("</detail>");

            }

            sb.append("</information>");

            return sb.toString();

        } catch (Throwable th) {
            th.printStackTrace();

            return getEDSDataEmptyACK();
        }
    }

    protected OneCheckoutDataHelper doGetEDSDataBase(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutChannelBase.doGetEDSDataBase - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutEDSGetData getData = trxHelper.getEdsGetData();

            String invoiceNo = getData.getTRANSIDMERCHANT();
            double amount = getData.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutChannelBase.doGetEDSDataBase - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            //Transactions trans = queryHelper.getEDSDataTransactionBy(invoiceNo, amount, acq, getData.getWORDS());
            Transactions trans = nl.getEDSDataTransactionBy(invoiceNo, amount, getData.getWORDS());
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutChannelBase.doGetEDSDataBase : Transaction is null");

                String ack = this.getEDSDataEmptyACK();

                getData.setACKNOWLEDGE(ack);

                getData.setNULLTRANSACTION(Boolean.TRUE);

                trxHelper.setEdsGetData(getData);
                return trxHelper;

            }

            getData.setNULLTRANSACTION(Boolean.FALSE);

            String ack = this.getEDSDataACK(trans, trans.getMerchantPaymentChannel().getMerchants());
            OneCheckoutLogger.log("OneCheckoutChannelBase.doGetEDSDataBase : " + ack);

            getData.setACKNOWLEDGE(ack);
            trxHelper.setEdsGetData(getData);

            OneCheckoutLogger.log("OneCheckoutChannelBase.doGetEDSDataBase - T2: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            String ack = this.getEDSDataEmptyACK();
            trxHelper.getEdsGetData().setACKNOWLEDGE(ack);

            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    protected OneCheckoutDataHelper doUpdateEDSStatusBase(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase - T0: %d Start process", (System.currentTimeMillis() - t1));
            OneCheckoutEDSUpdateStatusData updateStatus = trxHelper.getEdsUpdateStatus();
            String invoiceNo = updateStatus.getTRANSIDMERCHANT();
            OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = nl.getEDSUpdateTransactionBy(invoiceNo, updateStatus.getWORDS(), updateStatus.getSTATUS());
            if (trans == null) {
                OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase : Transaction is null");
                updateStatus.setACKNOWLEDGE("STOP");
                trxHelper.setEdsUpdateStatus(updateStatus);
                return trxHelper;
            } else {
                if (trans.getEduStatus() != null && trans.getEduStatus() != OneCheckoutDFSStatus.REVIEW.value()) {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase : Transaction is already have status");
                    updateStatus.setACKNOWLEDGE("CONTINUE");
                    trxHelper.setEdsUpdateStatus(updateStatus);
                    return trxHelper;
                }
            }

            if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                updateStatus.setACKNOWLEDGE("STOP");
                trxHelper.setEdsUpdateStatus(updateStatus);
                return trxHelper;
            }

            Merchants m = trans.getMerchantPaymentChannel().getMerchants();
            OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase edu status:%s ", updateStatus.getSTATUS().name());
            if (updateStatus.getSTATUS() == OneCheckoutDFSStatus.REJECT) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase update transaction status become :%s ", OneCheckoutTransactionStatus.VOIDED.name());
            }

            //       trans.setVerifyDatetime(new Date());
            trans.setVerifyStatus(updateStatus.getSTATUS().value());
            trans.setEduPassVoidDatetime(new Date());
            trans.setEduReason(updateStatus.getREASON());
            trans.setEduStatus(updateStatus.getSTATUS().value());

            OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            HashMap<String, String> params = this.getData(trans);
//            String url = m.getMerchantReviewUrl();//getMerchantNotifyStatusUrl();
//            int readTimeout = m.getMerchantReadTimeout();
//            int conTimeout = m.getMerchantConnectionTimeout();
//            Boolean notifyTimeOut = m.getMerchantNotifyTimeout();

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
            boolean ableToReversal = true;
            String resp = this.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.EDS_UPDATE_STATUS);
            if (resp != null && resp.equalsIgnoreCase(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE)) {
                em.merge(trans);

            } else {
                OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase failed notify or response stop or merchant");
                //resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP;
            }

            updateStatus.setACKNOWLEDGE(resp);
            trxHelper.setEdsUpdateStatus(updateStatus);
            OneCheckoutLogger.log("OneCheckoutChannelBase.doUpdateEDSStatusBase - T2: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getEdsUpdateStatus().setACKNOWLEDGE("STOP");
            trxHelper.setMessage(th.getMessage());
            return trxHelper;
        }
    }

    protected Map generatePaymentRequest(Transactions trans) {
        Map data = new HashMap();

        data.put("MALLID", String.valueOf(trans.getIncMallid()));
        if (trans.getIncChainmerchant() != null) {
            data.put("CHAINMERCHANT", String.valueOf(trans.getIncChainmerchant()));
        }

        if (trans.getRates() != null) {

            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
            data.put("CURRENCY", trans.getIncPurchasecurrency());
        } else {

            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            data.put("CURRENCY", trans.getIncCurrency());

        }
        //   data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
        data.put("PURCHASEAMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount()));
        data.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
        data.put("WORDS", trans.getIncWords());
        data.put("REQUESTDATETIME", OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getIncRequestdatetime()));
        //     data.put("CURRENCY", String.valueOf(trans.getIncCurrency()));
        data.put("PURCHASECURRENCY", String.valueOf(trans.getIncPurchasecurrency()));
        //data.put("SESSIONID", "01RETRY"+trans.getIncTransidmerchant()+OneCheckoutVerifyFormatData.datetimeFormat.format(new Date()));
        //data.put("SESSIONID", "01RETRY"+UUID.randomUUID().toString().replaceAll("-", ""));
        data.put("SESSIONID", trans.getIncSessionid());
        data.put("NAME", trans.getIncName());
        data.put("EMAIL", trans.getIncEmail());
        data.put("ADDITIONALDATA", trans.getIncAdditionalInformation());
        //data.put("PAYMENTCHANNEL", "01");
        data.put("INSTALLMENT_ACQUIRER", trans.getIncInstallmentAcquirer());
        data.put("TENOR", trans.getIncTenor());
        data.put("PROMOID", trans.getIncPromoid());

        // MERCHANT CATEGORY
        if (trans.getTransactionsDataCardholders() != null && trans.getTransactionsDataCardholders().size() > 0) {
            TransactionsDataCardholder cardHolder = trans.getTransactionsDataCardholders().iterator().next();

            data.put("ADDRESS", cardHolder.getIncAddress());
            data.put("CITY", cardHolder.getIncCity());
            data.put("STATE", cardHolder.getIncState());
            data.put("COUNTRY", cardHolder.getIncCountry());
            data.put("ZIPCODE", cardHolder.getIncZipcode());
            data.put("HOMEPHONE", cardHolder.getIncHomephone());
            data.put("MOBILEPHONE", cardHolder.getIncMobilephone());
            data.put("WORKPHONE", cardHolder.getIncWorkphone());
            data.put("BIRTHDATE", cardHolder.getIncBirthdate());
        }

        if (trans.getTransactionsDataNonAirlineses() != null && trans.getTransactionsDataNonAirlineses().size() > 0) {
            TransactionsDataNonAirlines nonAirlines = trans.getTransactionsDataNonAirlineses().iterator().next();

            data.put("BASKET", nonAirlines.getIncBasket());
            data.put("SHIPPING_ADDRESS", nonAirlines.getIncShippingAddress());
            data.put("SHIPPING_CITY", nonAirlines.getIncShippingCity());
            data.put("SHIPPING_STATE", nonAirlines.getIncShippingState());
            data.put("SHIPPING_COUNTRY", nonAirlines.getIncShippingCountry());
            data.put("SHIPPING_ZIPCODE", nonAirlines.getIncShippingZipcode());
        }

        if (trans.getTransactionsDataAirlineses() != null && trans.getTransactionsDataAirlineses().size() > 0) {
            TransactionsDataAirlines airLines = trans.getTransactionsDataAirlineses().iterator().next();

            data.put("FLIGHT", airLines.getIncFlight());
            data.put("FLIGHTTYPE", airLines.getIncFlighttype() + "");
            data.put("BOOKINGCODE", airLines.getIncBookingcode());

            data = generateArray("ROUTE", airLines.getIncRoute().split("\\|"), data);
            data = generateArray("FLIGHTDATE", airLines.getIncFlightdate().split("\\|"), data);
            data = generateArray("FLIGHTTIME", airLines.getIncFlighttime().split("\\|"), data);
            data = generateArray("FLIGHTNUMBER", airLines.getIncFlightnumber().split("\\|"), data);
            data = generateArray("PASSENGER_NAME", airLines.getIncPassengerName().split("\\|"), data);
            data = generateArray("PASSENGER_TYPE", airLines.getIncPassengerType().split("\\|"), data);

            data.put("VAT", airLines.getIncVat() != null ? OneCheckoutVerifyFormatData.sdf.format(airLines.getIncVat()) : "0.00");
            data.put("INSURANCE", airLines.getIncInsurance() != null ? OneCheckoutVerifyFormatData.sdf.format(airLines.getIncInsurance()) : "0.00");
            data.put("FUELSURCHARGE", airLines.getIncFuelsurcharge() != null ? OneCheckoutVerifyFormatData.sdf.format(airLines.getIncFuelsurcharge()) : "0.00");
            data.put("THIRDPARTY_STATUS", airLines.getIncThirdpartyStatus() + "");
            data.put("FFNUMBER", airLines.getIncFFNumber());
        }

        return data;
    }

    private Map generateArray(String name, String Arr[], Map data) {

        for (int i = 0; i < Arr.length; i++) {
            data.put(name, Arr[i]);
        }

        return data;
    }

    protected OneCheckoutDataHelper doShowResultPage(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {
            Transactions trans = trxHelper.getTransactions();
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();

            redirect.setUrlAction(config.getString("URL.GOBACK"));
            redirect.setPageTemplate("redirect.html");

            String id = String.valueOf(trans.getTransactionsId());
            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            data.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
            data.put("NUMBERID", id);

            String checksum = HashWithSHA1.doHashing(id + OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()) + trans.getIncSessionid() + "cintaKU**", "SHA2", null);
            data.put("CHECKSUM", checksum);
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", trans.getIncSessionid());

//          MODIFIED FOR INSTALLMENT
            data.put("INTEREST", trxHelper.getInterestResponse());
            data.put("INSTALLTENOR", trxHelper.getTenorResponse());
            data.put("PLANID", trxHelper.getPlanIdResponse());

            if (trxHelper.getPaymentRequest().getREDEMPTIONSTATUS().equals("TRUE")) {
                data.put("REDEMPTIONSTATUS", "TRUE");
                OneCheckoutRewards rewards = trxHelper.getPaymentPageObject().getOneCheckoutRewards();
                if (rewards != null) {
                    data.put("POINTREDEEMED", "" + rewards.getPointRedeemed());
                    data.put("AMOUNTREDEEMED", "" + OneCheckoutVerifyFormatData.moneyFormat.format(rewards.getAmountRedeemed()));
                }
            }

            OneCheckoutLogger.log("============== START REDIRECT PARAMETER to Show PAYMENT RESULT  ==============");
            OneCheckoutLogger.log("MERCHANT URL    [" + redirect.getUrlAction() + "]");
            OneCheckoutLogger.log("AMOUNT          [" + OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()) + "]");
            OneCheckoutLogger.log("TRANSIDMERCHANT [" + trans.getIncTransidmerchant() + "]");
            OneCheckoutLogger.log("CHECKSUM        [" + checksum + "]");
            OneCheckoutLogger.log("PAYMENTCHANNEL  [" + trxHelper.getPaymentChannel().value() + "]");
            OneCheckoutLogger.log("SESSIONID       [" + trans.getIncSessionid() + "]");
//          MODIFIED FOR INSTALLMENT            
            OneCheckoutLogger.log("INTEREST        [" + trxHelper.getInterestResponse() + "]");
            OneCheckoutLogger.log("TENOR           [" + trxHelper.getTenorResponse() + "]");
            OneCheckoutLogger.log("PLANID          [" + trxHelper.getPlanIdResponse() + "]");

            OneCheckoutLogger.log("============== END  REDIRECT  PARAMETER ==============");
            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);
        } catch (Exception ex) {
        }
        return trxHelper;
    }

    public boolean getDOKUWalletMIPStatus(OneCheckoutDataHelper oneCheckoutDataHelper) {
        boolean result = false;
        try {
            OneCheckoutLogger.log("CHECKING DOKUWALLET MIP STATUS...");
            String channelList = config.getString("ONECHECKOUT.PROC.LIST.CHANNEL", "").trim();
            if (channelList.toUpperCase().indexOf(OneCheckoutPaymentChannel.DOKUPAY.value()) >= 0) {

                String mipStatus = config.getString("ONECHECKOUT.PROC.DOKUWALLET.MIP.STATUS", "").trim();
                if (mipStatus != null && mipStatus.trim().equals("ON")) {
                    OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
                    MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckoutDataHelper.getMerchant(), oneCheckoutDataHelper.getPaymentChannel());
                    if (mpc.getMerchantPaymentChannelMethodCipmip() == 'M') {
                        result = true;
                    }
                } else {
                    String mallIdList = config.getString("ONECHECKOUT.PROC.LIST.MALLID", "").trim();
                    String mallID = "_" + oneCheckoutDataHelper.getMerchant().getMerchantCode();
                    if (mallIdList.equalsIgnoreCase("ALL") || mallIdList.toUpperCase().indexOf(mallID) >= 0) {
                        result = true;
                    }
                    String exceptmallIdList = config.getString("ONECHECKOUT.PROC.LIST.MALLID.EXCEPT", "").trim();
                    if (exceptmallIdList.toUpperCase().indexOf(mallID) >= 0) {
                        result = false;
                    }
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return result;
    }

    public String getCorrectProcessor(int MALLID, OneCheckoutPaymentChannel channel, PaymentChannel acq) {
        try {
            String channelList = config.getString("ONECHECKOUT.PROC.LIST.CHANNEL", "").trim();
            String processor = acq.getPaymentChannelProcessor();
            if (channelList.toUpperCase().indexOf(channel.value()) >= 0) {
                String mallIdList = config.getString("ONECHECKOUT.PROC.LIST.MALLID", "").trim();
                String mallID = "_" + MALLID;
                String processor_ori = processor;
                if (mallIdList.equalsIgnoreCase("ALL")) {
                    processor = processor.replaceAll("Bean/local", "DIRECTCOREBean/local");
                } else if (mallIdList.toUpperCase().indexOf(mallID) >= 0) {
                    processor = processor.replaceAll("Bean/local", "DIRECTCOREBean/local");
                }

                String exceptmallIdList = config.getString("ONECHECKOUT.PROC.LIST.MALLID.EXCEPT", "").trim();
                if (exceptmallIdList.toUpperCase().indexOf(mallID) >= 0) {
                    processor = processor_ori;
                }
            }
            OneCheckoutLogger.log(": : : USING PROCESSOR [%s]", processor);
            return processor;
        } catch (Exception ex) {
            ex.printStackTrace();
            return acq.getPaymentChannelProcessor();
        }
    }

    public String getCorrectProcessor(OneCheckoutDataHelper oneCheckoutDataHelper, PaymentChannel paymentChannel) {
        String processor = paymentChannel.getPaymentChannelProcessor();
        try {
            if (oneCheckoutDataHelper.getPaymentChannel() != null && oneCheckoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {
                if (getDOKUWalletMIPStatus(oneCheckoutDataHelper)) {
                    processor = processor.replaceAll("Bean/local", "DIRECTCOREBean/local");
                }
            } else {
                String channelList = config.getString("ONECHECKOUT.PROC.LIST.CHANNEL", "").trim();
                if (channelList.toUpperCase().indexOf(oneCheckoutDataHelper.getPaymentChannel().value()) >= 0) {
                    String mallIdList = config.getString("ONECHECKOUT.PROC.LIST.MALLID", "").trim();
                    String mallID = "_" + oneCheckoutDataHelper.getMallId();
                    if (oneCheckoutDataHelper.getMerchant() != null) {
                        mallID = "_" + oneCheckoutDataHelper.getMerchant().getMerchantCode();
                    }
                    String processor_ori = processor;
                    if (mallIdList.equalsIgnoreCase("ALL")) {
                        processor = processor.replaceAll("Bean/local", "DIRECTCOREBean/local");
                    } else if (mallIdList.toUpperCase().indexOf(mallID) >= 0) {
                        processor = processor.replaceAll("Bean/local", "DIRECTCOREBean/local");
                    }

                    String exceptmallIdList = config.getString("ONECHECKOUT.PROC.LIST.MALLID.EXCEPT", "").trim();
                    if (exceptmallIdList.toUpperCase().indexOf(mallID) >= 0) {
                        processor = processor_ori;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        OneCheckoutLogger.log(": : : USING PROCESSOR [%s]", processor);
        return processor;
    }
    
    protected OneCheckoutPaymentRequest parseXMLInquiry(MerchantPaymentChannel mpc, XMLConfiguration xml) {

        try {

            /*
             <?xml version="1.0"?>
             <INQUIRY_RESPONSE>
             <PAYMENTCODE></PAYMENTCODE>
             <AMOUNT><AMOUNT>
             <PURCHASEAMOUNT></PURCHASEAMOUNT>
             <TRANSIDMERCHANT></TRANSIDMERCHANT>
             <WORDS></WORDS>     AN     …200     Hashed key combination encryption (use SHA1 method). The hashed key generated from combining these parameters value in this order : AMOUNT+MALLID+ <shared key> + TRANSIDMERCHANT     X
             <REQUESTDATETIME></REQUESTDATETIME>     N     x     YYYYMMDDHHMMSS     X
             <CURRENCY></CURRENCY>     N     3     ISO3166 , numeric code     X
             <PURCHASECURRENCY></PURCHASECURRENCY>     N     3     ISO3166 , numeric code     X
             <SESSIONID>></SESSIONID>     AN     ...48           X
             <NAME></NAME>     AN     …50     Travel Arranger Name / Buyer name     X
             <EMAIL></EMAIL>     ANS     …100     Customer email     X
             <BASKET></BASKET>
             <BOOKINGCODE></BOOKINGCODE>
             <ROUTE></ROUTE>
             <ADDITIONALDATA></ADDITIONALDATA>
             </INQUIRY_RESPONSE>
             */
            OneCheckoutPaymentRequest paymentRequest = new OneCheckoutPaymentRequest();

            String responseCode = xml.getString("RESPONSECODE");
//            OneCheckoutLogger.log("RESPONSECODE DIRECT =* " + responseCode);
            String amount = xml.getString("AMOUNT");
            String purchaseAmount = xml.getString("PURCHASEAMOUNT");
            String transIdMerchant = xml.getString("TRANSIDMERCHANT");
            //  String paymentCode = xml.getString("PAYMENTCODE");
            String words = xml.getString("WORDS");
            String requestDateTime = xml.getString("REQUESTDATETIME");
            String currency = xml.getString("CURRENCY");
            String purchaseCurrency = xml.getString("PURCHASECURRENCY");
            String sessionID = xml.getString("SESSIONID");
            String name = xml.getString("NAME");
            String email = xml.getString("EMAIL");
            //   String basket = xml.getString("BASKET");
            String bookingCode = xml.getString("BOOKINGCODE");
            String route = xml.getString("ROUTE");
            String additionalData = xml.getString("ADDITIONALDATA");
            String mobilePhone = xml.getString("MOBILE") != null ? xml.getString("MOBILE") : null;
//            OneCheckoutLogger.log("MOBILE PHONE IN PARSE XML =* " + mobilePhone);
            String address = xml.getString("ADDRESS") != null ? xml.getString("ADDRESS") : null;
//            OneCheckoutLogger.log("ADDRESS IN PARSE XML =* " + address);

            String[] result = xml.getStringArray("BASKET");
            String basket = StringUtils.join(result, ",");

            //  basket = "ITEM 1," + amount + ",1," + amount;
            OneCheckoutLogger.log(": : : BASKET [%s]", basket);

            String[] routes = new String[1];
            OneCheckoutLogger.log("MerchantCode=%d, chainMerchantCode=%d", mpc.getMerchants().getMerchantCode(), mpc.getMerchants().getMerchantChainMerchantCode());
            routes[0] = route;
            if (responseCode != null && responseCode.equalsIgnoreCase(OneCheckoutVAMerchantResponseCode.SUCCESS.code())) {
                paymentRequest.setADDITIONALINFO("1");
            } else if (responseCode != null && responseCode.equalsIgnoreCase(OneCheckoutVAMerchantResponseCode.FAILED.code())) {
                paymentRequest.setADDITIONALINFO("0");
                return paymentRequest;
            } else if (responseCode != null && responseCode.equalsIgnoreCase(OneCheckoutVAMerchantResponseCode.TRANSACTION_HAS_BEEN_PAID.code())) {
//                OneCheckoutLogger.log("responseCode =* 2  = " + responseCode);
                paymentRequest.setADDITIONALINFO("2");
                return paymentRequest;
            } else if (responseCode != null && responseCode.equalsIgnoreCase(OneCheckoutVAMerchantResponseCode.INVALID_ACCOUNT_NUMBER.code())) {
                paymentRequest.setADDITIONALINFO("3");
                return paymentRequest;
            } else if (responseCode != null && responseCode.equalsIgnoreCase(OneCheckoutVAMerchantResponseCode.INVALID_AMOUNT.code())) {
                paymentRequest.setADDITIONALINFO("4");
                return paymentRequest;
            } else if (responseCode != null && responseCode.equalsIgnoreCase(OneCheckoutVAMerchantResponseCode.ACCOUNT_NUMBER_WAS_EXPIRED.code())) {
                paymentRequest.setADDITIONALINFO("5");
                return paymentRequest;
            }

            paymentRequest.setMALLID(mpc.getMerchants().getMerchantCode());
            paymentRequest.setCHAINMERCHANT(mpc.getMerchants().getMerchantChainMerchantCode());
            paymentRequest.setAMOUNT(amount);
            if (mpc.getPaymentChannelChainCode() != null) {
                paymentRequest.setCHAINMERCHANT(mpc.getPaymentChannelChainCode());
            } else {
                paymentRequest.setCHAINMERCHANT(0);
            }
            paymentRequest.setADDRESS("dummy");
            paymentRequest.setADDITIONALINFO(additionalData);
            paymentRequest.setBASKET(basket, paymentRequest.getAMOUNT(), transIdMerchant);

            Merchants m = mpc.getMerchants();
            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {
                paymentRequest.setBOOKINGCODE(bookingCode);
                paymentRequest.setROUTE(routes);
            }
            paymentRequest.setCITY("dummy");
            paymentRequest.setCOUNTRY("360");
            paymentRequest.setCURRENCY(currency);
            paymentRequest.setEMAIL(email);
            paymentRequest.setINVOICENUMBER(transIdMerchant);
            //  paymentRequest.setMALLID(mpc.getPaymentChannelCode());
            paymentRequest.setNAME(name);
            //      paymentRequest.setPAYCODE(paymentCode);
            paymentRequest.setPURCHASEAMOUNT(purchaseAmount);
            paymentRequest.setPURCHASECURRENCY(purchaseCurrency);
            paymentRequest.setREQUESTDATETIME(requestDateTime);
            if (mobilePhone != null) {
                OneCheckoutLogger.log("setMobilePhone");
                paymentRequest.setMOBILEPHONE(mobilePhone);
            }
            if (address != null) {
                paymentRequest.setADDRESS(address);
            }

            paymentRequest.setSESSIONID(sessionID);
            paymentRequest.setTRANSIDMERCHANT(transIdMerchant);
            paymentRequest.setVAT("0.00");
            paymentRequest.setWORDS(words);
            paymentRequest.setWORKPHONE("");
            paymentRequest.setZIPCODE("121212");

            return paymentRequest;
        } catch (Exception ex) {

            ex.printStackTrace();
            return null;
        }

    }

    public String generatePaymentResponseMIPWords(Transactions trans, Merchants m) {

        try {

            StringBuilder word = new StringBuilder();

            String amount = OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue());
            word.append(amount);
            word.append(m.getMerchantCode());
            word.append(m.getMerchantHashPassword());
            word.append(trans.getIncTransidmerchant());
            word.append(trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED");
            word.append(OneCheckoutDFSStatus.findType(trans.getVerifyStatus()).name());

            OneCheckoutLogger.log("OneCheckoutChannelBase.generatePaymentResponseMIPWords ONECHECKOUT ELEMENT [" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), m.getMerchantShaFormat(), m.getMerchantHashPassword());

//            if (m.getMerchantShaFormat()!=null && m.getMerchantShaFormat().equalsIgnoreCase("SHA2")) {
//                OneCheckoutLogger.log("OneCheckoutChannelBase.generatePaymentResponseMIPWords ONECHECKOUT SHA Format [SHA2]");
//                hashwords = HashWithSHA1.SHA2(word.toString());
//            }
//            else  {
//                OneCheckoutLogger.log("OneCheckoutChannelBase.generatePaymentResponseMIPWords ONECHECKOUT SHA Format [SHA1]");
//                hashwords = HashWithSHA1.SHA1(word.toString());
//            }
            return hashwords;
        } catch (Exception iv) {
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }

    protected OneCheckoutDataHelper generatePaymentResponseMIPXML(OneCheckoutDataHelper oco, Merchants merchant, PaymentChannel pChannel) {

        try {

            Transactions trans = oco.getTransactions();
            String amount = OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue());
            String verifyId = trans.getVerifyId();
            String verifyStatus = OneCheckoutDFSStatus.findType(trans.getVerifyStatus()).name();
            String verifyScore = trans.getVerifyScore() + "";
            String words = generatePaymentResponseMIPWords(oco.getTransactions(), merchant);
            String paymentDate = "";
            if (trans.getDokuInvokeStatusDatetime() != null) {
                paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            }
            //HashWithSHA1.SHA2(amount+paymentRequest.getMALLID() + trxHelper.getMerchant().getMerchantHashPassword() + trans.getIncTransidmerchant() + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + verifyStatus);
            String xmlMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<PAYMENT_STATUS>"
                    + "<AMOUNT>" + amount + "</AMOUNT>"
                    + "<TRANSIDMERCHANT>" + trans.getIncTransidmerchant() + "</TRANSIDMERCHANT>"
                    + "<WORDS>" + words + "</WORDS>"
                    + "<RESPONSECODE>" + trans.getDokuResponseCode() + "</RESPONSECODE>"
                    + "<APPROVALCODE>" + trans.getDokuApprovalCode() + "</APPROVALCODE>"
                    + "<RESULTMSG>" + (trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() ? "SUCCESS" : "FAILED") + "</RESULTMSG>"
                    + "<PAYMENTCHANNEL>" + pChannel.getPaymentChannelId() + "</PAYMENTCHANNEL>"
                    + "<PAYMENTCODE></PAYMENTCODE>"
                    + "<SESSIONID>" + trans.getIncSessionid() + "</SESSIONID>"
                    + "<BANK>" + trans.getDokuIssuerBank() + "</BANK>"
                    + "<MCN>" + trans.getAccountId() + "</MCN>"
                    + "<PAYMENTDATETIME>" + paymentDate + "</PAYMENTDATETIME>"
                    + "<VERIFYID>" + verifyId + "</VERIFYID>"
                    + "<VERIFYSCORE>" + verifyScore + "</VERIFYSCORE>"
                    + "<VERIFYSTATUS>" + verifyStatus + "</VERIFYSTATUS>"
                    + "</PAYMENT_STATUS>";
            oco.setMessage(xmlMessage);

        } catch (Exception ex) {

            ex.printStackTrace();

        }

        return oco;
    }

    protected Transactions CheckStatusCOREIPG(Transactions trans, PaymentChannel paymentChannel) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            //PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            StringBuilder sb = new StringBuilder();
            sb.append("MALLID=").append(merchantPaymentChannel.getPaymentChannelCode() + "");
            if (merchantPaymentChannel.getPaymentChannelChainCode() != null && merchantPaymentChannel.getPaymentChannelChainCode() > 0) {
                sb.append("&").append("CHAINMALLID=").append(merchantPaymentChannel.getPaymentChannelChainCode() + "");
            }
            sb.append("&").append("TRANSIDMERCHANT=").append(trans.getIncTransidmerchant());
            sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            sb.append("&").append("WORDS=").append("WORDS");

            OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("CHECK PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            String words = "";
            String paymentStatus = "";
            String paymentResponseCode = "";
            String paymentApprovalCode = "";
            String paymentHostRefNum = "";
            String cancelResponseCode = "";
            String cancelApprovalCode = "";
            String issuerBank = "";
            String verifyId = "";
            String verifyStatus = "";
            String verifyScore = "";
            String verifyReason = "";
            String acquirerBank = "";
            String maskedCard = "";
            String paymentDateTime = "";
            String trxCode = "";
            String errorCode = "";
            if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                /*
                 <TRANSACTION>
                 <MALLID>800</MALLID>
                 <AMOUNT>137999.00</AMOUNT>
                 <TRANSIDMERCHANT>01140000000030</TRANSIDMERCHANT>
                 <STATUS>SUCCESS</STATUS>
                 <RESPONSECODE>00</RESPONSECODE>
                 <APPROVALCODE>812762</APPROVALCODE>
                 <DATETIME>20140128150208</DATETIME>
                 </TRANSACTION>
                 */
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                xml.load(sr);
                paymentStatus = xml.getString("STATUS") != null ? xml.getString("STATUS").trim() : "";
                if (paymentStatus != null && !paymentStatus.trim().equals("")) {
                    paymentResponseCode = xml.getString("RESPONSECODE") != null ? xml.getString("RESPONSECODE").trim() : "";
                    if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                        paymentApprovalCode = xml.getString("APPROVALCODE") != null ? xml.getString("APPROVALCODE").trim() : "";
                        paymentHostRefNum = xml.getString("HOSTREFNUMBER") != null ? xml.getString("HOSTREFNUMBER").trim() : "";
                        maskedCard = xml.getString("CARDNUMBER") != null ? xml.getString("CARDNUMBER").trim() : "";
                        verifyId = xml.getString("FRAUDSCREENINGID") != null ? xml.getString("FRAUDSCREENINGID").trim() : "";
                        verifyStatus = xml.getString("FRAUDSCREENINGRESULT") != null ? xml.getString("FRAUDSCREENINGRESULT").trim() : "NA";
                        verifyScore = xml.getString("FRAUDSCREENINGSCORE") != null ? xml.getString("FRAUDSCREENINGSCORE").trim() : "-1";
                        verifyReason = xml.getString("FRAUDSCREENINGREASON") != null ? xml.getString("FRAUDSCREENINGREASON").trim() : "";
                        paymentDateTime = xml.getString("DATETIME") != null ? xml.getString("DATETIME").trim() : "";
                        issuerBank = xml.getString("ISSUERBANK") != null ? xml.getString("ISSUERBANK").trim() : "";
                        acquirerBank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
                        cancelResponseCode = xml.getString("CANCELRESPONSECODE") != null ? xml.getString("CANCELRESPONSECODE").trim() : "";
                        cancelApprovalCode = xml.getString("CANCELAPPROVALCODE") != null ? xml.getString("CANCELAPPROVALCODE").trim() : "";

                        if (!paymentResponseCode.isEmpty() && paymentResponseCode.length() != 4) {
                            if (errorCode != null) {
                                if (errorCode.trim().equalsIgnoreCase("XX07")) {
                                    paymentResponseCode = "BB";
                                }
                            }
                            paymentResponseCode = "00" + paymentResponseCode;
                        } else if (paymentResponseCode.isEmpty()) {
                            if (paymentStatus.equalsIgnoreCase("TRANSACTION_NOT_FOUND")) {
                                paymentResponseCode = OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND.value();
                            } else {
                                paymentResponseCode = OneCheckoutErrorMessage.UNKNOWN.value();
                            }
                        }
                        trans.setDokuApprovalCode(paymentApprovalCode);
                        trans.setDokuIssuerBank(issuerBank);
                        trans.setDokuResponseCode(paymentResponseCode);
                        trans.setDokuHostRefNum(paymentHostRefNum);
                        try {
                            OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                            if (verifyStatus != null) {
                                notifyRequest.setDFSStatus(verifyStatus);
                                notifyRequest.setDFSScore(verifyScore);
                                notifyRequest.setDFSIId(verifyId);
                                trans.setVerifyId(notifyRequest.getDFSId());
                                trans.setVerifyScore(notifyRequest.getDFSScore());
                                trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                                //       trans.setEduStatus(notifyRequest.getDFSStatus().value());
                            } else {
                                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                                notifyRequest.setDFSScore("-1");
                                notifyRequest.setDFSIId("");
                                trans.setVerifyId("");
                                trans.setVerifyScore(-1);
                                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                                //         trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                            }
                        } catch (Throwable t) {
                            trans.setVerifyScore(0);
                        }

                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                        trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        Date tpayment = new Date();
                        try {
                            tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                        } catch (Exception ex) {
                        }
                        trans.setDokuInvokeStatusDatetime(tpayment);
                        em.merge(trans);
                    } else if (paymentStatus.trim().equalsIgnoreCase("VOIDED")) {
                        /*
                         <TRANSACTION>
                         <MALLID>1173</MALLID>
                         <TRANSIDMERCHANT>150120749</TRANSIDMERCHANT>
                         <AMOUNT>10000.00</AMOUNT>
                         <CARDNUMBER>5***********8754</CARDNUMBER>
                         <STATUS>VOIDED</STATUS>
                         <RESPONSECODE>00</RESPONSECODE>
                         <APPROVALCODE>019377</APPROVALCODE>
                         <HOSTREFNUMBER>476474065912</HOSTREFNUMBER>
                         <DATETIME>20150120170243</DATETIME>
                         <FRAUDSCREENINGID />
                         <FRAUDSCREENINGSCORE />
                         <FRAUDSCREENINGRESULT />
                         <FRAUDSCREENINGREASON />
                         <ISSUERBANK>Citibank</ISSUERBANK>
                         <ACQUIRERBANK>BNI</ACQUIRERBANK>
                         <CANCELRESPONSECODE />
                         <CANCELAPPROVALCODE />
                         </TRANSACTION>
                         */
                        if (!paymentResponseCode.isEmpty() && paymentResponseCode.length() == 2) {
                            paymentResponseCode = "00" + paymentResponseCode;
                            trans.setDokuApprovalCode(paymentApprovalCode);
                            trans.setDokuIssuerBank(issuerBank);
                            trans.setDokuResponseCode(paymentResponseCode);
                            trans.setDokuHostRefNum(paymentHostRefNum);
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            em.merge(trans);
                        } else {
                            paymentResponseCode = OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value();
                            trans.setDokuResponseCode(paymentResponseCode);
                        }
                    } else {
                        if (!paymentResponseCode.isEmpty() && paymentResponseCode.length() == 2) {
                            paymentResponseCode = "00" + paymentResponseCode;
                            trans.setDokuResponseCode(paymentResponseCode);
                            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            em.merge(trans);
                        } else {
                            paymentResponseCode = OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value();
                            trans.setDokuResponseCode(paymentResponseCode);
                        }
                    }
                } else {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                }
                /*
                 if (!paymentResponseCode.isEmpty() && paymentResponseCode.length() != 4) {
                 if (errorCode != null) {
                 if (errorCode.trim().equalsIgnoreCase("XX07")) {
                 paymentResponseCode = "BB";
                 }
                 }
                 paymentResponseCode = "00" + paymentResponseCode;
                 }
                 else if (paymentResponseCode.isEmpty()) {
                 if (paymentStatus.equalsIgnoreCase("TRANSACTION_NOT_FOUND"))
                 paymentResponseCode = OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND.value();
                 else
                 paymentResponseCode = OneCheckoutErrorMessage.UNKNOWN.value();
                 }
                 trans.setDokuApprovalCode(paymentApprovalCode);
                 trans.setDokuIssuerBank(issuerBank);
                 trans.setDokuResponseCode(paymentResponseCode);
                 trans.setDokuHostRefNum(paymentHostRefNum);
                 try {
                 OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                 if (verifyStatus != null) {
                 notifyRequest.setDFSStatus(verifyStatus);
                 notifyRequest.setDFSScore(verifyScore);
                 notifyRequest.setDFSIId(verifyId);
                 trans.setVerifyId(notifyRequest.getDFSId());
                 trans.setVerifyScore(notifyRequest.getDFSScore());
                 trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                 //       trans.setEduStatus(notifyRequest.getDFSStatus().value());
                 } else {
                 notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                 notifyRequest.setDFSScore("-1");
                 notifyRequest.setDFSIId("");
                 trans.setVerifyId("");
                 trans.setVerifyScore(-1);
                 trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                 //         trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                 }
                 } catch (Throwable t) {
                 trans.setVerifyScore(0);
                 }

                 if (paymentStatus != null && !paymentStatus.trim().equals("")) {
                 if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("REVERSED")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("VOIDED")) {
                 trans.setDokuVoidApprovalCode(cancelApprovalCode);
                 trans.setDokuVoidResponseCode(cancelResponseCode);
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("REFUNDED")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.REFUND.value());
                 } else {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                 }
                 trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                 Date tpayment = new Date();                            
                 try {
                 tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                 } catch (Exception ex) {
                 }                          
                 trans.setDokuInvokeStatusDatetime(tpayment);                                                        
                 em.merge(trans);
                 } else {
                 trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                 }
                 */
            } else {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
            }
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
        } catch (Exception ex) {
            ex.printStackTrace();

        }
        return trans;
    }

    protected Transactions reversalToCOREIPG(Transactions trans) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            String currency = trans.getIncCurrency();
            StringBuilder sb = new StringBuilder();
            sb.append("MALLID=").append(merchantPaymentChannel.getPaymentChannelCode() + "");
            if (merchantPaymentChannel.getPaymentChannelChainCode() != null && merchantPaymentChannel.getPaymentChannelChainCode() > 0) {
                sb.append("&").append("CHAINMALLID=").append(merchantPaymentChannel.getPaymentChannelChainCode() + "");
            }
            sb.append("&").append("INVOICENUMBER=").append(trans.getIncTransidmerchant());
            sb.append("&").append("CURRENCY=").append(currency);
            sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            sb.append("&").append("WORDS=").append("WORDS");
            OneCheckoutLogger.log("REVERSAL PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getReversalPaymentUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("REVERSAL PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            /*
             String words = "";
             String reversalStatus = "";
             if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
             XMLConfiguration xml = new XMLConfiguration();
             StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
             xml.load(sr);
             reversalStatus = xml.getString("status") != null ? xml.getString("status").trim() : "";
             if (reversalStatus != null && reversalStatus.trim().equalsIgnoreCase("SUCCESS")) {
             trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
             em.merge(trans);
             } else {
             trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_REVERSAL_TO_CORE.value());
             }
             } else {
             trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_REVERSAL_TO_CORE.value());
             }
             */
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return trans;
    }
//  PREVIOUS VERSION

    protected Transactions reversalToCOREDOKUWallet(OneCheckoutPaymentRequest oneCheckoutPaymentRequest, Transactions transactions, MIPInquiryResponse mIPInquiryResponse) {
//  MODIFIED VERSION
//    protected Transactions reversalToCOREDOKUWallet(OneCheckoutPaymentRequest oneCheckoutPaymentRequest, Transactions transactions,MIPDokuInquiryResponse mIPDokuInquiryResponse) {
        try {
            /*
             DPMALLID
             TRANSIDMERCHANT
             SESSIONID
             PAYMENTCHANNELCODE
             WORDS = DPMALLID + SHAREDKEY + TRANSIDMERCHANT + SESSIONID
             CALLBACKDATA
             */

            StringBuilder word = new StringBuilder();
            word.append(transactions.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
            word.append(transactions.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(transactions.getIncTransidmerchant());
            word.append(transactions.getIncSessionid());
            OneCheckoutLogger.log("COMPONENT_WORDS_TO_DOKUWALLET[" + word.toString() + "]");
            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);
            OneCheckoutLogger.log("WORDS_TO_DOKUWALLET[" + hashwords + "]");

            StringBuilder sb = new StringBuilder();
            sb.append("DPMALLID=").append(transactions.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
            sb.append("&").append("TRANSIDMERCHANT=").append(transactions.getIncTransidmerchant());
            sb.append("&").append("SESSIONID=").append(transactions.getIncSessionid());
            sb.append("&").append("PAYMENTCHANNELCODE=").append(oneCheckoutPaymentRequest.getDOKUWALLETCHANNEL());
            sb.append("&").append("WORDS=").append(hashwords);
            OneCheckoutLogger.log("REVERSAL PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), transactions.getMerchantPaymentChannel().getPaymentChannel().getReversalPaymentUrl(), transactions.getMerchantPaymentChannel().getMerchants().getMerchantConnectionTimeout(), transactions.getMerchantPaymentChannel().getMerchants().getMerchantReadTimeout());
            OneCheckoutLogger.log("REVERSAL PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return transactions;
    }

    protected Transactions CheckStatusCOREMPG(Transactions trans, PaymentChannel paymentChannel) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            //PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            StringBuilder sb = new StringBuilder();
            String chainMallid = "";
            sb.append("MALLID=").append(merchantPaymentChannel.getPaymentChannelCode() + "");
            OneCheckoutLogger.log("mpc = " + merchantPaymentChannel.getMerchantPaymentChannelId());
            if (merchantPaymentChannel.getPaymentChannelChainCode() != null && merchantPaymentChannel.getPaymentChannelChainCode() > 0) {
//                sb.append("&").append("CHAINMALLID=").append(merchantPaymentChannel.getPaymentChannelChainCode() + "");

                chainMallid = merchantPaymentChannel.getPaymentChannelChainCode() + "";
            }
            sb.append("&").append("CHAINMALLID=").append(chainMallid);
            sb.append("&").append("SERVICEID=").append(paymentChannel.getServiceId());
            sb.append("&").append("TRXCODE=").append(trans.getDokuVoidApprovalCode());
            sb.append("&").append("INVOICENUMBER=").append(trans.getIncTransidmerchant());
            sb.append("&").append("WORDS=").append("WORDS");

            OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("CHECK PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            String words = "";
            String paymentStatus = "";
            String paymentResponseCode = "";
            String paymentApprovalCode = "";
            String cancelResponseCode = "";
            String cancelApprovalCode = "";
            String issuerBank = "";
            String verifyId = "";
            String verifyStatus = "";
            String verifyScore = "";
            String verifyReason = "";
            String hostRefNum = "";
            String acquirerBank = "";
            String maskedCard = "";
            String paymentDateTime = "";
            String trxCode = "";
            String errorCode = "";
            String batchId = "";
            String batchNumber = "";
            String tid = "";
            String settlementDate = "";
            String acquirerId = "";
            String mid = "";

            if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                xml.load(sr);
                paymentStatus = xml.getString("paymentStatus") != null ? xml.getString("paymentStatus").trim() : "";
                paymentResponseCode = xml.getString("paymentResponseCode") != null ? xml.getString("paymentResponseCode").trim() : "";
                paymentApprovalCode = xml.getString("paymentApprovalCode") != null ? xml.getString("paymentApprovalCode").trim() : "";
                cancelResponseCode = xml.getString("cancelResponseCode") != null ? xml.getString("cancelResponseCode").trim() : "";
                cancelApprovalCode = xml.getString("cancelApprovalCode") != null ? xml.getString("cancelApprovalCode").trim() : "";
                issuerBank = xml.getString("issuerBank") != null ? xml.getString("issuerBank").trim() : "";
                verifyId = xml.getString("fraudScreeningId") != null ? xml.getString("fraudScreeningId").trim() : "";
                verifyStatus = xml.getString("fraudScreeningStatus") != null ? xml.getString("fraudScreeningStatus").trim() : "NA";
                verifyScore = xml.getString("fraudScreeningScore") != null ? xml.getString("fraudScreeningScore").trim() : "-1";
                verifyReason = xml.getString("fraudScreeningReason") != null ? xml.getString("fraudScreeningReason").trim() : "";
                hostRefNum = xml.getString("hostReferenceNumber") != null ? xml.getString("hostReferenceNumber").trim() : "";
                acquirerBank = xml.getString("acquirerBank") != null ? xml.getString("acquirerBank").trim() : "";
                maskedCard = xml.getString("cardNumber") != null ? xml.getString("cardNumber").trim() : "";
                paymentDateTime = xml.getString("paymentDate") != null ? xml.getString("paymentDate").trim() : "";
                trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
                errorCode = xml.getString("errorCode") != null ? xml.getString("errorCode").trim() : "";
                mid = xml.getString("mid") != null ? xml.getString("mid").trim() : "";

                //required batch settlement(start)
                batchId = xml.getString("batchId") != null ? xml.getString("batchId").trim() : "";
                batchNumber = xml.getString("batchNumber") != null ? xml.getString("batchNumber").trim() : "";
                tid = xml.getString("tid") != null ? xml.getString("tid").trim() : "";
                acquirerId = xml.getString("bankId") != null ? xml.getString("bankId").trim() : "";
                settlementDate = xml.getString("settlementDate") != null ? xml.getString("settlementDate").trim() : "";
                //required batch settlement(end)

                if (paymentStatus != null && !paymentStatus.trim().equals("")) {
                    if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                        if (paymentResponseCode != null && paymentResponseCode.length() != 4) {
                            if (errorCode != null) {
                                if (errorCode.trim().equalsIgnoreCase("XX07")) {
                                    paymentResponseCode = "BB";
                                }
                            }
                            paymentResponseCode = "00" + paymentResponseCode;
                        }
                        if (!issuerBank.equals("")) {
                            trans.setDokuIssuerBank(issuerBank);
                        }
                        trans.setDokuApprovalCode(paymentApprovalCode);
                        trans.setDokuResponseCode(paymentResponseCode);
                        trans.setDokuHostRefNum(hostRefNum);
                        trans.setDokuVoidApprovalCode(trxCode);
                        trans.setTrxCodeCore(trxCode);
                        //required settlement and refund(start)

//                        if (!batchId.equals("") && !batchNumber.equals("") && !mid.equals("") && !tid.equals("") && !acquirerId.equals("") && m.getMerchantEscrow() != null && !m.getMerchantEscrow()) {
//                            Criterion c = Restrictions.eq("coreBatchId", batchId);
//                            Criterion c1 = Restrictions.eq("coreBatchNumber", batchNumber);
//                            Criterion c2 = Restrictions.eq("mid", mid);
//                            Criterion c3 = Restrictions.eq("tid", tid);
//                            Criterion c4 = Restrictions.eq("acquirerId", acquirerId);
////                            Criterion c5 = Restrictions.eq("mall_id", trans.getIncMallid());
////                            Criterion c6 = Restrictions.eq("chainMerchant", trans.getIncChainmerchant());
//
//                            Batch batch = nl.getBatchByCriterion(c, c1, c2, c3, c4);
////                            Batch batch = nl.getBatchByCriterion(c, c1, c2, c3, c4,c5,c6);
////                            Batch batch1 = new Batch();
//                            if (batch == null) {
//                                batch = new Batch();
//                                batch.setBatchId(nl.getNextVal(SeqBatchIdNextval.class).longValue());
//                                batch.setCoreBatchId(batchId);
//                                batch.setCoreBatchNumber(batchNumber);
//                                batch.setType(OneCheckOutBatchType.MPG.value());
//                                batch.setTid(tid);
//                                batch.setMid(mid);
//                                batch.setAcquirerId(acquirerId);
//                                batch.setStatus(OneCheckOutSettlementStatus.OPEN.value());
//                                batch.setMallId(trans.getIncMallid());
//                                batch.setChainMerchant(trans.getIncChainmerchant());
//                                if (!settlementDate.equals("")) {
//                                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH:MM:ss.SSS");
//                                    Date d2 = dateFormat.parse(settlementDate);
//                                    batch.setSettlementDate(d2);
//                                }
//                                nl.saveBatch(batch);
//                            }
//                            trans.setBatchId((int) batch.getBatchId());
//                        }
                        //required settlement and refund(end)
                        try {
                            OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                            if (verifyStatus != null) {
                                notifyRequest.setDFSStatus(verifyStatus);
                                notifyRequest.setDFSScore(verifyScore);
                                notifyRequest.setDFSIId(verifyId);
                                trans.setVerifyId(notifyRequest.getDFSId());
                                trans.setVerifyScore(notifyRequest.getDFSScore());
                                trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                                //      trans.setEduStatus(notifyRequest.getDFSStatus().value());
                            } else {
                                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                                notifyRequest.setDFSScore("-1");
                                notifyRequest.setDFSIId("");
                                trans.setVerifyId("");
                                trans.setVerifyScore(-1);
                                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                                //      trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                            }
                        } catch (Throwable t) {
                            trans.setVerifyScore(0);
                        }
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                        trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        Date tpayment = new Date();
                        try {
                            tpayment = OneCheckoutVerifyFormatData.email_datetimeFormat.parse(paymentDateTime);
                        } catch (Exception ex) {
                        }
                        trans.setDokuInvokeStatusDatetime(tpayment);
                        trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        em.merge(trans);
                    } else {
                        if (!paymentResponseCode.isEmpty() && paymentResponseCode.length() == 2) {
                            paymentResponseCode = "00" + paymentResponseCode;
                            trans.setDokuResponseCode(paymentResponseCode);
                            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            em.merge(trans);
                        } else {
                            paymentResponseCode = OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value();
                            trans.setDokuResponseCode(paymentResponseCode);
                        }
                    }
                } else {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                }

                /*
                 if (paymentResponseCode != null && paymentResponseCode.length() != 4) {
                 if (errorCode != null) {
                 if (errorCode.trim().equalsIgnoreCase("XX07")) {
                 paymentResponseCode = "BB";
                 }
                 }
                 paymentResponseCode = "00" + paymentResponseCode;
                 }
                 if (!issuerBank.equals("")) {
                 trans.setDokuIssuerBank(issuerBank);
                 }
                 trans.setDokuApprovalCode(paymentApprovalCode);
                 trans.setDokuResponseCode(paymentResponseCode);
                 trans.setDokuHostRefNum(hostRefNum);
                 trans.setDokuVoidApprovalCode(trxCode);
                 try {
                 OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                 if (verifyStatus != null) {
                 notifyRequest.setDFSStatus(verifyStatus);
                 notifyRequest.setDFSScore(verifyScore);
                 notifyRequest.setDFSIId(verifyId);
                 trans.setVerifyId(notifyRequest.getDFSId());
                 trans.setVerifyScore(notifyRequest.getDFSScore());
                 trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
                 //      trans.setEduStatus(notifyRequest.getDFSStatus().value());
                 } else {
                 notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                 notifyRequest.setDFSScore("-1");
                 notifyRequest.setDFSIId("");
                 trans.setVerifyId("");
                 trans.setVerifyScore(-1);
                 trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                 //      trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                 }
                 } catch (Throwable t) {
                 trans.setVerifyScore(0);
                 }

                 if (paymentStatus != null && !paymentStatus.trim().equals("")) {
                 if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("REVERSAL")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("VOID")) {
                 //trans.setDokuVoidApprovalCode(cancelResponseCode);
                 trans.setDokuVoidResponseCode(cancelResponseCode);
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("REFUND")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.REFUND.value());
                 } else {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                 }
                 trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                 Date tpayment = new Date();                            
                 try {
                 tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                 } catch (Exception ex) {
                 }                             
                 trans.setDokuInvokeStatusDatetime(tpayment);
                 em.merge(trans);
                 } else {
                 trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                 }*/
            } else {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return trans;
    }

    protected Transactions reversalToCOREMPG(Transactions trans) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            String currency = trans.getIncCurrency();
            Currency curr = nl.getCurrencyByCode(currency);
            if (curr != null && curr.getAlpha3Code() != null) {
                currency = curr.getAlpha3Code();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("MALLID=").append(merchantPaymentChannel.getPaymentChannelCode() + "");
            if (merchantPaymentChannel.getPaymentChannelChainCode() != null && merchantPaymentChannel.getPaymentChannelChainCode() > 0) {
                sb.append("&").append("CHAINMALLID=").append(merchantPaymentChannel.getPaymentChannelChainCode() + "");
            }
            sb.append("&").append("SERVICEID=").append(paymentChannel.getServiceId());
            sb.append("&").append("INVOICENUMBER=").append(trans.getIncTransidmerchant());
            sb.append("&").append("CURRENCY=").append(currency);
            sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            sb.append("&").append("WORDS=").append("WORDS");
            OneCheckoutLogger.log("REVERSAL PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getReversalPaymentUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("REVERSAL PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            /*
             String words = "";
             String reversalStatus = "";
             String errorCode = "";
             String message = "";
             if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
             XMLConfiguration xml = new XMLConfiguration();
             StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
             xml.load(sr);
             reversalStatus = xml.getString("result") != null ? xml.getString("result").trim() : "";
             errorCode = xml.getString("errorCode") != null ? xml.getString("errorCode").trim() : "";
             message = xml.getString("message") != null ? xml.getString("message").trim() : "";

             if (reversalStatus != null && reversalStatus.trim().equalsIgnoreCase("SUCCESS")) {
             //trans.setDokuResponseCode("00RV");
             //trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
             trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
             em.merge(trans);
             } else {
             trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_REVERSAL_TO_CORE.value());
             }
             } else {
             trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_REVERSAL_TO_CORE.value());
             }
             */
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return trans;
    }

    protected Transactions CheckStatusCOREMIB(Transactions trans, int acquirerId) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            StringBuilder sb = new StringBuilder();
            sb.append("MALLID=").append(merchantPaymentChannel.getPaymentChannelCode() + "");
            if (merchantPaymentChannel.getPaymentChannelChainCode() != null && merchantPaymentChannel.getPaymentChannelChainCode() > 0) {
                sb.append("&").append("CHAINMALLID=").append(merchantPaymentChannel.getPaymentChannelChainCode() + "");
            }
            sb.append("&").append("SERVICEID=").append(paymentChannel.getServiceId());
            sb.append("&").append("ACQUIRERID=").append(acquirerId);
            sb.append("&").append("TRXCODE=").append(trans.getDokuVoidApprovalCode());
            sb.append("&").append("INVOICENUMBER=").append(trans.getIncTransidmerchant());
            sb.append("&").append("WORDS=").append("WORDS");

            OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("CHECK PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            String words = "";
            String paymentStatus = "";
            String paymentResponseCode = "";
            String paymentApprovalCode = "";
            String cancelResponseCode = "";
            String cancelApprovalCode = "";
            String issuerBank = "";
            String acquirerBank = "";
            String paymentDateTime = "";
            String trxCode = "";
            String errorCode = "";
            if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                /*
                 <MIBGetTransactionResponse>
                 <status>SUCCESS</status>
                 <mallId>7</mallId>
                 <bank>KLIKPAY BCA</bank>
                 <trxCode>5a53b9808d2f8863e6550ce62e830f2e2a99fed1</trxCode>
                 <invoiceNumber>20142101016261648547</invoiceNumber>
                 <currency>IDR</currency>
                 <amount>365000.00</amount>
                 <type>SALE</type>
                 <paymentStatus>SUCCESS</paymentStatus>
                 <paymentDate>2014-09-19 09:40:51</paymentDate>
                 <paymentResponseCode>00</paymentResponseCode>
                 <paymentApprovalCode>005032</paymentApprovalCode>
                 </MIBGetTransactionResponse> 
                 */

                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                xml.load(sr);
                paymentStatus = xml.getString("paymentStatus") != null ? xml.getString("paymentStatus").trim() : "";
                paymentResponseCode = xml.getString("paymentResponseCode") != null ? xml.getString("paymentResponseCode").trim() : "";
                paymentApprovalCode = xml.getString("paymentApprovalCode") != null ? xml.getString("paymentApprovalCode").trim() : "";
                cancelResponseCode = xml.getString("cancelResponseCode") != null ? xml.getString("cancelResponseCode").trim() : "";
                cancelApprovalCode = xml.getString("cancelApprovalCode") != null ? xml.getString("cancelApprovalCode").trim() : "";
                issuerBank = xml.getString("bank") != null ? xml.getString("bank").trim() : "";
                acquirerBank = xml.getString("bank") != null ? xml.getString("bank").trim() : "";
                paymentDateTime = xml.getString("paymentDate") != null ? xml.getString("paymentDate").trim() : "";
                trxCode = xml.getString("trxCode") != null ? xml.getString("trxCode").trim() : "";
                errorCode = xml.getString("errorCode") != null ? xml.getString("errorCode").trim() : "";
                if (paymentStatus != null && !paymentStatus.trim().equals("")) {
                    if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                        if (paymentResponseCode != null && paymentResponseCode.length() != 4) {
                            if (errorCode != null) {
                                if (errorCode.trim().equalsIgnoreCase("XX07")) {
                                    paymentResponseCode = "BB";
                                }
                            }
                            paymentResponseCode = "00" + paymentResponseCode;
                        }
                        trans.setDokuApprovalCode(paymentApprovalCode);
                        trans.setDokuIssuerBank(issuerBank);
                        trans.setDokuResponseCode(paymentResponseCode);
                        trans.setDokuVoidApprovalCode(trxCode);
                        try {
                            OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                            notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                            notifyRequest.setDFSScore("-1");
                            notifyRequest.setDFSIId("");
                            trans.setVerifyId("");
                            trans.setVerifyScore(-1);
                            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                            //      trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                        } catch (Throwable t) {
                            trans.setVerifyScore(0);
                        }
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                        trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        Date tpayment = new Date();
                        try {
                            tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                        } catch (Exception ex) {

                        }
                        trans.setDokuInvokeStatusDatetime(tpayment);
                        em.merge(trans);
                    }
                } else {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                }

                /*
                 if (paymentResponseCode != null && paymentResponseCode.length() != 4) {
                 if (errorCode != null) {
                 if (errorCode.trim().equalsIgnoreCase("XX07")) {
                 paymentResponseCode = "BB";
                 }
                 }
                 paymentResponseCode = "00" + paymentResponseCode;
                 }
                 trans.setDokuApprovalCode(paymentApprovalCode);
                 trans.setDokuIssuerBank(issuerBank);
                 trans.setDokuResponseCode(paymentResponseCode);
                 trans.setDokuVoidApprovalCode(trxCode);
                 try {
                 OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                 notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                 notifyRequest.setDFSScore("-1");
                 notifyRequest.setDFSIId("");
                 trans.setVerifyId("");
                 trans.setVerifyScore(-1);
                 trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                 //      trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                 } catch (Throwable t) {
                 trans.setVerifyScore(0);
                 }

                 if (paymentStatus != null && !paymentStatus.trim().equals("")) {
                 if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                 }else if (paymentStatus.trim().equalsIgnoreCase("REVERSAL")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("VOID")) {
                 //trans.setDokuVoidApprovalCode(cancelResponseCode);
                 trans.setDokuVoidResponseCode(cancelResponseCode);
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                 } else if (paymentStatus.trim().equalsIgnoreCase("REFUND")) {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.REFUND.value());
                 } else {
                 trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                 }
                 trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                 Date tpayment = new Date();                            
                 try {
                 tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                 } catch (Exception ex) {

                 }                                 
                 trans.setDokuInvokeStatusDatetime(tpayment);
                 em.merge(trans);
                 } else {
                 trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                 }
                 */
            } else {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return trans;
    }

    protected Transactions CheckStatusCOREDokupayCIP(Transactions trans) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            String mallId = merchantPaymentChannel.getPaymentChannelCode() + "";
            String invoiceNumber = trans.getIncTransidmerchant();
            String sessionId = trans.getIncSessionid();
            String amount = OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue());
            String words = HashWithSHA1.doHashing(invoiceNumber + amount + mallId + sessionId, null, null);

            // WORDS => SHA1(TRANSIDMERCHANT + AMOUNT + MALLID + SESSIONID)
            StringBuilder sb = new StringBuilder();
            sb.append("MALLID=").append(mallId);
            if (merchantPaymentChannel.getPaymentChannelChainCode() != null && merchantPaymentChannel.getPaymentChannelChainCode() > 0) {
                sb.append("&").append("CHAINNUM=").append(merchantPaymentChannel.getPaymentChannelChainCode() + "");
            } else {
                sb.append("&").append("CHAINNUM=").append("NA");
            }
            sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNumber);
            sb.append("&").append("SESSIONID=").append(trans.getIncSessionid());
            sb.append("&").append("AMOUNT=").append(amount);
            sb.append("&").append("WORDS=").append(words);
            OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("CHECK PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            /*
             <DokupayPaymentResponse>
             <dpMallId>224</dpMallId>
             <ipgMallId>0</ipgMallId>
             <ipgChainnum>0</ipgChainnum>
             <amount>100000.0</amount>
             <transIdMerchant>114911019</transIdMerchant>
             <dokupayId>1840237879</dokupayId>
             <responseCode>0000</responseCode>
             <responseMsg>Transaction Success</responseMsg>
             <approvalCode>483625</approvalCode>
             <hostRefNum/>
             <systraceNo/>
             <batchNum/>
             </DokupayPaymentResponse> 
             */
            String dokupayId = "";
            String paymentResponseCode = "";
            String paymentResponseMessage = "";
            String paymentApprovalCode = "";
            String paymentHostRefNumber = "";
            String paymentSystraceNumber = "";
            String paymentBatchNumber = "";
            String dokuResult = "FAILED";
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.FAILED;

            if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                xml.load(sr);
                dokupayId = xml.getString("dokupayId") != null ? xml.getString("dokupayId").trim() : "";
                paymentResponseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
                if (paymentResponseCode.trim().equals("0000")) {
                    paymentResponseMessage = xml.getString("responseMsg") != null ? xml.getString("responseMsg").trim() : "";
                    paymentApprovalCode = xml.getString("approvalCode") != null ? xml.getString("approvalCode").trim() : "";
                    paymentHostRefNumber = xml.getString("hostRefNum") != null ? xml.getString("hostRefNum").trim() : "";
                    paymentSystraceNumber = xml.getString("systraceNo") != null ? xml.getString("systraceNo").trim() : "";
                    paymentBatchNumber = xml.getString("batchNum") != null ? xml.getString("batchNum").trim() : "";
                    dokuResult = "SUCCESS";
                    status = OneCheckoutTransactionStatus.SUCCESS;

                    trans.setDokuApprovalCode(paymentApprovalCode);
                    trans.setDokuResponseCode(paymentResponseCode);
                    trans.setDokuHostRefNum(paymentHostRefNumber);
                    trans.setDokuResult(dokuResult);
                    trans.setDokuResultMessage(paymentResponseMessage);
                    trans.setTransactionsStatus(status.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
                }
            } else {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return trans;
    }

    protected Transactions CheckStatusCOREDokupayMIP(Transactions trans) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            String dpMallId = merchantPaymentChannel.getMerchantPaymentChannelPid() + "";
            String invoiceNumber = trans.getIncTransidmerchant();
            String sessionId = trans.getIncSessionid();
            String amount = OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue());
            String words = HashWithSHA1.doHashing(invoiceNumber + amount + dpMallId + sessionId, null, null);
            // WORDS => SHA1(TRANSIDMERCHANT + AMOUNT + MALLID + SESSIONID)

            StringBuilder sb = new StringBuilder();
            sb.append("DPMALLID=").append(dpMallId);
            sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNumber);
            sb.append("&").append("SESSIONID=").append(trans.getIncSessionid());
            sb.append("&").append("AMOUNT=").append(amount);
            sb.append("&").append("WORDS=").append(words);
            OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("CHECK PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            /*
             <DokupayPaymentResponse>
             <dpMallId>224</dpMallId>
             <ipgMallId>0</ipgMallId>
             <ipgChainnum>0</ipgChainnum>
             <amount>100000.0</amount>
             <transIdMerchant>114911019</transIdMerchant>
             <dokupayId>1840237879</dokupayId>
             <responseCode>0000</responseCode>
             <responseMsg>Transaction Success</responseMsg>
             <approvalCode>483625</approvalCode>
             <hostRefNum/>
             <systraceNo/>
             <batchNum/>
             </DokupayPaymentResponse>
             */
            String dokupayId = "";
            String paymentResponseCode = "";
            String paymentResponseMessage = "";
            String paymentApprovalCode = "";
            String paymentHostRefNumber = "";
            String paymentSystraceNumber = "";
            String paymentBatchNumber = "";
            String dokuResult = "FAILED";
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.FAILED;

            if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                xml.load(sr);
                dokupayId = xml.getString("dokupayId") != null ? xml.getString("dokupayId").trim() : "";
                paymentResponseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
                if (paymentResponseCode.trim().equals("0000")) {
                    paymentResponseMessage = xml.getString("responseMsg") != null ? xml.getString("responseMsg").trim() : "";
                    paymentApprovalCode = xml.getString("approvalCode") != null ? xml.getString("approvalCode").trim() : "";
                    paymentHostRefNumber = xml.getString("hostRefNum") != null ? xml.getString("hostRefNum").trim() : "";
                    paymentSystraceNumber = xml.getString("systraceNo") != null ? xml.getString("systraceNo").trim() : "";
                    paymentBatchNumber = xml.getString("batchNum") != null ? xml.getString("batchNum").trim() : "";
                    dokuResult = "SUCCESS";
                    status = OneCheckoutTransactionStatus.SUCCESS;

                    trans.setDokuApprovalCode(paymentApprovalCode);
                    trans.setDokuResponseCode(paymentResponseCode);
                    trans.setDokuHostRefNum(paymentHostRefNumber);
                    trans.setDokuResult(dokuResult);
                    trans.setDokuResultMessage(paymentResponseMessage);
                    trans.setTransactionsStatus(status.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
                }
            } else {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return trans;
    }

    protected Transactions CheckStatusCOREAlfa(Transactions trans) {
        try {
            MerchantPaymentChannel merchantPaymentChannel = trans.getMerchantPaymentChannel();//queryHelper.getMerchantPaymentChannel(trxHelper.getMerchant(), trxHelper.getPaymentChannel());
            Merchants m = merchantPaymentChannel.getMerchants();
            PaymentChannel paymentChannel = merchantPaymentChannel.getPaymentChannel();
            String mallId = merchantPaymentChannel.getPaymentChannelCode() + "";
            String accountNumber = trans.getAccountId();
            String invoiceNumber = trans.getIncTransidmerchant();
            String sessionId = trans.getIncSessionid();
            String amount = OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue());
            String words = HashWithSHA1.doHashing(invoiceNumber + merchantPaymentChannel.getMerchantPaymentChannelHash() + accountNumber, null, null);

            // WORDS => SHA1(TRANSIDMERCHANT + AMOUNT + MALLID + SESSIONID)
            StringBuilder sb = new StringBuilder();
            sb.append("VANUMBER=").append(accountNumber);
            sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNumber);
            sb.append("&").append("AMOUNT=").append(amount);
            sb.append("&").append("WORDS=").append(words);
            OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), paymentChannel.getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("CHECK PAYMENT RESULT [" + inetResp.getMsgResponse() + "]");
            /*
             <DokupayCheckStatusResponse>
             <responseCode>0000</responseCode>
             <result>SUCCESS</result>
             <resultMessage></resultMessage>
             <companyCode>12345</companyCode>
             <merchantCode>1234</merchantCode>
             <amount>100000.0</amount>
             <words>a0a4d2c7ca09388f0c1cc78d23c5f6c1a0af8e33</words>
             </DokupayCheckStatusResponse>
             */
            String paymentResult = "FAILED";
            String paymentResultMessage = "";
            String paymentResponseCode = "";
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.FAILED;

            if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                xml.load(sr);
                paymentResponseCode = xml.getString("responseCode") != null ? xml.getString("responseCode").trim() : "";
                if (paymentResponseCode.trim().equals("0000")) {
                    paymentResult = xml.getString("result") != null ? xml.getString("result").trim() : "FAILED";
                    paymentResultMessage = xml.getString("resultMessage") != null ? xml.getString("resultMessage").trim() : "";
                    paymentResult = "SUCCESS";
                    status = OneCheckoutTransactionStatus.SUCCESS;

                    //trans.setDokuApprovalCode(paymentApprovalCode);
                    trans.setDokuResponseCode(paymentResponseCode);
                    //trans.setDokuHostRefNum(paymentHostRefNumber);
                    trans.setDokuResult(paymentResult);
                    trans.setDokuResultMessage(paymentResultMessage);
                    trans.setTransactionsStatus(status.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
                }
            } else {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return trans;
    }

    protected <T extends OneCheckoutDataHelper> String doDirectInquiry(T trxHelper, MerchantPaymentChannel mpc, OneCheckoutDOKUVerifyData verifyRequest) {

        /*
         INQUIRY REQUEST
         MALLID
         CHAINMERCHANT
         PAYMENTCHANNEL
         PAYMENTCODE
         WORDS   -> PAYMENTCODE+MALLID+<shared key>
  
         */
        Merchants merchant = mpc.getMerchants();//trxHelper.getMerchant();

        trxHelper.setCIPMIP(OneCheckoutMethod.MIP);
        String payCode = verifyRequest.getPAYCODE();
//        OneCheckoutLogger.log("PAYCODE =* " + payCode);

        StringBuilder sb = new StringBuilder();

        sb.append("MALLID=").append(merchant.getMerchantCode());
        sb.append("&CHAINMERCHANT=").append(merchant.getMerchantChainMerchantCode());
        sb.append("&PAYMENTCHANNEL=").append(trxHelper.getPaymentChannel().value());
        sb.append("&PAYMENTCODE=").append(payCode);
        sb.append("&STATUSTYPE=").append("I");
        String word = merchant.getMerchantCode() + merchant.getMerchantHashPassword() + payCode;
        String hashwords = HashWithSHA1.doHashing(word, "SHA1", null);

        sb.append("&WORDS=").append(hashwords);
//        String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//        String ocoId = this.generateOcoId(paymentChannelId);
        String ocoId = trxHelper.getOcoId();
        sb.append("&OCOID=").append(ocoId);
        try {

            OneCheckoutLogger.log("doInquiryInvoice : Inquiry Request : %s", sb.toString());
            InternetResponse resp = this.doFetchHTTP(sb.toString(), merchant.getMerchantIdentifyUrl(), merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout());
            // OneCheckoutNotifyStatusResponse inquiry_response = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout());
            OneCheckoutLogger.log("doInquiryInvoice : Inquiry Response : %s", resp.getMsgResponse());

            XMLConfiguration xml = new XMLConfiguration();
            StringReader sr = new StringReader(resp.getMsgResponse());
            xml.load(sr);

            OneCheckoutPaymentRequest pReq = this.parseXMLInquiry(mpc, xml);
            pReq.setPAYCODE(payCode);
            trxHelper.setPaymentRequest(pReq);

            String transExist = pReq.getADDITIONALINFO();

            return transExist;
        } catch (Exception ex) {
            return "EMPTY";
        }
    }

    protected Transactions CheckStatusCOREClickPayMandiri(Transactions trans, Merchants m) {

        try {

            StringBuilder sb = new StringBuilder();
            sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode() + "");
            if (trans.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && trans.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                sb.append("&").append("CHAINMALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
            }
            sb.append("&").append("ACQUIRERID=").append("300");
//            sb.append("&").append("INVOICENUMBER=").append(trans.getIncTransidmerchant());
            sb.append("&").append("TRANSIDMERCHANT=").append(trans.getIncTransidmerchant());
            sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            sb.append("&").append("SERVICEID=").append(trans.getMerchantPaymentChannel().getPaymentChannel().getServiceId());
            sb.append("&").append("TRXCODE=").append(trans.getTrxCodeCore());

            OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
            InternetResponse inetResp = this.doFetchHTTP(sb.toString(), trans.getMerchantPaymentChannel().getPaymentChannel().getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
            OneCheckoutLogger.log("::MESSAGE RESPONSE = " + inetResp.getMsgResponse().trim());
            if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {

                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                xml.load(sr);
                String paymentStatus = xml.getString("STATUS") != null ? xml.getString("STATUS").trim() : "";
                OneCheckoutLogger.log(":: PAYMENT STATUS = " + paymentStatus.trim());
                if (paymentStatus != null && !paymentStatus.trim().equalsIgnoreCase("") && !paymentStatus.trim().equalsIgnoreCase("INVALID_REQUEST")) {
                    if (!paymentStatus.trim().equalsIgnoreCase("TRX_NOT_FOUND")) {
                        String paymentResponseCode = xml.getString("RESPONSECODE") != null ? xml.getString("RESPONSECODE").trim() : "";
                        String paymentRefNumber = xml.getString("HOSTREFNUMBER") != null ? xml.getString("HOSTREFNUMBER").trim() : "";
                        String cancelResponseCode = xml.getString("CANCELRESPONSECODE") != null ? xml.getString("CANCELRESPONSECODE").trim() : "";
                        String cancelRefNumber = xml.getString("CANCELHOSTREFNUMBER") != null ? xml.getString("CANCELHOSTREFNUMBER").trim() : "";
                        String acquirerBank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
                        String maskedCard = xml.getString("CARDNUMBER") != null ? xml.getString("CARDNUMBER").trim() : "";
                        String paymentDateTime = xml.getString("DATETIME") != null ? xml.getString("DATETIME").trim() : "";
                        String bank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
                        if (paymentResponseCode.length() != 4) {
                            paymentResponseCode = "0099";
                        }
                        if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                            paymentResponseCode = "0000";
                        }
                        trans.setDokuIssuerBank(bank);
                        trans.setDokuResponseCode(paymentResponseCode);
                        trans.setDokuHostRefNum(paymentRefNumber);
                        try {
                            OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                            notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                            notifyRequest.setDFSScore("-1");
                            notifyRequest.setDFSIId("");
                            trans.setVerifyId("");
                            trans.setVerifyScore(-1);
                            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                            trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                        } catch (Throwable t) {
                            trans.setVerifyScore(0);
                        }

                        if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                        } else if (paymentStatus.trim().equalsIgnoreCase("REVERSAL")) {
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
                        } else if (paymentStatus.trim().equalsIgnoreCase("VOID")) {
                            //trans.setDokuVoidApprovalCode(cancelResponseCode);
                            trans.setDokuVoidResponseCode(cancelResponseCode);
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                        } else if (paymentStatus.trim().equalsIgnoreCase("REFUND")) {
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.REFUND.value());
                        } else {
                            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                        }
                        trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                        Date tpayment = new Date();
                        try {
                            tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                        } catch (Exception ex) {

                        }

                        trans.setDokuInvokeStatusDatetime(tpayment);

                        em.merge(trans);
                    } else {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                    }
                } else {
                    OneCheckoutLogger.log("::RESPONSE FROM MIB IS INVALID REQUEST::");
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                }
            } else {
                OneCheckoutLogger.log("::RESPONSE FROM MIB IS ERROR::");
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
            }

        } catch (Exception ex) {
            OneCheckoutLogger.log(":: PROCESS CATCH ::");
            trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
        }

        return trans;

    }

    protected OneCheckoutDataPGRedirect addDWRegistrationPage(OneCheckoutDataHelper oneCheckoutDataHelper, OneCheckoutDataPGRedirect redirect, OneCheckoutPaymentChannel channel, MerchantPaymentChannel merchantPaymentChannel, Transactions trans) {

        try {
            if (channel == OneCheckoutPaymentChannel.DOKUPAY) {
                return redirect;
            }

            MerchantPaymentChannel mpc = nl.getMerchantPaymentChannel(merchantPaymentChannel.getMerchants(), OneCheckoutPaymentChannel.DOKUPAY);
            if (mpc != null) {
                String arrayMallId = config.getString("ONECHECKOUT.REGDW.MALLID", "");
                String RegDWAction = config.getString("ONECHECKOUT.REGDW.SERVLET", "/Suite/DWReg");
                String grepMallId = "_" + mpc.getMerchants().getMerchantCode();
                if (!arrayMallId.trim().equals("") && arrayMallId.contains(grepMallId)) {
                    // BEGIN check to dokuwallet
                    String urlCheckDW = config.getString("ONECHECKOUT.REGDW.CHECKURL", "");
                    int conTimeout = config.getInt("ONECHECKOUT.REGDW.CONNECTIONTIMEOUT", -1);
                    int readTimeout = config.getInt("ONECHECKOUT.REGDW.READTIMEOUT", -1);

                    String email = trans.getIncEmail();
                    StringBuilder sb = new StringBuilder();
                    sb.append(mpc.getMerchantPaymentChannelPid());
                    sb.append(mpc.getMerchantPaymentChannelHash());
                    sb.append(email);
                    //SHA1 (DPMALLID + sharedKey + DOKUPAYID + EMAIL)
                    OneCheckoutLogger.log("OneCheckoutChannelBase.addDWRegistrationPage Clear Text " + sb.toString());
                    String hashwords = HashWithSHA1.doHashing(sb.toString(), null, null);
                    OneCheckoutLogger.log("OneCheckoutChannelBase.addDWRegistrationPage " + hashwords);

                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    hashMap.put("DPMALLID", mpc.getMerchantPaymentChannelPid());
                    hashMap.put("EMAIL", email);
                    hashMap.put("WORDS", hashwords);

                    String data_encode = this.createParamsHTTP(hashMap);
                    OneCheckoutLogger.log("OneCheckoutChannelBase.addDWRegistrationPage DW CheckStatus : Post Params" + data_encode);
                    OneCheckoutLogger.log("OneCheckoutChannelBase.addDWRegistrationPage Fetch URL     : %s", urlCheckDW);

                    boolean skipRegistration = false;
                    try {
                        InternetResponse resp = this.doFetchHTTP(data_encode, urlCheckDW, conTimeout, readTimeout);

                        OneCheckoutLogger.log("OneCheckoutChannelBase.addDWRegistrationPage  DW HTTP Response Code    : %t", resp.getHTTPrespCode());

                        if (resp.getHTTPrespCode() == 200) {

                            String respACK = resp.getMsgResponse();

                            OneCheckoutLogger.log("OneCheckoutChannelBase.addDWRegistrationPage  DW Response    : %s", respACK);

                            XMLConfiguration xml = new XMLConfiguration();
                            StringReader sr = new StringReader(respACK);
                            xml.load(sr);

                            String dokupayID = xml.getString("dokupayID");

                            if (dokupayID != null && !dokupayID.isEmpty()) {
                                // is email registered on dokuwallet
                                skipRegistration = true;
                            }

                            /*
                             *VALIDATION RESPONSE
                             <?xml version="1.0" encoding="UTF-8"?>
                             <WalletValidationResponse>
                             <dokupayID />
                             <email />
                             <phone />
                             <dob />
                             <gender />
                             <dpmallid />
                             </WalletValidationResponse>
                             */
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    // END check to dokuwallet

                    if (skipRegistration) {
                        return redirect;
                    } else {
                        try {
                            if (oneCheckoutDataHelper.isDwRegStatus()) {
                                String regUrl = config.getString("ONECHECKOUT.REGDW.REGURL");
                                OneCheckoutLogger.log("Fetch URL     : %s", regUrl);
                                String clearText = oneCheckoutDataHelper.getDwRegEmail() + mpc.getMerchantPaymentChannelPid() + mpc.getMerchantPaymentChannelHash() + oneCheckoutDataHelper.getDwRegPhone();
                                HashMap<String, String> regDWRequest = new HashMap<String, String>();
                                regDWRequest.put("NAME", oneCheckoutDataHelper.getDwRegName());
                                regDWRequest.put("EMAIL", oneCheckoutDataHelper.getDwRegEmail());
                                regDWRequest.put("PHONE", oneCheckoutDataHelper.getDwRegPhone());
                                regDWRequest.put("DPMALLID", mpc.getMerchantPaymentChannelPid());
                                regDWRequest.put("WORDS", HashWithSHA1.doHashing(clearText, null, null));
                                OneCheckoutLogger.log("DW Registration : Post Params" + regDWRequest.toString());
                                OneCheckoutChannelBase base = new OneCheckoutChannelBase();
                                String regDw = base.createParamsHTTP(regDWRequest);
                                OneCheckoutLogger.log("DW Data : %s", regDw);
                                InternetResponse resp = base.doFetchHTTP(regDw, regUrl, conTimeout, readTimeout);
                                OneCheckoutLogger.log("RESPONSE : %s", resp.getMsgResponse());
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    HashMap resultData = redirect.getRetryData();

                    resultData.put("REGNAME", trans.getIncName());
                    resultData.put("REGEMAIL", trans.getIncEmail());
                    resultData.put("REGPHONE", "");
                    resultData.put("REGACTION", RegDWAction != null ? RegDWAction : "");

                    String checksum_ver = HashWithSHA1.doHashing(trans.getTransactionsId() + "cintaKU**", "SHA2", null);

                    resultData.put("REGCHECKSUM", checksum_ver);
                    resultData.put("REGNUMBERID", trans.getTransactionsId() + "");

                    if (channel == OneCheckoutPaymentChannel.BSP || channel == OneCheckoutPaymentChannel.CreditCard || channel == OneCheckoutPaymentChannel.BNIDebitOnline) {
                        try {
                            if (trans.getTransactionsDataCardholders() != null && trans.getTransactionsDataCardholders().size() > 0) {
                                TransactionsDataCardholder ch = trans.getTransactionsDataCardholders().iterator().next();

                                resultData.put("REGPHONE", ch.getIncMobilephone() != null ? ch.getIncMobilephone() : "");
                            }
                        } catch (org.hibernate.LazyInitializationException le) {
                            OneCheckoutLogger.log(le.getMessage());
                            TransactionsDataCardholder ch = nl.getCH(trans);
                            if (ch != null) {
                                resultData.put("REGPHONE", ch.getIncMobilephone() != null ? ch.getIncMobilephone() : "");
                            }
                        } catch (Throwable ex) {
                            TransactionsDataCardholder ch = nl.getCH(trans);
                            if (ch != null) {
                                resultData.put("REGPHONE", ch.getIncMobilephone() != null ? ch.getIncMobilephone() : "");
                            }

                        }
                    }
                    hashMap.put("REGDPMALLID", mpc.getMerchantPaymentChannelPid());
                    //redirect.setPageTemplate("transactionSuccessWithDWReg.html");
                    redirect.setRetryData(resultData);
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return redirect;
    }

    public String generateMPGReversalWords(OneCheckoutDataHelper trxHelper, Transactions trans) {

        try {
            StringBuilder word = new StringBuilder();
            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();
            word.append(notifyRequest.getAMOUNT());
            word.append(trxHelper.getMallId());
            word.append(trxHelper.getChainMerchantId());
            word.append(notifyRequest.getTRANSIDMERCHANT());
            word.append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelHash());
            word.append(notifyRequest.getCURRENCY());

            OneCheckoutLogger.log("ONECHECKOUT ELEMENT [" + word.toString() + "]");

            String hashwords = HashWithSHA1.doHashing(word.toString(), null, null);

            return hashwords;
        } catch (Throwable iv) {
            iv.printStackTrace();
            throw new InvalidPaymentRequestException(iv.getMessage());
        }
    }

//    public String generateOcoId(String paymentChannel) {
//        String result = null;
//        try {
//            SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyhhmmssSSS");
//            String dateFormatResult = dateFormat.format(new Date());
//
//            Random rn = new Random();
//            int range = 999 - 0 + 1;
//            int randomNum = rn.nextInt(range) + 0;
//
//            result = dateFormatResult + randomNum + paymentChannel;
//
//        } catch (Throwable th) {
//            th.printStackTrace();
//            OneCheckoutLogger.log("Error GenerateOcoId = " + th.getMessage());
//        }
//        return result;
//    }
}
