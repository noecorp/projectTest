/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import com.doku.lib.inet.DokuIntConnection;
import com.doku.lib.inet.InternetRequest;
import com.doku.lib.inet.InternetResponse;
import com.doku.lib.inet.RequestType;
import com.onecheckoutV1.type.OneCheckoutEmailType;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import javax.mail.MessagingException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author aditya
 */
public class EmailUtility {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    public static boolean sendEmailTemplate(String templateDirectory, String templateFileName, Map emailParam, char emailType, String email, String subject) throws IOException, TemplateException, MessagingException {
        Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(new File(templateDirectory));
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        Template temp = cfg.getTemplate(templateFileName);
        StringWriter writer = new StringWriter();
        temp.process(emailParam, writer);
        if (emailType == OneCheckoutEmailType.HTML.code()) {
            return sendHtmmlFormat(email, subject, writer.toString());
        } else if (emailType == OneCheckoutEmailType.TEXT.code()) {
            return send(email, subject, writer.toString());
        } else if (emailType == OneCheckoutEmailType.HTML_ATTACHMENT.code()) {
            return send(email, subject, writer.toString());
        } else if (emailType == OneCheckoutEmailType.TEXT_ATTACHMENT.code()) {
            return send(email, subject, writer.toString());
        } else {
            return false;
        }
    }

    public static boolean send(String email, String subject, String isi) throws MessagingException {
        boolean status = false;
        try {
            String host = "", from = "", pass = "";
            host = config.getString("mail.host");
            from = config.getString("mail.account");
            pass = config.getString("mail.password");

            OneCheckoutLogger.log("*******************************************************************************");
            OneCheckoutLogger.log(" .:: Sending Email ::. ");
            OneCheckoutLogger.log("Host    : " + host);
            OneCheckoutLogger.log("From    : " + from);
            OneCheckoutLogger.log("To      : " + email);
            OneCheckoutLogger.log("Subject : " + subject);

            String messageText = isi;
            InternetRequest iRequest = new InternetRequest();
            iRequest.setEmailHost(host);
            iRequest.setEmailFrom(from);
            iRequest.setEmailPassword(pass);
            iRequest.setEmailSubject(subject);
            iRequest.setEmailMessageHeaderType("text/plain");
            iRequest.setEmailMessageBody(messageText);
            iRequest.setEmailTo(email);
            iRequest.setEmailCc("");
            iRequest.setRequest(RequestType.EMAIL);

            OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
            InternetResponse inetResp = DokuIntConnection.connect(iRequest);
            status = inetResp.isStatusEmail();
            if (status) {
                OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
            } else {
                OneCheckoutLogger.log("FAILED SENDING EMAIL...");
            }
            return status;

        } catch (Exception err) {
            status = false;
            OneCheckoutLogger.log("FAILED SENDING EMAIL...");
            OneCheckoutLogger.log("Error : " + err);
        }
        OneCheckoutLogger.log("*******************************************************************************");
        return status;
    }

    public static boolean sendHtmmlFormat(String email, String subject, String isi) throws MessagingException {
        try {
            boolean status = false;
            String host = "", from = "", pass = "";
            host = config.getString("mail.host");
            from = config.getString("mail.account");
            pass = config.getString("mail.password");

            OneCheckoutLogger.log("*******************************************************************************");
            OneCheckoutLogger.log(" .:: Sending Email ::. ");
            OneCheckoutLogger.log("Host    : " + host);
            OneCheckoutLogger.log("From    : " + from);
            OneCheckoutLogger.log("To      : " + email);
            OneCheckoutLogger.log("Subject : " + subject);

            String messageText = isi;
            InternetRequest iRequest = new InternetRequest();
            iRequest.setEmailHost(host);
            iRequest.setEmailFrom(from);
            iRequest.setEmailPassword(pass);
            iRequest.setEmailSubject(subject);
            iRequest.setEmailMessageHeaderType("text/html");
            iRequest.setEmailMessageBody(messageText);
            iRequest.setEmailTo(email);
            iRequest.setEmailCc("");
            iRequest.setRequest(RequestType.EMAIL);

            InternetResponse inetResp = DokuIntConnection.connect(iRequest);
            status = inetResp.isStatusEmail();
            if (status) {
                OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
            } else {
                OneCheckoutLogger.log("FAILED SENDING EMAIL...");
            }
            return status;
        } catch (Exception err) {
            OneCheckoutLogger.log("FAILED SENDING EMAIL...");
            OneCheckoutLogger.log("Error : " + err);
        }
        OneCheckoutLogger.log("*******************************************************************************");
        return false;
    }

    public static void sendNotifAlert(String msgError) {
        try {
            OneCheckoutLogger.log(":: SENDING NOTIFICATION ALERT ::");
            String messageText = "Dear Team, \n Error found in OCO System, please check to fixed. \n\n Error Message : [%s]\n\n\n Thanks,\n -Production System-.";
            messageText = String.format(messageText, msgError);
            String mailTo = OneCheckoutProperties.getOneCheckoutConfig().getString("NOTIFALERT.MAIL.TO");
            String subject = OneCheckoutProperties.getOneCheckoutConfig().getString("NOTIFALERT.MAIL.SUBJECT");
            Boolean result = send(mailTo, subject, messageText);
            if (result) {
                OneCheckoutLogger.log(":: SUCCESS ::");
            } else {
                OneCheckoutLogger.log("***** FAILED ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
