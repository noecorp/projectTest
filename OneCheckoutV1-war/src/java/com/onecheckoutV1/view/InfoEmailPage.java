/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.view;

import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.template.CustomerInfoEmailObject;
import com.onechekoutv1.dto.Merchants;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author ahmadfirdaus
 */
public class InfoEmailPage {

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

    public static ObjectPage infoEmailPage(CustomerInfoEmailObject page, Merchants merchant, HttpServletRequest request) {
        ObjectPage op = new ObjectPage();

        try {

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
            int mId = merchant.getMerchantCode();
            int cmId = merchant.getMerchantChainMerchantCode();
            OneCheckoutLogger.log("InfoEmailPage.emailPage  Merchants.merchantCode : %d ", mId);
            OneCheckoutLogger.log("InfoEmailPage.emailPage  Merchants.merchantChainMerchantCode : %d ", cmId);

            String subPath = InfoEmailPage.getSubPath(request.getHeader("User-Agent").toUpperCase());
            String templatePath = config.getString("ONECHECKOUT.TEMPLATE", "/apps/onecheckoutv1/template/");
            String templateFileName = subPath + "alfa_info.html";

            String custom = "";
            if (cmId == 0) {
                custom = mId + "";
            } else {
                custom = mId + "_" + cmId;
            }

            File file = new File(templatePath + custom + "/" + templateFileName);
            boolean exists = file.exists();

            templatePath = templatePath + custom;
            if (exists) {
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " is exist");
            } else {
                OneCheckoutLogger.log(templatePath + "/" + templateFileName + " does not exist");
            }

            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map params = new HashMap();
            params.put("CustomerInfoEmailObject", page);

            op.setRoot(params);
            op.setTemp(temp);

        } catch (Throwable t) {
            t.printStackTrace();
            OneCheckoutLogger.log("InfoEmailPage.emailPage create ErrorPage ERROR : %s", t.getMessage());
        }

        return op;
    }
}
