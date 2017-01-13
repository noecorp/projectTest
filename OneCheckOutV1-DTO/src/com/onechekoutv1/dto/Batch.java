/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onechekoutv1.dto;

import java.io.Serializable;
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
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author syamsulRudi <syamsulrudi@gmail.com>
 */
//@Entity
//@Table(name = "batch")
//@XmlRootElement
//@NamedQueries({
//    @NamedQuery(name = "Batch.findAll", query = "SELECT b FROM Batch b"),
//    @NamedQuery(name = "Batch.findByBatchId", query = "SELECT b FROM Batch b WHERE b.batchId = :batchId"),
//    @NamedQuery(name = "Batch.findByCoreBatchId", query = "SELECT b FROM Batch b WHERE b.coreBatchId = :coreBatchId"),
//    @NamedQuery(name = "Batch.findByCoreBatchNumber", query = "SELECT b FROM Batch b WHERE b.coreBatchNumber = :coreBatchNumber"),
//    @NamedQuery(name = "Batch.findByMid", query = "SELECT b FROM Batch b WHERE b.mid = :mid"),
//    @NamedQuery(name = "Batch.findByTid", query = "SELECT b FROM Batch b WHERE b.tid = :tid"),
//    @NamedQuery(name = "Batch.findByAcquirerId", query = "SELECT b FROM Batch b WHERE b.acquirerId = :acquirerId"),
//    @NamedQuery(name = "Batch.findByStatus", query = "SELECT b FROM Batch b WHERE b.status = :status"),
//    @NamedQuery(name = "Batch.findByMallId", query = "SELECT b FROM Batch b WHERE b.mallId = :mallId"),
//    @NamedQuery(name = "Batch.findByChainMerchant", query = "SELECT b FROM Batch b WHERE b.chainMerchant = :chainMerchant"),
//    @NamedQuery(name = "Batch.findBySettlementDate", query = "SELECT b FROM Batch b WHERE b.settlementDate = :settlementDate")})
public class Batch implements Serializable {

//    private static final long serialVersionUID = 1L;
//    @Id
//    @Basic(optional = false)
//    @Column(name = "batch_id")
//    private long batchId;
//    @Column(name = "core_batch_id")
//    private String coreBatchId;
//
//    @Column(name = "type")
//    private Character type;
//
//    @Column(name = "core_batch_number")
//    private String coreBatchNumber;
//    @Column(name = "mid")
//    private String mid;
//    @Column(name = "tid")
//    private String tid;
//    @Column(name = "acquirer_id")
//    private String acquirerId;
//    @Column(name = "status")
//    private Character status;
//    @Column(name = "mall_id")
//    private Integer mallId;
//    @Column(name = "chain_merchant")
//    private Integer chainMerchant;
//    @Column(name = "settlement_date")
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date settlementDate;
//
//    public Batch() {
//    }
//
//    public Batch(long batchId) {
//        this.batchId = batchId;
//    }
//
//    public long getBatchId() {
//        return batchId;
//    }
//
//    public void setBatchId(long batchId) {
//        this.batchId = batchId;
//    }
//
//    public String getCoreBatchNumber() {
//        return coreBatchNumber;
//    }
//
//    public void setCoreBatchNumber(String coreBatchNumber) {
//        this.coreBatchNumber = coreBatchNumber;
//    }
//
//    public String getMid() {
//        return mid;
//    }
//
//    public void setMid(String mid) {
//        this.mid = mid;
//    }
//
//    public String getTid() {
//        return tid;
//    }
//
//    public void setTid(String tid) {
//        this.tid = tid;
//    }
//
//    public String getAcquirerId() {
//        return acquirerId;
//    }
//
//    public void setAcquirerId(String acquirerId) {
//        this.acquirerId = acquirerId;
//    }
//
//    public Character getStatus() {
//        return status;
//    }
//
//    public void setStatus(Character status) {
//        this.status = status;
//    }
//
//    public Integer getMallId() {
//        return mallId;
//    }
//
//    public void setMallId(Integer mallId) {
//        this.mallId = mallId;
//    }
//
//    public Integer getChainMerchant() {
//        return chainMerchant;
//    }
//
//    public void setChainMerchant(Integer chainMerchant) {
//        this.chainMerchant = chainMerchant;
//    }
//
//    public Date getSettlementDate() {
//        return settlementDate;
//    }
//
//    public void setSettlementDate(Date settlementDate) {
//        this.settlementDate = settlementDate;
//    }
//
////    @Override
////    public int hashCode() {
////        int hash = 0;
////        hash += (batchId != null ? batchId.hashCode() : 0);
////        return hash;
////    }
////
////    @Override
////    public boolean equals(Object object) {
////        // TODO: Warning - this method won't work in the case the id fields are not set
////        if (!(object instanceof Batch)) {
////            return false;
////        }
////        Batch other = (Batch) object;
////        if ((this.batchId == null && other.batchId != null) || (this.batchId != null && !this.batchId.equals(other.batchId))) {
////            return false;
////        }
////        return true;
////    }
//
//    @Override
//    public String toString() {
//        return "com.onechekoutv1.dto.Batch[ batchId=" + batchId + " ]";
//    }
//
//    public String getCoreBatchId() {
//        return coreBatchId;
//    }
//
//    public void setCoreBatchId(String coreBatchId) {
//        this.coreBatchId = coreBatchId;
//    }
//
//    public Character getType() {
//        return type;
//    }
//
//    public void setType(Character type) {
//        this.type = type;
//    }
    
}
