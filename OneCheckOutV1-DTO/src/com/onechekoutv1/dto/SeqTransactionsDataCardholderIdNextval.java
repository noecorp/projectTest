/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onechekoutv1.dto;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author aditya
 */
@Entity
@Table(name="seq_transactions_data_cardholder_id_nextval"
    ,schema="public"
)
public class SeqTransactionsDataCardholderIdNextval implements Serializable {
    private Integer nextval;

    public SeqTransactionsDataCardholderIdNextval() {
    }

    public SeqTransactionsDataCardholderIdNextval(Integer nextval) {
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