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
import java.time.Duration;

/**
 * fixme job context
 */
public class JobBehavior extends AbstractBehavior<JobBehavior.Command> {

    private final TimeoutCommand TIMEOUT = new TimeoutCommand();

    private ActorRef<JobHandlerProxy.Command> jobHandler;
    private ActorRef<JobCallbackBehavior.Command> callback;

    private volatile long logId;
    private volatile State state;

    public JobBehavior(ActorContext<Command> context, long jobId, IJobHandler jobHandler) {
        super(context);
        this.jobHandler = context.spawn(Behaviors.setup(ctx -> new JobHandlerProxy(ctx, jobId, jobHandler)), String.valueOf(jobId));
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
        TriggerParam param = command.param;
        Duration timeout = Duration.ofDays(365L);
        if (param.getExecutorTimeout() > 0) {
            timeout = Duration.ofSeconds(param.getExecutorTimeout());
        }
        getContext().askWithStatus(
                ReturnT.class,
                jobHandler,
                timeout,
                replyTo -> new JobHandlerProxy.TriggerCommand(param, replyTo),
                TriggerReturnTWrapper::new);
        return newReceiveBuilder()
                .onMessage(TimeoutCommand.class, this::onTimeout)
                .onMessage(KillCommand.class, this::onKill)
                .onMessage(StateCommand.class, this::onState)
                .build();
    }

    private Behavior<Command> onTimeout(TimeoutCommand command) {
        this.state = State.TIMEOUT;
        return newReceiveBuilder()
                .onMessage(TriggerCommand.class, this::onTrigger)
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

    public static class TriggerReturnTWrapper implements Command {
        private final ReturnT returnT;
        private final Throwable throwable;

        public TriggerReturnTWrapper(ReturnT returnT, Throwable throwable) {
            this.returnT = returnT;
            this.throwable = throwable;
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

    }
}
