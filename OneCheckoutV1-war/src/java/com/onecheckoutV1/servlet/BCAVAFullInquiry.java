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
 * @author margono
 */
public class BCAVAFullInquiry extends HttpServlet {

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
        response.setContentType("text/html;charset=UTF-8");
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

            logCode = "BCAVAFullInquiry" + "_" + request.getParameter("MALLID") + "_" + request.getParameter("VANUMBER");
            WebUtils.setSecurityAssociation(logCode);
            OneCheckoutLogger.log("BCAVAFullInquiry.processRequest Receiving Verify Request from merchantCode=" + request.getParameter("MALLID") + ", VANUMBER=" + request.getParameter("VANUMBER"));

            onecheckout = WebUtils.parseVerifyDOKURequestData(params, request, session, OneCheckoutPaymentChannel.BCAVAFull);

            if (onecheckout.getMessage().equals("VALID")) {

                OneCheckoutLogger.log("BCAVAFullInquiry.processRequest Parameters Valid");

                InterfaceOneCheckoutV1BeanLocal proc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);

                onecheckout = proc.ProcessInquiryInvoice(onecheckout);

                ack = onecheckout.getVerifyRequest().getACKNOWLEDGE();

            }

            System.out.println("BCAVAFullInquiry.processRequest ACKNOWLEDGE : "+ack);
            //OneCheckoutLogger.log("PermataVAInquiry.processRequest ACKNOWLEDGE : %s", ack);

            out.print(ack);

        } catch (Throwable th) {
            out.print("STOP");
            OneCheckoutLogger.log("BCAVAFullInquiry.processRequest ACKNOWLEDGE : STOP");

        } finally {
            out.close();
            params = null;
            logCode = null;
            onecheckout = null;
            ack = null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
