/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RequestHelper;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.*;
import com.onecheckoutV1.enums.EActivity;
import com.onecheckoutV1.enums.EParameterName;
import com.onecheckoutV1.manage.ManageLocal;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutGeneralConstant;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onecheckoutV1.view.ObjectPage;
import com.onecheckoutV1.view.PageViewer;
import com.onecheckoutV1.view.WebUtils;
import com.onechekoutv1.dto.MerchantActivity;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author hafizsjafioedin
 */
public class Receive extends HttpServlet {

    String MALLIDParam = "MALLID";
    String CHAINMERCHANTIDParam = "CHAINMERCHANT";
    String BUTTONParam = "tombol";
    String errorPage = "/ErrorPage";
    String sessionCIPRequest = "cipRequest";
    String SESSION_NAME = "OCOSESSIONDOKU";
    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;
    @EJB
    private ManageLocal manageLocal;

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, TemplateException {

        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.        
        PrintWriter out = response.getWriter();
        
        HashMap<String, String> params = new HashMap<String, String>();
        String mallId = "";
        String logCode = "";
        OneCheckoutDataHelper onecheckout = null;

        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            params = WebUtils.copyParams(request);
            HttpSession session = request.getSession();
            if (request.getSession() != null) {
                session.invalidate();
            }
            session = request.getSession(true);

            mallId = request.getParameter(MALLIDParam);
            logCode = "Incoming_";
            if (mallId != null) {
                logCode = "Incoming_" + mallId + "_" + request.getParameter("TRANSIDMERCHANT");
            }
            WebUtils.setSecurityAssociation(logCode);
            String userAgent = request.getHeader("User-Agent");
            try {
                OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
            } catch (Throwable th) {
                OneCheckoutLogger.log("Receive.processRequest : Error occur when debug Browser UserAgent");
            }
            OneCheckoutLogger.log("Receive.processRequest Receiving Payment Request from MALLID=" + mallId);
            OneCheckoutLogger.log("Receive.processRequest Parse Payment Request Data ");

            try {
                onecheckout = WebUtils.parsePaymentRequestData(params, request, session);
                session.setAttribute(sessionCIPRequest, onecheckout);
                session.setMaxInactiveInterval(15 * 60);
                OneCheckoutLogger.log("Receive.processRequest session last access time : " + session.getLastAccessedTime());
//                String problem = null;
//                String status = null;
//                String invoice = null;
                if (onecheckout.getMessage().equals("VALID")) {
                    proses(onecheckout, request, out);
                } 
//                else if (onecheckout.getMessage().equals("TRANSACTION-ALREADYSUCCESS-ALREADYVOID")) {
//                    
//
//                    if (onecheckout.getTransactions().getTransactionsStatus().equals(OneCheckoutTransactionStatus.SUCCESS.value())) {
//                        status = "SUCCESS";
//                    } else if (onecheckout.getTransactions().getTransactionsStatus().equals(OneCheckoutTransactionStatus.VOIDED.value())) {
//                        status = "VOID";
//                    }
//                    problem = "ALREADY";
//                    invoice = onecheckout.getTransactions().getIncTransidmerchant();
//                    OneCheckoutLogger.log("Receive.processRequest parsePaymentRequestData = TRANSACTION ALREADY " + status);
//                    ObjectPage op = PageViewer.transactionAlreadySuccessVoid(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant(), status, invoice, problem);
////                    ObjectPage op = PageViewer.transactionAlreadySuccessVoid(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant(), problem);
//                    Template temp = op.getTemp();
//                    Map root = op.getRoot();
//                    temp.process(root, out);
//                } 
                else {
//                    problem = "PAYMENTREQUESTINVALID";
                    OneCheckoutLogger.log("Receive.processRequest parsePaymentRequestData NOT VALID");
                    ObjectPage op = PageViewer.invalidPaymentRequestPage(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant());
//                    ObjectPage op = PageViewer.transactionAlreadySuccessVoid(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant(), status, invoice, problem);
                    Template temp = op.getTemp();
                    Map root = op.getRoot();
                    temp.process(root, out);
                }

            } catch (Throwable t) {
                t.printStackTrace();
                OneCheckoutLogger.log("ERROR : " + t.getMessage());
                error(onecheckout, request, out);
            }
        } catch (Throwable th) {
            th.printStackTrace();
            int mId = 0;
            mallId = request.getParameter(MALLIDParam);
            if (mallId != null && !mallId.isEmpty()) {
                try {
                    mId = OneCheckoutBaseRules.validateMALLID2(mallId);
                } catch (Throwable e) {
                    mId = 0;
                }
            }
            int cmId = -1;
            String chainMerchantId = request.getParameter(CHAINMERCHANTIDParam);
            if (chainMerchantId != null && !chainMerchantId.isEmpty()) {
                try {
                    cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);
                } catch (Throwable e) {
                    cmId = -1;
                }
            }
            ObjectPage op = PageViewer.errorPage(mId, cmId, request);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } finally {
            out.close();
            params = null;
            mallId = null;
            logCode = null;
            onecheckout = null;
        }
    }

    public void proses(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        Merchants m = null;
        PaymentPageObject trxDetailpage = null;
        try {

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            m = onecheckout.getMerchant();
            trxDetailpage = onecheckout.getPaymentPageObject();

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            OneCheckoutMethod methodFlow = onecheckout.getCIPMIP();

            String actionUrl = config.getString("PAYMENTPAGE.ACTION.URL", "/Suite/PaymentPageShow");
            if (methodFlow == OneCheckoutMethod.MIP) {
                actionUrl = config.getString("PAYMENT.ACTION.URL", "/Suite/ProcessPayment");
            }

            actionUrl = actionUrl + "?MALLID=" + onecheckout.getMallId() + "&CHAINMERCHANT=" + onecheckout.getChainMerchantId() + "&INV=" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setPageTemplate("redirect.html");
            redirect.setUrlAction(actionUrl);

            ObjectPage op = PageViewer.redirectPage(redirect, m, request, trxDetailpage);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } catch (Throwable t) {
            t.printStackTrace();
            error(onecheckout, request, out);
        }finally{
            m =null;
            trxDetailpage = null;
        }
    }

    public void error(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        Map data = null;
        try {
            OneCheckoutLogger.log("Receive.processRequest Not Verify OK");
            OneCheckoutV1TransactionBeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1TransactionBeanLocal.class);
            boolean addTrxOnDB = paymentProc.saveInvalidParamsTransactions(onecheckout);
            if (addTrxOnDB) {
                OneCheckoutLogger.log("Receive.processRequest Successful insert Trx into database");
            }

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setPageTemplate("redirect.html");
            redirect.setUrlAction(onecheckout.getMerchant().getMerchantRedirectUrl());

            OneCheckoutPaymentRequest preq = onecheckout.getPaymentRequest();
            if (preq != null) {
                data = redirect.getParameters();
                OneCheckoutChannelBase base = new OneCheckoutChannelBase();
                redirect.setAMOUNT(preq.getAMOUNT());
                if (onecheckout.getFlag().equalsIgnoreCase(OneCheckoutGeneralConstant.CANCEL_BY_USER)) {
                    redirect.setSTATUSCODE(OneCheckoutErrorMessage.CANCEL_BY_USER.value());
                } else {
                    redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_VALIDATE_INPUT.value());
                }

                redirect.setTRANSIDMERCHANT(preq.getTRANSIDMERCHANT());
                data.put("WORDS", base.generateRedirectWords(redirect, onecheckout.getMerchant()));
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(preq.getAMOUNT()));
                if (preq.getTRANSIDMERCHANT() != null && !preq.getTRANSIDMERCHANT().isEmpty()) {
                    data.put("TRANSIDMERCHANT", preq.getTRANSIDMERCHANT());
                }
                data.put("PAYMENTCHANNEL", onecheckout.getPaymentChannel().value());
                if (preq.getSESSIONID() != null && !preq.getSESSIONID().isEmpty()) {
                    data.put("SESSIONID", preq.getSESSIONID());
                }
                data.put("STATUSCODE", redirect.getSTATUSCODE());
                data.put("PAYMENTCODE", "");
            }

            ObjectPage op = PageViewer.redirectPage(redirect, onecheckout.getMerchant(), request, null);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);

        } catch (Throwable t) {
            OneCheckoutLogger.log("Error : " + t.getMessage());

            t.printStackTrace();
        }finally{
            data = null;
//            System.gc();
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.        
        PrintWriter out = response.getWriter();

        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            HttpSession session = req.getSession(false);

            int mId = 0;
            if (session != null) {
                session.invalidate();
            }

            String mallId = request.getParameter(MALLIDParam);
            OneCheckoutLogger.log("Receive.doGet Receiving Payment Request from MALLID=" + mallId);
            OneCheckoutLogger.log("Receive.doGet denied, only receive HTTP POST method");
            if (mallId != null && !mallId.isEmpty()) {
                try {
                    mId = OneCheckoutBaseRules.validateMALLID2(mallId);
                } catch (Exception e) {
                    mId = 0;
                }

            }

            int cmId = -1;
            String chainMerchantId = request.getParameter(CHAINMERCHANTIDParam);
            if (chainMerchantId != null && !chainMerchantId.isEmpty()) {
                try {
                    cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);
                } catch (Throwable e) {
                    cmId = -1;
                }
            }

            ObjectPage op = PageViewer.errorPage(mId, cmId, request);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } catch (TemplateException ex1) {
            RequestDispatcher rd = request.getRequestDispatcher(errorPage);
            rd.forward(request, response);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            //HttpServletRequest req = request;//ESAPI.httpUtilities().getCurrentRequest();
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            String paymentChannelParam = EParameterName.PAYMENT_CHANNEL.code();
            String paymentChannel = req.getParameter(paymentChannelParam);
            if (paymentChannel.equals("17")) {
                response.setContentType("text/html;charset=UTF-8");
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
                response.setDateHeader("Expires", 0); // Proxies.
                HttpSession session = request.getSession(true);
                session.invalidate();
                session = request.getSession(true);
                PrintWriter out = response.getWriter();
                RequestHelper requestHelper = null;
                Merchants merchants = null;
                requestHelper = new RequestHelper();
                requestHelper.setFilteredRequest(req);
                requestHelper.setUnfilterRequest(request);
                String mallIdParam = EParameterName.MALL_ID.code();
                String chainMallIdParam = EParameterName.CHAIN_MALL_ID.code();
                String customerIdParam = EParameterName.CUSTOMER_ID.code();
                String invoiceNumberParam = EParameterName.INVOICE_NUMBER.code();
                String billingNumberParam = EParameterName.BILLING_NUMBER.code();
                String mallId = req.getParameter(mallIdParam);
                String chainMallId = req.getParameter(chainMallIdParam);
                String customerId = req.getParameter(customerIdParam);
                String invoiceNumber = req.getParameter(invoiceNumberParam);
                String billingNumber = req.getParameter(billingNumberParam);
                if (!mallId.equals("") && (chainMallId.equals("") || chainMallId.equals("NA"))) {
                    WebUtils.setSecurityAssociation("RECUR_" + mallId + "_" + customerId + "_" + billingNumber);
                    merchants = oneCheckoutV1QueryHelperBeanLocal.getMerchantByMallId(Integer.valueOf(mallId), 0);
                } else if (!mallId.equals("") && !chainMallId.equals("") && !chainMallId.equalsIgnoreCase("NA")) {
                    WebUtils.setSecurityAssociation("RECUR_" + mallId + "_" + chainMallId + "_" + customerId + "_" + billingNumber);
                    merchants = oneCheckoutV1QueryHelperBeanLocal.getMerchantByMallId(Integer.valueOf(mallId), Integer.valueOf(chainMallId));
                }
                String userAgent = request.getHeader("User-Agent");
                try {
                    OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
                } catch (Throwable th) {
                    OneCheckoutLogger.log("ProcessPaymentCIP.processRequest : Error occur when debug Browser UserAgent");
                }
                if (merchants != null) {
                    MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(merchants, OneCheckoutPaymentChannel.Recur);
                    if (merchantPaymentChannel != null) {
                        requestHelper.setMerchantPaymentChannel(merchantPaymentChannel);
                        HashMap<String, String> data = new HashMap<String, String>();
                        data.put("customerId", customerId);
                        data.put("invoiceNumber", invoiceNumber);
                        data.put("billingNumber", billingNumber);
                        MerchantActivity merchantActivity = manageLocal.startMerchantActivity(requestHelper, merchants, EActivity.DO_RECUR_CUSTOMER_REGISTRATION_REQUEST, data);
                        boolean activityStatus = manageLocal.manageMerchantActivity(requestHelper, merchantActivity);
                        manageLocal.endMerchantActtivity(requestHelper, activityStatus);
                        if (activityStatus) {
                            // CREATE SESSION
                            session.setAttribute(SESSION_NAME, requestHelper);
                            requestHelper.setTemplateFileName("recur.html");
                        } else {
                            session.invalidate();
                        }
                    } else {
                        System.out.println("MERCHANT PAYMENT CHANNEL IS NULL");
                    }
                } else {
                    System.out.println("MERCHANT IS NULL");
                }
                manageLocal.processTemplate(requestHelper, response.getWriter());
            } else if (paymentChannel.equals("16")) {
                response.setContentType("text/html;charset=UTF-8");
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
                response.setDateHeader("Expires", 0); // Proxies.
                HttpSession session = request.getSession(true);
                session.invalidate();
                session = request.getSession(true);
                PrintWriter out = response.getWriter();
                RequestHelper requestHelper = null;
                Merchants merchants = null;
                requestHelper = new RequestHelper();
                requestHelper.setFilteredRequest(req);
                requestHelper.setUnfilterRequest(request);

                String mallIdParam = EParameterName.MALL_ID.code();
                String chainMallIdParam = EParameterName.CHAIN_MALL_ID.code();
                String customerIdParam = EParameterName.CUSTOMER_ID.code();
                String invoiceNumberParam = EParameterName.INVOICE_NUMBER.code();
                String mallId = req.getParameter(mallIdParam);
                String chainMallId = req.getParameter(chainMallIdParam);
                String customerId = req.getParameter(customerIdParam);
                String invoiceNumber = req.getParameter(invoiceNumberParam);
                if (!mallId.equals("") && (chainMallId.equals("") || chainMallId.equals("NA"))) {
                    WebUtils.setSecurityAssociation("TOKENIZATION_" + mallId + "_" + customerId + "_" + invoiceNumber);
                    merchants = oneCheckoutV1QueryHelperBeanLocal.getMerchantByMallId(Integer.valueOf(mallId), 0);
                } else if (!mallId.equals("") && !chainMallId.equals("") && !chainMallId.equalsIgnoreCase("NA")) {
                    WebUtils.setSecurityAssociation("TOKENIZATION_" + mallId + "_" + chainMallId + "_" + customerId + "_" + invoiceNumber);
                    merchants = oneCheckoutV1QueryHelperBeanLocal.getMerchantByMallId(Integer.valueOf(mallId), Integer.valueOf(chainMallId));
                }
                String userAgent = request.getHeader("User-Agent");
                try {
                    OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
                } catch (Throwable th) {
                    OneCheckoutLogger.log("ProcessPaymentCIP.processRequest : Error occur when debug Browser UserAgent");
                }
                if (merchants != null) {
                    MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(merchants, OneCheckoutPaymentChannel.Tokenization);
                    if (merchantPaymentChannel != null) {
                        requestHelper.setMerchantPaymentChannel(merchantPaymentChannel);
                        HashMap<String, String> data = new HashMap<String, String>();
                        data.put("customerId", customerId);
                        MerchantActivity merchantActivity = manageLocal.startMerchantActivity(requestHelper, merchants, EActivity.DO_TOKEN_PAYMENT_REQUEST, data);
                        boolean activityStatus = manageLocal.manageMerchantActivity(requestHelper, merchantActivity);
                        manageLocal.endMerchantActtivity(requestHelper, activityStatus);
                        if (activityStatus) {
                            // CREATE SESSION
                            session.setAttribute(SESSION_NAME, requestHelper);
                            requestHelper.setTemplateFileName("tokenization.html");
                        } else {
                            session.invalidate();
                        }
                    } else {
                        System.out.println("MERCHANT PAYMENT CHANNEL IS NULL");
                    }
                } else {
                    System.out.println("MERCHANT IS NULL");
                }
                manageLocal.processTemplate(requestHelper, response.getWriter());
            } else {
                processRequest(request, response);
            }
        } catch (TemplateException ex) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
