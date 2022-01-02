package com.xxl.job.rpc.message.protocol;

import com.xxl.job.rpc.message.RpcProtocol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse implements RpcProtocol {

    private byte[] msg;
    private Throwable throwable;
}
