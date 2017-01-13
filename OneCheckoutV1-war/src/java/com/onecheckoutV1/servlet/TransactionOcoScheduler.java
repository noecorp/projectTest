/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.scheduler.SchedulerTransactionOcoLocal;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author iskandar
 */
public class TransactionOcoScheduler extends HttpServlet {
    
    protected void processRequest(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException{
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            OneCheckoutLogger.log("TransactionOcoScheduler.processRequest : starting scheduler");
            SchedulerTransactionOcoLocal scheduler = OneCheckoutServiceLocator.lookupLocal(SchedulerTransactionOcoLocal.class);
            scheduler.startSchedulerDaily();
            OneCheckoutLogger.log("TransactionOcoScheduler.processRequest Scheduler : up");
            out.println("<title>Servlet TransactionOcoScheduler</title>");
        } catch (Exception e) {
        }finally{
            out.close();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
