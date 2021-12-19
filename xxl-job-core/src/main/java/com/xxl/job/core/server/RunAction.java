package com.xxl.job.core.server;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.impl.GlueJobHandler;
import com.xxl.job.core.handler.impl.ScriptJobHandler;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

public class RunAction extends AbstractBehavior<AkkaServer.Command> {

    public RunAction(ActorContext<AkkaServer.Command> context) {
        super(context);
    }

    @Override
    public Receive<AkkaServer.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AkkaServer.RunCommand.class, this::onRun)
                .build();
    }

    private Behavior<AkkaServer.Command> onRun(AkkaServer.RunCommand command) {
        ActorRef<StatusReply<ReturnT<String>>> replyTo = command.getReplyTo();
        TriggerParam triggerParam = command.getParam();
        try {
            GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
            if (glueTypeEnum == null) {
                replyTo.tell(StatusReply.success(new ReturnT(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.")));
                return Behaviors.same();
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
                getContext().getLog().error(e.getMessage(), e);
                replyTo.tell(StatusReply.error(e));
                return Behaviors.same();
            }

            if (jobThread == null) {
                jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler);
                replyTo.tell(StatusReply.success(jobThread.pushTriggerQueue(triggerParam)));
                return Behaviors.same();
            }

            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
            if (jobThread.isRunningOrHasQueue()) {
                switch (blockStrategy) {
                    case DISCARD_LATER:
                        replyTo.tell(StatusReply.success(new ReturnT(ReturnT.FAIL_CODE, "block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle())));
                        return Behaviors.same();
                    case COVER_EARLY:
                        XxlJobExecutor.removeJobThread(triggerParam.getJobId(), "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle());
                        jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler);
                        break;
                    default:
                }
            }
            replyTo.tell(StatusReply.success(jobThread.pushTriggerQueue(triggerParam)));
            return Behaviors.same();
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
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
}
