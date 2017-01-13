/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.plugins;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.proc.OneCheckoutChannelBase;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.type.OneCheckoutRateStatus;
import com.onechekoutv1.dto.Rates;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 *
 * @author jauhaf
 */
@Stateless
public class ConvertAmountBean implements ConvertAmountLocal {

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    protected EntityManager em;

    public <T extends OneCheckoutDataHelper> boolean afterAuth(T helper) {
        try {

            if (helper.getPaymentRequest().getPURCHASECURRENCY() == null || helper.getPaymentRequest().getPURCHASECURRENCY().equals("")
                    || helper.getPaymentRequest().getCURRENCY() == null || helper.getPaymentRequest().getCURRENCY().equals("")) {
                OneCheckoutLogger.log("INSUFFICIENT PARAMS CURRENCY Or PURCHASECURRENCY ");
                helper.setMessage("Insufficient params");
                return false;
            }
            //:: for temp static convert USD to IDR...
            helper.getPaymentRequest().getPURCHASECURRENCY();
            helper.getPaymentRequest().setCURRENCY("360");
            //:: get active rates..
            Rates r;
            try {
                String hql = "SELECT r FROM Rates r WHERE r.sellCurrencyNum=:currency AND r.buyCurrencyNum=:purchaseCurrency AND r.status=:ratesStatus";
                r = (Rates) em.createQuery(hql)
                        .setParameter("currency", helper.getPaymentRequest().getCURRENCY())
                        .setParameter("purchaseCurrency", helper.getPaymentRequest().getPURCHASECURRENCY())
                        .setParameter("ratesStatus", OneCheckoutRateStatus.NEW.code())
                        .getSingleResult();
            } catch (NoResultException e) {
                OneCheckoutLogger.log("NO RESULT EXCEPTION WHEN GET RATES..");
                helper.setMessage("Failed get rates");
                return false;
            }

            BigDecimal amount = (BigDecimal.valueOf(helper.getPaymentRequest().getAMOUNT())).multiply(r.getFinalRate());
            OneCheckoutLogger.log("ConvertAmountBean.afterAuth : Purchase amount            [%s]", helper.getPaymentRequest().getAMOUNT());
            OneCheckoutLogger.log("ConvertAmountBean.afterAuth : Final Rates                [%s]", r.getFinalRate());
            OneCheckoutLogger.log("ConvertAmountBean.afterAuth : Amount = Purcahse*rates    [%s]", amount);
            helper.setRates(r);
            helper.getPaymentRequest().setAMOUNT((new DecimalFormat("0.00")).format(amount));
            helper.getPaymentRequest().setAMOUNT(amount.doubleValue());
            //generate new words for next step...
            OneCheckoutChannelBase ocoBase = new OneCheckoutChannelBase();
            String newWords = ocoBase.generatePaymentRequestWords(helper.getPaymentRequest(), helper.getMerchant());
            OneCheckoutLogger.log("ConvertAmountBean.afterAuth : new words                  [%s]", newWords);
            helper.getPaymentRequest().setWORDS(newWords);
            //generate new basket for this transaction...
            if (!convertAmountBasket(helper)) {
                helper.setMessage("failed parse basket.");
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        helper.setMessage("unexpected error");
        return false;
    }

    public <T extends OneCheckoutDataHelper> boolean convertAmountBasket(T helper) {
        //ITEM 1,50.00,1,50.00;ITEM 1,50.00,1,50.00;
        try {
            BigDecimal finalRate = helper.getRates().getFinalRate();
            BigDecimal priceItemConvert, totalPriceItemConvert;
            String newBasket = "";
            String[] datas = helper.getPaymentRequest().getBASKET().split(";");
            String[] items;

            if (datas.length == 0) {
                OneCheckoutLogger.log("ConvertAmountBean.convertAmountBasket : datas basket null [%s]", helper.getPaymentRequest().getBASKET());
                return false;
            }

            for (String data : datas) {
                if (data.equals("")) {
                    continue;
                }
                items = data.split(",");
                priceItemConvert = (new BigDecimal(items[1])).multiply(finalRate);
                totalPriceItemConvert = (new BigDecimal(items[3])).multiply(finalRate);
                newBasket += items[0] + "," + priceItemConvert + "," + items[2] + "," + totalPriceItemConvert + ";";
            }
            OneCheckoutLogger.log("ConvertAmountBean.convertAmountBasket : newbasket [%s]", newBasket);
            helper.getPaymentRequest().setBASKET(newBasket, helper.getPaymentRequest().getAMOUNT(),helper.getPaymentRequest().getTRANSIDMERCHANT());
            return true;
        } catch (Throwable ex) {
            OneCheckoutLogger.log("ConvertAmountBean.convertAmountBasket : UE when convert basket [%s]", helper.getPaymentRequest().getBASKET());
            ex.printStackTrace();
        }
        return false;
    }
}
