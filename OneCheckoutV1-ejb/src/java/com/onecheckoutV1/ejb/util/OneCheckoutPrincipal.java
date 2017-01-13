/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.ejb.util;

import java.security.Principal;

/**
 *
 * @author hafizsjafioedin
 */
public class OneCheckoutPrincipal  implements Principal{

    String name;
    public OneCheckoutPrincipal(String name){
            this.name="OneCheckoutV1_" + name;
    }
    public String getName() {
            return name;
    }
}
