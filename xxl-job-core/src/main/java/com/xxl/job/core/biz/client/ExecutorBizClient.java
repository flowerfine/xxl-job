package com.xxl.job.core.biz.client;

import akka.actor.ActorSelection;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.IdleBeatParam;
import com.xxl.job.core.biz.model.KillParam;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import com.xxl.job.remote.ActorSelectionHelper;

import java.util.HashMap;

public class ExecutorBizClient extends AbstractBehavior<ExecutorBizClient.Command> implements ExecutorBiz {

    private HashMap<String, String> executorRouterPathMap = new HashMap<>(8);
    private String accessToken;
    private int timeout = 3;

    private ActorSystem actorSystem;

    public ExecutorBizClient(ActorContext<Command> context, String executorHost, String accessToken) {
        super(context);
        executorRouterPathMap.put("beat", ActorSelectionHelper.getExecutorRouterPath(executorHost, "beat"));
        executorRouterPathMap.put("idleBeat", ActorSelectionHelper.getExecutorRouterPath(executorHost, "idleBeat"));
        executorRouterPathMap.put("run", ActorSelectionHelper.getExecutorRouterPath(executorHost, "run"));
        executorRouterPathMap.put("kill", ActorSelectionHelper.getExecutorRouterPath(executorHost, "kill"));
        executorRouterPathMap.put("log", ActorSelectionHelper.getExecutorRouterPath(executorHost, "log"));
        this.accessToken = accessToken;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(BeatCommand.class, unused -> onBeat())
                .build();
    }

    private Behavior<Command> onBeat() {
        String beatPath = executorRouterPathMap.get("beat");
        ActorSelection actorSelection = getContext().classicActorContext().actorSelection(beatPath);
    }

    @Override
    public ReturnT<String> beat() {
        String beatPath = executorRouterPathMap.get("beat");

        ActorRef actorRef = this.actorSystem;
        return XxlJobRemotingUtil.postBody(addressUrl + "beat", accessToken, timeout, "", String.class);
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "idleBeat", accessToken, timeout, idleBeatParam, String.class);
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "run", accessToken, timeout, triggerParam, String.class);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "kill", accessToken, timeout, killParam, String.class);
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "log", accessToken, timeout, logParam, LogResult.class);
    }

    interface Command {

    }

    public static class BeatCommand implements Command {

    }
}
