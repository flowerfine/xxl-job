package com.xxl.job.rpc.message.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RpcInvocation {

    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] args;
}
