package com.xxl.job.core.remote.impl;

import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.impl.GlueJobHandler;
import com.xxl.job.core.handler.impl.ScriptJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.remote.ExecutorService;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class ExecutorServiceImpl implements ExecutorService {

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        JobThread jobThread = XxlJobExecutor.loadJobThread(idleBeatParam.getJobId());
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            return new ReturnT(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.");
        }

        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        if (glueTypeEnum == null) {
            return new ReturnT(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }

        JobThread jobThread = XxlJobExecutor.loadJobThread(triggerParam.getJobId());
        IJobHandler jobHandler;
        try {
            if (jobThread == null) {
                jobHandler = newJobHandler(triggerParam);
            } else {
                jobHandler = getJobHandler(jobThread, triggerParam);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ReturnT(ReturnT.FAIL_CODE, e.getMessage());
        }

        if (jobThread == null) {
            jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler);
            return jobThread.pushTriggerQueue(triggerParam);
        }

        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
        if (jobThread.isRunningOrHasQueue()) {
            switch (blockStrategy) {
                case DISCARD_LATER:
                    return new ReturnT(ReturnT.FAIL_CODE, "block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
                case COVER_EARLY:
                    XxlJobExecutor.removeJobThread(triggerParam.getJobId(), "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle());
                    jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler);
                    break;
                default:
            }
        }
        return jobThread.pushTriggerQueue(triggerParam);
    }

    private IJobHandler newJobHandler(TriggerParam triggerParam) throws Exception {
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        switch (glueTypeEnum) {
            case BEAN:
                return XxlJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());
            case GLUE_GROOVY:
                IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
                return new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
            case GLUE_SHELL:
            case GLUE_PYTHON:
            case GLUE_PHP:
            case GLUE_NODEJS:
            case GLUE_POWERSHELL:
                return new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
            default:
                throw new IllegalStateException("glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }
    }

    private IJobHandler getJobHandler(JobThread jobThread, TriggerParam triggerParam) throws Exception {
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        switch (glueTypeEnum) {
            case BEAN:
                return getBeanJobHandler(jobThread, triggerParam);
            case GLUE_GROOVY:
                return getGroovyJobHandler(jobThread, triggerParam);
            case GLUE_SHELL:
            case GLUE_PYTHON:
            case GLUE_PHP:
            case GLUE_NODEJS:
            case GLUE_POWERSHELL:
                return getScriptJobHandler(jobThread, triggerParam);
            default:
                throw new IllegalStateException("glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }
    }

    private IJobHandler getBeanJobHandler(JobThread jobThread, TriggerParam triggerParam) {
        IJobHandler oldJobHandler = jobThread.getHandler();
        IJobHandler newJobHandler = XxlJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());
        if (oldJobHandler != newJobHandler) {
            XxlJobExecutor.removeJobThread(triggerParam.getJobId(), "change jobhandler or glue type, and terminate the old job thread.");
            return newJobHandler;
        }
        return oldJobHandler;
    }

    private IJobHandler getGroovyJobHandler(JobThread jobThread, TriggerParam triggerParam) throws Exception {
        GlueJobHandler jobHandler = (GlueJobHandler) jobThread.getHandler();
        if (jobHandler.getGlueUpdatetime() != triggerParam.getGlueUpdatetime()) {
            XxlJobExecutor.removeJobThread(triggerParam.getJobId(), "change job source or glue type, and terminate the old job thread.");
            IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
            return new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
        }
        return jobHandler;
    }

    private IJobHandler getScriptJobHandler(JobThread jobThread, TriggerParam triggerParam) {
        ScriptJobHandler jobHandler = (ScriptJobHandler) jobThread.getHandler();
        if (jobHandler.getGlueUpdatetime() != triggerParam.getGlueUpdatetime()) {
            XxlJobExecutor.removeJobThread(triggerParam.getJobId(), "change job source or glue type, and terminate the old job thread.");
            return new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
        }
        return jobHandler;
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        JobThread jobThread = XxlJobExecutor.loadJobThread(killParam.getJobId());
        if (jobThread != null) {
            XxlJobExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
            return ReturnT.SUCCESS;
        }

        return new ReturnT(com.xxl.job.core.biz.model.ReturnT.SUCCESS_CODE, "job thread already killed.");
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        String logFileName = XxlJobFileAppender.makeLogFileName(new Date(logParam.getLogDateTim()), logParam.getLogId());
        LogResult logResult = XxlJobFileAppender.readLog(logFileName, logParam.getFromLineNum());
        return new ReturnT(logResult);
    }
}
