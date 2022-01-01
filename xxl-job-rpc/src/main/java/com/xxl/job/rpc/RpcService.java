package com.xxl.job.rpc;

import java.util.concurrent.CompletableFuture;

public interface RpcService {

    <C> C connect(String host, int port, String endpoint,  Class<C> gateway);

    <C extends RpcServer> CompletableFuture<C> start(String endpoint, Object server);

}
