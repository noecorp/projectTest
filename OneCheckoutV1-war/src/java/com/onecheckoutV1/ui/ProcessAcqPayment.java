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
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.servlet.Receive;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutGeneralConstant;
import com.onecheckoutV1.view.ObjectPage;
import com.onecheckoutV1.view.PageViewer;
import com.onecheckoutV1.view.WebUtils;
import com.onechekoutv1.dto.Merchants;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author Opiks
 */
public class ProcessAcqPayment extends HttpServlet {

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
        PrintWriter out = response.getWriter();
        
        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();
        String logCode = "";
        
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpSession session = request.getSession();
            onecheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
            try {
                if (onecheckout != null) {
                    OneCheckoutLogger.log("ProcessAcqPayment.processRequest Receiving Input Payment Page from MALLID=" + onecheckout.getMallId());
                    if (request.getSession() != null) {
                        session.invalidate();
                    }
                    session = request.getSession(true);
                    session.setAttribute(sessionCIPRequest, onecheckout);
                } else {
                    OneCheckoutLogger.log("XXXXXX ProcessAcqPayment.processRequest - Customer Refresh on LoadingPage ACQUIRER REDIRECT...");
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
            String userAgent = request.getHeader("User-Agent");
            try {
                OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
            } catch (Throwable th) {
                OneCheckoutLogger.log("ProcessAcqPayment.processRequest : Error occur when debug Browser UserAgent");
            }
            logCode = "AuthAcquirer_" + onecheckout.getMallId() + "_" + onecheckout.getTransactions().getIncTransidmerchant();
            WebUtils.setSecurityAssociation(logCode);
            proses(onecheckout, request, out);
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
            request.getSession().invalidate();
        } finally {
            out.close();
            onecheckout = null;
            logCode = null;
        }
    }

    public void proses(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        try {
            InterfaceOneCheckoutV1BeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
            onecheckout = paymentProc.ProcessPayment(onecheckout);
            OneCheckoutDataPGRedirect redirect = onecheckout.getRedirect();
            Merchants m = onecheckout.getMerchant();
            PaymentPageObject trxDetailpage = onecheckout.getPaymentPageObject();
            ObjectPage op = PageViewer.redirectPage(redirect, m, request, trxDetailpage);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } catch (Throwable t) {
            t.printStackTrace();
            error(onecheckout, request, out);
        }
    }

    public void error(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        try {
            OneCheckoutLogger.log("ProcessAcqPayment.processRequest Not Verify OK");
            OneCheckoutV1TransactionBeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1TransactionBeanLocal.class);
            boolean addTrxOnDB = paymentProc.saveInvalidParamsTransactions(onecheckout);
            if (addTrxOnDB) {
                OneCheckoutLogger.log("ProcessAcqPayment.processRequest Successful insert Trx into database");
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
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try {
            processRequest(request, response);
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
