/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.util.AESTools;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.template.CustomerInfoEmailObject;
import com.onechekoutv1.dto.Rates;
import com.onechekoutv1.dto.Transactions;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author ahmadfirdaus
 */
public class CheckingPurchaseRates extends HttpServlet {

    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;

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
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            //OneCheckoutV1QueryHelperBeanLocal ocoBean = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

            String templatePath = config.getString("EMAIL.ALERT.TEMPLATE");
            String templateFileName = "rate_information.html";
            //String purchaseCurrency = request.getParameter("purchaseCurrency");
            //String currency = request.getParameter("currency");
            //String purchaseAmount = request.getParameter("purchaseAmount");
            //String merchantName = request.getParameter("merchantName");
            String transactionId = req.getParameter("refNumber");
            transactionId = AESTools.decrypt(transactionId);
            Transactions transactions = oneCheckoutV1QueryHelperBeanLocal.getTransactionByID(Long.valueOf(transactionId));
            if (transactions != null) {
                Rates rate = oneCheckoutV1QueryHelperBeanLocal.getRates(transactions.getIncCurrency(), transactions.getIncPurchasecurrency());
                double rateValue = rate.getFinalRate().doubleValue();
                String buyCurrencyCode = rate.getBuyCurrencyCode();
                File file = new File(templatePath + templateFileName);
                boolean fileExist = file.exists();
                if (!fileExist) {
                    OneCheckoutLogger.log(templateFileName + " does not exist");
                } else {
                    OneCheckoutLogger.log(templateFileName + " exist");
                }
                CustomerInfoEmailObject infoEmail = new CustomerInfoEmailObject();
                infoEmail.setRates(rateValue);
                infoEmail.setPuschaseRates(transactions.getIncPurchaseamount().doubleValue() * rateValue);
                infoEmail.setBuyCurrencyCode(buyCurrencyCode);
                infoEmail.setCustomerAmount(OneCheckoutVerifyFormatData.moneyFormat.format(transactions.getIncPurchaseamount()));
                infoEmail.setMerchantName(transactions.getMerchantPaymentChannel().getMerchants().getMerchantName());

                Configuration cfg = new Configuration();
                cfg.setDirectoryForTemplateLoading(new File(templatePath));
                cfg.setObjectWrapper(new DefaultObjectWrapper());
                Template temp = cfg.getTemplate(templateFileName);
                Map root = new HashMap();
                root.put("CustomerInfoEmailObject", infoEmail);
                temp.process(root, out, ObjectWrapper.BEANS_WRAPPER);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
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
