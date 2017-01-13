/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @desc menyimpan data informasi customer untuk tokenization
 * @author rienditya
 */
public class CustomerInfoObject implements Serializable {

    private String mallid;
    private String identifiertype;
    private String requestdatetime;
    private String customerid;
    private String customername;
    private String customeremail;
    private String customeraddress;
    private String customercity;
    private String customerstate;
    private String customercountry;
    private String customerzipcode;
    private String customermobilephone;
    private String customerhomephone;
    private String customerworkphone;
    private List<CustomerTokenObject> tokens = new ArrayList<CustomerTokenObject>();

    public String getCustomeraddress() {
        return customeraddress;
    }

    public void setCustomeraddress(String customeraddress) {
        this.customeraddress = customeraddress;
    }

    public String getCustomercity() {
        return customercity;
    }

    public void setCustomercity(String customercity) {
        this.customercity = customercity;
    }

    public String getCustomercountry() {
        return customercountry;
    }

    public void setCustomercountry(String customercountry) {
        this.customercountry = customercountry;
    }

    public String getCustomeremail() {
        return customeremail;
    }

    public void setCustomeremail(String customeremail) {
        this.customeremail = customeremail;
    }

    public String getCustomerhomephone() {
        return customerhomephone;
    }

    public void setCustomerhomephone(String customerhomephone) {
        this.customerhomephone = customerhomephone;
    }

    public String getCustomerid() {
        return customerid;
    }

    public void setCustomerid(String customerid) {
        this.customerid = customerid;
    }

    public String getCustomermobilephone() {
        return customermobilephone;
    }

    public void setCustomermobilephone(String customermobilephone) {
        this.customermobilephone = customermobilephone;
    }

    public String getCustomername() {
        return customername;
    }

    public void setCustomername(String customername) {
        this.customername = customername;
    }

    public String getCustomerstate() {
        return customerstate;
    }

    public void setCustomerstate(String customerstate) {
        this.customerstate = customerstate;
    }

    public String getCustomerworkphone() {
        return customerworkphone;
    }

    public void setCustomerworkphone(String customerworkphone) {
        this.customerworkphone = customerworkphone;
    }

    public String getCustomerzipcode() {
        return customerzipcode;
    }

    public void setCustomerzipcode(String customerzipcode) {
        this.customerzipcode = customerzipcode;
    }

    public String getIdentifiertype() {
        return identifiertype;
    }

    public void setIdentifiertype(String identifiertype) {
        this.identifiertype = identifiertype;
    }

    public String getMallid() {
        return mallid;
    }

    public void setMallid(String mallid) {
        this.mallid = mallid;
    }

    public String getRequestdatetime() {
        return requestdatetime;
    }

    public void setRequestdatetime(String requestdatetime) {
        this.requestdatetime = requestdatetime;
    }

    public List<CustomerTokenObject> getTokens() {
        return tokens;
    }

    public void setTokens(List<CustomerTokenObject> tokens) {
        this.tokens = tokens;
    }
}