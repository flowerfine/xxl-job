package com.xxl.job.core.remote.client;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import com.xxl.job.core.server.AkkaServer;
import com.xxl.job.remote.ExecutorService;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ExecutorClient implements ExecutorService {

    private final ActorSystem actorSystem;
    private final ActorRef<AkkaServer.Command> executor;

    public ExecutorClient(ActorSystem actorSystem, ActorRef<AkkaServer.Command> executor) {
        this.actorSystem = actorSystem;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<ReturnT<String>> beat() {
        CompletionStage<ReturnT<String>> stage = AskPattern.askWithStatus(executor,
                replyTo -> new AkkaServer.BeatCommand(replyTo),
                Duration.ofSeconds(30L),
                actorSystem.scheduler()
        );
        return stage.toCompletableFuture();
    }

    @Override
    public CompletableFuture<ReturnT<String>> idleBeat(IdleBeatParam idleBeatParam) {
        CompletionStage<ReturnT<String>> stage = AskPattern.askWithStatus(executor,
                replyTo -> new AkkaServer.IdleBeatCommand(idleBeatParam, replyTo),
                Duration.ofSeconds(30L),
                actorSystem.scheduler()
        );
        return stage.toCompletableFuture();
    }

    @Override
    public CompletableFuture<ReturnT<String>> run(TriggerParam triggerParam) {
        CompletionStage<ReturnT<String>> stage = AskPattern.askWithStatus(executor,
                replyTo -> new AkkaServer.RunCommand(triggerParam, replyTo),
                Duration.ofSeconds(30L),
                actorSystem.scheduler()
        );
        return stage.toCompletableFuture();
    }

    @Override
    public CompletableFuture<ReturnT<String>> kill(KillParam killParam) {
        CompletionStage<ReturnT<String>> stage = AskPattern.askWithStatus(executor,
                replyTo -> new AkkaServer.KillCommand(killParam, replyTo),
                Duration.ofSeconds(30L),
                actorSystem.scheduler()
        );
        return stage.toCompletableFuture();
    }

    @Override
    public CompletableFuture<ReturnT<LogResult>> log(LogParam logParam) {
        CompletionStage<ReturnT<LogResult>> stage = AskPattern.askWithStatus(executor,
                replyTo -> new AkkaServer.LogCommand(logParam, replyTo),
                Duration.ofSeconds(30L),
                actorSystem.scheduler()
        );
        return stage.toCompletableFuture();
    }
}
