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
 * @author hafiz
 */
@Entity
@Table(name="seq_transactions_data_recur_id_nextval"
    ,schema="public"
)
public class SeqTransactionsDataRecurIdNextval implements Serializable {
    private Integer nextval;

    public SeqTransactionsDataRecurIdNextval() {
    }

    public SeqTransactionsDataRecurIdNextval(Integer nextval) {
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
