/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutCheckStatusData;
import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.EmailUtility;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutEmailType;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import doku.virtualaccount.wsdl.MerchantVA;
import doku.virtualaccount.xml.VARegistrationParamDocument;
import doku.virtualaccount.xml.VARegistrationParamDocument.VARegistrationParam;
import doku.virtualaccount.xml.VARegistrationResponseDocument;
import doku.virtualaccount.xml.VARegistrationResponseDocument.VARegistrationResponse;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author aditya
 */
@Stateless
public class OneCheckoutV1MandiriVALiteBean extends OneCheckoutChannelBase implements OneCheckoutV1MandiriVALiteBeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;

    private boolean ableToReversal = false;  
    
    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

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

                try {
                    // Register To Virtual Account Before Save Databases
                    VARegistrationResponse vaRegistrationResponse = registerVa(trxHelper);

                    if (vaRegistrationResponse != null) {
                        trxHelper.setSystemMessage(vaRegistrationResponse.getResponseCode());
                    }

                } catch (Throwable t) {
                }

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }

                //set data redirect yang akan di kirim ke merchant

                redirect.setPageTemplate("mandirivalite_input.html");
                //redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setUrlAction(config.getString("URL.GOBACK"));
                redirect.setAMOUNT(paymentRequest.getAMOUNT());
                redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
                redirect.setSTATUSCODE(statusCode);
                HashMap<String, String> data = redirect.getParameters();

                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                data.put("WORDS", super.generateMerchantRedirectWordsBeforePayment(paymentRequest, m));
                //data.put("MERCHANTID", mpc.getMerchantPaymentChannelUid());
                //data.put("MALLID", m.getMerchantCode() + "");
                //data.put("MALLID", mpc.getPaymentChannelCode() + "");
                data.put("STATUSCODE", OneCheckoutErrorMessage.NOT_YET_PAID.value());
                data.put("PAYMENTCHANNEL", OneCheckoutPaymentChannel.MandiriVALite.value());
                data.put("SESSIONID", paymentRequest.getSESSIONID());
                data.put("PAYMENTCODE", paymentRequest.getPAYCODE());
                data.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());
//                String paymentChannelId = pChannel.getPaymentChannelId();
//                String ocoId = this.generateOcoId(paymentChannelId);
                data.put("OCOID", trans.getOcoId());
                trxHelper.setMessage("VALID");
                trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

                trxHelper.setTransactions(trans);
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

            } else {
                
                
                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);    

            }

            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doPayment - T2: %d Finish", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            String payCode = notifyRequest.getCOMPANYCODE() + notifyRequest.getMERCHANTCODE() + OneCheckoutVerifyFormatData.traceNo.format(Integer.parseInt(notifyRequest.getUSERACCOUNT()));
            ;
            notifyRequest.setPAYCODE(payCode);

            Transactions trans = queryHelper.getVATransactionBy(payCode, notifyRequest.getMERCHANTCODE(), acq, OneCheckoutTransactionState.INCOMING);

//            OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();

            OneCheckoutTransactionStatus status = null;
            //if (notifyRequest.getRESULTMSG() != null && notifyRequest.getRESULTMSG().toUpperCase().indexOf("SUCCESS") >= 0) {
            if (notifyRequest.getRESPONSECODE() != null && notifyRequest.getRESPONSECODE().toUpperCase().indexOf("00") >= 0) {
                status = OneCheckoutTransactionStatus.SUCCESS;
            } else {
                status = OneCheckoutTransactionStatus.FAILED;
            }

            if (trans == null) {


                trans = new Transactions();
                trans.setReconcileDateTime(new Date());

                String reconcileDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getReconcileDateTime());

                OneCheckoutPaymentRequest paymentRequest = new OneCheckoutPaymentRequest();
                MerchantPaymentChannel mpc = queryHelper.getMerchantPaymentChannel(notifyRequest.getMERCHANTCODE(), acq);
                if (mpc != null) {
                    trxHelper.setMerchant(mpc.getMerchants());

                    paymentRequest.setMALLID(trxHelper.getMerchant().getMerchantCode());
                }

                paymentRequest.setAMOUNT(OneCheckoutVerifyFormatData.sdf.format(notifyRequest.getAMOUNT()));
                paymentRequest.setPAYCODE(payCode);

                trxHelper.setCIPMIP(OneCheckoutMethod.MIP);
                trxHelper.setPaymentChannel(OneCheckoutPaymentChannel.MandiriSOALite);
                trxHelper.setPaymentRequest(paymentRequest);
                trans = transacBean.saveSettlementData(trxHelper, mpc);

                sendEmail(trans);

                OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus : Transaction is null, so insert new transaction");


                OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus - %s", trans.getDokuResultMessage());


                OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());

                HashMap<String, String> params = super.getData(trans);

                params.put("PAYMENTCODE", trans.getAccountId());

                String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);                
                OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus : statusNotify : %s", resp);

                OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus - T4: %d create trx record", (System.currentTimeMillis() - t1));
            } else {
                trans.setDokuApprovalCode(notifyRequest.getAPPROVALCODE());
                trans.setDokuIssuerBank(notifyRequest.getBANK());
                trans.setDokuInvokeStatusDatetime(new Date());

                trans.setDokuResponseCode("0000");
                trans.setDokuHostRefNum(notifyRequest.getHOSTREFNUM());
                trans.setDokuResult(notifyRequest.getRESULT());
                trans.setDokuResultMessage(notifyRequest.getRESULTMSG());
                trans.setTransactionsStatus(status.value());
                trans.setTransactionsState(OneCheckoutTransactionState.SETTLEMENT.value());

                em.merge(trans);
                OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus - T4: %d update trx record", (System.currentTimeMillis() - t1));
            }

            notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE);

            trxHelper.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doInvokeStatus - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public VARegistrationResponse registerVa(OneCheckoutDataHelper trxHelper) {

        try {

            PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

            StringBuilder sb = new StringBuilder();

            doku.virtualaccount.wsdl.MerchantVAService service = new doku.virtualaccount.wsdl.MerchantVAService();
            MerchantVA port = service.getMerchantVAPort();

            //BindingProvider bp = (BindingProvider) port;
            //bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, config.getString("onecheckout.wsdlurl", "http://localhost:8080/VA/MerchantVAService"));

            VARegistrationParamDocument registrationParamDoc = VARegistrationParamDocument.Factory.newInstance();
            VARegistrationParam registrationParam = registrationParamDoc.addNewVARegistrationParam();

            String payCode = trxHelper.getPaymentRequest().getPAYCODE();
            payCode = payCode.substring((payCode.length() - 8), payCode.length());

            VARegistrationParam.VirtualAccount va = registrationParam.addNewVirtualAccount();
            va.setVirtualAccountId(payCode);
            va.setCurrency(trxHelper.getPaymentRequest().getPURCHASECURRENCY());
            va.addNewInfo();
            va.getInfo().addDetail(trxHelper.getMerchant().getMerchantName());

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


            String paymentResponse = port.registerVirtualAccount(registrationParamDoc.toString());

            VARegistrationResponseDocument vaRegistrationResponseDoc = VARegistrationResponseDocument.Factory.parse(paymentResponse);
            VARegistrationResponse vaRegistrationResponse = vaRegistrationResponseDoc.getVARegistrationResponse();



            return vaRegistrationResponse;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }

    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
//        OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            String invoiceNo = statusRequest.getTRANSIDMERCHANT();
            String sessionId = statusRequest.getSESSIONID();
            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
            Transactions trans = trxHelper.getTransactions();
            //Transactions trans = queryHelper.getCheckVAStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.INCOMING);
            //if (trans == null) {
            //    trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);
            //}
            if (trans != null) {
                String word = super.generateCheckStatusRequestWords(trans, m);
                if (!word.equalsIgnoreCase(checkStatusRequest.getWORDS())) {
                    // Create Empty Notify
                    OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
                    //notify.setWORDS(word);
                    statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
                    return trxHelper;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode() + "");
                if (trans.getMerchantPaymentChannel().getPaymentChannelChainCode() != null && trans.getMerchantPaymentChannel().getPaymentChannelChainCode() > 0) {
                    sb.append("&").append("CHAINMALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelChainCode() + "");
                }
                sb.append("&").append("ACQUIRERID=").append("300");
                sb.append("&").append("VANUMBER=").append(trans.getAccountId());
                sb.append("&").append("TRANSIDMERCHANT=").append(trans.getIncTransidmerchant());
                sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
                OneCheckoutLogger.log("CHECK PAYMENT PARAM : " + sb.toString());
                InternetResponse inetResp = super.doFetchHTTP(sb.toString(), trans.getMerchantPaymentChannel().getPaymentChannel().getCheckStatusUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());
                if (inetResp != null && inetResp.getMsgResponse() != null && inetResp.getMsgResponse().trim().length() > 0 && !inetResp.getMsgResponse().trim().equalsIgnoreCase("ERROR")) {
                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(inetResp.getMsgResponse().trim());
                    xml.load(sr);
                    String paymentStatus = xml.getString("STATUS") != null ? xml.getString("STATUS").trim() : "";
                    if (paymentStatus != null && !paymentStatus.trim().equalsIgnoreCase("") && !paymentStatus.trim().equalsIgnoreCase("INVALID_REQUEST")) {
                        if (!paymentStatus.trim().equalsIgnoreCase("TRX_NOT_FOUND")) {
                            String paymentResponseCode = xml.getString("RESPONSECODE") != null ? xml.getString("RESPONSECODE").trim() : "";
                            String paymentRefNumber = xml.getString("HOSTREFNUMBER") != null ? xml.getString("HOSTREFNUMBER").trim() : "";
                            String cancelResponseCode = xml.getString("CANCELRESPONSECODE") != null ? xml.getString("CANCELRESPONSECODE").trim() : "";
                            String cancelRefNumber = xml.getString("CANCELHOSTREFNUMBER") != null ? xml.getString("CANCELHOSTREFNUMBER").trim() : "";
                            String acquirerBank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
                            String maskedCard = xml.getString("CARDNUMBER") != null ? xml.getString("CARDNUMBER").trim() : "";
                            String paymentDateTime = xml.getString("DATETIME") != null ? xml.getString("DATETIME").trim() : "";
                            String bank = xml.getString("ACQUIRERBANK") != null ? xml.getString("ACQUIRERBANK").trim() : "";
                            if (paymentResponseCode.length() == 2) {
                                paymentResponseCode = "00" + paymentResponseCode;
                            } else {
                                paymentResponseCode = "0099";
                            }
                            trans.setDokuIssuerBank(bank);
                            trans.setDokuResponseCode(paymentResponseCode);
                            trans.setDokuHostRefNum(paymentRefNumber);
                            try {
                                OneCheckoutDOKUNotifyData notifyRequest = new OneCheckoutDOKUNotifyData();
                                notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                                notifyRequest.setDFSScore("-1");
                                notifyRequest.setDFSIId("");
                                trans.setVerifyId("");
                                trans.setVerifyScore(-1);
                                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                           //     trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                            } catch (Throwable t) {
                                trans.setVerifyScore(0);
                            }

                            if (paymentStatus.trim().equalsIgnoreCase("SUCCESS")) {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                            } else if (paymentStatus.trim().equalsIgnoreCase("REVERSAL")) {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.REVERSED.value());
                            } else if (paymentStatus.trim().equalsIgnoreCase("VOID")) {
                                //trans.setDokuVoidApprovalCode(cancelResponseCode);
                                trans.setDokuVoidResponseCode(cancelResponseCode);
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());
                            } else if (paymentStatus.trim().equalsIgnoreCase("REFUND")) {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.REFUND.value());
                            } else {
                                trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                            }
                            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                            Date tpayment = new Date();
                            try {
                               tpayment = OneCheckoutVerifyFormatData.datetimeFormat.parse(paymentDateTime);
                            } catch (Exception ex) {

                            }
                            trans.setDokuInvokeStatusDatetime(tpayment);
                            em.merge(trans);
                        } else {
                            trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                        }
                    } else {
                        trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                    }
                } else {
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value());
                }

                // create NotificationRequest;
                HashMap<String, String> params = super.getData(trans);
                params.put("PAYMENTCODE", trans.getAccountId());
                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
                trxHelper.setMessage("VALID");
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params, m));
                trxHelper.setCheckStatusRequest(statusRequest);
                return trxHelper;
            }

            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);
            //  word = super.generateCheckStatusResponseWords(notify, m);
            //  notify.setWORDS(word);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");
            OneCheckoutLogger.log("OneCheckoutV1MandiriVALiteBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
            trxHelper.setCheckStatusRequest(statusRequest);
            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public Boolean sendEmail(Transactions trans) {
        boolean result = false;

        try {

            OneCheckoutLogger.log(": : : : Execute Send Email ");

            if (trans != null) {

                MerchantPaymentChannel mpc = trans.getMerchantPaymentChannel();
                Merchants merchant = mpc.getMerchants();
                PaymentChannel paymentChannel = mpc.getPaymentChannel();

                String templateDirectory = config.getString("email.directory").trim();
                String templateFileName = config.getString("email.newtrans.notification.filename").trim();
                String subject = config.getString("email.newtrans.subject").trim();
                String email = config.getString("email.support").trim();

                OneCheckoutLogger.log("templateDirectory : " + templateDirectory);
                OneCheckoutLogger.log("templateFileName : " + templateFileName);
                OneCheckoutLogger.log("subject : " + subject);

                Map emailParam = new HashMap();
                emailParam.put("merchantName", merchant.getMerchantName());
                emailParam.put("mallId", merchant.getMerchantCode());
                emailParam.put("customerName", trans.getIncName() == null ? "" : trans.getIncName());
                emailParam.put("customerPhone", "-");
                emailParam.put("customerEmail", trans.getIncEmail() == null ? "" : trans.getIncEmail());
                emailParam.put("paymentChannel", paymentChannel.getPaymentChannelName());
                emailParam.put("invoiceNo", trans.getIncTransidmerchant());
                emailParam.put("amount", trans.getIncAmount() != null ? OneCheckoutVerifyFormatData.moneyFormat.format(trans.getIncAmount()) : "");
                emailParam.put("status", OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus()));

                result = EmailUtility.sendEmailTemplate(templateDirectory, templateFileName, emailParam, OneCheckoutEmailType.HTML.code(), email, subject);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return result;
    }

    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriSOALiteBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));


            Merchants m = trxHelper.getMerchant();

            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();

            Transactions trans = queryHelper.getRedirectTransactionBy(redirectRequest.getTRANSIDMERCHANT(), redirectRequest.getSESSIONID(), redirectRequest.getAMOUNT(), redirectRequest.getPAYMENTCODE(), redirectRequest.getPAYMENTCHANNEL());

            //trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());

            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {

                trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_YET_PAID.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.NOT_YET_PAID.name());
                //trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            } else {
                trans.setDokuResponseCode(trans.getDokuResponseCode() != null ? trans.getDokuResponseCode() : redirectRequest.getSTATUSCODE());
            }

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate("redirect.html");
            redirect.setUrlAction(m.getMerchantRedirectUrl());
            redirect.setTRANSIDMERCHANT(redirectRequest.getTRANSIDMERCHANT());
            redirect.setSTATUSCODE(trans.getDokuResponseCode());
            Map data = redirect.getParameters();


            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());

            data.put("TRANSIDMERCHANT", redirectRequest.getTRANSIDMERCHANT());


            data.put("STATUSCODE", trans.getDokuResponseCode());
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", redirectRequest.getSESSIONID());
            data.put("PAYMENTCODE", redirectRequest.getPAYMENTCODE());
            data.put("PURCHASECURRENCY", trans.getIncPurchasecurrency());//getPURCHASECURRENCY());

            if (trans.getRates()!=null) {
                redirect.setAMOUNT(trans.getIncPurchaseamount().doubleValue());
                data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncPurchaseamount().doubleValue()));
                data.put("CURRENCY", trans.getIncPurchasecurrency());//.getPURCHASECURRENCY());
            }
            else {
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



            //    redirect.setCookie(trans.getIncCookies());
            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1MandiriSOALiteBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
            OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
            voidRequest.setVOIDRESPONSE("FAILED");
             OneCheckoutLogger.log("OneCheckoutV1MandiriSOALiteBean.doVoid : Void is not permitted");

            return trxHelper;
    }

    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}