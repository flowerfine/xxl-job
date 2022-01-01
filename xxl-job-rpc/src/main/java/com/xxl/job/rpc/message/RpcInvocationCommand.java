package com.xxl.job.rpc.message;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import com.xxl.job.rpc.message.protocol.RpcResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RpcInvocationCommand implements Message {

    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] args;
    private ActorRef<StatusReply<RpcResponse>> replyTo;
}
