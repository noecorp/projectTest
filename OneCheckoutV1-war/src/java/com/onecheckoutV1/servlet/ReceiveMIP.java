/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.servlet;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.view.WebUtils;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
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
 * @author aditya
 */
public class ReceiveMIP extends HttpServlet {

    String MALLIDParam = "MALLID";
    String BASKETParam = "BASKET";
    String errorPage = "/ErrorPage";

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, TemplateException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HashMap<String, String> params = new HashMap<String, String>();
        String mallId = "";
        String logCode = "";
        OneCheckoutDataHelper onecheckout = null;
        String ack = "STOP";
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            params = WebUtils.copyParams(request);
            HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));

            mallId = request.getParameter(MALLIDParam);
            logCode = "MIPXML_" + mallId + "_" + request.getParameter("TRANSIDMERCHANT");
            WebUtils.setSecurityAssociation(logCode);

            onecheckout = WebUtils.parsePaymentRequestData(params, request, session);
            String userAgent = request.getHeader("User-Agent");
            OneCheckoutLogger.log("MIPPayment.userAgent : "+userAgent);
            OneCheckoutLogger.log("BASKET : "+request.getParameter(BASKETParam));
            System.out.println("================================");
            OneCheckoutLogger.log("BASKET ESAPI : "+req.getParameter(BASKETParam));
            
            if (onecheckout != null && onecheckout.getMessage() != null && onecheckout.getMessage().trim().equals("VALID")) {
                OneCheckoutLogger.log("MIPPayment.processRequest Parameters Valid");
                InterfaceOneCheckoutV1BeanLocal proc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
                onecheckout = proc.ProcessMIPPayment(onecheckout);
                ack = onecheckout.getMessage();
            }
            OneCheckoutLogger.log("MIPPayment.processRequest ACKNOWLEDGE : %s", ack);
            out.print(ack);
        } catch (Throwable th) {
            th.printStackTrace();
            request.getSession().invalidate();
        } finally {
            out.close();
            params = null;
            mallId = null;
            logCode = null;
            onecheckout = null;
            ack = null;
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("GET DOESN'T SUPPORT");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Throwable t) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, null, t);
        }
    }
}