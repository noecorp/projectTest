/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.servlet;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutV1CreditCardBeanLocal;
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
public class EDSGarudaGetData extends HttpServlet {

    OneCheckoutV1CreditCardBeanLocal creditCardService = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1CreditCardBeanLocal.class);

    @Override
    public void init() throws ServletException {

        if ( ESAPI.securityConfiguration().getResourceDirectory() == null ) {
            System.out.println( "====ESAPI.properties ready to define====");
            ESAPI.securityConfiguration().setResourceDirectory( "/apps/ESAPI/resources/");
        } else if (!ESAPI.securityConfiguration().getResourceDirectory().equals("/apps/ESAPI/resources/"))   {
            System.out.println( "====ESAPI.properties already define but different path ====");
            ESAPI.securityConfiguration().setResourceDirectory( "/apps/ESAPI/resources/");
        } else {
            System.out.println( "====ESAPI.properties already define ====");
        }

    }


    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
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

            logCode = "EDSGetData_" + request.getParameter("invoiceNo");
            WebUtils.setSecurityAssociation(logCode);
            OneCheckoutLogger.log("EDSGetData.processRequest Receiving GetData Request from amount=" + request.getParameter("amount") + ", invoiceNo=" + request.getParameter("invoiceNo"));

            onecheckout = WebUtils.parseEDSGetDataRequestData(params, request, session, OneCheckoutPaymentChannel.CreditCard);

            OneCheckoutLogger.log("MESSAGE : %s",onecheckout.getMessage());
            if (onecheckout.getMessage().equals("VALID")) {

                OneCheckoutLogger.log("EDSGetData.processRequest Parameters Valid");

                InterfaceOneCheckoutV1BeanLocal proc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);

                onecheckout = proc.ProcessGetEDSData(onecheckout);

                ack = onecheckout.getEdsGetData().getACKNOWLEDGE();

//                if(ack == null || onecheckout.getEdsGetData().isNULLTRANSACTION() == Boolean.TRUE) {
//                    ack = MOBTransactionData(onecheckout.getEdsGetData());
//                }

//            } else {
//                ack = MOBTransactionData(onecheckout.getEdsGetData());
            }

            OneCheckoutLogger.log("EDSGetData.processRequest ACKNOWLEDGE : %s", ack);
            out.print(ack);

        } catch (Throwable th) {

            out.print("FAILED");
            OneCheckoutLogger.log("EDSGetData.processRequest ACKNOWLEDGE : FAILED");

        } finally {
            out.close();
            params = null;
            logCode = null;
            onecheckout = null;
            ack = null;
            
        }
    }

//    public String MOBTransactionData(OneCheckoutEDSGetData data) {
//        String response = "";
//
//        try {
//           response = creditCardService.getMOBTransactionData(data);
//
//           if (response == null || response.isEmpty()) {
//                OneCheckoutLogger.log("EDSGetData.processRequest : failed when getting Data to MOB BA");
//                response = creditCardService.getEDSDataEmptyACK();
//            }
//        } catch (Throwable th) {
//            OneCheckoutLogger.log("EDSGetData.processRequest : "+th.getMessage());
//        }
//
//        return response;
//    }

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
    }
}
