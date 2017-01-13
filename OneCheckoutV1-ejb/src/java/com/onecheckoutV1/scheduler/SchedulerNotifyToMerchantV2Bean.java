/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
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
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
public class SchedulerNotifyToMerchantV2Bean extends OneCheckoutChannelBase implements SchedulerNotifyToMerchantV2BeanLocal {

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    private long t1 = System.currentTimeMillis();
    @Resource
    SessionContext context;
    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;
    
    private final static String ONECHECKOUTV1_SCHEDULER = "_ONECHECKOUTV1_NOTIFY_MERCHANT_SCHEDULER";

    private Session getSession() {
        return (Session) em.getDelegate();
    }

    
    
    
    public String getTransactionList(int reminder, int quotient) {

        SecurityAssociation.setPrincipal(new OneCheckoutPrincipal(ONECHECKOUTV1_SCHEDULER));
 //       PermataUtils.setSecurityAssociation("NOTIFY_SCHEDULLER");
 //       OneCheckoutLogger.log(PermataUtils.LOG_SEPARATOR_THICK);
        String res = "";
        try {
            
            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            String on = config.getString("NOTIFY.SCHEDULLER","FALSE");

            if (on.equalsIgnoreCase("FALSE")) {

                OneCheckoutLogger.log("OneCheckoutV1 - scheduller of Notify Status To Merchant disabled");
                return null;

            }

            int maxrows = config.getInt("NOTIFY.STATUS.ROWS",10);
            int age = config.getInt("NOTIFY.STATUS.AGE",2);    

            Calendar max = Calendar.getInstance();
            max.add(Calendar.MINUTE, -1 * age);
            Date maxtime = max.getTime();

            Calendar min = Calendar.getInstance();
            min.add(Calendar.HOUR,-24);
            Date mintime = min.getTime();


            Criteria criteria = getSession().createCriteria(Transactions.class);
            criteria.createAlias("merchantPaymentChannel", "mpc");
            criteria.createAlias("mpc.merchants", "m");
            criteria.createAlias("mpc.paymentChannel", "pc");

            criteria.add(Restrictions.le("dokuInvokeStatusDatetime", maxtime));
            criteria.add(Restrictions.ge("dokuInvokeStatusDatetime", mintime));
            criteria.add(Restrictions.eq("transactionsStatus", OneCheckoutTransactionStatus.SUCCESS.value()));
           // criteria.add(Restrictions.ilike("systemMessage", "NOTIFYMERCHANT", MatchMode.START));
            criteria.add(Restrictions.in("systemMessage", Arrays.asList("NOTIFYMERCHANT1","NOTIFYMERCHANT2","NOTIFYMERCHANT3","NOTIFYMERCHANT4","NOTIFYMERCHANT5","NOTIFYMERCHANT6","NOTIFYMERCHANT7","NOTIFYMERCHANT8","NOTIFYMERCHANT9","NOTIFYMERCHANT10")));
            criteria.add(Restrictions.sqlRestriction("transactions_id % " + quotient + " = " + reminder));
            criteria.setMaxResults(maxrows);

            List<Transactions> lstTransaction = (List<Transactions>) criteria.list();            

            
            if (lstTransaction != null) {
                OneCheckoutLogger.log("TRANSACTION IS NOT NULL, SIZE[" + lstTransaction.size() + "]");
                //            int threadCount = PermataUtils.getConfig().getInt(PermataUtils.PAYMENT_NOTIFY_THREAD_COUNT, 25);
                //            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Transactions transaction : lstTransaction) {
                    //executorService.execute(new SettlementThread(transaction));

                    if (first) {
                        String poolingId = Long.toString(transaction.getTransactionsId());
                        String checksum = HashWithSHA1.doHashing(poolingId, "SHA1", "ocov1");

                        sb.append(poolingId).append("_").append(checksum);
                        first = false;

                    } else {

                        String poolingId = Long.toString(transaction.getTransactionsId());
                        String checksum = HashWithSHA1.doHashing(poolingId, "SHA1", "ocov1");

                        sb.append("|").append(poolingId).append("_").append(checksum);                        
                        
                    }
                }

                res = sb.toString();
            } else {
                
                res = "ERROR:52";
            }
            return res;

        } catch (Throwable th) {
            return "ERROR:53";
        }
    }

    public void doNotify(String transactionId) {
        
        PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
        try {
            String[] fword = transactionId.split("_");
            
            String poolingId = fword[0];
            String checksum = HashWithSHA1.doHashing(poolingId, "SHA1", "ocov1");//AeSimpleSHA1.SHA1(poolingId + "ocov1");
            
            if (checksum.equals(fword[1])) {
                
                long id = Long.valueOf(poolingId).longValue();

                Transactions transactions = em.find(Transactions.class, id);

                
                String logPrefix = "NotifyMerchantScheduler_" + transactions.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "_" + transactions.getIncTransidmerchant();
                SecurityAssociation.setPrincipal(new OneCheckoutPrincipal(logPrefix));
                OneCheckoutLogger.log("TRANSACTION.getSystemMessage[" + transactions.getSystemMessage() + "]");
                String ret = transactions.getSystemMessage().replace("NOTIFYMERCHANT","");

                int attempt = config.getInt("NOTIFY.RETRY", 0);
                try {
                     attempt = transactions.getMerchantPaymentChannel().getMerchants().getMerchantRetryNotifyNumber();
                }
                catch (Exception ex) {

                }

                if (!ret.isEmpty()) {
                    int retry = -1;
                    try {
                        retry = Integer.parseInt(ret);
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                    if (retry >= 0) {
                      //  int attempt = config.getInt("NOTIFY.RETRY", 0);
                        if (retry < attempt) {
                            doNotify(transactions, retry);
                        } else {
                            doEmail(transactions, config);
                        }
                    }
                }                

                
            }
            SecurityAssociation.clear();
         } catch (Throwable th) {

             th.printStackTrace();
         }
        
        
       
        
        
    }    
    

//    @Override
//    public boolean sendEmail(String bodyemail, String to, String subject, PropertiesConfiguration config) {
//
//
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
//
//                OneCheckoutLogger.log("sending email failed");
//            }
//
//        if (inetResp.isStatusEmail()) {
//            OneCheckoutLogger.log("SUCCESS SENDING EMAIL...");
//            return true;
//        } else {
//            OneCheckoutLogger.log("FAILED SENDING EMAIL...");
//            return false;
//
//        }
//
//    }


    @Override
    protected void doEmail(Transactions trans, PropertiesConfiguration config) {
///apps/onecheckoutv1/email/email_notify_status.html

        try {
            String templatePath = config.getString("EMAIL.ALERT.TEMPLATE", "/apps/onecheckoutv1/email/");
            String templateFileName = "email_notify_status.html";
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

            HashMap<String, String> params = getData(trans);
            ArrayList paramList = convertHashMap(params);
            root.put("itemlist", paramList);
            root.put("merchantName", trans.getMerchantPaymentChannel().getMerchants().getMerchantName());
            root.put("MALLID", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode()+"");
            root.put("invoiceNo", trans.getIncTransidmerchant());

            StringWriter writer = new StringWriter();
            temp.process(root, writer);

            String subject = "[" + trans.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "-" + trans.getIncTransidmerchant()  + "] Notify Status Scheduller " +  OneCheckoutVerifyFormatData.email_datetimeFormat.format(new Date());
            String to = trans.getMerchantPaymentChannel().getMerchants().getMerchantEmail();
            
            if (to==null || to.isEmpty()) {
                trans.setSystemMessage("EMAILFAILED, no destination email ");
                oneCheckoutV1QueryHelperBeanLocal.updateTransactions(trans);
                return;
            }
            boolean result = sendEmail(writer.toString(),to,subject,config);//sendEmail(,email,config);
            OneCheckoutLogger.log("SEND_EMAIL_RESULT[" + result + "]");
            
            if (result) {
                trans.setSystemMessage("EMAILSUCCESS");
                oneCheckoutV1QueryHelperBeanLocal.updateTransactions(trans);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            OneCheckoutLogger.log("ERROR : %s", ex.getMessage());
        }
    }

    private void doNotify(Transactions trans, int retry) {
        try {
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            HashMap<String, String> params  = super.getData(trans);
            String url = trans.getMerchantPaymentChannel().getMerchants().getMerchantNotifyStatusUrl();
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();
//            OneCheckoutNotifyStatusResponse tst = super.notifyMerchant(m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout(),trans.getRates(),trans.getIncPurchaseamount().doubleValue(),trans.getIncPurchasecurrency());
//            OneCheckoutNotifyStatusResponse tst = super.sendToMerchant(url, params, retry, retry, Boolean.TRUE)sendToMerchant(url, params, retry, retry, Boolean.TRUE)
            OneCheckoutNotifyStatusResponse tst = sendToMerchant(m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());

//            OneCheckoutNotifyStatusResponse tst = super.notifyMerchant(url, params, trans.getMerchantPaymentChannel().getMerchants().getMerchantConnectionTimeout(), trans.getMerchantPaymentChannel().getMerchants().getMerchantReadTimeout(), trans.getMerchantPaymentChannel().getMerchants().getMerchantNotifyTimeout());
            OneCheckoutLogger.log("SchedulerNotifyToMerchantBean.doNotify : statusNotify : %s", tst.getACKNOWLEDGE());

            String ack = tst.getACKNOWLEDGE();
            if (ack != null && ack.toUpperCase().indexOf("CONTINUE") >= 0) {
                trans.setSystemMessage("NOTIFYSUCCESS");
            } else {
                retry = retry+1;
                trans.setSystemMessage("NOTIFYMERCHANT" +retry);
            }

            OneCheckoutLogger.log("SchedulerNotifyToMerchantBean.doNotify : update trx : %s", trans.getSystemMessage());
            oneCheckoutV1QueryHelperBeanLocal.updateTransactions(trans);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
