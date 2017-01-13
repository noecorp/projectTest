/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import javax.naming.InitialContext;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutServiceLocator {
    public static final String PACKAGE_NAME = "OneCheckoutV1-ear/";

    public static <T> T lookupLocal(Class<T> localClass) {

        return lookupLocal(localClass, PACKAGE_NAME);
    }

    public static <T> T lookupLocal(Class<T> localClass, String packageName) {
        try {
            InitialContext ic = new InitialContext();
            String name = "";
            if (localClass.getSimpleName().endsWith("BeanLocal"))
                name = localClass.getSimpleName().replace("Local", "");
            else
                name = localClass.getSimpleName().replace("Local", "Bean");

            return (T) ic.lookup(packageName + name + "/local");
        } catch (Throwable th) {
            th.printStackTrace();
            return null;
        }
    }  
}
