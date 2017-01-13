/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onechekoutv1.dto;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 *
 * @author aditya
 */
@Entity()
@Table(name="paypal_response_code", schema="public")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class PaypalResponseCode implements Serializable {

    @Id 
    @Column(name="id", unique=true, nullable=false, length=5)
    private String id;

    @Column(name="paypal_response_code", length=5)
    private String paypalResponseCode;
    
    @Column(name="category", length=50)
    private String category;

    @Column(name="short_message", length=100)
    private String shortMessage;

    @Column(name="long_message", length=300)
    private String longMessage;

    @Column(name="doku_response_code", length=4)
    private String dokuResponseCode;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the paypalResponseCode
     */
    public String getPaypalResponseCode() {
        return paypalResponseCode;
    }

    /**
     * @param paypalResponseCode the paypalResponseCode to set
     */
    public void setPaypalResponseCode(String paypalResponseCode) {
        this.paypalResponseCode = paypalResponseCode;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * @return the shortMessage
     */
    public String getShortMessage() {
        return shortMessage;
    }

    /**
     * @param shortMessage the shortMessage to set
     */
    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    /**
     * @return the longMessage
     */
    public String getLongMessage() {
        return longMessage;
    }

    /**
     * @param longMessage the longMessage to set
     */
    public void setLongMessage(String longMessage) {
        this.longMessage = longMessage;
    }

    /**
     * @return the dokuResponseCode
     */
    public String getDokuResponseCode() {
        return dokuResponseCode;
    }

    /**
     * @param dokuResponseCode the dokuResponseCode to set
     */
    public void setDokuResponseCode(String dokuResponseCode) {
        this.dokuResponseCode = dokuResponseCode;
    }

}