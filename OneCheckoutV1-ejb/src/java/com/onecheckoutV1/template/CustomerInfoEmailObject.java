/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.template;

/**
 *
 * @author ahmadfirdaus
 */
public class CustomerInfoEmailObject {
    private String paymentCode = "";
    private String customerName = "";
    private String customerAmount = "";
    private String urlAboutAmount = "";
    private String expiredTime = "";
    private String buyCurrencyCode = "";
    private String merchantName = "";
    private double rates;
    private double puschaseRates;
    
    public CustomerInfoEmailObject(){}
    
    public void setPaymentCode(String paymentCode){
        this.paymentCode = paymentCode;
    }
    public String getPaymentCode(){
        return this.paymentCode;
    }
    
    public void setCustomerName(String customerName){
        this.customerName = customerName;
    }
    public String getCustomerName(){
        return this.customerName;
    }
    
    public void setCustomerAmount(String customerAmount){
        this.customerAmount = customerAmount;
    }
    public String getCustomerAmount(){
        return this.customerAmount;
    }
    
    public void setUrlAboutAmount(String urlAboutAmount){
        this.urlAboutAmount = urlAboutAmount;
    }
    public String getUrlAboutAmount(){
        return this.urlAboutAmount;
    }
    
    public void setExpiredTime(String expiredTime){
        this.expiredTime = expiredTime;
    }
    public String getExpiredTime(){
        return this.expiredTime;
    }
    
    public void setBuyCurrencyCode(String buyCurrencyCode){
        this.buyCurrencyCode = buyCurrencyCode;
    }
    public String getBuyCurrencyCode(){
        return this.buyCurrencyCode;
    }
    
    public void setMerchantName(String merchantName){
        this.merchantName = merchantName;
    }
    public String getMerchantName(){
        return this.merchantName;
    }
    
    public void setRates(double rates){
        this.rates = rates;
    }
    public double getRates(){
        return this.rates;
    }
    
    public void setPuschaseRates(double puschaseRates){
        this.puschaseRates = puschaseRates;
    }
    public double getPuschaseRates(){
        return this.puschaseRates;
    }
}
