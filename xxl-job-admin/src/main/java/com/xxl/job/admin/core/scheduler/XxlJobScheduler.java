package com.xxl.job.admin.core.scheduler;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.xxl.job.remote.ExecutorService;
import com.xxl.job.rpc.ActorSelectionHelper;
import com.xxl.job.rpc.AkkaRpcService;
import com.xxl.job.rpc.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class XxlJobScheduler {

    private static ConcurrentMap<String, ConcurrentMap<String, ExecutorService>> executorMap = new ConcurrentHashMap<>();

    private static RpcService rpcService;

    @Autowired
    public void setActorSystem(ActorSystem<SpawnProtocol.Command> actorSystem) {
        XxlJobScheduler.rpcService = new AkkaRpcService(actorSystem);
    }

    public static ExecutorService getExecutorBiz(String appname, String address) throws Exception {
        if (StringUtils.hasText(appname) == false) {
            return null;
        }
        ConcurrentMap<String, ExecutorService> addressMap = executorMap.computeIfAbsent(appname, (key) -> new ConcurrentHashMap<>());
        if (StringUtils.hasText(address) == false) {
            return null;
        }

        ExecutorService executorBiz = addressMap.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        String host = ActorSelectionHelper.getIp(address);
        int port = ActorSelectionHelper.getPort(address);

        executorBiz = rpcService.connect(host, port, ActorSelectionHelper.EXECUTOR_ENDPOINT, ExecutorService.class);
        addressMap.put(address, executorBiz);
        return executorBiz;
    }

}
