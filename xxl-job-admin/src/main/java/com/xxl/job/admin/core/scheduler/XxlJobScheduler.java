package com.xxl.job.admin.core.scheduler;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Props;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.remote.client.ExecutorBehavior;
import com.xxl.job.core.remote.client.ExecutorClient;
import com.xxl.job.core.server.AkkaServer;
import com.xxl.job.remote.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class XxlJobScheduler implements InitializingBean {

    private static ConcurrentMap<String, ConcurrentMap<String, ExecutorService>> executorMap = new ConcurrentHashMap<>();

    @Autowired
    private static ActorSystem<SpawnProtocol.Command> actorSystem;

    @Override
    public void afterPropertiesSet() throws Exception {
        initI18n();
        log.info(">>>>>>>>> init xxl-job admin success.");
    }

    private void initI18n() {
        for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
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

        String actorName = "Executor-" + appname + "-" + address;
        CompletionStage<ActorRef<ExecutorBehavior.Command>> registered =
                AskPattern.ask(
                        actorSystem,
                        replyTo -> new SpawnProtocol.Spawn<>(Behaviors.setup(ctx -> new ExecutorBehavior(ctx, appname, address)), actorName, Props.empty(), replyTo),
                        Duration.ofSeconds(3L),
                        actorSystem.scheduler());
        ActorRef<ExecutorBehavior.Command> actorRef = registered.toCompletableFuture().get();
        CompletionStage<ActorRef<AkkaServer.Command>> client =
                AskPattern.ask(
                        actorRef,
                        replyTo -> new ExecutorBehavior.RemoteActorCommand(replyTo),
                        Duration.ofSeconds(3L),
                        actorSystem.scheduler());

        executorBiz = new ExecutorClient(actorSystem, client.toCompletableFuture().get());
        addressMap.put(address, executorBiz);
        return executorBiz;
    }

}
