package com.xxl.job.executor.task;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

import java.io.Serializable;

public class JobBehavior extends AbstractBehavior<JobBehavior.Command> {

    private ActorRef<JobHandlerProxy.Command> jobHandler;
    private ActorRef<JobCallbackBehavior.Command> callback;

    private volatile long logId;
    private volatile State state;

    public JobBehavior(ActorContext<Command> context, long jobId, IJobHandler jobHandler, ActorRef<JobCallbackBehavior.Command> jobCallbackActor) {
        super(context);
//        AdminBizClient client = new AdminBizClient("http://localhost:8081/xxl-job-admin/", null);
//        Behavior<JobCallbackBehavior.Command> jobCallbackBehavior = Behaviors.setup(ctx -> new JobCallbackBehavior(ctx, client));
//        Behavior<JobCallbackBehavior.Command> failure = Behaviors.supervise(jobCallbackBehavior).onFailure(SupervisorStrategy.restart());

        this.jobHandler = context.spawn(Behaviors.setup(ctx -> new JobHandlerProxy(ctx, jobId, jobHandler, jobCallbackActor)), String.valueOf(jobId));
        this.jobHandler.tell(JobHandlerProxy.InitCommand.INSTANCE);
        this.state = State.IDLE;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(TriggerCommand.class, this::onTrigger)
                .onMessage(StateCommand.class, this::onState)
                .build();
    }

    private Behavior<Command> onState(StateCommand command) {
        command.replyTo.tell(StatusReply.success(state));
        return Behaviors.same();
    }

    private Behavior<Command> onTrigger(TriggerCommand command) {
        this.state = State.TRIGGER;
        jobHandler.tell(new JobHandlerProxy.TriggerCommand(command.param));
        return newReceiveBuilder()
                .onMessage(KillCommand.class, this::onKill)
                .onMessage(StateCommand.class, this::onState)
                .build();
    }

    private Behavior<Command> onKill(KillCommand command) {
        jobHandler.tell(JobHandlerProxy.KillCommand.INSTANCE);

        return Behaviors.stopped();
    }

    private Behavior<Command> postStop(PostStop signal) {
        getContext().getLog().info("job actor stop successfully! actorPath: {}", getContext().getSelf().path());
        return this;
    }

    public interface Command extends Serializable {

    }

    public static class StateCommand implements Command {
        public final ActorRef<StatusReply<State>> replyTo;

        public StateCommand(ActorRef<StatusReply<State>> replyTo) {
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

}
