package com.xxl.job.spring.boot.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = XxlJobProperties.XXL_JOB_PREFIX)
public class XxlJobProperties {

    public static final String XXL_JOB_PREFIX = "com.xxl.job";

    private String adminAddresses;

    private String accessToken;

    private String executorAppName;

    private String executorAddress;

    private String executorIp;

    private Integer executorPort;

    private String executorLogPath;

    private Duration executorLogRetentionDuration;
}

