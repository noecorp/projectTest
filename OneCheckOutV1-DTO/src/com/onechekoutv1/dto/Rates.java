/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onechekoutv1.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author jauhaf
 */
@Entity
@Table(name = "rates")
@NamedQueries({@NamedQuery(name = "Rates.findAll", query = "SELECT r FROM Rates r") })
public class Rates implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "sell_currency_code")
    private String sellCurrencyCode;
    @Column(name = "sell_currency_num")
    private String sellCurrencyNum;
    @Column(name = "buy_currency_code")
    private String buyCurrencyCode;
    @Column(name = "buy_currency_num")
    private String buyCurrencyNum;
    @Column(name = "rate_quote_id")
    private String rateQuoteId;
    @Column(name = "segment_id")
    private Integer segmentId;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "origin_rate")
    private BigDecimal originRate;
    @Column(name = "margin_rate")
    private BigDecimal marginRate;
    @Column(name = "final_rate")
    private BigDecimal finalRate;
    @Column(name = "status")
    private Character status;
    @Column(name = "value_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date valueDate;
    @Column(name = "expiry_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;
    @Column(name = "request_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestDate;
    @Column(name = "rate_type")
    private Character rateType;

    public Rates() {
    }

    public Rates(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSellCurrencyCode() {
        return sellCurrencyCode;
    }

    public void setSellCurrencyCode(String sellCurrencyCode) {
        this.sellCurrencyCode = sellCurrencyCode;
    }

    public String getSellCurrencyNum() {
        return sellCurrencyNum;
    }

    public void setSellCurrencyNum(String sellCurrencyNum) {
        this.sellCurrencyNum = sellCurrencyNum;
    }

    public String getBuyCurrencyCode() {
        return buyCurrencyCode;
    }

    public void setBuyCurrencyCode(String buyCurrencyCode) {
        this.buyCurrencyCode = buyCurrencyCode;
    }

    public String getBuyCurrencyNum() {
        return buyCurrencyNum;
    }

    public void setBuyCurrencyNum(String buyCurrencyNum) {
        this.buyCurrencyNum = buyCurrencyNum;
    }

    public String getRateQuoteId() {
        return rateQuoteId;
    }

    public void setRateQuoteId(String rateQuoteId) {
        this.rateQuoteId = rateQuoteId;
    }

    public Integer getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Integer segmentId) {
        this.segmentId = segmentId;
    }

    public BigDecimal getOriginRate() {
        return originRate;
    }

    public void setOriginRate(BigDecimal originRate) {
        this.originRate = originRate;
    }

    public BigDecimal getMarginRate() {
        return marginRate;
    }

    public void setMarginRate(BigDecimal marginRate) {
        this.marginRate = marginRate;
    }

    public BigDecimal getFinalRate() {
        return finalRate;
    }

    public void setFinalRate(BigDecimal finalRate) {
        this.finalRate = finalRate;
    }

    public Character getStatus() {
        return status;
    }

    public void setStatus(Character status) {
        this.status = status;
    }

    public Date getValueDate() {
        return valueDate;
    }

    public void setValueDate(Date valueDate) {
        this.valueDate = valueDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public Character getRateType() {
        return rateType;
    }

    public void setRateType(Character rateType) {
        this.rateType = rateType;
    }
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Rates)) {
            return false;
        }
        Rates other = (Rates) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.onechekoutv1.dto.Rates[ id=" + id + " ]";
    }
}
