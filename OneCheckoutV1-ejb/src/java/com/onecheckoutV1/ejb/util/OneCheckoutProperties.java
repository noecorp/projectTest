/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import java.io.File;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutProperties {

    public static final String ONECHECKOUT_SYSTEM_PROPERTIES = "/apps/onecheckoutv1/config.txt";
    public static final String ONECHECKOUT_ERROR_DESCRIPTION = "/apps/onecheckoutv1/error_desc.txt";

    private static transient PropertiesConfiguration oneCheckoutConfig = null;
    private static transient PropertiesConfiguration oneCheckoutErrDesc = null;

    static final Object LOCK1 = new Object();
    static final Object LOCK2 = new Object();
//    static WalletLogger log = WalletLogger.getLogger(WalletCommonUtils.class.getName());

    public static PropertiesConfiguration createConfigFile(String file, boolean disableDelimiter) {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration();
            config.setDelimiterParsingDisabled(disableDelimiter);
            File f = new File(file);
            config.load(file);
            config.setReloadingStrategy(new FileChangedReloadingStrategy());
            return config;
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }

    }

    public static PropertiesConfiguration getOneCheckoutConfig() {
        synchronized (LOCK1) {
            if (oneCheckoutConfig == null) {
                oneCheckoutConfig = createConfigFile(ONECHECKOUT_SYSTEM_PROPERTIES, false);
            }
        }
        return oneCheckoutConfig;
    }

    public static PropertiesConfiguration getOneCheckoutErrDesc() {
        synchronized (LOCK2) {
            if (oneCheckoutErrDesc == null) {
                oneCheckoutErrDesc = createConfigFile(ONECHECKOUT_ERROR_DESCRIPTION, false);
            }
        }
        return oneCheckoutErrDesc;
    }


}
