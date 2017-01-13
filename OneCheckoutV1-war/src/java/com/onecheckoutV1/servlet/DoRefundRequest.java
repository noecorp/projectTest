/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
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
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
public class DoRefundRequest extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
//        response.setContentType("text/html;charset=UTF-8");
        response.setContentType("application/xml;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.        
        PrintWriter out = response.getWriter();

        HashMap<String, String> params = new HashMap<String, String>();
        String mallId = "";
        String logCode = "";
        OneCheckoutDataHelper onecheckout = null;
        String responseToMerchant = null;
        HashMap<String, Object> responseXml = new HashMap<String, Object>();

        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            String requestData = request.getParameter("data");
            params = WebUtils.copyParams(request);
            HttpSession session = request.getSession();

            if (request.getSession() != null) {
                session.invalidate();
            }
            session = request.getSession(true);

            logCode = "DoRefundPayment_";
            if (params.containsKey("MALLID")) {
                logCode = "DoRefundPayment" + params.get("MALLID") + "_" + params.get("TRANSIDMERCHANT");
            }
            WebUtils.setSecurityAssociation(logCode);

            OneCheckoutLogger.log(":::::::: Process DoRefundPayment (START)::::::::");

//            parameter API
//                    1. req_merchant_code
//                            2. req_trans_id_merchant
//                                    3. req_chain_merchant(optional)
//                                            4. req_words
//                                                    5. req_refund_id
//                                                            6. req_reason_refund
//                                                                    7.req_refund_id
            try {
                InterfaceOneCheckoutV1BeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
                OneCheckoutDataHelper result = WebUtils.processRefundPayment(params);

//                responseToMerchant = gson.toJson(result);
//                response.getWriter().write(responseToMerchant);
//                response.flushBuffer();
                if (result.getMessage() != null) {
                    responseToMerchant = result.getMessage().replace("MPGRefundResponse", "REFUND_RESPONSE");
                } else {
                    OneCheckoutLogger.log("::: NO RESPONSE FROM MPG :::");
                    responseToMerchant = WebUtils.createXML(result.getRefundHelper().getMapResultRefund(), "REFUND_RESPONSE");
                }
                response.getWriter().write(responseToMerchant);
                response.flushBuffer();
                out.close();

            } catch (Throwable th) {
                th.printStackTrace();
                HashMap<String, Object> responseError = new HashMap<String, Object>();
                responseError.put("RESPONSECODE", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.value());
                responseError.put("RESPONSEMSG", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.name());
                responseToMerchant = WebUtils.createXML(responseError, "REFUND_RESPONSE");
                response.getWriter().write(responseToMerchant);
                response.flushBuffer();
                OneCheckoutLogger.log("Error in -DoRefundPayment- : " + th.getMessage());
            }
        } catch (Throwable th) {
            responseXml.put("RESPONSECODE", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.value());
            responseXml.put("RESPONSEMSG", OneCheckoutErrorMessage.INTERNAL_SERVER_ERROR.name());
            OneCheckoutLogger.log("Error = " + th.getMessage());
            responseToMerchant = WebUtils.createXML(responseXml, "REFUND_RESPONSE");
            response.getWriter().write(responseToMerchant);
            response.flushBuffer();
            th.printStackTrace();
        } finally {

            OneCheckoutLogger.log("::: RESPONSE DoRefundPayment [ " + responseToMerchant + " ]");
            OneCheckoutLogger.log(":::::::: Process DoRefundPayment (END)::::::::");
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
        processRequest(request, response);
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
