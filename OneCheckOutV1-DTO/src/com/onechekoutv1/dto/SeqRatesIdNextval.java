/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onechekoutv1.dto;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author jauhaf
 */
@Entity
@Table(name = "seq_rates_id_nextval")
@NamedQueries({
    @NamedQuery(name = "SeqRatesIdNextval.findAll", query = "SELECT s FROM SeqRatesIdNextval s")})
public class SeqRatesIdNextval implements Serializable {

    private static final long serialVersionUID = 1L;
    @Column(name = "nextval")
    @Id
    private Integer nextval;

    public SeqRatesIdNextval() {
    }

    public Integer getNextval() {
        return nextval;
    }

    public void setNextval(Integer nextval) {
        this.nextval = nextval;
    }
}
