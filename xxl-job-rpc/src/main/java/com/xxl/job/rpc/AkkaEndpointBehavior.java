package com.xxl.job.rpc;

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

import java.lang.reflect.Method;

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
        } catch (Exception e) {
            getContext().getLog().error("反序列化 rpc 请求失败!", e);
            request.getReplyTo().tell(StatusReply.error(e));
            return Behaviors.same();
        }
        try {
            Method method = clazz.getDeclaredMethod(invocation.getMethodName(), invocation.getParameterTypes());
            method.setAccessible(true);
            Object result = method.invoke(endpoint, invocation.getArgs());
            request.getReplyTo().tell(StatusReply.success(new RpcResponse(serialize(result), null)));
        } catch (Exception e) {
            getContext().getLog().error("处理 rpc 请求失败! methodName: {}, parameterTypes: {}, args: {}",
                    invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArgs(), e);
            request.getReplyTo().tell(StatusReply.error(e));
        }
        return Behaviors.same();
    }

}
