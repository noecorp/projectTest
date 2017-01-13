/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.scheduler.SendEmailVAToCustomerBean;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import doku.virtualaccount.wsdl.MerchantVA;
import doku.virtualaccount.xml.VARegistrationParamDocument;
import doku.virtualaccount.xml.VARegistrationResponseDocument;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.xml.ws.BindingProvider;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
@Stateless
public class OneCheckoutV1BRIBean extends OneCheckoutChannelBase implements OneCheckoutV1BRIBeanLocal{
    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIVaBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();

            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
            String invoiceNo = paymentRequest.getTRANSIDMERCHANT();
            boolean request_good = identrx.isRequestGood();
            String statusCode = identrx.getStatusCode();
            MerchantPaymentChannel mpc = identrx.getMpc();
            boolean needNotify = identrx.isNeedNotify();
            OneCheckoutErrorMessage errormsg = identrx.getErrorMsg();

            if (request_good) {
                OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
//                try {
//                    VARegistrationResponseDocument.VARegistrationResponse vaRegistrationResponse = registerVa(trxHelper);
//                    if (vaRegistrationResponse != null) {
//                        trxHelper.setSystemMessage(vaRegistrationResponse.getResponseCode());
//                    }
//
//                } catch (Throwable t) {
//                }

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }

                //set data redirect yang akan di kirim ke merchant
//                redirect.setPageTemplate("alfamartvalite_input.html");
                redirect.setPageTemplate("BRIResultPaycode.html");
                redirect.setUrlAction(config.getString("URL.GOBACK"));
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(statusCode);
                HashMap<String, String> data = redirect.getParameters();

                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                data.put("WORDS", super.generateMerchantRedirectWordsBeforePayment(paymentRequest, m));
                data.put("STATUSCODE", OneCheckoutErrorMessage.NOT_YET_PAID.value());
                data.put("PAYMENTCHANNEL", OneCheckoutPaymentChannel.BRIVA.value());
                data.put("SESSIONID", paymentRequest.getSESSIONID());
                data.put("VAINITNUMBER", paymentRequest.getVAinitNumberFormat());
                data.put("PAYMENTCODE", paymentRequest.getPAYCODE());
                data.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());
                data.put("EMAIL", trans.getIncEmail());
//                data.put("VAINITNUMBER", mpc.getVAInitNumber());
                //data.put("OCOID", String.valueOf(trans.getTransactionsId()));
                data.put("DEVICEID", paymentRequest.getDEVICEID() != null ? paymentRequest.getDEVICEID() : "");
//                data.put("UA_BROWSER", paymentRequest.getUA_BROWSER() != null ? paymentRequest.getUA_BROWSER() : "");
//                data.put("UA_OS", paymentRequest.getUA_OS() != null ? paymentRequest.getUA_OS() : "");
//                data.put("UA_OSVERSION", paymentRequest.getUA_OSVERSION() != null ? paymentRequest.getUA_OSVERSION() : "");
//                data.put("UA_SCREENPRINT", paymentRequest.getUA_SCREENPRINT() != null ? paymentRequest.getUA_SCREENPRINT() : "");
//                data.put("UA_LOWERCASE", paymentRequest.getUA_LOWERCASE() != null ? paymentRequest.getUA_LOWERCASE() : "");
                data.put("USERNAME", paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");
                data.put("CUSTIP", paymentRequest.getCUSTIP());
                data.put("PAYREASON", paymentRequest.getPAYREASON());
                //String ocoId = this.generateOcoId(pChannel.getPaymentChannelId());
                data.put("OCOID", trxHelper.getOcoId());
                trxHelper.setMessage("VALID");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

                SendEmailVAToCustomerBean sendEmailVAToCustomerBean = new SendEmailVAToCustomerBean();
                sendEmailVAToCustomerBean.sendEmailVAInfo(trans, config, m, trxHelper);

            } else {

                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);

            }

            OneCheckoutLogger.log("OneCheckoutV1BRIVaBean.doPayment - T2: %d Finish", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public VARegistrationResponseDocument.VARegistrationResponse registerVa(OneCheckoutDataHelper trxHelper) {

        try {

            StringBuilder sb = new StringBuilder();

            doku.virtualaccount.wsdl.MerchantVAService service = new doku.virtualaccount.wsdl.MerchantVAService();
            MerchantVA port = service.getMerchantVAPort();

            BindingProvider bp = (BindingProvider) port;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, config.getString("onecheckout.wsdlurl", "http://localhost:8080/VA/MerchantVAService"));
            VARegistrationParamDocument registrationParamDoc = VARegistrationParamDocument.Factory.newInstance();
            VARegistrationParamDocument.VARegistrationParam registrationParam = registrationParamDoc.addNewVARegistrationParam();

            String payCode = trxHelper.getPaymentRequest().getPAYCODE();
            payCode = payCode.substring((payCode.length() - 8), payCode.length());

            VARegistrationParamDocument.VARegistrationParam.VirtualAccount va = registrationParam.addNewVirtualAccount();
            va.setVirtualAccountId(payCode);
            va.setCurrency(trxHelper.getPaymentRequest().getPURCHASECURRENCY());
            va.addNewInfo();

            // trxHelper.getMerchant().getMerchantName()
            String customerName = trxHelper.getPaymentRequest().getNAME();

            if (customerName != null && customerName.length() > 19) {
                customerName = customerName.substring(0, 19);
            }

            va.getInfo().addDetail(customerName);

            //registrationParam.setVirtualAccountArray(new VARegistrationParam.VirtualAccount[1]);
            registrationParam.setMerchantId(String.valueOf(trxHelper.getPaymentChannelCode()));
            registrationParam.setUniqueId(payCode);

            /*registrationParam.getVirtualAccountArray()[1].setVirtualAccountId(trxHelper.getPaymentRequest().getPAYCODE());
             registrationParam.getVirtualAccountArray()[1].setCurrency(trxHelper.getPaymentRequest().getPURCHASECURRENCY());
             registrationParam.getVirtualAccountArray()[1].addNewInfo();
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail(trxHelper.getMerchant().getMerchantName());
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");
             registrationParam.getVirtualAccountArray()[1].getInfo().addDetail("");*/
            OneCheckoutLogger.log("::before paymentResponse");
            OneCheckoutLogger.log(":: Value Of = " + registrationParamDoc.toString());
            String paymentResponse = port.registerVirtualAccount(registrationParamDoc.toString());

            VARegistrationResponseDocument vaRegistrationResponseDoc = VARegistrationResponseDocument.Factory.parse(paymentResponse);
            VARegistrationResponseDocument.VARegistrationResponse vaRegistrationResponse = vaRegistrationResponseDoc.getVARegistrationResponse();

            return vaRegistrationResponse;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
        voidRequest.setVOIDRESPONSE("FAILED");
        OneCheckoutLogger.log("OneCheckoutV1BRIVaBean.doVoid : Void is not permitted");

        return trxHelper;
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1BRIVaBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();
            Transactions trans = queryHelper.getRedirectTransactionBy(redirectRequest.getTRANSIDMERCHANT(), redirectRequest.getSESSIONID(), redirectRequest.getAMOUNT(), redirectRequest.getPAYMENTCODE(), redirectRequest.getPAYMENTCHANNEL());
            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {
                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.NOT_YET_PAID.name());
            } else {
                trans.setDokuResponseCode(trans.getDokuResponseCode() != null ? trans.getDokuResponseCode() : redirectRequest.getSTATUSCODE());
            }

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            redirect.setUrlAction(m.getMerchantRedirectUrl());
            redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
            redirect.setSTATUSCODE(trans.getDokuResponseCode());
            Map data = redirect.getParameters();
            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());
            data.put("TRANSIDMERCHANT", redirectRequest.getTRANSIDMERCHANT());
            data.put("STATUSCODE", trans.getDokuResponseCode());
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", redirectRequest.getSESSIONID());
            data.put("PAYMENTCODE", redirectRequest.getPAYMENTCODE());
            data.put("PURCHASECURRENCY", trans.getIncPurchasecurrency());//getPURCHASECURRENCY());
            if (trans.getRates() != null) {
                redirect.setAMOUNT(trans.getIncPurchaseamount().doubleValue());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
                data.put("CURRENCY", trans.getIncPurchasecurrency());//.getPURCHASECURRENCY());
            } else {
                redirect.setAMOUNT(trans.getIncAmount().doubleValue());//.getAMOUNT());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
                data.put("CURRENCY", trans.getIncCurrency());

            }
            data.put("WORDS", super.generateRedirectWords(redirect, m));

            OneCheckoutLogger.log("============== START REDIRECT PARAMETER ==============");
            OneCheckoutLogger.log("MERCHANT URL         [" + m.getMerchantRedirectUrl() + "]");
            OneCheckoutLogger.log("AMOUNT               [" + data.get("AMOUNT") + "]");
            OneCheckoutLogger.log("TRANSIDMERCHANT      [" + data.get("TRANSIDMERCHANT") + "]");
            OneCheckoutLogger.log("WORDS                [" + data.get("WORDS") + "]");
            OneCheckoutLogger.log("STATUSCODE           [" + data.get("STATUSCODE") + "]");
            OneCheckoutLogger.log("PAYMENTCHANNEL       [" + data.get("PAYMENTCHANNEL") + "]");
            OneCheckoutLogger.log("SESSIONID            [" + data.get("SESSIONID") + "]");
            OneCheckoutLogger.log("PAYMENTCODE          [" + data.get("PAYMENTCODE") + "]");
            OneCheckoutLogger.log("CURRENCY             [" + data.get("CURRENCY") + "]");
            OneCheckoutLogger.log("PURCHASECURRENCY     [" + data.get("PURCHASECURRENCY") + "]");
            OneCheckoutLogger.log("============== END  REDIRECT  PARAMETER ==============");
            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);
            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1BRIVaBean.doRedirectToMerchant - T1: %d Register DOKUWallet", (System.currentTimeMillis() - t1));
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
            redirect = super.addDWRegistrationPage(trxHelper, redirect, channel, trans.getMerchantPaymentChannel(), trans);
            redirect.setPageTemplate("redirect.html");
            OneCheckoutLogger.log("OneCheckoutV1BRIVaBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    

}
