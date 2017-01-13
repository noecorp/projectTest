/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onechekoutv1.dto;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
@Entity
@Table(name = "response_code")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ResponseCode.findAll", query = "SELECT r FROM ResponseCode r"),
    @NamedQuery(name = "ResponseCode.findById", query = "SELECT r FROM ResponseCode r WHERE r.id = :id"),
    @NamedQuery(name = "ResponseCode.findByCode", query = "SELECT r FROM ResponseCode r WHERE r.code = :code"),
    @NamedQuery(name = "ResponseCode.findByDescription", query = "SELECT r FROM ResponseCode r WHERE r.description = :description")})
public class ResponseCode implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "code")
    private String code;
    @Column(name = "description")
    private String description;

    public ResponseCode() {
    }

    public ResponseCode(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ResponseCode)) {
            return false;
        }
        ResponseCode other = (ResponseCode) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.onechekoutv1.dto.ResponseCode[ id=" + id + " ]";
    }
    
}
