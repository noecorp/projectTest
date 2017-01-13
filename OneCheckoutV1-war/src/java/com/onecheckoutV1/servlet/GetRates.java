/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckoutV1.ejb.helper.OneCheckoutV1QueryHelperBeanLocal;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.ejb.util.OneCheckoutVerifyFormatData;
import com.onecheckoutV1.type.OneCheckoutErrorMessage;
import com.onechekoutv1.dto.Merchants;
import com.onechekoutv1.dto.Rates;
import doku.fx.micropay.enums.ECurrency;
import doku.fx.micropay.enums.EMethodType;
import doku.fx.micropay.enums.EResponseCode;
import doku.fx.micropay.helper.RBSRateRequest;
import doku.fx.micropay.helper.RBSRequestHelper;
import doku.fx.micropay.interfaces.RBSFXMicropayInterface;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.owasp.esapi.ESAPI;

/**
 *
 * @author jauhaf
 */
public class GetRates extends HttpServlet {

    private String paramMALLID = "MALLID",
            paramBUYCURRENCY = "BUYCURRENCY",
            paramSELLCURRENCY = "SELLCURRENCY",
            paramVALUEDATE = "VALUEDATE",
            paramWORDS = "WORDS";

    @Override
    public void init() throws ServletException {
        if (ESAPI.securityConfiguration().getResourceDirectory() == null) {
            ESAPI.securityConfiguration().setResourceDirectory("/apps/ESAPI/resources/");
        } else if (!ESAPI.securityConfiguration().getResourceDirectory().equals("/apps/ESAPI/resources/")) {
            ESAPI.securityConfiguration().setResourceDirectory("/apps/ESAPI/resources/");
        }

    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String result = failedResponse(null, null);
        
        String mallId = "";
        String buyCurrencyNum = "";
        String sellCurrencyNum = "";
        String valueDate = "";
        String words = "";
        
        try {
            /* TODO output your page here. You may use following sample code. */
            ESAPI.httpUtilities().setCurrentHTTP(request, response);
            HttpServletRequest req = ESAPI.httpUtilities().getCurrentRequest();

            mallId = req.getParameter(paramMALLID);
            buyCurrencyNum = req.getParameter(paramBUYCURRENCY);
            sellCurrencyNum = req.getParameter(paramSELLCURRENCY);
            valueDate = req.getParameter(paramVALUEDATE);
            words = req.getParameter(paramWORDS);

            if (mallId == null || mallId.equals("")) {
                failedResponse(OneCheckoutErrorMessage.INSUFFICIENT_PARAMS.value(), "Insufficient params");
                OneCheckoutLogger.log("Insufficient params MALLID [%s]", mallId + "");
                return;
            } else if (buyCurrencyNum == null || buyCurrencyNum.equals("")) {
                failedResponse(OneCheckoutErrorMessage.INSUFFICIENT_PARAMS.value(), "Insufficient params");
                OneCheckoutLogger.log("Insufficient params BUYCURRENCY [%s]", buyCurrencyNum + "");
                return;
            } else if (sellCurrencyNum == null || sellCurrencyNum.equals("")) {
                failedResponse(OneCheckoutErrorMessage.INSUFFICIENT_PARAMS.value(), "Insufficient params");
                OneCheckoutLogger.log("Insufficient params SELLCURRENCY [%s]", sellCurrencyNum + "");
                return;
            } else if (valueDate == null || valueDate.equals("")) {
                failedResponse(OneCheckoutErrorMessage.INSUFFICIENT_PARAMS.value(), "Insufficient params");
                OneCheckoutLogger.log("Insufficient params VALUEDATE [%s]", valueDate + "");
                return;
            } else if (words == null || words.equals("")) {
                failedResponse(OneCheckoutErrorMessage.INSUFFICIENT_PARAMS.value(), "Insufficient params");
                OneCheckoutLogger.log("Insufficient params WORDS [%s]", words + "");
                return;
            }

            OneCheckoutV1QueryHelperBeanLocal ocoBean = OneCheckoutServiceLocator.lookupLocal(OneCheckoutV1QueryHelperBeanLocal.class);
            Merchants m = ocoBean.getMerchantBy(Integer.valueOf(mallId));
            if (m == null) {
                failedResponse(OneCheckoutErrorMessage.INVALID_MERCHANT.value(), "Bad Request");
                OneCheckoutLogger.log("Failed getMerchant by MallId [%s]", mallId + "");
                return;
            }

            String element = mallId + m.getMerchantHashPassword() + sellCurrencyNum + valueDate + buyCurrencyNum;
            String hash = HashWithSHA1.doHashing(element,m.getMerchantShaFormat(),m.getMerchantHashPassword());
            if (!hash.equals(words)) {
                failedResponse(OneCheckoutErrorMessage.WORDS_DOES_NOT_MATCH.value(), "Bad Request");
                OneCheckoutLogger.log("WORDS not match");
                OneCheckoutLogger.log("ELEMENT      [%s]", element + "");
                OneCheckoutLogger.log("MY WORDS     [%s]", hash + "");
                OneCheckoutLogger.log("REQ WORDS    [%s]", words + "");
                return;
            }

            //::: oke now get rate from database...
            Rates rate = ocoBean.getRates(sellCurrencyNum, buyCurrencyNum);
            if (rate == null) {
                OneCheckoutLogger.log("Active rate not found...");
                failedResponse(OneCheckoutErrorMessage.RATE_NOT_FOUND.value(), "Active rate not found");
                return;
            }

            Date tradeValueDate = OneCheckoutVerifyFormatData.stringtoDate(valueDate, "yyyyMMddHHmmss");

            Long remainTimeExpire = getRemindTimeExpire(rate.getExpiryDate());


            if (remainTimeExpire < 0) {
                //this case should not found, if scheduler running properly, 
                //just in case if this appear, tried to get all rates from rbs now.
                OneCheckoutLogger.log("rate already expired [%s] ", rate.getExpiryDate().toString());
                OneCheckoutLogger.log("or interval [%s]", remainTimeExpire + "");

                RBSRequestHelper helper = new RBSRequestHelper();
                RBSRateRequest rr = new RBSRateRequest();
                rr.setSchemeBuyCurrency(ECurrency.getLookup().get(buyCurrencyNum).name());
                rr.setSchemeSellCurrency(ECurrency.getLookup().get(sellCurrencyNum).name());
                rr.setUnitCurrency(ECurrency.getLookup().get(buyCurrencyNum).name());
                helper.setMethodType(EMethodType.GET_RATES);
                helper.setRbsRateRequest(rr);

                RBSFXMicropayInterface intf = new RBSFXMicropayInterface();
                helper = intf.doRequest(helper);
                if (helper == null) {
                    OneCheckoutLogger.log("Failed get.rates, helper is null");
                    return;
                }

                helper = ocoBean.putNewRates(helper);
                if (!helper.getResponseCode().equals(EResponseCode.SUCCESS)) {
                    OneCheckoutLogger.log("Failed putNewRates to DB, responseCode : " + helper.getResponseCode().value());
                    return;
                }

                OneCheckoutLogger.log(":: SUCCESS put rates to db oco, now prepare sending data to ESCROW.");
                //sendingDataToEscrow(helper);
                rate = ocoBean.getRates(sellCurrencyNum, buyCurrencyNum);
                remainTimeExpire = getRemindTimeExpire(rate.getExpiryDate());
            }


            Calendar tradeCal = Calendar.getInstance();
            tradeCal.setTime(tradeValueDate);
            tradeCal.add(Calendar.MINUTE, remainTimeExpire.intValue());
            result = successResponse(
                    rate.getId() + "",
                    rate.getFinalRate(),
                    buyCurrencyNum,
                    sellCurrencyNum,
                    valueDate,
                    OneCheckoutVerifyFormatData.dateToString(tradeCal.getTime(), "yyyyMMddHHmmss"));
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            OneCheckoutLogger.log("result : \n" + result);
            out.print(result);
            out.close();
            mallId = null;
            buyCurrencyNum = null;
            sellCurrencyNum = null;
            valueDate = null;
            words = null;
        }
    }

    private Long getRemindTimeExpire(Date reteExpiredDate) {
        try {
            /*
             * long diffSeconds = diff / 1000 % 60;
             * long diffMinutes = diff / (60 * 1000) % 60;
             * long diffHours = diff / (60 * 60 * 1000) % 24;
             * long diffDays = diff / (24 * 60 * 60 * 1000);
             */
            Long remainTimeExpire = reteExpiredDate.getTime() - (new Date()).getTime();
            remainTimeExpire = remainTimeExpire / (60 * 1000) % 60;
            // less with interval scheduler submit trx RBS.
            Long intervalSubmitRBS = OneCheckoutProperties.getOneCheckoutConfig().getLong("RBS.INTERVAL.SCHEDULER.SUBMITTRX", 5);
            intervalSubmitRBS = intervalSubmitRBS / (60 * 1000) % 60;
            remainTimeExpire = remainTimeExpire - intervalSubmitRBS;

            return remainTimeExpire;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String successResponse(
            String quoteId,
            BigDecimal rate,
            String buyCurrencyNum,
            String sellCurrencyNum,
            String valueDate,
            String expiryDate) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append("<RATE_RESPONSE>");
            sb.append(String.format("<RESPONSECODE>%s</RESPONSECODE>", OneCheckoutErrorMessage.SUCCESS.value()));
            sb.append(String.format("<RESPONSEMSG>%s</RESPONSEMSG>", "Success"));
            sb.append(String.format("<RATEQUOTEID>%s</RATEQUOTEID>", quoteId));
            sb.append(String.format("<RATE>%s</RATE>", rate));
            sb.append(String.format("<BUYCURRENCY>%s</BUYCURRENCY>", buyCurrencyNum));
            sb.append(String.format("<SELLCURRENCY>%s</SELLCURRENCY>", sellCurrencyNum));
            sb.append(String.format("<VALUEDATE>%s</VALUEDATE>", valueDate));
            sb.append(String.format("<EXPIRYDATETIME>%s</EXPIRYDATETIME>", expiryDate));
            sb.append("</RATE_RESPONSE>");
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return failedResponse(null, null);
    }

    private String failedResponse(String respCode, String respMessage) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append("<RATE_RESPONSE>");
            sb.append(String.format("<RESPONSECODE>%s</RESPONSECODE>",
                    (respCode != null ? respCode : OneCheckoutErrorMessage.UNKNOWN.value())));
            sb.append(String.format("<RESPONSEMSG>%s</RESPONSEMSG>",
                    (respMessage != null ? respMessage : "Unknown Error")));
            sb.append("</RATE_RESPONSE>");
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
