/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.onecheckoutV1.servlet;

import com.google.gson.Gson;
import com.onecheckoutV1.ejb.util.HashWithSHA1;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.sa.TransactionSandboxReportBeanLocal;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author ahmadfirdaus
 */
public class GetTransactionSandboxReport extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    
    @EJB
    TransactionSandboxReportBeanLocal transactionSandboxReportBeanLocal;
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, ParseException, NoSuchAlgorithmException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            String dateInString = request.getParameter("DAY");
            
            Date day = simpleDateFormat.parse(dateInString);
            int merchant_code = Integer.parseInt(request.getParameter("MERCHANT_CODE"));
            
            String paramWords = request.getParameter("WORDS");
            String words = HashWithSHA1.doHashing(merchant_code + dateInString + "DOKUSandBox", "SHA1", null);
            
            System.out.println(day);
            System.out.println(paramWords);
            System.out.println(words);
            
            if(paramWords.equals(words)){
                String result = transactionSandboxReportBeanLocal.getTransaction(merchant_code, day);
                out.print(result);
            }else{
                Gson gson = new Gson();
                out.print(gson.toJson("INVALID WORDS"));
                OneCheckoutLogger.log("INVALID WORDS");
            }
            
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (ParseException ex) {
            Logger.getLogger(GetTransactionSandboxReport.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(GetTransactionSandboxReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (ParseException ex) {
            Logger.getLogger(GetTransactionSandboxReport.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(GetTransactionSandboxReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
