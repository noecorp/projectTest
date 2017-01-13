/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutPrincipal;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Transactions;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jboss.security.SecurityAssociation;

/**
 *
 * @author iskandar
 */
@Stateless
public class SchedulerTransactionOcoBean extends OneCheckoutChannelBase implements SchedulerTransactionOcoLocal{
    
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    
    private long t1 = System.currentTimeMillis();
    
    @Resource
    TimerService timerService;
    
    @Resource
    SessionContext context;
    
    @EJB
    private OneCheckoutV1QueryHelperBeanLocal oneCheckoutV1QueryHelperBeanLocal;

    private final static String ONECHECKOUTV1_TRANSACTION_OCO_SCHEDULER = "_ONECHECKOUTV1_TRANSACTION_OCO_SCHEDULER";
    
    private Session getSession() {
        return (Session) em.getDelegate();
    }
    
//    public void startSchedulerTimer() {
//        for (Object obj : timerService.getTimers()) {
//            Timer timer = (Timer) obj;
//            if (ONECHECKOUTV1_TRANSACTION_OCO_SCHEDULER.equals(timer.getInfo())) {
//                OneCheckoutLogger.log("Timer service is working. Cancelling it");
//                timer.cancel();
//            }
//        }    
//        PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
//        int minutes = config.getInt("NOTIFY.STATUS.PERIOD",5);        
//        
//        Timer timer = timerService.createTimer(0, (minutes * 60 * 1000), ONECHECKOUTV1_TRANSACTION_OCO_SCHEDULER);
//     }
     
//  @Timeout
     
    public void startSchedulerDaily(){
         PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
         String on = config.getString("TRANSACTION.OCO.SCHEDULER.","TRUE");
     
         if(on.equalsIgnoreCase("FALSE")){
            OneCheckoutLogger.log("OneCheckoutV1 - scheduller of Transaction OCO disabled");
            return;
         }
         
         int maxrows = config.getInt("NOTIFY.STATUS.ROWS",50);
         Calendar max = Calendar.getInstance();
         max.add(Calendar.HOUR,-24);
         Date executeTime = max.getTime();
         
         Criteria criterias = getSession().createCriteria(Transactions.class);
         criterias.createAlias("merchantPaymentChannel", "mpc");
         criterias.createAlias("mpc.merchants", "m");
         criterias.createAlias("mpc.paymentChannel", "pc");
         criterias.add(Restrictions.le("incRequestdatetime", executeTime));
         criterias.addOrder(Order.desc("incRequestdatetime"));
         criterias.setMaxResults(maxrows);
         
         OneCheckoutLogger.log("TRANSACTION.beforeToday.date" +executeTime + "]");
         
         List<Transactions>trxListOco = (List<Transactions>) criterias.list();
        
        if (trxListOco==null || trxListOco.isEmpty()) {
            OneCheckoutLogger.log("SchedulerTransactionOcoBean.startSchedulerExecutor : 0 transactions");
            return;
        }
        
        for (Transactions transactions : trxListOco) {
         if(StringUtils.isEmpty(transactions.getDokuResponseCode()) && transactions.getTransactionsStatus() == null){
                 OneCheckoutErrorMessage failedMsg = OneCheckoutErrorMessage.UNKNOWN;
                 
                 String logPrefix = "TransactionOcoScheduler_" + transactions.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "_" + transactions.getIncTransidmerchant();
                 SecurityAssociation.setPrincipal(new OneCheckoutPrincipal(logPrefix));

                 failedMsg = OneCheckoutErrorMessage.FAILED_TRANSACTION_VIA_SCHEDULER;
                 transactions.setDokuResponseCode(failedMsg.value());
                 transactions.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                 oneCheckoutV1QueryHelperBeanLocal.updateTransactions(transactions);
                 
                 OneCheckoutLogger.log("SchedulerTransactionOcoBean : update finished trx : ");
             }
         }
         SecurityAssociation.clear();
     }
    
}
