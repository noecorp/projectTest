/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @desc menyimpan data informasi token customer
 * @author rienditya
 */
public class CustomerTokenObject implements Serializable {

    private String tokenid;
    private String cardnumber;
    private String expirydate;
    private String ccname;
    private String ccemail;
    private String ccaddress;
    private String cccity;
    private String ccstate;
    private String cccountry;
    private String cczipcode;
    private String ccmobilephone;
    private String cchomephone;
    private String ccworkphone;
    private String tokenstatus;
    private List<CustomerBillObject> bills = new ArrayList<CustomerBillObject>();

    public String getCardnumber() {
        return cardnumber;
    }

    public void setCardnumber(String cardnumber) {
        this.cardnumber = cardnumber;
    }

    public String getCcaddress() {
        return ccaddress;
    }

    public void setCcaddress(String ccaddress) {
        this.ccaddress = ccaddress;
    }

    public String getCccity() {
        return cccity;
    }

    public void setCccity(String cccity) {
        this.cccity = cccity;
    }

    public String getCccountry() {
        return cccountry;
    }

    public void setCccountry(String cccountry) {
        this.cccountry = cccountry;
    }

    public String getCcemail() {
        return ccemail;
    }

    public void setCcemail(String ccemail) {
        this.ccemail = ccemail;
    }

    public String getCchomephone() {
        return cchomephone;
    }

    public void setCchomephone(String cchomephone) {
        this.cchomephone = cchomephone;
    }

    public String getCcmobilephone() {
        return ccmobilephone;
    }

    public void setCcmobilephone(String ccmobilephone) {
        this.ccmobilephone = ccmobilephone;
    }

    public String getCcname() {
        return ccname;
    }

    public void setCcname(String ccname) {
        this.ccname = ccname;
    }

    public String getCcstate() {
        return ccstate;
    }

    public void setCcstate(String ccstate) {
        this.ccstate = ccstate;
    }

    public String getCcworkphone() {
        return ccworkphone;
    }

    public void setCcworkphone(String ccworkphone) {
        this.ccworkphone = ccworkphone;
    }

    public String getCczipcode() {
        return cczipcode;
    }

    public void setCczipcode(String cczipcode) {
        this.cczipcode = cczipcode;
    }

    public String getExpirydate() {
        return expirydate;
    }

    public void setExpirydate(String expirydate) {
        this.expirydate = expirydate;
    }

    public String getTokenid() {
        return tokenid;
    }

    public void setTokenid(String tokenid) {
        this.tokenid = tokenid;
    }

    public String getTokenstatus() {
        return tokenstatus;
    }

    public void setTokenstatus(String tokenstatus) {
        this.tokenstatus = tokenstatus;
    }

    public List<CustomerBillObject> getBills() {
        return bills;
    }

    public void setBills(List<CustomerBillObject> bills) {
        this.bills = bills;
    }
}