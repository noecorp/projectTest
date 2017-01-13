/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import com.onecheckoutV1.data.OneCheckoutPaymentRequest;
import com.onecheckoutV1.ejb.exception.InvalidPaymentRequestException;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.type.OneCheckoutPaymentChannel;
import com.onechekoutv1.dto.Merchants;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutBaseRules extends OneCheckoutChannelBase {

    protected int validateMALLID1(String mallId) {
        String mall_id = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 8, "MALLID", mallId, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
        return Integer.parseInt(mall_id);
    }

    public static int validateMALLID2(String mallId) {

        try {
            String mall_id = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 8, "MALLID", mallId, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            return Integer.parseInt(mall_id);
        } catch (Exception ex) {
            throw new OneCheckoutInvalidInputException("10001|" + ex.getMessage());
        }
    }

    protected int validateCHAINMERCHANT1(String chainMerchantId) {

        if (chainMerchantId.equalsIgnoreCase("NA")) {
            return 0;
        } else {
            String mall_id = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 8, "CHAINMERCHANT", chainMerchantId, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
            return Integer.parseInt(mall_id);
        }
    }

    public static int validateCHAINMERCHANT2(String chainMerchantId) {

        try {
            if (chainMerchantId.equalsIgnoreCase("NA")) {
                return 0;
            } else {
                String mall_id = OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 8, "CHAINMERCHANT", chainMerchantId, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
                return Integer.parseInt(mall_id);
            }
        } catch (Exception ex) {
            throw new OneCheckoutInvalidInputException("10052|" + ex.getMessage());
        }
    }

    protected String validateInt(String value, String parameter, int length) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, length, parameter, value, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
    }

    protected String validateCardNumber(String value, String parameter) {
        return OneCheckoutVerifyFormatData.validateString(16, 16, parameter, value, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
    }

    protected String validateAmexCardNumber(String value, String parameter) {
        return OneCheckoutVerifyFormatData.validateString(15, 16, parameter, value, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
    }

    protected String validateUatpCardNumber(String value, String parameter) {
        return OneCheckoutVerifyFormatData.validateString(15, 16, parameter, value, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
    }

    protected String setExpiryMonth(String expiryMonth) {
        return String.format("%02d", OneCheckoutVerifyFormatData.validateInt(1, 12, "EXPIRYMONTH", expiryMonth));
    }

    protected String setExpiryYear(String expiryYear) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100;
        return String.format("%02d", OneCheckoutVerifyFormatData.validateInt(currentYear, currentYear + 20, "EXPIRYYEAR", expiryYear));
    }

    protected String setCvv2(String cvv2) {
        return OneCheckoutVerifyFormatData.validateString(3, 6, "CVV2", cvv2, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
    }

    protected String validateCurrency(String currency, String parameterName) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 3, parameterName, currency, OneCheckoutVerifyFormatData.NUMERIC_VALUE);

    }

    //   protected OneCheckoutPaymentChannel validatePaymentChannel(String paymentchannel) {
    //       return OneCheckoutPaymentChannel.findType(paymentchannel);
    //   }
    protected String setTransIdMerchant(String invoiceNo) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 40, "TRANSIDMERCHANT", invoiceNo, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
        //return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 16, "TRANSIDMERCHANT", invoiceNo, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String setBookingCode(String bookingCode) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 20, "BOOKINGCODE", bookingCode, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String setFlightNumber(String flightNumber) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 30, "FLIGHTNUMBER", flightNumber, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String setAddress(String address) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 100, "ADDRESS", address, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String setRoute(String route) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 50, "ROUTE", route, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected double validateAmount(String amount, String description) {
        double val = (double) OneCheckoutVerifyFormatData.validateDouble(OneCheckoutVerifyFormatData.ZERO, Double.MAX_VALUE, description, "###,###,##0.00", amount);
        return val;
    }

    protected double validateRegisterAmount(String registerAmount, String description) {
        double val = (double) OneCheckoutVerifyFormatData.validateDouble(OneCheckoutVerifyFormatData.ZERO, Double.MAX_VALUE, description, "###,###,##0.00", registerAmount);
        return val;
    }

    public static double validateAmount1(String amount, String description) {
        double val = (double) OneCheckoutVerifyFormatData.validateDouble(OneCheckoutVerifyFormatData.ZERO, Double.MAX_VALUE, description, "###,###,##0.00", amount);
        return val;
    }

    protected String validateName(String name, String parameterName) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 50, parameterName, name, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String validateCity(String city) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 100, "CITY", city, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String validateState(String state) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 100, "STATE", state, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String validateCountry(String country) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 3, "COUNTRY", country, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String validatePhone(String phone, String parameterName) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 20, parameterName, phone, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    protected String validateZipCode(String zip_code) {
        return OneCheckoutVerifyFormatData.validateString(OneCheckoutVerifyFormatData.ONE, 10, "ZIPCODE", zip_code, OneCheckoutVerifyFormatData.SPECIAL_CHARS_MEDIUM);
    }

    public Date validateDATETIME(String DateTime, String description) {

        String procdtime = OneCheckoutVerifyFormatData.validateString(14, 14, description, DateTime, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

        try {
            Date odate = df.parse(procdtime);
            return odate;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

    public Date validateDATE(String date, String description) {

        String OrderDate = OneCheckoutVerifyFormatData.validateString(8, 8, description, date, OneCheckoutVerifyFormatData.NUMERIC_VALUE);
        DateFormat df = new SimpleDateFormat("yyyyMMdd");

        try {
            Date odate = df.parse(OrderDate);
            return odate;
        } catch (ParseException e) {
            return null;
//            e.printStackTrace();
        }

    }

    /*
     public String maskCard(String cardNumber) {
     String maskedCard = "";
     try {
     if (cardNumber.length() > 5) {
     String prefix = cardNumber.substring(0, 1);
     String postfix = cardNumber.substring(cardNumber.length() - 4);
     String masked = "";
     for (int i = 0; i < cardNumber.length() - 5; i++) {
     masked += "*";
     }
     maskedCard = prefix + masked + postfix;
     } else {
     for (int i = 0; i < cardNumber.length(); i++) {
     cardNumber += "*";
     }
     }
     } catch (Throwable th) {
     OneCheckoutLogger.log("cannot masking value : [%s] , return : [%s]",cardNumber,maskedCard);
     //th.printStackTrace();
     }
     return maskedCard;
     }
     */
    public static String maskingString(String stringVal, String dataType) {
        String maskedRes = "";
        try {
//            if (stringVal==null)
//                maskedRes="";
//            else if (dataType!=null && dataType.equalsIgnoreCase("PAN"))
//                maskedRes = StringUtils.overlay(stringVal, StringUtils.repeat("*", stringVal.length()-5), 1, stringVal.length()-4);
//            else
//                maskedRes = StringUtils.overlay(stringVal, StringUtils.repeat("*", stringVal.length()),0,stringVal.length());

            if (stringVal == null) {
                maskedRes = "";
            } else if (dataType != null && dataType.equalsIgnoreCase("PAN")) {
                maskedRes = StringUtils.overlay(stringVal, StringUtils.repeat("*", stringVal.length() - 10), 6, stringVal.length() - 4);
            } else if (dataType != null && dataType.equals("SHAREDKEY")) {
                maskedRes = StringUtils.overlay(stringVal, StringUtils.repeat("*", stringVal.length() - 5), 2, stringVal.length() - 3);
            } else if (dataType != null && dataType.equals("EXPIRYDATE")) {
                maskedRes = StringUtils.overlay(stringVal, StringUtils.repeat("*", stringVal.length() - 0), 0, 4);
            } else if (dataType != null && dataType.equals("CVV")) {
                maskedRes = StringUtils.overlay(stringVal, StringUtils.repeat("*", stringVal.length() - 0), 0, 3);
            } else {
                maskedRes = StringUtils.overlay(stringVal, StringUtils.repeat("*", stringVal.length()), 0, stringVal.length());
            }

        } catch (Throwable th) {
            OneCheckoutLogger.log("cannot masking value : [%s] , return : [%s]", stringVal, maskedRes);

            maskedRes = stringVal;
        }
        return maskedRes;
    }

    public static OneCheckoutPaymentChannel validatePaymentChannel(String channel) {

        String fChannel = OneCheckoutVerifyFormatData.validateString(2, 2, "PAYMENTCHANNEL", channel, OneCheckoutVerifyFormatData.NUMERIC_VALUE, "UE");

        if (fChannel.equalsIgnoreCase("UE")) {
            return null;
        } else {
            OneCheckoutPaymentChannel paymentChannel = OneCheckoutPaymentChannel.findType(fChannel);
            return paymentChannel;
        }

    }

    protected boolean validateEmail(String email) {

        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {

            result = false;
        }
        return result;
    }

    public boolean validExpNumber(String monthMM, String yearYY) {

        SimpleDateFormat mFormat = new SimpleDateFormat("MM");
        SimpleDateFormat yFormat = new SimpleDateFormat("yy");
        Date d = new Date();
        String month = mFormat.format(d);
        String year = yFormat.format(d);

        int current = Integer.parseInt(year + month);

        int custExp = Integer.parseInt(yearYY + monthMM);

//        OneCheckoutLogger.log("current : %d , expdate : %d", current, custExp);
        if (custExp < current) {
            OneCheckoutLogger.log("Expired Date is not valid");
            return false;
        } else {
            return true;
        }
    }

    public static Merchants getValidMALLID(int mId, int cmId) {

        OneCheckoutV1QueryHelperBeanLocal queryHelper = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);

        try {

            OneCheckoutLogger.log("OneCheckoutBaseRules.getValidMALLID : MALLID=%d", mId);
            Merchants m = queryHelper.getMerchantByMallId(mId, cmId);

            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }
    }

    public boolean validateWords(OneCheckoutPaymentRequest paymentRequest, int mallId, int chain) {
        boolean result = true;

        Merchants merchant = getValidMALLID(mallId, chain);

        String wordDoku = generatePaymentRequestWords(paymentRequest, merchant);

        if (!wordDoku.equalsIgnoreCase(paymentRequest.getWORDS())) {
            result = false;
            throw new OneCheckoutInvalidInputException(String.format("Invalid WORDS from MERCHANT"));
        }

        return result;
    }
}
