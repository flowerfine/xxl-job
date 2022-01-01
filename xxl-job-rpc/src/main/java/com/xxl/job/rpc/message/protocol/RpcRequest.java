package com.xxl.job.rpc.message.protocol;

import akka.actor.typed.ActorRef;
import akka.pattern.StatusReply;
import com.xxl.job.rpc.message.RpcProtocol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements RpcProtocol {

    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] args;
    private ActorRef<StatusReply<RpcResponse>> replyTo;
}
