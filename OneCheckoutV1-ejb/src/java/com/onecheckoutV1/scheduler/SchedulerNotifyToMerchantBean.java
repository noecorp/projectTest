/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.scheduler;

import com.doku.lib.inet.DokuIntConnection;
import com.doku.lib.inet.InternetRequest;
import com.doku.lib.inet.InternetResponse;
import com.doku.lib.inet.RequestType;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutPrincipal;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Transactions;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
public class SchedulerNotifyToMerchantBean extends OneCheckoutChannelBase implements SchedulerNotifyToMerchantLocal {

//    @PersistenceContext(unitName = "ONECHECKOUTV1")
//    protected EntityManager em;
//    private long t1 = System.currentTimeMillis();
//    @Resource
//    TimerService timerService;
//    @Resource
//    SessionContext context;
//    @EJB
//    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;
//    
//    private final static String ONECHECKOUTV1_SCHEDULER = "_ONECHECKOUTV1_NOTIFY_MERCHANT_SCHEDULER";
//
//    private Session getSession() {
//        return (Session) em.getDelegate();
//    }
//
//     public void startSchedulerTimer() {
//        for (Object obj : timerService.getTimers()) {
//            Timer timer = (Timer) obj;
//            if (ONECHECKOUTV1_SCHEDULER.equals(timer.getInfo())) {
//
//                OneCheckoutLogger.log("Timer service is working. Cancelling it");
//                timer.cancel();
//
//            }
//        }    
//
//        PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
//        int minutes = config.getInt("NOTIFY.STATUS.PERIOD",5);        
//        
//        //1.5  60  60 * 1000
//        Timer timer = timerService.createTimer(0, (minutes * 60 * 1000), ONECHECKOUTV1_SCHEDULER);
// //   Timer timer = timerService.createTimer(0, (2  60  60 * 1000), ONECHECKOUT_SCHEDULER);
//
//    }
//
//    @Timeout    
//    public void startSchedulerExecutor(Timer timer) {
//
//        PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
//        String on = config.getString("NOTIFY.SCHEDULLER","FALSE");
//
//        if (on.equalsIgnoreCase("FALSE")) {
//            OneCheckoutLogger.log("OneCheckoutV1 - scheduller of Notify Status To Merchant disabled");
//            return;
//        }
//
//        int maxrows = config.getInt("NOTIFY.STATUS.ROWS",10);
//        int age = config.getInt("NOTIFY.STATUS.AGE",2);    
//        
//        Calendar max = Calendar.getInstance();
//        max.add(Calendar.MINUTE, -1 * age);
//        Date maxtime = max.getTime();
//
//        Calendar min = Calendar.getInstance();
//        min.add(Calendar.HOUR,-24);
//        Date mintime = min.getTime();
//
//        Criteria criteria = getSession().createCriteria(Transactions.class);
//        criteria.createAlias("merchantPaymentChannel", "mpc");
//        criteria.createAlias("mpc.merchants", "m");
//        criteria.createAlias("mpc.paymentChannel", "pc");
//
//        criteria.add(Restrictions.le("dokuInvokeStatusDatetime", maxtime));
//        criteria.add(Restrictions.ge("dokuInvokeStatusDatetime", mintime));
//        criteria.add(Restrictions.eq("transactionsStatus", OneCheckoutTransactionStatus.SUCCESS.value()));
//       // criteria.add(Restrictions.ilike("systemMessage", "NOTIFYMERCHANT", MatchMode.START));
//        criteria.add(Restrictions.in("systemMessage", Arrays.asList("NOTIFYMERCHANT1","NOTIFYMERCHANT2","NOTIFYMERCHANT3","NOTIFYMERCHANT4","NOTIFYMERCHANT5","NOTIFYMERCHANT6","NOTIFYMERCHANT7","NOTIFYMERCHANT8","NOTIFYMERCHANT9","NOTIFYMERCHANT10")));
//
//        criteria.setMaxResults(maxrows);
//        List<Transactions> trxList = (List<Transactions>) criteria.list();
//                
//        if (trxList==null || trxList.isEmpty()) {
//            OneCheckoutLogger.log("SchedulerNotifyToMerchantBean.startSchedulerExecutor : 0 transactions");
//            return;
//        }
//
//        for (Transactions transactions : trxList) {
//            String logPrefix = "NotifyMerchantScheduler_" + transactions.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "_" + transactions.getIncTransidmerchant();
//            SecurityAssociation.setPrincipal(new OneCheckoutPrincipal(logPrefix));
//            OneCheckoutLogger.log("TRANSACTION.getSystemMessage[" + transactions.getSystemMessage() + "]");
//            String ret = transactions.getSystemMessage().replace("NOTIFYMERCHANT","");
//            
//            int attempt = config.getInt("NOTIFY.RETRY", 0);
//            try {
//                 attempt = transactions.getMerchantPaymentChannel().getMerchants().getMerchantRetryNotifyNumber();
//            }
//            catch (Exception ex) {}
//            
//            if (!ret.isEmpty()) {
//                int retry = -1;
//                try {
//                    retry = Integer.parseInt(ret);
//                } catch (Throwable th) {
//                    th.printStackTrace();
//                }
//                if (retry >= 0) {
//                  //  int attempt = config.getInt("NOTIFY.RETRY", 0);
//                    if (retry < attempt) {
//                        doNotify(transactions, retry);
//                    } else {
//                        doEmail(transactions, config);
//                    }
//                }
//            }
//        }
//        
////        Criteria crits = getSession().createCriteria(Transactions.class);
////        crits.createAlias("merchantPaymentChannel","mpc");
////        crits.createAlias("mpc.merchants", "m");
////        crits.createAlias("mpc.paymentChannel", "pc");
////        crits.add(Restrictions.le("dokuInvokeStatusDatetime", maxtime));
////        crits.add(Restrictions.ge("dokuInvokeStatusDatetime", mintime));
////        crits.add(Restrictions.isNull("dokuResponseCode"));
////        crits.add(Restrictions.isNull("transactionsStatus"));
////        List<Transactions>trxNullList = (List<Transactions>)crits.list();
////        
////        for (Transactions trxNullLst : trxNullList) {
////            trxNullLst.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
////            oneCheckoutV1QueryHelperBeanLocal.updateTransactions(trxNullLst);
////        }
//        SecurityAssociation.clear();
//    }
//
//    @Override
//    public boolean sendEmail(String bodyemail, String to, String subject, PropertiesConfiguration config) {
//          //  String to = config.getString("EMAIL.TO." + mallId,"hafiz@nsiapay.com");
//            String from = config.getString("mail.account","system-noreply@doku.com");
//            String cc = config.getString("mail.cc","rudy.thong@doku.com");
//            String host = config.getString("mail.host","smtp.gmail.com");
//            String password = config.getString("mail.password","n51aJKT!@#");
//
//            InternetRequest iRequest = new InternetRequest();
//            iRequest.setRequest((RequestType.EMAIL));
//            iRequest.setEmailTo(to);
//            iRequest.setEmailFrom(from);
//            iRequest.setEmailCc(cc);
//            iRequest.setEmailHost(host);
//            iRequest.setEmailMessageBody(bodyemail);
//            iRequest.setEmailMessageHeaderType("text/html");
//            iRequest.setEmailPassword(password);
//            iRequest.setEmailSubject(subject);
//
//            InternetResponse inetResp = DokuIntConnection.connect(iRequest);
//
//            if (inetResp.isStatusEmail()) {
//                OneCheckoutLogger.log("sending email succeded");
//            }
//            else {
//                OneCheckoutLogger.log("sending email failed");
//            }
//
//        if (inetResp.isStatusEmail()) {
//            OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
//            return true;
//        } else {
//            OneCheckoutLogger.log("FAILED SENDING EMAIL...");
//            return false;
//        }
//    }
//
//
//    @Override
//    public void doEmail(Transactions trans, PropertiesConfiguration config) {
/////apps/onecheckoutv1/email/email_notify_status.html
//
//        try {
//            String templatePath = config.getString("EMAIL.ALERT.TEMPLATE", "/apps/onecheckoutv1/email/");
//            String templateFileName = "email_notify_status.html";
//            File file = new File(templatePath + templateFileName);
//            boolean exists = file.exists();
//
//            if (!exists) {
//                OneCheckoutLogger.log(templateFileName + " does not exist");
//                return;
//            }
//
//            //Template temp = op.getTemp();
//            Configuration cfg = new Configuration();
//            cfg.setDirectoryForTemplateLoading(new File(templatePath));
//            cfg.setObjectWrapper(new DefaultObjectWrapper());
//
//            Template temp = cfg.getTemplate(templateFileName);
//            Map root = new HashMap();
//
//            HashMap<String, String> params = getData(trans);
//            ArrayList paramList = convertHashMap(params);
//            root.put("itemlist", paramList);
//            root.put("merchantName", trans.getMerchantPaymentChannel().getMerchants().getMerchantName());
//            root.put("MALLID", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode()+"");
//            root.put("invoiceNo", trans.getIncTransidmerchant());
//
//            StringWriter writer = new StringWriter();
//            temp.process(root, writer);
//
//            String subject = "[" + trans.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "-" + trans.getIncTransidmerchant()  + "] Notify Status Scheduller " +  OneCheckoutVerifyFormatData.email_datetimeFormat.format(new Date());
//            String to = trans.getMerchantPaymentChannel().getMerchants().getMerchantEmail();
//            
//            if (to==null || to.isEmpty()) {
//                trans.setSystemMessage("EMAILFAILED, no destination email ");
//                oneCheckoutV1QueryHelperBeanLocal.updateTransactions(trans);
//                return;
//            }
//            boolean result = sendEmail(writer.toString(),to,subject,config);//sendEmail(,email,config);
//            OneCheckoutLogger.log("SEND_EMAIL_RESULT[" + result + "]");
//            
//            if (result) {
//                trans.setSystemMessage("EMAILSUCCESS");
//                oneCheckoutV1QueryHelperBeanLocal.updateTransactions(trans);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            OneCheckoutLogger.log("ERROR : %s", ex.getMessage());
//        }
//    }
//
//    private void doNotify(Transactions trans, int retry) {
//        try {
//            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
//            HashMap<String, String> params  = super.getData(trans);
//            String url = trans.getMerchantPaymentChannel().getMerchants().getMerchantNotifyStatusUrl();
//            Merchants m = trans.getMerchantPaymentChannel().getMerchants();
////            OneCheckoutNotifyStatusResponse tst = super.notifyMerchant(m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout(),trans.getRates(),trans.getIncPurchaseamount().doubleValue(),trans.getIncPurchasecurrency());
////            OneCheckoutNotifyStatusResponse tst = super.sendToMerchant(url, params, retry, retry, Boolean.TRUE)sendToMerchant(url, params, retry, retry, Boolean.TRUE)
//            OneCheckoutNotifyStatusResponse tst = sendToMerchant(m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());
//
////            OneCheckoutNotifyStatusResponse tst = super.notifyMerchant(url, params, trans.getMerchantPaymentChannel().getMerchants().getMerchantConnectionTimeout(), trans.getMerchantPaymentChannel().getMerchants().getMerchantReadTimeout(), trans.getMerchantPaymentChannel().getMerchants().getMerchantNotifyTimeout());
//            OneCheckoutLogger.log("SchedulerNotifyToMerchantBean.doNotify : statusNotify : %s", tst.getACKNOWLEDGE());
//
//            String ack = tst.getACKNOWLEDGE();
//            if (ack != null && ack.toUpperCase().indexOf("CONTINUE") >= 0) {
//                trans.setSystemMessage("NOTIFYSUCCESS");
//            } else {
//                retry = retry+1;
//                trans.setSystemMessage("NOTIFYMERCHANT" +retry);
//            }
//
//            OneCheckoutLogger.log("SchedulerNotifyToMerchantBean.doNotify : update trx : %s", trans.getSystemMessage());
//            oneCheckoutV1QueryHelperBeanLocal.updateTransactions(trans);
//        } catch (Throwable ex) {
//            ex.printStackTrace();
//        }
//    }


}
