/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.sa;

import com.google.gson.Gson;
import com.onechekoutv1.dto.Transactions;
import java.util.ArrayList;
import java.util.Calendar;
import javax.ejb.Stateless;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
/**
 *
 * @author ahmadfirdaus
 */
@Stateless
public class TransactionSandboxReportBean implements TransactionSandboxReportBeanLocal {

    @PersistenceContext(unitName = "ONECHECKOUTV1")
    private EntityManager em;
    
    private Session getSession(){
        return (Session)em.getDelegate();
    }
    
    public String getTransaction(int merchantCode, Date day){
        
        Criteria criteria = getSession().createCriteria(Transactions.class);
        criteria.createCriteria("merchantPaymentChannel", "mpc", Criteria.INNER_JOIN);
        criteria.createCriteria("mpc.paymentChannel","pc",Criteria.INNER_JOIN);
        
        criteria.add(Restrictions.eq("incMallid", merchantCode));
        criteria.add(Restrictions.ge("transactionsDatetime", day));
        
        List<Transactions> list = (List<Transactions>) criteria.list();
        List<HashMap> listhashmap = new ArrayList<HashMap>();
        System.out.println(list.size());
        
        for (Transactions transactions : list) {
            HashMap<String, String> obj = new HashMap<String, String>();
            obj.put("invoice", transactions.getIncTransidmerchant());
            obj.put("amount", transactions.getIncAmount().toString());
            obj.put("trans_date", transactions.getTransactionsDatetime().toString());
            obj.put("status", transactions.getTransactionsStatus().toString());
            obj.put("payment_channel", transactions.getMerchantPaymentChannel().getPaymentChannel().getPaymentChannelId());
            
            listhashmap.add(obj);
        }
        
        Gson gson = new Gson();
        return gson.toJson(listhashmap);
    }
}
