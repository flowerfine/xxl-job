package com.xxl.job.rpc;

import akka.actor.ActorSelection;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.pattern.StatusReply;
import com.xxl.job.rpc.message.Message;
import com.xxl.job.rpc.message.RpcInvocationCommand;
import com.xxl.job.rpc.message.protocol.RpcRequest;
import com.xxl.job.rpc.message.protocol.RpcResponse;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class AkkaGatewayBehavior extends AbstractBehavior<Message> {

    private ActorRef<Message> remote;

    public AkkaGatewayBehavior(ActorContext<Message> context, String host, int port, String endpoint) throws ExecutionException, InterruptedException {
        super(context);
        String remotePath = ActorSelectionHelper.getRemotePath(host, port, endpoint);
        getContext().getLog().info("连接远端 actor! remotePath: {}", remotePath);
        ActorSelection actorSelection = getContext().classicActorContext().actorSelection(remotePath);
        CompletionStage<akka.actor.ActorRef> future = actorSelection.resolveOne(Duration.ofSeconds(3L));

        this.remote = Adapter.toTyped(future.toCompletableFuture().get());
    }

    /**
     * 发送 rpc 消息等。
     */
    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(RpcInvocationCommand.class, this::onRpc)
                .build();
    }

    private Behavior<Message> onRpc(RpcInvocationCommand invocation) {
        getContext().askWithStatus(RpcResponse.class,
                remote,
                Duration.ofSeconds(3L),
                replyTo -> new RpcRequest(
                        invocation.getClassName(),
                        invocation.getMethodName(),
                        invocation.getParameterTypes(),
                        invocation.getArgs(),
                        replyTo),
                (result, throwable) -> {
                    if (throwable != null) {
                        invocation.getReplyTo().tell(StatusReply.error(throwable));
                    } else {
                        invocation.getReplyTo().tell(StatusReply.success(result));
                    }
                    return null;
                });
        return Behaviors.same();
    }
}
