/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author hafiz
 */
public class DWReg extends HttpServlet {

    String MALLIDParam = "MALLID"; 
    String CHAINMERCHANTIDParam = "CHAINMERCHANT";
    
    @Override
    public void init() throws ServletException {
        if ( ESAPI.securityConfiguration().getResourceDirectory() == null ) {
            System.out.println( "====ESAPI.properties ready to define====");
            ESAPI.securityConfiguration().setResourceDirectory( "/apps/ESAPI/resources/");
        }
        else if (!ESAPI.securityConfiguration().getResourceDirectory().equals("/apps/ESAPI/resources/"))   {
            System.out.println( "====ESAPI.properties already define but different path ====");
            ESAPI.securityConfiguration().setResourceDirectory( "/apps/ESAPI/resources/");
        }
        else    {
            System.out.println( "====ESAPI.properties already define ====");
        }


    }     
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, TemplateException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.           
        PrintWriter out = response.getWriter();
        
        HashMap<String, String> params = new HashMap<String, String>();
        String logCode = "";
        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();
        String ack = "FAILED";
        
        try {

            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

            params = WebUtils.copyParams(request);
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            session = req.getSession(true);
            
            logCode = "DWReg" + "_" + request.getParameter("TRANSIDMERCHANT");
            WebUtils.setSecurityAssociation(logCode);               
            
            
            OneCheckoutLogger.log("DWReg.processRequest Redirect from DOKU,  TRANSIDMERCHANT=" + request.getParameter("TRANSIDMERCHANT"));            
            
            onecheckout = WebUtils.parseDWRegistration(params, request, session);
//            boolean verifyOK = false;
            if (onecheckout.getMessage().equals("VALID")) {   
                OneCheckoutLogger.log("DWReg.processRequest parsePaymentRequestDataPrimary VALID");

                
                OneCheckoutLogger.log("DWReg.processRequest verifyOK");
                
                InterfaceOneCheckoutV1BeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
 
                onecheckout = paymentProc.ProcessRedirectToMerchant(onecheckout);
                
                OneCheckoutDataPGRedirect redirect = onecheckout.getRedirect();
                Merchants m = onecheckout.getMerchant();

                OneCheckoutLogger.log("DWReg.processRequest setCookies,  Merchants.merchantCode : %s ", m.getMerchantCode());

             //   Cookie cookie1 = new Cookie("current_PoS", redirect.getCookie());
             //   response.addCookie(cookie1);

                ObjectPage op =  null;
                if(redirect.getRetry() == Boolean.TRUE) {
                    int attempts = Integer.parseInt(redirect.getRetryData().get("ATTEMPTS"));

                    OneCheckoutLogger.log("ATTEMPTS ["+attempts+"]");

                    if(attempts >= 3) {
                        redirect.setRetry(Boolean.FALSE);
                    }

                    OneCheckoutLogger.log("DWReg.processRequest Display Retry Button");
                    op = PageViewer.retryPage(redirect, m, request);
                } else {
                    OneCheckoutLogger.log("DWReg.processRequest noretry");
                    op = PageViewer.redirectPage(redirect, m, request,null);
                }

                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, out);                                
                
            } else {
                OneCheckoutLogger.log("DWReg.processRequest Not verifyOK");
                int mId = onecheckout.getMallId();
                int cmId = onecheckout.getChainMerchantId();
                ObjectPage op = PageViewer.errorPage(mId,cmId,request);
                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, out);                
                              
            }

            

        } catch (Throwable th) {
            th.printStackTrace();
            int mId = 0;
            int cmId = -1;
            /*String mallId = request.getParameter(MALLIDParam);
            if (mallId!=null && !mallId.isEmpty()) {

                try {
                    
                    mId= OneCheckoutBaseRules.validateMALLID2(mallId);
                }
                catch (Exception e) {
                    mId=0;
                }
            
            }
            
            String chainMerchantId = request.getParameter(CHAINMERCHANTIDParam);

            if (chainMerchantId!=null && !chainMerchantId.isEmpty()) {

                try {

                    cmId=OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);

                } catch (Throwable e) {
                    cmId=-1;
                }

            }*/

            ObjectPage op = PageViewer.errorPage(mId,cmId,request);

            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);            
            
        
        } finally {
            out.close();
            params = null;
            logCode = null;
            onecheckout = null;
            ack = null;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
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
            Logger.getLogger(GoBack.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
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
            Logger.getLogger(GoBack.class.getName()).log(Level.SEVERE, null, ex);
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
