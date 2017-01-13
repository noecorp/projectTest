/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.*;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.*;
import com.onecheckoutV1.type.*;
import com.onechekoutv1.dto.*;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author hafiz
 */
@Stateless
public class OneCheckoutV1CreditCardDIRECTCOREBean  extends OneCheckoutChannelBase  implements OneCheckoutV1CreditCardDIRECTCOREBeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;

    private boolean ableToReversal = true;    
    
    //  @EJB
    //   protected MIPVNConnectorLocal connector;
    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        //MIPDokuSuitePaymentResponse paymentResp = trxHelper.getPaymentResponse();

        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));
            Merchants m = trxHelper.getMerchant();
            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            boolean needNotify = false;
            String paramString = "";
            OneCheckoutErrorMessage errormsg = OneCheckoutErrorMessage.UNKNOWN;
            // check disini
            
            String CoreUrl = this.getCoreMipURL(pChannel, "DEFAULT");//, CoreUrl)pChannel.getRedirectPaymentUrlMipXml();
            if (trxHelper.getMessage().equalsIgnoreCase("ACS")) {
                trans = trxHelper.getTransactions();
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment incoming from ACS");
                trxHelper.getPaymentRequest().getAllAdditionData().put("FROM", "ACS");
                //String ocoId = this.generateOcoId(pChannel.getPaymentChannelId());
                trxHelper.getPaymentRequest().getAllAdditionData().put("OCOID", trans.getOcoId());
                paramString = super.createParamsHTTP(trxHelper.getPaymentRequest().getAllAdditionData());
                CoreUrl = this.getCoreMipURL(pChannel, "ACS");
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment DATA => %s", paramString);
            } else {
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment incoming from Merchant");
                IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
                boolean request_good = identrx.isRequestGood();
                MerchantPaymentChannel mpc = identrx.getMpc();
                errormsg = identrx.getErrorMsg();
                needNotify = identrx.isNeedNotify();
                if (!request_good) {
                    trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, identrx.getTrans());
                    return trxHelper;
                }

                if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) {
                    trans = transacBean.saveTransactions(trxHelper, mpc);
                } else {
                    trans = transacBean.updateTransactions(trxHelper, mpc);
                }
                
                trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
                Boolean status = pluginExecutor.validationMerchantPlugins(trxHelper);

                OneCheckoutDataPGRedirect datatoCore = this.createRedirectMIP(paymentRequest, pChannel, mpc, m, trans.getOcoId());
                HashMap<String, String> params = datatoCore.getParameters();
                paramString = super.createParamsHTTP(params);
            }

            boolean checkStatusToCore = false;
            String resultPayment = "";
            try {
                resultPayment = super.postMIP(paramString, CoreUrl, pChannel);
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment - RESULT[" + resultPayment + "]");
                if (resultPayment == null || resultPayment.trim().equals("") || resultPayment.trim().equalsIgnoreCase("ERROR")) {
                    checkStatusToCore = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                checkStatusToCore = true;
                /*
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE;
                trans.setDokuResponseCode(errormsg.value());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
                //trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
                 */
            }
            if (checkStatusToCore) {
                // CHECK STATUS TO CORE
                Thread.sleep(5 * 1000);
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREIPG(trans, pChannel);
                if (trans.getDokuResponseCode() != null && trans.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value())) {
                    trans = super.reversalToCOREIPG(trans);
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.TRANSACTION_CAN_NOT_BE_PROCCED.value());
                }
                needNotify = true;

                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
                HashMap<String, String> params = super.getData(trans);
                params.put("PAYMENTCODE", "");
                String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
                //trans = super.notifyStatusMerchant(trans,trxHelper.getPaymentChannel());
                if (trans.getDokuResponseCode() != null && trans.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.NOTIFY_FAILED.value()) && trans.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                    super.reversalToCOREIPG(trans);
                }
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                em.merge(trans);
                trxHelper.setTransactions(trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
            }

            XMLConfiguration xml = new XMLConfiguration();
            try {
                //xml = new XMLConfiguration();
                StringReader sr = new StringReader(resultPayment);
                xml.load(sr);
            } catch (Throwable ex) {
                ex.printStackTrace();
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                trans.setDokuResponseCode(errormsg.value());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
                super.reversalToCOREIPG(trans);
                //trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
            }
            String statusResp = xml.getString("status");
            if (statusResp != null && (statusResp.equalsIgnoreCase("THREEDSECURE") || statusResp.equalsIgnoreCase("3DSECURE"))) {
                try {
                    OneCheckoutDataPGRedirect redirect = this.parsing3DSecureData(xml);
                    trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);
                    trans.setInc3dSecureStatus(redirect.getParameters().get("MD"));
                    trans.setDokuResponseCode(OneCheckoutErrorMessage.NOT_CONTINUE_FROM_ACS.value());
                    em.merge(trans);
                    return trxHelper;
                } catch (Exception ex) {
                    needNotify = true;
                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                    trans.setDokuResponseCode(errormsg.value());
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);
                    //trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
                    trxHelper.setMessage("VALID");
                    return trxHelper;
                }
            } else {
                trans = this.parseXMLPayment(trans, xml);
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
                HashMap<String, String> params = super.getData(trans);
                params.put("PAYMENTCODE", "");
                String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment - %s", resp);
                if (statusResp != null && statusResp.trim().equalsIgnoreCase("Success") && trans.getTransactionsStatus() == OneCheckoutTransactionStatus.FAILED.value()) {
                    super.reversalToCOREIPG(trans);
                }
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                em.merge(trans);

                OneCheckoutRedirectData redirectRequest = null;
                trxHelper.setRedirectDoku(redirectRequest);
                trxHelper.setTransactions(trans);
                //trxHelper = this.doRedirectToMerchant(trxHelper, pChannel);
                trxHelper = this.doShowResultPage(trxHelper, pChannel);
                trxHelper.setMessage("VALID");
                return trxHelper;
            }
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());
            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }

    public String getCoreMipURL(PaymentChannel pChannel, String step) {

        String CoreUrl = pChannel.getRedirectPaymentUrlMipXml();
//        if (step.equalsIgnoreCase("ACS"))
//            CoreUrl = pChannel.getRedirectPaymentUrlMip();


        return CoreUrl;

    }


    public OneCheckoutDataPGRedirect parsing3DSecureData(XMLConfiguration xml) {

        OneCheckoutDataPGRedirect redirect = null ;
        try {

            String secureData = xml.getString("verifyReason");

            String[] param3DS = secureData.split("splitField");
            String pareq = param3DS[0];
            String MD = param3DS[1];
            String ACSURL = param3DS[2];
            String mpiPassword = param3DS[3];


            String[] pareqs = pareq.split("\\|");
            String[] mds = MD.split("\\|");
            String[] acsurl = ACSURL.split("\\|");
            String[] mpipassword = mpiPassword.split("\\|");

            //set data redirect yang akan di kirim ke merchant
           // OneCheckoutDataPGRedirect redirect = null;
            redirect = new OneCheckoutDataPGRedirect();
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setUrlAction(acsurl[1]);
            redirect.setPageTemplate(redirect.getProgressPage());


            Map data = redirect.getParameters();



            data.put(pareqs[0], pareqs[1]);
            data.put(mds[0], mds[1]);

            String RedirectBackUrl = config.getString("ONECHECKOUT.RedirectBackURLFromACS", "").trim() + "?" + mpipassword[0] + "=" + mpipassword[1];
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doPayment RedirectBackUrl => %s", RedirectBackUrl);

            data.put("TermUrl", RedirectBackUrl); // ini url baliknya apa
            OneCheckoutLogger.log("ACSURL ==> [" + acsurl[0] + "]=[" + acsurl[1] + "]");
            OneCheckoutLogger.log("PAREQ ==> [" + pareqs[0] + "]=[" + pareqs[1] + "]");
            OneCheckoutLogger.log("XID ==> [" + mds[0] + "]=[" + mds[1] + "]");
            OneCheckoutLogger.log("TermUrl ==> [TermUrl]=[" + RedirectBackUrl + "]");


            redirect.setParameters(new HashMap(data));
            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return redirect;
        }

    }
    
    
    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml) {

        try {

/*
 <orderResponse>
	<order-number></order-number>
	<purchase-amount></purchase-amount>
	<responsecode></responsecode>
	<status></status>
	<ApprovalCode></ApprovalCode>
	<HostReferenceNumber></HostReferenceNumber>
	<StatusCode></StatusCode>
	<installment></installment>
	<plan></plan>
	<tenor></tenor>
	<monthly></monthly>
	<InterestRate></InterestRate>
	<purchase-mdr></purchase-mdr>
	<OnUs></OnUs>
	<IssuerBankName></IssuerBankName>
	<AcquirerCode></AcquirerCode>
	<mallId></mallId>
	<chainNum></chainNum>
	<verifyId></verifyId>
	<verifyScore></verifyScore>
	<verifyStatus></verifyStatus>
	<verifyReason></verifyReason>
	<systraceNumber></systraceNumber>
	<batchNumber></batchNumber>
</orderResponse>
 */

            String status = xml.getString("status");
            trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
            if (status!=null && status.equalsIgnoreCase("Success")) {
                trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
            }


            String responseCode = xml.getString("responsecode");
            String approvalCode = xml.getString("ApprovalCode");
            String issuerBankName = xml.getString("IssuerBankName");
            String verifyStatus = xml.getString("verifyStatus");
            String verifyScore = xml.getString("verifyScore");
          //  String verifyReason = xml.getString("verifyReason");
            String hostRefNum = xml.getString("HostReferenceNumber");
            if (responseCode == null || responseCode.isEmpty()) {
                responseCode = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE.value();
            } else if (responseCode != null && responseCode.length() != 4) {
                responseCode = "00" + responseCode;
            }

            trans.setDokuApprovalCode(approvalCode);
            trans.setDokuIssuerBank(issuerBankName);
            trans.setDokuResponseCode(responseCode);
            trans.setDokuHostRefNum(hostRefNum);

            try {

                if (verifyStatus != null) {

                    OneCheckoutDFSStatus vStatus = OneCheckoutDFSStatus.findType(verifyStatus.trim().charAt(0));
                    if (vStatus!=null)
                        trans.setVerifyStatus(vStatus.value());
                    else
                        trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());

                    if (verifyScore!=null)
                        trans.setVerifyScore(Integer.parseInt(verifyScore));
                    else
                        trans.setVerifyScore(-1);


                    trans.setVerifyId("");

                } else {

                    trans.setVerifyId("");
                    trans.setVerifyScore(-1);
                    trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());

                }

            } catch (Throwable t) {
                trans.setVerifyId("");
                trans.setVerifyScore(-1);
                trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());

            }


   //         trans.setEduStatus(trans.getVerifyStatus());            

            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());


            return trans;
        } catch(Exception ex) {


            return trans;
        }




    }


    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();
            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();

            String wordDoku = super.generateVoidWords(voidRequest, m);
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - Checking Hash WORDS");

            if (!wordDoku.equalsIgnoreCase(voidRequest.getWORDS())) {
                voidRequest.setVOIDRESPONSE("FAILED");
                trxHelper.setMessage("VALID");
                trxHelper.setVoidRequest(voidRequest);
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - Hash WORDS doesn't match");

                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - Hash WORDS match");


            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);

            if (trans != null) {

                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());

                if (status == OneCheckoutTransactionStatus.SUCCESS) {

                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - ready to void");

                    StringBuilder sb = new StringBuilder();

                    sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
                    sb.append("&").append("OrderNumber=").append(invoiceNo);
                    sb.append("&").append("ApprovalCode=").append(trans.getDokuApprovalCode());

                    //Tambahan SWIPER
                    sb.append("&").append("MerchantID=").append(trans.getMerchantPaymentChannel().getMerchantPaymentChannelUid());
                    sb.append("&").append("PurchaseAmt=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount()));
                    sb.append("&").append("PurchaseMDR=").append("TEST");

//                    sb.append("&").append("USERNAME=").append(m.getMerchantName());
//                    sb.append("&").append("IP=").append(trans.getSystemSession());
//                    sb.append("&").append("REASON=").append(voidRequest.getVOIDREASON());

                    OneCheckoutLogger.log("VOID PARAM : " + sb.toString());

                    InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                    voidRequest = DOKUVoidResponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
                    if (voidRequest.getVoidStatus().equalsIgnoreCase("VOIDED")) {

                        trans.setDokuVoidApprovalCode(voidRequest.getApprovalCode());
                        trans.setDokuVoidDatetime(new Date());
                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("SUCCESS");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - update trx as voided");

                    } else {

                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setDokuVoidDatetime(new Date());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("FAILED");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - update response code void");
                    }

                    em.merge(trans);

                } else if (status == OneCheckoutTransactionStatus.VOIDED) {

                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(super.generateVoidResponse(trans, "VOIDED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - trx already voided");

                } else {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(super.generateVoidResponse(trans, "FAILED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("FAILED");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - trx can't be voided");
                }

            } else {

                if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                    voidRequest.setVOIDRESPONSE(super.generateVoidResponse(null, ""));
                } else {
                    voidRequest.setVOIDRESPONSE("NOT FOUND");
                }

                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - trx isn't found");
            }

            trxHelper.setMessage("VALID");
            trxHelper.setVoidRequest(voidRequest);

            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doVoid - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;

        } catch (Throwable th) {
            th.printStackTrace();
            voidRequest.setVOIDRESPONSE("FAILED");
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }





    public OneCheckoutDataHelper doRedirectToMerchant(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutRedirectData redirectRequest = trxHelper.getRedirectDoku();

            String statusCode = "";
            String invoiceNo = null;
            String sessionId = null;
            double amount = 0.00;
            int attempts = 1;

            if (redirectRequest!=null) {
                invoiceNo = redirectRequest.getTRANSIDMERCHANT();
                sessionId = redirectRequest.getSESSIONID();
                amount = redirectRequest.getAMOUNT();
                statusCode = redirectRequest.getSTATUSCODE();

            }
            else {

                invoiceNo = trxHelper.getTransactions().getIncTransidmerchant();
                sessionId = trxHelper.getTransactions().getIncSessionid();
                amount = trxHelper.getTransactions().getIncAmount().doubleValue();

            }

            Object[] obj = queryHelper.getRedirectTransactionWithoutStateNumber(invoiceNo, sessionId, amount);//.getRedirectTransactionBy(invoiceNo, sessionId, amount);

            attempts = (Integer) obj[0];
            Transactions trans = (Transactions) obj[1];

            if (redirectRequest==null)  {
                trans = trxHelper.getTransactions();
                statusCode = trans.getDokuResponseCode();
            }

            OneCheckoutLogger.log("ATTEMPTS : " + attempts);

            if (trans.getTransactionsState() != OneCheckoutTransactionState.DONE.value()) {

                trans.setDokuResponseCode(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.value());
                trans.setDokuResultMessage(OneCheckoutErrorMessage.PAYMENT_HAS_NOT_BEEN_PROCCED.name());
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());

            }

            //set data redirect yang akan di kirim ke merchant
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();
            Map data = redirect.getParameters();

            // ADD PARAMETER PURCHASECURRENCY IN WORDS AND REDIRECT
            redirect.setPURCHASECURRENCY(trans.getIncPurchasecurrency());

            // 0 = FROM CHANNEL, 1 = RETRY, 2 = REDIRECT TO MERCHANT / KLIK CONTINUE
            if (redirectRequest!=null && redirectRequest.getFLAG().equalsIgnoreCase("1")) {

                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=1");

                if ((m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) && trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {
                    OneCheckoutLogger.log("FLAG=1 A");
                    if (attempts >= 3) {
                        OneCheckoutLogger.log("FLAG=1 A1");
                        //    redirect.setPageTemplate("transactionFailed.html");

                        //ERROR PAGE 3 ATTEMPTS
                        redirect.setUrlAction(m.getMerchantRedirectUrl());
                        redirect.setPageTemplate("transactionFailed.html");
                    } else {
                        OneCheckoutLogger.log("FLAG=1 A2");
                        //PAYMENT PAGE
                        redirect.setUrlAction(config.getString("URL.RECEIVE"));
                        redirect.setPageTemplate("redirect.html");

                        //MUST GENERATE PAYMENT REQUEST PARAMETER
                        data = super.generatePaymentRequest(trans);
                        redirect.setParameters(new HashMap(data));
                    }

                } else {
                    OneCheckoutLogger.log("FLAG=1 A1");
                    redirect.setAMOUNT(amount);
                    redirect.setTRANSIDMERCHANT(invoiceNo);
                    redirect.setSTATUSCODE(statusCode);


                    redirect.setUrlAction(m.getMerchantRedirectUrl());
                    redirect.setPageTemplate("redirect.html");

                }
                trxHelper.setRedirectDoku(redirectRequest);
            }
            else if (redirectRequest!=null && redirectRequest.getFLAG().equalsIgnoreCase("2")){
                statusCode = trans.getDokuResponseCode();
                redirectRequest.setSTATUSCODE(statusCode);
                trxHelper.setRedirectDoku(redirectRequest);


                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=2");
                // redirect = redirectToMerchant(redirect, redirectRequest, trans, m, trxHelper, data);
                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(redirectRequest.getSTATUSCODE());

                redirect.setUrlAction(m.getMerchantRedirectUrl());
                redirect.setPageTemplate("redirect.html");

            }

            else  {

                redirect.setAMOUNT(amount);
                redirect.setTRANSIDMERCHANT(invoiceNo);
                redirect.setSTATUSCODE(statusCode);

                OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0");
                HashMap resultData = redirect.getRetryData();


                resultData.put("DATENOW", OneCheckoutVerifyFormatData.detailDateFormat.format(new Date()));
                resultData.put("DISPLAYNAME", m.getMerchantName());
                resultData.put("DISPLAYADDRESS", m.getMerchantAddress());
                resultData.put("CREDIT_CARD", trans.getAccountId());

                Currency currency = queryHelper.getCurrencyByCode(trans.getIncCurrency());

                resultData.put("CURRENCY", currency.getAlpha3Code());
                resultData.put("INVOICE", invoiceNo);
                resultData.put("DISPLAYAMOUNT", OneCheckoutVerifyFormatData.moneyFormat.format(trans.getIncAmount()));

                if (m.getMerchantRetry() != null && m.getMerchantRetry() == Boolean.TRUE) {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A1");
                    redirect.setRetry(Boolean.TRUE);
                    resultData.put("ATTEMPTS", String.valueOf(attempts));
                }

                redirect.setUrlAction(config.getString("URL.GOBACK"));
                resultData.put("DISPLAYCHANNEL", trxHelper.getPaymentChannel().value());

                OneCheckoutLogger.log("TRANSACTION STATUS [" + trans.getTransactionsStatus() + "]");

                if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {

                    if (trans.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()) {
                        redirect.setRetry(Boolean.FALSE);
                    }
                    
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");

                    redirect.setPageTemplate("transactionFailed.html");

                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A4");
                    redirect.setPageTemplate("transactionSuccess.html");

                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with " + m.getMerchantName());

                    resultData.put("APPROVAL", trans.getDokuApprovalCode());
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
                    redirect = super.addDWRegistrationPage(trxHelper, redirect, channel, trans.getMerchantPaymentChannel(), trans);
                }
                redirect.setRetryData(resultData);

                HashMap<String, String> params = super.getData(trans);
                OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
                trans = super.notifyOnRedirect(trans, params, channel);  
            }


            data.put("TRANSIDMERCHANT", invoiceNo);
            data.put("STATUSCODE", statusCode);
            data.put("PAYMENTCHANNEL", trxHelper.getPaymentChannel().value());
            data.put("SESSIONID", sessionId);
            data.put("PAYMENTCODE", "");
            data.put("PURCHASECURRENCY", redirect.getPURCHASECURRENCY());

            if (trans.getRates()!=null && redirect.getUrlAction().equals(m.getMerchantRedirectUrl())) {
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


            trxHelper.setMessage("VALID");
            trxHelper.setRedirect(redirect);//.setPayResponse(paymentResp);

            trans.setRedirectDatetime(new Date());
            em.merge(trans);
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }


    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel paymentChannel) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
//        OneCheckoutNotifyStatusRequest oneCheckoutNotifyStatusRequest = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));

            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();

            Transactions trans = trxHelper.getTransactions();

            if (trans != null) {

                String word = super.generateCheckStatusRequestWords(trans, m);

                if (!word.equalsIgnoreCase(checkStatusRequest.getWORDS())) {
                    // Create Empty Notify
                    OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest, trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH);
                    //notify.setWORDS(word);
                    statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());
                    return trxHelper;
                }

                OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doCheckStatus - Checking status to CORE", (System.currentTimeMillis() - t1));
                trans = super.CheckStatusCOREIPG(trans, paymentChannel);


                HashMap<String, String> params =  super.getData(trans);
                trxHelper.setMessage("VALID");

                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
                statusRequest.setACKNOWLEDGE(notify.toCheckStatusString(params,m));//.toMPGString());
                trxHelper.setCheckStatusRequest(statusRequest);
                return trxHelper;

            }

            // Create Empty Notify
            OneCheckoutNotifyStatusRequest notify = super.createEmptyNotify(statusRequest,  trxHelper.getPaymentChannel(), OneCheckoutErrorMessage.TRANSACTION_NOT_FOUND);
            statusRequest.setACKNOWLEDGE(notify.toCheckStatusStringFailed());

            trxHelper.setCheckStatusRequest(statusRequest);
            trxHelper.setMessage("VALID");
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));

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

    public OneCheckoutDataPGRedirect createRedirectMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {

        //set data redirect yang akan di kirim ke merchant

        try {
            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlMip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            HashMap<String, String> data = redirect.getParameters();

            data.put("MerchantID", mpc.getMerchantPaymentChannelUid());


            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                data.put("CHAINNUM", "NA");
            } else {
                data.put("CHAINNUM", mpc.getPaymentChannelChainCode() + "");
            }

            data.put("OrderNumber", paymentRequest.getTRANSIDMERCHANT());
            if (paymentRequest.getBILLINGDESCRIPTION() != null && !paymentRequest.getBILLINGDESCRIPTION().trim().equals("")) {
                data.put("trxDesc", paymentRequest.getBILLINGDESCRIPTION().trim());
            }
            data.put("PurchaseAmt", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            data.put("vat", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getVAT()));
            data.put("insurance", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getINSURANCE()));
            data.put("fuelSurcharge", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()));
            data.put("PurchaseCurrency", paymentRequest.getCURRENCY());
            data.put("MerchantName", m.getMerchantName());
            data.put("MALLID", mpc.getPaymentChannelCode() + "");
            data.put("WORDS", super.generateDokuWords(paymentRequest, mpc));
            data.put("SESSIONID", paymentRequest.getSESSIONID());
            data.put("NAME", paymentRequest.getCC_NAME());
            data.put("CONTACTABLE_NAME", paymentRequest.getNAME());
            data.put("CITY", paymentRequest.getCITY());
            data.put("STATE", paymentRequest.getSTATE());
            data.put("CustIP",paymentRequest.getCUSTIP());

            String country = queryHelper.getCountryById(paymentRequest.getCOUNTRY());
            OneCheckoutLogger.log("COUNTRY VALUE : [%s]", country);
            data.put("COUNTRY", country != null ? country : paymentRequest.getCOUNTRY());

            data.put("PHONE", paymentRequest.getMOBILEPHONE());
            data.put("HOMEPHONE", paymentRequest.getHOMEPHONE());
            data.put("OFFICEPHONE", paymentRequest.getWORKPHONE());
            //String ocoId = this.generateOcoId(pChannel.getPaymentChannelId());
            data.put("OCOID", ocoId);
//            data.put("IP",paymentRequest.getCUSTIP() != null ?  paymentRequest.getCUSTIP() : "");
            
//            data.put("USERNAME",paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");
//            data.put("USERNAME",mpc.getMerchants().getMerchantName() != null ? mpc.getMerchants().getMerchantName() : "");
            
//            data.put("REASON",paymentRequest.getPAYREASON() != null ? paymentRequest.getPAYREASON() : "");
            
            if (m.getMerchantCategory() == OneCheckoutMerchantCategory.AIRLINE.value()) {
                /*
                 * StringBuffer basket = new StringBuffer();
                 * basket.append(paymentRequest.getROUTE());
                 *
                 * if (paymentRequest.getRETURN()==OneCheckoutReturnType.Return)
                 * basket.append(";").append(paymentRequest.getRETURN_ROUTE());
                 *
                 * String amount =
                 * OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT());
                 *
                 * basket.append(",").append(amount).append(",").append("1").append(",").append(amount);
                 */
                data.put("PurchaseDesc", paymentRequest.getBASKET());

                String kam = "";
                if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.OneWay) {
                    if (paymentRequest != null && paymentRequest.getROUTE() != null && paymentRequest.getROUTE().length > 0) {
                        kam = paymentRequest.getROUTE()[0];
                        data.put("decisionManager_travelData_completeRoute", paymentRequest.getROUTE()[0]);
                    }
                    data.put("decisionManager_travelData_journeyType", "one way");
                } else if (paymentRequest.getFLIGHTTYPE() == OneCheckoutReturnType.Return) {
                    if (paymentRequest != null && paymentRequest.getROUTE() != null && paymentRequest.getROUTE().length > 0) {
                        String temp = "";
                        if (paymentRequest.getROUTE().length > 1) {
                            temp = " n " + paymentRequest.getROUTE()[1];
                        }
                        kam = paymentRequest.getROUTE()[0] + temp;
                        data.put("decisionManager_travelData_completeRoute", paymentRequest.getROUTE()[0] + temp);
                    }
                    data.put("decisionManager_travelData_journeyType", "round trip");
                }
                OneCheckoutLogger.log("decisionManager_travelData_completeRoute : %s ", kam);

                data.put("decisionManager_travelData_departureDateTime", OneCheckoutVerifyFormatData.cybersource_datetimeFormat.format(paymentRequest.getFLIGHTDATETIME()));



                if (paymentRequest.getPASSENGER_NAME() != null) {

                    String[] passengerName = paymentRequest.getPASSENGER_NAME();
                    String[] passengerType = paymentRequest.getPASSENGER_TYPE();

                    int count = passengerName.length;


                    for (int i = 0; i < count; i++) {
                        if (passengerType[i] != null) {
                            String type = "";
                            if (passengerType[i].equalsIgnoreCase("A")) {
                                type = "ADT";
                            } else if (passengerType[i].equalsIgnoreCase("C")) {
                                type = "CNN";
                            }
                            String name = passengerName[i];
                            String[] names = name.split(" ");
                            String firstName = "";
                            String lastName = "";
                            if (name != null) {
                                if (names.length == 1) {
                                    firstName = names[0];
                                } else if (names.length > 1) {
                                    firstName = names[0];
                                    lastName = names[1];
                                }
                            }
                            data.put("item_" + i + "_passengerFirstName", firstName);
                            data.put("item_" + i + "_passengerLastName", lastName);
                            data.put("item_" + i + "_passengerType", type);
                            data.put("item_" + i + "_passengerEmail", paymentRequest.getEMAIL());
                            data.put("item_" + i + "_passengerPhone", paymentRequest.getMOBILEPHONE());
                            data.put("item_" + i + "_unitPrice", OneCheckoutVerifyFormatData.sdfnodecimal.format(paymentRequest.getAMOUNT()));
                        }
                    }
                    data.put("item_count", count + "");
                }
                data.put("ADDITIONALINFO", EDSAdditionalInformation.getAdditionalInfoForEdu(paymentRequest));
            } else {
                data.put("ADDITIONALINFO", paymentRequest.getADDITIONALINFO());
            }

            data.put("EMAIL", paymentRequest.getEMAIL());
            data.put("ZIP_CODE", paymentRequest.getZIPCODE());
            data.put("ADDRESS", paymentRequest.getADDRESS());
            data.put("ACQCODE", paymentRequest.getINSTALLMENT_ACQUIRER());
            data.put("PLAN", paymentRequest.getPROMOID());
            data.put("TENOR", paymentRequest.getTENOR());
            data.put("TYPE", "IMMEDIATE");

            data.put("PurchaseDesc", paymentRequest.getBASKET());

            HashMap<String, String> data1 = new HashMap(data);

            OneCheckoutBaseRules base = new OneCheckoutBaseRules();
            data1.put("CardNumber", base.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
            data1.put("EXPIRYDATE", paymentRequest.getEXPIRYDATE());

            OneCheckoutLogger.log("DATA to IPG => " + data1.toString());

            data.put("CardNumber", paymentRequest.getCARDNUMBER());
            data.put("EXPIRYDATE", paymentRequest.getEXPIRYDATE());
            data.put("CVV2", paymentRequest.getCVV2());


            redirect.setParameters(data);

            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }


    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {

        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doMIPPayment - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();

            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
            boolean request_good = identrx.isRequestGood();
            String statusCode = identrx.getStatusCode();
            MerchantPaymentChannel mpc = identrx.getMpc();
            boolean needNotify = identrx.isNeedNotify();
            OneCheckoutErrorMessage errormsg = identrx.getErrorMsg();

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            if (notifyRequest == null) {
                notifyRequest = new OneCheckoutDOKUNotifyData();
            }

            if (request_good) {
                boolean checkStatusToCore = false;
                String resultPayment = "";
                OneCheckoutDataPGRedirect redirect = null;
                trans = transacBean.saveTransactions(trxHelper, mpc);
                trxHelper.setOcoId(trans.getOcoId());
                String data = createPostMIP(paymentRequest, pChannel, mpc, m, trans.getOcoId());
                try {
                    resultPayment = super.postMIP(data, pChannel.getRedirectPaymentUrlMipXml(), pChannel);
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doMIPPayment - RESULT[" + resultPayment + "]");
                    if (resultPayment == null || resultPayment.trim().equals("") || resultPayment.trim().equalsIgnoreCase("ERROR")) {
                        checkStatusToCore = true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    checkStatusToCore = true;
                }
                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doMIPPayment - T1: %d Start", (System.currentTimeMillis() - t1));
                if (checkStatusToCore) {
                    // CHECK STATUS TO CORE
                    Thread.sleep(5 * 1000);
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doMIPPayment - Checking status to CORE", (System.currentTimeMillis() - t1));
                    trans = super.CheckStatusCOREIPG(trans, pChannel);
                    if (trans.getDokuResponseCode() != null && trans.getDokuResponseCode().trim().equals(OneCheckoutErrorMessage.CANNOT_GET_CHECKSTATUS.value())) {
                        trans = super.reversalToCOREIPG(trans);
                    }
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
                    HashMap<String, String> params = super.getData(trans);
                    params.put("PAYMENTCODE", "");
                    String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
                    
                    //trans = super.notifyStatusMerchant(trans,trxHelper.getPaymentChannel());
                    em.merge(trans);
                    trxHelper.setTransactions(trans);
                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
                    trxHelper.setMessage("VALID");
                    return trxHelper;
                }

                //String resultPayment = super.postMIP(data, pChannel.getRedirectPaymentUrlMipXml(), pChannel);
                //OneCheckoutLogger.log(":X:X:X:X: PAYMENT REPONSE RECEIVED FROM IPG[" + resultPayment + "]");

                trxHelper.setMessage(resultPayment);
                trxHelper.setRedirect(redirect);

                if (resultPayment != null && resultPayment.length() > 0 && !resultPayment.equalsIgnoreCase("ERROR")) {

                    XMLConfiguration xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);

                    //OneCheckoutLogger.log(": : : : : PAYMENT REPONSE RECEIVED FROM IPG[" + resultPayment + "]");

                    String responseCode = xml.getString("responsecode");
                    String approvalCode = xml.getString("ApprovalCode");
                    String issuerBankName = xml.getString("IssuerBankName");
                    String verifyStatus = xml.getString("verifyStatus");
                    String verifyScore = xml.getString("verifyScore");
                    String verifyReason = xml.getString("verifyReason");
                    String hostRefNum = xml.getString("HostReferenceNumber");

                    if (responseCode != null && responseCode.length() != 4) {
                        responseCode = "00" + responseCode;
                    }

                    trans.setDokuApprovalCode(approvalCode);
                    trans.setDokuIssuerBank(issuerBankName);
                    trans.setDokuResponseCode(responseCode);
                    trans.setDokuHostRefNum(hostRefNum);

                    try {

                        if (verifyStatus != null) {

                            notifyRequest.setDFSStatus(verifyStatus);
                            notifyRequest.setDFSScore(verifyScore);
                            notifyRequest.setDFSIId(verifyReason);

                            trans.setVerifyId(notifyRequest.getDFSId());
                            trans.setVerifyScore(notifyRequest.getDFSScore());
                            trans.setVerifyStatus(notifyRequest.getDFSStatus().value());
   //                         trans.setEduStatus(notifyRequest.getDFSStatus().value());

                        } else {

                            notifyRequest.setDFSStatus(OneCheckoutDFSStatus.NA.name());
                            notifyRequest.setDFSScore("-1");
                            notifyRequest.setDFSIId("");

                            trans.setVerifyId("");
                            trans.setVerifyScore(-1);
                            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
        //                    trans.setEduStatus(OneCheckoutDFSStatus.NA.value());
                        }

                    } catch (Throwable t) {
                        trans.setVerifyScore(0);
                    }

                    String status = xml.getString("status");
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                    if (status!=null && status.equalsIgnoreCase("Success")) {
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.SUCCESS.value());
                    }

                } else {
                    trans.setTransactionsStatus(OneCheckoutTransactionStatus.FAILED.value());
                }

                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());

                /* ################## FOR MY SHORCART ################## */
                if (m.getMerchantNotifyStatusUrl() != null && m.getMerchantNotifyStatusUrl().length() > 0) {

                    
                     OneCheckoutLogger.log("OneCheckoutV1CreditCardProcessorBean.doMIPPayment - T1: %d start Notify to Merchant", (System.currentTimeMillis() - t1));            
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());
        //            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
                    HashMap<String, String> params = super.getData(trans);

                    params.put("PAYMENTCODE", "");

                    String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
                    OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doMIPPayment : statusNotify : %s", resp);

                }
                /* ################## FOR MY SHORCART ################## */

                em.merge(trans);
            } else {

                if (trans == null) {
                    List<Transactions> list = queryHelper.getTransactionsBy(paymentRequest.getTRANSIDMERCHANT(), mpc);

                    if (list != null && list.size() > 0) {
                        trans = list.get(0);
                        list = null;
                    }
                }

                if (errormsg == null && trans.getTransactionsStatus() == OneCheckoutTransactionStatus.SUCCESS.value() && trans.getTransactionsState() == OneCheckoutTransactionState.DONE.value()) {
                    trxHelper.setMessage("Duplicate Invoice Number");
                } else {
                    trxHelper.setMessage(errormsg.value());
                }

            }

            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doMIPPayment - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public String createPostMIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {

        String data = "";
        try {

            data += URLEncoder.encode("TYPE", "UTF-8") + "=" + URLEncoder.encode("IMMEDIATE", "UTF-8") + "&";

            data += URLEncoder.encode("MerchantID", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getMerchantPaymentChannelUid(), "UTF-8") + "&";

            /*if (paymentRequest.getCHAINMERCHANT() == 0) {
            data += URLEncoder.encode("CHAINNUM", "UTF-8") + "=" + URLEncoder.encode("NA", "UTF-8") + "&";
            } else {
            data += URLEncoder.encode("CHAINNUM", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCHAINMERCHANT(), "UTF-8") + "&";
            }*/

            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                data += URLEncoder.encode("CHAINNUM", "UTF-8") + "=" + URLEncoder.encode("NA", "UTF-8") + "&";
            } else {
                data += URLEncoder.encode("CHAINNUM", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getPaymentChannelChainCode(), "UTF-8") + "&";
            }

            //OneCheckoutLogger.log(": : : : : CHAINNUM "+data);
            if (paymentRequest.getBILLINGDESCRIPTION() != null && !paymentRequest.getBILLINGDESCRIPTION().trim().equals("")) {
                data += URLEncoder.encode("trxDesc", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getBILLINGDESCRIPTION().trim(), "UTF-8") + "&";
            }
            data += URLEncoder.encode("OrderNumber", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getTRANSIDMERCHANT(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : OrderNumber "+data);
            data += URLEncoder.encode("PurchaseAmt", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PurchaseAmt "+data);
            data += URLEncoder.encode("vat", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getVAT()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : vat "+data);
            data += URLEncoder.encode("insurance", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getINSURANCE()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : insurance "+data);
            data += URLEncoder.encode("fuelSurcharge", "UTF-8") + "=" + URLEncoder.encode("" + OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getFUELSURCHARGE()), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : fuelSurcharge "+data);
            data += URLEncoder.encode("PurchaseCurrency", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCURRENCY(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PurchaseCurrency "+data);
            data += URLEncoder.encode("MerchantName", "UTF-8") + "=" + URLEncoder.encode("" + m.getMerchantName(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : MerchantName "+data);
            //data += URLEncoder.encode("MALLID", "UTF-8") + "=" + URLEncoder.encode("" + m.getMerchantCode(), "UTF-8") + "&";
            data += URLEncoder.encode("MALLID", "UTF-8") + "=" + URLEncoder.encode("" + mpc.getPaymentChannelCode(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : MALLID "+data);
            data += URLEncoder.encode("WORDS", "UTF-8") + "=" + URLEncoder.encode("" + super.generateDokuWords(paymentRequest, mpc), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : WORDS "+data);
            data += URLEncoder.encode("SESSIONID", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getSESSIONID(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : SESSIONID "+data);
            data += URLEncoder.encode("NAME", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCC_NAME(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : NAME "+data);
            data += URLEncoder.encode("CONTACTABLE_NAME", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getNAME(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : CONTACTABLE_NAME "+data);
            data += URLEncoder.encode("CITY", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCITY(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : CITY "+data);
            data += URLEncoder.encode("STATE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getSTATE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : STATE "+data);
            data += URLEncoder.encode("COUNTRY", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCOUNTRY(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : COUNTRY "+data);
            data += URLEncoder.encode("PHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getMOBILEPHONE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PHONE "+data);
            data += URLEncoder.encode("HOMEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getHOMEPHONE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : HOMEPHONE "+data);
            data += URLEncoder.encode("OFFICEPHONE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getWORKPHONE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : OFFICEPHONE "+data);

            if (m.getMerchantCategory() != null) {
                if (m.getMerchantCategory() != OneCheckoutMerchantCategory.NONAIRLINE.value()) {
                    data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("" + EDSAdditionalInformation.getAdditionalInfoForEdu(paymentRequest), "UTF-8") + "&";
                } else {
                    data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getADDITIONALINFO(), "UTF-8") + "&";
                }
            } else {
                data += URLEncoder.encode("ADDITIONALINFO", "UTF-8") + "=" + URLEncoder.encode("", "UTF-8") + "&";
            }

            //OneCheckoutLogger.log(": : : : : EMAIL "+paymentRequest.getEMAIL());
            data += URLEncoder.encode("EMAIL", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEMAIL(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : EMAIL "+data);
            data += URLEncoder.encode("ZIP_CODE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getZIPCODE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : ZIP_CODE "+data);
            data += URLEncoder.encode("ADDRESS", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getADDRESS(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : ADDRESS "+data);
            data += URLEncoder.encode("ACQCODE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getINSTALLMENT_ACQUIRER(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : ACQCODE "+data);
            data += URLEncoder.encode("PLAN", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getPROMOID(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PLAN "+data);
            data += URLEncoder.encode("TENOR", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getTENOR(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : TENOR "+data);

            data += URLEncoder.encode("PurchaseDesc", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getBASKET(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : PurchaseDesc "+data);

            if (!paymentRequest.getECI().isEmpty())  {

                data += URLEncoder.encode("Eci", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getECI(), "UTF-8") + "&";

                data += URLEncoder.encode("XID", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getXID(), "UTF-8") + "&";
                data += URLEncoder.encode("AuthResResponseCode", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getAUTHRESRESPONSECODE(), "UTF-8") + "&";
           //     data += URLEncoder.encode("VendorCode", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getAUTHRESVENDORCODE(), "UTF-8") + "&";
                data += URLEncoder.encode("CavvAlgorithm", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getCAVVALGORITHM(), "UTF-8") + "&";
                data += URLEncoder.encode("AuthResStatus", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getAUTHRESSTATUS(), "UTF-8") + "&";
                data += URLEncoder.encode("CAVV", "UTF-8") + "=" + URLEncoder.encode(paymentRequest.getCAVV(), "UTF-8") + "&";

            }

            OneCheckoutLogger.log(": : : : : DATA POST TO IPG[" + data + "]");
            //OneCheckoutLogger.log(": : : : : DATA POST TO IPG[" + data + "]");

            data += URLEncoder.encode("EXPIRYDATE", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getEXPIRYDATE(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : EXPIRYDATE "+data);

            data += URLEncoder.encode("CardNumber", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCARDNUMBER(), "UTF-8") + "&";
            //OneCheckoutLogger.log(": : : : : CardNumber "+data);
            
            data += URLEncoder.encode("OCOID", "UTF-8") + "=" + URLEncoder.encode("" + ocoId, "UTF-8") + "&";

            data += URLEncoder.encode("CVV2", "UTF-8") + "=" + URLEncoder.encode("" + paymentRequest.getCVV2(), "UTF-8");
            //OneCheckoutLogger.log(": : : : : EXPIRYDATE "+data);

            return data;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doReversal(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal - T0: %d Start process", (System.currentTimeMillis() - t1));

            OneCheckoutDOKUNotifyData notifyRequest = trxHelper.getNotifyRequest();

            String invoiceNo = notifyRequest.getTRANSIDMERCHANT();
            String sessionId = notifyRequest.getSESSIONID();
            double amount = notifyRequest.getAMOUNT();

            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getNotifyTransactionBy(invoiceNo, sessionId, acq);
            if (trans == null) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal : Transaction is null");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            String word = super.generateDOKUNotifyRequestWords(notifyRequest, trans);

            if (!word.equalsIgnoreCase(notifyRequest.getWORDS())) {

                OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal : WORDS doesn't match !");
                notifyRequest.setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);

                trxHelper.setNotifyRequest(notifyRequest);
                return trxHelper;

            }

            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal - T2: %d update transaction", (System.currentTimeMillis() - t1));

            OneCheckoutTransactionStatus status = null;
            if (notifyRequest.getTYPE().equalsIgnoreCase("REVERSAL")) {
                status = OneCheckoutTransactionStatus.REVERSED;
            }

            trans.setTransactionsStatus(status.value());
            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());


            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal - %s", trans.getDokuResultMessage());


            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
//            notifyStatusMerchant( T trans, HashMap<String, String> params,OneCheckoutPaymentChannel pChannel, boolean reversal,OneCheckoutStepNotify step)
            HashMap<String, String> params = super.getData(trans);
            
            params.put("PAYMENTCODE", "");
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.REVERSAL_PAYMENT);
            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal - T4: %d update trx record", (System.currentTimeMillis() - t1));


            em.merge(trans);


            notifyRequest.setACKNOWLEDGE(resp);

            trxHelper.setNotifyRequest(notifyRequest);


            OneCheckoutLogger.log("OneCheckoutV1CreditCardDIRECTCOREBean.doReversal - T5: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.getNotifyRequest().setACKNOWLEDGE(OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP);
            trxHelper.setMessage(th.getMessage());

            return trxHelper;
        }
    }

    public OneCheckoutDataHelper doCyberSource(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doReconcile(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doCCVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataPGRedirect createRedirectCIP(OneCheckoutPaymentRequest paymentRequest, PaymentChannel pChannel, MerchantPaymentChannel mpc, Merchants m, String ocoId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInquiryInvoice(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        throw new UnsupportedOperationException("Not supported yet.");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OneCheckoutDataHelper doInvokeStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {

        throw new UnsupportedOperationException("Not supported yet.");

    }

    public String getEDSDataEmptyACK() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doGetEDSData(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        return super.doGetEDSDataBase(trxHelper, acq);
    }

    public OneCheckoutDataHelper doUpdateEDSStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        return super.doUpdateEDSStatusBase(trxHelper, acq);
    }

    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml, Merchants merchants) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
