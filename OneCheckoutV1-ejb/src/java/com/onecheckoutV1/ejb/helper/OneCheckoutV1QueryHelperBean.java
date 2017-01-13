/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.ejb.util.EmailUtility;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutRateStatus;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.*;
import doku.fx.micropay.entity.RBSRateQuote;
import doku.fx.micropay.enums.ECurrency;
import doku.fx.micropay.enums.EResponseCode;
import doku.fx.micropay.helper.RBSRequestHelper;
import doku.keylib.tools.KMCardUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author hafizsjafioedin
 */
@Stateless
public class OneCheckoutV1QueryHelperBean implements OneCheckoutV1QueryHelperBeanLocal {

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    @PersistenceContext(unitName = "DOKUESCROW")
    protected EntityManager emEscrow;

    private Session getSession() {
        return (Session) em.getDelegate();
    }

    private Session getSessionEscrow() {
        return (Session) emEscrow.getDelegate();
    }

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    public com.onechekoutv1.dto.Session getSessionByRequestCode(String requestCode) {
        com.onechekoutv1.dto.Session session = null;
        try {
            String sql = "select s from Session s inner join fetch s.merchantIdx m where s.requestCode=:requestCode and s.status='A'";
            session = (com.onechekoutv1.dto.Session) em.createQuery(sql).setParameter("requestCode", requestCode).getSingleResult();
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println("ERROR - OneCheckoutV1QueryHelperBean.getSessionByRequestCode[" + th.getMessage() + "]");
        }
        return session;
    }

    public boolean deleteSessionById(int sessionId) {
        boolean status = false;
        try {
            String sql = "delete from Session s where s.id=:sessionId";
            em.createNativeQuery(sql).setParameter("sessionId", sessionId).executeUpdate();
            status = true;
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println("ERROR - OneCheckoutV1QueryHelperBean.deleteSessionById[" + th.getMessage() + "]");
        }
        return status;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateTransactions(Transactions trx) {
        try {
            em.merge(trx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getNextValue(Class cls) {
        int num = Integer.valueOf(em.createNativeQuery("select nextval('seq_" + cls.getSimpleName().toLowerCase() + "')").getSingleResult().toString());
        return num;
    }

    public Object updateRecord(Object obj) {
        return getSession().merge(obj);
    }

    public Number getNextVal(Class forEntity) {
        return (Number) em.createQuery("select serial.nextval from " + forEntity.getName() + " serial").getSingleResult();
    }

    public Country getCountryByNumericCode(String numericCode) {
        Country country = null;
        try {
            Criteria criteria = getSession().createCriteria(Country.class);
            criteria.add(Restrictions.eq("numericCode", numericCode));
            country = (Country) criteria.uniqueResult();
            if (country == null) {
                throw new Exception("country code Not Found");
            }
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCountryByNumericCode   Exception : %s", th.getMessage());
        }
        return country;
    }

    public Merchants getMerchantByMallId(int mallId, int chainmerchantId) {
        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantByMallId : mallId=%d, chainMerchantId=%d", mallId, chainmerchantId);
        //    return em.find(Merchant.class, mallId);

        Merchants m = null;
        try {

            String query = "SELECT m FROM Merchants m WHERE m.merchantCode=:mCode and m.merchantChainMerchantCode=:cmCode";

            m = (Merchants) em.createQuery(query).setParameter("mCode", mallId).setParameter("cmCode", chainmerchantId).getSingleResult();

            return m;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantByMallId Exception : %s", th.getMessage());
//            th.printStackTrace();
            return m;

        }

    }

    public PaymentChannel getPaymentChannelByChannel(OneCheckoutPaymentChannel channel) {
        return em.find(PaymentChannel.class, channel.value());
    }

    public MerchantPaymentChannel getMerchantPaymentChannel(Merchants merchant, PaymentChannel pchannel) {

        try {
            //    OneCheckoutLogger.log("PaymentChannel : %s", pchannel.getPaymentChannelId());
            //    OneCheckoutLogger.log("Merchant : %d", merchant.getMerchantId());
            OneCheckoutLogger.log("merchants = " + merchant.getMerchantIdx());
            OneCheckoutLogger.log("payChannel= " + pchannel.getPaymentChannelId());
            //old(start)
//            return (MerchantPaymentChannel) em.createQuery("SELECT mpc FROM MerchantPaymentChannel mpc "
//                    + "WHERE mpc.paymentChannel=:pc and mpc.merchants=:m").setParameter("pc", pchannel).setParameter("m", merchant).getSingleResult();
            //old(end)
            return (MerchantPaymentChannel) em.createQuery("SELECT mpc FROM MerchantPaymentChannel mpc "
                    + "WHERE mpc.paymentChannel=:pc and mpc.merchants=:m and mpc.merchantPaymentChannelStatus=:pcs").setParameter("pc", pchannel).setParameter("m", merchant).setParameter("pcs", true).getSingleResult();

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantPaymentChannel  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return null;

        }

    }

    public MerchantPaymentChannel getMerchantPaymentChannel(Merchants merchant, OneCheckoutPaymentChannel paymentChannel) {
        MerchantPaymentChannel mpc = null;

        try {
            Criteria criteria = getSession().createCriteria(MerchantPaymentChannel.class);

            criteria.createCriteria("merchants", "merchants", Criteria.INNER_JOIN);
            criteria.createCriteria("paymentChannel", "paymentChannel", Criteria.INNER_JOIN);

            //criteria.add(Restrictions.eq("paymentChannel.paymentChannelId", paymentChannel.PermataVALite.value()));
            criteria.add(Restrictions.eq("paymentChannel.paymentChannelId", paymentChannel.value()));
            criteria.add(Restrictions.eq("merchantPaymentChannelStatus", true));
            criteria.add(Restrictions.eq("merchants", merchant));

            mpc = (MerchantPaymentChannel) criteria.uniqueResult();
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantPaymentChannel   Exception : %s", th.getMessage());
//            th.printStackTrace();
        }

        return mpc;
    }

    public List<Transactions> getTransactionsBy(String invoiceNo, MerchantPaymentChannel mpc) {

        // tadinya ditambah where sessiond id, tapi hasil diskusi sepertinya tidak perlu
//        List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "WHERE trans.incTransidmerchant=:invNo and trans.incSessionid=:sessId and mpc.merchants=:merchant").setParameter("invNo", invoiceNo).setParameter("merchant", mpc.getMerchants()).setParameter("sessId", sessionID).getResultList();
        List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "WHERE trans.incTransidmerchant=:invNo and mpc.merchants=:merchant").setParameter("invNo", invoiceNo).setParameter("merchant", mpc.getMerchants()).getResultList();

        return transList;

    }

    public List<TransactionsDataAirlines> getTransactionsByPNR(String pnr, MerchantPaymentChannel mpc) {
        try {

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionsByPNR   PNR : %s", pnr);

            //      Transactions t = new Transactions();
            //      Set<TransactionsDataAirlines> data_airlines = t.getTransactionsDataAirlineses();
            Criteria criteria = getSession().createCriteria(TransactionsDataAirlines.class);
            //           TransactionsDataAirlines airline = new TransactionsDataAirlines();
            // airline.get
            criteria.createCriteria("transactions", "trans", Criteria.INNER_JOIN);
            criteria.createCriteria("trans.merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.ilike("incBookingcode", pnr));
            criteria.add(Restrictions.eq("m.merchantCode", mpc.getMerchants().getMerchantCode()));

            criteria.add(Restrictions.eq("trans.transactionsState", OneCheckoutTransactionState.DONE.value()));
            criteria.add(Restrictions.eq("trans.transactionsStatus", OneCheckoutTransactionStatus.SUCCESS.value()));

            Calendar border = Calendar.getInstance();
            border.set(Calendar.HOUR, 0);
            border.set(Calendar.MINUTE, 0);
            border.set(Calendar.SECOND, 0);

            Date mintime = border.getTime();

            criteria.add(Restrictions.ge("trans.transactionsDatetime", mintime));
            criteria.addOrder(Order.desc("trans.transactionsDatetime"));
            //         criteria.setMaxResults(1);

            List<TransactionsDataAirlines> dataAirlinesList = (List<TransactionsDataAirlines>) criteria.list();//.uniqueResult();

            return dataAirlinesList;

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionsByPNR   Exception : %s", th.getMessage());
            //      th.printStackTrace();
            return null;
        }

    }

    public Transactions getVerifyTransactionBy(String invoiceNo, String sessionId, double amount, PaymentChannel pchannel) {

        Transactions trans = null;
        try {

            BigDecimal amount_val = BigDecimal.valueOf(amount);
            trans = (Transactions) em.createQuery("SELECT trans FROM Transactions trans " + "INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "INNER JOIN FETCH mpc.merchants m " + "WHERE trans.incTransidmerchant=:invNo and trans.incSessionid=:sessId and trans.incAmount=:amount " + "and trans.transactionsState=:state and mpc.paymentChannel=:pc").setParameter("invNo", invoiceNo).setParameter("sessId", sessionId).setParameter("amount", amount_val).setParameter("pc", pchannel).setParameter("state", OneCheckoutTransactionState.INCOMING.value()).getSingleResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getVerifyTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    public Transactions getRedirectTransactionWithoutState(String invoiceNo, String sessionId, double amount) {

        Transactions tn = null;
        try {

            OneCheckoutLogger.log("[" + invoiceNo + "]" + "[" + sessionId + "]" + "[" + amount + "]");
            BigDecimal amount_val = BigDecimal.valueOf(amount);
            List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans "
                    + "INNER JOIN FETCH trans.merchantPaymentChannel mpc "
                    + "INNER JOIN FETCH mpc.merchants m "
                    + "INNER JOIN FETCH mpc.paymentChannel pc "
                    + "WHERE trans.incTransidmerchant=:invNo and trans.incSessionid=:sessId and trans.incAmount=:amount "
                    + "ORDER BY trans.transactionsDatetime DESC").setParameter("invNo", invoiceNo).setParameter("sessId", sessionId).setParameter("amount", amount_val) //   .setParameter("state", OneCheckoutTransactionState.DONE.value())   dfdfdf
                    .setMaxResults(1).getResultList();

            if (transList != null) {
                for (Transactions trans : transList) {
                    tn = trans;
                }
                return tn;
            } else {
                return null;
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionWithoutState  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return tn;

        }

    }

    public Object[] getRedirectTransactionWithoutStateNumber(String invoiceNo, String sessionId, double amount) {

        Transactions tn = null;
        Object[] o = null;
        try {

            OneCheckoutLogger.log("[" + invoiceNo + "]" + "[" + sessionId + "]" + "[" + amount + "]");
            BigDecimal amount_val = BigDecimal.valueOf(amount);
            List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans "
                    + //TAMBAHAN
                    //"LEFT JOIN FETCH trans.transactionsDataCardholders cardholder "
                    //+ "LEFT JOIN FETCH trans.transactionsDataNonAirlineses nonAirlines "
                    //+ "LEFT JOIN FETCH trans.transactionsDataAirlineses airlines "
                    "INNER JOIN FETCH trans.merchantPaymentChannel mpc "
                    + "INNER JOIN FETCH mpc.merchants m "
                    + "INNER JOIN FETCH mpc.paymentChannel pc "
                    + "WHERE trans.incTransidmerchant=:invNo and trans.incSessionid=:sessId and trans.incAmount=:amount "
                    + "ORDER BY trans.transactionsDatetime DESC").setParameter("invNo", invoiceNo).setParameter("sessId", sessionId).setParameter("amount", amount_val) //   .setParameter("state", OneCheckoutTransactionState.DONE.value())   dfdfdf
                    .getResultList();

            if (transList != null && transList.size() > 0) {
                tn = transList.get(0);
                if (tn.getDokuResponseCode() != null) {
                    return new Object[]{transList.size(), tn};
                } else {
                    return new Object[]{0, tn};
                }
            } else {
                return new Object[]{0, null};
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionWithoutStateNumber   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return new Object[]{0, null};

        }

    }

    public Transactions getRedirectTransactionWithoutState(String invoiceNo, String sessionId, double amount, String paymentCode, String paymentChannelId) {
        Transactions trans = null;

        try {
            BigDecimal _amount = BigDecimal.valueOf(amount);

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("incSessionid", sessionId));
            criteria.add(Restrictions.eq("incAmount", _amount));
            criteria.add(Restrictions.eq("accountId", paymentCode));
            criteria.add(Restrictions.eq("pc.paymentChannelId", paymentChannelId));

            criteria.addOrder(Order.desc("transactionsDatetime"));
            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionWithoutState  Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return trans;
    }

    public Transactions getNotifyTransactionBy(String invoiceNo, String sessionId, PaymentChannel pchannel) {

        Transactions trans = null;
        try {

            trans = (Transactions) em.createQuery("SELECT trans FROM Transactions trans " + "INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "INNER JOIN FETCH mpc.merchants m " //ADD THIS, FOR NOTIFY MPG/BSP
                    //+ "LEFT JOIN FETCH trans.transactionsDataCardholders cardHolder "
                    + "WHERE trans.incTransidmerchant=:invNo and trans.incSessionid=:sessId and trans.transactionsState=:state " + "and mpc.paymentChannel=:pc").setParameter("invNo", invoiceNo).setParameter("sessId", sessionId).setParameter("pc", pchannel).setParameter("state", OneCheckoutTransactionState.NOTIFYING.value()).getSingleResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getNotifyTransactionBy Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    public Transactions getVoidTransactionBy(String invoiceNo, String sessionId, PaymentChannel pchannel) {

        Transactions trans = null;
        try {

            trans = (Transactions) em.createQuery("SELECT trans FROM Transactions trans " + "INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "INNER JOIN FETCH mpc.merchants m " + "WHERE trans.incTransidmerchant=:invNo and trans.incSessionid=:sessId and trans.transactionsState=:state " + "and mpc.paymentChannel=:pc").setParameter("invNo", invoiceNo).setParameter("sessId", sessionId).setParameter("pc", pchannel).setParameter("state", OneCheckoutTransactionState.DONE.value()).getSingleResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getVoidTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    public void updateKlikBCAExpiredTransactions(String userId, String merchantCode, PaymentChannel pchannel, int expired_minutes) {

        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -1 * expired_minutes);
            Date ambang = cal.getTime();

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateKlikBCAExpiredTransactions accountId=%s", userId);
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateKlikBCAExpiredTransactions state=%s", OneCheckoutTransactionState.INCOMING.value());
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateKlikBCAExpiredTransactions pc=%s", pchannel.getPaymentChannelId());
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateKlikBCAExpiredTransactions batas=%s", ambang);
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateKlikBCAExpiredTransactions merchantCode=%s", merchantCode);

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", userId));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.le("transactionsDatetime", ambang));
            criteria.add(Restrictions.eq("mpc.merchantPaymentChannelUid", merchantCode));

            criteria.addOrder(Order.desc("transactionsDatetime"));

            List<Transactions> transList = (List<Transactions>) criteria.list();

            if (transList != null) {

                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateKlikBCAExpiredTransactions Get Rows = " + transList.size());

                for (Transactions trans : transList) {

                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.EXPIRED_TIME_EXCEED.value());
                    trans.setDokuResultMessage(OneCheckoutErrorMessage.EXPIRED_TIME_EXCEED.name());
                    em.merge(trans);
                }

            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateKlikBCAExpiredTransactions   Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

    }

    public void updateVirtualAccountExpiredTransactions(String paymentCode, String merchantCode, PaymentChannel pchannel, int expired_minutes) {

        try {

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, (-1 * expired_minutes));
            Date ambang = cal.getTime();
            //Date ambang = new Date(cal.getTime().getTime());

            OneCheckoutLogger.log("============= Param Expired Transactions =============");
            OneCheckoutLogger.log("PAYMENT CODE    [" + paymentCode + "]");
            OneCheckoutLogger.log("STATE           [" + OneCheckoutTransactionState.INCOMING.value() + "]");
            OneCheckoutLogger.log("MALL ID         [" + merchantCode + "]");
            OneCheckoutLogger.log("PAYMENT CHANNEL [" + pchannel.getPaymentChannelName() + "]");
            OneCheckoutLogger.log("EXP IN MINUTES  [" + expired_minutes + "]");
            OneCheckoutLogger.log("AMBANG          [" + ambang + "]");
            OneCheckoutLogger.log("======================================================");

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("merchantPaymentChannel.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paymentCode));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.le("transactionsDatetime", ambang));
            criteria.add(Restrictions.eq("mpc.paymentChannelCode", Integer.parseInt(merchantCode)));
            criteria.add(Restrictions.or(Restrictions.isNull("dokuResponseCode"), Restrictions.not(Restrictions.eq("dokuResponseCode", OneCheckoutErrorMessage.INIT_VA.value()))));
            criteria.addOrder(Order.desc("transactionsDatetime"));

            List<Transactions> transList = criteria.list();

            /*List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT t FROM Transactions t " +
             "WHERE t.accountId=:paymentCode AND t.transactionsState=:state " +
             "AND t.merchantPaymentChannel.paymentChannel=:pc " + "AND t.transactionsDatetime<:batas " +
             "AND t.merchantPaymentChannel.paymentChannelCode=:merchantCode")
             .setParameter("paymentCode", paymentCode).setParameter("state", OneCheckoutTransactionState.INCOMING.value())
             .setParameter("pc", pchannel).setParameter("batas", ambang)
             .setParameter("merchantCode", Integer.parseInt(merchantCode))
             .getResultList();*/
            if (transList != null) {

                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateVirtualAccountExpiredTransactions Get Rows = " + transList.size());

                for (Transactions trans : transList) {

                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.EXPIRED_TIME_EXCEED.value());
                    trans.setDokuResultMessage(OneCheckoutErrorMessage.EXPIRED_TIME_EXCEED.name());
                    em.merge(trans);
                }

            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.updateVirtualAccountExpiredTransactions  Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

    }

    public Transactions getKlikBCATransactionBy(String invoiceNo, String userId, double amount, Date requestPayment, PaymentChannel pchannel) {

        Transactions trans = null;

        try {

            BigDecimal amount_val = BigDecimal.valueOf(amount);

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", userId));
            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("incAmount", amount_val));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getKlikBCATransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;
        }

    }

    public List<Transactions> getKlikBCATransactionBy(String userId, String merchantCode, PaymentChannel pchannel) {

        List<Transactions> transList = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            //criteria.add(Restrictions.ilike("accountId", userId));            
            criteria.add(Restrictions.eq("accountId", userId));
            criteria.add(Restrictions.eq("mpc.merchantPaymentChannelUid", merchantCode));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            transList = criteria.list();

            OneCheckoutLogger.log(": : : : : TRANSACTION SIZE = " + transList.size());

            return transList;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getKlikBCATransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return transList;

        }

    }

    public TransactionsDataAirlines getTransactionAirlinesBy(Transactions trans) {

        TransactionsDataAirlines airlines = null;

        try {

            airlines = (TransactionsDataAirlines) em.createQuery("SELECT airlines FROM TransactionsDataAirlines airlines " + "INNER JOIN FETCH airlines.transactions trans " + "WHERE airlines.transactions=:t").setParameter("t", trans).getSingleResult();

            return airlines;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionAirlinesBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return airlines;

        }

    }

    public TransactionsDataNonAirlines getTransactionNonAirlinesBy(Transactions trans) {

        TransactionsDataNonAirlines nonairlines = null;

        try {

            nonairlines = (TransactionsDataNonAirlines) em.createQuery("SELECT nonAirlines FROM TransactionsDataNonAirlines nonAirlines " + "INNER JOIN FETCH nonAirlines.transactions trans " + "WHERE nonAirlines.transactions=:t").setParameter("t", trans).getSingleResult();

            return nonairlines;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionNonAirlinesBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return nonairlines;

        }

    }

    public TransactionsDataCardholder getTransactionCHBy(Transactions trans) {

        TransactionsDataCardholder ch = null;

        try {

            ch = (TransactionsDataCardholder) em.createQuery("SELECT ch FROM TransactionsDataCardholder ch " + "INNER JOIN FETCH ch.transactions trans " + "WHERE ch.transactions=:t").setParameter("t", trans).getSingleResult();

            return ch;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionCHBy Exception : %s", th.getMessage());
//            th.printStackTrace();
            return ch;

        }

    }

    public Transactions getRedirectTransactionBy(String invoiceNo, String sessionId, double amount) {

        Transactions tn = null;
        try {

            BigDecimal amount_val = BigDecimal.valueOf(amount);
            List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans "
                    + "INNER JOIN FETCH trans.merchantPaymentChannel mpc "
                    + "INNER JOIN FETCH mpc.merchants m "
                    + "INNER JOIN FETCH mpc.paymentChannel pc "
                    + "WHERE trans.incTransidmerchant=:invNo and "
                    + "trans.incSessionid=:sessId and "
                    + "trans.incAmount=:amount " + "and "
                    + "trans.transactionsState=:state "
                    + "ORDER BY trans.transactionsDatetime DESC").setParameter("invNo", invoiceNo).setParameter("sessId", sessionId).setParameter("amount", amount_val).setParameter("state", OneCheckoutTransactionState.DONE.value()).setMaxResults(1).getResultList();

            if (transList != null) {
                for (Transactions trans : transList) {
                    tn = trans;
                }
                return tn;
            } else {
                return null;
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return tn;

        }

    }

    public Transactions getRedirectTransactionBy(String invoiceNo, String sessionId, double amount, String paymentCode, String paymentChannelId) {
        Transactions trans = null;

        try {
            BigDecimal _amount = BigDecimal.valueOf(amount);

            Criteria criteria = getSession().createCriteria(Transactions.class);

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionBy TRANSIDMERCHANT  [" + invoiceNo + "]");
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionBy SESSIONID        [" + sessionId + "]");
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionBy AMOUNT           [" + amount + "]");
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionBy PAYMENTCODE      [" + paymentCode + "]");
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionBy PAYMENTCHANNEL   [" + paymentChannelId + "]");

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("incSessionid", sessionId));
            criteria.add(Restrictions.eq("incAmount", _amount));
            criteria.add(Restrictions.eq("accountId", paymentCode));
            criteria.add(Restrictions.eq("pc.paymentChannelId", paymentChannelId));
            //criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));

            criteria.addOrder(Order.desc("transactionsDatetime"));
            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return trans;
    }

    public Transactions getKlikBCARedirectTransactionBy(String invoiceNo, String sessionId, double amount) {
        Transactions trans = null;

        try {
            BigDecimal _amount = BigDecimal.valueOf(amount);

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("incSessionid", sessionId));
            criteria.add(Restrictions.eq("incAmount", _amount));
            criteria.add(Restrictions.eq("pc.paymentChannelId", OneCheckoutPaymentChannel.KlikBCA.value()));

            criteria.addOrder(Order.desc("transactionsDatetime"));
            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getKlikBCARedirectTransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return trans;
    }

    public Transactions getEDSDataTransactionBy(String invoiceNo, double amount, PaymentChannel pchannel, String words) {

        Transactions trans = null;
        try {

            BigDecimal amount_val = BigDecimal.valueOf(amount);
            List<Transactions> transList = em.createQuery("SELECT trans FROM Transactions trans " + "INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "INNER JOIN FETCH mpc.merchants m " + "WHERE trans.incTransidmerchant=:invNo and trans.transactionsStatus=:status and trans.incAmount=:amount " + "and trans.transactionsState=:state and mpc.paymentChannel=:pc").setParameter("invNo", invoiceNo).setParameter("status", OneCheckoutTransactionStatus.SUCCESS.value()).setParameter("amount", amount_val).setParameter("pc", pchannel).setParameter("state", OneCheckoutTransactionState.DONE.value()).getResultList();

            if (transList != null) {

                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : %d rows", transList.size());
                for (Transactions t : transList) {

                    //ref = merchant.getMid() + merchant.getPassword() + invoiceNo;
                    String strHash = t.getMerchantPaymentChannel().getMerchantPaymentChannelUid() + t.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + invoiceNo;
                    String hash = HashWithSHA1.doHashing(strHash, null, null);
                    //String ref = merchant.getMid() + merchant.getPassword() + invoiceNo;
                    if (hash.equalsIgnoreCase(words)) {
                        trans = t;
                        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : Match MallID=%d %s=%s", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
                    } else {
                        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : not Match MallID=%d %s=%s", t.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
                    }
                }

            } else {
                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : 0 rows");
                return trans;
            }

            return trans;

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    public Transactions getEDSDataTransactionBy(String invoiceNo, double amount, String words) {

        Transactions trans = null;
        try {

            BigDecimal amount_val = BigDecimal.valueOf(amount);
            List<Transactions> transList = em.createQuery("SELECT trans FROM Transactions trans " + "INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "INNER JOIN FETCH mpc.merchants m " + "WHERE trans.incTransidmerchant=:invNo and trans.transactionsStatus=:status and trans.incAmount=:amount " + "and trans.transactionsState=:state").setParameter("invNo", invoiceNo).setParameter("status", OneCheckoutTransactionStatus.SUCCESS.value()).setParameter("amount", amount_val).setParameter("state", OneCheckoutTransactionState.DONE.value()).getResultList();

            if (transList != null) {

                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : %d rows", transList.size());
                for (Transactions t : transList) {

                    //ref = merchant.getMid() + merchant.getPassword() + invoiceNo;
                    String strHash = t.getMerchantPaymentChannel().getMerchantPaymentChannelUid() + t.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + invoiceNo;
                    String hash = HashWithSHA1.doHashing(strHash, null, null);

                    if (hash.equalsIgnoreCase(words)) {
                        trans = t;
                        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : Match MallID=%d %s=%s", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
                    } else {
                        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : not Match MallID=%d %s=%s", t.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
                    }
                }

            } else {
                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy : 0 rows");
                return trans;
            }

            return trans;

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSDataTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    /*
     public Transactions getEDSUpdateTransactionBy(String invoiceNo, PaymentChannel pchannel, String words) {

     Transactions trans = null;
     try {

     //eduStatus
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : query by invoiceNo=%s and paymentChannel=%s ", invoiceNo, pchannel.getPaymentChannelId());
     List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans "
     + "INNER JOIN FETCH trans.merchantPaymentChannel mpc "
     + "INNER JOIN FETCH mpc.merchants m "
     + "WHERE trans.incTransidmerchant=:invNo "
     + "and trans.eduStatus=:edustatus "
     + "and trans.transactionsState=:state and mpc.paymentChannel=:pc").setParameter("invNo", invoiceNo) //     .setParameter("status", OneCheckoutTransactionStatus.SUCCESS.value())
     .setParameter("edustatus", OneCheckoutDFSStatus.REVIEW.value()).setParameter("pc", pchannel).setParameter("state", OneCheckoutTransactionState.DONE.value()).getResultList();

     if (transList != null) {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : %d rows", transList.size());
     for (Transactions t : transList) {

     //ref = merchant.getMid() + merchant.getPassword() + invoiceNo;
     String strHash = t.getMerchantPaymentChannel().getMerchantPaymentChannelUid() + t.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + invoiceNo;
     String hash = HashWithSHA1.doHashing(strHash, null, null);

     if (hash.equalsIgnoreCase(words)) {
     trans = t;
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : Match MallID=%d %s=%s", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
     } else {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : not Match MallID=%d %s=%s", t.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
     }
     }
     return trans;
     } else {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : 0 rows");
     return trans;
     }


     } catch (Throwable th) {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy   Exception : %s", th.getMessage());
     //            th.printStackTrace();
     return trans;

     }

     }
     */
    /*
     public Transactions getEDSUpdateTransactionBy(String invoiceNo, String words) {

     Transactions trans = null;
     try {

     //eduStatus
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : query by invoiceNo=%s ", invoiceNo);
     List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans "
     + "INNER JOIN FETCH trans.merchantPaymentChannel mpc "
     + "INNER JOIN FETCH mpc.merchants m "
     + "WHERE trans.incTransidmerchant=:invNo "
     + "and trans.eduStatus=:edustatus "
     + "and trans.transactionsState=:state").setParameter("invNo", invoiceNo) //     .setParameter("status", OneCheckoutTransactionStatus.SUCCESS.value())
     .setParameter("edustatus", OneCheckoutDFSStatus.REVIEW.value()).setParameter("state", OneCheckoutTransactionState.DONE.value()).getResultList();

     if (transList != null) {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : %d rows", transList.size());
     for (Transactions t : transList) {

     //ref = merchant.getMid() + merchant.getPassword() + invoiceNo;
     String strHash = t.getMerchantPaymentChannel().getMerchantPaymentChannelUid() + t.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + invoiceNo;
     String hash = HashWithSHA1.doHashing(strHash, null, null);

     if (hash.equalsIgnoreCase(words)) {
     trans = t;
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : Match MallID=%d %s=%s", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
     } else {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : not Match MallID=%d %s=%s", t.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
     }
     }
     return trans;
     } else {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : 0 rows");
     return trans;
     }


     } catch (Throwable th) {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy   Exception : %s", th.getMessage());
     //            th.printStackTrace();
     return trans;

     }

     }
     */
    public Transactions getEDSUpdateTransactionBy(String invoiceNo, String words, OneCheckoutDFSStatus status) {
        Transactions trans = null;

        try {

            if (status == null) {
                status = OneCheckoutDFSStatus.NA;
            }

            //eduStatus
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : query by invoiceNo=%s ", invoiceNo);

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy :Edu Param Status =%s ", status);

            List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans "
                    + "INNER JOIN FETCH trans.merchantPaymentChannel mpc "
                    + "INNER JOIN FETCH mpc.merchants m "
                    + "WHERE trans.incTransidmerchant=:invNo "
                    + "and trans.transactionsStatus='S' "
                    //+ "and (trans.eduStatus=:edustatus or trans.eduStatus=:eduparamstatus) "
                    + "and trans.transactionsState=:state").setParameter("invNo", invoiceNo)
                    //.setParameter("eduparamstatus", status.value())
                    //.setParameter("edustatus", OneCheckoutDFSStatus.REVIEW.value())
                    .setParameter("state", OneCheckoutTransactionState.DONE.value()).getResultList();

            if (transList != null) {
                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : %d rows", transList.size());
                for (Transactions t : transList) {

                    //ref = merchant.getMid() + merchant.getPassword() + invoiceNo;
                    String strHash = t.getMerchantPaymentChannel().getMerchantPaymentChannelUid() + t.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + invoiceNo;
                    String hash = HashWithSHA1.doHashing(strHash, null, null);

                    if (hash.equalsIgnoreCase(words)) {
                        trans = t;
                        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : Match MallID=%d %s=%s", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
                    } else {
                        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : not Match MallID=%d %s=%s", t.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
                    }
                }
                return trans;
            } else {
                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : 0 rows");
                return trans;
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    /*
     public Transactions getEDSUpdateTransactionBy(String invoiceNo, PaymentChannel pchannel, String words, OneCheckoutDFSStatus status) {

     Transactions trans = null;
     try {

     if (status == null) {
     status = OneCheckoutDFSStatus.NA;
     }

     //eduStatus
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : query by invoiceNo=%s and paymentChannel=%s ", invoiceNo, pchannel.getPaymentChannelId());

     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy :Edu Param Status =%s ", status);

     List<Transactions> transList = (List<Transactions>) em.createQuery("SELECT trans FROM Transactions trans "
     + "INNER JOIN FETCH trans.merchantPaymentChannel mpc "
     + "INNER JOIN FETCH mpc.merchants m "
     + "WHERE trans.incTransidmerchant=:invNo "
     + "and (trans.eduStatus=:edustatus or trans.eduStatus=:eduparamstatus) "
     + "and trans.transactionsState=:state and mpc.paymentChannel=:pc").setParameter("invNo", invoiceNo)
     .setParameter("eduparamstatus", status.value())
     .setParameter("edustatus", OneCheckoutDFSStatus.REVIEW.value())
     .setParameter("pc", pchannel).setParameter("state", OneCheckoutTransactionState.DONE.value())
     .getResultList();

     if (transList != null) {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : %d rows", transList.size());
     for (Transactions t : transList) {

     //ref = merchant.getMid() + merchant.getPassword() + invoiceNo;
     String strHash = t.getMerchantPaymentChannel().getMerchantPaymentChannelUid() + t.getMerchantPaymentChannel().getMerchantPaymentChannelHash() + invoiceNo;
     String hash = HashWithSHA1.doHashing(strHash, null, null);

     if (hash.equalsIgnoreCase(words)) {
     trans = t;
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : Match MallID=%d %s=%s", trans.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
     } else {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : not Match MallID=%d %s=%s", t.getMerchantPaymentChannel().getMerchants().getMerchantCode(), words, hash);
     }
     }
     return trans;
     } else {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy : 0 rows");
     return trans;
     }


     } catch (Throwable th) {
     OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getEDSUpdateTransactionBy  Exception : %s", th.getMessage());
     //            th.printStackTrace();
     return trans;

     }

     }
     */
    public List<MerchantPaymentChannel> getListMerchantPaymentChannel(Merchants merchant) {
        List<MerchantPaymentChannel> channels = null;

        try {

            Criteria criteria = getSession().createCriteria(MerchantPaymentChannel.class);

            criteria.createAlias("paymentChannel", "payChan");

            criteria.add(Restrictions.eq("merchants", merchant));
            criteria.add(Restrictions.eq("merchantPaymentChannelStatus", true));

            criteria.addOrder(Order.asc("payChan.paymentChannelId"));

            channels = criteria.list();
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getListMerchantPaymentChannel  Exception : %s", th.getMessage());
//            th.printStackTrace();
        }

        return channels;
    }

    public Transactions getInquiryVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel) {

        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            //criteria.add(Restrictions.eq("m.merchantCode", Integer.parseInt(merchantCode)));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.or(Restrictions.isNull("dokuResponseCode"), Restrictions.not(Restrictions.ilike("dokuResponseCode", OneCheckoutErrorMessage.INIT_VA.value()))));
            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getInquiryVATransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getInitVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel) {

        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            //criteria.add(Restrictions.ilike("accountId", paycode));
            criteria.add(Restrictions.eq("accountId", paycode));
            //criteria.add(Restrictions.eq("m.merchantCode", Integer.parseInt(merchantCode)));
            //   criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.eq("dokuResponseCode", OneCheckoutErrorMessage.INIT_VA.value()));

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getInquiryVATransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getVirtualTransactionBy(String paycode, String merchantCode, PaymentChannel pchannel) {

        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            criteria.add(Restrictions.or(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()), Restrictions.eq("transactionsState", OneCheckoutTransactionState.SETTLEMENT.value())));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getVirtualTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Merchants getMerchantBy(int mallId) {
        OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantByMallId : mallId=%d", mallId);

        Merchants m = null;
        try {

            String query = "SELECT m FROM Merchants m WHERE m.merchantCode=:mCode";

            m = (Merchants) em.createQuery(query).setParameter("mCode", mallId).getSingleResult();

            return m;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return m;

        }

    }

    public Merchants getMerchantBy(String accountId) {

        Merchants m = null;
        try {
            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.ilike("accountId", accountId));

            criteria.setMaxResults(1);

            Transactions trans = (Transactions) criteria.uniqueResult();

            m = trans.getMerchantPaymentChannel().getMerchants();

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
        }

        return m;
    }

    public Transactions getPermataVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel, String actionStatus) {
        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            //     criteria.add(Restrictions.eq("incAmount", amount));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            OneCheckoutLogger.log(": : : : : PAYCODE " + paycode);
            OneCheckoutLogger.log(": : : : : CHANNEL " + pchannel);

            if (actionStatus != null && actionStatus.equalsIgnoreCase("REVERSE")) {
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
            } else {
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            }

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getPermataVATransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getPermataVATransactionByWithAmount(String paycode, BigDecimal amount, String merchantCode, PaymentChannel pchannel, String actionStatus) {
        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            criteria.add(Restrictions.eq("incAmount", amount));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            OneCheckoutLogger.log(": : : : : PAYCODE " + paycode);
            OneCheckoutLogger.log(": : : : : AMOUT   " + amount);
            OneCheckoutLogger.log(": : : : : CHANNEL " + pchannel);

            if (actionStatus != null && actionStatus.equalsIgnoreCase("REVERSE")) {
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
            } else {
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            }

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getPermataVATransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getPermataVATransactionBy(String paycode, String merchantCode, String invoiceNo, PaymentChannel pchannel, String actionStatus) {
        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            //     criteria.add(Restrictions.eq("incAmount", amount));
            criteria.add(Restrictions.eq("mpc.paymentChannelCode", Integer.parseInt(merchantCode)));
            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            if (actionStatus != null && (actionStatus.equalsIgnoreCase("REVERSE") || actionStatus.equals("REVERSAL"))) {
                OneCheckoutLogger.log(": : : : : STEP    : REVERSE");
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
            } else {
                OneCheckoutLogger.log(": : : : : STEP    : PAYMENT");
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            }

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getPermataVATransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getPermataVATransactionByWithAmount(String paycode, BigDecimal amount, String merchantCode, String invoiceNo, PaymentChannel pchannel, String actionStatus) {
        Transactions trans = null;
        try {

            OneCheckoutLogger.log("paycode=%s , amount=%s , merchantCode=%s , invoiceNo=%s , pchannel=%s , actionStatus=%s ", paycode, OneCheckoutVerifyFormatData.sdf.format(amount.doubleValue()), merchantCode, invoiceNo, pchannel, actionStatus);
            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            criteria.add(Restrictions.eq("incAmount", amount));
            criteria.add(Restrictions.eq("mpc.paymentChannelCode", Integer.parseInt(merchantCode)));
            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            if (actionStatus != null && actionStatus.equalsIgnoreCase("REVERSE")) {
                OneCheckoutLogger.log(": : : : : STEP    : REVERSE");
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
            } else {
                OneCheckoutLogger.log(": : : : : STEP    : PAYMENT");
                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            }

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getPermataVATransactionBy  Exception : %s", th.getMessage());
            // th.printStackTrace();
            return trans;

        }
    }

//    public void updateTraceNoByMerchant(Merchants m) {
//
//        try {
//
//            em.merge(m);
//
//        } catch (Throwable th) {
//            th.printStackTrace();
//        }
//
//    }
    public String generatePaymentCodeByMerchant(Merchants m, OneCheckoutPaymentChannel pChannel, String invoiceNo, String words, Boolean openAmount) {

        try {

            Transactions trans = this.getVATransanctionByInvoiceId(invoiceNo, words);
            if (trans != null && trans.getAccountId() != null && !trans.getAccountId().isEmpty()) {
                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.generatePaymentCodeByMerchant  USE LAST PaymentCode : %s", trans.getAccountId());
                return trans.getAccountId();
            }
            MerchantPaymentChannel mpc = this.getMerchantPaymentChannel(m, pChannel);

            Integer VANumber = (Integer) em.createNativeQuery("select * from vanextvalue(:merchantId )")
                    .setParameter("merchantId", m.getMerchantIdx()).getSingleResult();
            String paycode = "";
            String numberFormat = OneCheckoutVerifyFormatData.traceNo.format(VANumber);

            if (pChannel == OneCheckoutPaymentChannel.PermataVAFull
                    || pChannel == OneCheckoutPaymentChannel.MandiriSOAFull
                    || pChannel == OneCheckoutPaymentChannel.MandiriVAFull) {

                paycode = mpc.getVAInitNumber() + numberFormat;

                if (paycode.length() > 16) {
                    int sisa = paycode.length() - 16;
                    numberFormat = numberFormat.substring(sisa, numberFormat.length());
                    paycode = mpc.getVAInitNumber() + numberFormat;
                }
//            } else if (pChannel == OneCheckoutPaymentChannel.Indomaret || pChannel == OneCheckoutPaymentChannel.CIMBVA || pChannel == OneCheckoutPaymentChannel.DanamonVA || pChannel == OneCheckoutPaymentChannel.BRIVA) {
            } else if (pChannel == OneCheckoutPaymentChannel.BRIVA || pChannel == OneCheckoutPaymentChannel.AlfaMVA || pChannel == OneCheckoutPaymentChannel.DanamonVA || pChannel == OneCheckoutPaymentChannel.Indomaret || pChannel == OneCheckoutPaymentChannel.PermataMVA
                    || pChannel == OneCheckoutPaymentChannel.CIMBVA || pChannel == OneCheckoutPaymentChannel.BNIVA) {

                //MVAOld(start)
//                String vaFullLiteChannel = config.getString("onecheckout.paymentchannel.vafull.valite");
//                OneCheckoutLogger.log("onecheckout.paymentchannel.vafull.valite := " + vaFullLiteChannel);
//                String[] vaFullLiteChannels = vaFullLiteChannel.split(" ");
//                List<String> channels = new ArrayList<String>();
//                for (String channel : vaFullLiteChannels) {
//                    channels.add(channel);
//                }
//
//                Criteria criteria = getSession().createCriteria(MerchantPaymentChannel.class)
//                        .add(Restrictions.eq("merchants", m))
//                        .add(Restrictions.in("paymentChannel.paymentChannelId", channels));
//                List<MerchantPaymentChannel> merchantsPaymentChannels = criteria.list();
//
//                String vaLiteChannel = config.getString("onecheckout.paymentchannel.valite");
//                OneCheckoutLogger.log("onecheckout.paymentchannel.valite := " + vaLiteChannel);
//                String mallId = "";
//                String paymentChannelCode = "";
//                MerchantPaymentChannel tempChannel = null;
//                boolean resultCheck = false;
//                for (MerchantPaymentChannel channel : merchantsPaymentChannels) {
//                    mallId = channel.getPaymentChannel().getPaymentChannelId() + "";
//                    tempChannel = channel;
//                    if (vaLiteChannel.indexOf(mallId) != -1) {
//                        resultCheck = true;
//                        paymentChannelCode = channel.getPaymentChannelCode() + "";
//                        OneCheckoutLogger.log(":: GENERATE PAYCODE USE VA LITE ::");
//                        break;
//                    }
//                }
//
//                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.generatePaymentCodeByMerchant MALLID from merchant paymentchannel: %s", mallId);
//                if (resultCheck) {
//                    OneCheckoutLogger.log("Using Channel ID : " + tempChannel.getMerchantPaymentChannelId());
//                    numberFormat = OneCheckoutVerifyFormatData.traceNoLite.format(VANumber);
//                    if (paymentChannelCode.length() <= 3) {
//                        paymentChannelCode = OneCheckoutVerifyFormatData.threeDigitMalldId.format(Integer.parseInt(paymentChannelCode));
//                    }
//
//                    OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.generatePaymentCodeByMerchant MALLID : %s", paymentChannelCode);
//                    paycode = paymentChannelCode + numberFormat;
//                    if (paycode.length() > 11) {
//                        int sisa = paycode.length() - 11;
//                        numberFormat = numberFormat.substring(sisa, numberFormat.length());
//                        paycode = paymentChannelCode + numberFormat;
//                    }
//                } else {
//                    numberFormat = OneCheckoutVerifyFormatData.traceNo.format(VANumber);
//                    paycode = numberFormat;
//                }
                //mvaOld(end)
                //newMva(start)
                String vaInitNumber = null;
                if (openAmount) {
                    vaInitNumber = mpc.getVAInitNumberOpen();
                    if (vaInitNumber != null && !vaInitNumber.equals("")) {
                        vaInitNumber = mpc.getVAInitNumberOpen();
                    } else {
                        vaInitNumber = mpc.getVAInitNumber();
                    }

                } else {
                    vaInitNumber = mpc.getVAInitNumber();
                }

                try {
                    vaInitNumber = StringUtils.rightPad(vaInitNumber, 8, "0");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                numberFormat = OneCheckoutVerifyFormatData.traceNoLite.format(VANumber);
                paycode = vaInitNumber + numberFormat;
                //newMva(end)

            } else {

                String mallId = String.valueOf(mpc.getPaymentChannelCode());
                if (mallId.length() <= 3) {
                    mallId = OneCheckoutVerifyFormatData.threeDigitMalldId.format(mpc.getPaymentChannelCode());
                }

                paycode = mpc.getVAInitNumber() + mallId + numberFormat;
                if (paycode.length() > 16) {
                    int sisa = paycode.length() - 16;
                    numberFormat = numberFormat.substring(sisa, numberFormat.length());
                    paycode = mpc.getVAInitNumber() + mallId + numberFormat;
                }
            }
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.generatePaymentCodeByMerchant  create new PaymentCode : %s", paycode);
            return paycode;

        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

//    public Transactions getCheckStatusTransactionByCC(Merchants m, String invoiceNo, String sessionId) {
//        Transactions trans = null;
//        try {
//
//            Criteria criteria = getSession().createCriteria(Transactions.class);
//
//            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
//            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);
//            criteria.createAlias("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);
//            criteria.createAlias("transactionsDataCardholders", "cardHolder", Criteria.LEFT_JOIN);
//
////            if (type == OneCheckoutTransactionState.INCOMING) {
////
////                criteria.add(Restrictions.or(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()), Restrictions.eq("transactionsState", OneCheckoutTransactionState.NOTIFYING.value())));
////
////            } else {
////
////                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
////
////            }
//
//            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
//            criteria.add(Restrictions.eq("mpc.merchants", m));
//            // criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
//            criteria.add(Restrictions.eq("incSessionid", sessionId));
//
//            criteria.addOrder(Order.desc("transactionsDatetime"));
//
//            criteria.setMaxResults(1);
//
//            trans = (Transactions) criteria.uniqueResult();
//
//            return trans;
//        } catch (Throwable th) {
//            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCheckStatusTransactionBy  Exception : %s", th.getMessage());
////            th.printStackTrace();
//            return trans;
//
//        }
//    }
    public TransactionsDataCardholder getCH(Transactions trans) {

        TransactionsDataCardholder ch = null;
        try {

            Criteria criteria = getSession().createCriteria(TransactionsDataCardholder.class);

            criteria.add(Restrictions.eq("transactions", trans));

            criteria.setMaxResults(1);

            ch = (TransactionsDataCardholder) criteria.uniqueResult();

        } catch (Throwable th) {

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCH  Exception : %s", th.getMessage());

        }

        return ch;

    }

    public TransactionsDataAirlines getPassengerData(Transactions trans) {

        TransactionsDataAirlines passenger = null;

        try {

            Criteria criteria = getSession().createCriteria(TransactionsDataAirlines.class);

            criteria.add(Restrictions.eq("transactions", trans));

            criteria.setMaxResults(1);

            List<TransactionsDataAirlines> list = criteria.list();

            if (list != null && list.size() > 0) {
                passenger = list.get(0);
            }

            //   passenger = (TransactionsDataAirlines) criteria.list();//.uniqueResult();             
        } catch (Throwable th) {

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getPassengerData  Exception : %s", th.getMessage());

        }
        return passenger;
    }

    public Transactions getTransactionByInvoiceSessionAmount(String invoiceNo, String sessionId, BigDecimal amount) {

        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("incSessionid", sessionId));
            criteria.add(Restrictions.eq("incAmount", amount));
            //        criteria.add(Restrictions.eq("pc.paymentChannelId", OneCheckoutPaymentChannel.KlikBCA.value()));

            criteria.addOrder(Order.desc("transactionsDatetime"));
            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionByInvoiceSessionAmount  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getCheckStatusTransactionByInvoiceSession(Merchants m, String invoiceNo, String sessionId) {
        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);
//            criteria.createAlias("transactionsDataCardholders", "cardHolder", Criteria.LEFT_JOIN);

//            if (type == OneCheckoutTransactionState.INCOMING) {
//
//                criteria.add(Restrictions.or(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()), Restrictions.eq("transactionsState", OneCheckoutTransactionState.NOTIFYING.value())));
//
//            } else {
//
//                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
//
//            }
            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("mpc.merchants", m));
            // criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.eq("incSessionid", sessionId));

            criteria.addOrder(Order.desc("transactionsDatetime"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCheckStatusTransactionByInvoiceSession  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getCheckStatusTransactionBy(String invoiceNo, String sessionId, PaymentChannel pchannel, OneCheckoutTransactionState type) {
        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);
            //        criteria.createAlias("transactionsDataCardholders", "cardHolder", Criteria.LEFT_JOIN);

            if (type == OneCheckoutTransactionState.INCOMING) {

                criteria.add(Restrictions.or(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()), Restrictions.eq("transactionsState", OneCheckoutTransactionState.NOTIFYING.value())));

            } else {

                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));

            }

            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.eq("incSessionid", sessionId));

            criteria.addOrder(Order.desc("transactionsDatetime"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCheckStatusTransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getCheckVAStatusTransactionBy(String payCode, String sessionId, PaymentChannel pchannel, OneCheckoutTransactionState type) {
        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            if (type == OneCheckoutTransactionState.INCOMING) {

                criteria.add(Restrictions.or(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()), Restrictions.eq("transactionsState", OneCheckoutTransactionState.NOTIFYING.value())));

            } else {

                criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));

            }

            //criteria.add(Restrictions.ilike("accountId", payCode));
            criteria.add(Restrictions.eq("incTransidmerchant", payCode));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));
            criteria.add(Restrictions.eq("incSessionid", sessionId));

            criteria.addOrder(Order.desc("transactionsDatetime"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCheckVAStatusTransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Currency getCurrencyByCode(String currencyCode) {
        Currency currency = null;

        try {
            Criteria criteria = getSession().createCriteria(Currency.class);

            criteria.add(Restrictions.eq("currencyCode", currencyCode));

            currency = (Currency) criteria.uniqueResult();

            if (currency == null) {

                throw new Exception("currency code Not Found");
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCurrencyByCode   Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return currency;
    }

    public Currency getCurrencyByAlphaCode(String currencyCode) {
        Currency currency = null;

        try {
            Criteria criteria = getSession().createCriteria(Currency.class);

            criteria.add(Restrictions.eq("alpha3Code", currencyCode));

            currency = (Currency) criteria.uniqueResult();

            if (currency == null) {

                throw new Exception("currency code Not Found");
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCurrencyByAlphaCode  Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return currency;
    }

//    public Rates getRates(String purchaseCurrencyNum, String currencyRatesNum) {
//        Rates rate = null;
//
//        try {
//            Criteria criteria = getSession().createCriteria(Rates.class);
//
//            criteria.add(Restrictions.eq("sellCurrencyNum", purchaseCurrencyNum));
//            criteria.add(Restrictions.eq("buyCurrencyNum", currencyRatesNum));
//            criteria.add(Restrictions.ge("expiryDate", new Date()));
//            criteria.add(Restrictions.eq("status", 'N'));
//           // criteria.add(Restrictions.)
//            
//            rate = (Rates) criteria.uniqueResult();
//
//            if (rate == null) {
//
//                throw new Exception("Rates code Not Found");
//            }
//
//        } catch (Throwable th) {
//            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRates  Exception : %s", th.getMessage());
////            th.printStackTrace();
//
//        }
//
//        return rate;
//    }    
    public Transactions refreshAmountBaseNewRate(Transactions trans) {

        try {

            Merchants m = trans.getMerchantPaymentChannel().getMerchants();//.getMerchant();
            if (m.getMerchantFxrateFeature() != null && !m.getMerchantFxrateFeature().isEmpty()) {

                Rates rate = this.getRates(m.getMerchantFxrateFeature(), trans.getIncPurchasecurrency());

                trans.setRates(rate);

                double finalRate = rate.getFinalRate().doubleValue();
                double realAmount = finalRate * trans.getIncPurchaseamount().doubleValue();
                //::: rounding up amount..                
                double amountRoundUp = Math.ceil(realAmount);
                //::: set to entity...
                trans.setIncAmount(BigDecimal.valueOf(amountRoundUp));
                trans.setConvertedAmount(BigDecimal.valueOf(realAmount));
                trans.setIncCurrency(m.getMerchantFxrateFeature());

                OneCheckoutLogger.log("realAmount       [%s]", realAmount);
                OneCheckoutLogger.log("amountRoundUp    [%s]", amountRoundUp);

            }
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.refreshAmountBaseNewRate  Exception : %s", th.getMessage());
            return null;
        }

        return trans;
    }

    public Transactions getPermataVATransactionBy(String payCode, double amount) {
        Transactions trans = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.add(Restrictions.eq("accountId", payCode));
            criteria.add(Restrictions.eq("incAmount", amount));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getPermataVATransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }
    }

    public Transactions getVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel) {

        Transactions trans = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            criteria.add(Restrictions.eq("mpc.paymentChannelCode", Integer.parseInt(merchantCode)));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getVATransactionBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    public Transactions getVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel, OneCheckoutTransactionState state) {

        Transactions trans = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createAlias("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createAlias("mpc.merchants", "m", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("accountId", paycode));
            criteria.add(Restrictions.eq("mpc.paymentChannelCode", Integer.parseInt(merchantCode)));
            criteria.add(Restrictions.eq("transactionsState", state.value()));
            criteria.add(Restrictions.eq("mpc.paymentChannel", pchannel));

            criteria.addOrder(Order.desc("transactionsId"));

            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getVATransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    public MerchantPaymentChannel getMerchantPaymentChannel(String paymentChannelCode, PaymentChannel paymentChannel) {
        MerchantPaymentChannel mpc = null;

        try {
            Criteria criteria = getSession().createCriteria(MerchantPaymentChannel.class);

            criteria.createCriteria("merchants", "merchants", Criteria.INNER_JOIN);
            criteria.createCriteria("paymentChannel", "paymentChannel", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("paymentChannel", paymentChannel));
            criteria.add(Restrictions.eq("paymentChannelCode", Integer.parseInt(paymentChannelCode)));
            criteria.add(Restrictions.eq("merchantPaymentChannelStatus", true));

            mpc = (MerchantPaymentChannel) criteria.uniqueResult();
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantPaymentChannel  Exception : %s", th.getMessage());
//            th.printStackTrace();
        }

        return mpc;
    }

    public MerchantPaymentChannel getMerchantPaymentChannel(String paymentChannelCode, String companyCode, String userAccount) {
        MerchantPaymentChannel mpc = null;

        try {

            Criteria criteria = getSession().createCriteria(MerchantPaymentChannel.class);

            criteria.createCriteria("merchants", "merchants", Criteria.LEFT_JOIN);
            criteria.createCriteria("paymentChannel", "paymentChannel", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("paymentChannelCode", Integer.parseInt(paymentChannelCode)));
            criteria.add(Restrictions.ilike("VAInitNumber", companyCode, MatchMode.ANYWHERE));
            criteria.add(Restrictions.eq("merchantPaymentChannelStatus", true));

            mpc = (MerchantPaymentChannel) criteria.uniqueResult();
            //paymentChannelCode : 112, companyCode : 88888, userAccount : 00003363
            if (mpc != null) {
                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantPaymentChannel mpc!=null");
            } else {
                OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantPaymentChannel mpc==null");
            }

        } catch (Throwable th) {

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantPaymentChannel   Exception : %s", th.getMessage());

        }

        return mpc;
    }

    @Override
    public PaypalResponseCode getPaypalDokuResponseCode(String errorCode, String shortMessage) {
        PaypalResponseCode responseCode = null;
        try {
            Criteria criteria = getSession().createCriteria(PaypalResponseCode.class);

            criteria.add(Restrictions.ilike("paypalResponseCode", errorCode));
            criteria.add(Restrictions.ilike("shortMessage", errorCode));

            try {
                responseCode = (PaypalResponseCode) criteria.uniqueResult();

            } catch (Throwable t) {
                OneCheckoutLogger.log(": : : : : ERROR MESSAGE : " + t.getMessage());

                List<PaypalResponseCode> list = criteria.list();

                if (list != null && list.size() > 0) {
                    responseCode = list.get(0);
                }
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getPaypalDokuResponseCode   Exception : %s", th.getMessage());
//            th.printStackTrace();
        }

        return responseCode;
    }

    public List<Country> getListCountry() {
        List<Country> countrys = null;

        try {
            Criteria criteria = getSession().createCriteria(Country.class);

            criteria.addOrder(Order.asc("name"));

            countrys = criteria.list();
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getListCountry  Exception : %s", th.getMessage());
//            th.printStackTrace();
        }

        return countrys;
    }

    public MerchantPaymentChannel getMerchantPaymentChannel(String invoiceNo, String sessionId, BigDecimal amount, OneCheckoutTransactionState state) {
        MerchantPaymentChannel mpc = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "merchantPaymentChannel", Criteria.INNER_JOIN);
            criteria.createCriteria("merchantPaymentChannel.paymentChannel", "paymentChannel", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("incSessionid", sessionId));

            if (amount != null) {
                criteria.add(Restrictions.eq("incAmount", amount));
            }

            if (state != null) {
                criteria.add(Restrictions.eq("transactionsState", state.value()));
            }

            criteria.setMaxResults(1);

            Transactions trans = (Transactions) criteria.uniqueResult();

            if (trans != null && trans.getMerchantPaymentChannel() != null) {
                mpc = trans.getMerchantPaymentChannel();
            }

        } catch (Throwable th) {

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getMerchantPaymentChannel   Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return mpc;
    }

    public Transactions getCyberSourceTransactionsBy(String requestID, String invoiceNo) {

        Transactions trans = null;
        try {

            trans = (Transactions) em.createQuery("SELECT trans FROM Transactions trans " + "INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "INNER JOIN FETCH mpc.merchant m " + "WHERE trans.incTransidmerchant=:invNo and trans.verifyStatus=:verstatus " + "and trans.verifyId=:verid").setParameter("verid", requestID).setParameter("invNo", invoiceNo).setParameter("verstatus", OneCheckoutDFSStatus.REVIEW.value()).getSingleResult();

            return trans;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCyberSourceTransactionsBy  Exception : %s", th.getMessage());
//            th.printStackTrace();
            return trans;

        }

    }

    public String getCountryById(String id) {

        try {

            Criteria criteria = getSession().createCriteria(Country.class);

            criteria.add(Restrictions.ilike("id", id.toUpperCase()));

            Country country = (Country) criteria.uniqueResult();

            if (country != null) {
                id = country.getNumericCode();
                country = null;
            } else {
                id = null;
            }

        } catch (Throwable th) {
            id = null;
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getCountryById   Exception : %s", th.getMessage());
//            th.printStackTrace();
        }

        return id;
    }

    public Transactions getTransactionByXID(String XID) {
        Transactions trans = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("inc3dSecureStatus", XID));
            criteria.addOrder(Order.desc("transactionsDatetime"));
            criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionWithoutState  Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return trans;
    }

    public Transactions getTransactionByID(long ID) {
        Transactions trans = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);

            criteria.add(Restrictions.eq("transactionsId", ID));
            //   criteria.addOrder(Order.desc("transactionsDatetime"));
            //    criteria.setMaxResults(1);

            trans = (Transactions) criteria.uniqueResult();

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getRedirectTransactionWithoutState  Exception : %s", th.getMessage());
//            th.printStackTrace();

        }

        return trans;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Transactions saveTokenizationTransactions(RequestHelper requestHelper) {
        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTokenizationTransactions : Begin Persisting Transaction");
        Transactions trans = null;
        try {
            trans = new Transactions();
            trans.setTransactionsId(getNextVal(SeqTransactionIdNextval.class).longValue());
            trans.setIncMallid(requestHelper.getTokenizationHelper().getMallId());
            trans.setIncChainmerchant(requestHelper.getTokenizationHelper().getChainMallId());

            trans.setIncAmount(BigDecimal.valueOf(requestHelper.getTokenizationHelper().getAmount()));
            trans.setIncPurchaseamount(BigDecimal.valueOf(requestHelper.getTokenizationHelper().getPurchaseAmount()));

            trans.setIncTransidmerchant(requestHelper.getTokenizationHelper().getInvoiceNumber());
            trans.setIncWords(requestHelper.getPaymentParam().get("WORDS"));
            if (!"".equals(requestHelper.getTokenizationHelper().getRequestDateTime())) {
                trans.setIncRequestdatetime(OneCheckoutVerifyFormatData.datetimeFormat.parse(requestHelper.getTokenizationHelper().getRequestDateTime()));
            }
            trans.setIncCurrency(requestHelper.getTokenizationHelper().getCurrency());
            trans.setIncPurchasecurrency(requestHelper.getTokenizationHelper().getPurchaseCurrency());
            trans.setIncSessionid(requestHelper.getTokenizationHelper().getSessionId());
            trans.setIncEmail(requestHelper.getTokenizationHelper().getCustomerEmail());
            trans.setIncName(requestHelper.getTokenizationHelper().getCustomerName());
            trans.setSystemMessage(requestHelper.getResultHelper().getSystemMessage());
            trans.setTransactionsDatetime(new Date());
            if (requestHelper.getCreditCardHelper().getAdditionalData() != null) {
                trans.setIncAdditionalInformation(requestHelper.getCreditCardHelper().getAdditionalData().toString());
            }
            trans.setIncPaymentchannel(requestHelper.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());
            trans.setMerchantPaymentChannel(requestHelper.getMerchantPaymentChannel());
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            trans.setAccountId(base.maskingString(requestHelper.getCreditCardHelper().getCardNumber(), "PAN"));

            Set<TransactionsDataCardholder> chs = new HashSet<TransactionsDataCardholder>();
            TransactionsDataCardholder ch = new TransactionsDataCardholder();
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTokenizationTransactions : TransactionsDataCardholder");
            ch.setTransactionsId(getNextVal(SeqTransactionsDataCardholderIdNextval.class).longValue());
            ch.setIncAddress(requestHelper.getCreditCardHelper().getCcAddress());
            //ch.setIncBirthdate();
            ch.setIncCcName(requestHelper.getCreditCardHelper().getCcName() == null ? requestHelper.getTokenizationHelper().getCustomerName() : requestHelper.getCreditCardHelper().getCcName());
            ch.setIncCity(requestHelper.getCreditCardHelper().getCcCity());
            ch.setIncCountry(requestHelper.getCreditCardHelper().getCcCountry());
            ch.setIncHomephone(requestHelper.getCreditCardHelper().getCcHomePhone());
            ch.setIncMobilephone(requestHelper.getCreditCardHelper().getCcMobilePhone());
            ch.setIncState(requestHelper.getCreditCardHelper().getCcRegion());
            ch.setIncWorkphone(requestHelper.getCreditCardHelper().getCcWorkPhone());
            ch.setIncZipcode(requestHelper.getCreditCardHelper().getCcZipCode());
            ch.setTransactions(trans);
            chs.add(ch);
            trans.setTransactionsDataCardholders(chs);
            trans.setTokenId(requestHelper.getTokenizationHelper().getTokenId());
            trans.setIncCustomerId(requestHelper.getTokenizationHelper().getCustomerId());
            trans.setTransactionsState(OneCheckoutTransactionState.INCOMING.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
            em.persist(trans);
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTokenizationTransactions : Done Persisting Transaction :" + trans.getTransactionsId());
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.saveTokenizationTransactions Exception : %s", th.getMessage());
        }
        return trans;
    }

    public Transactions saveRecurTransactions(RequestHelper requestHelper) {
        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveRecurTransactions : Begin Persisting Transaction");
        Transactions trans = null;
        try {
            trans = new Transactions();
            trans.setTransactionsId(getNextVal(SeqTransactionIdNextval.class).longValue());
            trans.setIncMallid(requestHelper.getRecurHelper().getMallId());
            trans.setIncChainmerchant(requestHelper.getRecurHelper().getChainMallId());
            trans.setIncAmount(BigDecimal.valueOf(requestHelper.getRecurHelper().getBillingAmount()));
            trans.setIncPurchaseamount(BigDecimal.valueOf(requestHelper.getRecurHelper().getBillingPurchaseAmount()));
            trans.setIncTransidmerchant(requestHelper.getRecurHelper().getInvoiceNumber());
            trans.setIncWords(requestHelper.getPaymentParam().get("WORDS"));
            if (!"".equals(requestHelper.getRecurHelper().getRequestDateTime())) {
                trans.setIncRequestdatetime(OneCheckoutVerifyFormatData.datetimeFormat.parse(requestHelper.getRecurHelper().getRequestDateTime()));
            }
            trans.setIncCurrency(requestHelper.getRecurHelper().getBillingCurrency());
            trans.setIncPurchasecurrency(requestHelper.getRecurHelper().getBillingPurchaseCurrency());
            trans.setIncSessionid(requestHelper.getRecurHelper().getSessionId());
            trans.setIncEmail(requestHelper.getRecurHelper().getCustomerEmail());
            trans.setIncName(requestHelper.getRecurHelper().getCustomerName());
            trans.setSystemMessage(requestHelper.getResultHelper().getSystemMessage());
            trans.setTransactionsDatetime(new Date());
            if (requestHelper.getCreditCardHelper().getAdditionalData() != null) {
                trans.setIncAdditionalInformation(requestHelper.getCreditCardHelper().getAdditionalData().toString());
            }
            trans.setIncPaymentchannel(requestHelper.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());
            trans.setMerchantPaymentChannel(requestHelper.getMerchantPaymentChannel());
            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            trans.setAccountId(base.maskingString(requestHelper.getCreditCardHelper().getCardNumber(), "PAN"));

            Set<TransactionsDataCardholder> chs = new HashSet<TransactionsDataCardholder>();
            TransactionsDataCardholder ch = new TransactionsDataCardholder();
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveRecurTransactions : TransactionsDataCardholder");
            ch.setTransactionsId(getNextVal(SeqTransactionsDataCardholderIdNextval.class).longValue());
            ch.setIncAddress(requestHelper.getCreditCardHelper().getCcAddress());
            //ch.setIncBirthdate();
            ch.setIncCcName(requestHelper.getCreditCardHelper().getCcName() == null ? requestHelper.getRecurHelper().getCustomerName() : requestHelper.getCreditCardHelper().getCcName());
            ch.setIncCity(requestHelper.getCreditCardHelper().getCcCity());
            ch.setIncCountry(requestHelper.getCreditCardHelper().getCcCountry());
            ch.setIncHomephone(requestHelper.getCreditCardHelper().getCcHomePhone());
            ch.setIncMobilephone(requestHelper.getCreditCardHelper().getCcMobilePhone());
            ch.setIncState(requestHelper.getCreditCardHelper().getCcRegion());
            ch.setIncWorkphone(requestHelper.getCreditCardHelper().getCcWorkPhone());
            ch.setIncZipcode(requestHelper.getCreditCardHelper().getCcZipCode());
            ch.setTransactions(trans);
            chs.add(ch);
            trans.setTransactionsDataCardholders(chs);
            trans.setTokenId(requestHelper.getRecurHelper().getTokenId());
            trans.setIncCustomerId(requestHelper.getRecurHelper().getCustomerId());
            trans.setTransactionsState(OneCheckoutTransactionState.INCOMING.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
            TransactionsDataRecur dataRecur = new TransactionsDataRecur();

            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveRecurTransactions : TransactionsDataRecur");

            if (trans.getTokenId() == null || trans.getTokenId().isEmpty()) {
                String data = KMCardUtils.encrypt(requestHelper.getCreditCardHelper().getCardNumber() + "|" + requestHelper.getCreditCardHelper().getExpMonth() + "" + requestHelper.getCreditCardHelper().getExpYear());
                dataRecur.setIncAdditionalData1(data);
            }

            dataRecur.setTransactionsId(getNextVal(SeqTransactionsDataRecurIdNextval.class).longValue());
            dataRecur.setIncAdditionalData2("");
            dataRecur.setIncBillDetail(requestHelper.getRecurHelper().getBillingDetail());
            dataRecur.setIncBillNumber(requestHelper.getRecurHelper().getBillingNumber());
            dataRecur.setIncBillType(requestHelper.getRecurHelper().getBillingType().charAt(0));
            dataRecur.setIncEndDate(requestHelper.getRecurHelper().getExecuteEndDate());
            dataRecur.setIncExecuteDate(requestHelper.getRecurHelper().getExecuteDate());
            dataRecur.setIncExecuteMonth(requestHelper.getRecurHelper().getExecuteMonth());
            dataRecur.setIncExecuteType(requestHelper.getRecurHelper().getExecuteType().charAt(0));
            dataRecur.setIncStartDate(requestHelper.getRecurHelper().getExecuteStartDate());
            dataRecur.setTransactions(trans);
            trans.setTransactionsDataRecur(dataRecur);

            em.persist(trans);
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveRecurTransactions : Done Persisting Transaction :" + trans.getTransactionsId());
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.saveRecurTransactions Exception : %s", th.getMessage());
        }
        return trans;
    }

    /**
     *
     * JOJO NAMBAH HERE...
     *
     *
     *
     * ------------------------------------------------------------------------------------------------
     * :: QUERY FOR SCHEDULER GET.ALLRATES....
     * ------------------------------------------------------------------------------------------------
     */
    public Rates getLastRate() {
        try {
            Criteria criteria = getSession().createCriteria(Rates.class);
            criteria.add(Restrictions.eq("status", OneCheckoutRateStatus.NEW.code()));
            criteria.setMaxResults(1);
            criteria.addOrder(Order.desc("id"));
            return (Rates) criteria.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean updatePrevRates(String sellCurreny, String buyCurrency) {
        try {
            Criteria criteria = getSession().createCriteria(Rates.class);
            criteria.add(Restrictions.eq("sellCurrencyCode", sellCurreny));
            criteria.add(Restrictions.eq("buyCurrencyCode", buyCurrency));
            criteria.add(Restrictions.eq("status", OneCheckoutRateStatus.NEW.code()));
            Rates rate = (Rates) criteria.uniqueResult();
            if (rate != null) {
                rate.setStatus(OneCheckoutRateStatus.OLD.code());
                getSession().saveOrUpdate(rate);
            }

            //:: UPDATE TABLE RATES DB ESCROW..
            Query query = getSessionEscrow().createSQLQuery("UPDATE rates SET status=:newStatus WHERE status=:status");
            query.setParameter("status", OneCheckoutRateStatus.NEW.code());
            query.setParameter("newStatus", OneCheckoutRateStatus.OLD.code());
            Integer rowUpdated = query.executeUpdate();
            OneCheckoutLogger.log("DB Escrow Total row updated prev rates : " + rowUpdated);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean updateExpireRates() {
        try {
            //:: UPDATE TABLE RATES DB OCO..
            Query query = getSession().createSQLQuery("UPDATE rates SET status=:newStatus WHERE expiry_date < :currentTime AND status <> 'E'");
            query.setParameter("newStatus", OneCheckoutRateStatus.EXPIRED.code());
            query.setParameter("currentTime", new Date());
            Integer rowUpdated = query.executeUpdate();
            OneCheckoutLogger.log("DB OCO Total row updated : " + rowUpdated);

            //:: UPDATE TABLE RATES DB ESCROW..
            query = getSessionEscrow().createSQLQuery("UPDATE rates SET status=:newStatus WHERE expiry_date < :currentTime AND status <> 'E'");
            query.setParameter("newStatus", OneCheckoutRateStatus.EXPIRED.code());
            query.setParameter("currentTime", new Date());
            rowUpdated = query.executeUpdate();
            OneCheckoutLogger.log("DB Escrow Total row updated : " + rowUpdated);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public RBSRequestHelper putAllNewRates(RBSRequestHelper helper) {
        try {
            OneCheckoutLogger.log(":: PUT NEW RATES TO DB..");
            Rates rates;
            Double marginAmount, originRate;
            Double percentRate = OneCheckoutProperties.getOneCheckoutConfig().getDouble("RBS.DOKU.RATES.PERCENTAGE.FEE", 0.02);
            for (RBSRateQuote rQuote : helper.getRateQuoteList()) {
                OneCheckoutLogger.log("-----------------------------------------------------------");
                OneCheckoutLogger.log(":: TenorDays          : [%s]", rQuote.getTenorDays());
                OneCheckoutLogger.log(":: ValueDate          : [%s]", rQuote.getValueDate());
                OneCheckoutLogger.log(":: RateQuoteId        : [%s]", rQuote.getRateQuoteId());
                OneCheckoutLogger.log(":: UnitCurrency       : [%s]", rQuote.getUnitCurrency());
                OneCheckoutLogger.log(":: IsRevoked          : [%s]", rQuote.getIsRevoked());
                OneCheckoutLogger.log(":: SegmentId          : [%s]", rQuote.getSegmentId());
                OneCheckoutLogger.log(":: ExpiryDateTime     : [%s]", rQuote.getExpiryDateTime());
                OneCheckoutLogger.log(":: SchemeSellCurrency : [%s]", rQuote.getSchemeSellCurrency());
                OneCheckoutLogger.log(":: SchemeBuyCurrency  : [%s]", rQuote.getSchemeBuyCurrency());
                OneCheckoutLogger.log(":: IsExecutable       : [%s]", rQuote.getIsExecutable());
                OneCheckoutLogger.log(":: Rate (Final)       : [%s]", rQuote.getRate());
                if (updatePrevRates(rQuote.getSchemeSellCurrency(), rQuote.getSchemeBuyCurrency())) {
                    marginAmount = rQuote.getRate() * percentRate;
                    originRate = rQuote.getRate() - marginAmount;
                    OneCheckoutLogger.log(":: Rate (Original)    : [%s]", originRate);
                    OneCheckoutLogger.log(":: Rate (margin)      : [%s]", marginAmount);
                    //:: SAVE TO TABLE RATES DB OCO..
                    rates = new Rates();
                    rates.setId((Integer) getNextVal(SeqRatesIdNextval.class));
                    rates.setSellCurrencyCode(rQuote.getSchemeSellCurrency());
                    rates.setSellCurrencyNum(ECurrency.valueOf(rQuote.getSchemeSellCurrency()).num());
                    rates.setBuyCurrencyCode(rQuote.getSchemeBuyCurrency());
                    rates.setBuyCurrencyNum(ECurrency.valueOf(rQuote.getSchemeBuyCurrency()).num());
                    rates.setRateQuoteId(rQuote.getRateQuoteId());
                    rates.setSegmentId(rQuote.getSegmentId());
                    rates.setFinalRate(BigDecimal.valueOf(rQuote.getRate()));
                    rates.setOriginRate(BigDecimal.valueOf(originRate));
                    rates.setMarginRate(BigDecimal.valueOf(marginAmount));
                    rates.setStatus(OneCheckoutRateStatus.NEW.code());
                    rates.setValueDate(rQuote.getValueDate());
                    rates.setExpiryDate(rQuote.getExpiryDateTime());
                    rates.setRequestDate(new Date());
                    getSession().save(rates);
                    getSession().flush();

                    //:: SAVE TO TABLE RATES DB ESCROW..
                    doku.escrow.entity.Rates escRates = new doku.escrow.entity.Rates();
                    escRates.setId(rates.getId());
                    escRates.setSellCurrencyCode(rates.getSellCurrencyCode());
                    escRates.setSellCurrencyNum(rates.getSellCurrencyNum());
                    escRates.setBuyCurrencyCode(rates.getBuyCurrencyCode());
                    escRates.setBuyCurrencyNum(rates.getBuyCurrencyNum());
                    escRates.setRateQuoteId(rates.getRateQuoteId());
                    escRates.setSegmentId(rates.getSegmentId());
                    escRates.setFinalRate(rates.getFinalRate());
                    escRates.setOriginRate(rates.getOriginRate());
                    escRates.setMarginRate(rates.getMarginRate());
                    escRates.setStatus(rates.getStatus());
                    escRates.setValueDate(rates.getValueDate());
                    escRates.setExpiryDate(rates.getExpiryDate());
                    escRates.setRequestDate(rates.getRequestDate());
                    getSessionEscrow().save(escRates);
                    getSessionEscrow().flush();

                    marginAmount = null;
                    originRate = null;
                } else {
                    helper.setResponseCode(EResponseCode.FAILED);
                    break;
                }
            }

            // :: update prev rates..
            updateExpireRates();
        } catch (Exception e) {
            helper.setResponseCode(EResponseCode.FAILED);
            e.printStackTrace();
        }
        return helper;
    }

    /**
     *
     *
     *
     * ------------------------------------------------------------------------------------------------
     * :: QUERY FOR OPEN API GETRATES....
     * ------------------------------------------------------------------------------------------------
     */
    public Rates getRates(String sellCurrencyNum, String buyCurrencyNum) {
        try {
            Criteria criteria = getSession().createCriteria(Rates.class);
            criteria.add(Restrictions.eq("status", OneCheckoutRateStatus.NEW.code()));
            criteria.add(Restrictions.eq("sellCurrencyNum", sellCurrencyNum));
            criteria.add(Restrictions.eq("buyCurrencyNum", buyCurrencyNum));
            criteria.add(Restrictions.gt("expiryDate", new Date()));//already expired or not..            
            criteria.setMaxResults(1);
            criteria.addOrder(Order.desc("id"));
            Rates rates = (Rates) criteria.uniqueResult();

            if (rates == null) {
                //::: try to get old rates..
                criteria = getSession().createCriteria(Rates.class);
                criteria.add(Restrictions.eq("status", OneCheckoutRateStatus.OLD.code()));
                criteria.add(Restrictions.eq("sellCurrencyNum", sellCurrencyNum));
                criteria.add(Restrictions.eq("buyCurrencyNum", buyCurrencyNum));
                criteria.add(Restrictions.gt("expiryDate", new Date()));//already expired or not..            
                criteria.setMaxResults(1);
                criteria.addOrder(Order.desc("id"));
                rates = (Rates) criteria.uniqueResult();
                //:: send alert to support..
                EmailUtility.sendNotifAlert("Active rates not found..");
            }

            if (rates != null) {
                return rates;
            }

        } catch (Exception e) {
            e.printStackTrace();
            EmailUtility.sendNotifAlert("Active rates not found..");
        }
        OneCheckoutLogger.log("FAILED GET ACTIVE RATES WITH STATUS \"NEW & OLD\", ALL RATES HAS BEEN EXPIRED...");
        return null;
    }

    public RBSRequestHelper putNewRates(RBSRequestHelper helper) {
        try {
            OneCheckoutLogger.log(":: PUT NEW RATES TO DB..");
            OneCheckoutLogger.log("-----------------------------------------------------------");
            Rates rates;
            Double marginAmount, originRate;
            Double percentRate = OneCheckoutProperties.getOneCheckoutConfig().getDouble("RBS.DOKU.RATES.PERCENTAGE.FEE", 0.02);
            RBSRateQuote rQuote = helper.getRateQuote();
            OneCheckoutLogger.log(":: Rate               : [%s]", rQuote.getRate());
            OneCheckoutLogger.log(":: TenorDays          : [%s]", rQuote.getTenorDays());
            OneCheckoutLogger.log(":: ValueDate          : [%s]", rQuote.getValueDate());
            OneCheckoutLogger.log(":: RateQuoteId        : [%s]", rQuote.getRateQuoteId());
            OneCheckoutLogger.log(":: UnitCurrency       : [%s]", rQuote.getUnitCurrency());
            OneCheckoutLogger.log(":: IsRevoked          : [%s]", rQuote.getIsRevoked());
            OneCheckoutLogger.log(":: SegmentId          : [%s]", rQuote.getSegmentId());
            OneCheckoutLogger.log(":: ExpiryDateTime     : [%s]", rQuote.getExpiryDateTime());
            OneCheckoutLogger.log(":: SchemeSellCurrency : [%s]", rQuote.getSchemeSellCurrency());
            OneCheckoutLogger.log(":: SchemeBuyCurrency  : [%s]", rQuote.getSchemeBuyCurrency());
            OneCheckoutLogger.log(":: IsExecutable       : [%s]", rQuote.getIsExecutable());
            OneCheckoutLogger.log("-----------------------------------------------------------");
            if (updatePrevRates(rQuote.getSchemeSellCurrency(), rQuote.getSchemeBuyCurrency())) {
//                marginAmount = rQuote.getRate() * percentRate;
//                originRate = rQuote.getRate() - marginAmount;
                originRate = rQuote.getRate() / (1 + percentRate);
                marginAmount = rQuote.getRate() - originRate;
                rates = new Rates();
                rates.setBuyCurrencyCode(rQuote.getSchemeBuyCurrency());
                rates.setBuyCurrencyNum(ECurrency.valueOf(rQuote.getSchemeBuyCurrency()).num());
                rates.setSellCurrencyCode(rQuote.getSchemeSellCurrency());
                rates.setSellCurrencyNum(ECurrency.valueOf(rQuote.getSchemeSellCurrency()).num());
                rates.setRateQuoteId(rQuote.getRateQuoteId());
                rates.setSegmentId(rQuote.getSegmentId());
                rates.setFinalRate(BigDecimal.valueOf(rQuote.getRate()));
                rates.setOriginRate(BigDecimal.valueOf(originRate));
                rates.setMarginRate(BigDecimal.valueOf(marginAmount));
                rates.setStatus(OneCheckoutRateStatus.NEW.code());
                rates.setValueDate(rQuote.getValueDate());
                rates.setExpiryDate(rQuote.getExpiryDateTime());
                getSession().save(rates);
                getSession().flush();

                //:: SAVE TO TABLE RATES DB ESCROW..
                doku.escrow.entity.Rates escRates = new doku.escrow.entity.Rates();
                escRates.setId(rates.getId());
                escRates.setBuyCurrencyCode(rates.getBuyCurrencyCode());
                escRates.setBuyCurrencyNum(rates.getBuyCurrencyNum());
                escRates.setSellCurrencyCode(rates.getSellCurrencyCode());
                escRates.setSellCurrencyNum(rates.getBuyCurrencyNum());
                escRates.setRateQuoteId(rates.getRateQuoteId());
                escRates.setSegmentId(rates.getSegmentId());
                escRates.setFinalRate(rates.getFinalRate());
                escRates.setOriginRate(rates.getOriginRate());
                escRates.setMarginRate(rates.getMarginRate());
                escRates.setStatus(rates.getStatus());
                escRates.setValueDate(rates.getValueDate());
                escRates.setExpiryDate(rates.getExpiryDate());
                marginAmount = null;
                originRate = null;
                // :: update prev rates..
                updateExpireRates();
            } else {
                helper.setResponseCode(EResponseCode.FAILED);
            }

        } catch (Exception e) {
            helper.setResponseCode(EResponseCode.FAILED);
            e.printStackTrace();
        }
        return helper;
    }

    private Transactions getVATransanctionByInvoiceId(String invoiceNo, String words) {
        Transactions trans = null;

        try {

            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getVATransactionByInvoiceId Search PaymentCode by INVOICE=%s and WORDS=%s", invoiceNo, words);
            Criteria criteria = getSession().createCriteria(Transactions.class);
            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));
            criteria.add(Restrictions.eq("incWords", words));
//            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.INCOMING.value()));
            criteria.add(Restrictions.isNotNull("accountId"));

//            OneCheckoutLogger.log(": : : : : INVOICE " + invoiceNo);
//            OneCheckoutLogger.log(": : : : : WORDS   " + words);
            criteria.addOrder(Order.asc("transactionsId"));
            criteria.setMaxResults(1);
            trans = (Transactions) criteria.uniqueResult();
            return trans;

        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getVATransactionByInvoiceId   Exception : %s", th.getMessage());
            return trans;
        }
    }

    public Transactions getTransactionByCriteria(String invoiceNo, Merchants merchants) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        Transactions trans = null;

        try {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionByCriteria Search Transactions by INVOICE=%s", invoiceNo);
//            Transactions trans = (Transactions) em.createQuery("SELECT trans FROM Transactions trans INNER JOIN FETCH trans.merchantPaymentChannel mpc " + "WHERE trans.incTransidmerchant=:invNo and mpc.merchants=:merchant").setParameter("invNo", invoiceNo).setParameter("merchant", merchants).get;
            Criteria criteria = getSession().createCriteria(Transactions.class);
//            criteria.createCriteria("transactions", "trans", Criteria.INNER_JOIN);
//            criteria.createCriteria("trans.merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
//            criteria.createCriteria("mpc.merchants", "m", Criteria.INNER_JOIN);

//            criteria.add(Restrictions.eq("m.merchantCode", merchants.getMerchantCode()));
//            criteria.add(Restrictions.eq("m.merchantChainMerchantCode", merchants.getMerchantChainMerchantCode()));
            Criterion c5 = Restrictions.eq("incMallid", merchants.getMerchantCode());
            Criterion c6 = Restrictions.eq("incChainmerchant", merchants.getMerchantChainMerchantCode());
            Criterion c7 = Restrictions.and(c5, c6);
            criteria.add(c7);
            criteria.add(Restrictions.eq("incTransidmerchant", invoiceNo));

            Criterion c1 = Restrictions.eq("transactionsStatus", OneCheckoutTransactionStatus.SUCCESS.value());
            Criterion c2 = Restrictions.eq("transactionsStatus", OneCheckoutTransactionStatus.VOIDED.value());
            Criterion c3 = Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value());
            Criterion c4 = Restrictions.or(Restrictions.and(c1, c3), Restrictions.and(c2, c3));

            criteria.add(c4);

//            OneCheckoutLogger.log(": : : : : INVOICE " + invoiceNo);
//            OneCheckoutLogger.log(": : : : : WORDS   " + words);
            OneCheckoutLogger.log(":: QUERY = " + criteria.toString());
//            criteria.addOrder(Order.asc("transactionsId"));
            criteria.setMaxResults(1);
            trans = (Transactions) criteria.uniqueResult();
            return trans;
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getTransactionByCriteria   Exception : %s", th.getMessage());
            return trans;
        }
    }

    public ResponseCode getResponseCode(String responseCode) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        ResponseCode responseCode1 = null;
        try {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getResponseCode Search responseCode by INVOICE=%s", responseCode);
            Criteria criteria = getSession().createCriteria(ResponseCode.class);
            Criterion c1 = Restrictions.eq("code", responseCode);
            criteria.add(c1);
            criteria.addOrder(Order.asc("id"));
            criteria.setMaxResults(1);
            responseCode1 = (ResponseCode) criteria.uniqueResult();
            return responseCode1;
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getResponseCode Exception : %s", th.getMessage());
        }
        return responseCode1;
    }

    public Batch getBatchByCriterion(Criterion... criterions) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        Batch batch = null;
        try {
            Criteria criteria = getSession().createCriteria(Batch.class);
            criteria.addOrder(Order.desc("batchId"));

            for (Criterion c : criterions) {
                criteria.add(c);
            }
            criteria.setMaxResults(1);
            OneCheckoutLogger.log(" : : : QUERY : " + criteria.toString());
            batch = (Batch) criteria.uniqueResult();
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("Error in -getBatchByCriterion- : " + th.getMessage());
        }
        return batch;
    }

    public void saveBatch(Batch batch) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        try {
            em.persist(batch);
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("Error in -saveBatch- : " + th.getMessage());
        }
    }

    public void updateTransaction(Transactions transactions) {
        try {
            OneCheckoutLogger.log("Update transactions = " + transactions.getTransactionsId());
            em.merge(transactions);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            em.getTransaction().rollback();
            OneCheckoutLogger.log("Error in -updateTransaction- : " + throwable.getMessage());
        }
    }

    public MerchantPaymentChannel getMerchantPaymentChannelOpenAmount(Merchants merchant, PaymentChannel pchannel) {
        MerchantPaymentChannel channel = null;

        try {
            Criteria criteria = getSession().createCriteria(MerchantPaymentChannel.class);

            criteria.createCriteria("merchants", "merchants", Criteria.INNER_JOIN);
            criteria.createCriteria("paymentChannel", "paymentChannel", Criteria.INNER_JOIN);

            //criteria.add(Restrictions.eq("paymentChannel.paymentChannelId", paymentChannel.PermataVALite.value()));
            criteria.add(Restrictions.eq("paymentChannel.paymentChannelId", pchannel.getPaymentChannelId()));
            criteria.add(Restrictions.eq("merchants", merchant));
            criteria.add(Restrictions.eq("VAOpenAMount", true));
            criteria.add(Restrictions.eq("merchantPaymentChannelStatus", true));
            criteria.addOrder(Order.desc("merchantPaymentChannelId"));

            channel = (MerchantPaymentChannel) criteria.uniqueResult();

        } catch (Throwable throwable) {
            OneCheckoutLogger.log("Error : " + throwable.getMessage());
            throwable.printStackTrace();
        }
        return channel;
    }

    public Transactions getTransactionByCriterion(Criterion... criterions) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        Transactions transactions = null;
        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);
            criteria.addOrder(Order.desc("transactionsId"));
            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);
            for (Criterion c : criterions) {
                criteria.add(c);
            }
            criteria.setMaxResults(1);
            OneCheckoutLogger.log(" : : : QUERY : " + criteria.toString());
            transactions = (Transactions) criteria.uniqueResult();

        } catch (Exception e) {
            e.printStackTrace();
            OneCheckoutLogger.log("ERROR : " + e.getMessage());
        }
        return transactions;
    }

    public List<Transactions> getTransactionsList(Criterion... criterions) {

        List<Transactions> transList = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);
            criteria.addOrder(Order.desc("transactionsId"));
            criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
            criteria.createCriteria("mpc.paymentChannel", "pc", Criteria.INNER_JOIN);
            for (Criterion c : criterions) {
                criteria.add(c);
            }
//            criteria.setMaxResults(1);
            OneCheckoutLogger.log(" : : : QUERY : " + criteria.toString());

            transList = criteria.list();

            if (transList != null) {
                OneCheckoutLogger.log(": : : : : TRANSACTION SIZE = " + transList.size());
                if (transList.size() == 0) {
                    return null;
                }
            }

            return transList;
        } catch (Throwable th) {
            OneCheckoutLogger.log("OneCheckoutV1QueryHelperBean.getKlikBCATransactionBy   Exception : %s", th.getMessage());
//            th.printStackTrace();
            return transList;

        }
        
    }
}
