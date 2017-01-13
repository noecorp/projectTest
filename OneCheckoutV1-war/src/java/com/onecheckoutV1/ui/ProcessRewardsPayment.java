/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ui;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.helper.InterfaceOneCheckoutV1BeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutGeneralConstant;
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
 * @author margono
 */
public class ProcessRewardsPayment extends HttpServlet {

    String MALLIDParam = "MALLID";
    String CHAINMERCHANTIDParam = "CHAINMERCHANT";
    String BUTTONParam = "tombol";
    String errorPage = "/ErrorPage";
    String sessionCIPRequest = "cipRequest";
    
    String FLAGREWARDSParam = "FLAGREWARDS";
    
    @Override
    public void init() throws ServletException {
        if (ESAPI.securityConfiguration().getResourceDirectory() == null) {
            System.out.println("====ESAPI.properties ready to define====");
            ESAPI.securityConfiguration().setResourceDirectory("/apps/ESAPI/resources/");
        } else if (!ESAPI.securityConfiguration().getResourceDirectory().equals("/apps/ESAPI/resources/")) {
            System.out.println("====ESAPI.properties already define but different path ====");
            ESAPI.securityConfiguration().setResourceDirectory("/apps/ESAPI/resources/");
        }else {
            System.out.println("====ESAPI.properties already define ====");
        }
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, TemplateException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.addHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        PrintWriter pw = response.getWriter();
        
        ESAPI.httpUtilities().setCurrentHTTP(request, response);
        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
        OneCheckoutDataHelper onecheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);

        HashMap<String, String> params = WebUtils.copyParams(request);
        String rewardsParamFlag = request.getParameter(FLAGREWARDSParam);
        
        OneCheckoutLogger.log("param Flag rewards : %s", rewardsParamFlag);
        try {
            if (onecheckout != null) {
                OneCheckoutLogger.log("ProcessRewardsPayment.processRequest Receiving Input Payment Page from MALLID=" + onecheckout.getMallId());
                if (request.getSession() != null) {
                    session.invalidate();
                }
                session = request.getSession(true);
                session.setAttribute(sessionCIPRequest, onecheckout);
            }
            
            onecheckout = WebUtils.prosesRewardsData(onecheckout, params, rewardsParamFlag);
            String userAgent = request.getHeader("User-Agent");
            try {
                OneCheckoutLogger.log("BROWSER TYPE :" + userAgent);
            } catch (Throwable th) {
                OneCheckoutLogger.log("Error occur when debug Browser UserAgent");
            }

            String logCode = "OffUsRewards_" + onecheckout.getMallId() + "_" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
            String button = request.getParameter(BUTTONParam);
            WebUtils.setSecurityAssociation(logCode);
            if (button != null && button.equalsIgnoreCase(OneCheckoutGeneralConstant.CANCEL_BY_USER)) {
                OneCheckoutLogger.log("::ProcessInstallmentPayment Detail::" + OneCheckoutGeneralConstant.CANCEL_BY_USER);
                onecheckout.setFlag(button);
                error(onecheckout, request, pw);
                return;
            }

            if (onecheckout.getMessage().equals("VALID")) {
                proses(onecheckout, request, pw);
            } else {
                OneCheckoutLogger.log("::ProcessInstallmentPayment.proses NOT VALID::");
                ObjectPage op = PageViewer.invalidPaymentRequestPage(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant());
                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, pw);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }finally{
            
        }
        
    }
    
    public void proses(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        try {
            onecheckout.setMessage("REWARDS");
            InterfaceOneCheckoutV1BeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
            onecheckout = paymentProc.ProcessPayment(onecheckout);
            OneCheckoutDataPGRedirect oneCheckoutDataPGRedirect = onecheckout.getRedirect();
            OneCheckoutLogger.log("ProcessRewardsPayment.proses message : "+onecheckout.getMessage());
            Merchants m = onecheckout.getMerchant();

            PaymentPageObject trxDetailPage = onecheckout.getPaymentPageObject();
            ObjectPage op = PageViewer.redirectPage(oneCheckoutDataPGRedirect, m, request, trxDetailPage);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } catch (Throwable th) {
            th.printStackTrace();
            error(onecheckout, request, out);
        }
    }
    
    private void error(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter pw) {
        
        try {
            OneCheckoutLogger.log("ProcessRewardsPayment.processRequest Not Verify OK");
            OneCheckoutV1TransactionBeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1TransactionBeanLocal.class);
            boolean addTrxOnDB = paymentProc.saveInvalidParamsTransactions(onecheckout);
            
            if (addTrxOnDB) {
                OneCheckoutLogger.log("ProcessRewardsPayment Successful insert Trx into database");
            }

            OneCheckoutDataPGRedirect oneCheckoutDataPGRedirect = new OneCheckoutDataPGRedirect();
            oneCheckoutDataPGRedirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);    
            oneCheckoutDataPGRedirect.setPageTemplate("redirect.html");
            oneCheckoutDataPGRedirect.setUrlAction(onecheckout.getMerchant().getMerchantRedirectUrl());
            OneCheckoutPaymentRequest oneCheckoutPaymentRequest = onecheckout.getPaymentRequest();
            
            if (oneCheckoutPaymentRequest != null) {
                HashMap<String,String> data = oneCheckoutDataPGRedirect.getParameters();
                OneCheckoutChannelBase base = new OneCheckoutChannelBase();
                oneCheckoutDataPGRedirect.setAMOUNT(oneCheckoutPaymentRequest.getAMOUNT());
                
                if (onecheckout.getFlag()!= null && onecheckout.getFlag().equalsIgnoreCase(OneCheckoutGeneralConstant.CANCEL_BY_USER)) {
                    oneCheckoutDataPGRedirect.setSTATUSCODE(OneCheckoutErrorMessage.CANCEL_BY_USER.value());
                } else {
                    oneCheckoutDataPGRedirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_VALIDATE_INPUT.value());
                }
                oneCheckoutDataPGRedirect.setTRANSIDMERCHANT(oneCheckoutPaymentRequest.getTRANSIDMERCHANT());
                
                data.put("WORDS", base.generateRedirectWords(oneCheckoutDataPGRedirect, onecheckout.getMerchant()));
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(oneCheckoutPaymentRequest.getAMOUNT()));
                data.put("PAYMENTCHANNEL", onecheckout.getPaymentChannel().value());
                data.put("PAYMENTCODE", "");

                if (oneCheckoutPaymentRequest.getTRANSIDMERCHANT() != null && !oneCheckoutPaymentRequest.getTRANSIDMERCHANT().isEmpty()) {
                    data.put("TRANSIDMERCHANT", oneCheckoutPaymentRequest.getTRANSIDMERCHANT());
                }
                
                if (oneCheckoutPaymentRequest.getSESSIONID() != null && !oneCheckoutPaymentRequest.getSESSIONID().isEmpty()) {
                    data.put("SESSIONID", oneCheckoutPaymentRequest.getSESSIONID());
                }
            }
            ObjectPage op = PageViewer.redirectPage(oneCheckoutDataPGRedirect, onecheckout.getMerchant(), request, null);
            Template templ = op.getTemp();
            Map root = op.getRoot();
            templ.process(root, pw);
        } catch (Throwable th) {
            OneCheckoutLogger.log("Error : " + th.getMessage());
            th.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (TemplateException ex) {
            Logger.getLogger(ProcessRewardsPayment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        OneCheckoutLogger.log("process rewards");
        try {
            processRequest(request, response);
        } catch (TemplateException ex) {
            Logger.getLogger(ProcessRewardsPayment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}