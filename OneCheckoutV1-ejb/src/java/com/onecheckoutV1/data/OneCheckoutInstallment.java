/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.data;

import java.io.Serializable;

/**
 *
 * @author iskandar
 */
public class OneCheckoutInstallment implements Serializable{
        
        private String code,description ,issuer ,plan, interest,tenor,trxCode,installmentStatus;
        private double monthlyInstallment;
        
        public OneCheckoutInstallment() {}
        
        public OneCheckoutInstallment(String code, String description, String issuer, String plan, String interest,String tenor,double monthlyInstallment,String trxCode,String installmentStatus) {
            this.code = code;
            this.description = description;
            this.issuer = issuer;
            this.plan = plan;
            this.interest = interest;
            this.tenor = tenor;
            this.monthlyInstallment=monthlyInstallment;
            this.trxCode = trxCode;
            this.installmentStatus = installmentStatus;
        }

        public String getInstallmentStatus() {
            return installmentStatus;
        }

        public void setInstallmentStatus(String installmentStatus) {
            this.installmentStatus = installmentStatus;
        }
        
        public String getTrxCode() {
            return trxCode;
        }

        public void setTrxCode(String trxCode) {
            this.trxCode = trxCode;
        }

        public double getMonthlyInstallment() {
            return monthlyInstallment;
        }

        public void setMonthlyInstallment(double monthlyInstallment) {
            this.monthlyInstallment = monthlyInstallment;
        }

        public String getTenor() {
            return tenor;
        }

        public void setTenor(String tenor) {
            this.tenor = tenor;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getPlan() {
            return plan;
        }

        public void setPlan(String plan) {
            this.plan = plan;
        }

        public String getInterest() {
            return interest;
        }

        public void setInterest(String interest) {
            this.interest = interest;
        }
}
