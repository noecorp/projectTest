/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckoutV1.data.OneCheckoutCyberSourceData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.view.WebUtils;
import java.io.IOException;
import java.io.PrintWriter;
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
public class CyberSource extends HttpServlet {

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

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {

            ESAPI.httpUtilities().setCurrentHTTP(request, response);

            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            session = req.getSession(true);

            String content = req.getParameter("content");

            OneCheckoutLogger.log("==========================================");
            OneCheckoutLogger.log(": : : GETTING REVIEW RESULT FROM CYBERSOURCE :.");
            OneCheckoutLogger.log("==========================================");
            System.out.println("XML : " + content);
            OneCheckoutLogger.log("==========================================");

            String ack = "";
            if (content == null || content.isEmpty()) {

                OneCheckoutLogger.log("==========================================");
                OneCheckoutLogger.log(": : : ERROR PARAM [content] is Null :.");
                OneCheckoutLogger.log("==========================================");
                ack = "Empty value of CONTENT parameter";

            } else  {

                OneCheckoutDataHelper onecheckout = new OneCheckoutDataHelper();

                OneCheckoutCyberSourceData cybs = new OneCheckoutCyberSourceData(content);

                String logCode = "CyberSourceReview" + "_" + cybs.getInvoiceNumber();
                WebUtils.setSecurityAssociation(logCode);
                OneCheckoutLogger.log("CyberSourceReview.processRequest Receiving CyberSource Review REVIEWERID=" + cybs.getReviewer() + ", RESULT=" + cybs.getNewDecision());

                onecheckout.setPaymentChannel(OneCheckoutPaymentChannel.CreditCard);
                onecheckout.setCyberSourceData(cybs);

                InterfaceOneCheckoutV1BeanLocal proc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);

                onecheckout = proc.ProcessCyberSourceReview(onecheckout);

                ack = onecheckout.getMessage();
                
            }

            out.print(ack);

        } catch (Throwable th) {
            out.print("STOP");

        } finally {
            out.close();
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