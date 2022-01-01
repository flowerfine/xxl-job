package com.xxl.job.rpc;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.StatusReply;
import com.xxl.job.rpc.message.Message;
import com.xxl.job.rpc.message.protocol.RpcRequest;
import com.xxl.job.rpc.message.protocol.RpcResponse;

import java.lang.reflect.Method;

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
        getContext().getLog().info("收到 rpc 请求! className: {}, methodName: {}, parameterTypes: {}, args: {}",
                request.getClassName(), request.getMethodName(), request.getParameterTypes(), request.getArgs());
        try {
            Method method = clazz.getDeclaredMethod(request.getMethodName(), request.getParameterTypes());
            method.setAccessible(true);
            Object result = method.invoke(endpoint, request.getArgs());
            request.getReplyTo().tell(StatusReply.success(new RpcResponse(result, null)));
        } catch (Exception e) {
            getContext().getLog().error("处理 rpc 请求失败! methodName: {}, parameterTypes: {}, args: {}",
                    request.getMethodName(), request.getParameterTypes(), request.getArgs(), e);
            request.getReplyTo().tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

}
