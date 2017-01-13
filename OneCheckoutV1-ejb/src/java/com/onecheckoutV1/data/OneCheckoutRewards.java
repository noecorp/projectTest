/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import java.io.Serializable;

/**
 *
 * @author margono
 */
public class OneCheckoutRewards implements Serializable{
    private int availablePoint;
    private int pointRedeemed;
    private double amountRedeemed;
    
    public OneCheckoutRewards(){}
    
    public OneCheckoutRewards(int availablePoint, int pointRedeemed, double amountRedeemed){
        this.availablePoint = availablePoint;
        this.pointRedeemed = pointRedeemed;
        this.amountRedeemed = amountRedeemed;
    }

    public int getAvailablePoint() {
        return availablePoint;
    }

    public void setAvailablePoint(int availablePoint) {
        this.availablePoint = availablePoint;
    }

    public int getPointRedeemed() {
        return pointRedeemed;
    }

    public void setPointRedeemed(int pointRedeemed) {
        this.pointRedeemed = pointRedeemed;
    }

    public double getAmountRedeemed() {
        return amountRedeemed;
    }

    public void setAmountRedeemed(double amountRedeemed) {
        this.amountRedeemed = amountRedeemed;
    }
    
}