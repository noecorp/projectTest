/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onechekoutv1.dto.MerchantPaymentChannel;
import com.onechekoutv1.dto.PaymentChannel;
import com.onechekoutv1.dto.Transactions;
import java.util.HashMap;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author hafizsjafioedin
 */
@Stateless
public class InterfaceOneCheckoutV1Bean implements InterfaceOneCheckoutV1BeanLocal {

    private static PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();
    @EJB
    protected OneCheckoutV1QueryHelperBeanLocal queryHelper;
    @Resource
    protected SessionContext ctx;

    public OneCheckoutDataHelper ProcessPayment(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doPayment(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessVoid(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doVoid(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessInquiryInvoice(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            String processor = paymentChannel.getPaymentChannelProcessor();

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doInquiryInvoice(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessInvokeStatus(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            String processor = paymentChannel.getPaymentChannelProcessor();

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doInvokeStatus(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessGetEDSData(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {

            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doGetEDSData(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessUpdateEDSStatus(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doUpdateEDSStatus(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public OneCheckoutDataHelper ProcessRedirectToMerchant(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doRedirectToMerchant(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    /*
     public OneCheckoutDataHelper ProcessRetryCheckStatus(OneCheckoutDataHelper trxHelper) {
     try {

     OneCheckoutPaymentChannel channel = trxHelper.getPaymentChannel();
     OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]",channel.name());
     PaymentChannel acq = queryHelper.getPaymentChannelByChannel(channel);
     OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(acq.getPaymentChannelProcessor());

     trxHelper = proc.doRetryCheckStatus(trxHelper, acq);

     return trxHelper;
     } catch (Throwable th) {
     th.printStackTrace();
     return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
     }
     }
     */

    /*

     public OneCheckoutDataHelper ProcessCyberSourceReview(OneCheckoutDataHelper trxHelper) {
     try {

     OneCheckoutPaymentChannel channel = trxHelper.getPaymentChannel();
     OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]",channel.name());
     PaymentChannel acq = queryHelper.getPaymentChannelByChannel(channel);
     OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(acq.getPaymentChannelProcessor());

     trxHelper = proc.doCybersourceReview(trxHelper, acq);

     return trxHelper;
     } catch (Throwable th) {
     th.printStackTrace();
     return trxHelper; //verifer.getFailureReplyURL(null, orderInfo, null, "Exception in Verfication");
     }
     }
     */
    public OneCheckoutDataHelper ProcessCheckStatus(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doCheckStatus(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessGetTodayTransactions(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doGetTodayTransaction(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessRetryCheckStatus(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doRetryCheckStatus(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessMIPPayment(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doMIPPayment(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessCCVoid(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            //oneCheckoutDataHelper = proc.doCCVoid(trxHelper, acq);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessReversal(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doReversal(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessReconcile(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doReconcile(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public OneCheckoutDataHelper ProcessCyberSourceReview(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doCyberSource(oneCheckoutDataHelper, paymentChannel);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }

    public HashMap<String, Object> processDoRefund(RefundHelper refundHelper) {
        HashMap<String, Object> result = null;
        OneCheckoutV1QueryHelperBeanLocal helperLocal = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBean.class);

        try {
            Transactions transactions = refundHelper.getTransactions();
            MerchantPaymentChannel mpc = transactions.getMerchantPaymentChannel();
            String processorRefund = "OneCheckoutV1MultiCurrencyCreditCardDIRECTCOREBean";
//            if (refundType.equalsIgnoreCase(OneCheckOutV2RefundType.ESCROW.value() + "")) {
//                processorRefund = "OneCheckOutV2-ear/OneCheckOutV2SettlementBean/local";
//            } else if (refundType.equalsIgnoreCase(OneCheckOutV2RefundType.ONLINE.value() + "")) {
//                processorRefund = "OneCheckOutV2-ear/OneCheckOutV2DoSettlementMPGBean/local";
//            }

//            OneCheckoutLogger.log("OneCheckOutV2RefundType.ONLINE.value() =* " + OneCheckOutV2RefundType.ONLINE.value());
//            OneCheckoutLogger.log("processor =* " + processorRefund);
            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processorRefund);
//            oneCheckoutDataHelper = proc.doPayment(oneCheckoutDataHelper, paymentChannel);
//            result = proc.doRefund(refundHelper, mpc);
        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log("Error in processDoRefund " + th.getMessage());
        }
        return result;

    }
    
    
    public OneCheckoutDataHelper ProcessRefund(OneCheckoutDataHelper oneCheckoutDataHelper) {
        try {
            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", oneCheckoutDataHelper.getPaymentChannel().name());
//            OneCheckoutLogger.log(": : : GOT CHANNEL ON PROCESSOR [%s]", refundHelper.getPaymentChannel().getPaymentChannelName());
            PaymentChannel paymentChannel = queryHelper.getPaymentChannelByChannel(oneCheckoutDataHelper.getPaymentChannel());
            OneCheckoutChannelBase base = new OneCheckoutChannelBase();
            String processor = base.getCorrectProcessor(oneCheckoutDataHelper, paymentChannel);

            OneCheckoutTransactionProcessor proc = (OneCheckoutTransactionProcessor) ctx.lookup(processor);
            oneCheckoutDataHelper = proc.doRefund(oneCheckoutDataHelper.getRefundHelper(), oneCheckoutDataHelper.getRefundHelper().getTransactions().getMerchantPaymentChannel());
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return oneCheckoutDataHelper;
    }
}
