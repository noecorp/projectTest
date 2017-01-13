/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.type;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hafizsjafioedin
 */
public enum OneCheckoutPaymentChannel {

    CreditCard("01"),
    DirectDebitMandiri("02"),
    KlikBCA("03"),
    DOKUPAY("04"),
    PermataVALite("05"),
    BRIPay("06"),
    PermataVAFull("07"),
    MandiriSOALite("08"),
    MandiriSOAFull("09"),
    MandiriVALite("10"),
    MandiriVAFull("11"),
    PayPal("12"),
    BNIDebitOnline("13"),
    Alfamart("14"),
    BSP("15"),
    Tokenization("16"),
    Recur("17"),
    KlikPayBCACard("18"),
    CIMBClicks("19"),
    PTPOS("20"),
    SinarMasVAFull("21"),
    SinarMasVALite("22"),
    MOTO("23"),
    KlikPayBCADebit("24"),
    Muamalat("25"),
    Danamon("26"),
    DompetKu("27"),
    Permata("28"),
    BCAVAFull("29"),
    Indomaret("31"),
    CIMBVA("32"),
    DanamonVA("33"),
    BRIVA("34"),
    AlfaMVA("35"),
    PermataMVA("36"),
    Kredivo("37"),
    BNIVA("38");

    private String type;
    private static final Map<String, OneCheckoutPaymentChannel> lookup = new HashMap<String, OneCheckoutPaymentChannel>();
    private static final long serialVersionUID = 1L;

    static {
        for (OneCheckoutPaymentChannel s : EnumSet.allOf(OneCheckoutPaymentChannel.class)) {
            getLookup().put(s.type, s);
        }
    }

    OneCheckoutPaymentChannel(String type) {
        this.type = type;
    }

    public String value() {
        return type;
    }

    public static Map<String, OneCheckoutPaymentChannel> getLookup() {
        return lookup;
    }

    public static OneCheckoutPaymentChannel findType(String c) {
        OneCheckoutPaymentChannel[] types = OneCheckoutPaymentChannel.values();
        for (OneCheckoutPaymentChannel ttype : types) {
            if (ttype.value().equals(c)) {
                return ttype;
            }
        }
        return null;
    }

}
