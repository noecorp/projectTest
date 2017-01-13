/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ui;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.servlet.Receive;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onecheckoutV1.view.ObjectPage;
import com.onecheckoutV1.view.PageViewer;
import com.onecheckoutV1.view.WebUtils;
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
 * @author hafizsjafioedin
 */
public class PaymentPageShow extends HttpServlet {

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

        OneCheckoutDataHelper onecheckout = null;
        String logCode = "";
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
       //     HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
     //       HashMap<String, String> params = WebUtils.copyParams(request);
            HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
            OneCheckoutLogger.log("PaymentPageShow.processRequest got OneCheckoutDataHelper from session");            
            onecheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);
            session.setMaxInactiveInterval(15 * 60);
            String userAgent = request.getHeader("User-Agent");
            try {
                OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
            } catch (Throwable th) {
                OneCheckoutLogger.log("PaymentPageShow.processRequest : Error occur when debug Browser UserAgent");
            }

            if (onecheckout != null) {  
               // postFlag = true;
                logCode = "PaymentPageShow_" + onecheckout.getPaymentRequest().getMALLID() + "_" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
                WebUtils.setSecurityAssociation(logCode);
                OneCheckoutLogger.log("Payment Channel TOP : " + (onecheckout.getPaymentChannel() == null ? null : onecheckout.getPaymentChannel()));
                
                
                if (onecheckout.getMessage().equals("VALID")) {
                    
                    // SHOW PAYMENT PAGE
                    OneCheckoutLogger.log("Display Payment Page Object ");
                    onecheckout = WebUtils.prosesCIPRequestParams(onecheckout);
                    
                    ObjectPage op = PageViewer.paymentPage(onecheckout.getPaymentPageObject(), onecheckout.getMerchant(), request);
                    Template temp = op.getTemp();
                    Map root = op.getRoot();
                    temp.process(root, out);                
                
                
                    
                } else {
                    OneCheckoutLogger.log("PaymentPageShow.processRequest parsePaymentRequestData NOT VALID");
                    ObjectPage op = PageViewer.invalidPaymentRequestPage(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(),onecheckout.getMerchant());
                    Template temp = op.getTemp();
                    Map root = op.getRoot();
                    temp.process(root, out);
                }                
                
                
            } else {
                OneCheckoutLogger.log("PaymentPageShow.processRequest parsePaymentRequestData NOT VALID");
                ObjectPage op = PageViewer.invalidPaymentRequestPage(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(),onecheckout.getMerchant());
                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, out);
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
            logCode = null;
            onecheckout = null;
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
            OneCheckoutLogger.log("PaymentPageShow.doGet Receiving Payment Request from MALLID=" + mallId);
            OneCheckoutLogger.log("PaymentPageShow.doGet denied, only receive HTTP POST method");
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
