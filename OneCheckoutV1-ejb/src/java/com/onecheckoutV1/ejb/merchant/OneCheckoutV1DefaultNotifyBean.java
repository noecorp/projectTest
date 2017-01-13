/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.merchant;

import com.onecheckoutV1.data.OneCheckoutDOKUNotifyData;
import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.data.OneCheckoutEDSUpdateStatusData;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusRequest;
import com.onecheckoutV1.data.OneCheckoutNotifyStatusResponse;
import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutDFSStatus;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onecheckoutV1.type.OneCheckoutStepNotify;
import com.onecheckoutV1.type.OneCheckoutTransactionStatus;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Transactions;
import java.util.Date;
import java.util.HashMap;
import javax.ejb.Stateless;

/**
 *
 * @author hafizsjafioedin
 */
@Stateless
public class OneCheckoutV1DefaultNotifyBean extends OneCheckoutChannelBase implements OneCheckoutV1DefaultNotifyBeanLocal {

    public <T extends OneCheckoutDataHelper> boolean afterTrans(T data) {
        boolean status = false;

        OneCheckoutStepNotify step = data.getStepNotify();

        if (step == OneCheckoutStepNotify.CANCEL_BY_USER) {
            status = cancelByUser(data);
        } else if (step == OneCheckoutStepNotify.NOT_GOOD_REQUEST) {
            status = notGoodRequest(data);
        } else if (step == OneCheckoutStepNotify.INVOKE_STATUS) {
            status = invokeStatus(data);
        } else if (step == OneCheckoutStepNotify.EDS_UPDATE_STATUS) {
            status = edsUpdateStatus(data);
        } else if (step == OneCheckoutStepNotify.IDENTIFY_PAYMENT) {
            status = identifyPayment(data);
        } else if (step == OneCheckoutStepNotify.VOID_PAYMENT) {
            status = voidPayment(data);
        } else if (step == OneCheckoutStepNotify.REVERSAL_PAYMENT) {
            status = reversalPayment(data);
        }

        return status;
    }

    public <T extends OneCheckoutDataHelper> boolean cancelByUser(T data) {
        boolean returnStatus = false;

        try {
            long t1 = System.currentTimeMillis();
            OneCheckoutPaymentChannel channel = data.getPaymentChannel();

            OneCheckoutPaymentRequest paymentRequest = data.getPaymentRequest();

            Transactions trans = data.getTransactions();
            Merchants m = data.getMerchant();

            HashMap<String, String> params = new HashMap<String, String>();


            params.put("VERIFYID", "");
            params.put("VERIFYSCORE", "-1");
            params.put("VERIFYSTATUS", OneCheckoutDFSStatus.NA.name());


            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.notifyToMerchant - %s", trans.getDokuResponseCode());


            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.notifyToMerchant - T1.1: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));                
            if (paymentRequest.getRATE()!=null) {

                params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getPURCHASEAMOUNT()));
                params.put("CURRENCY", paymentRequest.getPURCHASECURRENCY());
            }                
                            
            
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String word = base.generateNotifyWords(paymentRequest, m,OneCheckoutDFSStatus.NA,OneCheckoutTransactionStatus.FAILED);

            params.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            params.put("APPROVALCODE", "");
            params.put("BANK", "");
            params.put("MCN", "");
            params.put("PAYMENTCHANNEL", channel.value() + "");
            params.put("PAYMENTDATETIME", paymentDate);
            params.put("RESPONSECODE", trans.getDokuResponseCode());
            params.put("RESULTMSG", "FAILED");
            params.put("SESSIONID", paymentRequest.getSESSIONID());
            params.put("STATUSTYPE", "P");
            if (paymentRequest.getPAYCODE()!=null)
                params.put("PAYMENTCODE", paymentRequest.getPAYCODE());
            else    
                params.put("PAYMENTCODE", "");
            params.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());
            params.put("CURRENCY", paymentRequest.getCURRENCY());

            params.put("WORDS", word);
            
            


            OneCheckoutNotifyStatusResponse ack = sendToMerchant( m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());//, paymentRequest.getRATE(), paymentRequest.getPURCHASEAMOUNT(), paymentRequest.getPURCHASECURRENCY());

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.notifyToMerchant - T1.2: %d finish Notify to Merchant", (System.currentTimeMillis() - t1));

            data.setNotifyResponse(ack);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return returnStatus;
    }

    public <T extends OneCheckoutDataHelper> boolean notGoodRequest(T data) {
        boolean status = false;

        try {

            long t1 = System.currentTimeMillis();

            OneCheckoutPaymentChannel channel = data.getPaymentChannel();
            OneCheckoutPaymentRequest paymentRequest = data.getPaymentRequest();
            OneCheckoutErrorMessage errormsg = data.getErrorMessage();
            Merchants m = data.getMerchant();

//            OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
            HashMap<String, String> params = new HashMap<String, String>();


                params.put("VERIFYID", "");
                params.put("VERIFYSCORE", "-1");
                params.put("VERIFYSTATUS", OneCheckoutDFSStatus.NA.name());


            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(new Date());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean_DoPayment_NotGoodRequest - %s", errormsg.name());

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean_DoPayment_NotGoodRequest.doPayment - T1.1: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            if (paymentRequest.getPAYCODE()!=null)
                params.put("PAYMENTCODE", paymentRequest.getPAYCODE());
            else    
                params.put("PAYMENTCODE", "");            
            
            String word = super.generateNotifyWords(paymentRequest, m,OneCheckoutDFSStatus.NA,OneCheckoutTransactionStatus.FAILED);

            params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
            params.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
            params.put("WORDS", word);
            params.put("STATUSTYPE", "P");
            params.put("RESPONSECODE", errormsg.value());
            params.put("APPROVALCODE", "");
            params.put("RESULTMSG", "FAILED");
            params.put("PAYMENTCHANNEL", data.getPaymentChannel().value());
            params.put("SESSIONID", paymentRequest.getSESSIONID());
            params.put("BANK", "");
            params.put("PAYMENTDATETIME", paymentDate);
            params.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());
            params.put("CURRENCY", paymentRequest.getCURRENCY());            

            OneCheckoutNotifyStatusResponse ack = sendToMerchant( m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());//, paymentRequest.getRATE(), paymentRequest.getPURCHASEAMOUNT(), paymentRequest.getPURCHASECURRENCY());


            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean_DoPayment_NotGoodRequest.doPayment - T1.2: %d finish Notify to Merchant", (System.currentTimeMillis() - t1));

            data.setNotifyResponse(ack);

            status = true;

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return status;
    }

    public <T extends OneCheckoutDataHelper> boolean invokeStatus(T data) {
        boolean returnStatus = false;

        try {
            long t1 = System.currentTimeMillis();

            OneCheckoutPaymentChannel channel = data.getPaymentChannel();
         //   OneCheckoutTransactionStatus status = data.getStatus();
            OneCheckoutDOKUNotifyData notifyRequest = data.getNotifyRequest();

            Transactions trans = data.getTransactions();
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();

//            OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
            HashMap<String, String> params = new HashMap<String, String>();

            if (!channel.value().equalsIgnoreCase(OneCheckoutPaymentChannel.CreditCard.value()) || channel.value().equalsIgnoreCase(OneCheckoutPaymentChannel.DOKUPAY.value())) {
                params.put("VERIFYID", "");
                params.put("VERIFYSCORE", "-1");
                params.put("VERIFYSTATUS", OneCheckoutDFSStatus.NA.name());

            } else {
                params.put("VERIFYID", trans.getVerifyId());
                params.put("VERIFYSCORE", trans.getVerifyScore() + "");
                params.put("VERIFYSTATUS", notifyRequest.getDFSStatus().name());
                
            }

            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doInvokeStatuss - %s", trans.getDokuResultMessage());


            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());            
            OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.findType(trans.getVerifyStatus());              
            
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doInvokeStatuss - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));

            //String mword = super.generateNotifyWords(notify, merchant);
            String word = super.generateNotifyWords(trans, m,dfsStatus,status);
  //          notify.setWORDS(mword);

            params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            params.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
            params.put("WORDS", word);
            params.put("STATUSTYPE", "P");
            params.put("RESPONSECODE", trans.getDokuResponseCode());
            params.put("APPROVALCODE", trans.getDokuApprovalCode());
            params.put("RESULTMSG", status.name());
            params.put("PAYMENTCHANNEL", data.getPaymentChannel().value());
            params.put("PAYMENTCODE", trans.getAccountId());
            params.put("SESSIONID", trans.getIncSessionid());
            params.put("BANK", trans.getDokuIssuerBank());
            params.put("PAYMENTDATETIME", paymentDate);
            params.put("CURRENCY", trans.getIncCurrency());
            params.put("PURCHASECURRENCY", trans.getIncPurchasecurrency()); 
            
//            OneCheckoutNotifyStatusResponse ack = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout(),trans.getRates(),trans.getIncPurchaseamount().doubleValue(),trans.getIncPurchasecurrency());
            OneCheckoutNotifyStatusResponse ack = sendToMerchant( m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());//, paymentRequest.getRATE(), paymentRequest.getPURCHASEAMOUNT(), paymentRequest.getPURCHASECURRENCY());


            
            
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doInvokeStatuss : statusNotify : %s", ack.getACKNOWLEDGE());

            data.setNotifyResponse(ack);

            returnStatus = true;

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return returnStatus;
    }

    public <T extends OneCheckoutDataHelper> boolean edsUpdateStatus(T data) {
        boolean returnStatus = false;

        try {
            long t1 = System.currentTimeMillis();

            OneCheckoutEDSUpdateStatusData updateStatus = data.getEdsUpdateStatus();

            Transactions trans = data.getTransactions();
            Merchants m = data.getMerchant();

            HashMap<String, String> params = new HashMap<String, String>();

            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doUpdateEDSStatus - %s", trans.getDokuResultMessage());

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doUpdateEDSStatus - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());            
            OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.findType(trans.getVerifyStatus());              
            
            String word = super.generateNotifyWords(trans, m,dfsStatus,status);

            params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            params.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
            params.put("WORDS", word);
            params.put("STATUSTYPE", "P");
            params.put("RESPONSECODE", trans.getDokuResponseCode());
            params.put("APPROVALCODE", trans.getDokuApprovalCode());
            params.put("RESULTMSG", OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus().charValue()).name());
            params.put("PAYMENTCHANNEL", data.getPaymentChannel().value());
            params.put("PAYMENTCODE", "");
            params.put("SESSIONID", trans.getIncSessionid());
            params.put("BANK", trans.getDokuIssuerBank());
            params.put("MCN", super.createMCN(trans));
            params.put("PAYMENTDATETIME", paymentDate);
            params.put("VERIFYID", trans.getVerifyId());
            params.put("VERIFYSCORE", trans.getVerifyScore() + "");
            params.put("VERIFYSTATUS", updateStatus.getSTATUS().name());
            params.put("CURRENCY", trans.getIncCurrency());
            params.put("PURCHASECURRENCY", trans.getIncPurchasecurrency()); 
            
          //  OneCheckoutNotifyStatusResponse ack = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout());
//            OneCheckoutNotifyStatusResponse ack = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout(),trans.getRates(),trans.getIncPurchaseamount().doubleValue(),trans.getIncPurchasecurrency());
            OneCheckoutNotifyStatusResponse ack = sendToMerchant( m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doUpdateEDSStatus RESPONSE ACK = " + ack);

            //updateStatus.setACKNOWLEDGE("CONTINUE");
            updateStatus.setACKNOWLEDGE(ack.getACKNOWLEDGE());

            data.setEdsUpdateStatus(updateStatus);
            data.setNotifyResponse(ack);

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doUpdateEDSStatus - T2: %d Finish process", (System.currentTimeMillis() - t1));

            returnStatus = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return returnStatus;
    }

    public <T extends OneCheckoutDataHelper> boolean identifyPayment(OneCheckoutDataHelper data) {
        boolean status = false;

        try {

            Merchants merchant = data.getMerchant();

            if (merchant != null && merchant.getMerchantIdentifyUrl() != null && merchant.getMerchantIdentifyUrl().length() > 0) {

                long t1 = System.currentTimeMillis();

                OneCheckoutPaymentChannel channel = data.getPaymentChannel();
                OneCheckoutPaymentRequest paymentRequest = data.getPaymentRequest();

                OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
                HashMap<String, String> params = new HashMap<String, String>();


                if (paymentRequest.getRATE()!=null) {
                    params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getPURCHASEAMOUNT()));
                    params.put("CURRENCY", paymentRequest.getPURCHASECURRENCY());
                }
                else {
                    params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(paymentRequest.getAMOUNT()));
                    params.put("CURRENCY", paymentRequest.getCURRENCY());
                    
                }
                
                params.put("TRANSIDMERCHANT", paymentRequest.getTRANSIDMERCHANT());
                params.put("PAYMENTCHANNEL", data.getPaymentChannel().value());
                params.put("SESSIONID", paymentRequest.getSESSIONID());
                params.put("PURCHASECURRENCY", paymentRequest.getPURCHASECURRENCY());
                params.put("PAYMENTCODE", paymentRequest.getPAYCODE()!=null ? paymentRequest.getPAYCODE() : "" );

                OneCheckoutNotifyStatusResponse ack = super.notifyPaymentChannel(merchant.getMerchantIdentifyUrl(), params, merchant);

                OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.identifyPayment - T1.2: %d finish Identify (notify channel) to Merchant", (System.currentTimeMillis() - t1));

                data.setNotifyResponse(ack);

                status = true;
            } else {
                OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.identifyPayment : Identify Url Of Merchant is Null");
                OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.identifyPayment : Cannot send identify to merchant");
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return status;
    }

    public <T extends OneCheckoutDataHelper> boolean voidPayment(T data) {
        boolean returnStatus = false;

        try {
            long t1 = System.currentTimeMillis();

            OneCheckoutPaymentChannel channel = data.getPaymentChannel();
       //     OneCheckoutTransactionStatus status = data.getStatus();
            OneCheckoutDOKUNotifyData notifyRequest = data.getNotifyRequest();

            Transactions trans = data.getTransactions();
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();

//            OneCheckoutNotifyStatusRequest notify = new OneCheckoutNotifyStatusRequest();
            HashMap<String, String> params = new HashMap<String, String>();

            if (!channel.value().equalsIgnoreCase(OneCheckoutPaymentChannel.CreditCard.value()) || channel.value().equalsIgnoreCase(OneCheckoutPaymentChannel.DOKUPAY.value())) {
                params.put("PAYMENTCODE", trans.getAccountId() + "");

                params.put("VERIFYID", "");
                params.put("VERIFYSCORE", "-1");
                params.put("VERIFYSTATUS", OneCheckoutDFSStatus.NA.name());

//                notify.setVERIFYID("");
//                notify.setVERIFYSCORE("-1");
//                notify.setVERIFYSTATUS(OneCheckoutDFSStatus.NA.name());
            } else {
                params.put("PAYMENTCODE", "");
                params.put("VERIFYID", trans.getVerifyId());
                params.put("VERIFYSCORE", trans.getVerifyScore() + "");
                params.put("VERIFYSTATUS",trans.getVerifyStatus() +"");
            }

//            notify.setAMOUNT(trans.getIncAmount().doubleValue());
//            notify.setAPPROVALCODE(trans.getDokuApprovalCode());
//            notify.setBANK(trans.getDokuIssuerBank());
//            notify.setMCN(trans.getAccountId());
//            notify.setPAYMENTCHANNEL(data.getPaymentChannel().value());

            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doVoid - %s", trans.getDokuResultMessage());
//            notify.setPAYMENTDATETIME(paymentDate);
//            notify.setRESPONSECODE(trans.getDokuResponseCode());
//            notify.setRESULTMSG(status.name());
//            notify.setSESSIONID(trans.getIncSessionid());
//            notify.setSTATUSTYPE("R");
//            notify.setTRANSIDMERCHANT(trans.getIncTransidmerchant());

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doVoid - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());            
            OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.findType(trans.getVerifyStatus());              
            
            //String mword = super.generateNotifyWords(notify, merchant);
            String word = super.generateNotifyWords(trans, m,dfsStatus,status);
//            String mword = super.generateNotifyWords(notify, merchant);
//            notify.setWORDS(mword);

            params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            params.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
            params.put("WORDS", word);
            params.put("STATUSTYPE", "R");
            params.put("RESPONSECODE", trans.getDokuResponseCode());
            params.put("APPROVALCODE", trans.getDokuApprovalCode());
            params.put("RESULTMSG", status.name());
            params.put("PAYMENTCHANNEL", data.getPaymentChannel().value());
            params.put("SESSIONID", trans.getIncSessionid());
            params.put("BANK", trans.getDokuIssuerBank());
            params.put("MCN", super.createMCN(trans));
            params.put("PAYMENTDATETIME", paymentDate);

        //    OneCheckoutNotifyStatusResponse ack = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout());
//            OneCheckoutNotifyStatusResponse ack = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout(),trans.getRates(),trans.getIncPurchaseamount().doubleValue(),trans.getIncPurchasecurrency());
            OneCheckoutNotifyStatusResponse ack = sendToMerchant( m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doVoid : statusNotify : %s", ack.getACKNOWLEDGE());
            String resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE;
            if (status == OneCheckoutTransactionStatus.REVERSED || status == OneCheckoutTransactionStatus.VOIDED) {
                if (!ack.getSTATUS()) {
                    status = OneCheckoutTransactionStatus.REVERSED;
                    //trans.setDokuResponseCode(OneCheckoutErrorMessage.NOTIFY_FAILED.value());
                    trans.setTransactionsStatus(status.value());
                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP;
                    OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doVoid : do reversal");

                }
            }

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doVoid - T4: %d update trx record", (System.currentTimeMillis() - t1));

            notifyRequest.setACKNOWLEDGE(resp);

            data.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doVoid - T5: %d Finish process", (System.currentTimeMillis() - t1));

            returnStatus = true;

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return returnStatus;
    }

    public <T extends OneCheckoutDataHelper> boolean reversalPayment(OneCheckoutDataHelper data) {
        boolean returnStatus = false;

        try {
            long t1 = System.currentTimeMillis();

            OneCheckoutPaymentChannel channel = data.getPaymentChannel();
          //  OneCheckoutTransactionStatus status = data.getStatus();
            OneCheckoutDOKUNotifyData notifyRequest = data.getNotifyRequest();

            Transactions trans = data.getTransactions();
            Merchants m = trans.getMerchantPaymentChannel().getMerchants();

            HashMap<String, String> params = new HashMap<String, String>();


            String paymentDate = OneCheckoutVerifyFormatData.datetimeFormat.format(trans.getDokuInvokeStatusDatetime());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doReversal - %s", trans.getDokuResultMessage());


            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doReversal - T3: %d start Notify to Merchant", (System.currentTimeMillis() - t1));
            OneCheckoutTransactionStatus status = OneCheckoutTransactionStatus.findType(trans.getTransactionsStatus());            
            OneCheckoutDFSStatus dfsStatus = OneCheckoutDFSStatus.findType(trans.getVerifyStatus());              
            
            //String mword = super.generateNotifyWords(notify, merchant);
            String word = super.generateNotifyWords(trans, m,dfsStatus,status);
//            String mword = super.generateNotifyWords(notify, merchant);
//            notify.setWORDS(mword);

            params.put("AMOUNT", OneCheckoutVerifyFormatData.sdf.format(trans.getIncAmount().doubleValue()));
            params.put("TRANSIDMERCHANT", trans.getIncTransidmerchant());
            params.put("WORDS", word);
            params.put("STATUSTYPE", "R");
            params.put("RESPONSECODE", trans.getDokuResponseCode());
            params.put("APPROVALCODE", trans.getDokuApprovalCode());
            params.put("RESULTMSG", status.name());
            params.put("PAYMENTCHANNEL", data.getPaymentChannel().value());
            params.put("SESSIONID", trans.getIncSessionid());
            params.put("BANK", trans.getDokuIssuerBank());
            params.put("MCN", super.createMCN(trans));
            params.put("PAYMENTDATETIME", paymentDate);

            //OneCheckoutNotifyStatusResponse ack = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout());
    //        OneCheckoutNotifyStatusResponse ack = super.notifyMerchant(merchant.getMerchantNotifyStatusUrl(), params, merchant.getMerchantConnectionTimeout(), merchant.getMerchantReadTimeout(), merchant.getMerchantNotifyTimeout(),trans.getRates(),trans.getIncPurchaseamount().doubleValue(),trans.getIncPurchasecurrency());
            OneCheckoutNotifyStatusResponse ack = sendToMerchant( m.getMerchantNotifyStatusUrl(), params, m.getMerchantConnectionTimeout(), m.getMerchantReadTimeout(), m.getMerchantNotifyTimeout());
            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doReversal : statusNotify : %s", ack.getACKNOWLEDGE());
            String resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_CONTINUE;
            if (status == OneCheckoutTransactionStatus.REVERSED || status == OneCheckoutTransactionStatus.VOIDED) {
                if (!ack.getSTATUS()) {
                    status = OneCheckoutTransactionStatus.REVERSED;
                    trans.setTransactionsStatus(status.value());
                    resp = OneCheckoutVerifyFormatData.NOTIFYSTATUS_STOP;
                    OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doReversal : do reversal");

                }
            }

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doReversal - T4: %d update trx record", (System.currentTimeMillis() - t1));

            notifyRequest.setACKNOWLEDGE(resp);

            data.setNotifyRequest(notifyRequest);

            OneCheckoutLogger.log("OneCheckoutV1DefaultNotifyBean.doReversal - T5: %d Finish process", (System.currentTimeMillis() - t1));

            returnStatus = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return returnStatus;
    }
}
