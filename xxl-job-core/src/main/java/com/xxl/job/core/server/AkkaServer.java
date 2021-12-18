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
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;
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
                .onMessage(IdleBeatCommand.class, this::onIdleBeat)
                .onMessage(RunCommand.class, this::onRun)
                .onMessage(KillCommand.class, this::onKill)
                .onMessage(LogCommand.class, this::onLog)
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

    private Behavior<Command> onIdleBeat(IdleBeatCommand command) {
        ActorRef<StatusReply<ReturnT>> replyTo = command.replyTo;
        try {
            ReturnT<String> returnT = executorService.idleBeat(command.param);
            replyTo.tell(StatusReply.success(returnT));
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onRun(RunCommand command) {
        ActorRef<StatusReply<ReturnT>> replyTo = command.replyTo;
        try {
            ReturnT<String> returnT = executorService.run(command.param);
            replyTo.tell(StatusReply.success(returnT));
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onKill(KillCommand command) {
        ActorRef<StatusReply<ReturnT>> replyTo = command.replyTo;
        try {
            ReturnT<String> returnT = executorService.kill(command.param);
            replyTo.tell(StatusReply.success(returnT));
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onLog(LogCommand command) {
        ActorRef<StatusReply<ReturnT>> replyTo = command.replyTo;
        try {
            ReturnT<LogResult> returnT = executorService.log(command.param);
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

    public static class IdleBeatCommand implements Command {
        private IdleBeatParam param;
        private ActorRef<StatusReply<ReturnT>> replyTo;

        public IdleBeatCommand(IdleBeatParam param, ActorRef<StatusReply<ReturnT>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }

    public static class RunCommand implements Command {
        private TriggerParam param;
        private ActorRef<StatusReply<ReturnT>> replyTo;

        public RunCommand(TriggerParam param, ActorRef<StatusReply<ReturnT>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }

    public static class KillCommand implements Command {
        private KillParam param;
        private ActorRef<StatusReply<ReturnT>> replyTo;

        public KillCommand(KillParam param, ActorRef<StatusReply<ReturnT>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }

    public static class LogCommand implements Command {
        private LogParam param;
        private ActorRef<StatusReply<ReturnT>> replyTo;

        public LogCommand(LogParam param, ActorRef<StatusReply<ReturnT>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }


}
