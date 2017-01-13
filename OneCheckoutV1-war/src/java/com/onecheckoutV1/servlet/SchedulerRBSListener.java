/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onecheckoutV1.servlet;

import com.onecheckout.process.SchedulerRBSJob;
import com.onecheckoutV1.ejb.util.OneCheckoutLogger;
import com.onecheckoutV1.ejb.util.OneCheckoutProperties;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

/**
 *
 * @author jauhaf
 */
public class SchedulerRBSListener extends HttpServlet implements ServletContextListener {

    private String CRON_TIME_SCHEDULE = OneCheckoutProperties.getOneCheckoutConfig().getString("RBS.QUARTZUTILITY.JOBTIME.GETALLRATES.RBS", "0 0,5,10 11,23 * * ?");
    private String CRON_TRIGGER_NAME = "CronTriggerGetAllRates";
    private String CRON_JOB_NAME = "JOB_getAllRates";
    private String CRON_GROUP = "group1";

    public void contextInitialized(ServletContextEvent sce) {
        try {
////            String timeTrigger = "0 0 " + hours + " ? * " + dayOfWeek; // Second   Minute   Hours   Day-of-Month   Mounth    Day-of-Week
//            Scheduler sch = new StdSchedulerFactory().getScheduler();
//            JobDetail job = JobBuilder.newJob(SchedulerRBSJob.class)
//                    .withIdentity(CRON_JOB_NAME, CRON_GROUP)
//                    .build();
//
//            CronTrigger trigger = TriggerBuilder.newTrigger()
//                    .withIdentity(CRON_TRIGGER_NAME, CRON_GROUP)
//                    .withSchedule(CronScheduleBuilder.cronSchedule(CRON_TIME_SCHEDULE))
//                    .build();
//
//            sch.start();
//            sch.scheduleJob(job, trigger);
//            OneCheckoutLogger.log("Format Time Send Report : : [ " + CRON_TRIGGER_NAME + "] : : > " + CRON_TIME_SCHEDULE);
//            OneCheckoutLogger.log("::: STARTING SCHEDULER GET.ALLRATES RBS :::");

        } catch (Throwable th) {
            th.printStackTrace();
            OneCheckoutLogger.log(" ==== FAILED STARTED SCHEDULER GET.ALLRATES RBS ====");
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        try {
//            Scheduler sch = new StdSchedulerFactory().getScheduler();
//            sch.shutdown();
//            //sch.deleteJob(JobKey.jobKey(CRON_JOB_NAME, CRON_GROUP));
//            //sch.unscheduleJob(TriggerKey.triggerKey(CRON_TRIGGER_NAME, CRON_GROUP));
//            OneCheckoutLogger.log("::: SUCCESS SHUTDOWN SCHEDULER GET.ALLRATES RBS :::");
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            CRON_TIME_SCHEDULE = OneCheckoutProperties.getOneCheckoutConfig().getString("RBS.QUARTZUTILITY.JOBTIME.GETALLRATES.RBS", "0 0,5,10 11,23 * * ?");
            /* TODO output your page here. You may use following sample code. */
            OneCheckoutLogger.log("Format Time Send Report : : [ " + CRON_TRIGGER_NAME + "] : : > " + CRON_TIME_SCHEDULE);
            Scheduler sch = new StdSchedulerFactory().getScheduler();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(CRON_TRIGGER_NAME, CRON_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(CRON_TIME_SCHEDULE))
                    .build();
            sch.rescheduleJob(TriggerKey.triggerKey(CRON_TRIGGER_NAME, CRON_GROUP), trigger);
            OneCheckoutLogger.log("::: SUCCESS RESCHEDULER CRON QUARTZ :::");
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
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
        try {
            OneCheckoutLogger.log("MANUAL EXECUTE GET RATES NOW");            
            SchedulerRBSJob jobs = new SchedulerRBSJob();
            jobs.execute(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>   
}
