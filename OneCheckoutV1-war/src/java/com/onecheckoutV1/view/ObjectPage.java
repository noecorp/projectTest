/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.view;

import com.onecheckoutV1.template.CustomerInfoEmailObject;
import com.onecheckoutV1.template.PaymentPageObject;
import freemarker.template.Template;
import java.util.Map;

/**
 *
 * @author hafizsjafioedin
 */
public class ObjectPage {
    
    private Template temp;
    private Map root;
    private PaymentPageObject paymentPage;
    private CustomerInfoEmailObject customerInfoEmailPage;
   
    public ObjectPage() {
        temp = null;
        root = null;
    }

    /**
     * @return the temp
     */
    public Template getTemp() {
        return temp;
    }

    /**
     * @param temp the temp to set
     */
    public void setTemp(Template temp) {
        this.temp = temp;
    }

    /**
     * @return the root
     */
    public Map getRoot() {
        return root;
    }

    /**
     * @param root the root to set
     */
    public void setRoot(Map root) {
        this.root = root;
    }

    /**
     * @return the paymentPage
     */
    public PaymentPageObject getPaymentPage() {
        return paymentPage;
    }

    /**
     * @param paymentPage the paymentPage to set
     */
    public void setPaymentPage(PaymentPageObject paymentPage) {
        this.paymentPage = paymentPage;
    }
    
    public void setCustomerInfoEmailObject(CustomerInfoEmailObject customerInfoEmailObject){
        this.customerInfoEmailPage = customerInfoEmailObject;
    }
    public CustomerInfoEmailObject getCustomerInfoEmailObject(){
        return this.customerInfoEmailPage;
    }
}