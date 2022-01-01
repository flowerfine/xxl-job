package com.xxl.job.admin.config;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.cluster.typed.Cluster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
//@Configuration
public class AkkaClusterConfig {

    @Autowired
    private ActorSystem<SpawnProtocol.Command> actorSystem;

    @Bean
    public Cluster cluster() {
        return Cluster.get(actorSystem);
    }
}