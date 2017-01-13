/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author aditya
 */
public class OneCheckoutBasket implements Serializable {

    private String itemName;
    //private String itemPrice;
    private BigDecimal itemPrice;
    private Integer itemQuantity;
    private BigDecimal itemPriceTotal;
    
    /**
     * @return the itemName
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * @param itemName the itemName to set
     */
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    /**
     * @return the itemPrice
     */
    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    /**
     * @param itemPrice the itemPrice to set
     */
    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    /**
     * @return the itemQuantity
     */
    public Integer getItemQuantity() {
        return itemQuantity;
    }

    /**
     * @param itemQuantity the itemQuantity to set
     */
    public void setItemQuantity(Integer itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    /**
     * @return the itemPriceTotal
     */
    public BigDecimal getItemPriceTotal() {
        return itemPriceTotal;
    }

    /**
     * @param itemPriceTotal the itemPriceTotal to set
     */
    public void setItemPriceTotal(BigDecimal itemPriceTotal) {
        this.itemPriceTotal = itemPriceTotal;
    }
}
