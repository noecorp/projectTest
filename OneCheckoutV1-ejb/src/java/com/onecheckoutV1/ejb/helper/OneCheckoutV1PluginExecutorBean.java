/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.ejb.helper;

import com.onecheckoutV1.data.OneCheckoutDataHelper;
import com.onecheckoutV1.ejb.merchant.MerchantProcessor;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import com.onechekoutv1.dto.Merchants;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author aditya
 */
@Stateless
public class OneCheckoutV1PluginExecutorBean implements OneCheckoutV1PluginExecutorLocal {

    private PropertiesConfiguration config = OneCheckoutProperties.getOneCheckoutConfig();

    @Resource
    SessionContext context;

    public <T extends OneCheckoutDataHelper> boolean validationMerchantPlugins(T request) {
        Merchants merchant = request.getMerchant() != null ? request.getMerchant() : request.getTransactions().getMerchantPaymentChannel().getMerchants();

        String merchantPlugin = merchant.getMerchantProcessor() != null && merchant.getMerchantProcessor().length() > 0 ? merchant.getMerchantProcessor() : config.getString("MERCHANT.DEFAULT.NOTIFY");

        if (merchantPlugin == null || merchantPlugin.isEmpty()) {
            OneCheckoutLogger.log("::: THERE IS NO PRETRANSACTION PLUGIN FOR VALIDATION INPUT.");
            return true;
        }
        OneCheckoutLogger.log("PLUGINS:::" + merchantPlugin);
        String[] lstPlugins = merchantPlugin.split(";");
        for (String strPlugin : lstPlugins) {
            OneCheckoutLogger.log("START PLUGIN:::" + strPlugin);
            try {
                MerchantProcessor preTrans = (MerchantProcessor) context.lookup(strPlugin);
                if (preTrans.afterTrans(request)) {
                    OneCheckoutLogger.log("END PLUGIN:::" + strPlugin + " replied true");
                } else {
                    OneCheckoutLogger.log("END PLUGIN:::" + strPlugin + " replied false");
                    return false;
                }
            } catch (Throwable e) {
                OneCheckoutLogger.log("", "EXCEPTION WHILE PROCESSING AFTER TRANS PLUGINS :%s", strPlugin);
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
