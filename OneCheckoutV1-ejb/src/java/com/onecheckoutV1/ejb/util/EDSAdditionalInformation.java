/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.type.OneCheckoutThirdPartyStatus;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author hafizsjafioedin
 */
public class EDSAdditionalInformation {
    


    private static String checkingDateFlight(Date transTime, Date flightTime) {
        String additionalInfo = "";
        try {
            long different = 0;
            long minute = 0;
            long hour = 0;


            Calendar calTrans = Calendar.getInstance();
            calTrans.setTime(transTime);
            Calendar calFlight = Calendar.getInstance();
            calFlight.setTime(flightTime);

            // MENGHITUNG PERBEDAAN DALAM MILISECOND
            different = calFlight.getTimeInMillis() - calTrans.getTimeInMillis();
            // MENDAPATKAN JUMLAH PERBEDAAN DALAM SATUAN JAM
            hour = different / 3600000;

            // MENDAPATKAN JUMLAH PERBEDAAN DALAM SATUAN MENIT
            minute = (different - (3600000 * hour)) / 60000;

            // DIGABUNGKAN MENJADI FORMAT SEPERTI INI, CONTOH : 8.27
            float result = Float.parseFloat(hour + "." + minute);
            if (result < 24.00) {
                DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
                Date date = calFlight.getTime();
                additionalInfo = dateFormat.format(date);
                OneCheckoutLogger.log("additionalInfo  [" + additionalInfo + "]");
                OneCheckoutLogger.log(":::   Departure less than 24 hours ");
            } else {
            }

        } catch (Throwable th) {
            th.printStackTrace();
        }

        return additionalInfo;
    }    
    
    
    public static String getAdditionalInfoForEdu(OneCheckoutPaymentRequest payReq) {
        
        String additionalInfo3rdparty = "";
        String additionalInfo24Hours = "";

        String res = " ";
        try {
            
            //String tTime, String fTime
            Date tTime = payReq.getREQUESTDATETIME();
            Date fTime = payReq.getFLIGHTDATETIME();
            
            String CHName = payReq.getCC_NAME();
//            String[] psgList = payReq.getPASSENGER_NAME();

            String additionalInfo = "";
            if (payReq.getTHIRDPARTY_STATUS()==OneCheckoutThirdPartyStatus.TravelArrangerJointTheFlight)
                additionalInfo = "PARTY";
            
            
            additionalInfo24Hours = checkingDateFlight(tTime, fTime);
//            additionalInfo3rdparty = checkPassengerName(psgList, CHName);

            if (!additionalInfo24Hours.equalsIgnoreCase("") && !additionalInfo3rdparty.equalsIgnoreCase("")) {
                
                res = additionalInfo24Hours + "_" + additionalInfo3rdparty;
            }
            else if (!additionalInfo24Hours.equalsIgnoreCase("")) {
                
                res = additionalInfo24Hours;
            }
            else if (!additionalInfo3rdparty.equalsIgnoreCase("")) {
                res = additionalInfo3rdparty;
                
            }
            else 
                res = "";

            OneCheckoutLogger.log("ADDITIONALINFO : %s", res);
            return res;
        } catch (Throwable th) {
            th.printStackTrace();
            return res;
        }        
        
        
    }
        
    
}
