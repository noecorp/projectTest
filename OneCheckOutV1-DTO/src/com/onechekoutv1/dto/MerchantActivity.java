/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onechekoutv1.dto;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Opiks
 */
@Entity
@Table(name = "merchant_activity")
@NamedQueries({@NamedQuery(name = "MerchantActivity.findAll", query = "SELECT m FROM MerchantActivity m")})
public class MerchantActivity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;
    @Column(name = "message")
    private String message;
    @Column(name = "status")
    private Character status;
    @Column(name = "error_code")
    private String errorCode;
    @Column(name = "data")
    private String data;
    @JoinColumn(name = "activity_id", referencedColumnName = "id")
    @ManyToOne
    private Activity activityId;
    @JoinColumn(name = "merchant_idx", referencedColumnName = "merchant_idx")
    @ManyToOne
    private Merchants merchantIdx;

    public MerchantActivity() {
    }

    public MerchantActivity(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Character getStatus() {
        return status;
    }

    public void setStatus(Character status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Activity getActivityId() {
        return activityId;
    }

    public void setActivityId(Activity activityId) {
        this.activityId = activityId;
    }

    public Merchants getMerchantIdx() {
        return merchantIdx;
    }

    public void setMerchantIdx(Merchants merchantIdx) {
        this.merchantIdx = merchantIdx;
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
        if (!(object instanceof MerchantActivity)) {
            return false;
        }
        MerchantActivity other = (MerchantActivity) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.onechekoutv1.dto.MerchantActivity[id=" + id + "]";
    }

}
