/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import java.security.Principal;
import org.jboss.security.SecurityAssociation;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutLogger {

    public static String getSecurityAssociationInfo() {
        Principal p = SecurityAssociation.getCallerPrincipal();
        if (p != null) {
            return p.getName();
        } else {
            return "";
        }
    }

    public static void setSecurityAssociation(String sessionId){
               SecurityAssociation.setPrincipal(new OneCheckoutPrincipal(sessionId));
    }

    public static void unsetSecurityAssociation(){
            SecurityAssociation.clear();
    }
    public static void log(String message, Object... params) {
        try {
            String msg = "["+getSecurityAssociationInfo() + "] "+ String.format( message, params);
            System.out.println(msg);
        } catch (Exception ex) {

        }
    }

    public static void log(String message) {
        try {
            String msg = "["+getSecurityAssociationInfo() + "] "+ String.format( message);
            System.out.println(msg);
        } catch (Exception ex) {

        }
    }        
}
