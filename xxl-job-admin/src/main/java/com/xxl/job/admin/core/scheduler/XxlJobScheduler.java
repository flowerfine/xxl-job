package com.xxl.job.admin.core.scheduler;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import com.xxl.job.admin.core.thread.*;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.remote.client.ExecutorBehavior;
import com.xxl.job.core.remote.client.ExecutorClient;
import com.xxl.job.core.server.AkkaServer;
import com.xxl.job.remote.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
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
public class XxlJobScheduler implements InitializingBean, DisposableBean {

    private static ConcurrentMap<String, ConcurrentMap<String, ExecutorService>> executorMap = new ConcurrentHashMap<>();

    @Autowired
    private static ActorSystem<SpawnProtocol.Command> actorSystem;

    @Override
    public void afterPropertiesSet() throws Exception {
        initI18n();

        // admin trigger pool start
        JobTriggerPoolHelper.toStart();

        // admin registry monitor run
        JobRegistryHelper.getInstance().start();

        // admin fail-monitor run
        JobFailMonitorHelper.getInstance().start();

        // admin lose-monitor run ( depend on JobTriggerPoolHelper )
        JobCompleteHelper.getInstance().start();

        // admin log report start
        JobLogReportHelper.getInstance().start();

        // start-schedule  ( depend on JobTriggerPoolHelper )
        JobScheduleHelper.getInstance().start();

        log.info(">>>>>>>>> init xxl-job admin success.");
    }

    public void destroy() throws Exception {

        // stop-schedule
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        JobLogReportHelper.getInstance().toStop();

        // admin lose-monitor stop
        JobCompleteHelper.getInstance().toStop();

        // admin fail-monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        JobRegistryHelper.getInstance().toStop();

        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();

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
