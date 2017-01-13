/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onechekoutv1.dto;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author Opiks
 */
@Entity
@Table(name = "activity")
@NamedQueries({@NamedQuery(name = "Activity.findAll", query = "SELECT a FROM Activity a")})
public class Activity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    // syamsul
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "description")
    private String description;
    @Basic(optional = false)
    @Column(name = "status")
    private char status;
    @OneToMany(mappedBy = "activityId")
    private Set<MerchantActivity> merchantActivityCollection;

    public Activity() {
    }

    public Activity(Integer id) {
        this.id = id;
    }

    public Activity(Integer id, char status) {
        this.id = id;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public Set<MerchantActivity> getMerchantActivityCollection() {
        return merchantActivityCollection;
    }

    public void setMerchantActivityCollection(Set<MerchantActivity> merchantActivityCollection) {
        this.merchantActivityCollection = merchantActivityCollection;
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
        if (!(object instanceof Activity)) {
            return false;
        }
        Activity other = (Activity) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.onechekoutv1.dto.Activity[id=" + id + "]";
    }

}
