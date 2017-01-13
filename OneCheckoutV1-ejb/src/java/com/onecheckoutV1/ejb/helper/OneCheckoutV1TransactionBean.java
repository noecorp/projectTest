/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutDOKUVerifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutRSA;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckOutTransactionsType;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutGeneralConstant;
import com.onecheckoutV1.type.OneCheckoutMerchantCategory;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.*;
import doku.keylib.tools.KMCardUtils;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author hafizsjafioedin
 *
 */
@Stateless
public class OneCheckoutV1TransactionBean implements OneCheckoutV1TransactionBeanLocal {

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal nl;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;

    private Session getSession() {
        return (Session) em.getDelegate();
    }

    private long getTransactionNextval() {
        return nl.getNextVal(SeqTransactionIdNextval.class).longValue();
    }

    public Transactions saveTransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc) {

        Transactions trans = null;
        try {

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            Merchants m = trxHelper.getMerchant();
            trans = new Transactions();

            trans.setTransactionsId(getTransactionNextval());

            // mandatory
            trans.setIncMallid(paymentRequest.getMALLID());
            trans.setIncChainmerchant(paymentRequest.getCHAINMERCHANT());
            trans.setIncAmount(BigDecimal.valueOf(paymentRequest.getAMOUNT()));
            trans.setIncPurchaseamount(BigDecimal.valueOf(paymentRequest.getPURCHASEAMOUNT()));

            trans.setIncTransidmerchant(paymentRequest.getTRANSIDMERCHANT());
            trans.setIncWords(paymentRequest.getWORDS());
            trans.setIncRequestdatetime(paymentRequest.getREQUESTDATETIME());
            trans.setIncCurrency(paymentRequest.getCURRENCY());
            trans.setIncPurchasecurrency(paymentRequest.getPURCHASECURRENCY());
            trans.setIncSessionid(paymentRequest.getSESSIONID());
            trans.setIncEmail(paymentRequest.getEMAIL());
            trans.setIncName(paymentRequest.getNAME());
            trans.setTransactionsType(OneCheckOutTransactionsType.SALE.value());
//            if (mpc != null) {
//                String paymentChannelId = mpc.getPaymentChannel().getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
//                trans.setOcoId(ocoId);
//            }

            trans.setOcoId(generateOcoId(trxHelper.getPaymentChannel() != null ? trxHelper.getPaymentChannel().value() : "00"));
            if (paymentRequest.getRATE() != null) {
                trans.setRates(paymentRequest.getRATE());
                trans.setConvertedAmount(BigDecimal.valueOf(paymentRequest.getCONVERTEDAMOUNT()));
            }
            trans.setIncPromoid(paymentRequest.getPROMOID());
            trans.setIncInstallmentAcquirer(paymentRequest.getINSTALLMENT_ACQUIRER());
            trans.setIncTenor(paymentRequest.getTENOR());

            trans.setSystemMessage(trxHelper.getSystemMessage());
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions SYSTEM_SESSION : %s", trxHelper.getSystemSession());
            trans.setSystemSession(trxHelper.getSystemSession());
            trans.setTransactionsDatetime(new Date());
            if (paymentRequest.getADDITIONALINFO() != null) {
                trans.setIncAdditionalInformation(paymentRequest.getADDITIONALINFO());
            }
//            if (trxHelper.getRates() != null) {
//                trans.setRates(trxHelper.getRates());
//            }

            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions paymentRequest.getADDITIONALINFO() : %s", trans.getIncAdditionalInformation());
            if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                //   OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions trxHelper.getPaymentChannel() : %s", trxHelper.getPaymentChannel().value());
                trans.setIncPaymentchannel(trxHelper.getPaymentChannel().value());
                trans.setMerchantPaymentChannel(mpc);

                if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {

                    trans.setAccountId(paymentRequest.getDOKUPAYID());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DirectDebitMandiri) {
                    trans.setAccountId(paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
                    PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
                    String publicKeyString = config.getString("ONECHECKOUT.PUBLICKEY." + paymentRequest.getMALLID(), "NOKEY").trim();
                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions OneCheckoutPaymentChannel.DirectDebitMandiri");

                    try {
                        if (!publicKeyString.equalsIgnoreCase("NOKEY")) {
                            String ccen = OneCheckoutRSA.Encrypt(paymentRequest.getCARDNUMBER(), publicKeyString);
                            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions Save SystemSession : %s", trxHelper.getSystemSession() + "|" + ccen);
                            trans.setSystemSession(trxHelper.getSystemSession() + "|" + ccen);

                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : Exception.. " + ex.getMessage());
                    }

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.KlikBCA) {
                    trans.setAccountId(paymentRequest.getUSERIDKLIKBCA() != null ? paymentRequest.getUSERIDKLIKBCA().toUpperCase() : paymentRequest.getUSERIDKLIKBCA());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite) {
//                    if (!trxHelper.getPaymentPageObject().getPayCode().equals("")) {
//                        trans.setAccountId(trxHelper.getPaymentPageObject().getPayCode());
//                    } else {
                    trans.setAccountId(paymentRequest.getPAYCODE());
//                    }

                    if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                    }

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                    if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                    }
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite) {
                    trans.setAccountId(paymentRequest.getPAYCODE());
                    if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                    }

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());
                    if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                    }
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {
                    trans.setAccountId(paymentRequest.getPAYCODE());
                    if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                    }
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull) {
                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions paymentRequest.getPAYCODE() : %s", paymentRequest.getPAYCODE());
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVALite) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BCAVAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());
                    Set<TransactionsDataCardholder> chs = new HashSet<TransactionsDataCardholder>();
                    TransactionsDataCardholder ch = new TransactionsDataCardholder();

                    ch.setTransactionsId(nl.getNextVal(SeqTransactionsDataCardholderIdNextval.class).longValue());
                    ch.setIncAddress(paymentRequest.getADDRESS() != null ? paymentRequest.getADDRESS() : "");
                    ch.setIncBirthdate(paymentRequest.getBIRTHDATE() != null ? paymentRequest.getBIRTHDATE() : "");
                    ch.setIncCcName(paymentRequest.getCC_NAME() == null ? paymentRequest.getNAME() : paymentRequest.getCC_NAME());
                    ch.setIncCity(paymentRequest.getCITY() != null ? paymentRequest.getCITY() : "");
                    ch.setIncCountry(paymentRequest.getCOUNTRY() != null ? paymentRequest.getCOUNTRY() : "");
                    ch.setIncHomephone(paymentRequest.getHOMEPHONE() != null ? paymentRequest.getHOMEPHONE() : "");
                    ch.setIncMobilephone(paymentRequest.getMOBILEPHONE() != null ? paymentRequest.getMOBILEPHONE() : "");
                    ch.setIncState(paymentRequest.getSTATE() != null ? paymentRequest.getSTATE() : "");
                    ch.setIncWorkphone(paymentRequest.getWORKPHONE() != null ? paymentRequest.getWORKPHONE() : "");
                    ch.setIncZipcode(paymentRequest.getZIPCODE() != null ? paymentRequest.getZIPCODE() : "");
                    ch.setTransactions(trans);

                    chs.add(ch);
                    trans.setTransactionsDataCardholders(chs);
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Indomaret) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.CreditCard || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BNIDebitOnline || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BSP || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MOTO) {

                    trans.setAccountId(paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));

                    PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
                    String publicKeyString = config.getString("ONECHECKOUT.PUBLICKEY." + paymentRequest.getMALLID(), "NOKEY").trim();

                    try {
                        if (!publicKeyString.equalsIgnoreCase("NOKEY")) {

                            String ccen = OneCheckoutRSA.Encrypt(paymentRequest.getCARDNUMBER() + "," + paymentRequest.getEXPIRYDATE(), publicKeyString);
                            trans.setSystemSession(trxHelper.getSystemSession() + "|" + ccen);

                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : Exception " + ex.getMessage());
                    }

                    Set<TransactionsDataCardholder> chs = new HashSet<TransactionsDataCardholder>();
                    TransactionsDataCardholder ch = new TransactionsDataCardholder();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataCardholder");

                    ch.setTransactionsId(nl.getNextVal(SeqTransactionsDataCardholderIdNextval.class).longValue());
                    ch.setIncAddress(paymentRequest.getADDRESS());
                    ch.setIncBirthdate(paymentRequest.getBIRTHDATE());
                    //ch.setIncCcName(paymentRequest.getCC_NAME());
                    ch.setIncCcName(paymentRequest.getCC_NAME() == null ? paymentRequest.getNAME() : paymentRequest.getCC_NAME());
                    ch.setIncCity(paymentRequest.getCITY());
                    ch.setIncCountry(paymentRequest.getCOUNTRY());
                    ch.setIncHomephone(paymentRequest.getHOMEPHONE());
                    ch.setIncMobilephone(paymentRequest.getMOBILEPHONE());
                    ch.setIncState(paymentRequest.getSTATE());
                    ch.setIncWorkphone(paymentRequest.getWORKPHONE());
                    ch.setIncZipcode(paymentRequest.getZIPCODE());

                    ch.setTransactions(trans);
                    chs.add(ch);
                    trans.setTransactionsDataCardholders(chs);
                    trans.setDeviceId(paymentRequest.getDEVICEID());

                    if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization) {
                        trans.setTokenId(paymentRequest.getTOKENID());
                        trans.setIncCustomerId(paymentRequest.getCUSTOMERID());
                    }

                    if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur) {
                        trans.setIncCustomerId(paymentRequest.getCUSTOMERID());
                        if (paymentRequest.getTOKENID() != null && !paymentRequest.getTOKENID().isEmpty()) {
                            trans.setTokenId(paymentRequest.getTOKENID());
                        }

                        //    Set<TransactionsDataRecur> dataRecurs = new HashSet<TransactionsDataRecur>();
                        TransactionsDataRecur dataRecur = new TransactionsDataRecur();

                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataRecur");

                        if (trans.getTokenId() == null || trans.getTokenId().isEmpty()) {
                            String data = KMCardUtils.encrypt(paymentRequest.getCARDNUMBER() + "|" + paymentRequest.getEXPIRYDATE()); //.Encrypt(paymentRequest.getCARDNUMBER()+","+paymentRequest.getEXPIRYDATE(), publicKeyString);
                            dataRecur.setIncAdditionalData1(data);
                        }

                        dataRecur.setTransactionsId(nl.getNextVal(SeqTransactionsDataRecurIdNextval.class).longValue());

                        dataRecur.setIncAdditionalData2("");
                        dataRecur.setIncBillDetail(paymentRequest.getBILLDETAIL());
                        dataRecur.setIncBillNumber(paymentRequest.getBILLNUMBER());
                        dataRecur.setIncBillType(paymentRequest.getBILLTYPE().charAt(0));
                        dataRecur.setIncEndDate(OneCheckoutVerifyFormatData.dateFormat.format(paymentRequest.getENDDATE()));
                        dataRecur.setIncExecuteDate(paymentRequest.getEXECUTEDATE());
                        dataRecur.setIncExecuteMonth(paymentRequest.getEXECUTEMONTH());
                        dataRecur.setIncExecuteType(paymentRequest.getEXECUTETYPE().charAt(0));
                        dataRecur.setIncStartDate(OneCheckoutVerifyFormatData.dateFormat.format(paymentRequest.getSTARTDATE()));

                        dataRecur.setTransactions(trans);
                        //  dataRecurs.add(dataRecur);
                        trans.setTransactionsDataRecur(dataRecur);

                    }
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.KlikPayBCACard || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.KlikPayBCADebit) {
                    trans.setAccountId("");
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.CIMBClicks) {
                    trans.setAccountId("");
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Muamalat) {
                    trans.setAccountId("");
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Danamon) {
                    trans.setAccountId("");
                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DompetKu) {
                    trans.setAccountId("");
                }

            } else {
                trans.setAccountId("");
                trans.setIncPaymentchannel("");
                trans.setMerchantPaymentChannel(null);
            }

            trans.setTransactionsState(OneCheckoutTransactionState.INCOMING.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());

            if (m.getMerchantCategory() != null) {
                if (trxHelper.getPaymentChannel() == null || trxHelper.getPaymentChannel() != OneCheckoutPaymentChannel.Recur) {
                    if (m.getMerchantCategory() == OneCheckoutMerchantCategory.NONAIRLINE.value()) {
                        Set<TransactionsDataNonAirlines> nonairlines = new HashSet<TransactionsDataNonAirlines>();
                        TransactionsDataNonAirlines nonairline = new TransactionsDataNonAirlines();
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataNonAirlines");
                        nonairline.setTransactionsId(nl.getNextVal(SeqTransactionsDataNonAirlinesIdNextval.class).longValue());
                        nonairline.setIncBasket(paymentRequest.getBASKET());
                        nonairline.setIncShippingAddress(paymentRequest.getSHIPPING_ADDRESS());
                        nonairline.setIncShippingCity(paymentRequest.getSHIPPING_CITY());
                        nonairline.setIncShippingCountry(paymentRequest.getSHIPPING_COUNTRY());
                        nonairline.setIncShippingState(paymentRequest.getSHIPPING_STATE());
                        nonairline.setIncShippingZipcode(paymentRequest.getSHIPPING_ZIPCODE());
                        nonairline.setTransactions(trans);
                        nonairlines.add(nonairline);
                        trans.setTransactionsDataNonAirlineses(nonairlines);
                    } else if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {
                        Set<TransactionsDataAirlines> airlines = new HashSet<TransactionsDataAirlines>();
                        TransactionsDataAirlines airline = new TransactionsDataAirlines();
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataAirlines");
                        airline.setTransactionsId(nl.getNextVal(SeqTransactionsDataAirlinesNextval.class).longValue());
                        airline.setIncBookingcode(paymentRequest.getBOOKINGCODE());
                        airline.setIncFlight(paymentRequest.getFLIGHT().value());
                        airline.setIncFlightdate(paymentRequest.parseArray(paymentRequest.getFLIGHTDATE()));
                        airline.setIncFlightnumber(paymentRequest.parseArray(paymentRequest.getFLIGHTNUMBER()));
                        airline.setIncFlighttime(paymentRequest.parseArray(paymentRequest.getFLIGHTTIME()));
                        airline.setIncFlighttype(paymentRequest.getFLIGHTTYPE().value());
                        airline.setIncPassengerName(paymentRequest.parseArray(paymentRequest.getPASSENGER_NAME()));
                        airline.setIncPassengerType(paymentRequest.parseArray(paymentRequest.getPASSENGER_TYPE()));
                        airline.setIncRoute(paymentRequest.parseArray(paymentRequest.getROUTE()));
                        airline.setIncThirdpartyStatus(paymentRequest.getTHIRDPARTY_STATUS().value());
                        airline.setIncVat(BigDecimal.valueOf(paymentRequest.getVAT()));
                        airline.setIncInsurance(BigDecimal.valueOf(paymentRequest.getINSURANCE()));
                        airline.setIncFuelsurcharge(BigDecimal.valueOf(paymentRequest.getFUELSURCHARGE()));
                        airline.setIncFFNumber(paymentRequest.getFFNUMBER());
                        airline.setTransactions(trans);
                        airlines.add(airline);
                        trans.setTransactionsDataAirlineses(airlines);
                    }
                }
            }

            em.persist(trans);

            return trans;
        } catch (Exception ex) {

            ex.printStackTrace();

            return trans;
        }

    }

    public Transactions updateTransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc) {
        Transactions trans = null;

        try {

            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions =====================================");
            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            trans = getTransactionById(trxHelper);

            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions [" + trxHelper.getPaymentChannel().value() + "]");
            trans.setIncPaymentchannel(trxHelper.getPaymentChannel().value());
            trans.setMerchantPaymentChannel(mpc);

            if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {
                trans.setAccountId(paymentRequest.getDOKUPAYID());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DirectDebitMandiri) {
                trans.setAccountId(paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
                PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
                String publicKeyString = config.getString("ONECHECKOUT.PUBLICKEY." + paymentRequest.getMALLID(), "NOKEY").trim();
                OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions OneCheckoutPaymentChannel.DirectDebitMandiri");

                try {
                    if (!publicKeyString.equalsIgnoreCase("NOKEY")) {
                        String ccen = OneCheckoutRSA.Encrypt(paymentRequest.getCARDNUMBER(), publicKeyString);
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions Save SystemSession : %s", trxHelper.getSystemSession() + "|" + ccen);
                        trans.setSystemSession(trxHelper.getSystemSession() + "|" + ccen);

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions : Exception.. " + ex.getMessage());
                }

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.KlikBCA) {
                trans.setAccountId(paymentRequest.getUSERIDKLIKBCA() != null ? paymentRequest.getUSERIDKLIKBCA().toUpperCase() : paymentRequest.getUSERIDKLIKBCA());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                }

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                }
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                }

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                }
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                if (mpc.getMerchants().getMerchantReusablePaycode() != null && mpc.getMerchants().getMerchantReusablePaycode() == Boolean.TRUE) {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.INIT_VA.value());
                }

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite) {
                trans.setAccountId(paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVALite) {
                trans.setAccountId(paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart) {
                trans.setAccountId(paymentRequest.getPAYCODE());
            
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BRIVA) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                trans.setPartialAmount(paymentRequest.getVAPartialAmount());
                trans.setOpenAmount(paymentRequest.getVAOpenAmount());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DanamonVA) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                trans.setPartialAmount(paymentRequest.getVAPartialAmount());
                trans.setOpenAmount(paymentRequest.getVAOpenAmount());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.AlfaMVA) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                trans.setPartialAmount(paymentRequest.getVAPartialAmount());
                trans.setOpenAmount(paymentRequest.getVAOpenAmount());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Indomaret) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                trans.setPartialAmount(paymentRequest.getVAPartialAmount());
                trans.setOpenAmount(paymentRequest.getVAOpenAmount());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataMVA) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                trans.setPartialAmount(paymentRequest.getVAPartialAmount());
                trans.setOpenAmount(paymentRequest.getVAOpenAmount());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BNIVA) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                trans.setPartialAmount(paymentRequest.getVAPartialAmount());
                trans.setOpenAmount(paymentRequest.getVAOpenAmount());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.CIMBVA) {
                trans.setAccountId(paymentRequest.getPAYCODE());
                trans.setPartialAmount(paymentRequest.getVAPartialAmount());
                trans.setOpenAmount(paymentRequest.getVAOpenAmount());
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.CreditCard || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BNIDebitOnline || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BSP || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur) {

                //      trans.setAccountId(paymentRequest.maskCard(paymentRequest.getCARDNUMBER()));
                trans.setAccountId(paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));

                PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
                String publicKeyString = config.getString("ONECHECKOUT.PUBLICKEY." + paymentRequest.getMALLID(), "NOKEY").trim();

                try {
                    if (!publicKeyString.equalsIgnoreCase("NOKEY")) {

                        String ccen = OneCheckoutRSA.Encrypt(paymentRequest.getCARDNUMBER() + "," + paymentRequest.getEXPIRYDATE(), publicKeyString);
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions Save SystemSession : %s", trxHelper.getSystemSession() + "|" + ccen);

                        trans.setSystemSession(trxHelper.getSystemSession() + "|" + ccen);

                    }
                } catch (Exception ex) {
                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions : Exception " + ex.getMessage());
                }

                if (paymentRequest.getPROMOID() != null && !paymentRequest.getPROMOID().isEmpty()) {
                    trans.setIncPromoid(paymentRequest.getPROMOID());
                }

                if (paymentRequest.getINSTALLMENT_ACQUIRER() != null && !paymentRequest.getINSTALLMENT_ACQUIRER().isEmpty()) {
                    trans.setIncInstallmentAcquirer(paymentRequest.getINSTALLMENT_ACQUIRER());
                }

                if (paymentRequest.getTENOR() != null && !paymentRequest.getTENOR().isEmpty()) {
                    trans.setIncTenor(paymentRequest.getTENOR());
                }

                HashSet<TransactionsDataCardholder> chs = new HashSet<TransactionsDataCardholder>();
                TransactionsDataCardholder ch = new TransactionsDataCardholder();

                OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions : TransactionsDataCardholder");

                ch.setTransactionsId(nl.getNextVal(SeqTransactionsDataCardholderIdNextval.class).longValue());
                ch.setIncAddress(paymentRequest.getADDRESS() != null && paymentRequest.getADDRESS().length() > 0 ? paymentRequest.getADDRESS() : "");
                ch.setIncBirthdate(paymentRequest.getBIRTHDATE());
                ch.setIncCcName(paymentRequest.getCC_NAME() != null ? paymentRequest.getCC_NAME() : paymentRequest.getNAME());
                ch.setIncCity(paymentRequest.getCITY());
                ch.setIncCountry(paymentRequest.getCOUNTRY());
                ch.setIncHomephone(paymentRequest.getHOMEPHONE() != null && paymentRequest.getHOMEPHONE().length() > 0 ? paymentRequest.getHOMEPHONE() : "");
                ch.setIncMobilephone(paymentRequest.getMOBILEPHONE());
                ch.setIncState(paymentRequest.getSTATE());
                ch.setIncWorkphone(paymentRequest.getWORKPHONE());
                ch.setIncZipcode(paymentRequest.getZIPCODE());

                ch.setTransactions(trans);
                chs.add(ch);
                trans.setTransactionsDataCardholders(chs);
                trans.setTokenId(paymentRequest.getTOKENID());
//                trans.setAccountId(paymentRequest.getCUSTOMERID());
                trans.setDeviceId(paymentRequest.getDEVICEID());
                OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions :  CUSTOMERID=[%s]", paymentRequest.getCUSTOMERID());
                if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization) {
                    trans.setTokenId(paymentRequest.getTOKENID());
                    trans.setIncCustomerId(paymentRequest.getCUSTOMERID());
                }

                if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur) {
                    trans.setIncCustomerId(paymentRequest.getCUSTOMERID());
                    if (paymentRequest.getTOKENID() != null && !paymentRequest.getTOKENID().isEmpty()) {
                        trans.setTokenId(paymentRequest.getTOKENID());
                    }

                    //    Set<TransactionsDataRecur> dataRecurs = new HashSet<TransactionsDataRecur>();
                    TransactionsDataRecur dataRecur = new TransactionsDataRecur();
                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions :  trans.getIncCustomerId()=[%s]", trans.getIncCustomerId());
                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions : TransactionsDataRecur");

                    if (trans.getTokenId() == null || trans.getTokenId().isEmpty()) {
                        String data = KMCardUtils.encrypt(paymentRequest.getCARDNUMBER() + "|" + paymentRequest.getEXPIRYDATE()); //.Encrypt(paymentRequest.getCARDNUMBER()+","+paymentRequest.getEXPIRYDATE(), publicKeyString);
                        dataRecur.setIncAdditionalData1(data);
                    }

                    dataRecur.setTransactionsId(nl.getNextVal(SeqTransactionsDataRecurIdNextval.class).longValue());

                    dataRecur.setIncAdditionalData2("");
                    dataRecur.setIncBillDetail(paymentRequest.getBILLDETAIL());
                    dataRecur.setIncBillNumber(paymentRequest.getBILLNUMBER());
                    dataRecur.setIncBillType(paymentRequest.getBILLTYPE().charAt(0));
                    dataRecur.setIncEndDate(OneCheckoutVerifyFormatData.dateFormat.format(paymentRequest.getENDDATE()));
                    dataRecur.setIncExecuteDate(paymentRequest.getEXECUTEDATE());
                    dataRecur.setIncExecuteMonth(paymentRequest.getEXECUTEMONTH());
                    dataRecur.setIncExecuteType(paymentRequest.getEXECUTETYPE().charAt(0));
                    dataRecur.setIncStartDate(OneCheckoutVerifyFormatData.dateFormat.format(paymentRequest.getSTARTDATE()));

                    dataRecur.setTransactions(trans);
                    //  dataRecurs.add(dataRecur);
                    trans.setTransactionsDataRecur(dataRecur);

                }

            }

            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions 1=====================================");
            return trans;
        } catch (Exception ex) {
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions Exception : %s", ex.getMessage());
            ex.printStackTrace();
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.updateTransactions 2=====================================");
            return trans;
        }
    }

    public Transactions getTransactionById(OneCheckoutDataHelper helper) {
        Transactions trans = null;

        try {
            Criteria criteria = getSession().createCriteria(Transactions.class);

            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.getTransactionById start");
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.getTransactionById transactionId=[%s]", helper.getTrxId());
            criteria.add(Restrictions.eq("transactionsId", helper.getTrxId()));
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.getTransactionById end");
            trans = (Transactions) criteria.uniqueResult();
        } catch (Throwable t) {
            OneCheckoutLogger.log("OneCheckoutV1TransactionBean.getTransactionById Exception : %s", t.getMessage());
            t.printStackTrace();
        }

        return trans;
    }

    public Object load(Transactions trans, Class classParam) throws Exception {
        Object entity = null;

        try {
            Criteria criteria = getSession().createCriteria(classParam);

            criteria.add(Restrictions.eq("transactions", trans));

            entity = criteria.uniqueResult();
        } catch (Throwable t) {
            t.printStackTrace();
            OneCheckoutLogger.log("Error Get " + classParam.getName() + " By Transaction Id :" + trans.getTransactionsId() + " : " + t.getMessage());
        }

        return entity;
    }

    public boolean saveInvalidParamsTransactions(OneCheckoutDataHelper trxHelper) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            Merchants m = trxHelper.getMerchant();
            Transactions trans = null;

            if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {

                trans = new Transactions();
                trans.setTransactionsId(getTransactionNextval());

                // mandatory
                trans.setIncMallid(paymentRequest.getMALLID());
                trans.setIncChainmerchant(paymentRequest.getCHAINMERCHANT());
                trans.setIncAmount(BigDecimal.valueOf(paymentRequest.getAMOUNT()));
                trans.setIncPurchaseamount(BigDecimal.valueOf(paymentRequest.getPURCHASEAMOUNT()));

                trans.setIncTransidmerchant(paymentRequest.getTRANSIDMERCHANT());
                trans.setIncWords(paymentRequest.getWORDS());
                trans.setIncRequestdatetime(paymentRequest.getREQUESTDATETIME());
                trans.setIncCurrency(paymentRequest.getCURRENCY());
                trans.setIncPurchasecurrency(paymentRequest.getPURCHASECURRENCY());
                trans.setIncSessionid(paymentRequest.getSESSIONID());
                trans.setIncEmail(paymentRequest.getEMAIL());
                trans.setIncName(paymentRequest.getNAME());

                trans.setIncPromoid(paymentRequest.getPROMOID());
                trans.setIncInstallmentAcquirer(paymentRequest.getINSTALLMENT_ACQUIRER());
                trans.setIncTenor(paymentRequest.getTENOR());

                trans.setSystemSession(trxHelper.getSystemSession());
                trans.setTransactionsDatetime(new Date());
                trans.setIncAdditionalInformation(paymentRequest.getADDITIONALINFO());

            } else {
                trans = getTransactionById(trxHelper);
            }

            trans.setIncPaymentchannel(trxHelper.getPaymentChannel() == null ? "" : trxHelper.getPaymentChannel().value());

            if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {

                trans.setAccountId(paymentRequest.getDOKUPAYID() == null ? "" : paymentRequest.getDOKUPAYID());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DirectDebitMandiri) {
                trans.setAccountId(paymentRequest.getCARDNUMBER() == null ? "" : paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.KlikBCA) {
                trans.setAccountId(paymentRequest.getUSERIDKLIKBCA() == null ? "" : paymentRequest.getUSERIDKLIKBCA().toUpperCase());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVALite) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVAFull) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart) {
                trans.setAccountId(paymentRequest.getPAYCODE() == null ? "" : paymentRequest.getPAYCODE());

            } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.CreditCard || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BNIDebitOnline || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BSP || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur) {

                trans.setAccountId(paymentRequest.getCARDNUMBER() == null ? "" : paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));

                OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataCardholder");

                Set<TransactionsDataCardholder> chs = null;
                TransactionsDataCardholder ch = null;

                //if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                ch = new TransactionsDataCardholder();
                chs = new HashSet<TransactionsDataCardholder>();

                ch.setTransactionsId(nl.getNextVal(SeqTransactionsDataCardholderIdNextval.class).longValue());
                /*} else {
                 ch = (TransactionsDataCardholder) load(trans, TransactionsDataCardholder.class);
                 }*/

                ch.setIncAddress(paymentRequest.getADDRESS() == null ? "" : paymentRequest.getADDRESS());
                ch.setIncBirthdate(paymentRequest.getBIRTHDATE() == null ? "" : paymentRequest.getBIRTHDATE());
                //ch.setIncCcName(paymentRequest.getCC_NAME() == null ? "" : paymentRequest.getCC_NAME());
                ch.setIncCcName(paymentRequest.getCC_NAME() != null ? paymentRequest.getCC_NAME() : paymentRequest.getNAME() == null ? "" : paymentRequest.getNAME());
                ch.setIncCity(paymentRequest.getCITY() == null ? "" : paymentRequest.getCITY());
                ch.setIncCountry(paymentRequest.getCOUNTRY() == null ? "" : paymentRequest.getCOUNTRY());
                ch.setIncHomephone(paymentRequest.getHOMEPHONE() == null ? "" : paymentRequest.getHOMEPHONE());
                ch.setIncMobilephone(paymentRequest.getMOBILEPHONE() == null ? "" : paymentRequest.getMOBILEPHONE());
                ch.setIncState(paymentRequest.getSTATE() == null ? "" : paymentRequest.getSTATE());
                ch.setIncWorkphone(paymentRequest.getWORKPHONE() == null ? "" : paymentRequest.getWORKPHONE());
                ch.setIncZipcode(paymentRequest.getZIPCODE() == null ? "" : paymentRequest.getZIPCODE());

                //if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                ch.setTransactions(trans);
                chs.add(ch);
                trans.setTransactionsDataCardholders(chs);
                /*} else {
                 em.merge(ch);
                 }*/

            }

            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.NONAIRLINE.value()) {

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    Set<TransactionsDataNonAirlines> nonairlines = new HashSet<TransactionsDataNonAirlines>();
                    TransactionsDataNonAirlines nonairline = new TransactionsDataNonAirlines();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataNonAirlines");

                    nonairline.setTransactionsId(nl.getNextVal(SeqTransactionsDataNonAirlinesIdNextval.class).longValue());
                    nonairline.setIncBasket(paymentRequest.getBASKET());
                    nonairline.setIncShippingAddress(paymentRequest.getSHIPPING_ADDRESS());
                    nonairline.setIncShippingCity(paymentRequest.getSHIPPING_CITY());
                    nonairline.setIncShippingCountry(paymentRequest.getSHIPPING_COUNTRY());
                    nonairline.setIncShippingState(paymentRequest.getSHIPPING_STATE());
                    nonairline.setIncShippingZipcode(paymentRequest.getSHIPPING_ZIPCODE());

                    nonairline.setTransactions(trans);
                    nonairlines.add(nonairline);
                    trans.setTransactionsDataNonAirlineses(nonairlines);
                }

            } else if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    Set<TransactionsDataAirlines> airlines = new HashSet<TransactionsDataAirlines>();
                    TransactionsDataAirlines airline = new TransactionsDataAirlines();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataAirlines");

                    airline.setTransactionsId(nl.getNextVal(SeqTransactionsDataAirlinesNextval.class).longValue());
                    airline.setIncBookingcode(paymentRequest.getBOOKINGCODE());
                    airline.setIncFlight(paymentRequest.getFLIGHT().value());
                    airline.setIncFlightdate(paymentRequest.parseArray(paymentRequest.getFLIGHTDATE()));
                    airline.setIncFlightnumber(paymentRequest.parseArray(paymentRequest.getFLIGHTNUMBER()));
                    airline.setIncFlighttime(paymentRequest.parseArray(paymentRequest.getFLIGHTTIME()));
                    airline.setIncFlighttype(paymentRequest.getFLIGHTTYPE().value());

                    airline.setIncPassengerName(paymentRequest.parseArray(paymentRequest.getPASSENGER_NAME()));
                    airline.setIncPassengerType(paymentRequest.parseArray(paymentRequest.getPASSENGER_TYPE()));
                    airline.setIncRoute(paymentRequest.parseArray(paymentRequest.getROUTE()));
                    airline.setIncThirdpartyStatus(paymentRequest.getTHIRDPARTY_STATUS().value());

                    airline.setIncVat(BigDecimal.valueOf(paymentRequest.getVAT()));
                    airline.setIncInsurance(BigDecimal.valueOf(paymentRequest.getINSURANCE()));
                    airline.setIncFuelsurcharge(BigDecimal.valueOf(paymentRequest.getFUELSURCHARGE()));
                    airline.setIncFFNumber(paymentRequest.getFFNUMBER());

                    airline.setTransactions(trans);
                    airlines.add(airline);
                    trans.setTransactionsDataAirlineses(airlines);
                }

            }

            PaymentChannel dbChannel = nl.getPaymentChannelByChannel(trxHelper.getPaymentChannel());
            MerchantPaymentChannel mpc = nl.getMerchantPaymentChannel(trxHelper.getMerchant(), dbChannel);
            trans.setMerchantPaymentChannel(mpc);

            trans.setSystemSession(trxHelper.getSystemSession());
            trans.setTransactionsDatetime(new Date());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            if (trxHelper.getFlag() != null && trxHelper.getFlag().equalsIgnoreCase(OneCheckoutGeneralConstant.CANCEL_BY_USER)) {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.CANCEL_BY_USER.value());
            } else {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.ERROR_VALIDATE_INPUT.value());
            }

            if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                em.persist(trans);
            } else {
                em.merge(trans);
            }

            trxHelper.setTransactions(trans);
            trxHelper.setStepNotify(OneCheckoutStepNotify.CANCEL_BY_USER);
            Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

            OneCheckoutNotifyStatusResponse ack = trxHelper.getNotifyResponse();

            return true;
        } catch (Exception ex) {

            ex.printStackTrace();

            return false;
        }
    }

    public Transactions saveSettlementData(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc) {
        Transactions trans = null;
        try {

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            Merchants m = trxHelper.getMerchant();
            trans = trxHelper.getTransactions() == null ? new Transactions() : trxHelper.getTransactions();

            trans.setTransactionsId(getTransactionNextval());

            // mandatory
            trans.setTransactionsState(OneCheckoutTransactionState.SETTLEMENT.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            trans.setReconcileDateTime(new Date());

            trans.setDokuIssuerBank(trxHelper.getNotifyRequest().getBANK());
            trans.setDokuResponseCode("0000");
            trans.setDokuHostRefNum(trxHelper.getNotifyRequest().getHOSTREFNUM());
            trans.setDokuResult("");
            trans.setDokuResultMessage(OneCheckoutTransactionStatus.SUCCESS.name());

            trans.setAccountId(trxHelper.getNotifyRequest().getPAYCODE());
            trans.setIncPaymentchannel(mpc.getPaymentChannel().getPaymentChannelId());
            trans.setMerchantPaymentChannel(mpc);

            trans.setIncMallid(Integer.parseInt(trxHelper.getNotifyRequest().getMERCHANTCODE()));
            trans.setIncAmount(new BigDecimal(trxHelper.getNotifyRequest().getAMOUNT()));
            trans.setIncPurchaseamount(new BigDecimal(trxHelper.getNotifyRequest().getAMOUNT()));

            String generateIncoiceNumber = mpc.getPaymentChannel().getPaymentChannelId() + OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());

            trans.setIncTransidmerchant(generateIncoiceNumber);
            trans.setIncWords(trxHelper.getNotifyRequest().getWORDS());
            trans.setIncRequestdatetime(new Date());
            trans.setIncCurrency("360");
            trans.setIncPurchasecurrency("360");
            trans.setIncSessionid("");
            trans.setIncEmail("");
            trans.setIncName("");
            trans.setIncAdditionalInformation("");

            trans.setIncPromoid("");

            trans.setSystemMessage(trxHelper.getSystemMessage());
            trans.setSystemSession(trxHelper.getSystemSession());
            trans.setTransactionsDatetime(new Date());

            if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {

                trans.setIncPaymentchannel(trxHelper.getPaymentChannel().value());
                trans.setMerchantPaymentChannel(mpc);

                if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DOKUPAY) {

                    trans.setAccountId(paymentRequest.getDOKUPAYID());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.DirectDebitMandiri) {
                    trans.setAccountId(paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.KlikBCA) {
                    trans.setAccountId(paymentRequest.getUSERIDKLIKBCA() != null ? paymentRequest.getUSERIDKLIKBCA().toUpperCase() : paymentRequest.getUSERIDKLIKBCA());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVALite) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVAFull) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart) {
                    trans.setAccountId(paymentRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.CreditCard || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BNIDebitOnline || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.BSP || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Tokenization || trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Recur) {

                    trans.setAccountId(paymentRequest.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));

                    Set<TransactionsDataCardholder> chs = new HashSet<TransactionsDataCardholder>();
                    TransactionsDataCardholder ch = new TransactionsDataCardholder();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataCardholder");

                    ch.setTransactionsId(nl.getNextVal(SeqTransactionsDataCardholderIdNextval.class).longValue());
                    ch.setIncAddress(paymentRequest.getADDRESS());
                    ch.setIncBirthdate(paymentRequest.getBIRTHDATE());
                    //ch.setIncCcName(paymentRequest.getCC_NAME());
                    ch.setIncCcName(paymentRequest.getCC_NAME() == null ? paymentRequest.getNAME() : paymentRequest.getCC_NAME());
                    ch.setIncCity(paymentRequest.getCITY());
                    ch.setIncCountry(paymentRequest.getCOUNTRY());
                    ch.setIncHomephone(paymentRequest.getHOMEPHONE());
                    ch.setIncMobilephone(paymentRequest.getMOBILEPHONE());
                    ch.setIncState(paymentRequest.getSTATE());
                    ch.setIncWorkphone(paymentRequest.getWORKPHONE());
                    ch.setIncZipcode(paymentRequest.getZIPCODE());

                    ch.setTransactions(trans);
                    chs.add(ch);
                    trans.setTransactionsDataCardholders(chs);

                }

            } else {
                trans.setAccountId("");
                trans.setIncPaymentchannel("");
                trans.setMerchantPaymentChannel(null);
            }

            if (m.getMerchantCategory() != null) {
                if (m.getMerchantCategory() == OneCheckoutMerchantCategory.NONAIRLINE.value()) {

                    Set<TransactionsDataNonAirlines> nonairlines = new HashSet<TransactionsDataNonAirlines>();
                    TransactionsDataNonAirlines nonairline = new TransactionsDataNonAirlines();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataNonAirlines");

                    nonairline.setTransactionsId(nl.getNextVal(SeqTransactionsDataNonAirlinesIdNextval.class).longValue());
                    nonairline.setIncBasket("");
                    nonairline.setIncShippingAddress("");
                    nonairline.setIncShippingCity("");
                    nonairline.setIncShippingCountry("");
                    nonairline.setIncShippingState("");
                    nonairline.setIncShippingZipcode("");

                    nonairline.setTransactions(trans);
                    nonairlines.add(nonairline);
                    trans.setTransactionsDataNonAirlineses(nonairlines);

                } else if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {

                    Set<TransactionsDataAirlines> airlines = new HashSet<TransactionsDataAirlines>();
                    TransactionsDataAirlines airline = new TransactionsDataAirlines();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataAirlines");

                    airline.setTransactionsId(nl.getNextVal(SeqTransactionsDataAirlinesNextval.class).longValue());
                    airline.setIncBookingcode("");
                    airline.setIncFlight("");
                    airline.setIncFlightdate("");
                    airline.setIncFlightnumber("");
                    airline.setIncFlighttime("");
                    airline.setIncFlighttype(new Character('U'));

                    airline.setIncPassengerName("");
                    airline.setIncPassengerType("");
                    airline.setIncRoute("");
                    airline.setIncThirdpartyStatus(new Character('U'));

                    airline.setIncVat(BigDecimal.valueOf(0.0));
                    airline.setIncInsurance(BigDecimal.valueOf(0.0));
                    airline.setIncFuelsurcharge(BigDecimal.valueOf(0.0));
                    airline.setIncFFNumber("");

                    airline.setTransactions(trans);
                    airlines.add(airline);
                    trans.setTransactionsDataAirlineses(airlines);

                }
            }

            em.persist(trans);

            return trans;
        } catch (Exception ex) {

            ex.printStackTrace();

            return trans;
        }
    }

    public Transactions saveNotFoundInquiryTransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc) {
        Transactions trans = null;
        try {

            OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();
            Merchants m = trxHelper.getMerchant();
            trans = trxHelper.getTransactions() == null ? new Transactions() : trxHelper.getTransactions();

            trans.setTransactionsId(getTransactionNextval());

            // mandatory
            trans.setTransactionsState(OneCheckoutTransactionState.INCOMING.value());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            trans.setReconcileDateTime(new Date());

            trans.setDokuIssuerBank(verifyRequest.getBANK());
            trans.setDokuResponseCode("0000");
            trans.setDokuHostRefNum("");
            trans.setDokuResult("");
            trans.setDokuResultMessage(OneCheckoutTransactionStatus.SUCCESS.name());

            trans.setAccountId(verifyRequest.getPAYCODE());
            trans.setIncPaymentchannel(mpc.getPaymentChannel().getPaymentChannelId());
            trans.setMerchantPaymentChannel(mpc);

            trans.setIncMallid(Integer.parseInt(verifyRequest.getMALLID()));
            trans.setIncAmount(new BigDecimal(verifyRequest.getAMOUNT()));
            trans.setIncPurchaseamount(new BigDecimal(verifyRequest.getAMOUNT()));

            String generateIncoiceNumber = mpc.getPaymentChannel().getPaymentChannelId() + OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());

            trans.setIncTransidmerchant(generateIncoiceNumber);
            trans.setIncWords(verifyRequest.getWORDS());
            trans.setIncRequestdatetime(new Date());
            trans.setIncCurrency("360");
            trans.setIncPurchasecurrency("360");
            trans.setIncSessionid("");
            trans.setIncEmail("");
            trans.setIncName("");

            trans.setIncPromoid("");
            trans.setIncAdditionalInformation("");

            trans.setSystemMessage(trxHelper.getSystemMessage());
            trans.setSystemSession(trxHelper.getSystemSession());
            trans.setTransactionsDatetime(new Date());

            if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {

                trans.setIncPaymentchannel(trxHelper.getPaymentChannel().value());
                trans.setMerchantPaymentChannel(mpc);

                if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVALite) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PermataVAFull) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVALite) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.SinarMasVAFull) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.PTPOS) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOALite) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriSOAFull) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVALite) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.MandiriVAFull) {
                    trans.setAccountId(verifyRequest.getPAYCODE());

                } else if (trxHelper.getPaymentChannel() == OneCheckoutPaymentChannel.Alfamart) {
                    trans.setAccountId(verifyRequest.getPAYCODE());
                }

            } else {
                trans.setAccountId("");
                trans.setIncPaymentchannel("");
                trans.setMerchantPaymentChannel(null);
            }

            if (m.getMerchantCategory() != null) {
                if (m.getMerchantCategory() == OneCheckoutMerchantCategory.NONAIRLINE.value()) {

                    Set<TransactionsDataNonAirlines> nonairlines = new HashSet<TransactionsDataNonAirlines>();
                    TransactionsDataNonAirlines nonairline = new TransactionsDataNonAirlines();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataNonAirlines");

                    nonairline.setTransactionsId(nl.getNextVal(SeqTransactionsDataNonAirlinesIdNextval.class).longValue());
                    nonairline.setIncBasket("");
                    nonairline.setIncShippingAddress("");
                    nonairline.setIncShippingCity("");
                    nonairline.setIncShippingCountry("");
                    nonairline.setIncShippingState("");
                    nonairline.setIncShippingZipcode("");

                    nonairline.setTransactions(trans);
                    nonairlines.add(nonairline);
                    trans.setTransactionsDataNonAirlineses(nonairlines);

                } else if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {

                    Set<TransactionsDataAirlines> airlines = new HashSet<TransactionsDataAirlines>();
                    TransactionsDataAirlines airline = new TransactionsDataAirlines();

                    OneCheckoutLogger.log("OneCheckoutV1TransactionBean.saveTransactions : TransactionsDataAirlines");

                    airline.setTransactionsId(nl.getNextVal(SeqTransactionsDataAirlinesNextval.class).longValue());
                    airline.setIncBookingcode("");
                    airline.setIncFlight("");
                    airline.setIncFlightdate("");
                    airline.setIncFlightnumber("");
                    airline.setIncFlighttime("");
                    airline.setIncFlighttype(new Character('U'));

                    airline.setIncPassengerName("");
                    airline.setIncPassengerType("");
                    airline.setIncRoute("");
                    airline.setIncThirdpartyStatus(new Character('U'));

                    airline.setIncVat(BigDecimal.valueOf(0.0));
                    airline.setIncInsurance(BigDecimal.valueOf(0.0));
                    airline.setIncFuelsurcharge(BigDecimal.valueOf(0.0));
                    airline.setIncFFNumber("");

                    airline.setTransactions(trans);
                    airlines.add(airline);
                    trans.setTransactionsDataAirlineses(airlines);

                }
            }

            em.persist(trans);

            return trans;
        } catch (Exception ex) {

            ex.printStackTrace();

            return trans;
        }
    }

    public Transactions createReuseVATransactions(OneCheckoutDataHelper trxHelper, MerchantPaymentChannel mpc, Transactions transInit) {
        Transactions transNew = null;
        try {

            //    OneCheckoutDOKUVerifyData verifyRequest = trxHelper.getVerifyRequest();
            Merchants m = trxHelper.getMerchant();

            transNew = new Transactions();

            transNew.setTransactionsId(getTransactionNextval());
            transNew.setAccountId(transInit.getAccountId() == null ? "" : transInit.getAccountId());
            transNew.setIncAdditionalInformation(transInit.getIncAdditionalInformation() == null ? "" : transInit.getIncAdditionalInformation());
            transNew.setIncAmount(transInit.getIncAmount());
            transNew.setIncChainmerchant(transInit.getIncChainmerchant());
            transNew.setIncCurrency(transInit.getIncCurrency());
            transNew.setIncEmail(transInit.getIncEmail() == null ? "" : transInit.getIncEmail());
            transNew.setIncInstallmentAcquirer(transInit.getIncInstallmentAcquirer() == null ? "" : transInit.getIncInstallmentAcquirer());
            transNew.setIncLiability(transInit.getIncLiability() == null ? "" : transInit.getIncLiability());
            transNew.setIncMallid(transInit.getIncMallid());
            transNew.setIncName(transInit.getIncName() == null ? "" : transInit.getIncName());
            transNew.setIncPaymentchannel(transInit.getIncPaymentchannel());
            transNew.setIncPromoid(transInit.getIncPromoid() == null ? "" : transInit.getIncPromoid());
            transNew.setIncPurchaseamount(transInit.getIncPurchaseamount());
            transNew.setIncPurchasecurrency(transInit.getIncPurchasecurrency());
            transNew.setIncRequestdatetime(new Date());
            transNew.setIncSessionid(transInit.getIncSessionid());
            transNew.setIncTenor(transInit.getIncTenor() == null ? "" : transInit.getIncTenor());

            String incTransidmerchant = m.getMerchantCode() + OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());

            transNew.setIncTransidmerchant(incTransidmerchant);
            transNew.setIncWords("");
            transNew.setMerchantPaymentChannel(mpc);
            //   transNew.setReconcileDateTime(reconcileDateTime);
            //   transNew.setRedirectDatetime(redirectDatetime);
            //    transNew.setSystemMessage(systemMessage);
            //    transNew.setSystemSession(systemSession);
            //     transInit.getTransactionsDataAirlineses().
            //      transNew.setTransactionsDataAirlineses(transInit.getTransactionsDataAirlineses());
            //     transNew.setTransactionsDataCardholders(transInit.getTransactionsDataCardholders());
            //      transNew.setTransactionsDataNonAirlineses(transInit.getTransactionsDataNonAirlineses());
            //       transNew.setTransactionsDatetime(transactionsDatetime);

            transNew.setTransactionsState(OneCheckoutTransactionState.INCOMING.value());
            transNew.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            //     transNew.setVerifyDatetime(verifyDatetime);
            //     transNew.setVerifyId(verifyId);
            transNew.setVerifyScore(-1);
            transNew.setVerifyStatus(OneCheckoutDFSStatus.NA.value());

            if (m.getMerchantCategory() != null) {
                if (m.getMerchantCategory() == OneCheckoutMerchantCategory.NONAIRLINE.value()) {

                    Set<TransactionsDataNonAirlines> nonairlinesInit = transInit.getTransactionsDataNonAirlineses();
                    TransactionsDataNonAirlines nonairline = new TransactionsDataNonAirlines();

                    TransactionsDataNonAirlines f = null;
                    for (Iterator<TransactionsDataNonAirlines> ilist = nonairlinesInit.iterator(); ilist.hasNext();) {
                        f = ilist.next();
                    }

                    if (f != null) {
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.createReuseVATransactions : TransactionsDataNonAirlines");

                        nonairline.setTransactionsId(nl.getNextVal(SeqTransactionsDataNonAirlinesIdNextval.class).longValue());
                        nonairline.setIncBasket(f.getIncBasket());
                        nonairline.setIncShippingAddress(f.getIncShippingAddress());
                        nonairline.setIncShippingCity(f.getIncShippingCity());
                        nonairline.setIncShippingCountry(f.getIncShippingCity());
                        nonairline.setIncShippingState(f.getIncShippingState());
                        nonairline.setIncShippingZipcode(f.getIncShippingZipcode());

                        nonairline.setTransactions(transNew);
                        Set<TransactionsDataNonAirlines> nonairlines = new HashSet<TransactionsDataNonAirlines>();
                        nonairlines.add(nonairline);
                        transNew.setTransactionsDataNonAirlineses(nonairlines);

                    }

                } else if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {

                    Set<TransactionsDataAirlines> airlinesInit = transInit.getTransactionsDataAirlineses();
                    TransactionsDataAirlines airline = new TransactionsDataAirlines();

                    TransactionsDataAirlines f = null;
                    for (Iterator<TransactionsDataAirlines> ilist = airlinesInit.iterator(); ilist.hasNext();) {
                        f = ilist.next();
                    }

                    if (f != null) {
                        OneCheckoutLogger.log("OneCheckoutV1TransactionBean.createReuseVATransactions : TransactionsDataAirlines");

                        airline.setTransactionsId(nl.getNextVal(SeqTransactionsDataAirlinesNextval.class).longValue());
                        airline.setIncBookingcode(f.getIncBookingcode());
                        airline.setIncFlight(f.getIncFlight());
                        airline.setIncFlightdate(f.getIncFlightdate());
                        airline.setIncFlightnumber(f.getIncFlightnumber());
                        airline.setIncFlighttime(f.getIncFlighttime());
                        airline.setIncFlighttype(f.getIncFlighttype());

                        airline.setIncPassengerName(f.getIncPassengerName());
                        airline.setIncPassengerType(f.getIncPassengerType());
                        airline.setIncRoute(f.getIncRoute());
                        airline.setIncThirdpartyStatus(f.getIncThirdpartyStatus());

                        airline.setIncVat(f.getIncVat());
                        airline.setIncInsurance(f.getIncInsurance());
                        airline.setIncFuelsurcharge(f.getIncFuelsurcharge());
                        airline.setIncFFNumber(f.getIncFFNumber());

                        airline.setTransactions(transNew);
                        Set<TransactionsDataAirlines> airlines = new HashSet<TransactionsDataAirlines>();
                        airlines.add(airline);
                        transNew.setTransactionsDataAirlineses(airlines);
                    }

                }
            }

            em.persist(transNew);

            return transNew;
        } catch (Exception ex) {

            ex.printStackTrace();

            return transNew;
        }
    }

    private String generateOcoId(String paymentChannel) {
        String result = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyhhmmssSSS");
            String dateFormatResult = dateFormat.format(new Date());

            Random rn = new Random();
            int range = 999 - 0 + 1;
            int randomNum = rn.nextInt(range) + 0;

            result = dateFormatResult + randomNum + paymentChannel;

        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("Error GenerateOcoId = " + th.getMessage());
        }
        return result;
    }

    public Transactions saveTrannsactionRefund(Transactions transactionsDB, String amount) {
        Transactions trans = new Transactions();
        try {
//            trans.setTransactionsId(getTransactionNextval());
//            trans.setParentTransactions(transactionsDB.getTransactionsId());
//
//            //mandatory
//            trans.setIncMallid(transactionsDB.getIncMallid());
//            trans.setIncChainmerchant(transactionsDB.getIncChainmerchant());
//            trans.setIncAmount(transactionsDB.getIncAmount());
//            trans.setIncPurchaseamount(transactionsDB.getIncPurchaseamount());
//            trans.setIncTransidmerchant(transactionsDB.getIncTransidmerchant());
//            trans.setIncWords(transactionsDB.getIncWords());
//            trans.setIncRequestdatetime(new Date());
//            trans.setIncCurrency(transactionsDB.getIncCurrency());
//            trans.setIncPurchasecurrency(transactionsDB.getIncPurchasecurrency());
//            trans.setIncSessionid(transactionsDB.getIncSessionid());
//            trans.setIncEmail(transactionsDB.getIncEmail());
//            trans.setIncPaymentchannel(transactionsDB.getIncPaymentchannel());
//            trans.setOcoId(transactionsDB.getOcoId());
//            trans.setMerchantPaymentChannel(transactionsDB.getMerchantPaymentChannel());
//
//            trans.setAccountId(transactionsDB.getAccountId());
//            trans.setIncName(transactionsDB.getIncName());
//            trans.setBatchId(transactionsDB.getBatchId());
//
//            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
//            trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
//            trans.setTransactionsType(OneCheckOutTransactionsType.REFUND.value());
//
//            em.persist(trans);

            trans.setTransactionsId(getTransactionNextval());

            if (transactionsDB.getParentTransactions() != null) {
                trans.setParentTransactions(transactionsDB.getParentTransactions());
            } else {
                trans.setParentTransactions(transactionsDB.getTransactionsId());
            }

            //mandatory
            trans.setIncMallid(transactionsDB.getIncMallid());
            trans.setIncChainmerchant(transactionsDB.getIncChainmerchant());
            if (amount != null) {
            trans.setIncAmount(BigDecimal.valueOf(Double.valueOf(amount)));    
            }
            
            trans.setIncPurchaseamount(transactionsDB.getIncPurchaseamount());
            trans.setIncTransidmerchant(transactionsDB.getIncTransidmerchant());
            trans.setIncWords(transactionsDB.getIncWords());
            trans.setIncRequestdatetime(new Date());
            trans.setIncCurrency(transactionsDB.getIncCurrency());
            trans.setIncPurchasecurrency(transactionsDB.getIncPurchasecurrency());
            trans.setIncSessionid(transactionsDB.getIncSessionid());
            trans.setIncEmail(transactionsDB.getIncEmail());
            trans.setIncPaymentchannel(transactionsDB.getIncPaymentchannel());
            trans.setIncInstallmentAcquirer(transactionsDB.getIncInstallmentAcquirer());
            trans.setIncTenor(transactionsDB.getIncTenor());
            trans.setIncPromoid(transactionsDB.getIncPromoid());
            trans.setIncAdditionalInformation(transactionsDB.getIncAdditionalInformation());
            trans.setAccountId(transactionsDB.getAccountId());
            trans.setSystemSession(transactionsDB.getSystemSession());
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setTransactionsDatetime(new Date());
            trans.setDokuInquiryInvoiceDatetime(transactionsDB.getDokuInquiryInvoiceDatetime());
            trans.setDokuInvokeStatusDatetime(transactionsDB.getDokuInvokeStatusDatetime());
            trans.setDokuResponseCode(transactionsDB.getDokuResponseCode());
            trans.setDokuApprovalCode(transactionsDB.getDokuApprovalCode());
            trans.setDokuResultMessage(transactionsDB.getDokuResultMessage());
            trans.setDokuIssuerBank(transactionsDB.getDokuIssuerBank());
            trans.setDokuResult(transactionsDB.getDokuResult());
            trans.setEduGetdataDatetime(transactionsDB.getEduGetdataDatetime());
            trans.setEduPassVoidDatetime(transactionsDB.getEduPassVoidDatetime());
            trans.setEduStatus(transactionsDB.getEduStatus());
            trans.setEduReason(transactionsDB.getEduReason());
            trans.setRedirectDatetime(transactionsDB.getRedirectDatetime());
            trans.setSystemMessage(transactionsDB.getSystemMessage());
            trans.setVerifyScore(transactionsDB.getVerifyScore());
            trans.setVerifyStatus(transactionsDB.getVerifyStatus());
            trans.setVerifyId(transactionsDB.getVerifyId());
            trans.setVerifyDatetime(transactionsDB.getVerifyDatetime());
            trans.setDokuVoidResponseCode(transactionsDB.getDokuVoidResponseCode());
            trans.setDokuVoidApprovalCode(transactionsDB.getDokuApprovalCode());
            trans.setDokuVoidDatetime(transactionsDB.getDokuVoidDatetime());
            trans.setIncName(transactionsDB.getIncName());
            trans.setMerchantPaymentChannel(transactionsDB.getMerchantPaymentChannel());
            trans.setDokuHostRefNum(transactionsDB.getDokuHostRefNum());
            trans.setReconcileDateTime(transactionsDB.getReconcileDateTime());
            trans.setInc3dSecureStatus(transactionsDB.getInc3dSecureStatus());
            trans.setIncLiability(transactionsDB.getIncLiability());
            trans.setIncCustomerId(transactionsDB.getIncCustomerId());
            trans.setTokenId(transactionsDB.getTokenId());
            trans.setDeviceId(transactionsDB.getDeviceId());
            trans.setRates(transactionsDB.getRates());
            trans.setConvertedAmount(transactionsDB.getConvertedAmount());
            trans.setMerchantNotificationResponse(transactionsDB.getMerchantNotificationResponse());
            trans.setOcoId(transactionsDB.getOcoId());
//            trans.setPairingCode(transactionsDB.getPairingCode());
//            trans.setExpiryTimeInMinutes(transactionsDB.getExpiryTimeInMinutes());

            trans.setTrxCodeCore(transactionsDB.getTrxCodeCore());

//            trans.setIncSubPaymentchannel(transactionsDB.getIncSubPaymentchannel());
//            trans.setCaptureExpiryTimeInMinutes(transactionsDB.getCaptureExpiryTimeInMinutes());
//            trans.setBatchId(transactionsDB.getBatchId());
//            trans.setSettlementAmount(transactionsDB.getSettlementAmount());
            trans.setTransactionsType(OneCheckOutTransactionsType.REFUND.value());
            trans.setIncName(transactionsDB.getIncName());

            em.persist(trans);

        } catch (Throwable th) {
            OneCheckoutLogger.log("Error saveTrannsactionRefund [ " + th.getMessage() + " ]");
            th.printStackTrace();
        }

        return trans;
    }

}
