/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ui;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.*;
import com.onecheckoutV1.servlet.Receive;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutGeneralConstant;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
//import com.onecheckoutV1.type.OneCheckoutTransactionState;
//import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onecheckoutV1.view.ObjectPage;
import com.onecheckoutV1.view.PageViewer;
import com.onecheckoutV1.view.WebUtils;
import com.onechekoutv1.dto.Merchants;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author hafizsjafioedin
 */
public class ProcessPaymentCIP extends HttpServlet {

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, TemplateException {

        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.        
        PrintWriter out = response.getWriter();

        HashMap<String, String> params = new HashMap<String, String>();
        OneCheckoutDataHelper onecheckout = null;
        String logCode = "";
        
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            params = WebUtils.copyParams(request);
            HttpSession session = request.getSession();
            onecheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
            OneCheckoutLogger.log("ProcessPaymentCIP.processRequest Receiving Input Payment Page from MALLID=" + onecheckout.getMallId());
            String button = request.getParameter(BUTTONParam);
            logCode = "afterInputPaymentPage_" + onecheckout.getPaymentRequest().getMALLID() + "_" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
            WebUtils.setSecurityAssociation(logCode);
            String userAgent = request.getHeader("User-Agent");
            try {
                OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
            } catch (Throwable th) {
                OneCheckoutLogger.log("ProcessPaymentCIP.processRequest : Error occur when debug Browser UserAgent");
            }
            if (button != null && button.equalsIgnoreCase(OneCheckoutGeneralConstant.CANCEL_BY_USER)) {
                if (onecheckout.getPaymentChannel() == null) {
                    onecheckout.setPaymentChannel(OneCheckoutBaseRules.validatePaymentChannel(request.getParameter("PAYMENTCHANNEL")));
                }
                OneCheckoutLogger.log("ProcessPaymentCIP.processRequest : " + OneCheckoutGeneralConstant.CANCEL_BY_USER.toString());
                onecheckout.setFlag(button);
                error(onecheckout, request, out);
                //response.sendError(HttpServletResponse.SC_FORBIDDEN, OneCheckoutErrorMessage.CANCEL_BY_USER.value());
                return;
            } else {
                if (onecheckout.getPaymentPageObject() != null &&
                        onecheckout.getPaymentPageObject().getMIPInquiryResponse() != null &&
                        onecheckout.getPaymentPageObject().getMIPInquiryResponse().getResponseCode() != null &&
                        onecheckout.getPaymentPageObject().getMIPInquiryResponse().getResponseCode().equals("0000")) {
                    onecheckout = WebUtils.prosesCIPDOKUWalletData(onecheckout, params, request);
                } //                else if(onecheckout.getPaymentPageObject() != null && onecheckout.getPaymentPageObject().getmIPDokuInquiryResponse() != null &&
                //                        onecheckout.getPaymentPageObject().getmIPDokuInquiryResponse().getResponseCode().equals("0000")){
                //                    onecheckout = WebUtils.prosesCIPDOKUWalletData(onecheckout, params, request);
                //                }
                else {
                    onecheckout = WebUtils.prosesCIPRequestData(onecheckout, params, request);
                }
                if (onecheckout.getMessage().equals("VALID")) {
                    session.setAttribute(sessionCIPRequest, onecheckout);
                    proses(onecheckout, request, out, session);
                } else {
                    OneCheckoutLogger.log("ProcessPaymentCIP.processRequest parsePaymentRequestData NOT VALID");
                    //ObjectPage op = PageViewer.errorPage(onecheckout.getMallId(),onecheckout.getChainMerchantId(), request);
                    ObjectPage op = PageViewer.invalidPaymentRequestPage(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant());
                    Template temp = op.getTemp();
                    Map root = op.getRoot();
                    temp.process(root, out);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
            int mId = 0;
            String mallId = request.getParameter(MALLIDParam);
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
            onecheckout = null;
            logCode = null;
        }
    }

    public void proses(OneCheckoutDataHelper oneCheckoutDataHelper, HttpServletRequest request, PrintWriter out, HttpSession session) {
        Merchants merchants = null;
        try {
            OneCheckoutDataPGRedirect oneCheckoutDataPGRedirect = new OneCheckoutDataPGRedirect();
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            if (oneCheckoutDataHelper.getPaymentChannel() != null &&
                    oneCheckoutDataHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY &&
                    (oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL() == null || oneCheckoutDataHelper.getPaymentRequest().getDOKUWALLETCHANNEL().trim().equals(""))) {
                OneCheckoutChannelBase oneCheckoutChannelBase = new OneCheckoutChannelBase();
                boolean status = oneCheckoutChannelBase.getDOKUWalletMIPStatus(oneCheckoutDataHelper);
                if (status) {
                    InterfaceOneCheckoutV1BeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
                    oneCheckoutDataHelper = paymentProc.ProcessPayment(oneCheckoutDataHelper);
                    session.removeAttribute(sessionCIPRequest);
                    session.setAttribute(sessionCIPRequest, oneCheckoutDataHelper);

                    String actionUrl = "/Suite/ProcessPaymentCIP?MALLID=" + oneCheckoutDataHelper.getMallId() + "&CHAINMERCHANT=" + oneCheckoutDataHelper.getChainMerchantId() + "&INV=" + oneCheckoutDataHelper.getPaymentRequest().getTRANSIDMERCHANT();
                    oneCheckoutDataPGRedirect.setUrlAction(actionUrl);
                    oneCheckoutDataPGRedirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
                    PaymentPageObject paymentPageObject = oneCheckoutDataHelper.getPaymentPageObject();
                    if (oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse() != null &&
                            oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse().getResponseCode() != null &&
                            oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse().getResponseCode().equals("0000")) {
                        oneCheckoutDataPGRedirect.setPageTemplate("dokuwalletdetail.html");
                    } //                    else if (oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse() !=  null &&
                    //                            oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse().getResponseCode() != null &&
                    //                            oneCheckoutDataHelper.getPaymentPageObject().getmIPDokuInquiryResponse().getResponseCode().equals("0000")){
                    //                            oneCheckoutDataPGRedirect.setPageTemplate("dokuwalletpromotion.html");
                    //                    }
                    else {
                        paymentPageObject.setMessage(oneCheckoutDataHelper.getPaymentPageObject().getMIPInquiryResponse().getResponseMsg());
                        oneCheckoutDataPGRedirect.setPageTemplate("paymentpage.html");
                    }
                    merchants = oneCheckoutDataHelper.getMerchant();
                    ObjectPage op = PageViewer.redirectPage(oneCheckoutDataPGRedirect, merchants, request, paymentPageObject);
                    Template temp = op.getTemp();
                    Map root = op.getRoot();
                    temp.process(root, out);
                    return;
                }
            }
            String actionUrl = config.getString("PAYMENT.ACTION.URL", "/Suite/ProcessPayment");
            actionUrl = actionUrl + "?MALLID=" + oneCheckoutDataHelper.getMallId() + "&CHAINMERCHANT=" + oneCheckoutDataHelper.getChainMerchantId() + "&INV=" + oneCheckoutDataHelper.getPaymentRequest().getTRANSIDMERCHANT();

            oneCheckoutDataPGRedirect.setUrlAction(actionUrl);
            oneCheckoutDataPGRedirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            oneCheckoutDataPGRedirect.setPageTemplate("redirect.html");
            merchants = oneCheckoutDataHelper.getMerchant();
            PaymentPageObject trxDetailpage = oneCheckoutDataHelper.getPaymentPageObject();

            ObjectPage op = PageViewer.redirectPage(oneCheckoutDataPGRedirect, merchants, request, trxDetailpage);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } catch (Throwable t) {
            t.printStackTrace();
            error(oneCheckoutDataHelper, request, out);
        }finally{
            merchants = null;
        }
    }

    public void error(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        try {
            OneCheckoutLogger.log("ProcessPaymentCIP.processRequest Not Verify OK");
            OneCheckoutV1TransactionBeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1TransactionBeanLocal.class);
            boolean addTrxOnDB = paymentProc.saveInvalidParamsTransactions(onecheckout);
            if (addTrxOnDB) {
                OneCheckoutLogger.log("ProcessPaymentCIP.processRequest Successful insert Trx into database");
            }

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setPageTemplate("redirect.html");
            redirect.setUrlAction(onecheckout.getMerchant().getMerchantRedirectUrl());

            OneCheckoutPaymentRequest preq = onecheckout.getPaymentRequest();
            if (preq != null) {
                Map data = redirect.getParameters();
                OneCheckoutChannelBase base = new OneCheckoutChannelBase();
                redirect.setAMOUNT(preq.getAMOUNT());
                if (onecheckout.getFlag() != null && onecheckout.getFlag().equalsIgnoreCase(OneCheckoutGeneralConstant.CANCEL_BY_USER)) {
                    redirect.setSTATUSCODE(OneCheckoutErrorMessage.CANCEL_BY_USER.value());
                } else {
                    redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_VALIDATE_INPUT.value());
                }

                if (preq.getRATE() != null) {
                    redirect.setAMOUNT(preq.getPURCHASEAMOUNT());
                    data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(preq.getPURCHASEAMOUNT()));
                    data.put("CURRENCY", preq.getPURCHASECURRENCY());//.getPURCHASECURRENCY());
                } else {
                    data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(preq.getAMOUNT()));
                    data.put("CURRENCY", preq.getCURRENCY() != null ? preq.getCURRENCY() : "");
                }

                redirect.setTRANSIDMERCHANT(preq.getTRANSIDMERCHANT());
                redirect.setPURCHASECURRENCY(preq.getPURCHASECURRENCY() != null ? preq.getPURCHASECURRENCY() : "");
                data.put("WORDS", base.generateRedirectWords(redirect, onecheckout.getMerchant()));
                if (preq.getTRANSIDMERCHANT() != null && !preq.getTRANSIDMERCHANT().isEmpty()) {
                    data.put("TRANSIDMERCHANT", preq.getTRANSIDMERCHANT());
                }
                data.put("PAYMENTCHANNEL", onecheckout.getPaymentChannel().value());
                if (preq.getSESSIONID() != null && !preq.getSESSIONID().isEmpty()) {
                    data.put("SESSIONID", preq.getSESSIONID());
                }
                data.put("STATUSCODE", redirect.getSTATUSCODE());
                data.put("PAYMENTCODE", "");
                data.put("PURCHASECURRENCY", preq.getPURCHASECURRENCY() != null ? preq.getPURCHASECURRENCY() : "");
                data.put("PURCHASEAMOUNT", OneCheckoutVerifyFormatData.sdf.format(preq.getPURCHASEAMOUNT()));
            }

            OneCheckoutLogger.log("REDIRECT PARAM : \n" + redirect.getParameters().toString());
            ObjectPage op = PageViewer.redirectPage(redirect, onecheckout.getMerchant(), request, null);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);

        } catch (Throwable t) {
            OneCheckoutLogger.log("Error : " + t.getMessage());
            t.printStackTrace();
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
            OneCheckoutLogger.log("ProcessPaymentCIP.doGet Receiving Payment Request from MALLID=" + mallId);
            OneCheckoutLogger.log("ProcessPaymentCIP.doGet denied, only receive HTTP POST method");
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

            processRequest(request, response);
        } catch (TemplateException ex) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}