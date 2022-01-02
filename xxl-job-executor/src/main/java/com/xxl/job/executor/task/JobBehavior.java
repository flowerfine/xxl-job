package com.xxl.job.executor.task;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import cn.sliew.milky.common.exception.ThrowableTraceFormater;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

import java.io.Serializable;
import java.time.Duration;
import java.util.Date;

/**
 * fixme job context
 */
public class JobBehavior extends AbstractBehavior<JobBehavior.Command> {

    private final int jobId;
    private final IJobHandler jobHandler;
    private ActorRef<JobCallbackBehavior.Command> callback;

    private volatile long logId;

    public JobBehavior(ActorContext<Command> context, int jobId, IJobHandler jobHandler) {
        super(context);
        this.jobId = jobId;
        this.jobHandler = jobHandler;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InitCommand.class, this::onInit)
                .onMessage(DestroyCommand.class, this::onDestroy)
                .onMessage(TriggerCommand.class, this::onTrigger)
                .onMessage(TimeoutCommand.class, this::onTimeout)
                .onMessage(KillCommand.class, this::onKill)
                .onSignal(PostStop.class, this::postStop)
                .build();
    }

    private Behavior<Command> onInit(InitCommand command) {
        try {
            jobHandler.init();
            command.replyTo.tell(StatusReply.success(new Object()));
        } catch (Exception e) {
            command.replyTo.tell(StatusReply.error(e));
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    private Behavior<Command> onDestroy(DestroyCommand command) {
        try {
            jobHandler.destroy();
            getContext().getLog().info(">>>>>>>>>>> xxl-job JobActor stoped, actorPath:{}", getContext().getSelf().path());
            command.replyTo.tell(StatusReply.success(new Object()));
        } catch (Exception e) {
            command.replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.stopped();
    }

    private Behavior<Command> onTrigger(TriggerCommand command) {
        TriggerParam triggerParam = command.param;
        this.logId = triggerParam.getLogId();
//        log filename, like "logPath/yyyy-MM-dd/9999.log"
        try {
            String logFileName = XxlJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
            XxlJobContext xxlJobContext = new XxlJobContext(
                    triggerParam.getJobId(),
                    triggerParam.getExecutorParams(),
                    logFileName,
                    triggerParam.getBroadcastIndex(),
                    triggerParam.getBroadcastTotal());
            XxlJobContext.setXxlJobContext(xxlJobContext);

//            execute
            XxlJobHelper.log("<br>----------- xxl-job job execute start -----------<br>----------- Param:"
                    + xxlJobContext.getJobParam());
            if (triggerParam.getExecutorTimeout() > 0) {
                getContext().setReceiveTimeout(Duration.ofSeconds(triggerParam.getExecutorTimeout()), new TimeoutCommand(triggerParam));
            }
            jobHandler.execute();
            getContext().cancelReceiveTimeout();

            // valid execute handle data
            if (XxlJobContext.getXxlJobContext().getHandleCode() <= 0) {
                XxlJobHelper.handleFail("job handle result lost.");
            } else {
                String tempHandleMsg = XxlJobContext.getXxlJobContext().getHandleMsg();
                tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000)
                        ? tempHandleMsg.substring(0, 50000).concat("...")
                        : tempHandleMsg;
                XxlJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);
            }
            XxlJobHelper.log("<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode="
                    + XxlJobContext.getXxlJobContext().getHandleCode()
                    + ", handleMsg = "
                    + XxlJobContext.getXxlJobContext().getHandleMsg()
            );
        } catch (Exception e) {
            // handle result
            String errorMsg = ThrowableTraceFormater.readStackTrace(e);
            XxlJobHelper.handleFail(errorMsg);
            XxlJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- xxl-job job execute end(error) -----------");
        } finally {
            callback.tell(new JobCallbackBehavior.CallbackCommand(triggerParam.getLogId(),
                    triggerParam.getLogDateTime(),
                    XxlJobContext.getXxlJobContext().getHandleCode(),
                    XxlJobContext.getXxlJobContext().getHandleMsg()));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onTimeout(TimeoutCommand command) {
        XxlJobHelper.log("<br>----------- xxl-job job execute timeout");
        // handle result
        XxlJobHelper.handleTimeout("job execute timeout ");


        String tempHandleMsg = XxlJobContext.getXxlJobContext().getHandleMsg();
        tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000)
                ? tempHandleMsg.substring(0, 50000).concat("...")
                : tempHandleMsg;
        XxlJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);

        XxlJobHelper.log("<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode="
                + XxlJobContext.getXxlJobContext().getHandleCode()
                + ", handleMsg = "
                + XxlJobContext.getXxlJobContext().getHandleMsg()
        );
        callback.tell(new JobCallbackBehavior.CallbackCommand(command.param.getLogId(),
                command.param.getLogDateTime(),
                XxlJobContext.getXxlJobContext().getHandleCode(),
                XxlJobContext.getXxlJobContext().getHandleMsg()));
        return Behaviors.same();
    }

    private Behavior<Command> onKill(KillCommand command) {
        if (command.logId != this.logId) {
            command.replyTo.tell(StatusReply.success(new ReturnT(ReturnT.SUCCESS_CODE, "job execution already finished!")));
            return Behaviors.same();
        }

        try {
            this.logId = -1;
            ReturnT<String> returnT = jobHandler.kill();
            callback.tell(new JobCallbackBehavior.CallbackCommand(command.logId,
                    command.logDateTime,
                    XxlJobContext.HANDLE_COCE_FAIL,
                    command.reason + " [job not executed, in the job queue, killed.]"));
            command.replyTo.tell(StatusReply.success(returnT));
        } catch (Exception e) {
            command.replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.stopped();
    }

    private Behavior<Command> postStop(PostStop signal) {
        getContext().getLog().info("job actor stop successfully! jobId: {}", jobId);
        return this;
    }

    public interface Command extends Serializable {

    }

    public static class InitCommand implements Command {
        private final ActorRef<StatusReply<Object>> replyTo;

        public InitCommand(ActorRef<StatusReply<Object>> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class DestroyCommand implements Command {
        private final ActorRef<StatusReply<Object>> replyTo;

        public DestroyCommand(ActorRef<StatusReply<Object>> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class TriggerCommand implements Command {
        private final TriggerParam param;
        private final ActorRef<StatusReply<ReturnT<String>>> replyTo;

        public TriggerCommand(TriggerParam param, ActorRef<StatusReply<ReturnT<String>>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }

    public static class KillCommand implements Command {
        private final long logId;
        private final long logDateTime;
        private final String reason;
        private final ActorRef<StatusReply<ReturnT<String>>> replyTo;

        public KillCommand(long logId, long logDateTime, String reason, ActorRef<StatusReply<ReturnT<String>>> replyTo) {
            this.logId = logId;
            this.logDateTime = logDateTime;
            this.reason = reason;
            this.replyTo = replyTo;
        }
    }

    public static class TimeoutCommand implements Command {
        private final TriggerParam param;

        public TimeoutCommand(TriggerParam param) {
            this.param = param;
        }
    }
}
