/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.servlet;

import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.RequestHelper;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.enums.EActivity;
import com.onecheckoutV1.enums.EParameterName;
import com.onecheckoutV1.manage.ManageLocal;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.view.WebUtils;
import com.onechekoutv1.dto.MerchantActivity;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author Opiks
 */
public class RecurUpdateCard extends HttpServlet {
    
    String SESSION_NAME = "OCOSESSIONDOKU";
    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;

    @EJB
    ManageLocal manageLocal;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String esapiD = ESAPI.securityConfiguration().getResourceDirectory();
            if (esapiD == null) {
                esapiD = "";
            }
            if (!esapiD.equals("/apps/ESAPI/resources/")) {
                ESAPI.securityConfiguration().setResourceDirectory("/apps/ESAPI/resources/");
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
        /*
        String sessionid = request.getSession().getId();
        String contextPath = request.getContextPath();
        String secure = "";
        if (request.isSecure()) {
            secure = "; Secure";
        }
        response.setHeader("SET-COOKIE", "JSESSIONID=" + sessionid + "; Path=" + contextPath + "; HttpOnly" + secure);
         */
        
        PrintWriter out = response.getWriter();
        RequestHelper requestHelper = null;
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            HttpSession session = request.getSession(true);
            session.invalidate();
            session = request.getSession(true);
            Merchants merchants = null;
            requestHelper = new RequestHelper();
            requestHelper.setFilteredRequest(req);
            requestHelper.setUnfilterRequest(request);
            String mallId = req.getParameter(EParameterName.MALL_ID.code());
            String chainMallId = req.getParameter(EParameterName.CHAIN_MALL_ID.code());
            String customerId = req.getParameter(EParameterName.CUSTOMER_ID.code());
            String invoiceNumber = req.getParameter(EParameterName.INVOICE_NUMBER.code());
            String billingNumber = req.getParameter(EParameterName.BILLING_NUMBER.code());
            if (!mallId.equals("") && (chainMallId.equals("") || chainMallId.equals("NA"))) {
                WebUtils.setSecurityAssociation("RECURUPDATE_" + mallId + "_" + customerId + "_" + billingNumber);
                merchants = oneCheckoutV1QueryHelperBeanLocal.getMerchantByMallId(Integer.valueOf(mallId), 0);
            } else if (!mallId.equals("") && !chainMallId.equals("") && !chainMallId.equalsIgnoreCase("NA")) {
                WebUtils.setSecurityAssociation("RECURtUPDATE_" + mallId + "_" + chainMallId + "_" + customerId + "_" + billingNumber);
                merchants = oneCheckoutV1QueryHelperBeanLocal.getMerchantByMallId(Integer.valueOf(mallId), Integer.valueOf(chainMallId));
            }

            if (merchants != null) {
                MerchantPaymentChannel merchantPaymentChannel = oneCheckoutV1QueryHelperBeanLocal.getMerchantPaymentChannel(merchants, OneCheckoutPaymentChannel.Recur);
                if (merchantPaymentChannel != null) {
                    requestHelper.setMerchantPaymentChannel(merchantPaymentChannel);
                    HashMap<String, String> data = new HashMap<String, String>();
                    data.put("customerId", customerId);
                    data.put("invoiceNumber", invoiceNumber);
                    data.put("billingNumber", billingNumber);
                    MerchantActivity merchantActivity = manageLocal.startMerchantActivity(requestHelper, merchants, EActivity.DO_RECUR_CUSTOMER_UPDATE_REQUEST, data);
                    boolean activityStatus = manageLocal.manageMerchantActivity(requestHelper, merchantActivity);
                    manageLocal.endMerchantActtivity(requestHelper, activityStatus);
                    if (activityStatus) {
                        // CREATE SESSION
                        session.setAttribute(SESSION_NAME, requestHelper);
                        requestHelper.setTemplateFileName("recur.html");
                    } else {
                        session.invalidate();
                    }
                } else {
                    System.out.println("MERCHANT PAYMENT CHANNEL IS NULL");
                }
            } else {
                System.out.println("MERCHANT IS NULL");
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        manageLocal.processTemplate(requestHelper, response.getWriter());
        System.out.println("---------------------------------------------------------------------------------------");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("                         . : : DO RECUR UPDATE BILLING : : .");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("   REQUEST PARAM :");
        /*
        Map m = request.getParameterMap();
        Iterator i = m.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String value = ((String[]) m.get(key))[0];
            System.out.println("   [" + key + "] => [" + value + "]");
        }
        */
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println(" GET METHOD IS NOT ALLOWED");
        System.out.println("---------------------------------------------------------------------------------------");
        manageLocal.processTemplate(null, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("                         . : : DO RECUR UPDATE BILLING : : .");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("   REQUEST PARAM :");
        /*
        Map m = request.getParameterMap();
        Iterator i = m.keySet().iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String value = ((String[]) m.get(key))[0];
            System.out.println("   [" + key + "] => [" + value + "]");
        }
        */
        System.out.println("---------------------------------------------------------------------------------------");
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "RecurUpdateCard";
    }

}
