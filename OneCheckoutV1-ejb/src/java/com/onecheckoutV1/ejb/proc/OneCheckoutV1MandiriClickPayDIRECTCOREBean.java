/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.proc;

import com.doku.lib.inet.InternetResponse;
import com.onecheckoutV1.data.OneCheckoutCheckStatusData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutDataPGRedirect;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.data.OneCheckoutRedirectData;
import com.onecheckoutV1.data.OneCheckoutVoidRequest;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1PluginExecutorLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1TransactionBeanLocal;
import com.onecheckoutV1.ejb.helper.RefundHelper;
import com.onecheckoutV1.ejb.util.IdentifyTrx;
import com.onecheckoutV1.ejb.util.MandiriClickPayResponseXML;
import com.onecheckoutV1.ejb.util.OneCheckoutBaseRules;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutMethod;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionState;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Currency;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
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
public class OneCheckoutV1MandiriClickPayDIRECTCOREBean extends OneCheckoutChannelBase  implements OneCheckoutV1MandiriClickPayDIRECTCOREBeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @EJB
    protected OneCheckoutV1TransactionBeanLocal transacBean;
    @EJB
    protected OneCheckoutV1PluginExecutorLocal pluginExecutor;

    private boolean ableToReversal = true;          

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;
    

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)    
    public OneCheckoutDataHelper doPayment(OneCheckoutDataHelper trxHelper, PaymentChannel pChannel) {
        Transactions trans = null;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doPayment - T0: %d Start", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();

            OneCheckoutPaymentRequest paymentRequest = trxHelper.getPaymentRequest();
            boolean needNotify = false;
            String paramString = "";
            OneCheckoutErrorMessage errormsg = OneCheckoutErrorMessage.UNKNOWN;
            // check disini
            String CoreUrl = pChannel.getRedirectPaymentUrlMipXml();//this.getCoreMipURL(pChannel,"DEFAULT");//, CoreUrl)pChannel.getRedirectPaymentUrlMipXml();

                
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doPayment incoming from Merchant");               

            IdentifyTrx identrx = super.getTransactionInfo(paymentRequest, pChannel, m);
            boolean request_good = identrx.isRequestGood();
            MerchantPaymentChannel mpc = identrx.getMpc();
            errormsg = identrx.getErrorMsg();
            needNotify = identrx.isNeedNotify();

            if (!request_good) {
                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, identrx.getTrans());
                return trxHelper;
            }   

            if (trxHelper.getCIPMIP() == OneCheckoutMethod.MIP) 
                trans = transacBean.saveTransactions(trxHelper, mpc);
            else 
                trans = transacBean.updateTransactions(trxHelper, mpc);

            trxHelper.setStepNotify(OneCheckoutStepNotify.IDENTIFY_PAYMENT);
            pluginExecutor.validationMerchantPlugins(trxHelper);

            OneCheckoutDataPGRedirect datatoCore = this.createRedirectMIP(paymentRequest, pChannel, mpc, m, trans.getOcoId());


            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(pChannel.getPaymentChannelId());                                               
//            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doPayment DATA => %s", paramString);
                                    
            String resultPayment = "";
            boolean sendingOK = true;
            try {
                HashMap<String,String> params= datatoCore.getParameters();
                paramString = super.createParamsHTTP(params);               
                resultPayment = super.postMIP(paramString, CoreUrl, pChannel);
                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doPayment - RESULT[" + resultPayment + "]");
            } catch (Exception ex) {
                
                needNotify = true;
                errormsg = OneCheckoutErrorMessage.ERROR_CONNECT_TO_CORE;
                trans.setDokuResponseCode(errormsg.value());  
                trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                trans.setDokuInvokeStatusDatetime(new Date());
                em.merge(trans);
                 
                sendingOK = false;
                sendReversal(trans,m,pChannel);
////                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
//                trxHelper = this.doShowResultPage(trxHelper, pChannel);
//                trxHelper.setMessage("VALID");                 
//                return trxHelper;
            }

            XMLConfiguration xml = new XMLConfiguration();
            if (sendingOK) {
            
                
                try {
                    //xml = new XMLConfiguration();
                    StringReader sr = new StringReader(resultPayment);
                    xml.load(sr);

                    trans = this.parseXMLPayment(trans, xml);
            
                    
                } catch (Exception ex) {

                    needNotify = true;
                    errormsg = OneCheckoutErrorMessage.ERROR_PARSING_RESPONSE;
                    trans.setDokuResponseCode(errormsg.value());  
                    trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
                    trans.setDokuInvokeStatusDatetime(new Date());
                    em.merge(trans);

                    sendingOK = false;
                    sendReversal(trans,m,pChannel);
//    //                trxHelper = super.createRedirectAndNotifyCaseFail(trxHelper, errormsg, needNotify, trans);
//                    trxHelper = this.doShowResultPage(trxHelper, pChannel);
//                    trxHelper.setMessage("VALID");                 
//                    return trxHelper;                    

                }    
            }    

//            String statusResp = xml.getString("status");



            HashMap<String, String> params = super.getData(trans);

            params.put("PAYMENTCODE", "");
            
            
            String resp = super.notifyStatusMerchant(trans, params, channel, ableToReversal, OneCheckoutStepNotify.INVOKE_STATUS);
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doPayment - %s", resp);
    
            String status = xml.getString("status");
            if (status!=null && status.equalsIgnoreCase("Success") && trans.getTransactionsStatus()==OneCheckoutTransactionStatus.FAILED.value()) {            
                sendReversal(trans,m,pChannel);
            }    
            
            if (sendingOK)
                em.merge(trans);

            OneCheckoutRedirectData redirectRequest = null;
            trxHelper.setRedirectDoku(redirectRequest);
            trxHelper.setTransactions(trans);
            //trxHelper = this.doRedirectToMerchant(trxHelper, pChannel);
            trxHelper = this.doShowResultPage(trxHelper, pChannel);

            trxHelper.setMessage("VALID");
            return trxHelper;


            
            
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }
    
    
    private void sendReversal(Transactions trans, Merchants m,PaymentChannel acq) {
        
        try {
            StringBuilder sb = new StringBuilder();

            sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
            sb.append("&").append("TRANSIDMERCHANT=").append(trans.getIncTransidmerchant());
            sb.append("&").append("AMOUNT=").append(OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));

            // HashMap<String, String> params = new HashMap<String, String>();
            OneCheckoutLogger.log("Reversal PARAM : " + sb.toString());
            InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getReversalPaymentUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

            OneCheckoutLogger.log("Reversal Response : " + inetResp.getMsgResponse());
        } catch (Throwable th) {
            th.printStackTrace();

        }
        
    }
    
    
    public Transactions parseXMLPayment(Transactions trans, XMLConfiguration xml) {
        
        try {
            
/*
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>");
        sb.append("<orderResponse>");                                
        sb.append("<order-number>").append(trans.getTransactionMerchantInvoiceNo()).append("</order-number>");     
        sb.append("<purchase-amount>").append(sdf.format(trans.getTransactionAmount())).append("</purchase-amount>");      
        sb.append("<responsecode>").append(trans.getTransactionResponsecode()).append("</responsecode>");      
        sb.append("<responsecodeDetail>").append(trans.getTransactionResponsecodeDetail()).append("</responsecodeDetail>");   
        sb.append("<status>").append("Success").append("</status>");                       
        sb.append("<ApprovalCode>").append(trans.getTransactionHostRefNum().trim()).append("</ApprovalCode>");      
        sb.append("<StatusCode></StatusCode>");            
        sb.append("<installment>false</installment>");         
        sb.append("<plan></plan>");                              
        sb.append("<tenor></tenor>");                           
        sb.append("<monthly></monthly>");                  
        sb.append("<InterestRate></InterestRate>");      
        sb.append("</orderResponse>");
        
        * 
 <?xml version="1.0" encoding="ISO-8859-1" ?>
<orderResponse>                                 
<order-number>INVOICENUMBER</order-number>      
<purchase-amount>AMOUNT</purchase-amount>       
<responsecode>RESPONSECODE</responsecode>       
<status>Success</status>                        
<ApprovalCode>APPROVALCODE</ApprovalCode>       
<StatusCode>STATUSCODE</StatusCode>             
<installment>INSTALLMENT</installment>          
<plan>PLAN</plan>                               
<tenor>TENOR</tenor>                            
<monthly>MONTHLYFEE</monthly>                   
<InterestRate>INTERESTRATE</InterestRate>       
</orderResponse>         
* 
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
            
            
            String responsecodeDetail = xml.getString("responsecodeDetail");
            String responseCode = xml.getString("responsecode");
            String approvalCode = xml.getString("ApprovalCode");
            String issuerBankName = "MANDIRI";

          //  String verifyReason = xml.getString("verifyReason");                                   
        //    String hostRefNum = xml.getString("HostReferenceNumber");
            
            if (responseCode != null && responseCode.length() == 2) {
                responseCode = "00" + responseCode;
            }

            if (responsecodeDetail != null && responsecodeDetail.length() == 4) {
                responseCode = responsecodeDetail;
            }

            trans.setDokuApprovalCode(approvalCode);
            trans.setDokuIssuerBank(issuerBankName);
            trans.setDokuResponseCode(responseCode);
            trans.setDokuHostRefNum(approvalCode);



            trans.setVerifyId("");
            trans.setVerifyScore(-1);
            trans.setVerifyStatus(OneCheckoutDFSStatus.NA.value());
                    

       //     trans.setEduStatus(trans.getVerifyStatus());

            trans.setTransactionsState(OneCheckoutTransactionState.DONE.value());
            trans.setDokuInvokeStatusDatetime(new Date());


            return trans;
        } catch(Exception ex) {
            
            
            return trans;
        }
        
        
        
        
    }
        
    
    public OneCheckoutDataHelper doGetTodayTransaction(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doMIPPayment(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

    public OneCheckoutDataHelper doRetryCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public OneCheckoutDataHelper doVoid(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutVoidRequest voidRequest = trxHelper.getVoidRequest();
        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - T0: %d Start process", (System.currentTimeMillis() - t1));

            Merchants m = trxHelper.getMerchant();
            String invoiceNo = voidRequest.getTRANSIDMERCHANT();
            String sessionId = voidRequest.getSESSIONID();

            String wordDoku = super.generateVoidWords(voidRequest, m);
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - Checking Hash WORDS");

            if (!wordDoku.equalsIgnoreCase(voidRequest.getWORDS())) {
                voidRequest.setVOIDRESPONSE("FAILED");
                trxHelper.setMessage("VALID");
                trxHelper.setVoidRequest(voidRequest);
                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - Hash WORDS doesn't match");

                return trxHelper;
            }

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - Hash WORDS match");


            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - T1: %d querying transaction", (System.currentTimeMillis() - t1));

            Transactions trans = queryHelper.getCheckStatusTransactionBy(invoiceNo, sessionId, acq, OneCheckoutTransactionState.DONE);

            if (trans != null) {

                OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());



                if (status == OneCheckoutTransactionStatus.SUCCESS) {

                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - ready to void");

                    StringBuilder sb = new StringBuilder();

                    sb.append("MALLID=").append(trans.getMerchantPaymentChannel().getPaymentChannelCode());
                    sb.append("&").append("TRANSIDMERCHANT=").append(invoiceNo);
                    sb.append("&").append("HOSTREFNUM=").append(trans.getDokuHostRefNum());
                    
//                    sb.append("&").append("USERNAME=").append(m.getMerchantName());
//                    sb.append("&").append("IP").append(trans.getSystemSession());
//                    sb.append("&").append("REASON").append(voidRequest.getVOIDREASON());

                   // HashMap<String, String> params = new HashMap<String, String>();
                    OneCheckoutLogger.log("VOID PARAM : "+sb.toString());
                    InternetResponse inetResp = super.doFetchHTTP(sb.toString(), acq.getVoidUrl(), m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout());

                    OneCheckoutLogger.log("Merchant Void Response : "+inetResp.getMsgResponse());

                    voidRequest = MandiriClickPayResponseXML.parseResponseXML(inetResp.getMsgResponse(), voidRequest);
                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - void Status : %s", voidRequest.getVoidStatus());
                    if (voidRequest.getVoidStatus().trim().toUpperCase().equalsIgnoreCase("SUCCESS")) {

                        trans.setDokuVoidApprovalCode(voidRequest.getApprovalCode());
                        trans.setDokuVoidDatetime(new Date());
                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setTransactionsStatus(OneCheckoutTransactionStatus.VOIDED.value());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("SUCCESS");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - update trx as voided");

                    } else {

                        trans.setDokuVoidResponseCode(voidRequest.getResponseCode());
                        trans.setDokuVoidDatetime(new Date());

                        if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                            voidRequest.setVOIDRESPONSE(inetResp.getMsgResponse());
                        } else {
                            voidRequest.setVOIDRESPONSE("FAILED");
                        }

                        OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - update response code void");
                    }

                    em.merge(trans);

                } else if (status == OneCheckoutTransactionStatus.VOIDED) {

                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "VOIDED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("SUCCESS");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - trx already voided");

                } else {
                    if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                        voidRequest.setVOIDRESPONSE(generateVoidResponse(trans, "FAILED"));
                    } else {
                        voidRequest.setVOIDRESPONSE("FAILED");
                    }

                    OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - trx can't be voided");
                }

            } else {

                if (voidRequest.getType() != null && voidRequest.getType().length() > 0) {
                    voidRequest.setVOIDRESPONSE(generateVoidResponse(null, ""));
                } else {
                    voidRequest.setVOIDRESPONSE("NOT FOUND");
                }

                OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - trx isn't found");
            }

            trxHelper.setMessage("VALID");
            trxHelper.setVoidRequest(voidRequest);

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doVoid - T3: %d Finish process", (System.currentTimeMillis() - t1));

            return trxHelper;

        } catch (Throwable th) {
            th.printStackTrace();
            voidRequest.setVOIDRESPONSE("PROBLEM");
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
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

        try {

            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doRedirectToMerchant - T0: %d Start process", (System.currentTimeMillis() - t1));

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
                resultData.put("EMAIL", trans.getIncEmail());

                OneCheckoutLogger.log("TRANSACTION STATUS [" + trans.getTransactionsStatus() + "]");

                if (trans.getTransactionsStatus() != OneCheckoutTransactionStatus.SUCCESS.value()) {

                    if (trans.getVerifyStatus() == OneCheckoutDFSStatus.HIGH_RISK.value()) {
                        redirect.setRetry(Boolean.FALSE);
                    }
                    
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A2-1");
                    resultData.put("MSG", "Failed");
                    resultData.put("MSGDETAIL", "Please try again or contact your merchant");

                    redirect.setPageTemplate("transactionFailed.html");
                    redirect.setRetryData(resultData);
                    
                } else {
                    OneCheckoutLogger.log("TRANSACTION STATUS FLAG=0 A4");
                    redirect.setPageTemplate("transactionSuccess.html"); 

                    resultData.put("MSG", "Approved");
                    resultData.put("MSGDETAIL", "Thank you for doing Online Transaction with " + m.getMerchantName());

                    resultData.put("APPROVAL", trans.getDokuApprovalCode());
                    
                    redirect.setRetryData(resultData);
                    OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
                    redirect = super.addDWRegistrationPage(trxHelper, redirect, channel, trans.getMerchantPaymentChannel(), trans);
                }

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

            OneCheckoutLogger.log("OneCheckoutV1PermataVALiteBean.doRedirectToMerchant - T1: %d Register DOKUWallet", (System.currentTimeMillis() - t1));
            OneCheckoutPaymentChannel channel = OneCheckoutPaymentChannel.findType(acq.getPaymentChannelId());
            redirect = super.addDWRegistrationPage(trxHelper, redirect, channel, trans.getMerchantPaymentChannel(), trans);

            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doRedirectToMerchant - T1: %d Finish process", (System.currentTimeMillis() - t1));
            return trxHelper;
        } catch (Throwable th) {
            th.printStackTrace();
            trxHelper.setMessage(th.getMessage());

            return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
        }
    }
    
    public OneCheckoutDataHelper doCheckStatus(OneCheckoutDataHelper trxHelper, PaymentChannel acq) {
        OneCheckoutCheckStatusData statusRequest = trxHelper.getCheckStatusRequest();
//        OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doCheckStatus - T0: %d Start", (System.currentTimeMillis() - t1));
            OneCheckoutCheckStatusData checkStatusRequest = trxHelper.getCheckStatusRequest();
            Merchants m = trxHelper.getMerchant();
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doCheckStatus - T1: %d querying transaction", (System.currentTimeMillis() - t1));
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

                trans = super.CheckStatusCOREClickPayMandiri(trans,m);

                HashMap<String, String> params =  super.getData(trans); 
                trxHelper.setMessage("VALID");
                params.put("PAYMENTCODE","");
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
            OneCheckoutLogger.log("OneCheckoutV1MandiriClickPayDIRECTCOREBean.doCheckStatus - T2: %d Finish", (System.currentTimeMillis() - t1));
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
        
        try {

            OneCheckoutDataPGRedirect redirect = new OneCheckoutDataPGRedirect();

            redirect.setPageTemplate(redirect.getProgressPage());
            redirect.setUrlAction(pChannel.getRedirectPaymentUrlMip());
            redirect.setHttpProtocol(OneCheckoutDataPGRedirect.HTTP_POST);
            redirect.setAMOUNT(paymentRequest.getAMOUNT());
            redirect.setTRANSIDMERCHANT(paymentRequest.getTRANSIDMERCHANT());
            //  redirect.setSTATUSCODE(statusCode);
            HashMap<String, String> data = redirect.getParameters();

            /*if (paymentRequest.getCHAINMERCHANT() == 0) {
                data.put("CHAINNUM", "NA");
            } else {
                data.put("CHAINNUM", paymentRequest.getCHAINMERCHANT() + "");
            }*/

            if (mpc.getPaymentChannelChainCode() == null || mpc.getPaymentChannelChainCode() == 0) {
                data.put("CHAINNUM", "NA");
            } else {
                data.put("CHAINNUM", mpc.getPaymentChannelChainCode() + "");
            }

            data.put("CustIP",paymentRequest.getCUSTIP());
            
            data.put("txtCCode1", paymentRequest.getCHALLENGE_CODE_1());
            data.put("txtCCode2", paymentRequest.getCHALLENGE_CODE_2());
            data.put("txtCCode3", paymentRequest.getCHALLENGE_CODE_3());
            data.put("txtToken", paymentRequest.getRESPONSE_TOKEN());
            data.put("TYPE", "SINGLE");
            data.put("BASKET", paymentRequest.getBASKET());
            data.put("MERCHANTID", mpc.getMerchantPaymentChannelUid());
            data.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            data.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            data.put("CURRENCY", paymentRequest.getCURRENCY());
            data.put("PurchaseCurrency", paymentRequest.getCURRENCY());
            //data.put("MALLID", m.getMerchantCode() + "");
            data.put("MALLID", mpc.getPaymentChannelCode() + "");
            data.put("WORDS", super.generateDokuWords(paymentRequest, mpc));
            data.put("SESSIONID", paymentRequest.getSESSIONID());

            OneCheckoutBaseRules brules = new OneCheckoutBaseRules();
            data.put("coreid", brules.maskingString(paymentRequest.getCARDNUMBER(), "PAN"));
            
            OneCheckoutLogger.log(": : : : : DATA POST TO IBANKING [" + data.toString() + "]");            
            data.put("coreid", paymentRequest.getCARDNUMBER());
            String paymentChannelId = pChannel.getPaymentChannelId();
//            String ocoId = this.generateOcoId(paymentChannelId);
            data.put("OCOID", ocoId);
//            data.put("IP",paymentRequest.getCUSTIP() != null ?  paymentRequest.getCUSTIP() : "");
            
//            data.put("USERNAME",paymentRequest.getUSERNAME() != null ? paymentRequest.getUSERNAME() : "");
//            data.put("USERNAME",mpc.getMerchants().getMerchantName() != null ? mpc.getMerchants().getMerchantName() : "");            
            
//            data.put("REASON",paymentRequest.getPAYREASON() != null ? paymentRequest.getPAYREASON() : "");
                        
            redirect.setParameters(data);
            return redirect;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
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

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")

    public OneCheckoutDataHelper doRefund(RefundHelper refundHelper, MerchantPaymentChannel merchantPaymentChannel) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
}
