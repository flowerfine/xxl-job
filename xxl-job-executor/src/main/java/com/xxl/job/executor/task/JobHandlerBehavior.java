package com.xxl.job.executor.task;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
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
import com.xxl.job.remote.protocol.request.TriggerParam;

import java.io.Serializable;
import java.util.Date;

public class JobHandlerBehavior extends AbstractBehavior<JobHandlerBehavior.Command> {

    private final IJobHandler jobHandler;

    public JobHandlerBehavior(ActorContext<Command> context, IJobHandler jobHandler) {
        super(context);
        this.jobHandler = jobHandler;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ExecuteCommand.class, this::onExecute)
                .build();
    }

    private Behavior<Command> onExecute(ExecuteCommand command) {
        TriggerParam triggerParam = command.param;
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
            jobHandler.execute();
        } catch (Exception e) {
            // handle result
            String errorMsg = ThrowableTraceFormater.readStackTrace(e);
            XxlJobHelper.handleFail(errorMsg);
            XxlJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- xxl-job job execute end(error) -----------");
        }
        return Behaviors.same();
    }

    public interface Command extends Serializable {

    }

    public static class ExecuteCommand implements Command {
        private final TriggerParam param;
        private final ActorRef<StatusReply<JobBehavior.Command>> replyTo;

        public ExecuteCommand(TriggerParam param, ActorRef<StatusReply<JobBehavior.Command>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }
}
