/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.servlet;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.view.WebUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author aditya
 */
public class MandiriSOAFullPayment extends HttpServlet {

    String STEPParam = "STEP";

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HashMap<String, String> params = new HashMap<String, String>();
        String logCode = "";
        OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();
        String ack = "STOP";
        
        try {


            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

            params = WebUtils.copyParams(request);
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            session = req.getSession(true);

            String STEP = request.getParameter(STEPParam);

            logCode = "MandiriSOAFullPayment" + "_" + request.getParameter("TRANSACTIONID")+ "_" + request.getParameter("USERACCOUNT");
            WebUtils.setSecurityAssociation(logCode);
            OneCheckoutLogger.log("MandiriSOAFullPayment.processRequest Receiving Payment Request from COMPANYCODE=" + request.getParameter("COMPANYCODE") + ", USERACCOUNT=" + request.getParameter("USERACCOUNT") + ", TRANSACTIONID=" + request.getParameter("TRANSACTIONID"));

            onecheckout = WebUtils.parseNotifyDOKURequestData(params, request, session, OneCheckoutPaymentChannel.MandiriSOAFull);

            if (onecheckout.getMessage().equals("VALID")) {

                OneCheckoutLogger.log("MandiriSOAFullPayment.processRequest Parameters Valid");

                InterfaceOneCheckoutV1BeanLocal proc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);

                OneCheckoutLogger.log("MandiriSOAFullPayment.processRequest STEP :" + STEP);
                
                if(STEP != null && STEP.equalsIgnoreCase("REVERSE")) {

                    OneCheckoutLogger.log("PermataVAFullPayment.processRequest REVERSAL");

                    onecheckout = proc.ProcessReversal(onecheckout);

                } else if (STEP != null && STEP.equalsIgnoreCase("SETTLEMENT")) {

                    OneCheckoutLogger.log("MandiriSOAFullPayment.processRequest SETTLEMENT");

                    onecheckout = proc.ProcessReconcile(onecheckout);

                } else {

                    OneCheckoutLogger.log("MandiriSOAFullPayment.processRequest PAYMENT");

                    onecheckout = proc.ProcessInvokeStatus(onecheckout);

                }

                ack = onecheckout.getNotifyRequest().getACKNOWLEDGE();

            }

            //OneCheckoutLogger.log("MandiriSOAFullPayment.processRequest ACKNOWLEDGE : %s", ack);
            System.out.println("MandiriSOAFullPayment.processRequest ACKNOWLEDGE : "+ack);
            out.print(ack);

        } catch (Throwable th) {
            out.print("STOP");
            OneCheckoutLogger.log("MandiriSOAFullPayment.processRequest ACKNOWLEDGE : STOP");

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
        processRequest(request, response);
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
        processRequest(request, response);
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