package com.onechekoutv1.dto;
// Generated Oct 2, 2012 3:11:56 PM by Hibernate Tools 3.2.1.GA


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * SeqMerchantPaymentChannelIdNextval generated by hbm2java
 */
@Entity
@Table(name="seq_merchant_payment_channel_id_nextval"
    ,schema="public"
)
public class SeqMerchantPaymentChannelIdNextval  implements java.io.Serializable {


     private Integer nextval;

    public SeqMerchantPaymentChannelIdNextval() {
    }

    public SeqMerchantPaymentChannelIdNextval(Integer nextval) {
       this.nextval = nextval;
    }
   
     @Id 
    
    @Column(name="nextval")
    public Integer getNextval() {
        return this.nextval;
    }
    
    public void setNextval(Integer nextval) {
        this.nextval = nextval;
    }





}


