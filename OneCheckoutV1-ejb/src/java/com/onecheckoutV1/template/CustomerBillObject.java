/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.template;

import doku.mpg.xml.MPGPaymentResponseDocument;
import doku.mpg.xml.MPGPaymentResponseDocument.MPGPaymentResponse;
import java.io.Serializable;
import java.io.StringReader;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Opiks
 */
public class CustomerBillObject implements Serializable {

    private String billnumber = "";
    private String amount = "";
    private String currency = "";
    private String billdetail = "";
    private String billtype = "";
    private String billstatus = "";
    private String startdate = "";
    private String enddate = "";
    private String executetype = "";
    private String executedate = "";
    private String executemonth = "";
    private String flatstatus = "";

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getBilldetail() {
        return billdetail;
    }

    public void setBilldetail(String billdetail) {
        this.billdetail = billdetail;
    }

    public String getBillnumber() {
        return billnumber;
    }

    public void setBillnumber(String billnumber) {
        this.billnumber = billnumber;
    }

    public String getBillstatus() {
        return billstatus;
    }

    public void setBillstatus(String billstatus) {
        this.billstatus = billstatus;
    }

    public String getBilltype() {
        return billtype;
    }

    public void setBilltype(String billtype) {
        this.billtype = billtype;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    public String getExecutedate() {
        return executedate;
    }

    public void setExecutedate(String executedate) {
        this.executedate = executedate;
    }

    public String getExecutemonth() {
        return executemonth;
    }

    public void setExecutemonth(String executemonth) {
        this.executemonth = executemonth;
    }

    public String getExecutetype() {
        return executetype;
    }

    public void setExecutetype(String executetype) {
        this.executetype = executetype;
    }

    public String getFlatstatus() {
        return flatstatus;
    }

    public void setFlatstatus(String flatstatus) {
        this.flatstatus = flatstatus;
    }

    public String getStartdate() {
        return startdate;
    }

    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }
}
