package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.dao.XxlJobLogDao;
import com.xxl.job.dao.XxlJobLogReportDao;
import com.xxl.job.dao.model.XxlJobLogReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * job log report helper
 */
@Slf4j
@Component
public class JobLogReportHelper implements InitializingBean, DisposableBean {

    private Thread logrThread;
    private volatile boolean toStop = false;

    @Autowired
    private XxlJobLogReportDao xxlJobLogReportDao;
    @Autowired
    private XxlJobLogDao xxlJobLogDao;
    @Autowired
    private XxlJobAdminConfig xxlJobAdminConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        logrThread = new Thread(new Runnable() {

            @Override
            public void run() {

                // last clean log time
                long lastCleanLogTime = 0;


                while (!toStop) {

                    // 1、log-report refresh: refresh log report in 3 days
                    try {

                        for (int i = 0; i < 3; i++) {

                            // today
                            Calendar itemDay = Calendar.getInstance();
                            itemDay.add(Calendar.DAY_OF_MONTH, -i);
                            itemDay.set(Calendar.HOUR_OF_DAY, 0);
                            itemDay.set(Calendar.MINUTE, 0);
                            itemDay.set(Calendar.SECOND, 0);
                            itemDay.set(Calendar.MILLISECOND, 0);

                            Date todayFrom = itemDay.getTime();

                            itemDay.set(Calendar.HOUR_OF_DAY, 23);
                            itemDay.set(Calendar.MINUTE, 59);
                            itemDay.set(Calendar.SECOND, 59);
                            itemDay.set(Calendar.MILLISECOND, 999);

                            Date todayTo = itemDay.getTime();

                            // refresh log-report every minute
                            XxlJobLogReport xxlJobLogReport = new XxlJobLogReport();
                            xxlJobLogReport.setTriggerDay(todayFrom);
                            xxlJobLogReport.setRunningCount(0);
                            xxlJobLogReport.setSucCount(0);
                            xxlJobLogReport.setFailCount(0);

                            Map<String, Object> triggerCountMap = xxlJobLogDao.findLogReport(todayFrom, todayTo);
                            if (triggerCountMap != null && triggerCountMap.size() > 0) {
                                int triggerDayCount = triggerCountMap.containsKey("triggerDayCount") ? Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCount"))) : 0;
                                int triggerDayCountRunning = triggerCountMap.containsKey("triggerDayCountRunning") ? Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCountRunning"))) : 0;
                                int triggerDayCountSuc = triggerCountMap.containsKey("triggerDayCountSuc") ? Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCountSuc"))) : 0;
                                int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;

                                xxlJobLogReport.setRunningCount(triggerDayCountRunning);
                                xxlJobLogReport.setSucCount(triggerDayCountSuc);
                                xxlJobLogReport.setFailCount(triggerDayCountFail);
                            }

                            // do refresh
                            int ret = xxlJobLogReportDao.update(xxlJobLogReport);
                            if (ret < 1) {
                                xxlJobLogReportDao.save(xxlJobLogReport);
                            }
                        }

                    } catch (Exception e) {
                        if (!toStop) {
                            log.error(">>>>>>>>>>> xxl-job, job log report thread error:{}", e);
                        }
                    }

                    // 2、log-clean: switch open & once each day
                    if (xxlJobAdminConfig.getLogretentiondays() > 0
                            && System.currentTimeMillis() - lastCleanLogTime > 24 * 60 * 60 * 1000) {

                        // expire-time
                        Calendar expiredDay = Calendar.getInstance();
                        expiredDay.add(Calendar.DAY_OF_MONTH, -1 * xxlJobAdminConfig.getLogretentiondays());
                        expiredDay.set(Calendar.HOUR_OF_DAY, 0);
                        expiredDay.set(Calendar.MINUTE, 0);
                        expiredDay.set(Calendar.SECOND, 0);
                        expiredDay.set(Calendar.MILLISECOND, 0);
                        Date clearBeforeTime = expiredDay.getTime();

                        // clean expired log
                        List<Long> logIds = null;
                        do {
                            logIds = xxlJobLogDao.findClearLogIds(0, 0, clearBeforeTime, 0, 1000);
                            if (logIds != null && logIds.size() > 0) {
                                xxlJobLogDao.clearLog(logIds);
                            }
                        } while (logIds != null && logIds.size() > 0);

                        // update clean time
                        lastCleanLogTime = System.currentTimeMillis();
                    }

                    try {
                        TimeUnit.MINUTES.sleep(1);
                    } catch (Exception e) {
                        if (!toStop) {
                            log.error(e.getMessage(), e);
                        }
                    }

                }

                log.info(">>>>>>>>>>> xxl-job, job log report thread stop");

            }
        });
        logrThread.setDaemon(true);
        logrThread.setName("xxl-job, admin JobLogReportHelper");
        logrThread.start();
    }

    @Override
    public void destroy() throws Exception {
        toStop = true;
        // interrupt and wait
        logrThread.interrupt();
        try {
            logrThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

}
