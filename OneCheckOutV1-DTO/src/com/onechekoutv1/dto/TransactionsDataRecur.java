/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onechekoutv1.dto;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 *
 * @author hafiz
 */
@Entity
@Table(name="transactions_data_recur"
    ,schema="public"
)
public class TransactionsDataRecur implements java.io.Serializable {
    
     private long transactionsId;
     private Transactions transactions;
     private String incBillNumber;
     private String incBillDetail;
     private char incBillType;
     private String incStartDate;
     private String incEndDate;
     private char incExecuteType;
     private String incExecuteDate;
     private String incExecuteMonth;
     private String incAdditionalData1;
     private String incAdditionalData2;
     private String registrationResponse;

    public TransactionsDataRecur() {
    }

	
    public TransactionsDataRecur(long transactionsId, Transactions transactions, String incBillNumber, String incBillDetail, char incBillType, String incStartDate, String incEndDate, char incExecuteType, String incExecuteDate, String incExecuteMonth, String incAdditionalData1, String incAdditionalData2) {
        this.transactionsId = transactionsId;
        this.transactions = transactions;
        this.incBillNumber = incBillNumber;
        this.incBillDetail = incBillDetail;
        this.incBillType = incBillType;
        this.incStartDate = incStartDate;
        this.incEndDate = incEndDate;
        this.incExecuteType = incExecuteType;
        this.incExecuteDate = incExecuteDate;
        this.incExecuteMonth = incExecuteMonth;
        this.incAdditionalData1 = incAdditionalData1;
        this.incAdditionalData2 = incAdditionalData2;
        
        
    }
    

   
     @Id     
    @Column(name="id", unique=true, nullable=false)
    public long getTransactionsId() {
        return this.transactionsId;
    }
    
    public void setTransactionsId(long transactionsId) {
        this.transactionsId = transactionsId;
    }
    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactions_id", unique=true, nullable=false)
    public Transactions getTransactions() {
        return this.transactions;
    }
    
    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }
    
      
    @Column(name="inc_bill_number", nullable=false, length=50)
    public String getIncBillNumber() {
        return this.incBillNumber;
    }
    
    public void setIncBillNumber(String incBillNumber) {
        this.incBillNumber = incBillNumber;
    }
    
    @Column(name="inc_bill_detail", nullable=false, length=100)
    public String getIncBillDetail() {
        return this.incBillDetail;
    }
    
    public void setIncBillDetail(String incBillDetail) {
        this.incBillDetail = incBillDetail;
    }
    
    @Column(name="inc_bill_type", nullable=false, length=1)
    public char getIncBillType() {
        return this.incBillType;
    }
    
    public void setIncBillType(char incBillType) {
        this.incBillType = incBillType;
    }
    
    @Column(name="inc_start_date", nullable=false, length=20)
    public String getIncStartDate() {
        return this.incStartDate;
    }
    
    public void setIncStartDate(String incStartDate) {
        this.incStartDate = incStartDate;
    }
    
    @Column(name="inc_end_date", nullable=false, length=20)
    public String getIncEndDate() {
        return this.incEndDate;
    }
    
    public void setIncEndDate(String incEndDate) {
        this.incEndDate = incEndDate;
    }
         
    @Column(name="inc_execute_type", nullable=false, length=1)
    public char getIncExecuteType() {
        return this.incExecuteType;
    }
    
    public void setIncExecuteType(char incExecuteType) {
        this.incExecuteType = incExecuteType;
    }
    
    @Column(name="inc_execute_date", nullable=false, length=20)
    public String getIncExecuteDate() {
        return this.incExecuteDate;
    }
    
    public void setIncExecuteDate(String incExecuteDate) {
        this.incExecuteDate = incExecuteDate;
    }
    
    @Column(name="inc_execute_month", nullable=false, length=20)
    public String getIncExecuteMonth() {
        return this.incExecuteMonth;
    }
    
    public void setIncExecuteMonth(String incExecuteMonth) {
        this.incExecuteMonth = incExecuteMonth;
    }
    
    @Column(name="inc_additional_data1", length=100)
    public String getIncAdditionalData1() {
        return this.incAdditionalData1;
    }
    
    public void setIncAdditionalData1(String incAdditionalData1) {
        this.incAdditionalData1 = incAdditionalData1;
    }
    
    @Column(name="inc_additional_data2", length=100)
    public String getIncAdditionalData2() {
        return this.incAdditionalData2;
    }
    
    public void setIncAdditionalData2(String incAdditionalData2) {
        this.incAdditionalData2 = incAdditionalData2;
    }
    
    @Column(name="registration_response", length=2024)
    public String getRegistrationResponse() {
        return this.registrationResponse;
    }
    
    public void setRegistrationResponse(String registrationResponse) {
        this.registrationResponse = registrationResponse;
    }    
}
