package com.xxl.job.rpc.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RpcInvocation implements Message {

    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] args;
}
