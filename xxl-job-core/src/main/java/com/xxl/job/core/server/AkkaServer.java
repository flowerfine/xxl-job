package com.xxl.job.core.server;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.pattern.StatusReply;
import com.xxl.job.core.remote.impl.ExecutorServiceImpl;
import com.xxl.job.remote.ExecutorService;
import com.xxl.job.remote.protocol.ReturnT;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AkkaServer extends AbstractBehavior<AkkaServer.Command> {

    private ExecutorService executorService;

    public AkkaServer(ActorContext<Command> context, String appname) {
        super(context);
        this.executorService = new ExecutorServiceImpl();
        ServiceKey<Command> serviceKey = ServiceKeyHelper.getServiceKey(Command.class, appname);
        context.getSystem().receptionist().tell(Receptionist.register(serviceKey, getContext().getSelf()));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(BeatCommand.class, this::onBeat)
                .build();
    }

    private Behavior<Command> onBeat(BeatCommand command) {
        ActorRef<StatusReply<ReturnT>> replyTo = command.replyTo;
        try {
            ReturnT<String> returnT = executorService.beat();
            replyTo.tell(StatusReply.success(returnT));
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }


    public interface Command {

    }

    public static class BeatCommand implements Command {

        private ActorRef<StatusReply<ReturnT>> replyTo;

        public BeatCommand(ActorRef<StatusReply<ReturnT>> replyTo) {
            this.replyTo = replyTo;
        }
    }


}
