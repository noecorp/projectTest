package com.onechekoutv1.dto;
// Generated Oct 2, 2012 3:11:56 PM by Hibernate Tools 3.2.1.GA


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Country generated by hbm2java
 */
@Entity
@Table(name="country"
    ,schema="public"
)
public class Country  implements java.io.Serializable {


     private String id;
     private String name;
     private String numericCode;
     private String alpha3Code;

    public Country() {
    }

	
    public Country(String id, String name) {
        this.id = id;
        this.name = name;
    }
    public Country(String id, String name, String numericCode, String alpha3Code) {
       this.id = id;
       this.name = name;
       this.numericCode = numericCode;
       this.alpha3Code = alpha3Code;
    }
   
    @Id 
    @Column(name="id", unique=true, nullable=false, length=2)
    public String getId() {
        return this.id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    @Column(name="name", nullable=false, length=50)
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Column(name="numeric_code", length=3)
    public String getNumericCode() {
        return this.numericCode;
    }
    
    public void setNumericCode(String numericCode) {
        this.numericCode = numericCode;
    }
    
    @Column(name="alpha3_code", length=3)
    public String getAlpha3Code() {
        return this.alpha3Code;
    }
    
    public void setAlpha3Code(String alpha3Code) {
        this.alpha3Code = alpha3Code;
    }





}


