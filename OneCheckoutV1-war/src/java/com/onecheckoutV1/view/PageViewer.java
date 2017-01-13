/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.view;

import antlr.StringUtils;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutDataTodayTransactions;
import com.onecheckoutV1.data.OneCheckoutRewards;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.ResponseCode;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author hafizsjafioedin
 */
public class PageViewer {

    private static DecimalFormat sdf = new DecimalFormat("###,###,##0.00");
    private static DecimalFormat sdf1 = new DecimalFormat("########");

    private static String getSubPath(String userAgent) {

//        if (userAgent == null) {
//            return "";
//        } else if (userAgent.indexOf("BLACKBERRY") >= 0) {
//            return "mob_";
//        } else if (userAgent.indexOf("IPHONE") >= 0) {
//            return "mob_";
//        } else if (userAgent.indexOf("ANDROID") >= 0) {
//            return "mob_";
//        } else {
//            return "";
//        }
        String mobVersion = OneCheckoutProperties.getOneCheckoutConfig().getString("ONECHECKOUTV1.MOBILE.VERSION");

        mobVersion = mobVersion.toUpperCase();
//        OneCheckoutLogger.log("mobversion ManageBean =* " + mobVersion);
//        OneCheckoutLogger.log("userAgent ManageBean =* " + userAgent);

        if (userAgent == null) {
            return "";
        }
        boolean mobOS = false;
        String[] listMob = mobVersion.split(";");
        for (String listMob1 : listMob) {
            if (userAgent.indexOf(listMob1) >= 0) {
                mobOS = true;
                break;
            }
        }
        if (mobOS) {
            return "mob_";
        } else {
            return "";
        }

    }

    public static ObjectPage paymentPage(PaymentPageObject page, Merchants merchant, HttpServletRequest request) {
        ObjectPage op = new ObjectPage();

        try {

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            int mId = merchant.getMerchantCode();
            int cmId = merchant.getMerchantChainMerchantCode();
            OneCheckoutLogger.log("PageViewer.paymentPage  Merchants.merchantCode : %d ", mId);
            OneCheckoutLogger.log("PageViewer.paymentPage  Merchants.merchantChainMerchantCode : %d ", cmId);
            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());
            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");

            String templateFileName = subPath + "paymentpage.html";

            String custom = "";
            if (cmId == 0) {
                custom = mId + "";
            } else {
                custom = mId + "_" + cmId;
            }

            File file = new File(templatePath + custom + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + custom;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " is exist");
            } else {
                OneCheckoutLogger.log(templatePath + custom + "/" + templateFileName + " does not exist");
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map params = new HashMap();
            params.put("PaymentPageObject", page);
            //ArrayList paramList = WebUtils.convertHashMap(redirect.getParameters());

            op.setRoot(params);
            op.setTemp(temp);

        } catch (Throwable t) {
            t.printStackTrace();
            OneCheckoutLogger.log("PageViewer.paymentPage create ErrorPage ERROR : %s", t.getMessage());
        }

        return op;
    }

    public static ObjectPage errorPage(int mallId, int chainMerchantId, HttpServletRequest request) {

        ObjectPage op = new ObjectPage();

        try {
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            //       String action = config1.getString("SERVLET.PAYMENT", "/PaymentSubmit");
            //       String prjPath = config1.getString("PROJECT.PATH", "/GIPay");
            //       action = prjPath + action;
            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());

            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");

            String templateFileName = subPath + "error.html";
            File file = new File(templatePath + mallId + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + mallId;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " exist");
            } else {
                OneCheckoutLogger.log(templatePath + mallId + "/" + templateFileName + " does not exist");
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();

            op.setRoot(root);
            op.setTemp(temp);
            return op;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("PageViewer.errorPage create ErrorPage ERROR : %s", ex.getMessage());
            return op;
        }

    }

    public static ObjectPage transactionAlreadySuccessVoid(int mallId, int chainMerchantId, HttpServletRequest request, String message, Merchants merchant, String transactionsStatus, String invoice, String problems) {

        ObjectPage op = new ObjectPage();
        OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
        try {
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            //       String action = config1.getString("SERVLET.PAYMENT", "/PaymentSubmit");
            //       String prjPath = config1.getString("PROJECT.PATH", "/GIPay");
            //       action = prjPath + action;
            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());

            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");

            String templateFileName = subPath + "invalidPaymentRequest.html";
            File file = new File(templatePath + mallId + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + mallId;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " exist");
            } else {
                OneCheckoutLogger.log(templatePath + mallId + "/" + templateFileName + " does not exist");
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();

//            if (problems.equals("PAYMENTREQUESTINVALID")) {
//                try {
//                    PropertiesConfiguration errorDesc = OneCheckoutProperties.getOneCheckoutErrDesc();
//                    String[] errors = message.split("\\|");
//                    int len = errors.length;
//                    String code = errors[0];
//                    message = errorDesc.getString(code, " ");
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    OneCheckoutLogger.log("keep continue redirect to merchant site!");
//                }
//                root.put("ERRORMSG", message);
//            } else if (problems.equals("ALREADY")) {
//                root.put("TRANSACTIONSTATUS", transactionsStatus);
//                root.put("INVOICE", invoice);
//            }
            String description = null;
            Integer responseCode = null;
            ResponseCode code1 = null;
            OneCheckoutLogger.log(":: MESSAGE ERROR [" + message + "]");
            if (message.contains("\\|")) {
                String[] errors = message.split("\\|");
                int len = errors.length;
                String code = errors[0];
                code1 = queryHelper.getResponseCode(code);
                if (code1 != null) {
                    description = code1.getDescription();
                } else {
                    description = errors[1];
                }
            } else {
                try {
                    responseCode = Integer.parseInt(message);
                    code1 = queryHelper.getResponseCode(responseCode + "");
                    if (code1 != null) {
                        description = code1.getDescription();
                    } else {
                        description = "ERROR";
                    }

                } catch (Throwable th) {
                    description = message;
                }

            }

            root.put("ERRORMSG", description);
            String redirectToMerchant = "#";
            if (merchant != null && merchant.getMerchantRedirectUrl() != null && !merchant.getMerchantRedirectUrl().isEmpty()) {
                redirectToMerchant = merchant.getMerchantRedirectUrl();
            }
            root.put("URL_MERCHANT", redirectToMerchant);
            op.setRoot(root);
            op.setTemp(temp);
            return op;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("PageViewer.invalidPaymentRequestPage  create ErrorPage ERROR : %s", ex.getMessage());
            return op;
        }

    }

    public static ObjectPage invalidPaymentRequestPage(int mallId, int chainMerchantId, HttpServletRequest request, String errorMessage, Merchants merchant) {

        ObjectPage op = new ObjectPage();

        try {
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            //       String action = config1.getString("SERVLET.PAYMENT", "/PaymentSubmit");
            //       String prjPath = config1.getString("PROJECT.PATH", "/GIPay");
            //       action = prjPath + action;
            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());

            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");

            String templateFileName = subPath + "invalidPaymentRequest.html";
            File file = new File(templatePath + mallId + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + mallId;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " exist");
            } else {
                OneCheckoutLogger.log(templatePath + mallId + "/" + templateFileName + " does not exist");
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            try {
                PropertiesConfiguration errorDesc = OneCheckoutProperties.getOneCheckoutErrDesc();
                String[] errors = errorMessage.split("\\|");
                int len = errors.length;
                String code = errors[0];
                errorMessage = errorDesc.getString(code, " ");
            } catch (Exception ex) {
                ex.printStackTrace();
                OneCheckoutLogger.log("keep continue redirect to merchant site!");
            }

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();
            root.put("ERRORMSG", errorMessage);

            String redirectToMerchant = "#";
            if (merchant != null && merchant.getMerchantRedirectUrl() != null && !merchant.getMerchantRedirectUrl().isEmpty()) {
                redirectToMerchant = merchant.getMerchantRedirectUrl();
            }
            root.put("URL_MERCHANT", redirectToMerchant);
            op.setRoot(root);
            op.setTemp(temp);
            return op;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("PageViewer.invalidPaymentRequestPage  create ErrorPage ERROR : %s", ex.getMessage());
            return op;
        }

    }

    public static ObjectPage redirectPage(OneCheckoutDataPGRedirect redirect, Merchants m, HttpServletRequest request, PaymentPageObject trxDetail) {

        ObjectPage op = new ObjectPage();

        try {
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            //       String action = config1.getString("SERVLET.PAYMENT", "/PaymentSubmit");
            //       String prjPath = config1.getString("PROJECT.PATH", "/GIPay");
            //       action = prjPath + action;
            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());

            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");
            int mId = m.getMerchantCode();
            int cmId = m.getMerchantChainMerchantCode();
            OneCheckoutLogger.log("PageViewer.redirectPage  Merchants.merchantCode : %d ", mId);
            OneCheckoutLogger.log("PageViewer.redirectPage  Merchants.merchantChainMerchantCode : %d ", cmId);
            String templateFileName = subPath + redirect.getPageTemplate();//."error.html";

            String custom = "";
            if (cmId == 0) {
                custom = mId + "";
            } else {
                custom = mId + "_" + cmId;
            }

            File file = new File(templatePath + custom + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + custom;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " exist");
            } else {
                OneCheckoutLogger.log(templatePath + custom + "/" + templateFileName + " does not exist");
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();
            ArrayList paramList = new ArrayList();
            if (redirect.getParameters() != null) {
                 paramList = WebUtils.convertHashMap(redirect.getParameters());
            }

            OneCheckoutLogger.log("--- retry data --- ");
            ArrayList retryData = WebUtils.convertHashMap(redirect.getRetryData());
            OneCheckoutLogger.log("--- end of retry data --- ");

            System.out.println(": : : : URL ACTION : " + redirect.getUrlAction());
            root.put("ACTION_SERVLET", redirect.getUrlAction());
            root.put("PROTOCOL_HTTP", redirect.getHttpProtocol());
            root.put("ENABLEBUTTON", redirect.getEnableButton());
            if (paramList.size() > 0) {
//                OneCheckoutLogger.log("size =* " + paramList.size());
                String[] a = (String[]) paramList.get(0);
//                OneCheckoutLogger.log("DATA TO SIMULATOR =* " + a[1]);
//                OneCheckoutLogger.log("test value=* " + paramList.get(1));
            }

            root.put("itemlist", paramList);
            root.put("retryData", retryData);
            root.put("retry", redirect.getRetry());

            if (trxDetail != null) {
                root.put("PaymentPageObject", trxDetail);
                if (trxDetail.getMIPInquiryResponse() != null) {
                    root.put("dokuwalletData", trxDetail.getMIPInquiryResponse());
                }
//                if (trxDetail.getmIPDokuInquiryResponse() != null) {
//                    root.put("dokuwalletData", trxDetail.getmIPDokuInquiryResponse());
//                }
            }

            op.setRoot(root);
            op.setTemp(temp);
            return op;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("PageViewer.redirectPage create ErrorPage ERROR : %s", ex.getMessage());
            return op;
        }

    }

    public static ObjectPage retryPage(OneCheckoutDataPGRedirect redirect, Merchants m, HttpServletRequest request) {

        ObjectPage op = new ObjectPage();

        try {
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());

            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");
            int mId = m.getMerchantCode();
            int cmId = m.getMerchantChainMerchantCode();
            OneCheckoutLogger.log("PageViewer.retryPage Merchants.merchantCode : %d ", mId);
            OneCheckoutLogger.log("PageViewer.retryPage  Merchants.merchantChainMerchantCode : %d ", cmId);
            String templateFileName = subPath + redirect.getPageTemplate();//."error.html";

            String custom = "";
            if (cmId == 0) {
                custom = mId + "";
            } else {
                custom = mId + "_" + cmId;
            }

            File file = new File(templatePath + custom + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + custom;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " exist");
            } else {
                OneCheckoutLogger.log(templatePath + custom + "/" + templateFileName + " does not exist");
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();
            ArrayList itemlist = WebUtils.convertHashMap(redirect.getParameters());
            ArrayList retryData = WebUtils.convertHashMap(redirect.getRetryData());

            System.out.println(": : : : URL ACTION : " + redirect.getUrlAction());

            System.out.println("RETRY LIST :" + retryData.size());

            root.put("ACTION_SERVLET", redirect.getUrlAction());
            root.put("PROTOCOL_HTTP", redirect.getHttpProtocol());
            root.put("ENABLEBUTTON", redirect.getEnableButton());
            root.put("retryData", retryData);
            root.put("itemlist", itemlist);
            root.put("retry", redirect.getRetry());

            op.setRoot(root);
            op.setTemp(temp);
            return op;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("PageViewer.retryPage create ErrorPage ERROR : %s", ex.getMessage());
            return op;
        }

    }

    public static ObjectPage DisplayTrxDataPage(OneCheckoutDataTodayTransactions redirect, Merchants m, HttpServletRequest request) {

        ObjectPage op = new ObjectPage();

        try {
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());

            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckout/template/");
            int mId = m.getMerchantCode();
            int cmId = m.getMerchantChainMerchantCode();
            OneCheckoutLogger.log("PageViewer.DisplayTrxDataPage Merchants.merchantCode : %d ", mId);
            OneCheckoutLogger.log("PageViewer.DisplayTrxDataPage  Merchants.merchantChainMerchantCode : %d ", cmId);

            String templateFileName = subPath + redirect.getPageTemplate();//."error.html";

            String custom = "";
            if (cmId == 0) {
                custom = mId + "";
            } else {
                custom = mId + "_" + cmId;
            }

            File file = new File(templatePath + custom + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + custom;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " exist");
            } else {
                OneCheckoutLogger.log(templatePath + custom + "/" + templateFileName + " does not exist");
            }

            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();

            root.put("ACTION_SERVLET", redirect.getUrlAction());
            root.put("PROTOCOL_HTTP", redirect.getHttpProtocol());
            root.put("ENABLEBUTTON", redirect.getEnableButton());
            root.put("itemlist", redirect.getRowData());
            op.setRoot(root);
            op.setTemp(temp);
            return op;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("PageViewer.DisplayTrxDataPage create ErrorPage ERROR : %s", ex.getMessage());
            return op;
        }

    }

    public static ObjectPage sessionTimeoutPage(int mallId, int chainMerchantId, HttpServletRequest request) {

        ObjectPage op = new ObjectPage();

        try {
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            //       String action = config1.getString("SERVLET.PAYMENT", "/PaymentSubmit");
            //       String prjPath = config1.getString("PROJECT.PATH", "/GIPay");
            //       action = prjPath + action;
            String subPath = PageViewer.getSubPath(request.getHeader("User-Agent").toUpperCase());

            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");

            String templateFileName = subPath + "timeoutpage.html";
            File file = new File(templatePath + mallId + "/" + templateFileName);
            boolean exists = file.exists();

            if (exists) {
                templatePath = templatePath + mallId;
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " exist");
            } else {
                OneCheckoutLogger.log(templatePath + mallId + "/" + templateFileName + " does not exist");
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();

            op.setRoot(root);
            op.setTemp(temp);
            return op;
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("PageViewer.errorPage create ErrorPage ERROR : %s", ex.getMessage());
            return op;
        }

    }
}
