package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.complete.XxlJobCompleter;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.util.DateUtil;
import com.xxl.job.dao.model.XxlJobLog;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.HandleCallbackParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * job lose-monitor instance
 */
@Slf4j
@Component
public class JobCompleteHelper implements InitializingBean, DisposableBean {

    private ThreadPoolExecutor callbackThreadPool = null;
    private Thread monitorThread;
    private volatile boolean toStop = false;

    @Autowired
    private XxlJobCompleter xxlJobCompleter;

    @Override
    public void afterPropertiesSet() throws Exception {

        // for callback
        callbackThreadPool = new ThreadPoolExecutor(
                2,
                20,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(3000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                        log.warn(">>>>>>>>>>> xxl-job, callback too fast, match threadpool rejected handler(run now).");
                    }
                });


        // for monitor
        monitorThread = new Thread(new Runnable() {

            @Override
            public void run() {

                // wait for JobTriggerPoolHelper-init
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }

                // monitor
                while (!toStop) {
                    try {
                        // 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
                        Date losedTime = DateUtil.addMinutes(new Date(), -10);
                        List<Long> losedJobIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findLostJobIds(losedTime);

                        if (losedJobIds != null && losedJobIds.size() > 0) {
                            for (Long logId : losedJobIds) {

                                XxlJobLog jobLog = new XxlJobLog();
                                jobLog.setId(logId);

                                jobLog.setHandleTime(new Date());
                                jobLog.setHandleCode(ReturnT.FAIL_CODE);
                                jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail"));

                                xxlJobCompleter.updateHandleInfoAndFinish(jobLog);
                            }

                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            log.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(60);
                    } catch (Exception e) {
                        if (!toStop) {
                            log.error(e.getMessage(), e);
                        }
                    }

                }

                log.info(">>>>>>>>>>> xxl-job, JobLosedMonitorHelper stop");

            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobLosedMonitorHelper");
        monitorThread.start();
    }

    @Override
    public void destroy() throws Exception {
        toStop = true;

        // stop registryOrRemoveThreadPool
        callbackThreadPool.shutdownNow();

        // stop monitorThread (interrupt and wait)
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {

        callbackThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                    ReturnT<String> callbackResult = callback(handleCallbackParam);
                    log.debug(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                            (callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"), handleCallbackParam, callbackResult);
                }
            }
        });

        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
        // valid log item
        XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(handleCallbackParam.getLogId());
        if (log == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log item not found.");
        }
        if (log.getHandleCode() > 0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log repeate callback.");     // avoid repeat callback, trigger child job etc
        }

        // handle msg
        StringBuffer handleMsg = new StringBuffer();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getHandleMsg() != null) {
            handleMsg.append(handleCallbackParam.getHandleMsg());
        }

        // success, save log
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        xxlJobCompleter.updateHandleInfoAndFinish(log);

        return ReturnT.SUCCESS;
    }


}
