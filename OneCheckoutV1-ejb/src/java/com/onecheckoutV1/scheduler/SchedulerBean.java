/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.scheduler;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.EmailUtility;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutEmailType;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import com.onechekoutv1.dto.TransactionsDataCardholder;
import com.onechekoutv1.dto.TransactionsDataNonAirlines;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author aditya
 *
 */
@Stateless
public class SchedulerBean extends OneCheckoutChannelBase implements SchedulerBeanRemote {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;

    private Session getSession() {
        return (Session) em.getDelegate();
    }

    public Boolean executeMandiriVA() {
        boolean result = false;

        try {

            Date nowDate = new Date();

            DateFormat df = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            Calendar calendar = Calendar.getInstance();

            calendar.setTime(nowDate);
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 3);
            calendar.set(Calendar.HOUR_OF_DAY, 22);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            OneCheckoutLogger.log(": : : : Start Date " + df.format(calendar.getTime()));

            calendar.setTime(nowDate);
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
            calendar.set(Calendar.HOUR_OF_DAY, 21);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            OneCheckoutLogger.log(": : : : End Date   " + df.format(calendar.getTime()));

            result = sendEmail(listTransaction(nowDate, nowDate));

            OneCheckoutLogger.log(": : : : Generate Send Email Result [" + result + "]");

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return result;
    }

    public List<Transactions> listTransaction(Date startDate, Date endDate) {
        List<Transactions> trans = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class);

            criteria.createCriteria("merchantPaymentChannel", "merchantPaymentChannel");
            criteria.createCriteria("merchantPaymentChannel.paymentChannel", "paymentChannel");
            criteria.createCriteria("merchantPaymentChannel.merchants", "merchant");

            criteria.add(Restrictions.and(Restrictions.ge("startTime", startDate), Restrictions.le("endTime", endDate)));
            criteria.add(Restrictions.eq("transactionsState", OneCheckoutTransactionState.DONE.value()));
            //criteria.add(Restrictions.or(Restrictions.eq("paymentChannel.paymentChannelId", OneCheckoutPaymentChannel.MandiriVAFull.value()), Restrictions.eq("paymentChannel.paymentChannelId", OneCheckoutPaymentChannel.MandiriVALite.value())));
            criteria.add(Restrictions.or(Restrictions.eq("paymentChannel.paymentChannelId", OneCheckoutPaymentChannel.MandiriSOAFull.value()), Restrictions.eq("paymentChannel.paymentChannelId", OneCheckoutPaymentChannel.MandiriSOALite.value())));

            trans = criteria.list();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return trans;
    }

    public List<Transactions> listTransaction(OneCheckoutTransactionState state) {
        List<Transactions> trans = null;

        try {

            Criteria criteria = getSession().createCriteria(Transactions.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

            criteria.createCriteria("merchantPaymentChannel", "merchantPaymentChannel");
            criteria.createCriteria("merchantPaymentChannel.paymentChannel", "paymentChannel");
            criteria.createCriteria("merchantPaymentChannel.merchants", "merchant");

            criteria.setFetchMode("transactionsDataNonAirlineses", FetchMode.EAGER).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            criteria.setFetchMode("transactionsDataCardholders", FetchMode.EAGER).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

            criteria.add(Restrictions.eq("transactionsState", state.value()));

            trans = criteria.list();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return trans;
    }

    public Boolean sendEmail(List<Transactions> transactions) {
        boolean result = false;

        try {

            OneCheckoutLogger.log(": : : : Execute Send Email ");

            for (Transactions trans : transactions) {

                MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
                Merchants merchant = mpc.getMerchants();
                PaymentChannel paymentChannel = mpc.getPaymentChannel();

                String templateDirectory = config.getString("email.directory").trim();
                String templateFileName = config.getString("email.unsetlled.notification.filename").trim();
                String subject = config.getString("email.unsetlled.subject").trim();
                String email = config.getString("email.support").trim();

               OneCheckoutLogger.log("templateDirectory : " + templateDirectory);
                OneCheckoutLogger.log("templateFileName : " + templateFileName);
                OneCheckoutLogger.log("subject : " + subject);

                Map emailParam = new HashMap();
                emailParam.put("merchantName", merchant.getMerchantName());
                emailParam.put("mallId", merchant.getMerchantCode());
                emailParam.put("customerName", trans.getIncName());
                emailParam.put("customerPhone", "-");
                emailParam.put("customerEmail", trans.getIncEmail());
                emailParam.put("paymentChannel", paymentChannel.getPaymentChannelName());
                emailParam.put("invoiceNo", trans.getIncTransidmerchant());
                emailParam.put("amount", OneCheckoutVerifyFormatData.moneyFormat.format(trans.getIncAmount()));
                emailParam.put("status", OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus()));

                result = EmailUtility.sendEmailTemplate(templateDirectory, templateFileName, emailParam, OneCheckoutEmailType.HTML.code(), email, subject);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return result;
    }

    public Boolean executeNotify() {
        boolean result = false;

        try {

            sendNotify(listTransaction(OneCheckoutTransactionState.PROCESS));

            OneCheckoutLogger.log(": : : : Generate Send Notify Result [" + result + "]");

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return result;
    }

    public Boolean sendNotify(List<Transactions> transactions) {
        boolean result = false;

        try {

            OneCheckoutLogger.log(": : : : Execute Send Notify ");

            for (Transactions trans : transactions) {
                notifyMerchant(trans);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return result;
    }

    public OneCheckoutNotifyStatusResponse notifyMerchant(Transactions trans) {

        OneCheckoutNotifyStatusResponse notifyResp = new OneCheckoutNotifyStatusResponse();

        try {
            OneCheckoutLogger.log("SchedulerBean.notifyMerchant do Notify Status Merchant");

            Merchants merchant = trans.getMerchantPaymentChannel().getMerchants();

            String url = config.getString(merchant.getMerchantCode() +"123onecheckout.notifyurl").trim();

            TransactionsDataNonAirlines transData = new TransactionsDataNonAirlines();
            if (trans.getTransactionsDataNonAirlineses() != null && trans.getTransactionsDataNonAirlineses().size() > 0) {
                transData = trans.getTransactionsDataNonAirlineses().iterator().next();
            }

            TransactionsDataCardholder cardHolderData = new TransactionsDataCardholder();
            if (trans.getTransactionsDataCardholders() != null && trans.getTransactionsDataCardholders().size() > 0) {
                cardHolderData = trans.getTransactionsDataCardholders().iterator().next();
            }

            String data_encode = "";

            //POST DATA
            data_encode += URLEncoder.encode("basket", "UTF-8") + "=" + URLEncoder.encode(transData.getIncBasket() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("trans_date", "UTF-8") + "=" + URLEncoder.encode(OneCheckoutVerifyFormatData.email_datetimeFormat.format(trans.getTransactionsDatetime()) + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("trans_status", "UTF-8") + "=" + URLEncoder.encode(trans.getTransactionsStatus() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("trans_id_merchant", "UTF-8") + "=" + URLEncoder.encode(trans.getMerchantPaymentChannel().getMerchants().getMerchantCode() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("amount", "UTF-8") + "=" + URLEncoder.encode(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()) + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_name", "UTF-8") + "=" + URLEncoder.encode(trans.getIncName() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_email", "UTF-8") + "=" + URLEncoder.encode(trans.getIncEmail() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_address", "UTF-8") + "=" + URLEncoder.encode(cardHolderData.getIncAddress() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_handphone", "UTF-8") + "=" + URLEncoder.encode(cardHolderData.getIncMobilephone() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_zipcode", "UTF-8") + "=" + URLEncoder.encode(cardHolderData.getIncZipcode() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_city", "UTF-8") + "=" + URLEncoder.encode(cardHolderData.getIncCity() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_state", "UTF-8") + "=" + URLEncoder.encode(cardHolderData.getIncState() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_ship_address", "UTF-8") + "=" + URLEncoder.encode(transData.getIncShippingAddress() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_ship_city", "UTF-8") + "=" + URLEncoder.encode(transData.getIncShippingCity() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_ship_state", "UTF-8") + "=" + URLEncoder.encode(transData.getIncShippingState() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_ship_zipcode", "UTF-8") + "=" + URLEncoder.encode(transData.getIncShippingZipcode() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("customer_ship_country", "UTF-8") + "=" + URLEncoder.encode(transData.getIncShippingCountry() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("response_code", "UTF-8") + "=" + URLEncoder.encode(trans.getDokuResponseCode() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("approval_code", "UTF-8") + "=" + URLEncoder.encode(trans.getDokuVoidApprovalCode() + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("bank", "UTF-8") + "=" + URLEncoder.encode(trans.getDokuIssuerBank() + "", "UTF-8") + "&";

            HashMap<String, String> additionalInfo = new HashMap<String, String>();

            if (trans.getIncAdditionalInformation() != null && trans.getIncAdditionalInformation().length() > 0) {
                additionalInfo = super.stringToHashMap(trans.getIncAdditionalInformation());
            }

            data_encode += URLEncoder.encode("swiper_no", "UTF-8") + "=" + URLEncoder.encode(additionalInfo.get("swiperId") + "", "UTF-8") + "&";
            data_encode += URLEncoder.encode("mobile_no", "UTF-8") + "=" + URLEncoder.encode(additionalInfo.get("mobileId") + "", "UTF-8");

            OneCheckoutLogger.log("NOTIFY PARAM : "+data_encode);
            InternetResponse inetResp = super.doFetchHTTP(data_encode, url, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout());

            notifyResp.setACKNOWLEDGE(inetResp.getMsgResponse());
            notifyResp.setHTTP_RESPONSE_CODE(inetResp.getHTTPrespCode());
            notifyResp.setSTATUS(inetResp.getMsgResponse(),merchant.getMerchantNotifyTimeout());

            return notifyResp;

        } catch (Exception ex) {
            ex.printStackTrace();

            return notifyResp;
        }

    }

}