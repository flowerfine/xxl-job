package com.xxl.job.executor.task;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

public class JobHandler extends AbstractBehavior<TriggerParam> {

    private final IJobHandler jobHandler;
    private final ActorRef<JobHandlerProxy.Command> proxy;

    public JobHandler(ActorContext<TriggerParam> context, IJobHandler jobHandler, ActorRef<JobHandlerProxy.Command> proxy) {
        super(context);
        this.jobHandler = jobHandler;
        this.proxy = proxy;
    }

    @Override
    public Receive<TriggerParam> createReceive() {
        return newReceiveBuilder()
                .onMessage(TriggerParam.class, this::onTrigger)
                .build();
    }

    public Behavior<TriggerParam> onTrigger(TriggerParam param) {
        try {
            jobHandler.execute();
            proxy.tell(new JobHandlerProxy.TriggerReturnTWrapper(ReturnT.SUCCESS, null));
        } catch (Exception e) {
            getContext().getLog().error(">>>>>>>>>>> xxl-job 执行 jobHandler 异常! actorPath: {}", getContext().getSelf().path(), e);
            proxy.tell(new JobHandlerProxy.TriggerReturnTWrapper(null, e));
            return Behaviors.stopped();
        }
        return Behaviors.same();
    }
}
