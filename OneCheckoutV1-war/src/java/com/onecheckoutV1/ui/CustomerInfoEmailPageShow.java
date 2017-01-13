/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ui;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.scheduler.SendEmailVAToCustomerBeanLocal;
import com.onecheckoutV1.template.CustomerInfoEmailObject;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.view.InfoEmailPage;
import com.onecheckoutV1.view.ObjectPage;
import com.onecheckoutV1.view.WebUtils;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author ahmadfirdaus
 */
public class CustomerInfoEmailPageShow extends HttpServlet {

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
    
    @EJB
    SendEmailVAToCustomerBeanLocal sendEmailVAToCustomerBeanLocal;
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, TemplateException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
            OneCheckoutLogger.log("");
            OneCheckoutDataHelper oneCheckout = (OneCheckoutDataHelper)session.getAttribute(sessionCIPRequest);
            
            String userAgent = request.getHeader("User-Agent");
            try {
                OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
            } catch (Throwable th) {
                OneCheckoutLogger.log("CustomerInfoEmailPageShow.processRequest : Error occur when debug Browser UserAgent");
            }
            
            if(oneCheckout != null){
                String logCode = "CustomerInfoEmailPageShow_" + oneCheckout.getPaymentRequest().getMALLID() + "_" + oneCheckout.getPaymentRequest().getTRANSIDMERCHANT();
                WebUtils.setSecurityAssociation(logCode);
                OneCheckoutLogger.log("Payment Channel TOP : " + (oneCheckout.getPaymentChannel() == null ? null : oneCheckout.getPaymentChannel()));
                
                if(oneCheckout.getMessage().equals("VALID")){
                    OneCheckoutLogger.log("Display Payment Page Object ");
                    oneCheckout = WebUtils.prosesCIPRequestParams(oneCheckout);
                    
                    ObjectPage objectPage;
                    objectPage = InfoEmailPage.infoEmailPage(oneCheckout.getCustomerInfoEmailObject(), oneCheckout.getMerchant(), request);
                    Template temp = objectPage.getTemp();
                    Map root = objectPage.getRoot();
                    temp.process(root, out);
                }
                else{
                    /* CustomerInfoEmailPageShow.invalidPaymentRequestPage belum dibuat
                    OneCheckoutLogger.log("PaymentPageShow.processRequest parsePaymentRequestData NOT VALID");
                    ObjectPage op = CustomerInfoEmailPageShow.invalidPaymentRequestPage(oneCheckout.getMallId(), oneCheckout.getChainMerchantId(), request, oneCheckout.getMessage(),oneCheckout.getMerchant());
                    Template temp = op.getTemp();
                    Map root = op.getRoot();
                    temp.process(root, out);
                    */
                }
            }
            else{
                /* CustomerInfoEmailPageShow.invalidPaymentRequestPage belum dibuat
                OneCheckoutLogger.log("PaymentPageShow.processRequest parsePaymentRequestData NOT VALID");
                ObjectPage op = CustomerInfoEmailPageShow.invalidPaymentRequestPage(oneCheckout.getMallId(), oneCheckout.getChainMerchantId(), request, oneCheckout.getMessage(),oneCheckout.getMerchant());
                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, out);
                */
            }
            
        } 
        catch(Throwable th){
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
            /* CustomerInfoEmailPageShow.errorPage belum dibuat
            ObjectPage op = CustomerInfoEmailPageShow.errorPage(mId, cmId, request);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
            */
        }
        finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (TemplateException ex) {
            Logger.getLogger(CustomerInfoEmailPageShow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (TemplateException ex) {
            Logger.getLogger(CustomerInfoEmailPageShow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
