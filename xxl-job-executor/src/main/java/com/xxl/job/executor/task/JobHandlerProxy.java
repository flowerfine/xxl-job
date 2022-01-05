package com.xxl.job.executor.task;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

import java.io.Serializable;

public class JobHandlerProxy extends AbstractBehavior<JobHandlerProxy.Command> {

    private final long jobId;
    private final IJobHandler jobHandler;

    private ActorRef<TriggerParam> jobHandlerActor;

    public JobHandlerProxy(ActorContext<Command> context, long jobId, IJobHandler jobHandler) {
        super(context);
        this.jobId = jobId;
        this.jobHandler = jobHandler;

        Behavior<TriggerParam> jobHanderBehavior = Behaviors.setup(ctx -> new JobHandler(ctx, jobHandler, context.getSelf()));
        Behavior<TriggerParam> suprevisedBehavior = Behaviors.supervise(jobHanderBehavior).onFailure(SupervisorStrategy.restart());
        this.jobHandlerActor = context.spawn(suprevisedBehavior, String.valueOf(jobId));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(InitCommand.INSTANCE, this::onInit)
                .build();
    }

    public Behavior<Command> onInit() {
        try {
            jobHandler.init();
        } catch (Exception e) {
            getContext().getLog().error(">>>>>>>>>>> xxl-job 初始化 jobHandler 异常! jobId: {}", jobId, e);
            return Behaviors.stopped();
        }
        return newReceiveBuilder()
                .onMessageEquals(DestroyCommand.INSTANCE, this::onDestroy)
                .onMessage(TriggerCommand.class, this::onTrigger)
                .build();
    }

    public Behavior<Command> onDestroy() {
        try {
            jobHandler.destroy();
        } catch (Exception e) {
            getContext().getLog().error(">>>>>>>>>>> xxl-job 销毁 jobHandler 异常! jobId: {}", jobId, e);
            return Behaviors.stopped();
        }
        return Behaviors.stopped();
    }

    public Behavior<Command> onTrigger(TriggerCommand command) {
        jobHandlerActor.tell(command.param);
        return newReceiveBuilder()
                .onMessageEquals(DestroyCommand.INSTANCE, this::onDestroy)
                .onMessageEquals(KillCommand.INSTANCE, this::onKill)
                .onMessage(TriggerReturnTWrapper.class, this::onTriggerReturnT)
                .build();
    }

    public Behavior<Command> onTriggerReturnT(TriggerReturnTWrapper wrapper) {
        if (wrapper.throwable != null) {

        }
        return newReceiveBuilder()
                .onMessageEquals(DestroyCommand.INSTANCE, this::onDestroy)
                .onMessage(TriggerCommand.class, this::onTrigger)
                .build();
    }

    public Behavior<Command> onKill() {
        try {
            jobHandler.kill();
            // 终止 第三个 actor
        } catch (Exception e) {
            getContext().getLog().error(">>>>>>>>>>> xxl-job kill jobHandler 异常! jobId: {}", jobId, e);
            return Behaviors.stopped();
        }
        return newReceiveBuilder()
                .onMessageEquals(DestroyCommand.INSTANCE, this::onDestroy)
                .build();
    }

    public interface Command extends Serializable {

    }

    public enum InitCommand implements Command {
        INSTANCE
    }

    public enum DestroyCommand implements Command {
        INSTANCE
    }

    public static class TriggerCommand implements Command {
        private final TriggerParam param;
        private final ActorRef<StatusReply<ReturnT>> replyTo;

        public TriggerCommand(TriggerParam param, ActorRef<StatusReply<ReturnT>> replyTo) {
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

    public enum KillCommand implements Command {
        INSTANCE
    }

}
