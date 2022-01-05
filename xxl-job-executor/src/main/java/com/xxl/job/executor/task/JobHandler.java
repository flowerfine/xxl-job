package com.xxl.job.executor.task;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

import java.io.Serializable;

public class JobHandler extends AbstractBehavior<JobHandler.Command> {

    private final IJobHandler jobHandler;

    public JobHandler(ActorContext<Command> context, IJobHandler jobHandler) {
        super(context);
        this.jobHandler = jobHandler;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(TriggerCommand.class, this::onTrigger)
                .build();
    }

    public Behavior<Command> onTrigger(TriggerCommand command) {
        try {
            jobHandler.execute();
            command.replyTo.tell(StatusReply.success(ReturnT.SUCCESS));
        } catch (Exception e) {
            getContext().getLog().error(">>>>>>>>>>> xxl-job 执行 jobHandler 异常! actorPath: {}", getContext().getSelf().path(), e);
            command.replyTo.tell(StatusReply.error(e));
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }

    public interface Command extends Serializable {

    }

    public static class TriggerCommand implements Command {
        private final TriggerParam param;
        private final ActorRef<StatusReply<ReturnT>> replyTo;

        public TriggerCommand(TriggerParam param, ActorRef<StatusReply<ReturnT>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }


}
