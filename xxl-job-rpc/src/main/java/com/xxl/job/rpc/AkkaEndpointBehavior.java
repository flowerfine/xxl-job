package com.xxl.job.rpc;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import com.xxl.job.rpc.message.Message;
import com.xxl.job.rpc.message.protocol.RpcInvocation;
import com.xxl.job.rpc.message.protocol.RpcRequest;
import com.xxl.job.rpc.message.protocol.RpcResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static com.xxl.job.rpc.util.CodecUtil.deserialize;
import static com.xxl.job.rpc.util.CodecUtil.serialize;

public class AkkaEndpointBehavior<C> extends AbstractBehavior<Message> {

    private final C endpoint;
    private final Class<?> clazz;

    public AkkaEndpointBehavior(ActorContext<Message> context, C endpoint) {
        super(context);
        this.endpoint = endpoint;
        this.clazz = endpoint.getClass();
    }

    @Override
    public Receive<Message> createReceive() {
        return newReceiveBuilder()
                .onMessage(RpcRequest.class, this::onRpc)
                .build();
    }

    public Behavior<Message> onRpc(RpcRequest request) {
        RpcInvocation invocation;
        try {
            invocation = deserialize(request.getMsg());
            getContext().getLog().info("收到 rpc 请求! methodName: {}, parameterTypes: {}, args: {}",
                    invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArgs());
        } catch (Exception e) {
            getContext().getLog().error("反序列化 rpc 请求失败!", e);
            request.getReplyTo().tell(StatusReply.error(e));
            return Behaviors.same();
        }
        try {
            Method method = clazz.getDeclaredMethod(invocation.getMethodName(), invocation.getParameterTypes());
            method.setAccessible(true);
            Object result = method.invoke(endpoint, invocation.getArgs());
            sendAsyncResponse(request.getReplyTo(), result);
        } catch (Exception e) {
            getContext().getLog().error("处理 rpc 请求失败! methodName: {}, parameterTypes: {}, args: {}",
                    invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArgs(), e);
            request.getReplyTo().tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

    private void sendAsyncResponse(ActorRef<StatusReply<RpcResponse>> replyTo, Object result) {
        if (result instanceof CompletableFuture) {
            CompletableFuture<Object> future = (CompletableFuture) result;
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    replyTo.tell(StatusReply.error(throwable));
                } else {
                    try {
                        replyTo.tell(StatusReply.success(new RpcResponse(serialize(response), null)));
                    } catch (IOException e) {
                        replyTo.tell(StatusReply.error(e));
                    }
                }
            });
        } else {
            try {
                replyTo.tell(StatusReply.success(new RpcResponse(serialize(result), null)));
            } catch (IOException e) {
                replyTo.tell(StatusReply.error(e));
            }
        }
    }

}
