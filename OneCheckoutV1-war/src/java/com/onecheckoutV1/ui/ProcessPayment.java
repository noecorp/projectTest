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
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.servlet.Receive;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutGeneralConstant;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.view.ObjectPage;
import com.onecheckoutV1.view.PageViewer;
import com.onecheckoutV1.view.WebUtils;
import com.onechekoutv1.dto.Merchants;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author hafizsjafioedin
 */
public class ProcessPayment extends HttpServlet {

    String MALLIDParam = "MALLID";
    String CHAINMERCHANTIDParam = "CHAINMERCHANT";
    String BUTTONParam = "tombol";
    String errorPage = "/ErrorPage";
    String sessionCIPRequest = "cipRequest";

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

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, TemplateException {

        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.        
        PrintWriter out = response.getWriter();

        OneCheckoutDataHelper onecheckout = null;
        String logCode = "";

        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
            OneCheckoutLogger.log("PaymentPageShow.processRequest got OneCheckoutDataHelper from session");
            onecheckout = (OneCheckoutDataHelper) session.getAttribute(sessionCIPRequest);

            logCode = "ProcessPayment_" + onecheckout.getPaymentRequest().getMALLID() + "_" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
            WebUtils.setSecurityAssociation(logCode);

            String userAgent = request.getHeader("User-Agent");
            try {
                OneCheckoutLogger.log("BROWSER TYPE : " + userAgent);
            } catch (Throwable th) {
                OneCheckoutLogger.log("ProcessPayment.processRequest : Error occur when debug Browser UserAgent");
            }

            if (onecheckout.getMessage().equals("VALID")) {
                proses(onecheckout, request, out);
            } else {
                OneCheckoutLogger.log("ProcessPayment.processRequest parsePaymentRequestData NOT VALID");
                ObjectPage op = PageViewer.invalidPaymentRequestPage(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant());
                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, out);
            }
        } catch (Throwable th) {
            th.printStackTrace();
            int mId = 0;
            String mallId = request.getParameter(MALLIDParam);
            if (mallId != null && !mallId.isEmpty()) {
                try {
                    mId = OneCheckoutBaseRules.validateMALLID2(mallId);
                } catch (Throwable e) {
                    mId = 0;
                }
            }
            int cmId = -1;
            String chainMerchantId = request.getParameter(CHAINMERCHANTIDParam);
            if (chainMerchantId != null && !chainMerchantId.isEmpty()) {
                try {
                    cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);
                } catch (Throwable e) {
                    cmId = -1;
                }
            }
            ObjectPage op = PageViewer.errorPage(mId, cmId, request);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } finally {
            out.close();
            onecheckout = null;
            logCode = null;
        }
    }

    public void proses(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        OneCheckoutDataPGRedirect redirect = null;
        Merchants m = null;
        PaymentPageObject trxDetailpage = null;
        try {
            request.getSession().invalidate();
            InterfaceOneCheckoutV1BeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(InterfaceOneCheckoutV1BeanLocal.class);
            onecheckout = paymentProc.ProcessPayment(onecheckout);
            redirect = onecheckout.getRedirect();
            m = onecheckout.getMerchant();
            trxDetailpage = onecheckout.getPaymentPageObject();
            OneCheckoutLogger.log("ProcessPayment.proses Checking if merchant have feature installment");

            if (onecheckout.getMerchant() == null) {
                OneCheckoutLogger.log("ProcessPayment.proses merchant : null");
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.KlikBCA) {
                HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
                session.setAttribute(WebUtils.SESSION_KLIKBCA_THANKS, onecheckout);
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Danamon) {
                redirect.setPageTemplate("redirectDanamon.html");
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.BSP) {
                if (trxDetailpage != null) {
                    if (trxDetailpage.getMessage().equalsIgnoreCase("INSTALLMENT") && trxDetailpage.getListOneCheckoutInstallment() != null && !trxDetailpage.getListOneCheckoutInstallment().isEmpty()
                            && trxDetailpage.getListOneCheckoutInstallment().size() > 0) {
                        OneCheckoutLogger.log("Oneheckoutv1.ProcessPayment INSTALLMENT");
                        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
                        session.setAttribute(sessionCIPRequest, onecheckout);
                        String actionUrlInstallment = "/Suite/ProcessInstallmentPayment?MALLID=" + onecheckout.getMallId() + "&CHAINMERCHANT=" + onecheckout.getChainMerchantId() + "&INV=" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
                        trxDetailpage.setActionUrl(actionUrlInstallment);
                        redirect.setUrlAction(actionUrlInstallment);
                        redirect.setPageTemplate("installmentpage.html");

                    } else if (trxDetailpage.getMessage().equalsIgnoreCase("INSTALLMENTANDREWARDS") && trxDetailpage.getListOneCheckoutInstallment() != null && !trxDetailpage.getListOneCheckoutInstallment().isEmpty()
                            && trxDetailpage.getListOneCheckoutInstallment().size() > 0 && trxDetailpage.getOneCheckoutRewards() != null) {

                        OneCheckoutLogger.log("PROCESSS INSTALLMENTANDREWARDS");
                        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
                        session.setAttribute(sessionCIPRequest, onecheckout);
                        String actionUrlInstallment = "/Suite/ProcessInstallmentAndRewardsPayment?MALLID=" + onecheckout.getMallId() + "&CHAINMERCHANT=" + onecheckout.getChainMerchantId() + "&INV=" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
                        trxDetailpage.setActionUrl(actionUrlInstallment);
                        redirect.setUrlAction(actionUrlInstallment);
                        redirect.setPageTemplate("installmentandrewardspage.html");
                    } else if (trxDetailpage.getMessage().equalsIgnoreCase("REWARDS") && trxDetailpage.getOneCheckoutRewards() != null) {
                        OneCheckoutLogger.log("Oneheckoutv1.ProcessPayment REWARDS");
                        HttpSession session = (request.getSession() != null ? request.getSession() : request.getSession(true));
                        session.setAttribute(sessionCIPRequest, onecheckout);
                        String actionUrlInstallment = "/Suite/ProcessRewardsPayment?MALLID=" + onecheckout.getMallId() + "&CHAINMERCHANT=" + onecheckout.getChainMerchantId() + "&INV=" + onecheckout.getPaymentRequest().getTRANSIDMERCHANT();
                        trxDetailpage.setActionUrl(actionUrlInstallment);
                        redirect.setUrlAction(actionUrlInstallment);
                        redirect.setPageTemplate("rewardspage.html");
                    }
                }
            } else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Permata) {
                redirect.setPageTemplate("redirectPermata.html");
            }else if (onecheckout.getPaymentChannel() == OneCheckoutPaymentChannel.Kredivo) {
                redirect.setPageTemplate("redirectKredivo.html");
            }

            if (onecheckout.getMessage().equalsIgnoreCase("FAILTORETRIEVE")) {
                ObjectPage op = PageViewer.invalidPaymentRequestPage(onecheckout.getMallId(), onecheckout.getChainMerchantId(), request, onecheckout.getMessage(), onecheckout.getMerchant());
                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, out);
            } else if (onecheckout.getMessage().equalsIgnoreCase("ERROR")) {
                OneCheckoutLogger.log(":: redirect to Error ::");
                error(onecheckout, request, out);
            } else {
                onecheckout = null;
                ObjectPage op = PageViewer.redirectPage(redirect, m, request, trxDetailpage);
                Template temp = op.getTemp();
                Map root = op.getRoot();
                temp.process(root, out);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            OneCheckoutLogger.log("Error processPayment = " + t.getMessage());
            error(onecheckout, request, out);
        } finally {
            redirect = null;
            m = null;
            trxDetailpage = null;
        }
    }

    public void error(OneCheckoutDataHelper onecheckout, HttpServletRequest request, PrintWriter out) {
        try {
            OneCheckoutLogger.log("ProcessPayment.processRequest Not Verify OK");
            OneCheckoutV1TransactionBeanLocal paymentProc = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1TransactionBeanLocal.class);
            boolean addTrxOnDB = paymentProc.saveInvalidParamsTransactions(onecheckout);
            if (addTrxOnDB) {
                OneCheckoutLogger.log("ProcessPayment.processRequest Successful insert Trx into database");
            }

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setPageTemplate("redirect.html");
            redirect.setUrlAction(onecheckout.getMerchant().getMerchantRedirectUrl());

            OneCheckoutPaymentRequest preq = onecheckout.getPaymentRequest();
            if (preq != null) {
                Map data = redirect.getParameters();
                OneCheckoutChannelBase base = new OneCheckoutChannelBase();
                redirect.setAMOUNT(preq.getAMOUNT());
                if (onecheckout.getFlag() != null && onecheckout.getFlag().equalsIgnoreCase(OneCheckoutGeneralConstant.CANCEL_BY_USER)) {
                    redirect.setSTATUSCODE(OneCheckoutErrorMessage.CANCEL_BY_USER.value());
                } else {
                    redirect.setSTATUSCODE(OneCheckoutErrorMessage.ERROR_VALIDATE_INPUT.value());
                }

                redirect.setTRANSIDMERCHANT(preq.getTRANSIDMERCHANT());
                data.put("WORDS", base.generateRedirectWords(redirect, onecheckout.getMerchant()));
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(preq.getAMOUNT()));
                if (preq.getTRANSIDMERCHANT() != null && !preq.getTRANSIDMERCHANT().isEmpty()) {
                    data.put("TRANSIDMERCHANT", preq.getTRANSIDMERCHANT());
                }
                data.put("PAYMENTCHANNEL", onecheckout.getPaymentChannel().value());
                if (preq.getSESSIONID() != null && !preq.getSESSIONID().isEmpty()) {
                    data.put("SESSIONID", preq.getSESSIONID());
                }
                data.put("STATUSCODE", redirect.getSTATUSCODE());
                data.put("PAYMENTCODE", "");
            }

            ObjectPage op = PageViewer.redirectPage(redirect, onecheckout.getMerchant(), request, null);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);

        } catch (Throwable t) {
            OneCheckoutLogger.log("Error : " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.addHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.        
        PrintWriter out = response.getWriter();

        try {
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();
            HttpSession session = req.getSession(false);

            int mId = 0;
            if (session != null) {
                session.invalidate();
            }

            String mallId = request.getParameter(MALLIDParam);
            OneCheckoutLogger.log("ProcessPayment.doGet Receiving Payment Request from MALLID=" + mallId);
            OneCheckoutLogger.log("ProcessPayment.doGet denied, only receive HTTP POST method");
            if (mallId != null && !mallId.isEmpty()) {
                try {
                    mId = OneCheckoutBaseRules.validateMALLID2(mallId);
                } catch (Exception e) {
                    mId = 0;
                }

            }

            int cmId = -1;
            String chainMerchantId = request.getParameter(CHAINMERCHANTIDParam);
            if (chainMerchantId != null && !chainMerchantId.isEmpty()) {
                try {
                    cmId = OneCheckoutBaseRules.validateCHAINMERCHANT2(chainMerchantId);
                } catch (Throwable e) {
                    cmId = -1;
                }
            }

            ObjectPage op = PageViewer.errorPage(mId, cmId, request);
            Template temp = op.getTemp();
            Map root = op.getRoot();
            temp.process(root, out);
        } catch (TemplateException ex1) {
            RequestDispatcher rd = request.getRequestDispatcher(errorPage);
            rd.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (TemplateException ex) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
