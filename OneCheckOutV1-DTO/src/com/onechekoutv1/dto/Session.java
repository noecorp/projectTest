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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Type;

/**
 *
 * @author Opiks
 */
@Entity
@Table(name = "session")
@NamedQueries({@NamedQuery(name = "Session.findAll", query = "SELECT s FROM Session s")})
public class Session implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "request_code")
    private String requestCode;
    @Column(name = "create_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;
    @Column(name = "execute_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date executeDate;
    @Type(type="org.hibernate.type.BinaryType")
    @Column(name = "session")
    private byte[] session;
    @Column(name = "status")
    private Character status;
    @JoinColumn(name = "merchant_idx", referencedColumnName = "merchant_idx")
    @ManyToOne(fetch = FetchType.LAZY)
    private Merchants merchantIdx;

    public Session() {
    }

    public Session(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getExecuteDate() {
        return executeDate;
    }

    public void setExecuteDate(Date executeDate) {
        this.executeDate = executeDate;
    }

    public byte[] getSession() {
        return session;
    }

    public void setSession(byte[] session) {
        this.session = session;
    }

    public Character getStatus() {
        return status;
    }

    public void setStatus(Character status) {
        this.status = status;
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
        if (!(object instanceof Session)) {
            return false;
        }
        Session other = (Session) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.onechekoutv1.dto.Session[id=" + id + "]";
    }

}
