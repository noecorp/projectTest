/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.view;

import com.doku.lib.inet.DOKUInternetProtocol;
import com.doku.lib.inet.DokuIntConnection;
import com.doku.lib.inet.InternetRequest;
import com.doku.lib.inet.InternetResponse;
import com.doku.lib.inet.RequestType;
import com.google.gson.Gson;
import com.onecheckoutV1.data.*;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBean;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBean;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.interfaces.InterfacePluginPostParsePayment;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;

import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutPrincipal;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.template.CustomerBillObject;
import com.onecheckoutV1.template.CustomerInfoObject;
import com.onecheckoutV1.template.CustomerTokenObject;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.type.*;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Rates;
import com.onechekoutv1.dto.Transactions;
import doku.accountbilling.xml.RESPONSEDocument;
import doku.accountbilling.xml.RESPONSEDocument.RESPONSE.TOKENS.TOKEN;
import doku.accountbilling.xml.RESPONSEDocument.RESPONSE.TOKENS.TOKEN.BILLS.BILL;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.Key;
import java.security.MessageDigest;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.jboss.security.SecurityAssociation;
import org.owasp.esapi.ESAPI;
import sun.misc.BASE64Decoder;

/**
 *
 * @author hafizsjafioedin
 */
public class WebUtils {

    public static final String SESSION_KLIKBCA_THANKS = "ONECHECKOUTYASAYN121212sdsdKK";
    private static String numberChar[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    public static HashMap<String, String> copyParams(HttpServletRequest request) {
        HashMap<String, String> params = new HashMap<String, String>();
        HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
        params.keySet();
        OneCheckoutBaseRules base = new OneCheckoutBaseRules();
        Set m = req.getParameterMap().keySet();
        for (Object o : m) {
            String key = (String) o;
            String val = null;

            if (!key.equals("BASKET") || !key.equals("SECURITYCODE") || !key.equals("MOBILEPHONE") || !key.equals("ADDITIONALDATA") || !key.equals("EMAIL") || !key.equals("ADDRESS")) {
                val = req.getParameter(key);
            }
            if (key.equalsIgnoreCase("CARDNUMBER")) {
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : %s , VALUE : [%s]", key, base.maskingString(val, "PAN"));
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : BIN , VALUE : [%s]", val.substring(0, 6));
            } else if (key.equalsIgnoreCase("SECURITYCODE")) {
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : %s , VALUE : [%s]", key, base.maskingString(val, null));
            } else if (key.equalsIgnoreCase("CVV2")) {
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : 3DIGIT , VALUE : [%s]", key, "***");
            } else {
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : %s , VALUE : [%s]", key, val);
            }
//            OneCheckoutLogger.log(key + " = "+val);
            params.put(key, val);
        }

//        if (request.getParameter("TOKENID") != null){
//            params.put("TOKENID", request.getParameter("TOKENID"));
//            params.put("CVV2", request.getParameter("CVV2TOKEN"));
//        }
        params.put("SECURITYCODE", request.getParameter("SECURITYCODE"));
        //OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : SECURITYCODE , VALUE : [%s]", "*********");
        params.put("BASKET", request.getParameter("BASKET"));
        OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : BASKET , VALUE : [%s]", params.get("BASKET"));
        params.put("MOBILEPHONE", request.getParameter("MOBILEPHONE"));
        OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : MOBILEPHONE , VALUE : [%s]", params.get("MOBILEPHONE"));
        params.put("EMAIL", request.getParameter("EMAIL"));
        OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : EMAIL , VALUE : [%s]", params.get("EMAIL"));
        params.put("ADDITIONALDATA", request.getParameter("ADDITIONALDATA"));
        OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : ADDITIONALDATA , VALUE : [%s]", params.get("ADDITIONALDATA"));
        params.put("ADDRESS", request.getParameter("ADDRESS"));
        OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : ADDRESS , VALUE : [%s]", params.get("ADDRESS"));

        if (request.getParameter("BILLNUMBER") != null) {
            params.put("BILLNUMBER", request.getParameter("BILLNUMBER"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : BILLNUMBER , VALUE : [%s]", params.get("BILLNUMBER"));
            params.put("BILLDETAIL", request.getParameter("BILLDETAIL"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : BILLDETAIL , VALUE : [%s]", params.get("BILLDETAIL"));
            params.put("BILLTYPE", request.getParameter("BILLTYPE"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : BILLTYPE , VALUE : [%s]", params.get("BILLTYPE"));
            params.put("STARTDATE", request.getParameter("STARTDATE"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : STARTDATE , VALUE : [%s]", params.get("STARTDATE"));
            params.put("ENDDATE", request.getParameter("ENDDATE"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : ENDDATE , VALUE : [%s]", params.get("ENDDATE"));
            params.put("EXECUTETYPE", request.getParameter("EXECUTETYPE"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : EXECUTETYPE , VALUE : [%s]", params.get("EXECUTETYPE"));
            params.put("EXECUTEDATE", request.getParameter("EXECUTEDATE"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : EXECUTEDATE , VALUE : [%s]", params.get("EXECUTEDATE"));
            params.put("EXECUTEMONTH", request.getParameter("EXECUTEMONTH"));
            OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : EXECUTEMONTH , VALUE : [%s]", params.get("EXECUTEMONTH"));
        }
        if (request.getParameter("ACQUIRERID") != null) {
            if (!request.getParameter("ACQUIRERID").equals("")) {
                params.put("ACQUIRERID", request.getParameter("ACQUIRERID"));
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : ACQUIRERID , VALUE : [%s]", params.get("ACQUIRERID"));
            }
        }
        if (request.getParameter("INVOICENUMBER") != null) {
            if (!request.getParameter("INVOICENUMBER").equals("")) {
                params.put("INVOICENUMBER", request.getParameter("INVOICENUMBER"));
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : INVOICENUMBER , VALUE : [%s]", params.get("INVOICENUMBER"));
            }
        }
        if (request.getParameter("DESCRIPTION") != null) {
            if (!request.getParameter("DESCRIPTION").equals("")) {
                params.put("DESCRIPTION", request.getParameter("DESCRIPTION"));
                OneCheckoutLogger.log("WebUtils.copyParams PARAMETER : DESCRIPTION , VALUE : [%s]", params.get("DESCRIPTION"));
            }
        }
        // System.out.println(params.toString());
        return params;
    }

    // OK
    public static ArrayList convertHashMap(HashMap<String, String> params) {
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
                if (!key.equalsIgnoreCase("vpc_CardExp") && !key.equalsIgnoreCase("vpc_CardSecurityCode") && !key.equalsIgnoreCase("vpc_CardNum")) {
                    OneCheckoutLogger.log(key + " : " + val);
                }

                OneCheckoutBaseRules base = new OneCheckoutBaseRules();
//                OneCheckoutLogger.log(key + " : " + val);

                if (key.equalsIgnoreCase("vpc_CardNum")) {
                    OneCheckoutLogger.log(key + " : " + base.maskingString(val, "PAN"));
                } else if (key.equalsIgnoreCase("vpc_CardExp")) {
                    OneCheckoutLogger.log(key + " : " + base.maskingString(val, "EXPIRYDATE"));
                } else if (key.equalsIgnoreCase("vpc_CardSecurityCode")) {
                    OneCheckoutLogger.log(key + " : " + base.maskingString(val, "CVV"));
                }
//                OneCheckoutLogger.log(key + " : " + val);
            }

        }

        return list;

    }

    // OK
    public static Merchants getValidMALLID(int mId, int cmId) {

        OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

        try {

            OneCheckoutLogger.log("WebUtils.getValidMALLID : MALLID=%d", mId);
            Merchants m = queryHelper.getMerchantByMallId(mId, cmId);

            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }
    }

    public static OneCheckoutDataHelper parseACSData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();
        try {

            OneCheckoutPaymentRequest paymentRequest = new OneCheckoutPaymentRequest();

            paymentRequest.setAllAdditionData(params);
            paymentRequest.getAllAdditionData().put("PaRes", request.getParameter("PaRes"));

            OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

            String XID = paymentRequest.getAdditionalData("MD");

            if (XID == null || XID.isEmpty()) {
                onecheckout.setMessage("10064|XID is null");
                //onecheckout.setErrorMessage(OneCheckoutErrorMessage.ERROR_VALIDATE_INPUT);ytfh
                return onecheckout;
            }

            Transactions trans = queryHelper.getTransactionByXID(XID);

            if (trans == null) {
                onecheckout.setMessage("10065|Transaction not found by XID=" + XID);
                return onecheckout;
            } else if (trans.getTransactionsState() == OneCheckoutTransactionState.INCOMING.value()) {
                trans.setTransactionsState(OneCheckoutTransactionState.PROCESS.value());
                queryHelper.updateTransactions(trans);
            } else {
                onecheckout.setMessage("10066|Transaction with XID=" + XID + " has been processed");
                return onecheckout;
            }

            onecheckout.setMallId(trans.getIncMallid());
            onecheckout.setChainMerchantId(trans.getIncChainmerchant());

            Merchants m = trans.getMerchantPaymentChannel().getMerchants();
            onecheckout.setMerchant(m);
            onecheckout.setPaymentRequest(paymentRequest);
            onecheckout.setTransactions(trans);

            OneCheckoutPaymentChannel pchannel = OneCheckoutPaymentChannel.findType(trans.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());

            onecheckout.setPaymentChannel(pchannel);

            onecheckout.setMessage("ACS");

        } catch (Exception ex) {
            ex.printStackTrace();
            onecheckout.setMessage("10055|Fail read parameters");
        }

        return onecheckout;
    }

    public static OneCheckoutDataHelper parseAcquirerData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {

        OneCheckoutDataHelper oneCheckoutDataHelper = new OneCheckoutDataHelper();
        try {

            OneCheckoutPaymentRequest paymentRequest = new OneCheckoutPaymentRequest();
            paymentRequest.setAllAdditionData(params);
            String XID = paymentRequest.getAdditionalData("vpc_Merchant") + paymentRequest.getAdditionalData("vpc_MerchTxnRef");
            if (XID == null || XID.isEmpty()) {
                oneCheckoutDataHelper.setMessage("10064|XID is null");
                return oneCheckoutDataHelper;
            }

            OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            Transactions trans = queryHelper.getTransactionByXID(XID);
            if (trans == null) {
                oneCheckoutDataHelper.setMessage("10065|Transaction not found by XID=" + XID);
                return oneCheckoutDataHelper;
            } else if (trans.getTransactionsState() == OneCheckoutTransactionState.INCOMING.value()) {
                trans.setTransactionsState(OneCheckoutTransactionState.PROCESS.value());
                queryHelper.updateTransactions(trans);
            } else {
                oneCheckoutDataHelper.setMessage("10066|Transaction with XID=" + XID + " has been processed");
                return oneCheckoutDataHelper;
            }

            oneCheckoutDataHelper.setMallId(trans.getIncMallid());
            oneCheckoutDataHelper.setChainMerchantId(trans.getIncChainmerchant());
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();
            oneCheckoutDataHelper.setMerchant(m);
            oneCheckoutDataHelper.setPaymentRequest(paymentRequest);
            oneCheckoutDataHelper.setTransactions(trans);
            OneCheckoutPaymentChannel pchannel = OneCheckoutPaymentChannel.findType(trans.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());
            oneCheckoutDataHelper.setPaymentChannel(pchannel);
            oneCheckoutDataHelper.setMessage("REDIRECTACQUIRER");
        } catch (Exception ex) {
            ex.printStackTrace();
            oneCheckoutDataHelper.setMessage("10055|Fail read parameters");
        }

        return oneCheckoutDataHelper;
    }

    public static Merchants getValidMALLID(int mId) {

        OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

        try {

            OneCheckoutLogger.log("WebUtils.getValidMALLID : MALLID=%d", mId);
            Merchants m = queryHelper.getMerchantBy(mId);

            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }
    }

    public static OneCheckoutPaymentRequest autoGenerate(HashMap<String, String> params, OneCheckoutPaymentRequest paymentRequest, OneCheckoutPaymentChannel pchannel) {

        boolean BNIDebitOnline = pchannel != null && pchannel.value().equalsIgnoreCase(OneCheckoutPaymentChannel.BNIDebitOnline.value());

        if (params.get("CARDNUMBER") != null) {
            paymentRequest.setCARDNUMBER(params.remove("CARDNUMBER"));

        }

        if (params.get("EXPIRYDATE") != null) {
            paymentRequest.setEXPIRYDATE(params.remove("EXPIRYDATE"));
        }

        if (params.get("CVV2") != null) {
            paymentRequest.setCVV2(params.remove("CVV2"));
        }

        if (params.get("INSTALLMENT_ACQUIRER") != null) {
            paymentRequest.setINSTALLMENT_ACQUIRER(params.remove("INSTALLMENT_ACQUIRER"));
        }

        if (params.get("TENOR") != null) {
            paymentRequest.setTENOR(params.remove("TENOR"));
        }

        if (params.get("PROMOID") != null) {
            paymentRequest.setPROMOID(params.remove("PROMOID"));
        }

        if (params.get("CC_NAME") != null) {
            paymentRequest.setCC_NAME(params.remove("CC_NAME"));
        } else if (BNIDebitOnline) {
            paymentRequest.setCC_NAME("NAME " + OneCheckoutPaymentChannel.BNIDebitOnline.toString());
        }

        if (params.get("ADDRESS") != null) {
            try {
                paymentRequest.setADDRESS(params.remove("ADDRESS"));
            } catch (Exception ex) {
                paymentRequest.setEmptyADDRESS();
            }

        } else if (BNIDebitOnline) {
            paymentRequest.setADDRESS("ADDRESS " + OneCheckoutPaymentChannel.BNIDebitOnline.toString());
        }

        if (params.get("CITY") != null) {
            try {
                paymentRequest.setCITY(params.remove("CITY"));
            } catch (Exception ex) {
                paymentRequest.setEmptyCITY();
            }
        } else if (BNIDebitOnline) {
            paymentRequest.setCITY("CITY " + OneCheckoutPaymentChannel.BNIDebitOnline.toString());
        }

        if (params.get("STATE") != null) {

            try {
                paymentRequest.setSTATE(params.remove("STATE"));
            } catch (Exception ex) {
                paymentRequest.setEmptySTATE();
            }

        } else if (BNIDebitOnline) {
            paymentRequest.setSTATE("STATE " + OneCheckoutPaymentChannel.BNIDebitOnline.toString());
        }

        if (params.get("COUNTRY") != null) {
            try {
                paymentRequest.setCOUNTRY(params.remove("COUNTRY"));
            } catch (Exception ex) {
                paymentRequest.setEmptyCOUNTRY();
            }

        } else if (BNIDebitOnline) {
            paymentRequest.setCOUNTRY("ID");
        }

        if (params.get("ZIPCODE") != null) {
            try {
                paymentRequest.setZIPCODE(params.remove("ZIPCODE"));
            } catch (Exception ex) {
                paymentRequest.setEmptyZIPCODE();
            }

        } else if (BNIDebitOnline) {
            paymentRequest.setZIPCODE("123456");
        }

        if (params.get("HOMEPHONE") != null) {
            paymentRequest.setHOMEPHONE(params.remove("HOMEPHONE"));
        } else if (BNIDebitOnline) {
            paymentRequest.setHOMEPHONE("0212345678");
        }

        if (params.get("MOBILEPHONE") != null) {
            try {
                paymentRequest.setMOBILEPHONE(params.remove("MOBILEPHONE"));
            } catch (Exception ex) {
                paymentRequest.setEmptyMOBILEPHONE();
            }
        } else if (BNIDebitOnline) {
            paymentRequest.setMOBILEPHONE("6285123456789");
        }

        if (params.get("WORKPHONE") != null) {
            paymentRequest.setWORKPHONE(params.remove("WORKPHONE"));
        } else if (BNIDebitOnline) {
            paymentRequest.setWORKPHONE("0212345678");
        }

        if (params.get("BIRTHDATE") != null) {
            paymentRequest.setBIRTHDATE(params.remove("BIRTHDATE"));
        } else if (BNIDebitOnline) {
            paymentRequest.setBIRTHDATE("19700606");
        }

        return paymentRequest;
    }

    public static OneCheckoutDataHelper parseRecurUpdateCardRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {
        OneCheckoutDataHelper oneCheckoutDataHelper = new OneCheckoutDataHelper();

        String mallId = "";
        int mId = 0;
        String chainMerchantId = "";
        int cmId = 0;
        String ipaddress = "";
        String customerId = "";
        String billNumber = "";

        try {
            try {
                mallId = params.remove("MALLID");
                mId = OneCheckoutBaseRules.validateMALLID2(mallId);
                OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : MALLID=%d", mId);
                chainMerchantId = params.remove("CHAINMERCHANT");
                cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);

                OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : CHAINMERCHANT=%d", cmId);
                oneCheckoutDataHelper.setMallId(mId);
                oneCheckoutDataHelper.setChainMerchantId(cmId);
                Merchants m = WebUtils.getValidMALLID(mId, cmId);
                oneCheckoutDataHelper.setMerchant(m);
            } catch (Exception e) {
                OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : ERROR1 : %s", e.getMessage());
                oneCheckoutDataHelper.setMessage(e.getMessage());
                oneCheckoutDataHelper.setMallId(0);
                oneCheckoutDataHelper.setChainMerchantId(0);
                oneCheckoutDataHelper.setMerchant(null);
                return oneCheckoutDataHelper;
            }

            //OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            OneCheckoutPaymentRequest oneCheckoutPaymentRequest = new OneCheckoutPaymentRequest();
            ipaddress = request.getHeader("True-Client-IP");
            if (ipaddress == null) {
                ipaddress = request.getHeader("X-Forwarded-For");
            }
            if (ipaddress == null) {
                ipaddress = request.getRemoteAddr();
            }
            oneCheckoutDataHelper.setSystemSession(ipaddress + "|" + request.getHeader("User-Agent") + "|" + request.getSession().getId());
            OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData SYSTEM_SESSION : %s", oneCheckoutDataHelper.getSystemSession());
            OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : Parsing Mandatory Parameters");
//            oneCheckoutPaymentRequest.setCUSTIP(request.getRemoteAddr()
            oneCheckoutPaymentRequest.setCUSTIP(ipaddress);
            oneCheckoutPaymentRequest.setMALLID(oneCheckoutDataHelper.getMallId());
            oneCheckoutPaymentRequest.setCHAINMERCHANT(oneCheckoutDataHelper.getChainMerchantId());
            oneCheckoutPaymentRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
            oneCheckoutPaymentRequest.setREQUESTDATETIME(params.remove("REQUESTDATETIME"));
            oneCheckoutPaymentRequest.setCURRENCY("360");
            oneCheckoutPaymentRequest.setAMOUNT("10000");
            oneCheckoutPaymentRequest.setPURCHASECURRENCY("360");
            oneCheckoutPaymentRequest.setPURCHASEAMOUNT("10000");

            oneCheckoutPaymentRequest.setWORDS(params.remove("WORDS"));
            oneCheckoutPaymentRequest.setSESSIONID(params.remove("SESSIONID"));
            oneCheckoutPaymentRequest.setADDITIONALINFO(params.remove("ADDITIONALDATA"));
            OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : paymentRequest.getADDITIONALINFO() : %s", oneCheckoutPaymentRequest.getADDITIONALINFO());

//          oneCheckoutPaymentRequest.setUSERNAME(request.getUserPrincipal().getName() != null ? request.getUserPrincipal().getName() : "");
//          oneCheckoutPaymentRequest.setPAYREASON(params.remove("PAYREASON"));
            //PAYMENTCHANNEL
            String paymentChannel = params.remove("PAYMENTCHANNEL");
            OneCheckoutPaymentChannel oneCheckoutPaymentChannel = OneCheckoutBaseRules.validatePaymentChannel(paymentChannel);
            if ((paymentChannel == null || paymentChannel.isEmpty()) && oneCheckoutPaymentChannel == null) {
                oneCheckoutPaymentRequest = autoGenerate(params, oneCheckoutPaymentRequest, oneCheckoutPaymentChannel);
                // CIP without channel selected
                // multi payment page display
                OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : Undefined Payment Channel, give multi payment page");
                oneCheckoutDataHelper.setPaymentRequest(oneCheckoutPaymentRequest);
                oneCheckoutDataHelper.setMessage("VALID");
                OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : end");
                return oneCheckoutDataHelper;
            }
            OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : Payment Channel : %s", oneCheckoutPaymentChannel.name());
            oneCheckoutDataHelper.setPaymentChannel(oneCheckoutPaymentChannel);

            Character merchantCategory = oneCheckoutDataHelper.getMerchant().getMerchantCategory();
            OneCheckoutMerchantCategory oneCheckoutMerchantCategory = null;
            if (merchantCategory != null) {
                try {
                    oneCheckoutMerchantCategory = OneCheckoutMerchantCategory.findType(merchantCategory);
                    OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : Merchant Category : %s", oneCheckoutMerchantCategory.name());
                    oneCheckoutDataHelper.setMerchantCategory(oneCheckoutMerchantCategory);
                } catch (Throwable t) {
                    OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : CANNOT FIND MERCHANT CATEGORY");
                }
            }

            customerId = params.get("CUSTOMERID") != null ? params.remove("CUSTOMERID").trim() : "";
            billNumber = params.get("BILLNUMBER") != null ? params.remove("BILLNUMBER").trim() : "";
            oneCheckoutPaymentRequest.setCUSTOMERID(customerId);
            oneCheckoutPaymentRequest.setBILLNUMBER(billNumber);
            oneCheckoutPaymentRequest.setUPDATEBILLSTATUS(true);

            oneCheckoutDataHelper.setPaymentRequest(oneCheckoutPaymentRequest);
            oneCheckoutDataHelper.setMessage("VALID");
            OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : end");
            return oneCheckoutDataHelper;

        } catch (Exception e) {
            e.printStackTrace();
            OneCheckoutLogger.log("WebUtils.parseRecurUpdateCardRequestData : ERROR2 : %s", e.getMessage());
            oneCheckoutDataHelper.setMessage(e.getMessage());
            return oneCheckoutDataHelper;
        } finally {
            mallId = null;
            mId = 0;
            chainMerchantId = null;
            cmId = 0;
            ipaddress = null;
            customerId = null;
            billNumber = null;
        }
    }

    public static String createXML(HashMap<String, Object> params, String processName) {
        String result = null;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\"?>");
            builder.append("<").append(processName.toUpperCase()).append(">");
            for (int i = 0; i < params.size(); i++) {
                String key = params.keySet().toArray()[i] + "";
                String value = params.get(key) + "";
                builder.append("<").append(key).append(">").append(value).append("</").append(key).append(">");

            }
            builder.append("</").append(processName.toUpperCase()).append(">");
            result = builder + "";
        } catch (Throwable th) {
            OneCheckoutLogger.log("Error createXML = " + th.getMessage());
            th.printStackTrace();
        }
        return result;
    }

    public static String getChainMerchantCode(String chainMerchant) {
        String resultChain = null;
        try {
            if (chainMerchant != null || !chainMerchant.isEmpty() || chainMerchant.equals(null)) {
                if (chainMerchant.equals("NA") || chainMerchant == "NA") {
                    resultChain = "0";
                } else {
                    resultChain = chainMerchant;
                }
            } else {
                resultChain = "ERROR";
            }
        } catch (Exception e) {
        }
        return resultChain;
    }

    public static OneCheckoutDataHelper processRefundPayment(HashMap<String, String> params) {

        OneCheckoutV1QueryHelperBeanLocal helperLocal = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBean.class);
//        HashMap<String, Object> result = new HashMap<String, Object>();
        List<String> batchIds = new ArrayList<String>();
        HashMap<String, Object> responseError = new HashMap<String, Object>();
//        OneCheckOutInterfaceSettlementLocal proc = OneCheckoutServiceLocator.lookupLocal(OneCheckOutInterfaceSettlementLocal.class);
        InterfaceOneCheckoutV1BeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
        OneCheckoutDataHelper checkoutDataHelper = new OneCheckoutDataHelper();
        RefundHelper refundHelper = new RefundHelper();
        try {
            String MerchantCode = params.remove("MALLID");
            String chainMerchantcode = params.get("CHAINMERCHANT") != null ? params.get("CHAINMERCHANT") : "NA";
            String chainMerchantCodeResult = getChainMerchantCode(chainMerchantcode);
            String dokuApprovalCode = params.remove("APPROVALCODE");
            String sessionId = params.remove("SESSIONID");
            String currency = params.remove("CURRENCY");
            String bankData = params.remove("BANKDATA");
            String data1 = params.remove("DATA1");
            String data2 = params.remove("DATA2");
            String data3 = params.remove("DATA3");
            String data4 = params.remove("DATA4");
            String amount = params.get("AMOUNT");

            Merchants merchant = helperLocal.getMerchantByMallId(Integer.parseInt(MerchantCode), Integer.parseInt(chainMerchantCodeResult));
            if (merchant == null) {
                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.INVALID_MERCHANT.value());
                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.INVALID_MERCHANT.name());
                refundHelper.setMapResultRefund(responseError);
                return checkoutDataHelper;

            }

            String words = params.get("WORDS");
            //kebutuhan serviceID(start)
//            String paramServiceId = params.get(reqServiceId);
//            String sharedKey = null;
//            if (paramServiceId != null && !paramServiceId.equals("1")) {
//                OneCheckoutLogger.log("serviceid =* " + paramServiceId);
//                Criterion criterion = Restrictions.eq("id", Integer.parseInt(paramServiceId));
//
//                Services services = helperLocal.getServices(criterion);
//                if (services != null) {
//                    sharedKey = services.getSharedKey();
//                } else {
//                    sharedKey = merchant.getMerchantHashPassword();
//                }
//            } else {
//                sharedKey = merchant.getMerchantHashPassword();
//            }
            //kebutuhan serviceID(end)

            String sharedKey = merchant.getMerchantHashPassword();
//            String batchid = params.get(reqBatchId);
            String refIdMerchant = params.get("REFIDMERCHANT");
            refundHelper.setRefIdMerchant(refIdMerchant);
//            String mustWords = generateSHA1(MerchantCode + sharedKey + refIdMerchant);
            String mustWords = null;
            String logWords = null;
            if (currency == null || currency.equalsIgnoreCase("360")) {
                mustWords = amount + MerchantCode + sharedKey + refIdMerchant + sessionId;
                logWords = amount + MerchantCode + OneCheckoutBaseRules.maskingString(sharedKey, "SHAREDKEY") + refIdMerchant + sessionId;
            } else if (currency != null && !currency.equalsIgnoreCase("360")) {
                mustWords = amount + MerchantCode + sharedKey + refIdMerchant + sessionId + currency;
                logWords = amount + MerchantCode + OneCheckoutBaseRules.maskingString(sharedKey, "SHAREDKEY") + refIdMerchant + sessionId + currency;
            }

            mustWords = generateSHA1(mustWords);
            OneCheckoutLogger.log("COMPONENT WORDS STANDARD OCO = " + logWords);
            OneCheckoutLogger.log("WORDS PARAM = " + words);
            OneCheckoutLogger.log("MUST WORDS = " + mustWords);
            if (!words.equals(mustWords)) {
                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH.value());
                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH.name());
                OneCheckoutLogger.log("WORDS NOT MATCH!!!!");
                refundHelper.setMapResultRefund(responseError);
                checkoutDataHelper.setRefundHelper(refundHelper);
                return checkoutDataHelper;
            }

            String transactionId = params.get("TRANSIDMERCHANT");
//            String refIdMerchant = params.get(reqRefundId);
            refundHelper.setBankData(bankData);
            refundHelper.setCurrency(currency);
            refundHelper.setData1(data1);
            refundHelper.setData2(data2);
            refundHelper.setData3(data3);
            refundHelper.setData4(data4);
            if (transactionId != null || refIdMerchant != null) {

                Criterion qTransIdMerchant = Restrictions.eq("incTransidmerchant", transactionId);
                Criterion qIncMallId = Restrictions.eq("incMallid", Integer.parseInt(MerchantCode));
                Criterion qIncChainMerchant = Restrictions.eq("incChainmerchant", Integer.parseInt(chainMerchantCodeResult));
                Criterion qDokuApprovalCode = Restrictions.eq("dokuApprovalCode", dokuApprovalCode);
                Criterion qParentTransactions = Restrictions.isNull("parentTransactions");
                Criterion qSessionId = Restrictions.eq("incSessionid", sessionId);
                Transactions transactions = helperLocal.getTransactionByCriterion(qDokuApprovalCode, qIncChainMerchant, qIncMallId, qParentTransactions, qSessionId, qTransIdMerchant);
                if (transactions == null) {
                    responseError.put("RESPONSECODE", OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND.value());
                    responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND.name());
                    OneCheckoutLogger.log("TRANSACTION NOT FOUND!!!!");
                    refundHelper.setMapResultRefund(responseError);
                    checkoutDataHelper.setRefundHelper(refundHelper);
                    return checkoutDataHelper;
                }

//                result = proc.processDoSettlement(batchidDB, batch, transactions.getMerchantPaymentChannel());
//                OneCheckoutDataHelper checkoutDataHelper = new OneCheckoutDataHelper();
//                RefundHelper refundHelper = new RefundHelper();
                String paramRefund = params.remove("REFUNDTYPE");
                checkoutDataHelper.setMallId(Integer.parseInt(MerchantCode));
                checkoutDataHelper.setMerchant(merchant);
                refundHelper.setTransactions(transactions);
                OneCheckoutPaymentChannel pchannel = OneCheckoutPaymentChannel.findType(transactions.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());
                checkoutDataHelper.setPaymentChannel(pchannel);
                refundHelper.setPaymentChannel(transactions.getMerchantPaymentChannel().getPaymentChannel());
                refundHelper.setReason(params.get("REASON"));

                if (paramRefund != null && (paramRefund.equalsIgnoreCase("02") || paramRefund.equalsIgnoreCase("03"))) {
                    if (paramRefund.equalsIgnoreCase("03")) {
                        refundHelper.setRefundType("PD");
                        OneCheckoutLogger.log(":: PARTIAL REFUND DEBIT ::");
                        Criterion criterion3 = Restrictions.eq("parentTransactions", transactions.getTransactionsId());
                        List<Transactions> transactionses = helperLocal.getTransactionsList(criterion3);
                        if (transactionses == null) {
                            //belum pernah partial refund
                            Double amountParam = Double.valueOf(amount);
                            Double amountTransaction = transactions.getIncAmount().doubleValue();
                            refundHelper.setAmount(amount);
                            //validasi amount
//                            if (amountParam <= amountTransaction) {
//                                refundHelper.setAmount(amount);
//                            } else {
//                                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.value());
//                                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.name());
//                                OneCheckoutLogger.log("INVALID AMOUNT!!!!");
//                                refundHelper.setMapResultRefund(responseError);
//                                return checkoutDataHelper;
//                            }

                        } else {
                            //udah pernah partial refund
                            double amountTransaction = 0.0;
                            for (Transactions transactionse : transactionses) {
                                amountTransaction = amountTransaction + transactionse.getIncAmount().doubleValue();
                            }
                            Double amountTransatcionOriginal = transactions.getIncAmount().doubleValue();
                            amountTransaction = amountTransaction + Double.valueOf(amount);
                            refundHelper.setAmount(amount);
//                            if (amountTransaction <= amountTransatcionOriginal) {
//                                refundHelper.setAmount(amount);
//                            } else {
//                                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.value());
//                                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.name());
//                                OneCheckoutLogger.log("INVALID AMOUNT!!!!");
//                                refundHelper.setMapResultRefund(responseError);
//                                return checkoutDataHelper;
//                            }
                        }
                    } else if (paramRefund.equalsIgnoreCase("02")) {
                        refundHelper.setRefundType("PC");
                        OneCheckoutLogger.log(":: PARTIAL REFUND CREDIT ::");
                        Criterion criterion3 = Restrictions.eq("parentTransactions", transactions.getTransactionsId());
                        List<Transactions> transactionses = helperLocal.getTransactionsList(criterion3);
                        if (transactionses == null) {
                            //belum pernah partial refund
                            OneCheckoutLogger.log(":: has not partialRefund ::");
                            Double amountParam = Double.valueOf(amount);
                            Double amountTransaction = transactions.getIncAmount().doubleValue();
                            if (amountParam <= amountTransaction) {
                                OneCheckoutLogger.log(":: ready to partial refund ::");
                                refundHelper.setAmount(amount);
                                checkoutDataHelper.setRefundHelper(refundHelper);
                            } else {
                                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.value());
                                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.name());
                                OneCheckoutLogger.log("INVALID AMOUNT!!!!");
                                refundHelper.setMapResultRefund(responseError);
                                checkoutDataHelper.setRefundHelper(refundHelper);
                                return checkoutDataHelper;
                            }

                        } else {
                            //udah pernah partial refund
                            OneCheckoutLogger.log(":: has partialRefund ::");
                            double amountTransaction = 0.0;
                            for (Transactions transactionse : transactionses) {
                                amountTransaction = amountTransaction + transactionse.getIncAmount().doubleValue();
                            }
                            Double amountTransatcionOriginal = transactions.getIncAmount().doubleValue();
                            amountTransaction = amountTransaction + Double.valueOf(amount);
                            if (amountTransaction <= amountTransatcionOriginal) {
                                OneCheckoutLogger.log(":: ready to partial refund ::");
                                refundHelper.setAmount(amount);
                                checkoutDataHelper.setRefundHelper(refundHelper);
                            } else {
                                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.value());
                                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.AMOUNT_REFUND_NOT_VALID.name());
                                OneCheckoutLogger.log("INVALID AMOUNT!!!!");
                                refundHelper.setMapResultRefund(responseError);
                                checkoutDataHelper.setRefundHelper(refundHelper);
                                return checkoutDataHelper;
                            }
                        }

                    }

                } else if (paramRefund == null || paramRefund.equalsIgnoreCase("01")) {
                    OneCheckoutLogger.log(":: FULL REFUND ::");
                    refundHelper.setRefundType("FULL");
//                refundHelper.setRefIdMerchant(reqRefundId);
                    checkoutDataHelper.setRefundHelper(refundHelper);

                }

                checkoutDataHelper = paymentProc.ProcessRefund(checkoutDataHelper);
            } else {
                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.INVALID_PARAMETER.value());
                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.INVALID_PARAMETER.name());
                OneCheckoutLogger.log("INVALID PARAMETER!!!!");
                refundHelper.setMapResultRefund(responseError);
                checkoutDataHelper.setRefundHelper(refundHelper);
                return checkoutDataHelper;
            }

        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("Error in -processDoSettlementPayment- : " + th.getMessage());
            responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.name());
            responseError.put("RESPONSECODE", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.value());
            refundHelper.setMapResultRefund(responseError);
            return checkoutDataHelper;
        }

        return checkoutDataHelper;
    }

    public static String generateSHA1(String requestCodeId) {
        String resultHash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(requestCodeId.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            resultHash = sb.toString();
//            System.out.println("Hex format : " + sb.toString());
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("Error in -generateSHA1- : " + th.getMessage());
        }
        return resultHash;
    }

    public static OneCheckoutDataHelper parsePaymentRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {
        OneCheckoutDataHelper checkoutDataHelper = new OneCheckoutDataHelper();
        try {
            String mallId = params.remove("MALLID");
            int mId = OneCheckoutBaseRules.validateMALLID2(mallId);
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : MALLID=%d", mId);
            String chainMerchantId = params.remove("CHAINMERCHANT");
            int cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);

            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : CHAINMERCHANT=%d", cmId);
            checkoutDataHelper.setMallId(mId);
            checkoutDataHelper.setChainMerchantId(cmId);
            Merchants m = WebUtils.getValidMALLID(mId, cmId);
            checkoutDataHelper.setMerchant(m);
        } catch (Exception e) {
            e.printStackTrace();
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : ERROR2 : %s", e.getMessage());
            checkoutDataHelper.setMessage(e.getMessage());
            checkoutDataHelper.setMallId(0);
            checkoutDataHelper.setChainMerchantId(0);
            checkoutDataHelper.setMerchant(null);
            return checkoutDataHelper;
        }

        try {
            OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            //cek transaction already success or already void
            String invoice = params.get("TRANSIDMERCHANT");

            Transactions transactions = queryHelper.getTransactionByCriteria(invoice, checkoutDataHelper.getMerchant());

            if (transactions != null) {
                OneCheckoutLogger.log("TRANSACTION WITH INVOICE " + invoice + " already success or void.");
//                checkoutDataHelper.setMessage("TRANSACTION-ALREADYSUCCESS-ALREADYVOID");
                checkoutDataHelper.setMessage(OneCheckoutErrorMessage.RE_ENTER_TRANSACTION.value());
                checkoutDataHelper.setTransactions(transactions);
                return checkoutDataHelper;
            }

            OneCheckoutPaymentRequest paymentRequest = new OneCheckoutPaymentRequest();
            paymentRequest.setVAPartialAmount(params.remove("PARTIALAMOUNT"));
            paymentRequest.setVAOpenAmount(params.remove("OPENAMOUNT"));
            String ipaddress = request.getHeader("True-Client-IP");
            if (ipaddress == null) {
                ipaddress = request.getHeader("X-Forwarded-For");
            }
            if (ipaddress == null) {
                ipaddress = request.getRemoteAddr();
            }
            checkoutDataHelper.setSystemSession(ipaddress + "|" + request.getHeader("User-Agent") + "|" + request.getSession().getId());
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData SYSTEM_SESSION : %s", checkoutDataHelper.getSystemSession());
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing Mandatory Parameters");
            paymentRequest.setCUSTIP(ipaddress);
            paymentRequest.setMALLID(checkoutDataHelper.getMallId());
            paymentRequest.setCHAINMERCHANT(checkoutDataHelper.getChainMerchantId());
            paymentRequest.setAMOUNT(params.remove("AMOUNT"));
            if (params.containsKey("REGISTERAMOUNT")) {
                paymentRequest.setRegisterAMOUNT(params.remove("REGISTERAMOUNT"));
            }
            paymentRequest.setPURCHASEAMOUNT(params.remove("PURCHASEAMOUNT"));
            paymentRequest.setTRANSIDMERCHANT(params.get("TRANSIDMERCHANT"));
            paymentRequest.setREQUESTDATETIME(params.remove("REQUESTDATETIME"));
            paymentRequest.setCURRENCY(params.remove("CURRENCY"));
            paymentRequest.setPURCHASECURRENCY(params.remove("PURCHASECURRENCY"));
            paymentRequest.setWORDS(params.remove("WORDS"));
            paymentRequest.setBILLINGDESCRIPTION(params.remove("BILLINGDESCRIPTION"));
            paymentRequest.setPAYMENTTYPE(params.remove("PAYMENTTYPE"));

//          paymentRequest.setUSERNAME(request.getUserPrincipal().getName() != null ? request.getUserPrincipal().getName() : "");
//          paymentRequest.setPAYREASON(params.remove("PAYREASON"));
            Currency currency = queryHelper.getCurrencyByCode(paymentRequest.getCURRENCY());
            if (currency == null) {
                return checkoutDataHelper;
            }
            currency = queryHelper.getCurrencyByCode(paymentRequest.getPURCHASECURRENCY());
            if (currency == null) {
                return checkoutDataHelper;
            }

            Merchants m = checkoutDataHelper.getMerchant();
            if (m.getMerchantFxrateFeature() != null && !m.getMerchantFxrateFeature().isEmpty()) {
                Rates rate = queryHelper.getRates(m.getMerchantFxrateFeature(), paymentRequest.getPURCHASECURRENCY());
                if (rate == null) {
                    OneCheckoutLogger.log("FAILED GET ACTIVE RATES, WITH CRITERIA BELOW...");
                    OneCheckoutLogger.log("status           eq [%s]", OneCheckoutRateStatus.NEW.code());
                    OneCheckoutLogger.log("sellCurrencyNum  eq [%s]", m.getMerchantFxrateFeature() + "");
                    OneCheckoutLogger.log("buyCurrencyNum   eq [%s]", paymentRequest.getPURCHASECURRENCY() + "");
                    OneCheckoutLogger.log("expiryDate       gt [%s]", new Date());//already expired or not..            
                    return checkoutDataHelper;
                }
                paymentRequest.setRATE(rate);
                double finalRate = rate.getFinalRate().doubleValue();
                double realAmount = finalRate * paymentRequest.getPURCHASEAMOUNT();
                //::: rounding up amount..                
                double amountRoundUp = Math.ceil(realAmount);
                OneCheckoutLogger.log("realAmount       [%s]", realAmount);
                OneCheckoutLogger.log("amountRoundUp    [%s]", amountRoundUp);
                paymentRequest.setAMOUNT(amountRoundUp);
                paymentRequest.setCURRENCY(m.getMerchantFxrateFeature());
                paymentRequest.setCONVERTEDAMOUNT(realAmount);
                checkoutDataHelper.setRates(rate);
                paymentRequest.setBASKET(params.get("BASKET"), paymentRequest.getAMOUNT(), paymentRequest.getTRANSIDMERCHANT());
                params.put("BASKET", paymentRequest.getBASKET());
            }

            paymentRequest.setSESSIONID(params.remove("SESSIONID"));
            paymentRequest.setEMAIL(params.remove("EMAIL"));
            paymentRequest.setNAME(params.remove("NAME"));
            paymentRequest.setADDITIONALINFO(params.remove("ADDITIONALDATA"));
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : paymentRequest.getADDITIONALINFO() : %s", paymentRequest.getADDITIONALINFO());
            if (checkoutDataHelper.getMerchant() == null) {
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : onecheckout.getMerchant()==null");
            }

            //PAYMENTCHANNEL
            String paymentChannel = params.remove("PAYMENTCHANNEL");
            OneCheckoutPaymentChannel oneCheckoutPaymentChannel = OneCheckoutBaseRules.validatePaymentChannel(paymentChannel);

            Character merchantCategory = checkoutDataHelper.getMerchant().getMerchantCategory();
            OneCheckoutMerchantCategory mc = null;
            if (merchantCategory != null) {
                try {
                    mc = OneCheckoutMerchantCategory.findType(merchantCategory);
                    OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Merchant Category : %s", mc.name());
                    checkoutDataHelper.setMerchantCategory(mc);
                } catch (Throwable t) {
                    OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : CANNOT FIND MERCHANT CATEGORY");
                }
            } else {
                mc = null;
            }

            if (oneCheckoutPaymentChannel != OneCheckoutPaymentChannel.Recur) {
                if (mc != null && mc == OneCheckoutMerchantCategory.NONAIRLINE) {
                    OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing NON AIRLINES Parameters");
                    String basket = params.remove("BASKET");
                    System.out.println("BASKET : " + basket);
                    paymentRequest.setBASKET(basket, paymentRequest.getAMOUNT(), paymentRequest.getTRANSIDMERCHANT());
                    paymentRequest.setSHIPPING_ADDRESS(params.remove("SHIPPING_ADDRESS"));
                    paymentRequest.setSHIPPING_CITY(params.remove("SHIPPING_CITY"));
                    paymentRequest.setSHIPPING_STATE(params.remove("SHIPPING_STATE"));
                    paymentRequest.setSHIPPING_COUNTRY(params.remove("SHIPPING_COUNTRY"));
                    paymentRequest.setSHIPPING_ZIPCODE(params.remove("SHIPPING_ZIPCODE"));

                    //KlikPayBCA
                    if (params.containsKey("ACQUIRERID")) {
                        if (!params.get("ACQUIRERID").equals("")) {
                            OneCheckoutLogger.log("============ KLIKPAYBCA ADD ============");
                            HashMap<String, String> paramKlikPayBCA = new HashMap<String, String>();
                            paramKlikPayBCA.put("ACQUIRERID", params.remove("ACQUIRERID"));
                            paramKlikPayBCA.put("INVOICENUMBER", params.remove("INVOICENUMBER"));
                            paramKlikPayBCA.put("DESCRIPTION", params.remove("DESCRIPTION"));
                            paymentRequest.setADDITIONALINFO(paramKlikPayBCA.toString());
                            OneCheckoutLogger.log("paramKlikPayBCA  : " + paramKlikPayBCA.toString());
                        }
                    }
                } else if (mc != null && mc == OneCheckoutMerchantCategory.AIRLINE) {
                    OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing AIRLINES Parameters");
                    paymentRequest.setFLIGHT(params.remove("FLIGHT"));
                    paymentRequest.setFLIGHTTYPE(params.remove("FLIGHTTYPE"));
                    paymentRequest.setBOOKINGCODE(params.remove("BOOKINGCODE"));

                    paymentRequest.setVAT(params.remove("VAT"));
                    paymentRequest.setINSURANCE(params.remove("INSURANCE"));
                    paymentRequest.setFUELSURCHARGE(params.remove("FUELSURCHARGE"));

                    if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.OneWay) {

                        String[] routes = new String[1];
                        String[] fdates = new String[1];
                        String[] ftimes = new String[1];
                        String[] fnumbers = new String[1];

                        routes[0] = params.remove("ROUTE");
                        fdates[0] = params.remove("FLIGHTDATE");
                        ftimes[0] = params.remove("FLIGHTTIME");
                        fnumbers[0] = params.remove("FLIGHTNUMBER");

                        paymentRequest.setROUTE(routes);
                        paymentRequest.setFLIGHTDATE(fdates);
                        paymentRequest.setFLIGHTTIME(ftimes);
                        paymentRequest.setFLIGHTNUMBER(fnumbers);
                    } else {
                        paymentRequest.setROUTE(request.getParameterValues("ROUTE"));
                        paymentRequest.setFLIGHTDATE(request.getParameterValues("FLIGHTDATE"));
                        paymentRequest.setFLIGHTTIME(request.getParameterValues("FLIGHTTIME"));
                        paymentRequest.setFLIGHTNUMBER(request.getParameterValues("FLIGHTNUMBER"));
                    }
                    paymentRequest.setFLIGHTDATETIME();
                    paymentRequest.setPASSENGER_NAME(request.getParameterValues("PASSENGER_NAME"));
                    paymentRequest.setPASSENGER_TYPE(request.getParameterValues("PASSENGER_TYPE"));
                    paymentRequest.setFFNUMBER(params.remove("FFNUMBER"));
                    paymentRequest.setTHIRDPARTY_STATUS(params.remove("THIRDPARTY_STATUS").charAt(0));
                    StringBuilder basket = new StringBuilder();
                    basket.append(paymentRequest.getBOOKINGCODE());
                    String amount = OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT());
                    basket.append(",").append(amount).append(",").append("1").append(",").append(amount);
                    paymentRequest.setBASKET(basket.toString(), paymentRequest.getAMOUNT(), paymentRequest.getBOOKINGCODE());
                }

            }
            if ((paymentChannel == null || paymentChannel.isEmpty()) && oneCheckoutPaymentChannel == null) {
                paymentRequest = autoGenerate(params, paymentRequest, oneCheckoutPaymentChannel);
                // CIP without channel selected                
                // multi payment page display
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Undefined Payment Channel, give multi payment page");
                checkoutDataHelper.setPaymentRequest(paymentRequest);
//                if (!WebUtils.doExecutePostParsePaymentPlugins(checkoutDataHelper)) {
//                    OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : end - valid execute plugin");
//                    return checkoutDataHelper;
//                }
                checkoutDataHelper.setMessage("VALID");
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : end");
                return checkoutDataHelper;
            }

            /*Adding for tokenization TODO pindahkan ketempat yang seharusnya berada*/
            if (oneCheckoutPaymentChannel == OneCheckoutPaymentChannel.Recur || oneCheckoutPaymentChannel == OneCheckoutPaymentChannel.Tokenization) {
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Tokenization Customerid");
                paymentRequest.setCUSTOMERID(params.remove("CUSTOMERID"));
                if (params.containsKey("CUSTOMERTYPE")) {
                    paymentRequest.setCUSTOMERTYPE(params.remove("CUSTOMERTYPE"));
                } else {
                    paymentRequest.setCUSTOMERTYPE("C");
                }

                String tokenId = params.remove("TOKENID");
                if (tokenId != null && !tokenId.isEmpty()) {
                    paymentRequest.setTOKENID(tokenId);
                } else {
                    paymentRequest.setTOKENID("");
                }
                if (oneCheckoutPaymentChannel == OneCheckoutPaymentChannel.Recur) {
                    String billNumber = params.get("BILLNUMBER") != null ? params.remove("BILLNUMBER").trim() : "";
                    String billDetail = params.get("BILLDETAIL") != null ? params.remove("BILLDETAIL").trim() : "";
                    String billType = params.get("BILLTYPE") != null ? params.remove("BILLTYPE").trim() : "";
                    String startDate = params.get("STARTDATE") != null ? params.remove("STARTDATE").trim() : "";
                    String endDate = params.get("ENDDATE") != null ? params.remove("ENDDATE").trim() : "";
                    String executeType = params.get("EXECUTETYPE") != null ? params.remove("EXECUTETYPE").trim() : "";
                    String executeDate = params.get("EXECUTEDATE") != null ? params.remove("EXECUTEDATE").trim() : "";
                    String executeMonth = params.get("EXECUTEMONTH") != null ? params.remove("EXECUTEMONTH").trim() : "";

                    if (!billNumber.equals("")) {
                        OneCheckoutLogger.log("============ RECUR DATA ============");
                        paymentRequest.setBILLNUMBER(billNumber);
                        if (billDetail.equals("") && billType.equals("") && startDate.equals("") && endDate.equals("") && executeType.equals("") && executeDate.equals("") && executeMonth.equals("")) {
                            paymentRequest.setUPDATEBILLSTATUS(true);
                        } else {
                            paymentRequest.setBILLDETAIL(billDetail);
                            paymentRequest.setBILLTYPE(billType);
                            paymentRequest.setSTARTDATE(startDate);
                            paymentRequest.setENDDATE(endDate);
                            paymentRequest.setEXECUTETYPE(executeType);
                            paymentRequest.setEXECUTEDATE(executeDate);
                            paymentRequest.setEXECUTEMONTH(executeMonth);
                        }
                    }
                }
            }

            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Payment Channel : %s", oneCheckoutPaymentChannel.name());
            checkoutDataHelper.setPaymentChannel(oneCheckoutPaymentChannel);
            PaymentChannel dbChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutPaymentChannel);
//            System.out.println("dbChannel =* " + dbChannel.getPaymentChannelId());
//            System.out.println("merchant =* " + checkoutDataHelper.getMerchant().getMerchantIdx() + " " + checkoutDataHelper.getMerchant().getMerchantName());
            MerchantPaymentChannel mpc = queryHelper.getMerchantPaymentChannel(checkoutDataHelper.getMerchant(), dbChannel);
//            OneCheckoutLogger.log("MPC =* " + mpc.getMerchantPaymentChannelId());
            OneCheckoutMethod method = OneCheckoutMethod.findType(mpc.getMerchantPaymentChannelMethodCipmip());
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Merchant using %s method", method.name());
            checkoutDataHelper.setCIPMIP(method);
            checkoutDataHelper.setPaymentChannelCode(mpc.getPaymentChannelCode());
            // CIP with channel selected
            if (method == OneCheckoutMethod.CIP) {
                paymentRequest = autoGenerate(params, paymentRequest, oneCheckoutPaymentChannel);
                checkoutDataHelper.setPaymentRequest(paymentRequest);
//                if (!WebUtils.doExecutePostParsePaymentPlugins(checkoutDataHelper)) {
//                    OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : end - valid execute plugin");
//                    return checkoutDataHelper;
//                }                               
                checkoutDataHelper.setMessage("VALID");
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : end");
                return checkoutDataHelper;
            }

            // MIP
            if (checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.CreditCard || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BSP || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MOTO) {

                /*
                 *
                 *
                 // Payment Account Information
                 private String CARDNUMBER;
                 private String EXPIRYDATE;
                 private String CVV2;
                 private String INSTALLMENT_ACQUIRER;
                 private String TENOR;
                 private String PROMOID;
                 private String CHALLENGE_CODE_1;
                 private String CHALLENGE_CODE_2;
                 private String CHALLENGE_CODE_3;
                 private String RESPONSE_TOKEN;
                 private String USERIDKLIKBCA;
                 private String DOKUPAYID;
                 *
                 // CH Information
                 private String CC_NAME;
                 private String ADDRESS;
                 private String CITY;
                 private String STATE;
                 private String COUNTRY;
                 private String ZIPCODE;
                 private String HOMEPHONE;
                 private String MOBILEPHONE;
                 private String WORKPHONE;
                 private String BIRTHDATE;

                 */
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing Visa/Master Parameters");

                paymentRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                paymentRequest.setEXPIRYDATE(params.remove("EXPIRYDATE"));
                if (checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MOTO) {
                    paymentRequest.setCVV2withEmpty();
                } else {
                    paymentRequest.setCVV2(params.remove("CVV2"));
                }
                paymentRequest.setINSTALLMENT_ACQUIRER(params.remove("INSTALLMENT_ACQUIRER"));
                paymentRequest.setTENOR(params.remove("TENOR"));
                paymentRequest.setPROMOID(params.remove("PROMOID"));

                paymentRequest.setCC_NAME(params.remove("CC_NAME"));

                paymentRequest.setADDRESS(params.remove("ADDRESS"));

                paymentRequest.setCITY(params.remove("CITY"));

                paymentRequest.setSTATE(params.remove("STATE"));
                paymentRequest.setCOUNTRY(params.remove("COUNTRY"));
                paymentRequest.setZIPCODE(params.remove("ZIPCODE"));
                paymentRequest.setHOMEPHONE(params.remove("HOMEPHONE"));
                paymentRequest.setMOBILEPHONE(params.remove("MOBILEPHONE"));
                paymentRequest.setWORKPHONE(params.remove("WORKPHONE"));
                paymentRequest.setBIRTHDATE(params.remove("BIRTHDATE"));

                paymentRequest.setECI(params.remove("VERESSTATUS"));
                OneCheckoutLogger.log("paymentRequest.getVERESSTATUS[" + paymentRequest.getVERESSTATUS() + "]");
                paymentRequest.setECI(params.remove("ECI"));
                OneCheckoutLogger.log("paymentRequest.getECI[" + paymentRequest.getECI() + "]");
                if (paymentRequest.getECI().equalsIgnoreCase("01") || paymentRequest.getECI().equalsIgnoreCase("02") || paymentRequest.getECI().equalsIgnoreCase("05") || paymentRequest.getECI().equalsIgnoreCase("06")) {

                    paymentRequest.setXID(params.remove("XID"));
                    OneCheckoutLogger.log("paymentRequest.getXID[" + paymentRequest.getXID() + "]");
                    paymentRequest.setAUTHRESRESPONSECODE(params.remove("AUTHRESRESPONSECODE"));
                    OneCheckoutLogger.log("paymentRequest.getAUTHRESRESPONSECODE[" + paymentRequest.getAUTHRESRESPONSECODE() + "]");
                    paymentRequest.setAUTHRESSTATUS(params.remove("AUTHRESSTATUS"));
                    OneCheckoutLogger.log("paymentRequest.getAUTHRESSTATUS[" + paymentRequest.getAUTHRESSTATUS() + "]");
                    //      paymentRequest.setAUTHRESVENDORCODE(params.remove("AuthResVendorCode"));
                    paymentRequest.setCAVVALGORITHM(params.remove("CAVVALGORITHM"));
                    OneCheckoutLogger.log("paymentRequest.getCAVVALGORITHM[" + paymentRequest.getCAVVALGORITHM() + "]");
                    paymentRequest.setCAVV(params.remove("CAVV"));
                    OneCheckoutLogger.log("paymentRequest.getCAVV[" + paymentRequest.getCAVV() + "]");

                }

            } else if (checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DirectDebitMandiri) {

                /*
                 *
                 *
                 // Payment Account Information
                 private String CARDNUMBER;
                 private String CHALLENGE_CODE_1;
                 private String CHALLENGE_CODE_2;
                 private String CHALLENGE_CODE_3;
                 private String RESPONSE_TOKEN;

                 */
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing ClickPay Mandiri Parameters");
                paymentRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                paymentRequest.setCHALLENGE_CODE_1(params.remove("CHALLENGE_CODE_1"));
                paymentRequest.setCHALLENGE_CODE_2(params.remove("CHALLENGE_CODE_2"));
                paymentRequest.setCHALLENGE_CODE_3(params.remove("CHALLENGE_CODE_3"));
                paymentRequest.setRESPONSE_TOKEN(params.remove("RESPONSE_TOKEN"));

            } else if (checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.KlikBCA) {

                /*
                 Payment Account Information
                 private String USERIDKLIKBCA;
                 */
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing KlikBCA Parameters");
                paymentRequest.setUSERIDKLIKBCA(params.remove("USERIDKLIKBCA"));

            } else if (checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {
                /*
                 // Payment Account Information
                 private String DOKUPAYID;
                 */
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing Dokupay Parameters");
                paymentRequest.setDOKUPAYID(params.remove("DOKUPAYID"));

            } else if (checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart
                    || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite
                    || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull
                    || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite || checkoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing VA Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));

                if (paymentRequest.getPAYCODE() == null && paymentRequest.getPAYCODE().equalsIgnoreCase("00")) {

                    OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
                    String paycode = queryBeans.generatePaymentCodeByMerchant(checkoutDataHelper.getMerchant(), checkoutDataHelper.getPaymentChannel(), checkoutDataHelper.getPaymentRequest().getTRANSIDMERCHANT(), checkoutDataHelper.getPaymentRequest().getWORDS(), paymentRequest.getVAOpenAmount());
                    paymentRequest.setPAYCODE(paycode);
                }
            } else if (checkoutDataHelper.getPaymentChannel() == oneCheckoutPaymentChannel.KlikPayBCACard) {
                OneCheckoutLogger.log("set Tenor and planID");
                paymentRequest.setTENOR(params.remove("TENOR"));
                paymentRequest.setPROMOID(params.remove("PLANID"));
                OneCheckoutLogger.log("Tenor =* " + paymentRequest.getTENOR() + " Plan=* " + paymentRequest.getPROMOID());
            }
            checkoutDataHelper.setPaymentRequest(paymentRequest);
//            if (!WebUtils.doExecutePostParsePaymentPlugins(checkoutDataHelper)) {
//                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : end - valid execute plugin");                
//                return checkoutDataHelper;
//            }            
            checkoutDataHelper.setMessage("VALID");
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : end");
            return checkoutDataHelper;

        } catch (Exception e) {
            e.printStackTrace();
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : ERROR2 : %s", e.getMessage());
            checkoutDataHelper.setMessage(e.getMessage());
            return checkoutDataHelper;

        }
    }

    public static String convertAmountBasket(OneCheckoutDataHelper helper, String basket) {
        //ITEM 1,50.00,1,50.00;ITEM 1,50.00,1,50.00;
        try {
            BigDecimal finalRate = helper.getRates().getFinalRate();
            BigDecimal priceItemConvert, totalPriceItemConvert;
            String newBasket = "";
            String[] datas = basket.split(";");
            String[] items;

            if (datas.length == 0) {
                OneCheckoutLogger.log("ConvertAmountBean.convertAmountBasket : datas basket null [%s]", basket);// helper.getPaymentRequest().getBASKET());
                return null;
            }

            for (String data : datas) {
                if (data.equals("")) {
                    continue;
                }
                items = data.split(",");
                priceItemConvert = (new BigDecimal(items[1])).multiply(finalRate);
                totalPriceItemConvert = (new BigDecimal(items[3])).multiply(finalRate);
                newBasket += items[0] + "," + priceItemConvert + "," + items[2] + "," + totalPriceItemConvert + ";";
            }
            OneCheckoutLogger.log("ConvertAmountBean.convertAmountBasket : newbasket [%s]", newBasket);
            //  helper.getPaymentRequest().setBASKET(newBasket, helper.getPaymentRequest().getAMOUNT());
            return newBasket;
        } catch (Throwable ex) {
            //OneCheckoutLogger.log("ConvertAmountBean.convertAmountBasket : UE when convert basket [%s]", helper.getPaymentRequest().getBASKET());
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * checkoutDataHelper.setMessage(e.getMessage());
     * --------------------------------------------------------------------------------
     * :: ADD PLUGIN AFTER PARSE PAYMENT REQUEST FROM CLIENT..
     * --------------------------------------------------------------------------------
     */
    // : : : do execute post after auth plugin (verify trx, invoice validator, etc)...
    public static <T extends OneCheckoutDataHelper> boolean doExecutePostParsePaymentPlugins1(T helper) {
        try {
            OneCheckoutLogger.log(".::-- WebUtils.doExecutePostParsePaymentPlugins --::.");
            String plugins = helper.getMerchant().getMerchantPluginPostParsePayment();
            if (plugins == null || plugins.equals("")) {
                OneCheckoutLogger.log("WebUtils.doExecutePostParsePaymentPlugins : No plugins found");
                return true;
            }

            String[] lstPlugins = plugins.split(";");
            InitialContext ic = new InitialContext();
            InterfacePluginPostParsePayment intfce;
            for (String strPlugin : lstPlugins) {
                OneCheckoutLogger.log(": : startplugin[" + strPlugin + "]");
                try {
                    intfce = (InterfacePluginPostParsePayment) ic.lookup(strPlugin);
                    if (intfce.afterAuth(helper)) {
                        OneCheckoutLogger.log(": : post parse plugin %s returned true", strPlugin);
                    } else {
                        OneCheckoutLogger.log(": : post parse plugin %s returned false", strPlugin);
                        return false;
                    }
                } catch (Throwable e) {
                    OneCheckoutLogger.log(": : EXCEPTION WHILE PROCESSING WebUtils.doExecutePostParsePaymentPlugins : " + strPlugin);
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        } catch (Throwable th) {
            th.printStackTrace();
        }
        OneCheckoutLogger.log("FAILED EXECUTE POST AUTH PPLUGINS");
        return false;
    }

    public static OneCheckoutDataHelper parseVerifyDOKURequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session, OneCheckoutPaymentChannel pChannel) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            onecheckout.setPaymentChannel(pChannel);

            OneCheckoutDOKUVerifyData verifyRequest = new OneCheckoutDOKUVerifyData();

            if (pChannel == OneCheckoutPaymentChannel.CreditCard || pChannel == OneCheckoutPaymentChannel.BNIDebitOnline) {

                verifyRequest.setCURRENCY(params.remove("CURRENCY"));
                verifyRequest.setSESSIONID(params.remove("SESSIONID"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
                verifyRequest.setWORDS(params.remove("WORDS"));

            } else if (pChannel == OneCheckoutPaymentChannel.DirectDebitMandiri) {

                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
                verifyRequest.setCURRENCY(params.remove("CURRENCY"));
                verifyRequest.setSESSIONID(params.remove("SESSIONID"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                verifyRequest.setWORDS(params.remove("WORDS"));

            } else if (pChannel == OneCheckoutPaymentChannel.KlikBCA) {

                verifyRequest.setKLIKBCA_USERID(params.remove("userId").toUpperCase());
                verifyRequest.setKLIKBCA_MERCHANTCODE(params.remove("merchantCode"));

                OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

                Merchants merchants = queryHelper.getMerchantBy(verifyRequest.getKlikBCA_USERID());
                onecheckout.setMerchant(merchants);
            } else if (pChannel == OneCheckoutPaymentChannel.DOKUPAY) {

                verifyRequest.setDOKUPAYID(params.remove("DOKUPAYID"));

                verifyRequest.setCURRENCY(params.remove("CURRENCY"));
                verifyRequest.setSESSIONID(params.remove("SESSIONID"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
                verifyRequest.setWORDS(params.remove("WORDS"));

            } else if (pChannel == OneCheckoutPaymentChannel.BRIPay) {

                verifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                verifyRequest.setCURRENCY(params.remove("CURRENCY"));
                verifyRequest.setWORDS(params.remove("WORDS"));
                verifyRequest.setSESSIONID(params.remove("SESSIONID"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
            } else if (pChannel == OneCheckoutPaymentChannel.PermataVALite) {

                verifyRequest.setVAID(params.remove("VAID"));
                verifyRequest.setMALLID(params.remove("MERCHANTCODE"));
                verifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("PERMATA");
                verifyRequest.setSTEP(params.remove("STEP"));
                verifyRequest.setWORDS(params.remove("WORDS"));
            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVALite) {
                verifyRequest.setVAID(params.remove("VAID"));
                verifyRequest.setMALLID(params.remove("MERCHANTCODE"));
                verifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("SINARMAS");
                verifyRequest.setSTEP(params.remove("STEP"));
                verifyRequest.setWORDS(params.remove("WORDS"));
            } else if (pChannel == OneCheckoutPaymentChannel.PTPOS) {

                verifyRequest.setVAID(params.remove("VAID"));
                verifyRequest.setMALLID(params.remove("MERCHANTCODE"));
                verifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("PTPOS");
                verifyRequest.setSTEP(params.remove("STEP"));
                verifyRequest.setWORDS(params.remove("WORDS"));

            } else if (pChannel == OneCheckoutPaymentChannel.PermataVAFull) {
                String vaId = params.remove("VANUMBER");
                verifyRequest.setVANUMBER(vaId);
                verifyRequest.setPAYCODE(vaId);
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setTRACENO(params.remove("TRACENO"));
                verifyRequest.setBANK(params.remove("PERMATA"));
            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVAFull) {
                String vaId = params.remove("VAID");
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setVANUMBER(vaId);
                verifyRequest.setPAYCODE(vaId);
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setTRACENO(params.remove("TRACENO"));
                verifyRequest.setBANK("SINARMAS");
            } else if (pChannel == OneCheckoutPaymentChannel.Alfamart) {

                verifyRequest.setVAID(params.remove("VAID"));
                verifyRequest.setMALLID(params.remove("MERCHANTCODE"));
                verifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("Alfamart");
                verifyRequest.setSTEP(params.remove("STEP"));
                verifyRequest.setWORDS(params.remove("WORDS"));
            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOALite) {

                verifyRequest.setVAID(params.remove("VAID"));
                verifyRequest.setMALLID(params.remove("MERCHANTCODE"));
                verifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("MANDIRI");
                verifyRequest.setSTEP(params.remove("STEP"));
                verifyRequest.setWORDS(params.remove("WORDS"));
            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOAFull) {
                String vaId = params.remove("USERACCOUNT");
                verifyRequest.setUSERACCOUNT(vaId);
                verifyRequest.setPAYCODE(vaId);
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("MANDIRI");
                verifyRequest.setSTEP(params.remove("STEP"));
            } else if (pChannel == OneCheckoutPaymentChannel.MandiriVALite) {

                verifyRequest.setVAID(params.remove("VAID"));
                verifyRequest.setMALLID(params.remove("MERCHANTCODE"));
                verifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("MANDIRI");
                verifyRequest.setSTEP(params.remove("STEP"));
                verifyRequest.setWORDS(params.remove("WORDS"));
            } else if (pChannel == OneCheckoutPaymentChannel.MandiriVAFull) {
            } else if (pChannel == OneCheckoutPaymentChannel.PayPal) {
                verifyRequest.setCURRENCY(params.remove("CURRENCY"));
                verifyRequest.setSESSIONID(params.remove("SESSIONID"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
                verifyRequest.setWORDS(params.remove("WORDS"));

            } else if (pChannel == OneCheckoutPaymentChannel.BSP) {
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setCHAINMERCHANT(params.remove("CHAINMALLID"));
                verifyRequest.setTYPE(params.remove("TYPE"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));

                OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

                Currency currency = queryHelper.getCurrencyByAlphaCode(params.remove("CURRENCY"));

                if (currency == null) {
                    return onecheckout;
                }
                //   currency = queryHelper.getCurrencyByCode(paymentRequest.getPURCHASECURRENCY());

                verifyRequest.setCURRENCY(currency.getCurrencyCode());
                //OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

                //Currency currency = queryHelper.getCurrencyByAlphaCode(params.remove("CURRENCY"));
                //verifyRequest.setCURRENCY(currency != null ? currency.getCurrencyCode() : params.remove("CURRENCY"));
                //verifyRequest.setCURRENCY(params.remove("CURRENCY"));
                verifyRequest.setSESSIONID(params.remove("SESSIONID"));
                verifyRequest.setWORDS(params.remove("WORDS"));
            } else if (pChannel == OneCheckoutPaymentChannel.Tokenization) {
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setCHAINMERCHANT(params.remove("CHAINMALLID"));
                verifyRequest.setTYPE(params.remove("TYPE"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));

            } else if (pChannel == OneCheckoutPaymentChannel.KlikPayBCACard || pChannel == OneCheckoutPaymentChannel.KlikPayBCADebit) {
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setCHAINMERCHANT(params.remove("CHAINMALLID"));
                verifyRequest.setTYPE(params.remove("TYPE"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
            } else if (pChannel == OneCheckoutPaymentChannel.CIMBClicks) {
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setCHAINMERCHANT(params.remove("CHAINMALLID"));
                verifyRequest.setTYPE(params.remove("TYPE"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
            } else if (pChannel == OneCheckoutPaymentChannel.Muamalat) {
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setCHAINMERCHANT(params.remove("CHAINMALLID"));
                verifyRequest.setTYPE(params.remove("TYPE"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
            } else if (pChannel == OneCheckoutPaymentChannel.Danamon) {
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setCHAINMERCHANT(params.remove("CHAINMALLID"));
                verifyRequest.setTYPE(params.remove("TYPE"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
            } else if (pChannel == OneCheckoutPaymentChannel.DompetKu) {
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setCHAINMERCHANT(params.remove("CHAINMALLID"));
                verifyRequest.setTYPE(params.remove("TYPE"));
                verifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                verifyRequest.setAMOUNT(params.remove("AMOUNT"));
            } else if (pChannel == OneCheckoutPaymentChannel.BCAVAFull) {

                verifyRequest.setVAID(params.remove("VAID"));
                verifyRequest.setMALLID(params.remove("MALLID"));
                verifyRequest.setVANUMBER(params.remove("VANUMBER"));
                verifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                verifyRequest.setPAYCODE(verifyRequest.getUSERACCOUNT());
                verifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                verifyRequest.setBANK("BCA");
                verifyRequest.setSTEP(params.remove("STEP"));
                verifyRequest.setWORDS(params.remove("WORDS"));
            }

            onecheckout.setMessage("VALID");
            onecheckout.setVerifyRequest(verifyRequest);
            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setVerifyRequest(null);
            onecheckout.setMessage(e.getMessage());
            return onecheckout;

        }

    }

    public static OneCheckoutDataHelper parseNotifyDOKURequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session, OneCheckoutPaymentChannel pChannel) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            onecheckout.setPaymentChannel(pChannel);

            OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();

            if (pChannel == OneCheckoutPaymentChannel.CreditCard || pChannel == OneCheckoutPaymentChannel.BNIDebitOnline) {

                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setBANK(params.remove("BANK"));
                if (notifyRequest.getBANK() == null || notifyRequest.getBANK().equalsIgnoreCase("null")) {
                    notifyRequest.setBANK("");
                }
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                notifyRequest.setDFSStatus(params.remove("VERIFYSTATUS"));
                notifyRequest.setDFSScore(params.remove("VERIFYSCORE"));
                notifyRequest.setDFSIId(params.remove("VERIFYID"));

                if (notifyRequest.getRESULT() != null && notifyRequest.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
                } else if (notifyRequest.getDFSStatus() == OneCheckoutDFSStatus.MEDIUM_RISK) {
                    notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                    notifyRequest.setDFSScore("-1");
                }

            } else if (pChannel == OneCheckoutPaymentChannel.DirectDebitMandiri) {

                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODEDETAIL"));//RESPONSECODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setBANK("MANDIRI");//params.remove("BANK"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                //   notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.KlikBCA) {

                notifyRequest.setTRANSIDMERCHANT(params.remove("transactionNo"));
                notifyRequest.setRESPONSECODE(params.remove("status"));
                if (notifyRequest.getRESPONSECODE() != null && notifyRequest.getRESPONSECODE().equalsIgnoreCase("00")) {
                    notifyRequest.setRESULT("SUCCESS");
                } else {
                    notifyRequest.setRESULT("FAILED");
                }

                notifyRequest.setKLIKBCA_USERID(params.remove("userId").toUpperCase());
                notifyRequest.setCARDNUMBER(notifyRequest.getCARDNUMBER());
                notifyRequest.setBANK("BCA");
                notifyRequest.setAPPROVALCODE("");
                notifyRequest.setAMOUNT(params.remove("amount"));
                notifyRequest.setRESULTMSG(params.remove("additionalInfo"));
                notifyRequest.setKLIKBCA_MERCHANTCODE(params.remove("merchantCode"));
                notifyRequest.setREQUESPAYMENTDATETIME(request.getParameter("transactionDate"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.DOKUPAY) {

                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                //    notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setBANK(params.remove("BANK"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                //    notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                //          notifyRequest.setDFSStatus(params.remove("VERIFYSTATUS"));
                //          notifyRequest.setDFSScore(params.remove("VERIFYSCORE"));
                //          notifyRequest.setDFSIId(params.remove("VERIFYID"));
                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.BRIPay) {

                notifyRequest.setBANK("BRI");
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.PermataVALite) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("PERMATA");

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");
            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVALite) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("SINARMAS");
                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");
            } else if (pChannel == OneCheckoutPaymentChannel.PTPOS) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("PTPOS");

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.Alfamart) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                //notifyRequest.setUSERACCOUNT(params.remove("VANUMBER"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("Alfamart");

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.PermataVAFull) {

                notifyRequest.setPERMATA_VANUMBER(params.remove("VANUMBER"));
                notifyRequest.setPERMATA_MALLID(params.remove("MALLID"));
                notifyRequest.setPERMATA_TRACENO(params.remove("TRACENO"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE").trim());
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setREQUESPAYMENTDATETIME(params.remove("PAYMENTDATETIME"));
                notifyRequest.setPERMATA_STEP(params.remove("STEP"));
                notifyRequest.setBANK("PERMATA");

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

                //For Reconcile Only
                /*if(notifyRequest.getPERMATA_STEP().equalsIgnoreCase("SETTLEMENT")) {
                 notifyRequest.setPAYCODE("VAID");
                 }*/
            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVAFull) {

                /*
                 16:24:43,678 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : BANK , VALUE : [SINARMAS]
                 16:24:43,678 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : TRANSIDMERCHANT , VALUE : [20141201016225551747]
                 16:24:43,678 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : STEP , VALUE : [PAYMENT]
                 16:24:43,678 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : VAID , VALUE : [8456123456789]
                 16:24:43,679 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : COMPANYCODE , VALUE : [8456]
                 16:24:43,679 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : TRANSACTIONID , VALUE : [99]
                 16:24:43,679 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : USERACCOUNT , VALUE : [8456123456789]
                 16:24:43,679 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : AMOUNT , VALUE : [360000.00]
                 16:24:43,679 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : MALLID , VALUE : [8456]
                 16:24:43,679 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : WORDS , VALUE : [6c58b8636f75b01c868be2e863229f51152956b9]
                 16:24:43,679 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : TRACENO , VALUE : [99]
                 16:24:43,680 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : SESSIONID , VALUE : [CE3F8FC3F9C7756E3291091B924944AA.node1]
                 16:24:43,680 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : BASKET , VALUE : [null]
                 16:24:43,680 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : MOBILEPHONE , VALUE : [null]
                 16:24:43,680 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : EMAIL , VALUE : [null]
                 16:24:43,680 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : ADDITIONALDATA , VALUE : [null]
                 16:24:43,680 INFO  [STDOUT] [] WebUtils.copyParams PARAMETER : ADDRESS , VALUE : [null]
                 */
                notifyRequest.setSINARMAS_VANUMBER(params.remove("VAID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setSINARMAS_MALLID(params.remove("MALLID"));
                notifyRequest.setSINARMAS_TRACENO(params.remove("TRACENO"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setREQUESPAYMENTDATETIME(params.remove("PAYMENTDATETIME"));
                notifyRequest.setSINARMAS_STEP(params.remove("STEP"));
                notifyRequest.setBANK("SINARMAS");
                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOALite) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                if (notifyRequest.getSTEP() != null && notifyRequest.getSTEP().equalsIgnoreCase("REVERSE")) {
                } else {
                    notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE").trim());
                }
                notifyRequest.setBANK("MANDIRI");

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOAFull) {

                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setAPPROVALCODE(params.remove("TRANSACTIONID"));

                if (notifyRequest.getSTEP() != null && notifyRequest.getSTEP().equalsIgnoreCase("REVERSE")) {

                } else {
                    notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE").trim());
                }
                notifyRequest.setBANK("MANDIRI");

                //For RECONCILE
                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriVALite) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("MANDIRI");

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriVAFull) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setBANK("MANDIRI");

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

            } else if (pChannel == OneCheckoutPaymentChannel.PayPal) {
                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                //      notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setBANK(params.remove("BANK"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                //    notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                //   notifyRequest.setDFSStatus(params.remove("VERIFYSTATUS"));
                //       notifyRequest.setDFSScore(params.remove("VERIFYSCORE"));
                //       notifyRequest.setDFSIId(params.remove("VERIFYID"));
                notifyRequest.setMESSAGE(params.remove("SHORTMESSAGE"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");
            } else if (pChannel == OneCheckoutPaymentChannel.BNIDebitOnline) {

                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                //      notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setBANK(params.remove("BANK"));
                if (notifyRequest.getBANK() == null || notifyRequest.getBANK().equalsIgnoreCase("null")) {
                    notifyRequest.setBANK("");
                }
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                //    notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                notifyRequest.setDFSStatus(params.remove("VERIFYSTATUS"));
                notifyRequest.setDFSScore(params.remove("VERIFYSCORE"));
                notifyRequest.setDFSIId(params.remove("VERIFYID"));

            } else if (pChannel == OneCheckoutPaymentChannel.BSP) {

                notifyRequest.setTYPE(params.remove("TYPE"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setMALLID(params.remove("MALLID"));

                try {
                    if (params.get("CHAINMALLID") != null) {
                        notifyRequest.setCHAINNUM(params.remove("CHAINMALLID"));
                    }

                } catch (Throwable t) {
                    t.printStackTrace();
                }

                notifyRequest.setTRXCODE(params.remove("TRXCODE"));
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));

                String paymentDate = params.remove("PAYMENTDATE");
                if (paymentDate != null && !paymentDate.trim().equals("")) {
                    notifyRequest.setREQUESPAYMENTDATETIME(OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDate));
                }

                OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

                //Currency currency = queryHelper.getCurrencyByAlphaCode(params.remove("CURRENCY"));
                //notifyRequest.setCURRENCY(currency != null ? currency.getCurrencyCode() : params.remove("CURRENCY"));
                //notifyRequest.setCURRENCY(params.remove("CURRENCY"));
                notifyRequest.setRESULT(params.remove("RESULT"));

                notifyRequest.setBANK(params.remove("BANK"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULT"));
                notifyRequest.setTHREEDSECURESTATUS(params.remove("THREEDSECURESTATUS"));
                notifyRequest.setLIABILITY(params.remove("LIABILITY"));

                if (notifyRequest.getRESULT() != null && notifyRequest.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
                } else if (notifyRequest.getDFSStatus() == OneCheckoutDFSStatus.REVIEW) { // harusnya kalau transaksi FAILED, verify status nya tidak mungkin review
                    notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                    notifyRequest.setDFSScore("-1");
                }

                notifyRequest.setDFSStatus(params.remove("FRAUDSCREENINGSTATUS"));
                notifyRequest.setDFSScore(params.remove("FRAUDSCREENINGSCORE"));
                notifyRequest.setDFSIId(params.remove("FRAUDSCREENINGID"));

                Currency currency = queryHelper.getCurrencyByAlphaCode(params.remove("CURRENCY"));

                if (currency == null) {
                    return onecheckout;
                }

                notifyRequest.setCURRENCY(currency.getCurrencyCode());

            } else if (pChannel == OneCheckoutPaymentChannel.Tokenization) {
                String result = params.remove("RESULTMSG");
                notifyRequest.setTRANSIDMERCHANT(params.remove("BILLNUMBER"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                //      notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(result);
                //notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setBANK(params.remove("BANK"));
                if (notifyRequest.getBANK() == null || notifyRequest.getBANK().equalsIgnoreCase("null")) {
                    notifyRequest.setBANK("");
                }
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(result);

            } else if (pChannel == OneCheckoutPaymentChannel.KlikPayBCACard || pChannel == OneCheckoutPaymentChannel.KlikPayBCADebit) {
                notifyRequest.setBANK("BCA");
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");
            } else if (pChannel == OneCheckoutPaymentChannel.CIMBClicks) {
                notifyRequest.setBANK("CIMB Niaga");
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");
            } else if (pChannel == OneCheckoutPaymentChannel.Muamalat) {
                notifyRequest.setBANK("Muamalat");
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));

                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");
            } else if (pChannel == OneCheckoutPaymentChannel.Danamon) {
                notifyRequest.setBANK("Danamon");
                notifyRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("OrderNumber"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setRESULT(params.remove("RESULT"));
                notifyRequest.setAPPROVALCODE(params.remove("APPROVALCODE"));

                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");
            } else if (pChannel == OneCheckoutPaymentChannel.BCAVAFull) {

//                notifyRequest.setBCA_VANUMBER(params.remove("VANUMBER"));
//                notifyRequest.setBCA_MALLID(params.remove("MALLID"));
//                notifyRequest.setBCA_TRACENO(params.remove("TRACENO"));
//                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
//                notifyRequest.setWORDS(params.remove("WORDS"));
//                notifyRequest.setTRANSIDMERCHANT(params.remove("TRANSACTIONNO"));
////                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE").trim());
//                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
//                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
//                notifyRequest.setREQUESPAYMENTDATETIME(params.remove("PAYMENTDATETIME"));
//                notifyRequest.setBCA_STEP(params.remove("STEP"));
//                notifyRequest.setBANK("BCA");
                notifyRequest.setTRANSIDMERCHANT(params.remove("TRANSACTIONNO"));
                notifyRequest.setBANK(params.get("BANK"));
                notifyRequest.setBCA_MALLID(params.get("MALLID"));
                notifyRequest.setBCA_VANUMBER(params.get("USERACCOUNT"));
                notifyRequest.setBCA_STEP(params.get("STEP"));
                notifyRequest.setAMOUNT(params.get("AMOUNT"));
                notifyRequest.setWORDS(params.get("WORDS"));
                notifyRequest.setCOMPANYCODE(params.get("COMPANYCODE"));
                notifyRequest.setAPPROVALCODE(params.get("APPROVALCODE"));
                notifyRequest.setHOSTREFNUM(params.get("TRACENO"));

                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                notifyRequest.setDFSScore("-1");
                notifyRequest.setDFSIId("");

                //For Reconcile Only
                /*if(notifyRequest.getPERMATA_STEP().equalsIgnoreCase("SETTLEMENT")) {
                 notifyRequest.setPAYCODE("VAID");
                 }*/
            }

            if (notifyRequest.getRESPONSECODE() != null && notifyRequest.getRESPONSECODE().length() == 4) {
                notifyRequest.setRESPONSECODE(notifyRequest.getRESPONSECODE());
            } else {
                notifyRequest.setRESPONSECODE("00" + notifyRequest.getRESPONSECODE());
                if (notifyRequest.getRESPONSECODE().length() > 4) {
                    int sisa = notifyRequest.getRESPONSECODE().length() - 4;
                    notifyRequest.setRESPONSECODE(notifyRequest.getRESPONSECODE().substring(sisa, notifyRequest.getRESPONSECODE().length()));
                }
            }

            onecheckout.setMessage("VALID");
            onecheckout.setNotifyRequest(notifyRequest);
            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());
            return onecheckout;

        }

    }

    public static OneCheckoutDataHelper parseNotifyMIBRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {

        OneCheckoutDataHelper oneCheckoutDataHelper = new OneCheckoutDataHelper();
        try {
            OneCheckoutDOKUNotifyData oneCheckoutDOKUNotifyData = new OneCheckoutDOKUNotifyData();
            String bank = params.remove("BANK");

            oneCheckoutDOKUNotifyData.setTYPE(params.remove("TYPE"));
            oneCheckoutDOKUNotifyData.setWORDS(params.remove("WORDS"));
            oneCheckoutDOKUNotifyData.setMALLID(params.remove("MALLID"));

            try {
                if (params.get("CHAINMALLID") != null) {
                    oneCheckoutDOKUNotifyData.setCHAINNUM(params.remove("CHAINMALLID"));
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            oneCheckoutDOKUNotifyData.setTRXCODE(params.remove("TRXCODE"));
            oneCheckoutDOKUNotifyData.setTRANSIDMERCHANT(params.remove("INVOICENUMBER"));
            oneCheckoutDOKUNotifyData.setAMOUNT(params.remove("AMOUNT"));
            String paymentDate = params.remove("PAYMENTDATE");
            if (paymentDate != null && !paymentDate.trim().equals("")) {
                oneCheckoutDOKUNotifyData.setREQUESPAYMENTDATETIME(OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDate));
            }

            oneCheckoutDOKUNotifyData.setRESULT(params.remove("RESULT"));
            oneCheckoutDOKUNotifyData.setBANK(bank);
            oneCheckoutDOKUNotifyData.setAPPROVALCODE(params.remove("APPROVALCODE"));
            oneCheckoutDOKUNotifyData.setRESPONSECODE(params.remove("RESPONSECODE"));
            oneCheckoutDOKUNotifyData.setSESSIONID(params.remove("SESSIONID"));
            oneCheckoutDOKUNotifyData.setRESULTMSG(params.remove("MESSAGE"));

            if (bank.equals("CIMB Clicks")) {
                oneCheckoutDataHelper.setPaymentChannel(OneCheckoutPaymentChannel.CIMBClicks);
            } else if (bank.equals("KLIKPAY BCA")) {
                String sessionId = oneCheckoutDOKUNotifyData.getSESSIONID();
                String invoiceNo = oneCheckoutDOKUNotifyData.getTRANSIDMERCHANT();
                BigDecimal amot = BigDecimal.valueOf(oneCheckoutDOKUNotifyData.getAMOUNT());

                OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
                OneCheckoutLogger.log("WebUtils.parseNotifyMIBRequestData : INVOICENO : %s   SESSIONID : %s   AMOUNT : %s  ", invoiceNo, sessionId, OneCheckoutVerifyFormatData.sdf.format(oneCheckoutDOKUNotifyData.getAMOUNT()));
                Transactions trx = queryBeans.getTransactionByInvoiceSessionAmount(invoiceNo, sessionId, amot);//getCheckStatusTransactionByInvoiceSession(m, invoiceNo, sessionId);

                if (trx == null) {
                    OneCheckoutLogger.log("WebUtils.parseNotifyMIBRequestData : ERROR : trx==null");
                    return oneCheckoutDataHelper;
                }

                oneCheckoutDataHelper.setTransactions(trx);
                OneCheckoutPaymentChannel pChannel = OneCheckoutPaymentChannel.findType(trx.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());
                oneCheckoutDataHelper.setPaymentChannel(pChannel);
            } else if (bank.equalsIgnoreCase("BMI")) {
                oneCheckoutDataHelper.setPaymentChannel(OneCheckoutPaymentChannel.Muamalat);
            } else if (bank.equalsIgnoreCase("DANAMON")) {
                oneCheckoutDataHelper.setPaymentChannel(OneCheckoutPaymentChannel.Danamon);
            } else if (bank.equalsIgnoreCase("DOMPETKU")) {
                oneCheckoutDataHelper.setPaymentChannel(OneCheckoutPaymentChannel.DompetKu);
            } else if (bank.equalsIgnoreCase("PERMATA")) {
                oneCheckoutDataHelper.setPaymentChannel(OneCheckoutPaymentChannel.Permata);
            } else if (bank.equalsIgnoreCase("KREDIVO")) {
                oneCheckoutDataHelper.setPaymentChannel(OneCheckoutPaymentChannel.Kredivo);
            }

            if (oneCheckoutDOKUNotifyData.getRESULT() != null && oneCheckoutDOKUNotifyData.getRESULT().toUpperCase().indexOf("SUCCESS") >= 0) {
            } else if (oneCheckoutDOKUNotifyData.getDFSStatus() == OneCheckoutDFSStatus.REVIEW) { // harusnya kalau transaksi FAILED, verify status nya tidak mungkin review
                oneCheckoutDOKUNotifyData.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                oneCheckoutDOKUNotifyData.setDFSScore("-1");
            }
            oneCheckoutDOKUNotifyData.setDFSStatus(params.remove("FRAUDSCREENINGSTATUS"));
            oneCheckoutDOKUNotifyData.setDFSScore(params.remove("FRAUDSCREENINGSCORE"));
            oneCheckoutDOKUNotifyData.setDFSIId(params.remove("FRAUDSCREENINGID"));

            OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            Currency currency = queryHelper.getCurrencyByAlphaCode(params.remove("CURRENCY"));
            if (currency != null) {
                oneCheckoutDOKUNotifyData.setCURRENCY(currency.getCurrencyCode());
            }

            if (oneCheckoutDOKUNotifyData.getRESPONSECODE() != null && oneCheckoutDOKUNotifyData.getRESPONSECODE().length() == 4) {
            } else {
                oneCheckoutDOKUNotifyData.setRESPONSECODE("00" + oneCheckoutDOKUNotifyData.getRESPONSECODE());
                if (oneCheckoutDOKUNotifyData.getRESPONSECODE().length() > 4) {
                    int sisa = oneCheckoutDOKUNotifyData.getRESPONSECODE().length() - 4;
                    oneCheckoutDOKUNotifyData.setRESPONSECODE(oneCheckoutDOKUNotifyData.getRESPONSECODE().substring(sisa, oneCheckoutDOKUNotifyData.getRESPONSECODE().length()));
                }
            }

            oneCheckoutDataHelper.setMessage("VALID");
            oneCheckoutDataHelper.setNotifyRequest(oneCheckoutDOKUNotifyData);
        } catch (Throwable e) {
            e.printStackTrace();
            oneCheckoutDataHelper.setMessage(e.getMessage());
        }
        return oneCheckoutDataHelper;
    }

    public static OneCheckoutDataHelper parseReversalDOKURequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session, OneCheckoutPaymentChannel pChannel) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            onecheckout.setPaymentChannel(pChannel);

            OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();

            if (pChannel == OneCheckoutPaymentChannel.PermataVALite) {

                //notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("PERMATA");

            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVALite) {

                //notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("SINARMAS");
            } else if (pChannel == OneCheckoutPaymentChannel.PTPOS) {

                notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("PTPOS");

            } else if (pChannel == OneCheckoutPaymentChannel.Alfamart) {

                //notifyRequest.setVAID(params.remove("VAID"));
                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("Alfamart");

            } else if (pChannel == OneCheckoutPaymentChannel.PermataVAFull) {

                notifyRequest.setPERMATA_VANUMBER(params.remove("VANUMBER"));
                notifyRequest.setPERMATA_MALLID(params.remove("MALLID"));
                notifyRequest.setPERMATA_TRACENO(params.remove("TRACENO"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE").trim());
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setREQUESPAYMENTDATETIME(params.remove("PAYMENTDATETIME"));
                notifyRequest.setPERMATA_STEP(params.remove("STEP"));
                notifyRequest.setBANK("PERMATA");

            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVAFull) {

                notifyRequest.setSINARMAS_VANUMBER(params.remove("USERACCOUNT"));
                notifyRequest.setSINARMAS_MALLID(params.remove("MALLID"));
                notifyRequest.setSINARMAS_TRACENO(params.remove("TRACENO"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                notifyRequest.setRESPONSECODE(params.remove("RESPONSECODE").trim());
                notifyRequest.setRESULTMSG(params.remove("RESULTMSG"));
                notifyRequest.setSESSIONID(params.remove("SESSIONID"));
                notifyRequest.setREQUESPAYMENTDATETIME(params.remove("PAYMENTDATETIME"));
                notifyRequest.setSINARMAS_STEP(params.remove("STEP"));
                notifyRequest.setBANK("SINARMAS");

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOALite) {

                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("MANDIRI");

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOAFull) {

                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setBANK("MANDIRI");

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriVALite) {

                notifyRequest.setUSERACCOUNT(params.remove("USERACCOUNT"));
                notifyRequest.setMERCHANTCODE(params.remove("MERCHANTCODE"));
                notifyRequest.setTRANSACTIONID(params.remove("TRANSACTIONID"));
                notifyRequest.setSTEP(params.remove("STEP"));
                notifyRequest.setWORDS(params.remove("WORDS"));
                notifyRequest.setAMOUNT(params.remove("AMOUNT"));
                notifyRequest.setHOSTREFNUM(params.remove("HOSTREFNUM"));
                notifyRequest.setCHANNELID(params.remove("CHANNELID"));
                notifyRequest.setCOMPANYCODE(params.remove("COMPANYCODE"));
                notifyRequest.setBANK("MANDIRI");

            }

            /*if(notifyRequest.getRESPONSECODE() != null && notifyRequest.getRESPONSECODE().length() == 4) {
             notifyRequest.setRESPONSECODE(notifyRequest.getRESPONSECODE());
             } else {
             notifyRequest.setRESPONSECODE("00" + notifyRequest.getRESPONSECODE());
             }*/
            onecheckout.setMessage("VALID");
            onecheckout.setNotifyRequest(notifyRequest);
            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());
            return onecheckout;

        }
    }

    public static OneCheckoutDataHelper parseDWRegistration(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {
        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

//            String checksum = request.getParameter("REGCHECKSUM");
//            String numberid = request.getParameter("REGNUMBERID");
            String phone = request.getParameter("REGPHONE");
            String email = request.getParameter("REGEMAIL");
            String name = request.getParameter("REGNAME");

            String numberid = request.getParameter("REGNUMBERID");
            long id = Long.valueOf(numberid).longValue();
            OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            Transactions trans = queryHelper.getTransactionByID(id);
            MerchantPaymentChannel merchantPaymentChannel = queryHelper.getMerchantPaymentChannel(trans.getMerchantPaymentChannel().getMerchants(), OneCheckoutPaymentChannel.DOKUPAY);
            //SHA1 (EMAIL + DPMALLID + sharedKey + PHONE)
            String clearText = email + merchantPaymentChannel.getMerchantPaymentChannelPid() + merchantPaymentChannel.getMerchantPaymentChannelHash() + phone;

//            String checksum_ver = HashWithSHA1.doHashing(numberid+"cintaKU**","SHA2",null);            
//            if (checksum.equals(checksum_ver)){
            // proses tembak DOKUWALLET       
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("NAME", name);
            hashMap.put("EMAIL", email);
            hashMap.put("PHONE", phone);
            hashMap.put("DPMALLID", merchantPaymentChannel.getMerchantPaymentChannelPid());
            hashMap.put("WORDS", HashWithSHA1.doHashing(clearText, null, null));

            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String data_encode = base.createParamsHTTP(hashMap);

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            String regUrl = config.getString("ONECHECKOUT.REGDW.REGURL");
            int conTimeout = config.getInt("ONECHECKOUT.REGDW.CONNECTIONTIMEOUT");
            int readTimeout = config.getInt("ONECHECKOUT.REGDW.READTIMEOUT");

            OneCheckoutLogger.log("DW Registration : Post Params" + params.toString());

            OneCheckoutLogger.log("Fetch URL     : %s", regUrl);
            OneCheckoutLogger.log("DW Data : %s", data_encode);
            InternetResponse resp = base.doFetchHTTP(data_encode, regUrl, conTimeout, readTimeout);

            OneCheckoutLogger.log("RESPONSE : %s", resp.getMsgResponse());
//            }
//            else {
//                OneCheckoutLogger.log("CHECKSUM MATCH !");
//            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        params.put("FLAG", "2");
        onecheckout = WebUtils.parseRedirectRequestData(params, request, session);
        return onecheckout;
    }

    public static OneCheckoutDataHelper parseRedirectRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();
        OneCheckoutLogger.log("PARSEREDIRECTREQUESTDATA");
        try {
            String TRANSIDMERCHANT = null;
            String transorinvoicenumber = null;
            if (params.containsKey("TRANSIDMERCHANT")) {
                TRANSIDMERCHANT = request.getParameter("TRANSIDMERCHANT");
                transorinvoicenumber = "TRANSIDMERCHANT";
            } else if (params.containsKey("INVOICENUMBER")) {
                TRANSIDMERCHANT = request.getParameter("INVOICENUMBER");
                transorinvoicenumber = "INVOICENUMBER";
            }

            OneCheckoutRedirectData redirectRequest = new OneCheckoutRedirectData();
            //block point redemeed
//            String amountRedeemed = params.get("AMOUNTREDEEMED");
//            if(amountRedeemed != null && !amountRedeemed.equals("")){
//                onecheckout.setAmountRedeemed(amountRedeemed);
//            }

            String transIdMerchant = params.get("TRANSIDMERCHANT");
            String sessionId = params.get("SESSIONID");
            double amount = OneCheckoutBaseRules.validateAmount1(params.get("AMOUNT"), "AMOUNT");

            OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

            String paymentCode = request.getParameter("PAYMENTCODE");
            String pChannelid = request.getParameter("PAYMENTCHANNEL");

            /*
             data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
             data.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
             data.put("NUMBERID", trans.getTransactionsId());
            
             String checksum = HashWithSHA1.SHA2(trans.getTransactionsId()+OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue())+ trans.getIncSessionid()+"cintaKU**");

             data.put("CHECKSUM", checksum);
             data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
             data.put("SESSIONID", trans.getIncSessionid()); 
  
             */
            String checksum = request.getParameter("CHECKSUM");
            String numberid = request.getParameter("NUMBERID");
            String dwReqName = params.get("REGNAME") != null ? params.remove("REGNAME").toString().trim() : "";
            String dwReqEmail = params.get("REGEMAIL") != null ? params.remove("REGEMAIL").toString().trim() : "";
            String dwReqPhone = params.get("REGPHONE") != null ? params.remove("REGPHONE").toString().trim() : "";
            String dwReqStatus = params.get("recogida") != null ? params.remove("recogida").toString().trim() : "";
            onecheckout.setDwRegEmail(dwReqEmail);
            onecheckout.setDwRegName(dwReqName);
            onecheckout.setDwRegPhone(dwReqPhone);

            if (dwReqStatus != null && dwReqStatus.equals("domicilio")) {
                onecheckout.setDwRegStatus(true);
            }
            Transactions trans = null;

            /* GET PARAMETER GOBACK KLIK PAY BCA */
            String checkpay = request.getParameter("CHECKPAY");
            if (checkpay != null && !checkpay.isEmpty() && numberid != null && !numberid.isEmpty()) {
                PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
                String GOBACKAUTH = config.getString("KLIKPAYBCA.GOBACK.AUTH");
                String checkpay_ver = HashWithSHA1.doHashing(numberid + params.get("AMOUNT") + GOBACKAUTH, "SHA2", null);//HashWithSHA1.SHA2(numberid+params.get("AMOUNT")+GOBACKAUTH);
                if (checkpay_ver.equals(checkpay)) {
                    onecheckout.setMessage("CHECKSUM MATCH !");
                    long id = Long.valueOf(numberid).longValue();
                    trans = queryHelper.getTransactionByID(id);
                    onecheckout.setMessage("VALID");
                    onecheckout.setTransactions(trans);
                    MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
                    Merchants m = mpc.getMerchants();
                    PaymentChannel pc = mpc.getPaymentChannel();
                    OneCheckoutPaymentChannel pChannel = OneCheckoutPaymentChannel.findType(pc.getPaymentChannelId());
                    onecheckout.setMerchant(m);
                    onecheckout.setMallId(m.getMerchantCode());
                    onecheckout.setPaymentChannel(pChannel);
                } else {
                    onecheckout.setMessage("CHECKPAY DOES NOT MATCH !");
                    onecheckout.setRedirectDoku(redirectRequest);
                }
                return onecheckout;
            }

            if (checksum != null && !checksum.isEmpty() && numberid != null && !numberid.isEmpty()) {
                String checksum_ver = HashWithSHA1.doHashing(numberid + params.get("AMOUNT") + sessionId + "cintaKU**", "SHA2", null);

                if (checksum_ver.equals(checksum)) {
                    onecheckout.setMessage("CHECKSUM MATCH !");
                    long id = Long.valueOf(numberid).longValue();
                    trans = queryHelper.getTransactionByID(id);

                    onecheckout.setMessage("VALID");
                    onecheckout.setTransactions(trans);

                    MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
                    Merchants m = mpc.getMerchants();
                    PaymentChannel pc = mpc.getPaymentChannel();
                    OneCheckoutPaymentChannel pChannel = OneCheckoutPaymentChannel.findType(pc.getPaymentChannelId());
                    onecheckout.setMerchant(m);
                    onecheckout.setMallId(m.getMerchantCode());
                    onecheckout.setPaymentChannel(pChannel);
                    //      onecheckout.setRedirectDoku(null);
                } else {

                    onecheckout.setMessage("CHECKSUM DOES NOT MATCH !");
                    onecheckout.setRedirectDoku(redirectRequest);

                }

                return onecheckout;

            } else if ((paymentCode != null && paymentCode.length() > 0) && (pChannelid != null && pChannelid.length() > 0)) {
                OneCheckoutLogger.log("DATA QUERY = " + transIdMerchant + " " + sessionId + " " + amount + " " + paymentCode + " " + pChannelid);
                trans = queryHelper.getRedirectTransactionWithoutState(transIdMerchant, sessionId, amount, paymentCode, pChannelid);

            } else {
                trans = queryHelper.getRedirectTransactionWithoutState(transIdMerchant, sessionId, amount);
            }

            MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
            Merchants m = mpc.getMerchants();
            PaymentChannel pc = mpc.getPaymentChannel();
            OneCheckoutPaymentChannel pChannel = OneCheckoutPaymentChannel.findType(pc.getPaymentChannelId());
            onecheckout.setMerchant(m);
            onecheckout.setMallId(m.getMerchantCode());
            onecheckout.setPaymentChannel(pChannel);

            if (pChannel == OneCheckoutPaymentChannel.CreditCard) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

                //FOR TRANSACTION RESULT
                redirectRequest.setFLAG(params.remove("FLAG"));

                /*if(redirectRequest.getFLAG() == null || redirectRequest.getFLAG().equalsIgnoreCase("0")) {
                 redirectRequest.setDATENOW(params.remove("DATENOW"));
                 redirectRequest.setDISPLAYNAME(params.remove("DISPLAYNAME"));
                 redirectRequest.setDISPLAYADDRESS(params.remove("DISPLAYADDRESS"));

                 redirectRequest.setMSG(params.remove("MSG"));
                 redirectRequest.setMSGDETAIL(params.remove("MSGDETAIL"));

                 redirectRequest.setAPRROVALCODE(params.remove("APRROVALCODE"));
                 redirectRequest.setTYPE(params.remove("TYPE"));
                 redirectRequest.setBRANDID(params.remove("BRANDID"));
                 redirectRequest.setCREDITCARD(params.remove("CREDITCARD"));

                 redirectRequest.setREWARDSSTATUS(params.remove("REWARDSSTATUS"));
                 if(redirectRequest.getREWARDSSTATUS() == Boolean.TRUE) {
                 redirectRequest.setNETAMOUNT(params.remove("NETAMOUNT"));
                 redirectRequest.setPOINTSREDEEMED(params.remove("POINTSREDEEMED"));
                 redirectRequest.setAMOUNTREDEEMED(params.remove("AMOUNTREDEEMED"));
                 }

                 redirectRequest.setCURRENCY(params.remove("CURRENCY"));
                 redirectRequest.setMERCHANTNUM(params.remove("MERCHANTNUM"));

                 //     if(redirectRequest.getDISPLAYNAME() == null || redirectRequest.getDISPLAYADDRESS() == null) {
                 //         redirectRequest.setFLAG("2");
                 //     }

                 }*/
            } else if (pChannel == OneCheckoutPaymentChannel.DirectDebitMandiri) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

                redirectRequest.setFLAG(params.remove("FLAG"));

            } else if (pChannel == OneCheckoutPaymentChannel.KlikBCA) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.DOKUPAY) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

                redirectRequest.setFLAG(params.remove("FLAG"));

            } else if (pChannel == OneCheckoutPaymentChannel.BRIPay) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

                redirectRequest.setFLAG(params.remove("FLAG"));

            } else if (pChannel == OneCheckoutPaymentChannel.PermataVALite) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVALite) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.PTPOS) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.Alfamart) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.BRIVA) {
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));
            } else if (pChannel == OneCheckoutPaymentChannel.AlfaMVA) {
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));
            } else if (pChannel == OneCheckoutPaymentChannel.DanamonVA) {
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));
            } else if (pChannel == OneCheckoutPaymentChannel.Indomaret) {
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.PermataMVA) {
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.CIMBVA) {
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.PermataVAFull) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.SinarMasVAFull) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOALite) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriSOAFull) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriVALite) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.MandiriVAFull) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.PayPal) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

                redirectRequest.setFLAG(params.remove("FLAG"));

            } else if (pChannel == OneCheckoutPaymentChannel.BNIDebitOnline) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

                redirectRequest.setFLAG(params.remove("FLAG"));

            } else if (pChannel == OneCheckoutPaymentChannel.BSP || pChannel == OneCheckoutPaymentChannel.MOTO) {
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));

                //Currency currency = queryHelper.getCurrencyByAlphaCode(params.remove("CURRENCY"));
                //redirectRequest.setCURRENCY(currency != null ? currency.getCurrencyCode() : params.remove("CURRENCY"));
                redirectRequest.setSTATUSCODE(params.remove("NOTIFYSTATUS"));

                //FOR TRANSACTION RESULT
                redirectRequest.setFLAG(params.remove("FLAG"));

            } else if (pChannel == OneCheckoutPaymentChannel.Tokenization) {
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));

            } else if (pChannel == OneCheckoutPaymentChannel.Recur) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));

                //FOR TRANSACTION RESULT
                redirectRequest.setFLAG(params.remove("FLAG"));
            } else if (pChannel == OneCheckoutPaymentChannel.KlikPayBCACard || pChannel == OneCheckoutPaymentChannel.KlikPayBCADebit) {
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setFLAG(params.remove("FLAG"));
            } else if (pChannel == OneCheckoutPaymentChannel.CIMBClicks) {
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setFLAG(params.remove("FLAG"));
            } else if (pChannel == OneCheckoutPaymentChannel.Muamalat) {
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setFLAG(params.remove("FLAG"));
            } else if (pChannel == OneCheckoutPaymentChannel.Danamon) {
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setFLAG(params.remove("FLAG"));
            } else if (pChannel == OneCheckoutPaymentChannel.DompetKu) {
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setFLAG(params.remove("FLAG"));
            } else if (pChannel == OneCheckoutPaymentChannel.Permata) {
                redirectRequest.setTRANSIDMERCHANT(params.remove(transorinvoicenumber));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setFLAG(params.remove("FLAG"));
            } else if (pChannel == OneCheckoutPaymentChannel.BNIVA) {

                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setWORDS(params.remove("WORDS"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setPAYMENTCHANNEL(params.remove("PAYMENTCHANNEL"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setPAYMENTCODE(params.remove("PAYMENTCODE"));

            } else if (pChannel == OneCheckoutPaymentChannel.Kredivo) {
                redirectRequest.setTRANSIDMERCHANT(params.remove(transorinvoicenumber));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
                redirectRequest.setFLAG(params.remove("FLAG"));
            }

            if (redirectRequest.getSTATUSCODE() != null && redirectRequest.getSTATUSCODE().length() == 4) {
                redirectRequest.setSTATUSCODE(redirectRequest.getSTATUSCODE());
            } else {
                redirectRequest.setSTATUSCODE("00" + redirectRequest.getSTATUSCODE());
                if (redirectRequest.getSTATUSCODE().length() > 4) {
                    int sisa = redirectRequest.getSTATUSCODE().length() - 4;
                    redirectRequest.setSTATUSCODE(redirectRequest.getSTATUSCODE().substring(sisa, redirectRequest.getSTATUSCODE().length()));
                }
            }
            onecheckout.setMessage("VALID");
            onecheckout.setRedirectDoku(redirectRequest);
            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setRedirectDoku(null);
            onecheckout.setMessage(e.getMessage());
            return onecheckout;
        }
    }

    /*
     public static OneCheckoutDataHelper parseVoidRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {


     OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

     try {

     String paymentChannel = params.remove("PAYMENTCHANNEL");
     OneCheckoutPaymentChannel pchannel = OneCheckoutBaseRules.validatePaymentChannel(paymentChannel);

     onecheckout.setPaymentChannel(pchannel);
     String mallId = params.remove("MALLID");


     OneCheckoutVoidRequest voidRequest = new OneCheckoutVoidRequest(mallId);

     voidRequest.setSESSIONID(params.remove("SESSIONID"));
     voidRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
     //      voidRequest.setVOIDDATETIME(params.remove("VOIDDATETIME"));
     voidRequest.setWORDS(params.remove("WORDS"));

     onecheckout.setMessage("VALID");

     Merchant m = WebUtils.getValidMALLID(voidRequest.getMALLID());
     onecheckout.setMerchant(m);

     onecheckout.setVoidRequest(voidRequest);


     return onecheckout;


     } catch (Exception e) {
     e.printStackTrace();
     onecheckout.setMessage(e.getMessage());
     return onecheckout;

     }

     }


     public static OneCheckoutDataHelper parseCheckStatusRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {


     OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

     try {

     String paymentChannel = params.remove("PAYMENTCHANNEL");
     OneCheckoutPaymentChannel pchannel = OneCheckoutBaseRules.validatePaymentChannel(paymentChannel);

     onecheckout.setPaymentChannel(pchannel);
     String mallId = params.remove("MALLID");
     OneCheckoutCheckStatusRequest checkStatusRequest = new OneCheckoutCheckStatusRequest(mallId);

     checkStatusRequest.setSESSIONID(params.remove("SESSIONID"));
     checkStatusRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));

     OneCheckoutLogger.log("DEBUG 1");
     Merchant m = WebUtils.getValidMALLID(checkStatusRequest.getMALLID());
     OneCheckoutLogger.log("DEBUG 2");

     onecheckout.setMerchant(m);

     onecheckout.setCheckStatusRequest(checkStatusRequest);
     onecheckout.setMessage("VALID");


     return onecheckout;


     } catch (Exception e) {
     e.printStackTrace();
     onecheckout.setMessage(e.getMessage());
     return onecheckout;

     }

     }




     */
    public static OneCheckoutDataHelper parseEDSGetDataRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session, OneCheckoutPaymentChannel pChannel) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();
        try {
            onecheckout.setPaymentChannel(pChannel);
            OneCheckoutEDSGetData edsData = new OneCheckoutEDSGetData();

            edsData.setAMOUNT(params.remove("amount"));
            edsData.setTRANSIDMERCHANT(params.remove("invoiceNo"));
            edsData.setWORDS(params.remove("ref"));

            onecheckout.setMessage("VALID");
            onecheckout.setEdsGetData(edsData);
            return onecheckout;
        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());
            return onecheckout;
        }
    }

    public static OneCheckoutDataHelper prosesCIPRequestParams(OneCheckoutDataHelper oneCheckout) {
        PaymentPageObject paymentPageObject = new PaymentPageObject();

        PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
        String actionUrl = config.getString("PAYMENTCIP.ACTION.URL", "/Suite/ProcessPaymentCIP");

        //page.setActionUrl(actionUrl);
        //        if (oneCheckout.getMallId()==1111) {
        actionUrl = actionUrl + "?MALLID=" + oneCheckout.getMallId() + "&CHAINMERCHANT=" + oneCheckout.getChainMerchantId() + "&INV=" + oneCheckout.getPaymentRequest().getTRANSIDMERCHANT();
        paymentPageObject.setActionUrl(actionUrl);
        paymentPageObject.setCancelUrl(actionUrl);
        //        }    
        //        else {
        //            paymentPageObject.setActionUrl("");
        //            paymentPageObject.setCancelUrl("");            
        //        } 

        OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class
        );

        Currency currency = queryBeans.getCurrencyByCode(oneCheckout.getPaymentRequest().getCURRENCY());

        paymentPageObject.setDatenow(OneCheckoutVerifyFormatData.email_datetimeFormat.format(new Date()));
        paymentPageObject.setMerchantName(oneCheckout.getMerchant().getMerchantName());
        paymentPageObject.setMerchantAddress(oneCheckout.getMerchant().getMerchantAddress());
        paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());

        //Normal
        paymentPageObject.setPurchaseCurrencyused(currency
                != null ? currency.getAlpha3Code() : oneCheckout.getPaymentRequest().getCURRENCY());
        paymentPageObject.setAmount(OneCheckoutVerifyFormatData.moneyFormat.format(oneCheckout.getPaymentRequest().getAMOUNT()));

        //Purchanse
        currency = queryBeans.getCurrencyByCode(oneCheckout.getPaymentRequest().getPURCHASECURRENCY());

        paymentPageObject.setPurchaseCurrency(currency
                != null ? currency.getAlpha3Code() : oneCheckout.getPaymentRequest().getCURRENCY());
        paymentPageObject.setPurchaseAmount(OneCheckoutVerifyFormatData.moneyFormat.format(oneCheckout.getPaymentRequest().getPURCHASEAMOUNT()));

        if (oneCheckout.getPaymentChannel()
                == null || oneCheckout.getPaymentChannel() != OneCheckoutPaymentChannel.Recur) {
            paymentPageObject.setBasket(OneCheckoutVerifyFormatData.GetValidBasket(oneCheckout.getPaymentRequest().getBASKET(), oneCheckout.getPaymentRequest().getAMOUNT()));
        }

        paymentPageObject.setChallengeCode2(
                "");
        paymentPageObject.setChallengeCode3(
                "");
        paymentPageObject.setListCountry(queryBeans.getListCountry());

        OneCheckoutLogger.log(
                ": : : : : COUNTRY SIZE " + paymentPageObject.getListCountry().size());

        List<MerchantPaymentChannel> payChannel = queryBeans.getListMerchantPaymentChannel(oneCheckout.getMerchant());

        OneCheckoutLogger.log(
                ": : : : : CHANNEL SIZE " + payChannel.size());

        ArrayList<String> channel = new ArrayList<String>();

        if (oneCheckout.getPaymentChannel()
                != null) {
            channel.add(oneCheckout.getPaymentChannel().toString());

            OneCheckoutLogger.log(": : : : : PAYMENT CHANNEL " + oneCheckout.getPaymentChannel().toString());

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.CreditCard || oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BSP || oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization || oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Recur || oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {

                paymentPageObject.setName(oneCheckout.getPaymentRequest().getNAME() == null ? "" : oneCheckout.getPaymentRequest().getNAME());
                paymentPageObject.setEmail(oneCheckout.getPaymentRequest().getEMAIL() == null ? "" : oneCheckout.getPaymentRequest().getEMAIL());
                paymentPageObject.setAddress(oneCheckout.getPaymentRequest().getADDRESS() == null ? "" : oneCheckout.getPaymentRequest().getADDRESS());
                paymentPageObject.setCity(oneCheckout.getPaymentRequest().getCITY() == null ? "" : oneCheckout.getPaymentRequest().getCITY());
                paymentPageObject.setState(oneCheckout.getPaymentRequest().getSTATE() == null ? "" : oneCheckout.getPaymentRequest().getSTATE());
                paymentPageObject.setCountry(oneCheckout.getPaymentRequest().getCOUNTRY() == null ? "" : oneCheckout.getPaymentRequest().getCOUNTRY());
                paymentPageObject.setZipcode(oneCheckout.getPaymentRequest().getZIPCODE() == null ? "" : oneCheckout.getPaymentRequest().getZIPCODE());
                paymentPageObject.setHomephone(oneCheckout.getPaymentRequest().getHOMEPHONE() == null ? "" : oneCheckout.getPaymentRequest().getHOMEPHONE());
                paymentPageObject.setMobilephone(oneCheckout.getPaymentRequest().getMOBILEPHONE() == null ? "" : oneCheckout.getPaymentRequest().getMOBILEPHONE());
                paymentPageObject.setWorkphone(oneCheckout.getPaymentRequest().getWORKPHONE() == null ? "" : oneCheckout.getPaymentRequest().getWORKPHONE());

                if (oneCheckout.getPaymentRequest().getBIRTHDATE() != null) {
                    String birthDate = oneCheckout.getPaymentRequest().getBIRTHDATE();
                    paymentPageObject.setDate(birthDate.substring(6, 8));
                    paymentPageObject.setMonth(birthDate.substring(4, 6));
                    paymentPageObject.setYear(birthDate.substring(0, 4));
                }

            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization) {
                MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Tokenization);
                OneCheckoutLogger.log(":: LOAD CUSTOMER INFO ::");
                paymentPageObject.setCustomerInfo(WebUtils.getInfoCustomer(oneCheckout, mpc));
            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Recur) {
                MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Recur);
                OneCheckoutLogger.log(":: LOAD CUSTOMER INFO RECURR ::");
                paymentPageObject.setUpdateBillCardStatus(oneCheckout.getPaymentRequest().isUPDATEBILLSTATUS());
                paymentPageObject.setCustomerId(oneCheckout.getPaymentRequest().getCUSTOMERID() == null ? "" : oneCheckout.getPaymentRequest().getCUSTOMERID());
                paymentPageObject.setBillingNumber(oneCheckout.getPaymentRequest().getBILLNUMBER() == null ? "" : oneCheckout.getPaymentRequest().getBILLNUMBER());
                paymentPageObject.setCustomerInfo(WebUtils.getInfoCustomerRecur(oneCheckout, mpc));
                if (oneCheckout.getPaymentRequest().isUPDATEBILLSTATUS()) {
                    oneCheckout.getPaymentRequest().setEMAIL(paymentPageObject.getCustomerInfo().getCustomeremail() == null ? "" : paymentPageObject.getCustomerInfo().getCustomeremail());
                    oneCheckout.getPaymentRequest().setNAME(paymentPageObject.getCustomerInfo().getCustomername() == null ? "" : paymentPageObject.getCustomerInfo().getCustomername());
                    paymentPageObject.setName(paymentPageObject.getCustomerInfo().getCustomername() == null ? "" : paymentPageObject.getCustomerInfo().getCustomername());
                    paymentPageObject.setEmail(paymentPageObject.getCustomerInfo().getCustomeremail() == null ? "" : paymentPageObject.getCustomerInfo().getCustomeremail());
                    paymentPageObject.setAddress(paymentPageObject.getCustomerInfo().getCustomeraddress() == null ? "" : paymentPageObject.getCustomerInfo().getCustomeraddress());
                    paymentPageObject.setCity(paymentPageObject.getCustomerInfo().getCustomercity() == null ? "" : paymentPageObject.getCustomerInfo().getCustomercity());
                    paymentPageObject.setState(paymentPageObject.getCustomerInfo().getCustomerstate() == null ? "" : paymentPageObject.getCustomerInfo().getCustomerstate());
                    paymentPageObject.setCountry(paymentPageObject.getCustomerInfo().getCustomercountry() == null ? "" : paymentPageObject.getCustomerInfo().getCustomercountry());
                    paymentPageObject.setZipcode(paymentPageObject.getCustomerInfo().getCustomerzipcode() == null ? "" : paymentPageObject.getCustomerInfo().getCustomerzipcode());
                    paymentPageObject.setHomephone(paymentPageObject.getCustomerInfo().getCustomerhomephone() == null ? "" : paymentPageObject.getCustomerInfo().getCustomerhomephone());
                    paymentPageObject.setMobilephone(paymentPageObject.getCustomerInfo().getCustomermobilephone() == null ? "" : paymentPageObject.getCustomerInfo().getCustomermobilephone());
                    paymentPageObject.setWorkphone(paymentPageObject.getCustomerInfo().getCustomerworkphone() == null ? "" : paymentPageObject.getCustomerInfo().getCustomerworkphone());
                    List<CustomerTokenObject> customerTokenObjects = paymentPageObject.getCustomerInfo().getTokens();

                    boolean breakStatus = false;
                    for (CustomerTokenObject customerTokenObject : customerTokenObjects) {
                        for (CustomerBillObject customerBillObject : customerTokenObject.getBills()) {
                            if (customerBillObject.getBillnumber() != null && customerBillObject.getBillnumber().equals(oneCheckout.getPaymentRequest().getBILLNUMBER())) {
                                paymentPageObject.setTokenNumber(customerTokenObject.getTokenid());
                                paymentPageObject.setCardNumber(customerTokenObject.getCardnumber() == null ? "" : customerTokenObject.getCardnumber());
                                paymentPageObject.setBillingType(customerBillObject.getBilltype() == null ? "" : customerBillObject.getBilltype());
                                paymentPageObject.setBillingDetail(customerBillObject.getBilldetail() == null ? "" : customerBillObject.getBilldetail());
                                Date startDate = null;
                                try {
                                    startDate = OneCheckoutVerifyFormatData.dateFormat.parse(customerBillObject.getStartdate());
                                } catch (Throwable th) {
                                }
                                paymentPageObject.setExecuteStartDate(startDate == null ? "" : OneCheckoutVerifyFormatData.recur_dateFormat.format(startDate));
                                Date endDate = null;
                                try {
                                    endDate = OneCheckoutVerifyFormatData.dateFormat.parse(customerBillObject.getEnddate());
                                } catch (Throwable th) {
                                }
                                paymentPageObject.setExecuteEndDate(endDate == null ? "" : OneCheckoutVerifyFormatData.recur_dateFormat.format(endDate));
                                paymentPageObject.setExecuteType(customerBillObject.getExecutetype() == null ? "" : customerBillObject.getExecutetype());
                                paymentPageObject.setExecuteDate(customerBillObject.getExecutedate() == null ? "" : customerBillObject.getExecutedate());
                                paymentPageObject.setExecuteMonth(customerBillObject.getExecutemonth() == null ? "" : customerBillObject.getExecutemonth());
                                String flatStatus = customerBillObject.getFlatstatus() == null ? "" : customerBillObject.getFlatstatus();
                                paymentPageObject.setFlatStatus(flatStatus.equalsIgnoreCase("true"));
                                breakStatus = true;
                                break;
                            }
                        }
                        if (breakStatus) {
                            break;
                        }
                    }

                } else {
                    paymentPageObject.setBillingType(oneCheckout.getPaymentRequest().getBILLTYPE() == null ? "" : oneCheckout.getPaymentRequest().getBILLTYPE());
                    paymentPageObject.setBillingDetail(oneCheckout.getPaymentRequest().getBILLDETAIL() == null ? "" : oneCheckout.getPaymentRequest().getBILLDETAIL());
                    paymentPageObject.setExecuteStartDate(oneCheckout.getPaymentRequest().getSTARTDATE() == null ? "" : OneCheckoutVerifyFormatData.recur_dateFormat.format(oneCheckout.getPaymentRequest().getSTARTDATE()));
                    paymentPageObject.setExecuteEndDate(oneCheckout.getPaymentRequest().getENDDATE() == null ? "" : OneCheckoutVerifyFormatData.recur_dateFormat.format(oneCheckout.getPaymentRequest().getENDDATE()));
                    paymentPageObject.setExecuteType(oneCheckout.getPaymentRequest().getEXECUTETYPE() == null ? "" : oneCheckout.getPaymentRequest().getEXECUTETYPE());
                    paymentPageObject.setExecuteDate(oneCheckout.getPaymentRequest().getEXECUTEDATE() == null ? "" : oneCheckout.getPaymentRequest().getEXECUTEDATE());
                    paymentPageObject.setExecuteMonth(oneCheckout.getPaymentRequest().getEXECUTEMONTH() == null ? "" : oneCheckout.getPaymentRequest().getEXECUTEMONTH());
                    paymentPageObject.setCashNowStatus(oneCheckout.getPaymentRequest().isCASHNOW());
                    paymentPageObject.setFlatStatus(oneCheckout.getPaymentRequest().isFLATSTATUS());
                }

            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.KlikPayBCACard || oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.KlikPayBCADebit) {
                paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.CIMBClicks) {
                paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Kredivo) {
                paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
                //hit to MIB to getPaymentType(start)
                PaymentChannel pc = queryBeans.getPaymentChannelByChannel(OneCheckoutPaymentChannel.Kredivo);
                MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), pc);
                OneCheckoutChannelBase channelBase = new OneCheckoutChannelBase();
                String[] strParam = {"UTF-8", "&"};
                PropertiesConfiguration configapps = OneCheckoutProperties.getOneCheckoutConfig();
                String urlGetpaymentType = configapps.getString("onecheckoutv1.url.getpaymenttype", "");

                StringBuilder stringBuilder = new StringBuilder();
                try {
                    stringBuilder.append("MALLID=").append(URLEncoder.encode(mpc.getPaymentChannelCode() + "", strParam[0])).append(strParam[1]);
                    String chainMallid = mpc.getPaymentChannelChainCode() != null && mpc.getPaymentChannelChainCode() > 0 ? mpc.getPaymentChannelChainCode() + "" : "";
                    stringBuilder.append("CHAINMALLID=").append(URLEncoder.encode(chainMallid, strParam[0])).append(strParam[1]);
//                    stringBuilder.append("CHAINMALLID=").append(URLEncoder.encode(mpc.getPaymentChannelChainCode() + "", strParam[0])).append(strParam[1]);
                    stringBuilder.append("AMOUNT=").append(URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(oneCheckout.getPaymentRequest().getAMOUNT()), strParam[0])).append(strParam[1]);
                    stringBuilder.append("BASKET=").append(URLEncoder.encode(oneCheckout.getPaymentRequest().getBASKET(), strParam[0])).append(strParam[1]);
                    stringBuilder.append("ACQUIRERID=").append(URLEncoder.encode("1010", strParam[0])).append(strParam[1]);
                    stringBuilder.append("SERVICEID=").append(URLEncoder.encode("1", strParam[0])).append(strParam[1]);
                    System.out.println("SEND PARAMETER =  " + stringBuilder.toString());

                    InternetResponse response = channelBase.doFetchHTTP(stringBuilder.toString(), urlGetpaymentType, oneCheckout.getMerchant().getMerchantConnectionTimeout(), oneCheckout.getMerchant().getMerchantReadTimeout());
                    String resultGetpayment = response.getMsgResponse();
//                    String resultGetpayment = "<MIBGetPaymentTypeResponse>\n"
//                            + "  <mallId>1</mallId>\n"
//                            + "  <result>OK</result>\n"
//                            + "  <message>Available payment types are listed.</message>\n"
//                            + "  <paymentTypes>\n"
//                            + "    <paymentType>\n"
//                            + "      <id>30_days</id>\n"
//                            + "      <name>Bayar dalam 30 hari</name>\n"
//                            + "      <tenor>1</tenor>\n"
//                            + "      <rate>0</rate>\n"
//                            + "      <downPayment>0.0</downPayment>\n"
//                            + "      <monthlyInstallment>1000.00</monthlyInstallment>\n"
//                            + "      <amount>1000.00</amount>\n"
//                            + "    </paymentType>\n"
//                            + "    <paymentType>\n"
//                            + "      <id>60_days</id>\n"
//                            + "      <name>Bayar dalam 60 hari</name>\n"
//                            + "      <tenor>1</tenor>\n"
//                            + "      <rate>0</rate>\n"
//                            + "      <downPayment>0.0</downPayment>\n"
//                            + "      <monthlyInstallment>1000.00</monthlyInstallment>\n"
//                            + "      <amount>1000.00</amount>\n"
//                            + "    </paymentType>\n"
//                            + "  </paymentTypes>\n"
//                            + "</MIBGetPaymentTypeResponse>";
                    System.out.println("RESPONSE GETPAYMENT = \n" + resultGetpayment);

                    try {
                        XMLConfiguration xmlc = new XMLConfiguration();
                        StringReader sr = new StringReader(resultGetpayment);
                        xmlc.load(sr);
                        String responsegetPament = xmlc.getString("result");
                        List<String> paymentType = new ArrayList<String>();
                        if (responsegetPament.equalsIgnoreCase("OK")) {
                            List<HierarchicalConfiguration> fields
                                    = xmlc.configurationsAt("paymentTypes.paymentType");
                            for (HierarchicalConfiguration sub : fields) {
                                String a = sub.getString("id");
                                OneCheckoutLogger.log("a =* " + a);
                                paymentType.add(a);
                            }
                            paymentPageObject.setPaymentTypeKredivo(paymentType);
                            OneCheckoutLogger.log("value kredivo =* " + paymentPageObject.getPaymentTypeKredivo());
                        } else {
                            //if responseGetPaymentType is failed
                            OneCheckoutLogger.log("::: getPaymentType is FAILED :::");
                        }

                    } catch (Exception e) {
                        OneCheckoutLogger.log("::: getPaymentType is CATCH :::");
                    }

                } catch (Exception e) {
                    OneCheckoutLogger.log("error when hit to getPaymentType " + e.getMessage());
                }

                //hit to MIB to getPaymentType(end)
            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DirectDebitMandiri) {
                //page.setChallengeCode2(WebUtils.generateRandomNumber(5));
                paymentPageObject.setChallengeCode2(OneCheckoutVerifyFormatData.sdfnodecimal.format(oneCheckout.getPaymentRequest().getAMOUNT()));
                paymentPageObject.setChallengeCode3(WebUtils.generateRandomNumber(8));
            }

            // Generate Pay Code Lite
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    paymentPageObject.setPayCode(paycode);

                }
            }

            // Generate Pay Code Lite
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    paymentPageObject.setPayCode(paycode);

                }
            }
            // Generate Pay Code PTPOS
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    paymentPageObject.setPTPOSPayCode(paycode);

                }
            }

            // Generate Pay Code Full
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    paymentPageObject.setPermataFullPayCode(paycode);

                }

            }

            // Generate Pay Code Full
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    paymentPageObject.setSinarMasFullPayCode(paycode);

                }

            }

            // Generate Pay Code Mandiri SOA LITE
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    paymentPageObject.setMandiriSOALitePayCode(paycode);

                }

            }

            // Generate Pay Code Mandiri SOA FULL
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    paymentPageObject.setMandiriSOALitePayCode(paycode);

                }

            }

            // Generate Pay Code Mandiri SOA LITE
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVALite) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    paymentPageObject.setMandiriVALitePayCode(paycode);

                }

            }

            // Generate Pay Code Mandiri SOA LITE
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVAFull) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    paymentPageObject.setMandiriVAFullPayCode(paycode);

                }

            }

            // Alfamart
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    paymentPageObject.setAlfamartPayCode(paycode);
                    paymentPageObject.setPayCode(paycode);

                }
            }
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DanamonVA) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    String VainitNumber = paycode.substring(0, 8);
                    String randomPaycode = paycode.substring(8, paycode.length());
                    paymentPageObject.setVainitNumberFormat(VainitNumber);
                    paymentPageObject.setDanamonPayCode(randomPaycode);
                    paymentPageObject.setPayCode(randomPaycode);
                }
            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.CIMBVA) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    String VainitNumber = paycode.substring(0, 8);
                    String randomPaycode = paycode.substring(8, paycode.length());
                    paymentPageObject.setVainitNumberFormat(VainitNumber);
                    paymentPageObject.setCIMBVaPayCode(randomPaycode);
                    paymentPageObject.setPayCode(randomPaycode);

                }
            }
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BNIVA) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    String VainitNumber = paycode.substring(0, 8);
                    String randomPaycode = paycode.substring(8, paycode.length());
                    paymentPageObject.setVainitNumberFormat(VainitNumber);
                    paymentPageObject.setBNIVaPaycode(randomPaycode);
                    paymentPageObject.setPayCode(randomPaycode);

                }
            }

            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BRIVA) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                    String VainitNumber = paycode.substring(0, 8);
                    String randomPaycode = paycode.substring(8, paycode.length());
                    paymentPageObject.setVainitNumberFormat(VainitNumber);
                    paymentPageObject.setBriPayCode(randomPaycode);
                    paymentPageObject.setPayCode(randomPaycode);
                }
            }

            // Alfamart as switching
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.AlfaMVA) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    String VainitNumber = paycode.substring(0, 8);
                    String randomPaycode = paycode.substring(8, paycode.length());
                    paymentPageObject.setVainitNumberFormat(VainitNumber);
                    paymentPageObject.setAlfamartMVAPayCode(randomPaycode);
                    paymentPageObject.setPayCode(randomPaycode);

                }
            }

//            INDOMARET
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Indomaret) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    String VainitNumber = paycode.substring(0, 8);
                    String randomPaycode = paycode.substring(8, paycode.length());
                    paymentPageObject.setVainitNumberFormat(VainitNumber);
                    paymentPageObject.setIndomaretPayCode(randomPaycode);
                    paymentPageObject.setPayCode(randomPaycode);

                }
            }

            //permata mVA
            if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PermataMVA) {

                if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                    String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), oneCheckout.getPaymentChannel(), oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                    String VainitNumber = paycode.substring(0, 8);
                    String randomPaycode = paycode.substring(8, paycode.length());
                    paymentPageObject.setVainitNumberFormat(VainitNumber);
                    paymentPageObject.setPermataMvaPaycode(randomPaycode);
                    paymentPageObject.setPayCode(randomPaycode);

                }
            }

        } else {

            if (payChannel != null && payChannel.size() > 0) {
                for (MerchantPaymentChannel c : payChannel) {

                    OneCheckoutPaymentChannel ocoPChannel = OneCheckoutPaymentChannel.getLookup().get(c.getPaymentChannel().getPaymentChannelId());
                    channel.add(OneCheckoutPaymentChannel.getLookup().get(c.getPaymentChannel().getPaymentChannelId()).toString().trim());

                    OneCheckoutLogger.log(": : : : : CHANNEL OF MERCHANT => " + c.getPaymentChannel().getPaymentChannelId());

                    if (ocoPChannel == OneCheckoutPaymentChannel.CreditCard || ocoPChannel == OneCheckoutPaymentChannel.DOKUPAY) {
                        paymentPageObject.setName(oneCheckout.getPaymentRequest().getNAME() == null ? "" : oneCheckout.getPaymentRequest().getNAME());
                        paymentPageObject.setEmail(oneCheckout.getPaymentRequest().getEMAIL() == null ? "" : oneCheckout.getPaymentRequest().getEMAIL());
                        paymentPageObject.setAddress(oneCheckout.getPaymentRequest().getADDRESS() == null ? "" : oneCheckout.getPaymentRequest().getADDRESS());
                        paymentPageObject.setCity(oneCheckout.getPaymentRequest().getCITY() == null ? "" : oneCheckout.getPaymentRequest().getCITY());
                        paymentPageObject.setState(oneCheckout.getPaymentRequest().getSTATE() == null ? "" : oneCheckout.getPaymentRequest().getSTATE());
                        paymentPageObject.setCountry(oneCheckout.getPaymentRequest().getCOUNTRY() == null ? "" : oneCheckout.getPaymentRequest().getCOUNTRY());
                        paymentPageObject.setZipcode(oneCheckout.getPaymentRequest().getZIPCODE() == null ? "" : oneCheckout.getPaymentRequest().getZIPCODE());
                        paymentPageObject.setHomephone(oneCheckout.getPaymentRequest().getHOMEPHONE() == null ? "" : oneCheckout.getPaymentRequest().getHOMEPHONE());
                        paymentPageObject.setMobilephone(oneCheckout.getPaymentRequest().getMOBILEPHONE() == null ? "" : oneCheckout.getPaymentRequest().getMOBILEPHONE());
                        paymentPageObject.setWorkphone(oneCheckout.getPaymentRequest().getWORKPHONE() == null ? "" : oneCheckout.getPaymentRequest().getWORKPHONE());

                        if (oneCheckout.getPaymentRequest().getBIRTHDATE() != null) {
                            String birthDate = oneCheckout.getPaymentRequest().getBIRTHDATE();

                            paymentPageObject.setDate(birthDate.substring(6, 8));
                            paymentPageObject.setMonth(birthDate.substring(4, 6));
                            paymentPageObject.setYear(birthDate.substring(0, 4));
                        }

                    }

                    // Generate Challenge Code
                    if (ocoPChannel == OneCheckoutPaymentChannel.DirectDebitMandiri) {
                        //page.setChallengeCode2(WebUtils.generateRandomNumber(5));
                        paymentPageObject.setChallengeCode2(OneCheckoutVerifyFormatData.sdfnodecimal.format(oneCheckout.getPaymentRequest().getAMOUNT()));
                        paymentPageObject.setChallengeCode3(WebUtils.generateRandomNumber(8));
                    }

                    // Generate Pay Code
                    if (ocoPChannel == OneCheckoutPaymentChannel.PermataVALite) {

                        if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                            String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                            paymentPageObject.setPayCode(paycode);

                        }
                    }

                    // Generate Pay Code Full
                    if (ocoPChannel == OneCheckoutPaymentChannel.PermataVAFull) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setPermataFullPayCode(paycode);

                    }

                    // Generate Pay Code
                    if (ocoPChannel == OneCheckoutPaymentChannel.SinarMasVALite) {

                        if (oneCheckout.getPaymentRequest().getPAYCODE() == null || oneCheckout.getPaymentRequest().getPAYCODE().equalsIgnoreCase("00")) {

                            String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                            paymentPageObject.setPayCode(paycode);

                        }
                    }

                    // Generate Pay Code Full
                    if (ocoPChannel == OneCheckoutPaymentChannel.SinarMasVAFull) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setSinarMasFullPayCode(paycode);

                    }

                    // Generate Pay Code PT POS
                    if (ocoPChannel == OneCheckoutPaymentChannel.PTPOS) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setPTPOSPayCode(paycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.MandiriSOALite) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setMandiriSOALitePayCode(paycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.MandiriSOAFull) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setMandiriSOAFullPayCode(paycode);

                    }

                    // Generate Pay Code Mandiri VA LITE
                    if (ocoPChannel == OneCheckoutPaymentChannel.MandiriVALite) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setMandiriVALitePayCode(paycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.MandiriVAFull) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setMandiriVAFullPayCode(paycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.Alfamart) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        paymentPageObject.setAlfamartPayCode(paycode);

                    }
                    if (ocoPChannel == OneCheckoutPaymentChannel.DanamonVA) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        String VainitNumber = paycode.substring(0, 8);
                        String randomPaycode = paycode.substring(8, paycode.length());
                        paymentPageObject.setVainitNumberFormat(VainitNumber);
                        paymentPageObject.setDanamonPayCode(randomPaycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.BRIVA) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        String VainitNumber = paycode.substring(0, 8);
                        String randomPaycode = paycode.substring(8, paycode.length());
                        paymentPageObject.setVainitNumberFormat(VainitNumber);
                        paymentPageObject.setBriPayCode(randomPaycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.AlfaMVA) {
                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                        String VainitNumber = paycode.substring(0, 8);
                        String randomPaycode = paycode.substring(8, paycode.length());
                        paymentPageObject.setVainitNumberFormat(VainitNumber);
                        paymentPageObject.setAlfamartMVAPayCode(randomPaycode);
                    }
                    if (ocoPChannel == OneCheckoutPaymentChannel.BNIVA) {
                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                        String VainitNumber = paycode.substring(0, 8);
                        String randomPaycode = paycode.substring(8, paycode.length());
                        paymentPageObject.setVainitNumberFormat(VainitNumber);
                        paymentPageObject.setBNIVaPaycode(randomPaycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.Indomaret) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                        String VainitNumber = paycode.substring(0, 8);
                        String randomPaycode = paycode.substring(8, paycode.length());
                        paymentPageObject.setVainitNumberFormat(VainitNumber);
                        paymentPageObject.setIndomaretPayCode(randomPaycode);
                        paymentPageObject.setPayCode(randomPaycode);
                    }
                    if (ocoPChannel == OneCheckoutPaymentChannel.PermataMVA) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());
                        String VainitNumber = paycode.substring(0, 8);
                        String randomPaycode = paycode.substring(8, paycode.length());
                        paymentPageObject.setVainitNumberFormat(VainitNumber);
                        paymentPageObject.setPermataMvaPaycode(randomPaycode);

                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.CIMBVA) {

                        String paycode = queryBeans.generatePaymentCodeByMerchant(oneCheckout.getMerchant(), ocoPChannel, oneCheckout.getPaymentRequest().getTRANSIDMERCHANT(), oneCheckout.getPaymentRequest().getWORDS(), oneCheckout.getPaymentRequest().getVAOpenAmount());

                        String VainitNumber = paycode.substring(0, 8);
                        String randomPaycode = paycode.substring(8, paycode.length());
                        paymentPageObject.setVainitNumberFormat(VainitNumber);
                        paymentPageObject.setCIMBVaPayCode(randomPaycode);

                    }

                    //                    if (c.getPaymentChannel().getPaymentChannelId().equalsIgnoreCase(OneCheckoutPaymentChannel.Recur.value())) {
                    //                        MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Recur);
                    //                        OneCheckoutLogger.log(":: LOAD CUSTOMER INFO RECURR ::");
                    //                        page.setCustomerInfo(WebUtils.getInfoCustomerRecur(oneCheckout, mpc));
                    //                    }
                    if (ocoPChannel == OneCheckoutPaymentChannel.KlikPayBCACard || oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.KlikPayBCADebit) {
                        if (oneCheckout.getPaymentRequest().getADDITIONALINFO() != null && !oneCheckout.getPaymentRequest().getADDITIONALINFO().equals("null")) {
                            HashMap<String, String> paramKlikPayBCA = new HashMap<String, String>();
                            paramKlikPayBCA = stringToHashMap(oneCheckout.getPaymentRequest().getADDITIONALINFO());
                            paymentPageObject.setInvoiceNo(paramKlikPayBCA.get("INVOICENUMBER"));
                        } else {
                            paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
                        }
                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.CIMBClicks) {
                        if (oneCheckout.getPaymentRequest().getADDITIONALINFO() != null && !oneCheckout.getPaymentRequest().getADDITIONALINFO().equals("null")) {
                            HashMap<String, String> ParamCimbClicks = new HashMap<String, String>();
                            ParamCimbClicks = stringToHashMap(oneCheckout.getPaymentRequest().getADDITIONALINFO());
                            paymentPageObject.setInvoiceNo(ParamCimbClicks.get("INVOICENUMBER"));
                        } else {
                            paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
                        }
                    }

                    if (ocoPChannel == OneCheckoutPaymentChannel.Kredivo) {
//                        HashMap<String, String> paramKredivo = new HashMap<String, String>();
//                        paramKredivo = stringToHashMap(oneCheckout.getPaymentRequest().getADDITIONALINFO());
//                        paymentPageObject.setInvoiceNo(paramKredivo.get("INVOICENUMBER"));
                        paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
                        //hit to MIB to getPaymentType(start)
                        PaymentChannel pc = queryBeans.getPaymentChannelByChannel(OneCheckoutPaymentChannel.Kredivo);
                        MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), pc);
                        OneCheckoutChannelBase channelBase = new OneCheckoutChannelBase();
                        String[] strParam = {"UTF-8", "&"};
                        PropertiesConfiguration configapps = OneCheckoutProperties.getOneCheckoutConfig();
                        String urlGetpaymentType = configapps.getString("onecheckoutv1.url.getpaymenttype", "");

                        StringBuilder stringBuilder = new StringBuilder();
                        try {
                            stringBuilder.append("MALLID=").append(URLEncoder.encode(mpc.getPaymentChannelCode() + "", strParam[0])).append(strParam[1]);
                            String chainMallid = mpc.getPaymentChannelChainCode() != null && mpc.getPaymentChannelChainCode() > 0 ? mpc.getPaymentChannelChainCode() + "" : "";
                            stringBuilder.append("CHAINMALLID=").append(URLEncoder.encode(chainMallid, strParam[0])).append(strParam[1]);
                            OneCheckoutLogger.log("CHAIN MALLID KREDIVO = " + chainMallid);
                            stringBuilder.append("AMOUNT=").append(URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(oneCheckout.getPaymentRequest().getAMOUNT()), strParam[0])).append(strParam[1]);
                            stringBuilder.append("BASKET=").append(URLEncoder.encode(oneCheckout.getPaymentRequest().getBASKET(), strParam[0])).append(strParam[1]);
                            stringBuilder.append("ACQUIRERID=").append(URLEncoder.encode("1010", strParam[0])).append(strParam[1]);
                            stringBuilder.append("SERVICEID=").append(URLEncoder.encode("1", strParam[0])).append(strParam[1]);
                            System.out.println("SEND PARAMETER =  " + stringBuilder.toString());

                            InternetResponse response = channelBase.doFetchHTTP(stringBuilder.toString(), urlGetpaymentType, oneCheckout.getMerchant().getMerchantConnectionTimeout(), oneCheckout.getMerchant().getMerchantReadTimeout());
                            String resultGetpayment = response.getMsgResponse();
                            System.out.println("RESPONSE GETPAYMENT = \n" + resultGetpayment);

                            try {
                                XMLConfiguration xmlc = new XMLConfiguration();
                                StringReader sr = new StringReader(resultGetpayment);
                                xmlc.load(sr);
                                String responsegetPament = xmlc.getString("result");
                                List<String> paymentType = new ArrayList<String>();
                                if (responsegetPament.equalsIgnoreCase("OK")) {
                                    List<HierarchicalConfiguration> fields
                                            = xmlc.configurationsAt("paymentTypes.paymentType");
                                    for (HierarchicalConfiguration sub : fields) {
                                        String a = sub.getString("id");
                                        paymentType.add(a);
                                    }
                                    paymentPageObject.setPaymentTypeKredivo(paymentType);
                                } else {
                                    //if responseGetPaymentType is failed
                                    OneCheckoutLogger.log("::: getPaymentType is FAILED :::");
                                }

                            } catch (Exception e) {
                                OneCheckoutLogger.log("::: getPaymentType is CATCH :::");
                            }

                        } catch (Exception e) {
                            OneCheckoutLogger.log("error when hit to getPaymentType " + e.getMessage());
                        }

                        //hit to MIB to getPaymentType(end)
                    }

                    if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Muamalat) {
                        if (oneCheckout.getPaymentRequest().getADDITIONALINFO() != null && !oneCheckout.getPaymentRequest().getADDITIONALINFO().equals("null")) {
                            HashMap<String, String> paramMuamalat = new HashMap<String, String>();
                            paramMuamalat = stringToHashMap(oneCheckout.getPaymentRequest().getADDITIONALINFO());
                            paymentPageObject.setInvoiceNo(paramMuamalat.get("INVOICENUMBER"));
                        } else {
                            paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
                        }

                    }

                    if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Danamon) {
                        if (oneCheckout.getPaymentRequest().getADDITIONALINFO() != null && !oneCheckout.getPaymentRequest().getADDITIONALINFO().equals("null")) {
                            HashMap<String, String> paramDanamon = new HashMap<String, String>();
                            paramDanamon = stringToHashMap(oneCheckout.getPaymentRequest().getADDITIONALINFO());
                            paymentPageObject.setInvoiceNo(paramDanamon.get("INVOICENUMBER"));
                        } else {
                            paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
                        }
                    }

                    if (oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DompetKu) {
                        if (oneCheckout.getPaymentRequest().getADDITIONALINFO() != null && !oneCheckout.getPaymentRequest().getADDITIONALINFO().equals("null")) {
                            HashMap<String, String> paramDompetKu = new HashMap<String, String>();
                            paramDompetKu = stringToHashMap(oneCheckout.getPaymentRequest().getADDITIONALINFO());
                            paymentPageObject.setInvoiceNo(paramDompetKu.get("INVOICENUMBER"));
                        } else {
                            paymentPageObject.setInvoiceNo(oneCheckout.getPaymentRequest().getTRANSIDMERCHANT());
                        }
                    }
                }
            }
        }
        paymentPageObject.setChannel(channel);
        oneCheckout.setPaymentPageObject(paymentPageObject);
        OneCheckoutV1TransactionBeanLocal prepareTransBean = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1TransactionBean.class);
        Transactions trans = prepareTransBean.saveTransactions(oneCheckout, null);
        oneCheckout.setTrxId(trans.getTransactionsId());
        oneCheckout.setOcoId(trans.getOcoId());
        /*
         if (oneCheckout.getPaymentChannel() != null && oneCheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Recur && oneCheckout.getPaymentRequest().isUPDATEBILLSTATUS()) {
         } else {
         OneCheckoutV1TransactionBeanLocal prepareTransBean = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1TransactionBean.class);
         Transactions trans = prepareTransBean.saveTransactions(oneCheckout, null);
         oneCheckout.setTrxId(trans.getTransactionsId());
         }
         */
        return oneCheckout;
    }

    public static OneCheckoutDataHelper prosesInstallmentData(OneCheckoutDataHelper onecheckout, HashMap<String, String> params, String paramFlag) {
        try {
            OneCheckoutPaymentRequest paymentRequest = onecheckout.getPaymentRequest();
            paymentRequest.setINSTALLMENTCODE(params.remove("INSTALLMENTCODE"));
            if (paramFlag != null) {
                paymentRequest.setINSTALLMENTSTATUS(paramFlag);
            }
            onecheckout.setPaymentRequest(paymentRequest);
            return onecheckout;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("WebUtils.prosesInstallmentData : ERROR2 : %s", ex.getMessage());
            onecheckout.setMessage(ex.getMessage());
            return onecheckout;
        }
    }

    public static OneCheckoutDataHelper prosesInstallmentAndRewardsData(OneCheckoutDataHelper onecheckout, HashMap<String, String> params, String installmentParamFlag, String rewardsParamFlag) {
        try {
            OneCheckoutPaymentRequest paymentRequest = onecheckout.getPaymentRequest();
            paymentRequest.setINSTALLMENTCODE(params.remove("INSTALLMENTCODE"));
            if (installmentParamFlag != null) {
                paymentRequest.setINSTALLMENTSTATUS(installmentParamFlag);
            }
            if (rewardsParamFlag != null) {
                paymentRequest.setREDEMPTIONSTATUS(rewardsParamFlag);
            }
            onecheckout.setPaymentRequest(paymentRequest);
            return onecheckout;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("WebUtils.prosesInstallmentandRewardsData : ERROR2 : %s", ex.getMessage());
            onecheckout.setMessage(ex.getMessage());
            return onecheckout;
        }
    }

    public static OneCheckoutDataHelper prosesRewardsData(OneCheckoutDataHelper onecheckout, HashMap<String, String> params, String rewardsParamFlag) {
        try {
            OneCheckoutPaymentRequest paymentRequest = onecheckout.getPaymentRequest();

            if (rewardsParamFlag != null) {
                paymentRequest.setREDEMPTIONSTATUS(rewardsParamFlag);
            }
            onecheckout.setPaymentRequest(paymentRequest);
            return onecheckout;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("WebUtils.prosesInstallmentandRewardsData : ERROR2 : %s", ex.getMessage());
            onecheckout.setMessage(ex.getMessage());
            return onecheckout;
        }
    }

    public static OneCheckoutDataHelper prosesCIPDOKUWalletData(OneCheckoutDataHelper oneCheckoutDataHelper, HashMap<String, String> params, HttpServletRequest request) {
        try {
            OneCheckoutLogger.log("WebUtils.prosesCIPDOKUWalletData : Parsing Input Parameters");
            oneCheckoutDataHelper.getPaymentRequest().setDOKUWALLETCHANNEL(params.remove("DOKUWALLETCHANNEL"));
            oneCheckoutDataHelper.getPaymentRequest().setCASHWALLETPIN(params.remove("CASHWALLETPIN"));
            oneCheckoutDataHelper.getPaymentRequest().setDWCREDITCARD(params.remove("DWCREDITCARD"));
            oneCheckoutDataHelper.getPaymentRequest().setDWCVV2(params.remove("DWCVV2"));
            oneCheckoutDataHelper.getPaymentRequest().setDWTCASHACCOUNTNUMBER(params.remove("DWTCASHACCOUNTNUMBER"));
            oneCheckoutDataHelper.getPaymentRequest().setDWTCASHTOKENNUMBER(params.remove("DWTCASHTOKENNUMBER"));

//            oneCheckoutDataHelper.getPaymentRequest().setPROMOTIONID(params.remove("PROMOTIONID"));
//            oneCheckoutDataHelper.getPaymentRequest().setFLAGDOKU(params.remove("FLAGDOKU"));   
//            oneCheckoutDataHelper.getPaymentRequest().setAMOUNTAFTERPROMOTION(params.remove("AMOUNTAFTERPROMOTION"));
            oneCheckoutDataHelper.setMessage("VALID");
            OneCheckoutLogger.log("WebUtils.prosesCIPDOKUWalletData : end");
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public static OneCheckoutDataHelper prosesCIPRequestData(OneCheckoutDataHelper onecheckout, HashMap<String, String> params, HttpServletRequest request) {

        try {

            OneCheckoutPaymentRequest paymentRequest = onecheckout.getPaymentRequest();
            OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing Input Parameters");
            String paymentChannel = params.remove("PAYMENTCHANNEL");
            OneCheckoutPaymentChannel pchannel = OneCheckoutBaseRules.validatePaymentChannel(paymentChannel);

            String ipaddress = request.getHeader("True-Client-IP");
            if (ipaddress == null) {
                ipaddress = request.getHeader("X-Forwarded-For");
            }
            if (ipaddress == null) {
                ipaddress = request.getRemoteAddr();
            }
            onecheckout.setSystemSession(ipaddress + "|" + request.getHeader("User-Agent") + "|" + request.getSession().getId());
            OneCheckoutLogger.log("WebUtils.prosesCIPRequestData SYSTEM_SESSION : %s", onecheckout.getSystemSession());
            OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing Input Parameters");
//            onecheckout.setSystemSession(request.getSession().getId());
            onecheckout.setFlag(params.remove("tombol"));

            if (pchannel == null) {
                // CIP without channel selected
                // multi payment page display
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : pchannel null");
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Undefined Payment Channel, give multi payment page");
                onecheckout.setPaymentRequest(paymentRequest);
                onecheckout.setMessage("VALID");
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : end");
                return onecheckout;
            }
            onecheckout.setPaymentChannel(pchannel);
            OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(onecheckout.getMerchant(), pchannel);

            onecheckout.setPaymentChannelCode(mpc.getPaymentChannelCode());
            OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Channel " + pchannel);
            if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.CreditCard || onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BSP || onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MOTO) {

                /*
                 *
                 *
                 // Payment Account Information
                 private String CARDNUMBER;
                 private String EXPIRYDATE;
                 private String CVV2;
                 private String INSTALLMENT_ACQUIRER;
                 private String TENOR;
                 private String PROMOID;
                 *
                 // CH Information
                 private String CC_NAME;
                 private String ADDRESS;
                 private String CITY;
                 private String STATE;
                 private String COUNTRY;
                 private String ZIPCODE;
                 private String HOMEPHONE;
                 private String MOBILEPHONE;
                 private String WORKPHONE;
                 private String BIRTHDATE;

                 */
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing Visa/Master Parameters");
                paymentRequest.setCARDNUMBER(params.remove("CARDNUMBER"));

                String expiryDate = params.remove("YEAR") + params.remove("MONTH");
                paymentRequest.setEXPIRYDATE(expiryDate);
                if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MOTO) {
                    paymentRequest.setCVV2withEmpty();
                } else {
                    paymentRequest.setCVV2(params.remove("CVV2"));
                }

                /*paymentRequest.setINSTALLMENT_ACQUIRER(params.remove("INSTALLMENT_ACQUIRER"));
                 paymentRequest.setTENOR(params.remove("TENOR"));
                 paymentRequest.setPROMOID(params.remove("PROMOID"));*/
//dfd
                paymentRequest.setCC_NAME(params.remove("CC_NAME"));
                paymentRequest.setEMAIL(params.remove("EMAIL"));
                paymentRequest.setADDRESS(params.remove("ADDRESS"));
                paymentRequest.setCITY(params.remove("CITY"));
                paymentRequest.setSTATE(params.remove("STATE"));
                paymentRequest.setCOUNTRY(params.remove("COUNTRY"));
                paymentRequest.setZIPCODE(params.remove("ZIPCODE"));
                paymentRequest.setHOMEPHONE(params.remove("HOMEPHONE"));
                paymentRequest.setMOBILEPHONE(params.remove("MOBILEPHONE"));
                paymentRequest.setWORKPHONE(params.remove("WORKPHONE"));
                paymentRequest.setBIRTHDATE(params.remove("BYEAR") + params.remove("BMONTH") + params.remove("BDATE"));
                paymentRequest.setDEVICEID(params.remove("DEVICEID"));

                String acqCode = params.remove("INSTALLMENT_ACQUIRER");
                if (acqCode != null && !acqCode.isEmpty()) {
                    paymentRequest.setINSTALLMENT_ACQUIRER(acqCode);
                }

                String tenor = params.remove("TENOR");
                if (tenor != null && !tenor.isEmpty()) {
                    paymentRequest.setTENOR(tenor);
                }

                String promoId = params.remove("PROMOID");
                if (promoId != null && !promoId.isEmpty()) {
                    paymentRequest.setPROMOID(promoId);
                }

                String installmentCode = params.remove("IDSS");
                OneCheckoutLogger.log("INSTALLMENT_CODE[" + installmentCode + "]");
                if (installmentCode != null && !installmentCode.trim().equals("")) {
                    String installmentSetting = "";
                    try {
                        installmentSetting = decrypt(installmentCode);
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                    OneCheckoutLogger.log("INSTALLMENT_SETTING[" + installmentSetting + "]");
                    if (installmentSetting != null && !installmentSetting.trim().equals("")) {
                        paymentRequest.setINSTALLMENT_ACQUIRER(installmentSetting.substring(0, 3));
                        paymentRequest.setPROMOID(installmentSetting.substring(3, 6));
                        paymentRequest.setTENOR(installmentSetting.substring(6, 8));
                    }
                }

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization) {
                paymentRequest.setCVV2(params.remove("CVV2_pay"));
                paymentRequest.setTOKENID(params.remove("TOKENID"));
                if (params.containsKey("CUSTOMERID")) {
                    OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Tokenization CUSTOMERID");
                    paymentRequest.setCUSTOMERID(params.remove("CUSTOMERID"));
                    if (params.containsKey("CUSTOMERTYPE")) {
                        paymentRequest.setCUSTOMERTYPE(params.remove("CUSTOMERTYPE"));
                    } else {
                        paymentRequest.setCUSTOMERTYPE("C");
                    }
                }
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BNIDebitOnline) {

                /*
                 *Payment Account Information
                 */
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing BNI Debit Online Parameters");

                paymentRequest.setCARDNUMBER(params.remove("CARDNUMBER"));

                String expiryDate = params.remove("YEAR") + params.remove("MONTH");

                paymentRequest.setEXPIRYDATE(expiryDate);
                paymentRequest.setCVV2(params.remove("CVV2"));

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DirectDebitMandiri) {

                /*
                 *
                 *
                 // Payment Account Information
                 private String CARDNUMBER;
                 private String CHALLENGE_CODE_1;
                 private String CHALLENGE_CODE_2;
                 private String CHALLENGE_CODE_3;
                 private String RESPONSE_TOKEN;

                 */
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing ClickPay Mandiri Parameters");
                paymentRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                paymentRequest.setCHALLENGE_CODE_1(params.remove("CHALLENGE_CODE_1"));
                paymentRequest.setCHALLENGE_CODE_2(params.remove("CHALLENGE_CODE_2"));
                paymentRequest.setCHALLENGE_CODE_3(params.remove("CHALLENGE_CODE_3"));
                paymentRequest.setRESPONSE_TOKEN(params.remove("RESPONSE_TOKEN"));
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.KlikBCA) {

                /*
                 Payment Account Information
                 private String USERIDKLIKBCA;
                 */
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing KlikBCA Parameters");
                paymentRequest.setUSERIDKLIKBCA(params.remove("USERIDKLIKBCA").toUpperCase());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : USERIDKLIKBCA  " + paymentRequest.getUSERIDKLIKBCA());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {
                /*
                 // Payment Account Information
                 private String DOKUPAYID;
                 */
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing Dokupay Parameters");
                paymentRequest.setDOKUPAYID(params.remove("DOKUPAYID"));
                paymentRequest.setSECURITYCODE(params.remove("SECURITYCODE"));

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : DOKUPAYID  " + paymentRequest.getDOKUPAYID());

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing PermataVALite Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : - end set pay code");
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing SinarMasVALite Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing PT POS Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing PermataVAFull Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing SinarMasVAFull Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing MandiriSOALite Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing MandiriSOAFull Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVALite) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing MandiriVALite Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVAFull) {
                /*
                 // Payment Account Information
                 private String PAYCODE;
                 */

                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing VA Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing Alfamart Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BRIVA) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing BRI VA Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                paymentRequest.setVAinitNumberFormat(params.remove("VAINITNUMBERFORMAT"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : VaInitNUmber " + paymentRequest.getVAinitNumberFormat());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DanamonVA) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing DanamonVA Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                paymentRequest.setVAinitNumberFormat(params.remove("VAINITNUMBERFORMAT"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : VAINITNUMBERFORMAT  " + paymentRequest.getVAinitNumberFormat());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.CIMBVA) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing CIMB-VA Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                paymentRequest.setVAinitNumberFormat(params.remove("VAINITNUMBERFORMAT"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : VAINITNUMBERFORMAT  " + paymentRequest.getVAinitNumberFormat());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.AlfaMVA) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing AlfamartMVA Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                paymentRequest.setVAinitNumberFormat(params.remove("VAINITNUMBERFORMAT"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : VaInitNUmber " + paymentRequest.getVAinitNumberFormat());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Indomaret) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing indomaret Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                paymentRequest.setVAinitNumberFormat(params.remove("VAINITNUMBERFORMAT"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : VaInitNUmber " + paymentRequest.getVAinitNumberFormat());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.PermataMVA) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing PermataMva Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                paymentRequest.setVAinitNumberFormat(params.remove("VAINITNUMBER"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : VAINITNUMBER  " + paymentRequest.getVAinitNumberFormat());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Recur) {
                OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : Parsing Recur Parameters");
                String cardType = params.get("CARDTYPE") != null ? params.get("CARDTYPE").trim() : "";
                if (cardType.equals("EXISTINGCARD")) {
                    paymentRequest.setTOKENID(params.remove("TOKENID"));
                    if (!paymentRequest.isUPDATEBILLSTATUS()) {
                        paymentRequest.setCVV2(params.remove("CVV2TOKEN"));
                    }
                    OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : USING TOKENID  " + paymentRequest.getTOKENID());
                    CustomerInfoObject ci = WebUtils.getInfoCustomerRecurbyToken(onecheckout, mpc, paymentRequest.getTOKENID());
                    CustomerTokenObject credentialData = null;
                    if (ci.getTokens().size() > 0) {
                        credentialData = ci.getTokens().iterator().next();
                        paymentRequest.setCARDNUMBER(credentialData.getCardnumber());
                        paymentRequest.setEXPIRYDATE(credentialData.getExpirydate());
                        //   paymentRequest.setCVV2(params.remove("CVV2"));
                        paymentRequest.setCC_NAME(credentialData.getCcname());
                        paymentRequest.setADDRESS(credentialData.getCcaddress());
                        paymentRequest.setEMAIL(credentialData.getCcemail());
                        paymentRequest.setCITY(credentialData.getCccity());
                        if (!credentialData.getCcstate().equals("")) {
                            paymentRequest.setSTATE(credentialData.getCcstate());
                        }
                        paymentRequest.setZIPCODE(credentialData.getCczipcode());
                        paymentRequest.setCOUNTRY(credentialData.getCccountry());
                        if (!credentialData.getCcmobilephone().equals("")) {
                            paymentRequest.setMOBILEPHONE(credentialData.getCcmobilephone());
                        }
                        paymentRequest.setHOMEPHONE(credentialData.getCchomephone());
                    }
                } else if (cardType.equals("NEWCARD")) {
                    OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Without TOKENID");
                    paymentRequest.setCARDNUMBER(params.remove("CARDNUMBER"));
                    paymentRequest.setEXPIRYDATE(params.remove("YEAR") + params.remove("MONTH"));
                    paymentRequest.setCVV2(params.remove("CVV2"));
                    paymentRequest.setCC_NAME(params.remove("CC_NAME"));
                    paymentRequest.setADDRESS(params.remove("ADDRESS"));
                    paymentRequest.setEMAIL(params.remove("EMAIL"));
                    paymentRequest.setCITY(params.remove("CITY"));
                    paymentRequest.setSTATE(params.remove("STATE"));
                    paymentRequest.setZIPCODE(params.remove("ZIPCODE"));
                    paymentRequest.setCOUNTRY(params.remove("COUNTRY"));
                    paymentRequest.setMOBILEPHONE(params.remove("MOBILEPHONE"));
                    paymentRequest.setHOMEPHONE(params.remove("HOMEPHONE"));
                }

                OneCheckoutLogger.log("==========Detail TOKEN PARAMETER===========");
                OneCheckoutLogger.log("CARDNUMBER   : " + paymentRequest.getCARDNUMBER());
                OneCheckoutLogger.log("EXPIRYDATE   : " + paymentRequest.getEXPIRYDATE());
                OneCheckoutLogger.log("CVV2         : " + "CVV2");//paymentRequest.getCVV2());
                OneCheckoutLogger.log("CC_NAME      : " + paymentRequest.getCC_NAME());
                OneCheckoutLogger.log("ADDRESS      : " + paymentRequest.getADDRESS());
                OneCheckoutLogger.log("EMAIL        : " + paymentRequest.getEMAIL());
                OneCheckoutLogger.log("CITY         : " + paymentRequest.getCITY());
                OneCheckoutLogger.log("STATE        : " + paymentRequest.getSTATE());
                OneCheckoutLogger.log("ZIPCODE      : " + paymentRequest.getZIPCODE());
                OneCheckoutLogger.log("COUNTRY      : " + paymentRequest.getCOUNTRY());
                OneCheckoutLogger.log("MOBILEPHONE  : " + paymentRequest.getMOBILEPHONE());
                OneCheckoutLogger.log("HOMEPHONE    : " + paymentRequest.getHOMEPHONE());
                OneCheckoutLogger.log("========Detail TOKEN PARAMETER=========");

            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.KlikPayBCACard || onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.KlikPayBCADebit) {
                OneCheckoutLogger.log("==========Detail KlikPayBCA Parameter===========");
//                paymentRequest.setACQUIRERID(params.remove("ACQUIRERID"));
//                paymentRequest.setINVOICENUMBER(params.remove("INVOICENUMBER"));
//                paymentRequest.setDESCRIPTION(params.remove("DESCRIPTION"));

                OneCheckoutLogger.log("ADDITIONALDATA: " + paymentRequest.getADDITIONALINFO());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.CIMBClicks) {
                OneCheckoutLogger.log("==========Detail CIMB Clicks Parameter===========");
//                paymentRequest.setACQUIRERID(params.remove("ACQUIRERID"));
//                paymentRequest.setINVOICENUMBER(params.remove("INVOICENUMBER"));
//                paymentRequest.setDESCRIPTION(params.remove("DESCRIPTION"));

                OneCheckoutLogger.log("ADDITIONALDATA: " + paymentRequest.getADDITIONALINFO());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Muamalat) {
                OneCheckoutLogger.log("===========Detail Muamalat Clicks Parameter===========");
                OneCheckoutLogger.log("ADDITIONALDATA: " + paymentRequest.getADDITIONALINFO());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Danamon) {
                OneCheckoutLogger.log("===========Detail Danamon Clicks Parameter===========");
                OneCheckoutLogger.log("ADDITIONALDATA: " + paymentRequest.getADDITIONALINFO());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.DompetKu) {
                OneCheckoutLogger.log("===========Detail DompetKu Clicks Parameter===========");
                paymentRequest.setMSISDN(params.remove("MSISDN"));
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BNIVA) {
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : Parsing BNIVA Parameters");
                paymentRequest.setPAYCODE(params.remove("PAYCODE"));
                paymentRequest.setVAinitNumberFormat(params.remove("VAINITNUMBERFORMAT"));
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : PAYCODE  " + paymentRequest.getPAYCODE());
                OneCheckoutLogger.log("WebUtils.prosesCIPRequestData : VAINITNUMBER  " + paymentRequest.getVAinitNumberFormat());
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Kredivo) {
                OneCheckoutLogger.log("===========Detail Kredivo Parameter===========");
                OneCheckoutLogger.log("ADDITIONALDATA : " + paymentRequest.getADDITIONALINFO());
                OneCheckoutLogger.log("paymentMethode : " + params.get("PAYMENTMETHODE"));
                paymentRequest.setPaymentTypeKredivo(params.remove("PAYMENTMETHODE"));
            }

            onecheckout.setPaymentRequest(paymentRequest);
            onecheckout.setMessage("VALID");
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : end");
            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            OneCheckoutLogger.log("WebUtils.parsePaymentRequestData : ERROR2 : %s", e.getMessage());
            onecheckout.setMessage(e.getMessage());
            return onecheckout;

        }
    }

//    public static List<OneCheckoutBasket> getBasket(String basket) {
//        List<OneCheckoutBasket> al = new ArrayList();
//        OneCheckoutBasket b;
//        //String[] datas = basket.split(";;");
//        String[] datas = basket.split(";");
//        if (datas.length > 0) {
//            for (String data : datas) {  sdsd
//                //String[] items = data.split("\\|\\|");
//                String[] items = data.split(",");
//               if (items.length >= 3) {
//                    b = new OneCheckoutBasket();
//                    b.setItemName(ESAPI.encoder().encodeForHTML(items[0]));
//                    b.setItemQuantity(Integer.valueOf(items[2]));
//                    b.setItemPrice(BigDecimal.valueOf(Double.valueOf(items[1])));
//                    al.add(b);
//                } else {
//                    System.out.println(": : : ITEM IS EMPTY");
//                }
//            }
//        } else {
//            System.out.println(": : : BASKET IS EMPTY");
//        }
//        return al;
//    }
    public static String generateRandomNumber(int length) {
        String result = "";
        try {
            double rnd;
            for (int i = 0; i < length; i++) {
                rnd = Math.random() * numberChar.length;
                int rnd1 = (int) rnd;
                result += numberChar[(rnd1)];
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return result;
    }

    public static OneCheckoutDataHelper parseCheckStatusRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {
            Merchants m = null;
            String mallId = params.remove("MALLID");
            try {
                int mId = OneCheckoutBaseRules.validateMALLID2(mallId);
                OneCheckoutLogger.log("WebUtils.parseCheckStatusRequestData : MALLID=%d", mId);
                String chainMerchantId = params.remove("CHAINMERCHANT");
                int cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);

                OneCheckoutLogger.log("WebUtils.parseCheckStatusRequestData : CHAINMERCHANT=%d", cmId);
                onecheckout.setMallId(mId);
                onecheckout.setChainMerchantId(cmId);

                OneCheckoutLogger.log("DEBUG 1");

                if (cmId != 0) {
                    m = WebUtils.getValidMALLID(mId, cmId);
                } else {
                    m = WebUtils.getValidMALLID(mId);
                }
                OneCheckoutLogger.log("DEBUG 2");

                if (m == null) {
                    OneCheckoutLogger.log("WebUtils.parseCheckStatusRequestData : ERROR : MALLID %s not found", mallId);
                    return onecheckout;
                }
                onecheckout.setMerchant(m);
            } catch (Exception e) {
                e.printStackTrace();
                OneCheckoutLogger.log("WebUtils.parseCheckStatusRequestData : ERROR : %s", e.getMessage());
                onecheckout.setMessage(e.getMessage());
                onecheckout.setMallId(0);
                onecheckout.setChainMerchantId(0);
                onecheckout.setMerchant(null);
                return onecheckout;
            }

            OneCheckoutCheckStatusData checkStatusRequest = new OneCheckoutCheckStatusData(mallId);

            checkStatusRequest.setSESSIONID(params.remove("SESSIONID"));
            checkStatusRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
            checkStatusRequest.setWORDS(params.remove("WORDS"));

            OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class
            );

            Transactions trx = queryBeans.getCheckStatusTransactionByInvoiceSession(onecheckout.getMerchant(), checkStatusRequest.getTRANSIDMERCHANT(), checkStatusRequest.getSESSIONID());

            if (trx
                    == null) {
                OneCheckoutLogger.log("WebUtils.parseCheckStatusRequestData : ERROR : TRX %s - %s not found", checkStatusRequest.getTRANSIDMERCHANT(), checkStatusRequest.getSESSIONID());

                OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();
                String ack = oneCheckoutNotifyStatusRequest.toErrorCheckStatusString(OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND.name(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND.value());
                checkStatusRequest.setACKNOWLEDGE(ack);
                onecheckout.setCheckStatusRequest(checkStatusRequest);
                return onecheckout;
            } else if (trx.getDokuResponseCode()
                    != null && (trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.PAYMENTCHANNEL_NOT_REGISTERED.value())
                    || trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.PAYMENTCHANNEL_MERCHANT_DISABLED.value())
                    || trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.MAXIMUM_3_TIMES_ATTEMPT.value())
                    || trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH.value())
                    || trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.ERROR_VALIDATE_INPUT.value())
                    || trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.RE_ENTER_TRANSACTION.value())
                    || trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.EXPIRED_TIME_EXCEED.value()))) {

                OneCheckoutErrorMessage errMsg = OneCheckoutErrorMessage.findType(trx.getDokuResponseCode());

                OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();
                String ack = oneCheckoutNotifyStatusRequest.toErrorCheckStatusString("FAILED", errMsg.value());
                checkStatusRequest.setACKNOWLEDGE(ack);
                onecheckout.setCheckStatusRequest(checkStatusRequest);
                return onecheckout;
            } else if (trx.getDokuResponseCode()
                    != null && trx.getDokuResponseCode().equals(OneCheckoutErrorMessage.CANCEL_BY_USER.value())) {

                OneCheckoutErrorMessage errMsg = OneCheckoutErrorMessage.findType(trx.getDokuResponseCode());
                OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();

                String ack = oneCheckoutNotifyStatusRequest.toErrorCheckStatusString(errMsg.name(), errMsg.value());
                checkStatusRequest.setACKNOWLEDGE(ack);
                onecheckout.setCheckStatusRequest(checkStatusRequest);
                return onecheckout;

                /*                
                 UNKNOWN("5555"),

                 PAYMENTCHANNEL_NOT_REGISTERED("5501"),
                 PAYMENTCHANNEL_MERCHANT_DISABLED("5502"),
                 MAXIMUM_3_TIMES_ATTEMPT("5503"),
                 WORDS_DOES_NOT_MATCH("5504"),
                 ERROR_VALIDATE_INPUT("5505"),
                 NOTIFY_FAILED("5506"),
                 PAYMENT_HAS_NOT_BEEN_PROCCED("5507"),
                 RE_ENTER_TRANSACTION("5508"),//payment has not been initiated;
                 EXPIRED_TIME_EXCEED("5509"),
                 CANCEL_BY_USER("5510"),
                 NOT_YET_PAID("5511"),
                 INSUFFICIENT_PARAMS("5512"),
                 VOID_BY_COREPAYMENT("5513"),
                 HIGHRISK_OR_REJECT("5514"),
                 DUPLICATE_PNR("5515"),
                 TRANSACTION_NOT_FOUND("5516"),
                 ERROR_CONNECT_TO_CORE("5517"),
                 ERROR_PARSING_RESPONSE("5518"),
                 NOT_CONTINUE_FROM_ACS("5519"),
                 FAILED_REGISTER_ACCOUNT_BILLING("5520"),
                 INVALID_MERCHANT("5521"),
                 RATE_NOT_FOUND("5522"),
                 CANNOT_GET_CHECKSTATUS("5523"),
                 SUCCESS("0000"),
                 INIT_VA("INIT");                
                
                 */
            }
            OneCheckoutPaymentChannel pchannel = OneCheckoutBaseRules.validatePaymentChannel(trx.getIncPaymentchannel());

            onecheckout.setTransactions(trx);

            onecheckout.setPaymentChannel(pchannel);

            /*OneCheckoutLogger.log("DEBUG 1");
             Merchants m = WebUtils.getValidMALLID(checkStatusRequest.getMALLID());
             OneCheckoutLogger.log("DEBUG 2");

             //onecheckout.setMerchant(m);*/
            onecheckout.setCheckStatusRequest(checkStatusRequest);

            onecheckout.setMessage(
                    "VALID");

            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());
            return onecheckout;

        }

    }

    public static OneCheckoutDataHelper parseEDSUpdateStatusRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session, OneCheckoutPaymentChannel pChannel) {
        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            onecheckout.setPaymentChannel(pChannel);
            OneCheckoutEDSUpdateStatusData updateData = new OneCheckoutEDSUpdateStatusData();

            updateData.setTRANSIDMERCHANT(params.remove("invoiceNo"));
            updateData.setWORDS(params.remove("ref"));
            updateData.setREASON(params.remove("reason"));
            updateData.setSTATUS(params.remove("status"));

            onecheckout.setMessage("VALID");
            onecheckout.setEdsUpdateStatus(updateData);
            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());
            return onecheckout;
        }
    }

    public static OneCheckoutDataHelper parseGetTodayTransactionData(HashMap<String, String> params, HttpServletRequest request, HttpSession session, OneCheckoutPaymentChannel pChannel, int mallid) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            onecheckout.setMallId(mallid);
            Merchants m = WebUtils.getValidMALLID(mallid);
            onecheckout.setMerchant(m);
            onecheckout.setPaymentChannel(pChannel);

            OneCheckoutTodayKlikBCARequest today = new OneCheckoutTodayKlikBCARequest();
            today.setUSERIDKLIKBCA(params.remove("USERIDKLIKBCA"));

            onecheckout.setMessage("VALID");
            onecheckout.setTodayKlikBCA(today);
            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());
            return onecheckout;
        }

    }

    public static OneCheckoutDataHelper parseVoidRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            String paymentChannel = params.remove("PAYMENTCHANNEL");
            OneCheckoutPaymentChannel pchannel = OneCheckoutBaseRules.validatePaymentChannel(paymentChannel);

            if (pchannel == null) {
                OneCheckoutLogger.log("WebUtils.parseVoidRequestData : ERROR : PAYMENTCHANNEL %s not found", paymentChannel);
                return onecheckout;
            }

            onecheckout.setPaymentChannel(pchannel);
            String mallId = params.remove("MALLID");

            int mId = OneCheckoutBaseRules.validateMALLID2(mallId);
            OneCheckoutLogger.log("WebUtils.parseVoidRequestData : MALLID=%d", mId);

            OneCheckoutVoidRequest voidRequest = new OneCheckoutVoidRequest(mallId);
            voidRequest.setSESSIONID(params.remove("SESSIONID"));
            voidRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
            voidRequest.setWORDS(params.remove("WORDS"));
            voidRequest.setType(params.remove("TYPE"));

//          voidRequest.setVOIDREASON(params.remove("VOIDREASON"));
            onecheckout.setMessage("VALID");

            String chainMerchantId = params.remove("CHAINMERCHANT");
            int cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);

            OneCheckoutLogger.log("WebUtils.parseVoidRequestData : CHAINMERCHANT=%d", mId);

            onecheckout.setMallId(mId);
            onecheckout.setChainMerchantId(cmId);

            OneCheckoutLogger.log("DEBUG 1");
            Merchants m = null;
            if (cmId != 0) {
                m = WebUtils.getValidMALLID(mId, cmId);
            } else {
                m = WebUtils.getValidMALLID(mId);
            }
            OneCheckoutLogger.log("DEBUG 2");
            if (m == null) {
                OneCheckoutLogger.log("WebUtils.parseCheckStatusRequestData : ERROR : MALLID %s not found", mallId);
                return onecheckout;
            }

            onecheckout.setMerchant(m);
            onecheckout.setVoidRequest(voidRequest);

            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());
            return onecheckout;

        }

    }

    public static OneCheckoutDataHelper parseCCRequestData(HashMap<String, String> params, HttpServletRequest request, HttpSession session, OneCheckoutPaymentChannel pchannel) {
        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            //OneCheckoutPaymentChannel pchannel = OneCheckoutPaymentChannel.CreditCard;
            onecheckout.setPaymentChannel(pchannel);
            String mallId = params.remove("MALLID");

            OneCheckoutDOKUNotifyData notifyData = new OneCheckoutDOKUNotifyData();

            notifyData.setMALLID(mallId);

            if (pchannel == OneCheckoutPaymentChannel.CreditCard || pchannel == OneCheckoutPaymentChannel.BNIDebitOnline) {

                notifyData.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                notifyData.setAMOUNT(params.remove("AMOUNT"));
                notifyData.setCURRENCY(params.remove("CURRENCY"));
                notifyData.setSESSIONID(params.remove("SESSIONID"));
                notifyData.setWORDS(params.remove("WORDS"));
                notifyData.setTYPE(params.remove("TYPE"));
                notifyData.setCHAINNUM(params.remove("CHAINNUM"));

            }

            onecheckout.setMessage("VALID");

            Merchants m = WebUtils.getValidMALLID(notifyData.getMALLID());
            onecheckout.setMerchant(m);

            onecheckout.setNotifyRequest(notifyData);

        } catch (Exception e) {

            e.printStackTrace();
            onecheckout.setMessage(e.getMessage());

        }

        return onecheckout;
    }

    public static OneCheckoutPaymentChannel paymentChannel(String paymentChannelCode, String companyCode, String userAccount) {
        OneCheckoutPaymentChannel channel = null;
        OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class
        );

        try {

            OneCheckoutLogger.log("paymentChannelCode : %s, companyCode : %s, userAccount : %s", paymentChannelCode, companyCode, userAccount);

            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(paymentChannelCode, companyCode, userAccount);

            if (mpc != null) {
                OneCheckoutLogger.log("WebUtils.paymentChannel    [" + mpc.getMerchants() + "]");
                channel = OneCheckoutPaymentChannel.findType(mpc.getPaymentChannel().getPaymentChannelId());
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return channel;
    }

    public static OneCheckoutPaymentChannel paymentChannel(String invoiceNo, String sessionId, String amount, OneCheckoutTransactionState state) {
        OneCheckoutPaymentChannel channel = null;

        OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class
        );

        try {

            BigDecimal _amount = null;

            if (amount != null && amount.length() > 0) {
                _amount = BigDecimal.valueOf(OneCheckoutVerifyFormatData.sdf.parse(amount).doubleValue());
            }

            OneCheckoutLogger.log("================= GOT CHANNEL PARAMETER ================= ");
            OneCheckoutLogger.log("INVOICE    [" + invoiceNo + "]");
            OneCheckoutLogger.log("SESSION ID [" + sessionId + "]");
            OneCheckoutLogger.log("AMOUNT     [" + amount + "]");
            OneCheckoutLogger.log("STATE      [" + state.name() + "]");

            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(invoiceNo, sessionId, _amount, state);

            if (mpc != null) {
                channel = OneCheckoutPaymentChannel.findType(mpc.getPaymentChannel().getPaymentChannelId());
            } else {
                OneCheckoutLogger.log("CANNOT FIND CHANNEL BY TRANSACTION");
                channel = OneCheckoutPaymentChannel.CreditCard;
            }

            OneCheckoutLogger.log("MPC OBJ    [" + mpc + "]");
            OneCheckoutLogger.log("CHANNEL    [" + channel.name() + "]");

            OneCheckoutLogger.log("================= GOT CHANNEL PARAMETER ================= ");

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return channel;
    }

    public static String getSecurityAssociationInfo() {
        Principal p = SecurityAssociation.getCallerPrincipal();
        if (p != null) {
            return p.getName();
        } else {
            return "";
        }
    }

    public static void setSecurityAssociation(String sessionId) {
        SecurityAssociation.setPrincipal(new OneCheckoutPrincipal(sessionId));
    }

    public static void unsetSecurityAssociation() {
        SecurityAssociation.clear();
    }

    public static OneCheckoutDataHelper parseTransactionResult(HashMap<String, String> params, HttpServletRequest request, HttpSession session) {

        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

        try {

            OneCheckoutRedirectData redirectRequest = new OneCheckoutRedirectData();

            String transIdMerchant = params.get("TRANSIDMERCHANT");
            String sessionId = params.get("SESSIONID");
            double amount = OneCheckoutBaseRules.validateAmount1(params.get("AMOUNT"), "AMOUNT");

            OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class
            );

            Transactions trans = queryHelper.getRedirectTransactionWithoutState(transIdMerchant, sessionId, amount);

            MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
            Merchants m = mpc.getMerchants();
            PaymentChannel pc = mpc.getPaymentChannel();
            OneCheckoutPaymentChannel pChannel = OneCheckoutPaymentChannel.findType(pc.getPaymentChannelId());

            onecheckout.setMerchant(m);

            onecheckout.setMallId(m.getMerchantCode());
            onecheckout.setPaymentChannel(pChannel);

            if (pChannel == OneCheckoutPaymentChannel.CreditCard) {

                redirectRequest.setTRANSIDMERCHANT(params.remove("TRANSIDMERCHANT"));
                redirectRequest.setSESSIONID(params.remove("SESSIONID"));
                redirectRequest.setAMOUNT(params.remove("AMOUNT"));
                redirectRequest.setSTATUSCODE(params.remove("STATUSCODE"));
            }

            onecheckout.setMessage(
                    "VALID");
            onecheckout.setRedirectDoku(redirectRequest);

            return onecheckout;

        } catch (Exception e) {
            e.printStackTrace();
            onecheckout.setRedirectDoku(null);
            onecheckout.setMessage(e.getMessage());
            return onecheckout;
        }
    }

    public static HashMap<String, String> stringToHashMap(String hasmap) {
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

    public static CustomerInfoObject getInfoCustomer(OneCheckoutDataHelper oneCheckout, MerchantPaymentChannel mpc) {
        try {
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            HashMap<String, String> params = new HashMap<String, String>();

            /*Create datetime request*/
            String reqDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            /*Create account billing words for request info customer*/
            StringBuffer sbWords = new StringBuffer();
            sbWords.append(oneCheckout.getPaymentRequest().getMALLID()); // Mallid
            sbWords.append(mpc.getMerchantPaymentChannelHash());//sharedkey [c.getMerchantPaymentChannelHash()]
            sbWords.append("C");//identifiertype
            sbWords.append(oneCheckout.getPaymentRequest().getCUSTOMERID());//identifierno
            sbWords.append("Q");//requesttype
            sbWords.append(reqDateTime);//requestdatetime

            String hashwords = HashWithSHA1.doHashing(sbWords.toString(), null, null);

            /*Create parameter for sending to Account Billing Module*/
            StringBuffer sb = new StringBuffer("");
            sb.append("MALLID=").append(mpc.getPaymentChannelCode());
            sb.append("&CHAINMALLID=").append(mpc.getPaymentChannelChainCode());
            /**
             * *
             */
            sb.append("&IDENTIFIERTYPE=").append("C");
            sb.append("&IDENTIFIERNO=").append(oneCheckout.getPaymentRequest().getCUSTOMERID());
            sb.append("&REQUESTTYPE=").append("Q");
            sb.append("&REQUESTDATETIME=").append(reqDateTime);
            sb.append("&WORDS=").append(hashwords);
            sb.append("&SHAREDKEY=").append(mpc.getMerchantPaymentChannelHash());//[c.getMerchantPaymentChannelHash()]

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            String urlGetInfo = config.getString("RECUR.URL.GETINFO", null);
            //   String urlGetInfo = "http://103.10.129.17/AccountBilling/DoGetInfo";

            OneCheckoutLogger.log("try to get customer information with customer id %s ", oneCheckout.getPaymentRequest().getCUSTOMERID());
            OneCheckoutLogger.log("=====================================================================");
            OneCheckoutLogger.log("Parameter to send : \n" + sb.toString());
            OneCheckoutLogger.log("send to " + urlGetInfo);

            InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo, 30, 30);
            String resp = intResp.getMsgResponse();
            OneCheckoutLogger.log("respon info customer : %s", resp);

            if (resp.startsWith("<RESPONSE>")) {
                OneCheckoutLogger.log("Parsing respon xml to CustomerInfoObject : ");

                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(resp);
                xml.load(sr);

                CustomerInfoObject cio = new CustomerInfoObject();

                for (Iterator it = xml.getKeys(); it.hasNext();) {
                    String key = (String) it.next();
                    if (!key.contains("TOKENS.TOKEN")) {
                        try {
                            OneCheckoutLogger.log("parsing data :" + key + "=" + xml.getString(key));
                            BeanUtils.copyProperty(cio, key.toLowerCase(), xml.getString(key));
                        } catch (Exception e) {
                            OneCheckoutLogger.log("Failed parsing couse : " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                List<SubnodeConfiguration> tokens = xml.configurationsAt("TOKENS.TOKEN");
                for (SubnodeConfiguration snc : tokens) {
                    if (snc.getString("TOKENSTATUS").equals("ACTIVE")) {
                        OneCheckoutLogger.log("\tparsing data token : " + snc.getString("TOKENID"));
                        CustomerTokenObject cto = new CustomerTokenObject();
                        for (Iterator its = snc.getKeys(); its.hasNext();) {
                            String keyToken = (String) its.next();
                            try {
                                OneCheckoutLogger.log("\t\tparsing data :" + keyToken + "=" + snc.getString(keyToken));
                                /*ENtry data ke CustomerTokenObject dari xml yang nama objectnya sama dengan nama key*/
                                BeanUtils.copyProperty(cto, keyToken.toLowerCase(), snc.getString(keyToken));
                            } catch (Exception e) {
                                OneCheckoutLogger.log("Failed parsing token couse : " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        cio.getTokens().add(cto);
                    }
                }
                return cio;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger
                    .getLogger(WebUtils.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        OneCheckoutLogger.log("kesini");
        return new CustomerInfoObject();
    }

    public static CustomerInfoObject getInfoCustomerRecur(OneCheckoutDataHelper oneCheckout, MerchantPaymentChannel mpc) {
        try {
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            HashMap<String, String> params = new HashMap<String, String>();
            String reqDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            StringBuffer sbWords = new StringBuffer();
            sbWords.append(oneCheckout.getPaymentRequest().getMALLID());
            sbWords.append(mpc.getMerchantPaymentChannelHash());
            sbWords.append("C");
            sbWords.append(oneCheckout.getPaymentRequest().getCUSTOMERID());
            sbWords.append("Q");
            sbWords.append(reqDateTime);
            String hashwords = HashWithSHA1.doHashing(sbWords.toString(), null, null);
            StringBuilder sb = new StringBuilder("");
            sb.append("MALLID=").append(mpc.getPaymentChannelCode());
            sb.append("&CHAINMALLID=").append(mpc.getPaymentChannelChainCode());
            sb.append("&IDENTIFIERTYPE=").append("C");
            sb.append("&IDENTIFIERNO=").append(oneCheckout.getPaymentRequest().getCUSTOMERID());
            sb.append("&REQUESTTYPE=").append("Q");
            sb.append("&REQUESTDATETIME=").append(reqDateTime);
            sb.append("&WORDS=").append(hashwords);

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            String urlGetInfo = config.getString("RECUR.URL.GETINFO", null);
            OneCheckoutLogger.log("::: ACCOUNT BILLING GET CUSTOMER INFO :::");
            OneCheckoutLogger.log("Parameter    : \n" + sb.toString());
            OneCheckoutLogger.log("send to      : \n" + urlGetInfo);
            InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo, 30, 30);
            String resp = intResp.getMsgResponse();
            OneCheckoutLogger.log("Response AB  : \n%s", resp);
            if (resp.startsWith("<RESPONSE>")) {

                /*
                 OneCheckoutLogger.log("XML Parsing  : ");
                 XMLConfiguration xml = new XMLConfiguration();
                 StringReader sr = new StringReader(resp);
                 xml.load(sr);
                 CustomerInfoObject cio = new CustomerInfoObject();
                 OneCheckoutLogger.log("Data         :");
                 OneCheckoutLogger.log("--------------");
                 for (Iterator it = xml.getKeys(); it.hasNext();) {
                 String key = (String) it.next();
                 if (!key.contains("TOKENS.TOKEN")) {
                 try {
                 OneCheckoutLogger.log(key + "=" + xml.getString(key));
                 BeanUtils.copyProperty(cio, key.toLowerCase(), xml.getString(key));
                 } catch (Exception e) {
                 OneCheckoutLogger.log("Failed parsing couse : " + e.getMessage());
                 e.printStackTrace();
                 }
                 }
                 }
                 List<SubnodeConfiguration> tokens = xml.configurationsAt("TOKENS.TOKEN");
                 OneCheckoutLogger.log("");
                 OneCheckoutLogger.log("Token        : ");
                 OneCheckoutLogger.log("--------------");
                 for (SubnodeConfiguration snc : tokens) {
                 OneCheckoutLogger.log("TOKENID=" + snc.getString("TOKENID"));
                 CustomerTokenObject cto = new CustomerTokenObject();
                 for (Iterator its = snc.getKeys(); its.hasNext();) {
                 String keyToken = (String) its.next();
                 try {
                 OneCheckoutLogger.log(keyToken + "=" + snc.getString(keyToken));
                 BeanUtils.copyProperty(cto, keyToken.toLowerCase(), snc.getString(keyToken));
                 } catch (Exception e) {
                 OneCheckoutLogger.log("Failed parsing token couse : " + e.getMessage());
                 e.printStackTrace();
                 }
                 }
                 cio.getTokens().add(cto);
                 }
                 */
                RESPONSEDocument rESPONSEDocument = RESPONSEDocument.Factory.parse(resp);
                if (rESPONSEDocument != null) {
                    CustomerInfoObject cio = new CustomerInfoObject();
                    cio.setCustomeraddress(rESPONSEDocument.getRESPONSE().getCUSTOMERADDRESS() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERADDRESS().trim() : "");
                    cio.setCustomercity(rESPONSEDocument.getRESPONSE().getCUSTOMERCITY() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERCITY().trim() : "");
                    cio.setCustomercountry(rESPONSEDocument.getRESPONSE().getCUSTOMERCOUNTRY() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERCOUNTRY().trim() : "");
                    cio.setCustomeremail(rESPONSEDocument.getRESPONSE().getCUSTOMEREMAIL() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMEREMAIL().trim() : "");
                    cio.setCustomerhomephone(rESPONSEDocument.getRESPONSE().getCUSTOMERHOMEPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERHOMEPHONE().trim() : "");
                    cio.setCustomerid(rESPONSEDocument.getRESPONSE().getCUSTOMERID() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERID().trim() : "");
                    cio.setCustomermobilephone(rESPONSEDocument.getRESPONSE().getCUSTOMERMOBILEPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERMOBILEPHONE().trim() : "");
                    cio.setCustomername(rESPONSEDocument.getRESPONSE().getCUSTOMERNAME() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERNAME().trim() : "");
                    cio.setCustomerstate(rESPONSEDocument.getRESPONSE().getCUSTOMERSTATE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERSTATE().trim() : "");
                    cio.setCustomerworkphone(rESPONSEDocument.getRESPONSE().getCUSTOMERWORKPHONE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERWORKPHONE().trim() : "");
                    cio.setCustomerzipcode(rESPONSEDocument.getRESPONSE().getCUSTOMERZIPCODE() != null ? rESPONSEDocument.getRESPONSE().getCUSTOMERZIPCODE().trim() : "");
                    cio.setIdentifiertype(rESPONSEDocument.getRESPONSE().getIDENTIFIERTYPE() != null ? rESPONSEDocument.getRESPONSE().getIDENTIFIERTYPE().trim() : "");
                    cio.setMallid(rESPONSEDocument.getRESPONSE().getMALLID() != null ? rESPONSEDocument.getRESPONSE().getMALLID().trim() : "");
                    cio.setRequestdatetime(rESPONSEDocument.getRESPONSE().getREQUESTDATETIME() != null ? rESPONSEDocument.getRESPONSE().getREQUESTDATETIME().trim() : "");

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
                                cio.getTokens().add(customerTokenObject);
                            }
                        }
                    }
                    return cio;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger
                    .getLogger(WebUtils.class
                            .getName()).log(Level.SEVERE, null, ex);
        }
        return new CustomerInfoObject();
    }

    public static CustomerInfoObject getInfoCustomerRecurbyToken(OneCheckoutDataHelper oneCheckout, MerchantPaymentChannel mpc, String TOKENID) {
        try {
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            HashMap<String, String> params = new HashMap<String, String>();
            String reqDateTime = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            StringBuilder sbWords = new StringBuilder();
            sbWords.append(mpc.getPaymentChannelCode());
            sbWords.append(mpc.getMerchantPaymentChannelHash());
            sbWords.append("C");
            sbWords.append(oneCheckout.getPaymentRequest().getCUSTOMERID());
            sbWords.append("Q");
            sbWords.append(reqDateTime);
            String hashwords = HashWithSHA1.doHashing(sbWords.toString(), null, null);
            StringBuilder sb = new StringBuilder("");
            sb.append("MALLID=").append(mpc.getPaymentChannelCode());
            sb.append("&CHAINMALLID=").append(mpc.getPaymentChannelChainCode());
            /**
             * *
             */
            sb.append("&IDENTIFIERTYPE=").append("C");
            sb.append("&IDENTIFIERNO=").append(oneCheckout.getPaymentRequest().getCUSTOMERID());
            sb.append("&REQUESTTYPE=").append("Q");
            sb.append("&REQUESTDATETIME=").append(reqDateTime);
            sb.append("&WORDS=").append(hashwords);

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            String urlGetInfo = config.getString("RECUR.URL.GETINFObyTOKEN", null);

            OneCheckoutLogger.log("::: ACCOUNT BILLING DoServiceGetInfo :::");
            OneCheckoutLogger.log("Parameter                    : \n" + sb.toString());
            OneCheckoutLogger.log("send to                      : " + urlGetInfo);
            OneCheckoutLogger.log("Token Request                : " + TOKENID);
            InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo, 30, 30);
            String resp = intResp.getMsgResponse();
            OneCheckoutLogger.log("Response DoServiceGetInfo    : \n%s", resp);
            if (resp.startsWith("<RESPONSE>")) {
                XMLConfiguration xml = new XMLConfiguration();
                StringReader sr = new StringReader(resp);
                xml.load(sr);
                CustomerInfoObject cio = new CustomerInfoObject();
                for (Iterator it = xml.getKeys(); it.hasNext();) {
                    String key = (String) it.next();
                    if (!key.contains("TOKENS.TOKEN")) {
                        try {
                            BeanUtils.copyProperty(cio, key.toLowerCase(), xml.getString(key));
                        } catch (Exception e) {
                            OneCheckoutLogger.log("Failed parsing couse : " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                List<SubnodeConfiguration> tokens = xml.configurationsAt("TOKENS.TOKEN");
                for (SubnodeConfiguration snc : tokens) {
                    if (TOKENID.equals(snc.getString("TOKENID"))) {
                        OneCheckoutLogger.log("Token Response               : " + snc.getString("TOKENID"));
                        CustomerTokenObject cto = new CustomerTokenObject();
                        for (Iterator its = snc.getKeys(); its.hasNext();) {
                            String keyToken = (String) its.next();
                            try {
                                BeanUtils.copyProperty(cto, keyToken.toLowerCase(), snc.getString(keyToken));
                            } catch (Exception e) {
                                OneCheckoutLogger.log("Failed parsing token couse : " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        cio.getTokens().add(cto);
                    }
                }
                return cio;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new CustomerInfoObject();
    }

    public static String decrypt(String encryptedValue) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedValue);
        byte[] decValue = c.doFinal(decordedValue);
        String decryptedValue = new String(decValue, "UTF-8");
        return decryptedValue;
    }

    private static Key generateKey() throws Exception {
        byte[] keyValue = new byte[]{'d', '0', 'k', 'U', 'p', 'A', 'Y', 'M', '3', 'n', '7', '9', 'a', 't', '3', 'W'};
        Key key = new SecretKeySpec(keyValue, "AES");
        return key;
    }
}
