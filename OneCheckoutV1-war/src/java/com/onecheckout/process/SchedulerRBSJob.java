/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckout.process;

import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.EmailUtility;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onechekoutv1.dto.Rates;
import doku.fx.micropay.enums.EMethodType;
import doku.fx.micropay.enums.EResponseCode;
import doku.fx.micropay.helper.RBSRequestHelper;
import doku.fx.micropay.interfaces.RBSFXMicropayInterface;
import java.util.Date;
import javax.naming.InitialContext;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author jauhaf
 */
public class SchedulerRBSJob extends OneCheckoutChannelBase implements Job {

    public void execute(JobExecutionContext jec) throws JobExecutionException {
        try {

            OneCheckoutLogger.log(":: STARTING JOB ::");
            InitialContext ic = new InitialContext();
            OneCheckoutV1QueryHelperBeanLocal ocoBean = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            RBSRequestHelper helper = new RBSRequestHelper();
            helper.setMethodType(EMethodType.GET_ALL_RATES);

            boolean isLastCron = false;
            Date currentTime = new Date();
            String lastMinuteCron = OneCheckoutProperties.getOneCheckoutConfig().getString("RBS.QUARTZUTILITY.JOBTIME.LAST.MINUTE");
            if (lastMinuteCron.equals(OneCheckoutVerifyFormatData.dateToString(currentTime, "mm"))) {
                isLastCron = true;
            }

            //------------------------------------------------------------
            // :: check prev, record time.. yyyyMMddHH
            //------------------------------------------------------------
            Rates rate = ocoBean.getLastRate();
            if (rate != null) {
                String recDate = OneCheckoutVerifyFormatData.dateToString(rate.getRequestDate(), "yyyyMMddHH");
                String curDate = OneCheckoutVerifyFormatData.dateToString(currentTime, "yyyyMMddHH");
                OneCheckoutLogger.log("RECORD DATE  : " + recDate);
                OneCheckoutLogger.log("CURRENT DATE : " + curDate);
                if (recDate.equals(curDate)) {
                    OneCheckoutLogger.log("new rate data has been record..");
                    return;
                }
            }

            RBSFXMicropayInterface intf = new RBSFXMicropayInterface();
            helper = intf.doRequest(helper);

            if (helper == null || helper.getResponseCode() == null || helper.getResponseCode().equals(EResponseCode.FAILED)) {
                OneCheckoutLogger.log("Failed get.allrates, helper is null or response failed");
                //if (isLastCron) {
                EmailUtility.sendNotifAlert("getAllrates - response from library RBSFXMicropayInterface null");
                //}
                return;
            }
            if (!helper.getResponseCode().equals(EResponseCode.SUCCESS)) {
                OneCheckoutLogger.log("Failed get.allrates, responseCode : " + helper.getResponseCode().value());
                //if (isLastCron) {
                EmailUtility.sendNotifAlert("getAllrates - Response Code from RBSFXMicropayInterface " + helper.getResponseCode().toString());
                //}
                return;
            }

            helper = ocoBean.putAllNewRates(helper);
            if (!helper.getResponseCode().equals(EResponseCode.SUCCESS)) {
                OneCheckoutLogger.log("Failed putAllNewRates to DB, responseCode : " + helper.getResponseCode().value());
                //if (isLastCron) {
                EmailUtility.sendNotifAlert("getAllrates - Failed when putAllNewRates to DB");
                //}
                return;
            }

            OneCheckoutLogger.log(":: SUCCESS put all rates to db oco, now prepare sending data to ESCROW.");
        } catch (Exception e) {
            OneCheckoutLogger.log(":: EXCEPTION ON JOB...");
            e.printStackTrace();
            EmailUtility.sendNotifAlert("getAllrates - Enexepected error.");
        }
    }

}