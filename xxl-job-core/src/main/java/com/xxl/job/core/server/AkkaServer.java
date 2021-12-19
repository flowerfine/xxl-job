package com.xxl.job.core.server;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.pattern.StatusReply;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AkkaServer extends AbstractBehavior<AkkaServer.Command> {

    private ActorRef<AkkaServer.Command> runActor;
    private ActorRef<AkkaServer.Command> logActor;

    public AkkaServer(ActorContext<Command> context, String appname) {
        super(context);
        this.runActor = context.spawn(Behaviors.setup(RunAction::new), "RunAction");
        Behavior<Command> logAction = Behaviors.setup(LogAction::new);
        this.logActor = context.spawn(Routers.pool(5, logAction), "LogAction-Pool");

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
        ActorRef<StatusReply<ReturnT<String>>> replyTo = command.replyTo;
        try {
            replyTo.tell(StatusReply.success(ReturnT.SUCCESS));
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onIdleBeat(IdleBeatCommand command) {
        ActorRef<StatusReply<ReturnT<String>>> replyTo = command.replyTo;
        try {
            JobThread jobThread = XxlJobExecutor.loadJobThread(command.param.getJobId());
            if (jobThread != null && jobThread.isRunningOrHasQueue()) {
                replyTo.tell(StatusReply.success(new ReturnT(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.")));
            } else {
                replyTo.tell(StatusReply.success(ReturnT.SUCCESS));
            }
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onRun(RunCommand command) {
        runActor.tell(command);
        return Behaviors.same();
    }

    private Behavior<Command> onKill(KillCommand command) {
        ActorRef<StatusReply<ReturnT<String>>> replyTo = command.replyTo;
        KillParam killParam = command.param;
        try {
            JobThread jobThread = XxlJobExecutor.loadJobThread(killParam.getJobId());
            if (jobThread != null) {
                XxlJobExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
                replyTo.tell(StatusReply.success(ReturnT.SUCCESS));
            } else {
                replyTo.tell(StatusReply.success(new ReturnT(ReturnT.SUCCESS_CODE, "job thread already killed.")));
            }
        } catch (Exception e) {
            replyTo.tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

    private Behavior<Command> onLog(LogCommand command) {
        logActor.tell(command);
        return Behaviors.same();
    }


    public interface Command {

    }

    public static class BeatCommand implements Command {

        private ActorRef<StatusReply<ReturnT<String>>> replyTo;

        public BeatCommand(ActorRef<StatusReply<ReturnT<String>>> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class IdleBeatCommand implements Command {
        private IdleBeatParam param;
        private ActorRef<StatusReply<ReturnT<String>>> replyTo;

        public IdleBeatCommand(IdleBeatParam param, ActorRef<StatusReply<ReturnT<String>>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class RunCommand implements Command {
        private TriggerParam param;
        private ActorRef<StatusReply<ReturnT<String>>> replyTo;

        public RunCommand(TriggerParam param, ActorRef<StatusReply<ReturnT<String>>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }

    public static class KillCommand implements Command {
        private KillParam param;
        private ActorRef<StatusReply<ReturnT<String>>> replyTo;

        public KillCommand(KillParam param, ActorRef<StatusReply<ReturnT<String>>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class LogCommand implements Command {
        private LogParam param;
        private ActorRef<StatusReply<ReturnT<LogResult>>> replyTo;

        public LogCommand(LogParam param, ActorRef<StatusReply<ReturnT<LogResult>>> replyTo) {
            this.param = param;
            this.replyTo = replyTo;
        }
    }


}
