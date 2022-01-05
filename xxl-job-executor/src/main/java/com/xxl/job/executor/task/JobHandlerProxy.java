package com.xxl.job.executor.task;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.util.JacksonUtil;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

import java.io.Serializable;
import java.time.Duration;

public class JobHandlerProxy extends AbstractBehavior<JobHandlerProxy.Command> {

    private final long jobId;
    private final IJobHandler jobHandler;
    private final ActorRef<JobCallbackBehavior.Command> jobCallbackActor;

    private ActorRef<JobHandler.Command> jobHandlerActor;


    private volatile State state;

    public JobHandlerProxy(ActorContext<Command> context, long jobId, IJobHandler jobHandler, ActorRef<JobCallbackBehavior.Command> jobCallbackActor) {
        super(context);
        this.jobId = jobId;
        this.jobHandler = jobHandler;
        this.jobCallbackActor = jobCallbackActor;

        Behavior<JobHandler.Command> jobHanderBehavior = Behaviors.setup(ctx -> new JobHandler(ctx, jobHandler));
        Behavior<JobHandler.Command> suprevisedBehavior = Behaviors.supervise(jobHanderBehavior).onFailure(SupervisorStrategy.restart());
        this.jobHandlerActor = context.spawn(suprevisedBehavior, String.valueOf(jobId));
        this.state = State.IDLE;
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
        TriggerParam param = command.param;
        Duration timeout = Duration.ofDays(1000L);
        if (param.getExecutorTimeout() > 0) {
            timeout = Duration.ofSeconds(param.getExecutorTimeout());
        }

        getContext().askWithStatus(ReturnT.class, jobHandlerActor, timeout,
                replyTo -> new JobHandler.TriggerCommand(param, replyTo), (returnT, throwable) -> new TriggerReturnTWrapper(param, returnT, throwable));

        return newReceiveBuilder()
                .onMessageEquals(DestroyCommand.INSTANCE, this::onDestroy)
                .onMessageEquals(KillCommand.INSTANCE, this::onKill)
                .onMessage(TriggerReturnTWrapper.class, this::onTriggerReturnT)
                .build();
    }

    public Behavior<Command> onTriggerReturnT(TriggerReturnTWrapper wrapper) {
        if (wrapper.throwable != null) {
            getContext().getLog().error("处理任务失败！", wrapper.throwable);
        } else {
            getContext().getLog().info("处理任务成功！returnT: {}", JacksonUtil.toJsonString(wrapper.returnT));
            jobCallbackActor.tell(
                    new JobCallbackBehavior.CallbackCommand(
                            wrapper.param.getLogId(),
                            wrapper.param.getLogDateTime(),
                            wrapper.returnT.getCode(),
                            wrapper.returnT.getMsg()));
        }
        return newReceiveBuilder()
                .onMessageEquals(DestroyCommand.INSTANCE, this::onDestroy)
                .onMessage(TriggerCommand.class, this::onTrigger)
                .build();
    }

    public Behavior<Command> onKill() {
        try {
            jobHandler.kill();
            getContext().stop(jobHandlerActor);
            this.jobHandlerActor = null;
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

        public TriggerCommand(TriggerParam param) {
            this.param = param;
        }
    }

    public static class TriggerReturnTWrapper implements Command {
        private final TriggerParam param;
        private final ReturnT returnT;
        private final Throwable throwable;

        public TriggerReturnTWrapper(TriggerParam param, ReturnT returnT, Throwable throwable) {
            this.param = param;
            this.returnT = returnT;
            this.throwable = throwable;
        }
    }

    public enum KillCommand implements Command {
        INSTANCE
    }

}
