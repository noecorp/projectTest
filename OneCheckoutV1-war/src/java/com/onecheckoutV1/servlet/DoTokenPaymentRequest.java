/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.servlet;

import com.onecheckoutV1.ejb.helper.CreditCardHelper;
import com.onecheckoutV1.ejb.helper.RequestHelper;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.enums.EActivity;
import com.onecheckoutV1.enums.EParameterName;
import com.onecheckoutV1.manage.ManageLocal;
import com.onecheckoutV1.view.WebUtils;
import com.onechekoutv1.dto.MerchantActivity;
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
public class DoTokenPaymentRequest extends HttpServlet {
    
    String SESSION_NAME = "OCOSESSIONDOKU";
    
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
        HttpSession session = request.getSession(true);
        PrintWriter out = response.getWriter();
        RequestHelper requestHelper = null;
        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            requestHelper = (RequestHelper) session.getAttribute(SESSION_NAME);
            if (requestHelper != null && requestHelper.getMerchantActivity() != null && requestHelper.getMerchantActivity().getMerchantIdx() != null) {
                if (requestHelper.getMerchantActivity().getMerchantIdx().getMerchantChainMerchantCode() != null && requestHelper.getMerchantActivity().getMerchantIdx().getMerchantChainMerchantCode() > 0) {
                    WebUtils.setSecurityAssociation("TOKEN_" + requestHelper.getMerchantActivity().getMerchantIdx().getMerchantCode() + "_" + requestHelper.getMerchantActivity().getMerchantIdx().getMerchantChainMerchantCode() + "_" + requestHelper.getTokenizationHelper().getCustomerId() + "_" + requestHelper.getTokenizationHelper().getInvoiceNumber());
                } else {
                    WebUtils.setSecurityAssociation("TOKEN_" + requestHelper.getMerchantActivity().getMerchantIdx().getMerchantCode() + "_" + requestHelper.getTokenizationHelper().getCustomerId() + "_" + requestHelper.getTokenizationHelper().getInvoiceNumber());
                }
                String userAgent = request.getHeader("User-Agent");
                try {
                    OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
                } catch (Throwable th) {
                    OneCheckoutLogger.log("ProcessPaymentCIP.processRequest : Error occur when debug Browser UserAgent");
                }
                OneCheckoutLogger.log("SESSION_ID[" + session.getId() + "]");
                if (requestHelper.getMerchantActivity().getActivityId() != null && (requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_PAYMENT_REQUEST.code() || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_UPDATE_CARD.code() || requestHelper.getMerchantActivity().getActivityId().getId() == EActivity.DO_TOKEN_DELETE_CARD.code())) {
                    requestHelper.getResultHelper().setSystemMessage("");
                    requestHelper.setCreditCardHelper(new CreditCardHelper());
                    requestHelper.setFilteredRequest(req);
                    requestHelper.setUnfilterRequest(request);
                    HashMap<String, String> data = WebUtils.stringToHashMap(requestHelper.getMerchantActivity().getData());
                    MerchantActivity merchantActivity = null;
                    if(req.getParameter("BUTTON").equals("DELETE")){
                        merchantActivity = manageLocal.startMerchantActivity(requestHelper, requestHelper.getMerchantActivity().getMerchantIdx(), EActivity.DO_TOKEN_DELETE_CARD, data);
                    } else {
                        merchantActivity = manageLocal.startMerchantActivity(requestHelper, requestHelper.getMerchantActivity().getMerchantIdx(), EActivity.DO_TOKEN_PAYMENT_DATA, data);
                    }
                    boolean activityStatus = manageLocal.manageMerchantActivity(requestHelper, merchantActivity);
                    manageLocal.endMerchantActtivity(requestHelper, activityStatus);
                    if (activityStatus) {
                        if (requestHelper.getCreditCardHelper() != null && requestHelper.getCreditCardHelper().getButton().equals("EDITCARD")) {
                            requestHelper.setTemplateFileName("tokenEdit.html");
                        } else if (requestHelper.getCreditCardHelper() != null && requestHelper.getCreditCardHelper().getButton().equals("DELETE")) {
                            requestHelper.setTemplateFileName("tokenization.html");
                        } else {
                            requestHelper.setTemplateFileName("tokenLoading.html");
                        }

                        // CREATE SESSION
                        session.setAttribute(SESSION_NAME, requestHelper);
                    } else {
                        System.out.println("ACTIVITY_STATUS_IS_FALSE");
                        session.invalidate();
                    }
                } else {
                    System.out.println("INVALID_MERCHANT_ACTIVITY_BEFORE");
                    session.invalidate();
                }
            } else {
                System.out.println("REQUEST_HELPER_IS_NULL");
                session.invalidate();
            }
        } catch (Throwable th) {
            th.printStackTrace();
            session.invalidate();
        }
        manageLocal.processTemplate(requestHelper, response.getWriter());
        System.out.println("---------------------------------------------------------------------------------------");
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("                         . : : DO TOKEN PAYMENT DATA : : .");
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("                         . : : DO TOKEN PAYMENT DATA : : .");
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
        return "DoTokenPaymentRequest";
    }

}
