/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import com.doku.lib.inet.DokuIntConnection;
import com.doku.lib.inet.InternetRequest;
import com.doku.lib.inet.InternetResponse;
import com.doku.lib.inet.RequestType;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.AESTools;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.template.CustomerInfoEmailObject;
import com.onecheckoutV1.template.PaymentPageObject;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jvnet.mimepull.MIMEMessage;

/**
 *
 * @author hafiz
 */
@Stateless
public class SendEmailVAToCustomerBean implements SendEmailVAToCustomerBeanLocal {

    //private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    PaymentPageObject paymentPageObject = new PaymentPageObject();
    OneCheckoutDataHelper oneCheckout = new OneCheckoutDataHelper();

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;

    public void sendEmailVAInfo(Transactions trans, PropertiesConfiguration config, Merchants merchants, OneCheckoutDataHelper dataHelper) {
        try {

            String mallId = trans.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "";
            String mallIdList = config.getString("ONECHECKOUT.LIST.MALLID.SEND.VA.NUMBER", "").trim();
            if (mallIdList.equalsIgnoreCase("ALL") || mallIdList.toUpperCase().indexOf(mallId) >= 0) {
                String payment_channel_id = trans.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId();
                String payment_channel_name = config.getString("CHANNEL.NAME." + payment_channel_id);
                String ext = ".html";
                String defaultTemp = config.getString("CHANNEL.DEFAULT.EMAIL.TEMPLATE." + payment_channel_id);
                String templatePath = config.getString("EMAIL.ALERT.TEMPLATE");
                String templateFileName = defaultTemp + "_" + trans.getIncMallid() + ext;

                File file = new File(templatePath + templateFileName);
                boolean fileExist = file.exists();

                if (!fileExist) {
                    OneCheckoutLogger.log(templateFileName + " does not exist");
                    templateFileName = defaultTemp + ext; //if file not exist use default template.
                } else {
                    OneCheckoutLogger.log(templateFileName + " exist");
                }

                Configuration cfg = new Configuration();
                cfg.setDirectoryForTemplateLoading(new File(templatePath));
                cfg.setObjectWrapper(new DefaultObjectWrapper());
                Template temp = cfg.getTemplate(templateFileName);

                Map root = new HashMap();
                CustomerInfoEmailObject infoEmail = new CustomerInfoEmailObject();
                root.put("CustomerInfoEmailObject", infoEmail);
                infoEmail.setCustomerName(trans.getIncName());
                infoEmail.setPaymentCode(trans.getAccountId());
                infoEmail.setCustomerAmount(OneCheckoutVerifyFormatData.moneyFormat.format(trans.getIncAmount()));
                infoEmail.setExpiredTime(ConvertMinutesToHours(trans.getMerchantPaymentChannel().getInvExpiredInMinutes()));
                infoEmail.setUrlAboutAmount(getLinkPurchaseRates(trans, config));
                infoEmail.setMerchantName(trans.getMerchantPaymentChannel().getMerchants().getMerchantName());

                System.out.println("Trans ID : " + trans.getTransactionsId());
                System.out.println("Expired : " + trans.getMerchantPaymentChannel().getInvExpiredInMinutes());
                System.out.println("Convert : " + ConvertMinutesToHours(trans.getMerchantPaymentChannel().getInvExpiredInMinutes()));
                System.out.println("Action URL : " + getLinkPurchaseRates(trans, config));

                StringWriter writer = new StringWriter();
                temp.process(root, writer);

                String lbl_merchantname = merchants.getMerchantName() != null ? merchants.getMerchantName() : "";
                String transIdmerchant = trans.getIncTransidmerchant() != null ? trans.getIncTransidmerchant() : "";
                String label_paycode = trans.getAccountId() != null ? trans.getAccountId() : "";
                String additionalInfo = dataHelper.getPaymentRequest().getADDITIONALINFO() != null ? dataHelper.getPaymentRequest().getADDITIONALINFO() : "";
//                PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(dataHelper.getPaymentChannel());
                String paymentchannelcode = dataHelper.getPaymentChannel().value();
                String paymentchannelName = OneCheckoutProperties.getOneCheckoutConfig().getString("onecheckoutv2.payment.channel.name.custom." + paymentchannelcode);
                
//                String thirdWord = trans.getMerchantPaymentChannel().getMerchants().getMerchantName();
//                String 

                String subjectEmail = null;
                Boolean bcc = false;
                if (merchants.getMerchantPaycodeEmailSubject() == null || merchants.getMerchantPaycodeEmailSubject().isEmpty()) {
                    subjectEmail = "Bayar Pesanan Anda [" + trans.getAccountId() + "] Via [" + payment_channel_name + "] - [" + trans.getMerchantPaymentChannel().getMerchants().getMerchantName() + "]";
                } else {
                    bcc = true;
                    subjectEmail = merchants.getMerchantPaycodeEmailSubject();
                    if (subjectEmail.contains("{{LABEL_MERCHANT_NAME}}")) {
                        subjectEmail = subjectEmail.replace("{{LABEL_MERCHANT_NAME}}", lbl_merchantname);
                    }
                    if (subjectEmail.contains("{{LABEL_TRANSIDMERCHANT}}")) {
                        subjectEmail = subjectEmail.replace("{{LABEL_TRANSIDMERCHANT}}", transIdmerchant);
                    }
                    if (subjectEmail.contains("{{LABEL_PAYCODE}}")) {
                        subjectEmail = subjectEmail.replace("{{LABEL_PAYCODE}}", label_paycode);
                    }
                    if (subjectEmail.contains("{{LABEL_ADDITIONALINFO}}")) {
                        subjectEmail = subjectEmail.replace("{{LABEL_ADDITIONALINFO}}", additionalInfo);
                    }
                    if (subjectEmail.contains("{{LABEL_PAYMENTCHANNEL}}")) {
                        subjectEmail = subjectEmail.replace("{{LABEL_PAYMENTCHANNEL}}", paymentchannelName);
                    }
                }

                String toEmail = trans.getIncEmail();
//                if ((!merchants.getMerchantPaycodeEmailCc().equals(null) && !merchants.getMerchantPaycodeEmailSubject().equals(null))||(merchants.getMerchantPaycodeEmailCc() != null && merchants.getMerchantPaycodeEmailSubject() != null) || (!merchants.getMerchantPaycodeEmailCc().equals("") && !merchants.getMerchantPaycodeEmailSubject().equals(""))) {
                if ((merchants.getMerchantPaycodeEmailCc() != null && merchants.getMerchantPaycodeEmailSubject() != null) || (merchants.getMerchantPaycodeEmailCc() != null && merchants.getMerchantPaycodeEmailSubject() == null)||
                        (merchants.getMerchantPaycodeEmailCc() == null && merchants.getMerchantPaycodeEmailSubject() != null)) {
                    boolean result = sendEmail(writer.toString(), toEmail, subjectEmail, config, merchants.getMerchantPaycodeEmailCc(), bcc);
                }

            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("ERROR : %s", ex.getStackTrace());
        }
    }

    protected String ConvertMinutesToHours(int minutes) {
        String result;
        int hour;
        int minute;

        if (minutes >= 1440) {
            hour = minutes % 1440;
            minutes = minutes / 1440;
            result = minutes + " Hari";

            if (hour >= 60) {
                minute = hour % 60;
                hour = hour / 60;
                result = minutes + " Hari " + hour + " Jam";

//                if(minute > 0){
//                    result = minutes + " Hari " + hour + " Jam "+ minute + " Menit";
//                }
            }
        } else if (minutes >= 60) {
            hour = minutes % 60;
            minutes = minutes / 60;
            result = minutes + " Jam"; // + hour + " Menit";
        } else {
            result = minutes + " Menit";
        }

        return result;
    }

    protected String getLinkPurchaseRates(Transactions trans, PropertiesConfiguration config) {
        String refNumber = "";
        String actionUrl = config.getString("PURCHASERATES.ACTION.URL");
        try {
            refNumber = AESTools.encrypt(trans.getTransactionsId() + "");
            String passingUrl = URLEncoder.encode("refNumber", "UTF-8") + "=" + URLEncoder.encode(refNumber, "UTF-8");
            actionUrl = actionUrl + "?" + passingUrl;
            //String passingUrl = "?purchaseAmount="+trans.getIncAmount()+"&purchaseCurrency="+trans.getIncPurchasecurrency() + "&currency="+trans.getIncCurrency()+"&merchantName="+trans.getMerchantPaymentChannel().getMerchants().getMerchantName();
        } catch (Throwable th) {
        }
        return actionUrl;
    }

    protected HashMap<String, String> getData(Transactions trans, PropertiesConfiguration config) {
        HashMap<String, String> params = new HashMap<String, String>();
        try {

            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String exeptionalMallId = null, grepMallId = null;
            Merchants merchant = trans.getMerchantPaymentChannel().getMerchants();

            try {

                exeptionalMallId = config.getString("ONECHECKOUT.SWIPER.ADDITIONALDATA").trim();
                grepMallId = merchant.getMerchantCode() + "";
                OneCheckoutLogger.log("OneCheckoutChannelBase.getData ONECHECKOUT.SWIPER.ADDITIONALDATA = %s", exeptionalMallId);

                if (merchant.getMerchantChainMerchantCode() != null && merchant.getMerchantChainMerchantCode() > 0) {
                    grepMallId = merchant.getMerchantCode() + "_" + merchant.getMerchantChainMerchantCode();
                }

                OneCheckoutLogger.log("OneCheckoutChannelBase.getData MERCHANTID_CHAINMERCHANTID = %s", grepMallId);

                if (exeptionalMallId != null && exeptionalMallId.contains(grepMallId)) {

                    OneCheckoutLogger.log("OneCheckoutChannelBase.getData - This merchant is SWIPER ADDITIONAL DATA");
                    params.put("ADDITIONALDATA", trans.getIncAdditionalInformation());
                    OneCheckoutLogger.log("OneCheckoutChannelBase.getData Add Parameter ADDITIONAL_DATA = %s", trans.getIncAdditionalInformation());

                } else {
                    OneCheckoutLogger.log("OneCheckoutChannelBase.getData - This merchant is NOT SWIPER ADDITIONAL DATA");
                }

            } catch (Throwable t) {
                exeptionalMallId = null;
                OneCheckoutLogger.log("OneCheckoutChannelBase.getData ERROR : " + t.getMessage());
                OneCheckoutLogger.log("OneCheckoutChannelBase.getData Can't Find Properti in Apps Config");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("ERROR : %s", ex.getMessage());
        }

        return params;
    }

    public ArrayList convertHashMap(HashMap<String, String> params) {
        ArrayList list = new ArrayList();
        if (params.isEmpty()) {
            return list;
        } else {
            Set set = params.keySet();
            int i = 0;

            for (Object obj : set) {
                String[] param = new String[2];
                String key = (String) obj;
                String val = params.get(key);
                param[0] = key;

                if (val == null) {
                    param[0] = "";
                } else {
                    param[0] = val;
                }

                list.add(i, param);
                i++;
            }
        }

        return list;
    }

    protected boolean sendEmail(String bodyemail, String to, String subject, PropertiesConfiguration config, String cc, Boolean bcc) {

        String from = config.getString("mail.account");
//        String cc = config.getString("mail.cc");
        String host = config.getString("mail.host");
        String password = config.getString("mail.password");
        String bccaddres = config.getString("onecheckoutv1.mail.bcc");

        System.out.println("to email \t\t: " + to);
        System.out.println("cc email \t\t: " + cc);
//        if (bcc) {
//            System.out.println("bcc email \t\t: " + bccaddres);
//        }
        System.out.println("from email \t\t: " + from);
        System.out.println("subject email \t: " + subject);
        System.out.println("host email \t\t: " + host);
        System.out.println("pasword email \t: " + password);

        InternetRequest iRequest = new InternetRequest();
        iRequest.setRequest((RequestType.EMAIL));
        iRequest.setEmailMessageHeaderType("text/html");
        iRequest.setEmailFrom(from);
        iRequest.setEmailTo(to);
        iRequest.setEmailCc(cc);
        iRequest.setEmailHost(host);
        iRequest.setEmailPassword(password);
        iRequest.setEmailSubject(subject);
        iRequest.setEmailMessageBody(bodyemail);
//        if (bcc) {
//            iRequest.setEmailBcc(bccaddres);
//        }

        InternetResponse inetres = DokuIntConnection.connect(iRequest);
        if (inetres.isStatusEmail()) {
            OneCheckoutLogger.log("sending email succeded");
            OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
            return true;
        } else {
            OneCheckoutLogger.log("sending email failed");
            OneCheckoutLogger.log("FAILED SENDING EMAIL...");
            return false;
        }
    }
}
