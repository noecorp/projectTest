/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.servlet;

import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutServiceLocator;
import com.onecheckoutV1.scheduler.SchedulerNotifyToMerchantLocal;
import com.onecheckoutV1.scheduler.SchedulerNotifyToMerchantV2BeanLocal;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author hafiz
 */
public class NotifyMerchantScheduler extends HttpServlet {
   
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            
            
            String reminder = request.getParameter("SISA");
            String quotient = request.getParameter("PEMBAGI");       
            String trxId = request.getParameter("ID");

            SchedulerNotifyToMerchantV2BeanLocal notifier = OneCheckoutServiceLocator.lookupLocal(SchedulerNotifyToMerchantV2BeanLocal.class);
            
            if (reminder!=null && !reminder.isEmpty() && quotient!=null && !quotient.isEmpty()) {
            
                int rem = Integer.parseInt(reminder);
                int quo = Integer.parseInt(quotient);

                String list = "ERROR:51";
                if (rem<quo)
                    list = notifier.getTransactionList(rem, quo);
                    
                out.print(list);
            
            }
            else if (trxId!=null && !trxId.isEmpty()) {
                
                notifier.doNotify(trxId);
                out.println("OK");
            }
            else {
                String list = "ERROR:50";
                out.print(list);
            }
            

            
        } finally {
            
            out.close();
        }
    }    
//    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
//            throws ServletException, IOException {
//        response.setContentType("text/html;charset=UTF-8");
//        PrintWriter out = response.getWriter();
//        try {
//
//            OneCheckoutLogger.log("NotifyMerchantScheduler.processRequest : starting scheduler");
//
//            SchedulerNotifyToMerchantLocal schedule = OneCheckoutServiceLocator.lookupLocal(SchedulerNotifyToMerchantLocal.class);
//
//            schedule.startSchedulerTimer();
//            OneCheckoutLogger.log("NotifyMerchantScheduler.processRequest Scheduler : up");
//
//
//
//            out.println("<title>Servlet NotifyMerchantScheduler</title>");
//        } finally {
//            out.close();
//        }
//    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
