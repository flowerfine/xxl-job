package com.xxl.job.spring.boot.autoconfigure;

import com.xxl.job.core.executor.XxlJobExecutor;

@FunctionalInterface
public interface XxlJobExecutorCustomizer {

    void customize(XxlJobExecutor xxlJobExecutor);
}
