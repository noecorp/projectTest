/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import com.doku.lib.inet.DOKUInternetProtocol;
import com.doku.lib.inet.DokuIntConnection;
import com.doku.lib.inet.InternetRequest;
import com.doku.lib.inet.InternetResponse;
import com.doku.lib.inet.RequestType;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutPrincipal;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Transactions;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.annotation.Resource;
import javax.ejb.*;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jboss.security.SecurityAssociation;

/**
 *
 * @author hafiz
 */
@Stateless
public class SchedulerRedirectToMerchantBean extends OneCheckoutChannelBase implements SchedulerRedirectToMerchantLocal {

    PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    private long t1 = System.currentTimeMillis();
    @Resource
    TimerService timerService;
    @Resource
    SessionContext context;
    private final static String ONECHECKOUTV1_SCHEDULER = "_ONECHECKOUTV1_REDIRECT_MERCHANT_SCHEDULER";

    private Session getSession() {
        return (Session) em.getDelegate();
    }

    public void startSchedulerTimer() {
        for (Object obj : timerService.getTimers()) {
            Timer timer = (Timer) obj;
            if (ONECHECKOUTV1_SCHEDULER.equals(timer.getInfo())) {

                OneCheckoutLogger.log("Timer service is working. Cancelling it");
                timer.cancel();

            }
        }                                           //1.5 * 60 * 60 * 1000
        Timer timer = timerService.createTimer(0, (300 * 1000), ONECHECKOUTV1_SCHEDULER);
    //     Timer timer = timerService.createTimer(0, (2 * 60 * 60 * 1000), ONECHECKOUT_SCHEDULER);

    }

    @Timeout
    public void startSchedulerExecutor(Timer timer) {

        // TRUE, EMAIL, REDIRECT, FALSE
        String on = config.getString("REDIRECT.SCHEDULLER", "FALSE");

        if (on.equalsIgnoreCase("FALSE")) {

            OneCheckoutLogger.log("OneCheckoutV1 - scheduller of Redirect To Merchant disabled");
            return;

        }

        String merchantCodeList = config.getString("REDIRECT.MERCHANTCODELIST", "");

        int maxrows = config.getInt("REDIRECT.STATUS.ROWS", 10);
        String paymentChannelList = config.getString("REDIRECT.STATUS.PAYMENTCHANNELCODE", "NONE");

        Calendar max = Calendar.getInstance();
        max.add(Calendar.MINUTE, -5);
        Date maxtime = max.getTime();

        Calendar min = Calendar.getInstance();
        min.add(Calendar.HOUR, -24);
        Date mintime = min.getTime();

        Criteria criteria = getSession().createCriteria(Transactions.class);
        criteria.createAlias("merchantPaymentChannel", "mpc");
        criteria.createAlias("mpc.merchants", "m");
        criteria.createAlias("mpc.paymentChannel", "pc");
        criteria.createCriteria("transactionsDataAirlineses", "airlines", Criteria.LEFT_JOIN);

        criteria.add(Restrictions.le("dokuInvokeStatusDatetime", maxtime));
        criteria.add(Restrictions.ge("dokuInvokeStatusDatetime", mintime));
        criteria.add(Restrictions.isNull("redirectDatetime"));
        if (!merchantCodeList.isEmpty() && !merchantCodeList.equalsIgnoreCase("ALL")) {

            try {
                String[] merchantList = merchantCodeList.split(";");
                int len = merchantList.length;
                Integer[] mList = new Integer[len];

                for (int i = 0; i < len; i++) {
                    mList[i] = Integer.parseInt(merchantList[i]);
                }

                criteria.add(Restrictions.in("m.merchantCode", mList));
            } catch (NumberFormatException exception) {
                exception.printStackTrace();
            }
        }

        if (!paymentChannelList.equalsIgnoreCase("NONE")) {
            criteria.add(Restrictions.ilike("pc.paymentChannelId", paymentChannelList));
        }

        criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
        criteria.add(Restrictions.eq("transactionsStatus", OneCheckoutTransactionStatus.SUCCESS.value()));
        
//      criteria.add(Restrictions.isNotNull("dokuResponseCode"));

        //criteria.add(Restrictions.ilike("systemMessage", "NOTIFYMERCHANT", MatchMode.START));
        criteria.setMaxResults(maxrows);

        List<Transactions> trxList = (List<Transactions>) criteria.list();


        if (trxList == null || trxList.isEmpty()) {
            OneCheckoutLogger.log("SchedulerRedirectToMerchantBean.startSchedulerExecutor : 0 transactions");
            return;
        }

        for (Transactions trans : trxList) {

            String logPrefix = "RedirectToMerchantScheduler_" + trans.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "_" + trans.getIncTransidmerchant();

            SecurityAssociation.setPrincipal(new OneCheckoutPrincipal(logPrefix));

            if (on.equalsIgnoreCase("TRUE")) {
                doEmail(trans, config);
                doRedirectViaOpenConnection(trans);
            } else if (on.equalsIgnoreCase("REDIRECT")) {
                doRedirectViaOpenConnection(trans);
            } else if (on.equalsIgnoreCase("EMAIL")) {
                doEmail(trans, config);
            }
        }
        SecurityAssociation.clear();
    }

    private void doRedirectViaOpenConnection(Transactions trans) {

        try {

            MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
            Merchants m = mpc.getMerchants();

            //set data redirect yang akan di kirim ke merchant
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(m.getMerchantRedirectUrl());
            redirect.setAMOUNT(trans.getIncAmount().doubleValue());
            redirect.setTRANSIDMERCHANT(trans.getIncTransidmerchant());
            redirect.setSTATUSCODE(OneCheckoutErrorMessage.SUCCESS.value());
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());
//            Map data = redirect.getParameters();
//
//            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
//            data.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
//            data.put("WORDS", base.generateRedirectWords(redirect, m));
//            data.put("STATUSCODE", redirect.getSTATUSCODE());
//            data.put("PAYMENTCHANNEL", mpc.getPaymentChannel().getPaymentChannelId());
//            data.put("SESSIONID", trans.getIncSessionid());
//
            if (trans.getRates()!=null) {
                redirect.setAMOUNT(trans.getIncPurchaseamount().doubleValue());
            }
            else {
                redirect.setAMOUNT(trans.getIncAmount().doubleValue());//.getAMOUNT());
            }  
            
            String data_encode = "";
            data_encode += URLEncoder.encode("AMOUNT", "UTF-8") + "=" + URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()), "UTF-8") + "&";
            data_encode += URLEncoder.encode("TRANSIDMERCHANT", "UTF-8") + "=" + URLEncoder.encode(trans.getIncTransidmerchant(), "UTF-8") + "&";
            data_encode += URLEncoder.encode("WORDS", "UTF-8") + "=" + URLEncoder.encode(super.generateRedirectWords(redirect, m), "UTF-8") + "&";
            data_encode += URLEncoder.encode("STATUSCODE", "UTF-8") + "=" + URLEncoder.encode(redirect.getSTATUSCODE(), "UTF-8") + "&";
            data_encode += URLEncoder.encode("PAYMENTCHANNEL", "UTF-8") + "=" + URLEncoder.encode(mpc.getPaymentChannel().getPaymentChannelId(), "UTF-8") + "&";
            data_encode += URLEncoder.encode("SESSIONID", "UTF-8") + "=" + URLEncoder.encode(trans.getIncSessionid(), "UTF-8");

            if(trans.getIncPurchasecurrency() != null && !trans.getIncPurchasecurrency().equalsIgnoreCase("360")) {
                data_encode += URLEncoder.encode("PURCHASECURRENCY", "UTF-8") + "=" + URLEncoder.encode(trans.getIncPurchasecurrency(), "UTF-8");
            }

            HashMap<String, String> params = new HashMap<String, String>();

            OneCheckoutLogger.log("Fetch URL     : %s", m.getMerchantRedirectUrl());
            OneCheckoutLogger.log("Merchant Data : %s", data_encode);
            //doFetchHTTP(String data, String url, HashMap<String, String> params, Merchants merchant) {
            OneCheckoutLogger.log("base.doFetchHTTP : start");
            InternetResponse inetResp = fopen(data_encode, m.getMerchantRedirectUrl(), params, m);
            OneCheckoutLogger.log("base.doFetchHTTP : end");

            if (inetResp.getHTTPrespCode() == HttpURLConnection.HTTP_OK) {
                OneCheckoutLogger.log("Reading response from merchant");
                trans.setRedirectDatetime(new Date());
                //trans.setSystemMessage(data_encode)
                em.merge(trans);
            } else {
                OneCheckoutLogger.log("Got HTTP RESPONSE CODE = %d", inetResp.getHTTPrespCode());
            }

            OneCheckoutLogger.log("SchedulerRedirectToMerchantBean.doRedirectViaOpenConnection : update trx : %s", trans.getSystemMessage());

        } catch (Exception ex) {
            OneCheckoutLogger.log("ERROR " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private InternetResponse fopen(String data, String url, HashMap<String, String> params, Merchants merchant) {

        InternetResponse inetResp = null;
        try {
            String resp = "";

            InternetRequest iRequest = new InternetRequest();
            iRequest.setURLAddress(url);
            /*iRequest.setConnectionTimeout(merchant.getMerchantConnectionTimeout()*1000);
            iRequest.setReadTimeout(merchant.getMerchantReadTimeout()*1000);*/
            iRequest.setConnectionTimeout(config.getInt("SCHEDULER.CONNECTION.TIMEOUT", 30) * 1000);
            iRequest.setReadTimeout(config.getInt("SCHEDULER.READ.TIMEOUT", 30) * 1000);
            //iRequest.setMessageData("");
            iRequest.setRequest(RequestType.HTTP_HTTPS);

            if (url.toLowerCase().startsWith("https")) {
                iRequest.setProtocol(DOKUInternetProtocol.HTTPS_POST);
            } else {
                iRequest.setProtocol(DOKUInternetProtocol.HTTP_POST);
            }
            //iRequest.setProtocol(DOKUInternetProtocol.HTTP_POST);
            iRequest.setParameterProperties(params);
            //  String dataencode = URLEncoder.encode(data, "UTF-8");
            iRequest.setMessageData(data);
            inetResp = DokuIntConnection.connect(iRequest);
            OneCheckoutLogger.log("Response Code Http Connection => %d", inetResp.getHTTPrespCode());

            if (inetResp.getHTTPrespCode() == HttpURLConnection.HTTP_OK) {
                OneCheckoutLogger.log("Reading response from merchant");
            } else {
                OneCheckoutLogger.log("Got HTTP RESPONSE CODE = %d", inetResp.getHTTPrespCode());
            }
            String ack = inetResp.getMsgResponse();
            OneCheckoutLogger.log("ACKNOWLEDGE : " + ack);
            return inetResp;
        } catch (Exception ex) {
            ex.printStackTrace();
            return inetResp;
        }
    }

    public void doEmail(Transactions trans, PropertiesConfiguration config) {

        try {
            String templatePath = config.getString("EMAIL.ALERT.TEMPLATE", "/apps/onecheckoutv1/email/");

            String templateFileName = "email_redirect_status.html";
            File file = new File(templatePath + templateFileName);
            boolean exists = file.exists();

            if (!exists) {
                OneCheckoutLogger.log(templateFileName + " does not exist");
                return;
            }

            //Template temp = op.getTemp();
            Configuration cfg = new Configuration();
            cfg.setDirectoryForTemplateLoading(new File(templatePath));
            cfg.setObjectWrapper(new DefaultObjectWrapper());

            Template temp = cfg.getTemplate(templateFileName);
            Map root = new HashMap();
            HashMap<String, String> params = super.getData(trans);
            ArrayList paramList = super.convertHashMap(params);

            root.put("itemlist", paramList);
            root.put("merchantName", trans.getMerchantPaymentChannel().getMerchants().getMerchantName());
            root.put("MALLID", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode());
            root.put("invoiceNo", trans.getIncTransidmerchant());

            StringWriter writer = new StringWriter();
            temp.process(root, writer);

            String subject = "[" + trans.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "-" + trans.getIncTransidmerchant() + "] Notify Redirect Scheduller " + OneCheckoutVerifyFormatData.email_datetimeFormat.format(new Date());
            String to = trans.getMerchantPaymentChannel().getMerchants().getMerchantEmail();
            boolean result = sendEmail(writer.toString(), to, subject, config);//sendEmail(,email,config);

            if (result) {
                OneCheckoutLogger.log("Reading response from merchant");
                trans.setRedirectDatetime(new Date());
                trans.setSystemMessage("EMAILSUCCESS");
                em.merge(trans);
            }
        //   return sendEmail(writer.toString(),email,config);
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("ERROR : %s", ex.getMessage());
        //return false;
        }
    }

    public boolean sendEmail(String bodyemail, String to, String subject, PropertiesConfiguration config) {
        //  String to = config.getString("EMAIL.TO." + mallId,"hafiz@nsiapay.com");
        String from = config.getString("mail.account", "system-noreply@doku.com");
        String cc = config.getString("mail.cc", "rudy.thong@doku.com");
        String host = config.getString("mail.host", "smtp.gmail.com");
        String password = config.getString("mail.password", "n51aJKT!@#");

        InternetRequest iRequest = new InternetRequest();
        iRequest.setRequest((RequestType.EMAIL));
        iRequest.setEmailTo(to);
        iRequest.setEmailFrom(from);
        iRequest.setEmailCc(cc);
        iRequest.setEmailHost(host);
        iRequest.setEmailMessageBody(bodyemail);
        iRequest.setEmailMessageHeaderType("text/html");
        iRequest.setEmailPassword(password);
        iRequest.setEmailSubject(subject);

        InternetResponse inetResp = DokuIntConnection.connect(iRequest);

        if (inetResp.isStatusEmail()) {
            OneCheckoutLogger.log("sending email succeded");
        } else {

            OneCheckoutLogger.log("sending email failed");
        }

        if (inetResp.isStatusEmail()) {
            OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
            return true;
        } else {
            OneCheckoutLogger.log("FAILED SENDING EMAIL...");
            return false;
        }
    }
}
