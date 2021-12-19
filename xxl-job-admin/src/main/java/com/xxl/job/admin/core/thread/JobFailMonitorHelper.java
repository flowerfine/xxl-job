package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.alarm.JobAlarmer;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.dao.XxlJobInfoDao;
import com.xxl.job.dao.XxlJobLogDao;
import com.xxl.job.dao.model.XxlJobInfo;
import com.xxl.job.dao.model.XxlJobLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * job monitor instance
 */
@Slf4j
@Component
public class JobFailMonitorHelper implements InitializingBean, DisposableBean {

    private Thread monitorThread;
    private volatile boolean toStop = false;

    @Autowired
    private XxlJobInfoDao xxlJobInfoDao;
    @Autowired
    private XxlJobLogDao xxlJobLogDao;
    @Autowired
    private JobTriggerPoolHelper jobTriggerPoolHelper;
    @Autowired
    private JobAlarmer jobAlarmer;

    @Override
    public void afterPropertiesSet() throws Exception {
        monitorThread = new Thread(new Runnable() {

            @Override
            public void run() {

                // monitor
                while (!toStop) {
                    try {

                        List<Long> failLogIds = xxlJobLogDao.findFailJobLogIds(1000);
                        if (failLogIds != null && !failLogIds.isEmpty()) {
                            for (long failLogId : failLogIds) {

                                // lock log
                                int lockRet = xxlJobLogDao.updateAlarmStatus(failLogId, 0, -1);
                                if (lockRet < 1) {
                                    continue;
                                }
                                XxlJobLog log = xxlJobLogDao.load(failLogId);
                                XxlJobInfo info = xxlJobInfoDao.loadById(log.getJobId());

                                // 1、fail retry monitor
                                if (log.getExecutorFailRetryCount() > 0) {
                                    jobTriggerPoolHelper.trigger(log.getJobId(), TriggerTypeEnum.RETRY, (log.getExecutorFailRetryCount() - 1), log.getExecutorShardingParam(), log.getExecutorParam(), null);
                                    String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_type_retry") + "<<<<<<<<<<< </span><br>";
                                    log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
                                    xxlJobLogDao.updateTriggerInfo(log);
                                }

                                // 2、fail alarm monitor
                                int newAlarmStatus = 0;        // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                                if (info != null && info.getAlarmEmail() != null && info.getAlarmEmail().trim().length() > 0) {
                                    boolean alarmResult = jobAlarmer.alarm(info, log);
                                    newAlarmStatus = alarmResult ? 2 : 3;
                                } else {
                                    newAlarmStatus = 1;
                                }

                                xxlJobLogDao.updateAlarmStatus(failLogId, -1, newAlarmStatus);
                            }
                        }

                    } catch (Exception e) {
                        if (!toStop) {
                            log.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (Exception e) {
                        if (!toStop) {
                            log.error(e.getMessage(), e);
                        }
                    }

                }

                log.info(">>>>>>>>>>> xxl-job, job fail monitor thread stop");

            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobFailMonitorHelper");
        monitorThread.start();
    }

    @Override
    public void destroy() throws Exception {
        toStop = true;
        // interrupt and wait
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

}
