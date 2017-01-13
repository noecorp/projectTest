/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

//import com.dokupay.logger.DWLogger;
import com.onecheckoutV1.data.OneCheckoutBasket;
import com.onecheckoutV1.ejb.exception.OneCheckoutInvalidInputException;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutVerifyFormatData {
    public static final int ZERO=0;
    public static final int ONE=1;


    public static final String NOTIFYSTATUS_STOP = "STOP";
    public static final String NOTIFYSTATUS_CONTINUE = "CONTINUE";
    public static final String NOTIFYSTATUS_UNDEFINED = "";
    public static final String NOTIFYSTATUS_TIMEOUT = "00TU";
    public static final String NOTIFYSTATUS_VOID = "VOID";

    // Number Format yeee &
    public static final NumberFormat tenGenerateApprovalCode = new DecimalFormat("0000000000");
    public static final NumberFormat threeDigitMalldId = new DecimalFormat("000");
    public static final NumberFormat fourDigitMalldId = new DecimalFormat("0000");
    public static final NumberFormat elevenTraceNo = new DecimalFormat("00000000000");
    public static final NumberFormat traceNo = new DecimalFormat("00000000");
    public static final NumberFormat traceNoLite = new DecimalFormat("00000000");

    public static final DecimalFormat sdf = new DecimalFormat("########0.00");
    public static final DecimalFormat moneyFormat = new DecimalFormat("###,###.00");
    public static final DecimalFormat sdfnodecimal = new DecimalFormat("########");
    public static final SimpleDateFormat detailDateFormat  = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");
    public static final SimpleDateFormat datetimeFormat  = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat dateFormat  = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat cybersource_datetimeFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm a z");
    public static final SimpleDateFormat mandiri_datetimeFormat  = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public static final SimpleDateFormat klikbca_datetimeFormat  = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public static final SimpleDateFormat email_datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat ticket_dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat recur_dateFormat = new SimpleDateFormat("dd MMMM yyyy");
    public static final SimpleDateFormat dompetku_datetimeFormat  = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    
    
    public static final int DEFAULT_MAX_INT=Integer.MAX_VALUE;
    public static final double DEFAULT_MIN_DOUBLE=0;
    public static final double DEFAULT_MAX_DOUBLE=Double.MAX_VALUE;

    public static final String SPECIAL_CHARS_MEDIUM = "[\\w-,.#&%;+:/=()\\p{Blank}]*?";
    public static final String SPECIAL_CHARS_STRICT = "[\\w\\p{Blank}]*?";
    public static final String NUMERIC_VALUE = "[\\d]*?";
//    public static final String NUMERIC_SYMBOL_VALUE = "[^0-9.,{Blank}]+()";
    public static final String NUMERIC_SYMBOL_VALUE = "[\\d-:()=+., ]+";
    public static final String DOUBLE_VALUE = "[\\d.,]*?";
    public static final String keyStore = System.getProperty("jboss.home.dir")+"/bin/DESedempas.keystore";
//    public static final String keyStore = "/apps/DESedempas.keystore";
    public static final String TIMED_OUT = "TO";
    transient HashMap<String, File> reversalDirs = new HashMap<String, File>();
    transient HashMap<String, File> tmpReversalDirs = new HashMap<String, File>();

    public static final String BILLTYPES_REGEX = "(S|I|D|P)";
    public static final String EXECUTETYPE_REGEX = "(DAY|DATE|FULLDATE)";
    public static final String EXECUTEDATE_DAY_REGEX = "(SUN|MON|TUE|WED|THU|FRI|SAT)";
    public static final String EXECUTEMONTH_REGEX = "(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)";


    public static String validateString(int min, int max, String attribute, String value, String specialCharsRegex) {
        if (value == null) {
            throw new OneCheckoutInvalidInputException(String.format("%s cannot be null", attribute));
        }
        value = value.trim();
        if (value.length() < min || value.length() > max) {
            throw new OneCheckoutInvalidInputException(String.format("%s lenght should be in range between %d and %d", attribute, min, max));
        }

        if (value.matches(specialCharsRegex)) {
            return value;
        }



        throw new OneCheckoutInvalidInputException(String.format("%s has value %s contains invalid chars. Allowed chars are %s ", attribute, value, specialCharsRegex));
    }

    public static String validateStringbyPattern(int min, int max, String attribute, String value, String specialCharsRegex) {
        if (value == null) {
            throw new OneCheckoutInvalidInputException(String.format("%s cannot be null", attribute));
        }
        value = value.trim();
        if (value.length() < min || value.length() > max) {
            throw new OneCheckoutInvalidInputException(String.format("%s lenght should be in range between %d and %d", attribute, min, max));
        }

        Pattern pattern = Pattern.compile(specialCharsRegex);
        Matcher matcher = pattern.matcher(value);
        boolean isMatch = matcher.matches();

        if (isMatch) {
            return value;
        }



        throw new OneCheckoutInvalidInputException(String.format("%s has value %s contains invalid chars. Allowed chars are %s ", attribute, value, specialCharsRegex));
    }


    public static String validateString(int min, int max, String attribute, String value, String specialCharsRegex, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        value = value.trim();
        if (value.length() < min || value.length() > max) {
            throw new OneCheckoutInvalidInputException(String.format("%s lenght should be in range between %d and %d", attribute, min, max));
        }
        if (value.matches(specialCharsRegex)) {
            return value;
        }
        throw new OneCheckoutInvalidInputException(String.format("%s has value %s contains invalid chars. Allowed chars are %s ", attribute, value, specialCharsRegex));
    }

    public static Integer validateInt(int min, int max, String attribute, String value) {
        if (value == null) {
            throw new OneCheckoutInvalidInputException(String.format("%s cannot be null", attribute));
        }
        value = value.trim();
        if (!value.matches(NUMERIC_VALUE)) {
            throw new OneCheckoutInvalidInputException(String.format("%s has value %s contains invalid chars. Allowed chars are %s ", attribute, value, NUMERIC_VALUE));
        }
        int val = Integer.parseInt(value);
        if (val < min || val > max) {
            throw new OneCheckoutInvalidInputException(String.format("%s value should be in range between %d and %d", attribute, min, max));
        }
        return Integer.parseInt(value);
    }

    public static Integer validateInt(int min, int max, String attribute, String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        value = value.trim();
        if (!value.matches(NUMERIC_VALUE)) {
            throw new OneCheckoutInvalidInputException(String.format("%s has value %s contains invalid chars. Allowed chars are %s ", attribute, value, NUMERIC_VALUE));
        }
        int val = Integer.parseInt(value);
        if (val < min || val > max) {
            throw new OneCheckoutInvalidInputException(String.format("%s value should be in range between %d and %d", attribute, min, max));
        }
        return Integer.parseInt(value);
    }

    public static Double validateDouble(double min, double max, String attribute, String pattern, String value) {
        if (value == null) {
            throw new OneCheckoutInvalidInputException(String.format("%s cannot be null", attribute));
        }
        value = value.trim();
        if (!value.matches(DOUBLE_VALUE)) {
            throw new OneCheckoutInvalidInputException(String.format("%s has value %s contains invalid chars. Allowed chars are %s ", attribute, value, DOUBLE_VALUE));
        }
        double val = 0;
        try {
            DecimalFormat df = new DecimalFormat(pattern);
            val = df.parse(value).doubleValue();
        } catch (Exception ex) {
            throw new OneCheckoutInvalidInputException(String.format("%s has value %s having problem in parsing to double with pattern %s", attribute, value, pattern));
        }

        if (val < min || val > max) {
            throw new OneCheckoutInvalidInputException(String.format("%s value should be in range between %d and %d", attribute, min, max));
        }
        return val;
    }

    public static Double validateDouble(double min, double max, String attribute, String value, String pattern, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        value = value.trim();
        if (!value.matches(DOUBLE_VALUE)) {
            throw new OneCheckoutInvalidInputException(String.format("%s has value %s contains invalid chars. Allowed chars are %s ", attribute, value, DOUBLE_VALUE));
        }
        double val = 0;
        try {
            DecimalFormat df = new DecimalFormat(pattern);
            val = df.parse(value).doubleValue();
        } catch (Exception ex) {
            throw new OneCheckoutInvalidInputException(String.format("%s has value %s having problem in parsing to double with pattern %s", attribute, value, pattern));
        }

        if (val < min || val > max) {
            throw new OneCheckoutInvalidInputException(String.format("%s value should be in range between %d and %d", attribute, min, max));
        }
        return val;
    }

    public static String validURL(String input, String dflt)  {

        String safeString;
        try {
            safeString = ESAPI.validator().getValidInput("Check URL Valid", input, "URL", 1000, false);
            return safeString;
        } catch (ValidationException ex) {
            throw new OneCheckoutInvalidInputException(String.format("%s , invalid url", input));

        } catch (IntrusionException ex) {

            throw new OneCheckoutInvalidInputException(String.format("%s , invalid url", input));
        }

    }


    public static List<OneCheckoutBasket> GetValidBasket(String basket, double amount) {
        List<OneCheckoutBasket> al = new ArrayList();

        try {
            OneCheckoutBasket b;

            String[] datas = basket.split(";");
            if (datas.length > 0) {
                BigDecimal totalAllItemPrice = BigDecimal.ZERO;
                for (String data : datas) {

                    String[] items = data.split(",");

                    b = new OneCheckoutBasket();
                    b.setItemName(ESAPI.encoder().encodeForHTML(items[0]));
                    b.setItemPrice(BigDecimal.valueOf(Double.valueOf(items[1])));
                    b.setItemQuantity(Integer.valueOf(items[2]));
                    b.setItemPriceTotal(BigDecimal.valueOf(Double.valueOf(items[3])));
                    al.add(b);


                    BigDecimal quantity = BigDecimal.valueOf(Double.parseDouble(items[2]));

                    if (b.getItemPriceTotal().doubleValue()!=quantity.multiply(b.getItemPrice()).doubleValue()) {
                        throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : %s, %s x %s = %s ", items[0],items[1],items[2],items[3]));
                    }

                    totalAllItemPrice = totalAllItemPrice.add(b.getItemPriceTotal());
                }
                
                if (totalAllItemPrice.doubleValue()!=amount) {
                    throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : TOTALPRICE != AMOUNT, %s = %s ", OneCheckoutVerifyFormatData.sdf.format(amount),OneCheckoutVerifyFormatData.sdf.format(totalAllItemPrice.doubleValue())));
                }

                return al;

            } else {
                throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : Empty basket [%s] ", basket));
            }

        } catch (IntrusionException ex) {

            throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : %s ", ex.getMessage()));

        }
    }
    
    public static List<OneCheckoutBasket> GetValidBasketTokenization(String basket, double amount) {
        List<OneCheckoutBasket> al = new ArrayList();

        try {
            OneCheckoutBasket b;

            String[] datas = basket.split(";");
            if (datas.length > 0) {
                BigDecimal totalAllItemPrice = BigDecimal.ZERO;
                for (String data : datas) {

                    String[] items = data.split(",");

                    b = new OneCheckoutBasket();
                    b.setItemName(ESAPI.encoder().encodeForHTML(items[0]));
                    b.setItemPrice(BigDecimal.valueOf(Double.valueOf(items[1])));
                    b.setItemQuantity(Integer.valueOf(items[2]));
                    b.setItemPriceTotal(BigDecimal.valueOf(Double.valueOf(items[3])));
                    al.add(b);


                    BigDecimal quantity = BigDecimal.valueOf(Double.parseDouble(items[2]));

                    if (b.getItemPriceTotal().doubleValue()!=quantity.multiply(b.getItemPrice()).doubleValue()) {
                        throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : %s, %s x %s = %s ", items[0],items[1],items[2],items[3]));
                    }

                    totalAllItemPrice = totalAllItemPrice.add(b.getItemPriceTotal());
                }
                
//                if (totalAllItemPrice.doubleValue()!=amount) {
//                    throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : TOTALPRICE != AMOUNT, %s = %s ", OneCheckoutVerifyFormatData.sdf.format(amount),OneCheckoutVerifyFormatData.sdf.format(totalAllItemPrice.doubleValue())));
//                }

                return al;

            } else {
                throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : Empty basket [%s] ", basket));
            }

        } catch (IntrusionException ex) {

            throw new OneCheckoutInvalidInputException(String.format("ERROR BASKET : %s ", ex.getMessage()));

        }
    }

    public static String dateToString(Date dt, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(dt);
    }

    public static Date stringtoDate(String str, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.parse(str);
        } catch (ParseException ex) {
            OneCheckoutLogger.log(String.format("Failed parse %s to Date", str));
            OneCheckoutLogger.log("msg error : " + ex.getMessage());
            return null;
        }
    }
}