package com.xxl.job.rpc;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Props;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import akka.pattern.StatusReply;
import com.xxl.job.rpc.message.Message;
import com.xxl.job.rpc.message.RpcInvocationCommand;
import com.xxl.job.rpc.message.protocol.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class AkkaInvocationHandler implements InvocationHandler {

    private final ActorSystem actorSystem;
    private final ActorRef<Message> gatewayActor;

    public AkkaInvocationHandler(ActorSystem actorSystem, String host, int port, String endpoint) throws ExecutionException, InterruptedException {
        this.actorSystem = actorSystem;
        CompletionStage<ActorRef<Message>> gatewayFuture =
                AskPattern.ask(
                        actorSystem,
                        replyTo -> new SpawnProtocol.Spawn(Behaviors.<Message>setup(ctx -> new AkkaGatewayBehavior(ctx, host, port, endpoint)), "AkkaGatewayBehavior", Props.empty(), replyTo),
                        Duration.ofSeconds(3),
                        actorSystem.scheduler());
        this.gatewayActor = gatewayFuture.toCompletableFuture().get();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.equals(Object.class)) {
            return method.invoke(proxy, args);
        }
        return invokeRpc(method, args);
    }

    private Object invokeRpc(Method method, Object[] args) throws Exception {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        CompletableFuture<RpcResponse> future = ask(className, methodName, parameterTypes, args);
        RpcResponse response = future.get();
        if (method.getReturnType().equals(Void.TYPE)) {
            return null;
        } else {
            return response.getResult();
        }
    }

    private CompletableFuture<RpcResponse> ask(String className, String methodName, Class<?>[] parameterTypes, Object[] args) {
        return AskPattern.askWithStatus(
                gatewayActor,
                (ActorRef<StatusReply<RpcResponse>> replyTo) -> new RpcInvocationCommand(className, methodName, parameterTypes, args, replyTo),
                Duration.ofSeconds(3L),
                actorSystem.scheduler()
        ).toCompletableFuture();
    }
}
