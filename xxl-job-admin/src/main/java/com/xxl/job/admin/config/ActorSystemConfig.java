package com.xxl.job.admin.config;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.xxl.job.rpc.util.AkkaUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorSystemConfig {

    @Bean(destroyMethod = "terminate")
    public ActorSystem<SpawnProtocol.Command> actorSystem() {
        return AkkaUtil.startConsumerActorSystem();
    }

}