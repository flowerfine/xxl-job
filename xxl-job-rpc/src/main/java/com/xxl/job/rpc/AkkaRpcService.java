package com.xxl.job.rpc;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Props;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import com.xxl.job.rpc.message.Message;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AkkaRpcService implements RpcService {

    private final ActorSystem actorSystem;

    public AkkaRpcService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @Override
    public <C> C connect(String host, int port, String endpoint, Class<C> gateway) {
        AkkaInvocationHandler invocationHandler = null;
        try {
            invocationHandler = new AkkaInvocationHandler(actorSystem, host, port, endpoint);
        } catch (Exception e) {
            throw new RuntimeException("连接 remote 服务异常!", e);
        }
        return (C) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{gateway}, invocationHandler);
    }

    @Override
    public <C extends RpcServer> CompletableFuture<C> start(String endpoint, Object server) {
        CompletionStage<ActorRef<Message>> endpointFuture =
                AskPattern.ask(
                        actorSystem,
                        replyTo -> new SpawnProtocol.Spawn(Behaviors.<Message>setup(ctx -> new AkkaEndpointBehavior(ctx, server)), endpoint, Props.empty(), replyTo),
                        Duration.ofSeconds(3),
                        actorSystem.scheduler());
        return (CompletableFuture<C>) endpointFuture.thenApply(actorRef -> new AkkaEndpoint(actorRef)).toCompletableFuture();
    }
}
