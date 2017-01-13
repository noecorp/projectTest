/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onechekoutv1.dto.Batch;
import com.onechekoutv1.dto.Country;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.PaypalResponseCode;
import com.onechekoutv1.dto.Rates;
import com.onechekoutv1.dto.ResponseCode;
import com.onechekoutv1.dto.Transactions;
import com.onechekoutv1.dto.TransactionsDataAirlines;
import com.onechekoutv1.dto.TransactionsDataCardholder;
import com.onechekoutv1.dto.TransactionsDataNonAirlines;
import doku.fx.micropay.helper.RBSRequestHelper;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.ejb.Local;
import org.hibernate.criterion.Criterion;

/**
 *
 * @author hafizsjafioedin
 */
@Local
public interface OneCheckoutV1QueryHelperBeanLocal {

    public Transactions getKlikBCARedirectTransactionBy(String invoiceNo, String sessionId, double amount);

    public PaymentChannel getPaymentChannelByChannel(com.onecheckoutV1.type.OneCheckoutPaymentChannel channel);

    public Number getNextVal(Class forEntity);

    public Merchants getMerchantByMallId(int mallId, int chainmerchantId);

    public Merchants getMerchantBy(int mallId);

//  public void updateTraceNoByMerchant(Merchants m);
//  public String generatePaymentCodeByMerchant(Merchants m,OneCheckoutPaymentChannel pChannel);
    public Merchants getMerchantBy(String accountId);

    public MerchantPaymentChannel getMerchantPaymentChannel(Merchants merchant, PaymentChannel pchannel);

    public MerchantPaymentChannel getMerchantPaymentChannel(Merchants merchant, OneCheckoutPaymentChannel paymentChannel);

    public MerchantPaymentChannel getMerchantPaymentChannel(String paymentChannelCode, PaymentChannel paymentChannel);

    public MerchantPaymentChannel getMerchantPaymentChannel(String paymentChannelCode, String companyCode, String userAccount);

    public MerchantPaymentChannel getMerchantPaymentChannel(String invoiceNo, String sessionId, BigDecimal amount, OneCheckoutTransactionState state);

    public List<MerchantPaymentChannel> getListMerchantPaymentChannel(Merchants merchant);

    public List<Transactions> getTransactionsBy(String invoiceNo, MerchantPaymentChannel mpc);

    public Batch getBatchByCriterion(Criterion... criterions);

    public void saveBatch(Batch batch);

    public Transactions getTransactionByCriteria(String invoiceNo, Merchants m);

    public List<TransactionsDataAirlines> getTransactionsByPNR(String pnr, MerchantPaymentChannel mpc);

    public Transactions getVerifyTransactionBy(String invoiceNo, String sessionId, double amount, PaymentChannel pchannel);

    public Transactions getRedirectTransactionWithoutState(String invoiceNo, String sessionId, double amount);

    public Transactions getRedirectTransactionWithoutState(String invoiceNo, String sessionId, double amount, String paymentCode, String paymentChannelId);

    public Object[] getRedirectTransactionWithoutStateNumber(String invoiceNo, String sessionId, double amount);

    public Transactions getNotifyTransactionBy(String invoiceNo, String sessionId, PaymentChannel pchannel);

    public Transactions getVoidTransactionBy(String invoiceNo, String sessionId, PaymentChannel pchannel);

    public void updateKlikBCAExpiredTransactions(String userId, String merchantCode, PaymentChannel pchannel, int expired_minutes);

    public void updateVirtualAccountExpiredTransactions(String paymentCode, String merchantCode, PaymentChannel pchannel, int expired_minutes);

    public TransactionsDataNonAirlines getTransactionNonAirlinesBy(Transactions trans);

    public Transactions getKlikBCATransactionBy(String invoiceNo, String userId, double amount, Date requestPayment, PaymentChannel pchannel);

    public TransactionsDataAirlines getTransactionAirlinesBy(Transactions trans);

    public List<Transactions> getKlikBCATransactionBy(String userId, String merchantCode, PaymentChannel pchannel);

    public Transactions getRedirectTransactionBy(String invoiceNo, String sessionId, double amount);

    public Transactions getRedirectTransactionBy(String invoiceNo, String sessionId, double amount, String paymentCode, String paymentChannelId);

    public Transactions getEDSDataTransactionBy(String invoiceNo, double amount, PaymentChannel pchannel, String words);

    public Transactions getEDSDataTransactionBy(String invoiceNo, double amount, String words);

    public TransactionsDataCardholder getTransactionCHBy(Transactions trans);

//  public Transactions getEDSUpdateTransactionBy(String invoiceNo, PaymentChannel pchannel, String words);
//  public Transactions getEDSUpdateTransactionBy(String invoiceNo, PaymentChannel pchannel, String words, OneCheckoutDFSStatus status);
//  public Transactions getEDSUpdateTransactionBy(String invoiceNo, String words);
    public Transactions getEDSUpdateTransactionBy(String invoiceNo, String words, OneCheckoutDFSStatus status);

//  public Transactions getCheckStatusTransactionByCC(Merchants m, String invoiceNo, String sessionId);
    public Transactions getCheckStatusTransactionBy(String invoiceNo, String sessionId, PaymentChannel pchannel, OneCheckoutTransactionState type);

    public Transactions getCheckVAStatusTransactionBy(String payCode, String sessionId, PaymentChannel pchannel, OneCheckoutTransactionState type);

    public Currency getCurrencyByCode(String currencyCode);

    public Currency getCurrencyByAlphaCode(String currencyCode);

    public Transactions getPermataVATransactionBy(String payCode, double amount);

//  public Transactions getPermataVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel);
    public Transactions getPermataVATransactionBy(String paycode, String merchantCode, String invoiceNo, PaymentChannel pchannel, String actionStatus);

    public Transactions getPermataVATransactionByWithAmount(String paycode, BigDecimal amount, String merchantCode, String invoiceNo, PaymentChannel pchannel, String actionStatus);

    public Transactions getPermataVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel, String actionStatus);

    public Transactions getPermataVATransactionByWithAmount(String paycode, BigDecimal amount, String merchantCode, PaymentChannel pchannel, String actionStatus);

    public Transactions getVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel);

    public Transactions getVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel, OneCheckoutTransactionState state);

    public Transactions getVirtualTransactionBy(String paycode, String merchantCode, PaymentChannel pchannel);

    public PaypalResponseCode getPaypalDokuResponseCode(String errorCode, String shortMessage);

    public List<Country> getListCountry();

    public String getCountryById(String id);

    public Transactions getCyberSourceTransactionsBy(String requestID, String invoiceNo);

    public com.onechekoutv1.dto.Country getCountryByNumericCode(java.lang.String numericCode);

    public Transactions getInquiryVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel);

    public Transactions getInitVATransactionBy(String paycode, String merchantCode, PaymentChannel pchannel);

    public Transactions getTransactionByXID(String XID);

    public Transactions getTransactionByID(long ID);

    public java.lang.Object updateRecord(java.lang.Object obj);

    public int getNextValue(java.lang.Class cls);

    @javax.ejb.TransactionAttribute(value = javax.ejb.TransactionAttributeType.REQUIRES_NEW)
    public void updateTransactions(com.onechekoutv1.dto.Transactions trx);

    @javax.ejb.TransactionAttribute(value = javax.ejb.TransactionAttributeType.REQUIRES_NEW)
    public Transactions saveTokenizationTransactions(RequestHelper requestHelper);

    @javax.ejb.TransactionAttribute(value = javax.ejb.TransactionAttributeType.REQUIRES_NEW)
    public Transactions saveRecurTransactions(RequestHelper requestHelper);

//  public Rates getRates(String currencyNum, String currencyRatesNum);
    public Transactions refreshAmountBaseNewRate(Transactions trans);

    public Transactions getCheckStatusTransactionByInvoiceSession(Merchants m, String invoiceNo, String sessionId);

    public RBSRequestHelper putAllNewRates(RBSRequestHelper helper);

    public Rates getLastRate();

    public Rates getRates(String sellCurrencyNum, String buyCurrencyNum);

    public RBSRequestHelper putNewRates(RBSRequestHelper helper);

    public TransactionsDataCardholder getCH(Transactions trans);

    public TransactionsDataAirlines getPassengerData(Transactions trans);

    public Transactions getTransactionByInvoiceSessionAmount(String invoiceNo, String sessionId, BigDecimal amount);

//  public Transactions getPermataVATransanctionByInvoiceId(String invoiceNo,String words);
    public String generatePaymentCodeByMerchant(Merchants m, OneCheckoutPaymentChannel pChannel, String invoiceNo, String words, Boolean openAmount);

    public com.onechekoutv1.dto.Session getSessionByRequestCode(java.lang.String requestCode);

    public boolean deleteSessionById(int sessionId);

    public ResponseCode getResponseCode(String responseCode);

    public void updateTransaction(Transactions transactions);

    public MerchantPaymentChannel getMerchantPaymentChannelOpenAmount(Merchants merchant, PaymentChannel pchannel);
    
    public Transactions getTransactionByCriterion(Criterion... criterions);
    
    public List<Transactions> getTransactionsList(Criterion... criterions);
}
