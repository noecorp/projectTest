package com.onechekoutv1.dto;
// Generated Oct 2, 2012 3:11:56 PM by Hibernate Tools 3.2.1.GA


import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TransactionsDataAirlines generated by hbm2java
 */
@Entity
@Table(name="transactions_data_airlines"
    ,schema="public"
)
public class TransactionsDataAirlines  implements java.io.Serializable {


     private long transactionsId;
     private Transactions transactions;
     private String incFlight;
     private char incFlighttype;
     private String incBookingcode;
     private String incRoute;
     private String incFlightdate;
     private String incFlighttime;
     private String incFlightnumber;
     private String incPassengerName;
     private String incPassengerType;
     private String incFFNumber;
     private char incThirdpartyStatus;
     private BigDecimal incVat;
     private BigDecimal incInsurance;
     private BigDecimal incFuelsurcharge;

    public TransactionsDataAirlines() {
    }

	
    public TransactionsDataAirlines(long transactionsId, Transactions transactions, String incFlight, char incFlighttype, String incBookingcode, String incRoute, String incFlightdate, String incFlighttime, String incFlightnumber, String incPassengerName, String incPassengerType, char incThirdpartyStatus) {
        this.transactionsId = transactionsId;
        this.transactions = transactions;
        this.incFlight = incFlight;
        this.incFlighttype = incFlighttype;
        this.incBookingcode = incBookingcode;
        this.incRoute = incRoute;
        this.incFlightdate = incFlightdate;
        this.incFlighttime = incFlighttime;
        this.incFlightnumber = incFlightnumber;
        this.incPassengerName = incPassengerName;
        this.incPassengerType = incPassengerType;
        this.incThirdpartyStatus = incThirdpartyStatus;
    }
    public TransactionsDataAirlines(long transactionsId, Transactions transactions, String incFlight, char incFlighttype, String incBookingcode, String incRoute, String incFlightdate, String incFlighttime, String incFlightnumber, String incPassengerName, String incPassengerType, char incThirdpartyStatus, BigDecimal incVat, BigDecimal incInsurance, BigDecimal incFuelsurcharge) {
       this.transactionsId = transactionsId;
       this.transactions = transactions;
       this.incFlight = incFlight;
       this.incFlighttype = incFlighttype;
       this.incBookingcode = incBookingcode;
       this.incRoute = incRoute;
       this.incFlightdate = incFlightdate;
       this.incFlighttime = incFlighttime;
       this.incFlightnumber = incFlightnumber;
       this.incPassengerName = incPassengerName;
       this.incPassengerType = incPassengerType;
       this.incThirdpartyStatus = incThirdpartyStatus;
       this.incVat = incVat;
       this.incInsurance = incInsurance;
       this.incFuelsurcharge = incFuelsurcharge;
    }
   
    @Id 
    @Column(name="id", unique=true, nullable=false)
    public long getTransactionsId() {
        return this.transactionsId;
    }
    
    public void setTransactionsId(long transactionsId) {
        this.transactionsId = transactionsId;
    }
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactions_id", unique=true, nullable=false)
    public Transactions getTransactions() {
        return this.transactions;
    }
    
    public void setTransactions(Transactions transactions) {
        this.transactions = transactions;
    }
    
    @Column(name="inc_flight", nullable=false, length=2)
    public String getIncFlight() {
        return this.incFlight;
    }
    
    public void setIncFlight(String incFlight) {
        this.incFlight = incFlight;
    }
    
    @Column(name="inc_flighttype", nullable=false, length=1)
    public char getIncFlighttype() {
        return this.incFlighttype;
    }
    
    public void setIncFlighttype(char incFlighttype) {
        this.incFlighttype = incFlighttype;
    }
    
    @Column(name="inc_bookingcode", nullable=false, length=20)
    public String getIncBookingcode() {
        return this.incBookingcode;
    }
    
    public void setIncBookingcode(String incBookingcode) {
        this.incBookingcode = incBookingcode;
    }
    
    @Column(name="inc_route", nullable=false, length=50)
    public String getIncRoute() {
        return this.incRoute;
    }
    
    public void setIncRoute(String incRoute) {
        this.incRoute = incRoute;
    }
    
    @Column(name="inc_flightdate", nullable=false, length=256)
    public String getIncFlightdate() {
        return this.incFlightdate;
    }
    
    public void setIncFlightdate(String incFlightdate) {
        this.incFlightdate = incFlightdate;
    }
    
    @Column(name="inc_flighttime", nullable=false, length=256)
    public String getIncFlighttime() {
        return this.incFlighttime;
    }
    
    public void setIncFlighttime(String incFlighttime) {
        this.incFlighttime = incFlighttime;
    }
    
    @Column(name="inc_flightnumber", nullable=false, length=256)
    public String getIncFlightnumber() {
        return this.incFlightnumber;
    }
    
    public void setIncFlightnumber(String incFlightnumber) {
        this.incFlightnumber = incFlightnumber;
    }
    
    @Column(name="inc_passenger_name", nullable=false, length=256)
    public String getIncPassengerName() {
        return this.incPassengerName;
    }
    
    public void setIncPassengerName(String incPassengerName) {
        this.incPassengerName = incPassengerName;
    }
    
    @Column(name="inc_passenger_type", nullable=false, length=256)
    public String getIncPassengerType() {
        return this.incPassengerType;
    }
    
    public void setIncPassengerType(String incPassengerType) {
        this.incPassengerType = incPassengerType;
    }
    
    @Column(name="inc_thirdparty_status", nullable=false, length=1)
    public char getIncThirdpartyStatus() {
        return this.incThirdpartyStatus;
    }
    
    public void setIncThirdpartyStatus(char incThirdpartyStatus) {
        this.incThirdpartyStatus = incThirdpartyStatus;
    }
    
    @Column(name="inc_vat", precision=12)
    public BigDecimal getIncVat() {
        return this.incVat;
    }
    
    public void setIncVat(BigDecimal incVat) {
        this.incVat = incVat;
    }
    
    @Column(name="inc_insurance", precision=12)
    public BigDecimal getIncInsurance() {
        return this.incInsurance;
    }
    
    public void setIncInsurance(BigDecimal incInsurance) {
        this.incInsurance = incInsurance;
    }
    
    @Column(name="inc_fuelsurcharge", precision=12)
    public BigDecimal getIncFuelsurcharge() {
        return this.incFuelsurcharge;
    }
    
    public void setIncFuelsurcharge(BigDecimal incFuelsurcharge) {
        this.incFuelsurcharge = incFuelsurcharge;
    }

    /**
     * @return the incFFNumber
     */
    @Column(name="inc_ffnumber", nullable=false, length=256)
    public String getIncFFNumber() {
        return incFFNumber;
    }

    /**
     * @param incFFNumber the incFFNumber to set
     */
    public void setIncFFNumber(String incFFNumber) {
        this.incFFNumber = incFFNumber;
    }
    
}