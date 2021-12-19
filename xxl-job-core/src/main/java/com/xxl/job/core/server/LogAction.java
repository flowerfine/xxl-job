package com.xxl.job.core.server;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.response.LogResult;

import java.util.Date;

public class LogAction extends AbstractBehavior<AkkaServer.Command> {

    public LogAction(ActorContext<AkkaServer.Command> context) {
        super(context);
    }

    @Override
    public Receive<AkkaServer.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AkkaServer.LogCommand.class, this::onLog)
                .build();
    }

    private Behavior<AkkaServer.Command> onLog(AkkaServer.LogCommand command) {
        ActorRef<StatusReply<ReturnT<LogResult>>> replyTo = command.getReplyTo();
        LogParam logParam = command.getParam();
        try {
            String logFileName = XxlJobFileAppender.makeLogFileName(new Date(logParam.getLogDateTim()), logParam.getLogId());
            LogResult logResult = XxlJobFileAppender.readLog(logFileName, logParam.getFromLineNum());
            replyTo.tell(StatusReply.success(new ReturnT(logResult)));
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }
}
