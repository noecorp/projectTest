/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.template.CustomerTokenObject;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.view.WebUtils;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.codehaus.jettison.json.JSONObject;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author hafizsjafioedin
 */
public class AccountBilling extends HttpServlet {

    String MALLIDParam = "MALLID";
    String CHAINMERCHANTIDParam = "CHAINMERCHANT";
    String BUTTONParam = "tombol";
    String errorPage = "/ErrorPage";
    String sessionCIPRequest = "cipRequest";

    @Override
    public void init() throws ServletException {
        if (ESAPI.securityConfiguration().getResourceDirectory() == null) {
            System.out.println("====ESAPI.properties ready to define====");
            ESAPI.securityConfiguration().setResourceDirectory("/apps/ESAPI/resources/");
        } else if (!ESAPI.securityConfiguration().getResourceDirectory().equals("/apps/ESAPI/resources/")) {
            System.out.println("====ESAPI.properties already define but different path ====");
            ESAPI.securityConfiguration().setResourceDirectory("/apps/ESAPI/resources/");
        } else {
            System.out.println("====ESAPI.properties already define ====");
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

    
    //syamsul edit
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ESAPI.httpUtilities().setCurrentHTTP(request, response);
        HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
        if (req.getParameter("procode").equals("1")) {
            doProsesAdd(request, response);
        } else if (req.getParameter("procode").equals("2")) {
            doProsesDelete(request, response);
        } else if (req.getParameter("procode").equals("3")) {
            doProsesUpdateSubscriptionMIP(request, response);
        } else if (req.getParameter("procode").equals("4")) {
            doShowUpdateSubscriptionMIP(request, response);
        }

    }

    protected void doProsesAdd(HttpServletRequest request, HttpServletResponse response) {
        OneCheckoutLogger.log("AccountBilling.doProsesAdd");
        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
        OneCheckoutDataHelper oneCheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
        if (oneCheckout == null) {
            OneCheckoutLogger.log("AccountBilling.doProsesAdd with one checkout helper null");
        } else {
            //OneCheckoutLogger.log("Data Merchant Payment Channel : " + oneCheckout.getMerchant().getMerchantPaymentChannels().iterator().next().getMerchantPaymentChannelHash());

            OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Tokenization);

            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
//            HashMap<String, String> params = new HashMap<String, String>();

            String reqDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            
            HashMap<String, String> paramsReq = new HashMap<String, String>();
            StringBuilder sb = new StringBuilder("");
            StringBuffer sbWords = new StringBuffer();
            String hashwords = "";
            String resp = "";
            JSONObject jsonObject = new JSONObject();
                
            try {

                sbWords.append(oneCheckout.getPaymentRequest().getMALLID()); // Mallid
                sbWords.append(mpc.getMerchantPaymentChannelHash());//sharedkey [c.getMerchantPaymentChannelHash()]
                sbWords.append(oneCheckout.getPaymentRequest().getCUSTOMERID()); //Customerid
                sbWords.append(reqDateTime);//requestdatetime

                hashwords = HashWithSHA1.doHashing(sbWords.toString(),null,null);
                ESAPI.httpUtilities().setCurrentHTTP(request, response);
                HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

                paramsReq = WebUtils.copyParams(request);

                sb = new StringBuilder("");
                sb.append("MALLID=").append(mpc.getPaymentChannelCode());
                sb.append("&CHAINMALLID=").append(mpc.getPaymentChannelChainCode());
                sb.append("&CUSTOMERID=").append(oneCheckout.getPaymentRequest().getCUSTOMERID());
                sb.append("&CUSTOMERNAME=").append(oneCheckout.getPaymentRequest().getNAME());
                sb.append("&CUSTOMEREMAIL=").append(oneCheckout.getPaymentRequest().getEMAIL());
                sb.append("&CUSTOMERADDRESS=").append(paramsReq.get("ADDRESS"));
                sb.append("&CUSTOMERCITY=").append(paramsReq.get("CITY"));
                sb.append("&CUSTOMERSTATE=").append(paramsReq.get("STATE"));
                sb.append("&CUSTOMERCOUNTRY=").append(paramsReq.get("COUNTRY"));
                sb.append("&CUSTOMERZIPCODE=").append(paramsReq.get("ZIPCODE"));
                sb.append("&CUSTOMERMOBILEPHONE=").append(paramsReq.get("MOBILEPHONE"));
                sb.append("&CUSTOMERHOMEPHONE=").append(paramsReq.get("HOMEPHONE"));
                sb.append("&CUSTOMERWORKPHONE=").append(paramsReq.get("HOMEPHONE"));
                sb.append("&CARDNUMBER=").append(paramsReq.get("CARDNUMBER"));
                sb.append("&EXPIRYDATE=").append(paramsReq.get("YEAR")).append(paramsReq.get("MONTH"));
                sb.append("&CVV2=").append(paramsReq.get("CVV2"));
                sb.append("&CCNAME=").append(paramsReq.get("CC_NAME"));
                sb.append("&CCEMAIL=").append(paramsReq.get("EMAIL"));
                sb.append("&CCADDRESS=").append(paramsReq.get("ADDRESS"));
                sb.append("&CCCITY=").append(paramsReq.get("CITY"));
                sb.append("&CCSTATE=").append(paramsReq.get("STATE"));
                sb.append("&CCCOUNTRY=").append(paramsReq.get("COUNTRY"));
                sb.append("&CCZIPCODE=").append(paramsReq.get("ZIPCODE"));
                sb.append("&CCMOBILEPHONE=").append(paramsReq.get("MOBILEPHONE"));
                sb.append("&CCHOMEPHONE=").append(paramsReq.get("HOMEPHONE"));
                sb.append("&CCWORKPHONE=").append(paramsReq.get("HOMEPHONE"));
                sb.append("&REQUESTDATETIME=").append(reqDateTime);
                sb.append("&WORDS=").append(hashwords);
                sb.append("&SHAREDKEY=").append(mpc.getMerchantPaymentChannelHash());//[c.getMerchantPaymentChannelHash()]

                //String urlGetInfo = "http://103.10.129.17/AccountBilling/DoTokenizationCustomerRegistrationMIPRequest";
                String urlGetInfo = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.URL.Registration", "http://dokulocal_luna_app:8080/AccountBilling/DoTokenizationCustomerRegistrationMIPRequest");

                OneCheckoutLogger.log("try to get customer information with customer id %s ", oneCheckout.getPaymentRequest().getCUSTOMERID());
                OneCheckoutLogger.log("=====================================================================");
                OneCheckoutLogger.log("Parameter to send : \n" + sb.toString());
                OneCheckoutLogger.log("send to " + urlGetInfo);

                InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo,  30, 30);
                resp = intResp.getMsgResponse();
                OneCheckoutLogger.log("respon info customer : %s", resp);

                response.setContentType("text/*");
                if (resp.startsWith("<RESPONSE>")) {
                    OneCheckoutLogger.log("Parsing Respon from XML");

                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resp);
                    xml.load(sr);

                    String kodeRespon = xml.getString("RESULTMSG");
                    jsonObject.put("STATUS", kodeRespon);

                    if (kodeRespon.equals("SUCCESS")) {
                        jsonObject.put("STATUSMSG", "ADDING CARD SUCCESS");
                        List<SubnodeConfiguration> tokens = xml.configurationsAt("TOKENS.TOKEN");
                        for (SubnodeConfiguration token : tokens) {
                            jsonObject.put("TOKENID", token.getString("TOKENID"));
                            jsonObject.put("CARDNUMBER", token.getString("CARDNUMBER"));
                        }
                    } else {
                        jsonObject.put("STATUSMSG", "ADDING CARD FAILED");
                    }

//
                    OneCheckoutLogger.log("Parsing respon xml to json : " + jsonObject.toString());
                } else {
                    jsonObject.put("STATUS", "FAILED");
                    jsonObject.put("STATUSMSG", "ADDING CARD FAILED");
                }
                response.getWriter().write(jsonObject.toString());
            } catch (Exception ex) {
                Logger.getLogger(WebUtils.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                paramsReq = null;
                sb = null;
                sbWords = null;
                hashwords = null;
                resp = null;
                jsonObject = null;
            }
        }

    }

    protected void doProsesDelete(HttpServletRequest request, HttpServletResponse response) {
        OneCheckoutLogger.log("AccountBilling.doProsesDelete");
        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
        OneCheckoutDataHelper oneCheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
        if (oneCheckout == null) {
            OneCheckoutLogger.log("AccountBilling.doProsesDelete with one checkout helper null");
        } else {
            //OneCheckoutLogger.log("Data Merchant Payment Channel : " + oneCheckout.getMerchant().getMerchantPaymentChannels().iterator().next().getMerchantPaymentChannelHash());

            OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Tokenization);

            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            //HashMap<String, String> params = new HashMap<String, String>();

            String reqDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            
            HashMap<String, String> paramsReq = new HashMap<String, String>();
            StringBuffer sbWords = new StringBuffer();
            StringBuffer sb = new StringBuffer("");
            String resp = "";
            JSONObject jsonObject = new JSONObject();
                
            try {

                ESAPI.httpUtilities().setCurrentHTTP(request, response);
                HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

                paramsReq = WebUtils.copyParams(request);

                sbWords.append(oneCheckout.getPaymentRequest().getMALLID()); // Mallid
                sbWords.append(mpc.getMerchantPaymentChannelHash());//sharedkey [c.getMerchantPaymentChannelHash()]
                sbWords.append(oneCheckout.getPaymentRequest().getCUSTOMERID());//Customerid
                sbWords.append("T");//identifiertype
                sbWords.append(paramsReq.get("tokenid"));//identifierno
                sbWords.append("C");//requesttype
                sbWords.append(reqDateTime);//requestdatetime

                String hashwords = HashWithSHA1.doHashing(sbWords.toString(),null,null);

                sb.append("MALLID=").append(mpc.getPaymentChannelCode());
                sb.append("&CHAINMALLID=").append(mpc.getPaymentChannelChainCode());

//                sb.append("MALLID=").append(oneCheckout.getPaymentRequest().getMALLID());
//                if (oneCheckout.getPaymentRequest().getCHAINMERCHANT() == 0) {
//                    sb.append("&CHAINMERCHANT=NA");
//                } else {
//                    sb.append("&CHAINMERCHANT=").append(oneCheckout.getPaymentRequest().getCHAINMERCHANT());
//                }
                sb.append("&IDENTIFIERTYPE=T");
                sb.append("&IDENTIFIERNO=").append(paramsReq.get("tokenid"));
                sb.append("&REQUESTTYPE=C");
                sb.append("&REQUESTDATETIME=").append(reqDateTime);
                sb.append("&WORDS=").append(hashwords);


                //String urlGetInfo = "http://103.10.129.17/AccountBilling/DoDeleteSubscription";
                String urlGetInfo = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.URL.DeleteToken", "http://dokulocal_luna_app:8080/AccountBilling/DoDeleteSubscription");

                OneCheckoutLogger.log("try to get customer information with customer id %s ", oneCheckout.getPaymentRequest().getCUSTOMERID());
                OneCheckoutLogger.log("=====================================================================");
                OneCheckoutLogger.log("Parameter to send : \n" + sb.toString());
                OneCheckoutLogger.log("send to " + urlGetInfo);

                InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo,  30, 30);
                resp = intResp.getMsgResponse();
                OneCheckoutLogger.log("respon info customer : %s", resp);

                response.setContentType("text/*");
                if (resp.startsWith("<RESPONSE>")) {
                    OneCheckoutLogger.log("Parsing Respon from XML");

                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resp);
                    xml.load(sr);

                    String kodeRespon = xml.getString("RESULTMSG");
                    jsonObject.put("STATUS", kodeRespon);

                    if (kodeRespon.equals("SUCCESS")) {
                        jsonObject.put("STATUSMSG", "DELETE CARD SUCCESS");
                        List<SubnodeConfiguration> tokens = xml.configurationsAt("TOKENS.TOKEN");
                        for (SubnodeConfiguration token : tokens) {
                            jsonObject.put("TOKENID", token.getString("TOKENID"));
                            jsonObject.put("CARDNUMBER", token.getString("CARDNUMBER"));
                        }
                    } else {
                        jsonObject.put("STATUSMSG", "DELETE CARD FAILED");
                    }

//
                    OneCheckoutLogger.log("Parsing respon xml to json : " + jsonObject.toString());
                } else {
                    jsonObject.put("STATUS", "FAILED");
                    jsonObject.put("STATUSMSG", "DELETE CARD FAILED");
                }
                response.getWriter().write(jsonObject.toString());
            } catch (Exception ex) {
                Logger.getLogger(WebUtils.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                paramsReq = null;
                sbWords = null;
                sb = null;
                resp = null;
                jsonObject = null;
            }
        }

    }

    protected void doProsesUpdateSubscription(HttpServletRequest request, HttpServletResponse response) {
        OneCheckoutLogger.log("AccountBilling.doProsesUpdateSubscription");
        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
        OneCheckoutDataHelper oneCheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
        if (oneCheckout == null) {
            OneCheckoutLogger.log("AccountBilling.doProsesUpdateSubscription with one checkout helper null");
        } else {

            OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Tokenization);

            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            HashMap<String, String> params = new HashMap<String, String>();

            String reqDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            try {

                ESAPI.httpUtilities().setCurrentHTTP(request, response);
                HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

                HashMap<String, String> paramsReq = WebUtils.copyParams(request);

                StringBuffer sbWords = new StringBuffer();
                sbWords.append(oneCheckout.getPaymentRequest().getMALLID()); // Mallid
                sbWords.append(mpc.getMerchantPaymentChannelHash());//sharedkey [c.getMerchantPaymentChannelHash()]
                sbWords.append(oneCheckout.getPaymentRequest().getCUSTOMERID());//CustomerID
                sbWords.append(paramsReq.get("tokenid"));//TokenID
                sbWords.append(reqDateTime);//requestdatetime

                String hashwords = HashWithSHA1.doHashing(sbWords.toString(),null,null);

                StringBuffer sb = new StringBuffer("");
                sb.append("MALLID=").append(mpc.getPaymentChannelCode());
                sb.append("&CHAINMALLID=").append(mpc.getPaymentChannelChainCode());

//                sb.append("MALLID=").append(oneCheckout.getPaymentRequest().getMALLID());
//                if (oneCheckout.getPaymentRequest().getCHAINMERCHANT() == 0) {
//                    sb.append("&CHAINMALLID=NA");
//                } else {
//                    sb.append("&CHAINMALLID=").append(oneCheckout.getPaymentRequest().getCHAINMERCHANT());
//                }
                sb.append("&CUSTOMERID=").append(oneCheckout.getPaymentRequest().getCUSTOMERID());
                sb.append("&TOKENID=").append(paramsReq.get("tokenid"));
                sb.append("&SESSIONID=").append(oneCheckout.getPaymentRequest().getSESSIONID());
                sb.append("&REQUESTDATETIME=").append(reqDateTime);
                sb.append("&WORDS=").append(hashwords);

                //String urlGetInfo = "http://103.10.129.17/AccountBilling/DoUpdateTokenDataRequest";
                String urlGetInfo = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.URL.UpdateToken", "http://dokulocal_luna_app:8080/AccountBilling/DoUpdateTokenDataRequest");

                OneCheckoutLogger.log("try to get customer information with customer id %s ", oneCheckout.getPaymentRequest().getCUSTOMERID());
                OneCheckoutLogger.log("=====================================================================");
                OneCheckoutLogger.log("Parameter to send : \n" + sb.toString());
                OneCheckoutLogger.log("send to " + urlGetInfo);

                InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo, 30, 30);
                String resp = intResp.getMsgResponse();
                OneCheckoutLogger.log("respon info customer : %s", resp);

                JSONObject jsonObject = new JSONObject();
                response.setContentType("text/*");
                if (resp.startsWith("<RESPONSE>")) {
                    OneCheckoutLogger.log("Parsing Respon from XML");

                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resp);
                    xml.load(sr);

                    String kodeRespon = xml.getString("RESULTMSG");
                    jsonObject.put("STATUS", kodeRespon);

                    if (kodeRespon.equals("SUCCESS")) {
                        jsonObject.put("STATUSMSG", "TOKEN SUBSCRIPTION SUCCESS");
                        List<SubnodeConfiguration> tokens = xml.configurationsAt("TOKENS.TOKEN");
                        for (SubnodeConfiguration token : tokens) {
                            jsonObject.put("TOKENID", token.getString("TOKENID"));
                            jsonObject.put("CARDNUMBER", token.getString("CARDNUMBER"));
                        }
                    } else {
                        jsonObject.put("STATUSMSG", "TOKEN SUBSCRIPTION FAILED");
                    }

//
                    OneCheckoutLogger.log("Parsing respon xml to json : " + jsonObject.toString());
                } else {
                    jsonObject.put("STATUS", "FAILED");
                    jsonObject.put("STATUSMSG", "TOKEN SUBSCRIPTION FAILED");
                }
                response.getWriter().write(jsonObject.toString());
            } catch (Exception ex) {
                Logger.getLogger(WebUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    protected void doProsesUpdateSubscriptionMIP(HttpServletRequest request, HttpServletResponse response) {
        OneCheckoutLogger.log("AccountBilling.doProsesUpdateSubscriptionMIP");
        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
        OneCheckoutDataHelper oneCheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
        if (oneCheckout == null) {
            OneCheckoutLogger.log("AccountBilling.doProsesUpdateSubscriptionMIP with one checkout helper null");
        } else {

            OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Tokenization);

            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
//            HashMap<String, String> params = new HashMap<String, String>();
            
            String reqDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            HashMap<String, String> paramsReq = new HashMap<String, String>();
            StringBuffer sbWords = new StringBuffer();
            StringBuffer sb = new StringBuffer("");
            String resp = "";
            JSONObject jsonObject = new JSONObject();
            
            try {

                ESAPI.httpUtilities().setCurrentHTTP(request, response);
                HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

                paramsReq = WebUtils.copyParams(request);

                sbWords.append(oneCheckout.getPaymentRequest().getMALLID()); // Mallid
                sbWords.append(mpc.getMerchantPaymentChannelHash());//sharedkey [c.getMerchantPaymentChannelHash()]
                sbWords.append(oneCheckout.getPaymentRequest().getCUSTOMERID());//CustomerID
                sbWords.append(paramsReq.get("tokenid"));//TokenID
                sbWords.append(reqDateTime);//requestdatetime

                String hashwords = HashWithSHA1.doHashing(sbWords.toString(),null,null);

                OneCheckoutLogger.log("send data with MALLID " + oneCheckout.getPaymentChannelCode());
                
                sb.append("MALLID=").append(mpc.getPaymentChannelCode());
                sb.append("&CHAINMALLID=").append(mpc.getPaymentChannelChainCode());
                
                //sb.append("MALLID=").append(oneCheckout.getPaymentChannelCode());
                //if (oneCheckout.getPaymentRequest().getCHAINMERCHANT() == 0) {
                //  sb.append("&CHAINMERCHANT=NA");
                //} else {
                //sb.append("&CHAINMALLID=").append(oneCheckout.getPaymentRequest().getCHAINMERCHANT());
                //}
                sb.append("&CUSTOMERID=").append(oneCheckout.getPaymentRequest().getCUSTOMERID());
                sb.append("&TOKENID=").append(paramsReq.get("tokenid"));
                //for (CustomerTokenObject cto : oneCheckout.getPaymentPageObject().getCustomerInfo().getTokens()) {
                //if (cto.getTokenid().equals(paramsReq.get("tokenid"))) {
                sb.append("&CARDNUMBER=").append(paramsReq.get("CARDNUMBER"));
                sb.append("&EXPIRYDATE=").append(paramsReq.get("YEAR") + paramsReq.get("MONTH"));
                sb.append("&CVV2=").append(paramsReq.get("CVV2"));
                sb.append("&CCNAME=").append(paramsReq.get("CC_NAME"));
                sb.append("&CCEMAIL=").append(paramsReq.get("EMAIL"));
                sb.append("&CCADDRESS=").append(paramsReq.get("ADDRESS"));
                sb.append("&CCCITY=").append(paramsReq.get("CITY"));
                sb.append("&CCSTATE=").append(paramsReq.get("STATE"));
                sb.append("&CCCOUNTRY=").append(paramsReq.get("COUNTRY"));
                sb.append("&CCZIPCODE=").append(paramsReq.get("ZIPCODE"));
                sb.append("&CCMOBILEPHONE=").append(paramsReq.get("MOBILEPHONE"));
                sb.append("&CCHOMEPHONE=").append(paramsReq.get("HOMEPHONE"));
                sb.append("&CCWORKPHONE=").append(paramsReq.get("WORKPHONE"));
                sb.append("&REQUESTDATETIME=").append(reqDateTime);
                //}
                //}

                sb.append("&WORDS=").append(hashwords);

                //String urlGetInfo = "http://103.10.129.17/AccountBilling/DoUpdateTokenMIPData";
                String urlGetInfo = OneCheckoutProperties.getOneCheckoutConfig().getString("TOKEN.URL.UpdateTokenMIP", "http://dokulocal_luna_app:8080/AccountBilling/DoUpdateTokenMIPData");

                OneCheckoutLogger.log("try to get customer information with customer id %s ", oneCheckout.getPaymentRequest().getCUSTOMERID());
                OneCheckoutLogger.log("=====================================================================");
                OneCheckoutLogger.log("Parameter to send : \n" + sb.toString());
                OneCheckoutLogger.log("send to " + urlGetInfo);

                InternetResponse intResp = base.doFetchHTTP(sb.toString(), urlGetInfo,  30, 30);
                resp = intResp.getMsgResponse();
                OneCheckoutLogger.log("respon info customer : %s", resp);

                response.setContentType("text/*");
                if (resp.startsWith("<RESPONSE>")) {
                    OneCheckoutLogger.log("Parsing Respon from XML");

                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resp);
                    xml.load(sr);

                    String kodeRespon = xml.getString("RESULTMSG");
                    jsonObject.put("STATUS", kodeRespon);

                    if (kodeRespon.equals("SUCCESS")) {
                        jsonObject.put("STATUSMSG", "TOKEN SUBSCRIPTION SUCCESS");
                        List<SubnodeConfiguration> tokens = xml.configurationsAt("TOKENS.TOKEN");
                        for (SubnodeConfiguration token : tokens) {
                            jsonObject.put("TOKENID", token.getString("TOKENID"));
                            jsonObject.put("CARDNUMBER", token.getString("CARDNUMBER"));
                        }
                    } else {
                        jsonObject.put("STATUSMSG", "TOKEN SUBSCRIPTION FAILED");
                    }

//
                    OneCheckoutLogger.log("Parsing respon xml to json : " + jsonObject.toString());
                } else {
                    jsonObject.put("STATUS", "FAILED");
                    jsonObject.put("STATUSMSG", "TOKEN SUBSCRIPTION FAILED");
                }
                response.getWriter().write(jsonObject.toString());
            } catch (Exception ex) {
                Logger.getLogger(WebUtils.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                paramsReq = null;
                sbWords = null;
                sb = null;
                resp = null;
                jsonObject = null;
            }
        }

    }

    protected void doShowUpdateSubscriptionMIP(HttpServletRequest request, HttpServletResponse response) {
        OneCheckoutLogger.log("AccountBilling.doShowUpdateSubscriptionMIP");
        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
        OneCheckoutDataHelper oneCheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
        if (oneCheckout == null) {
            OneCheckoutLogger.log("AccountBilling.doShowUpdateSubscriptionMIP with one checkout helper null");
        } else {

            OneCheckoutV1QueryHelperBeanLocal queryBeans = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            MerchantPaymentChannel mpc = queryBeans.getMerchantPaymentChannel(oneCheckout.getMerchant(), OneCheckoutPaymentChannel.Tokenization);

//            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
//            HashMap<String, String> params = new HashMap<String, String>();
//
//            String reqDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            HashMap<String, String> paramsReq = new HashMap<String, String>();
            JSONObject sb = new JSONObject();
            
            try {

                ESAPI.httpUtilities().setCurrentHTTP(request, response);
                HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

                paramsReq = WebUtils.copyParams(request);

                OneCheckoutLogger.log("send data with MALLID " + oneCheckout.getPaymentChannelCode());

                
                response.setContentType("text/*");

                for (CustomerTokenObject cto : oneCheckout.getPaymentPageObject().getCustomerInfo().getTokens()) {
                    if (cto.getTokenid().equals(paramsReq.get("tokenid"))) {
                        sb.put("STATUS", "SUCCESS");
                        sb.put("CARDNUMBER", cto.getCardnumber());
                        sb.put("EXPIRYDATE", cto.getExpirydate());
                        sb.put("CVV2", "");
                        sb.put("CCNAME", cto.getCcname());
                        sb.put("CCEMAIL", cto.getCcemail());
                        sb.put("CCADDRESS", cto.getCcaddress());
                        sb.put("CCCITY", cto.getCccity());
                        sb.put("CCSTATE", cto.getCcstate());
                        sb.put("CCCOUNTRY", cto.getCccountry());
                        sb.put("CCZIPCODE", cto.getCczipcode());
                        sb.put("CCMOBILEPHONE", cto.getCcmobilephone());
                        sb.put("CCHOMEPHONE", cto.getCchomephone());
                        sb.put("CCWORKPHONE", cto.getCcworkphone());
                    }
                }
                //OneCheckoutLogger.log("isi cto " + sb.toString());

                response.getWriter().write(sb.toString());
            } catch (Exception ex) {
                Logger.getLogger(WebUtils.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                paramsReq = null;
                sb = null;
            }
        }

    }
}