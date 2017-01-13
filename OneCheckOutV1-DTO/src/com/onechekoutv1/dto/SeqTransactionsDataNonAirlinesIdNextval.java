/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onechekoutv1.dto;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author aditya
 */
@Entity
@Table(name="seq_transactions_data_non_airlines_id_nextval"
    ,schema="public"
)
public class SeqTransactionsDataNonAirlinesIdNextval {
    private Integer nextval;

    public SeqTransactionsDataNonAirlinesIdNextval() {
    }

    public SeqTransactionsDataNonAirlinesIdNextval(Integer nextval) {
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