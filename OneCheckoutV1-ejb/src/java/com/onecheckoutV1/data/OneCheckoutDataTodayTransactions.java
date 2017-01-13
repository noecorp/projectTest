/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author aditya
 */
public class OneCheckoutDataTodayTransactions  implements Serializable {

    private static final long serialVersionUID = 1L;
    private ArrayList rowData;
    private HashMap<String, String>  parameters;
    private String urlAction;
    private String pageTemplate;
    private int enableButton;
    private String cookie;
    private String httpProtocol;


    public OneCheckoutDataTodayTransactions() {
        pageTemplate = "viewKlikBCAToday.html";
        httpProtocol = OneCheckoutDataPGRedirect.HTTP_POST;
        parameters = new HashMap();
        enableButton = 1;
    }

    /**
     * @return the parameters
     */
    public HashMap<String, String> getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the urlAction
     */
    public String getUrlAction() {
        return urlAction;
    }

    /**
     * @param urlAction the urlAction to set
     */
    public void setUrlAction(String urlAction) {
        this.urlAction = urlAction;
    }

    /**
     * @return the pageTemplate
     */
    public String getPageTemplate() {
        return pageTemplate;
    }

    /**
     * @param pageTemplate the pageTemplate to set
     */
    public void setPageTemplate(String pageTemplate) {
        this.pageTemplate = pageTemplate;
    }

    /**
     * @return the enableButton
     */
    public int getEnableButton() {
        return enableButton;
    }

    /**
     * @param enableButton the enableButton to set
     */
    public void setEnableButton(int enableButton) {
        this.enableButton = enableButton;
    }

    /**
     * @return the cookie
     */
    public String getCookie() {
        return cookie;
    }

    /**
     * @param cookie the cookie to set
     */
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * @return the httpProtocol
     */
    public String getHttpProtocol() {
        return httpProtocol;
    }

    /**
     * @param httpProtocol the httpProtocol to set
     */
    public void setHttpProtocol(String httpProtocol) {
        this.httpProtocol = httpProtocol;
    }

    /**
     * @return the rowData
     */
    public ArrayList getRowData() {
        return rowData;
    }

    /**
     * @param rowData the rowData to set
     */
    public void setRowData(ArrayList rowData) {
        this.rowData = rowData;
    }
}
