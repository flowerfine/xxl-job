package com.xxl.job.spring.boot.autoconfigure;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobAutoConfiguration {

    private final XxlJobProperties properties;
    private final List<XxlJobExecutorCustomizer> customizers;

    public XxlJobAutoConfiguration(XxlJobProperties properties,
                                   ObjectProvider<List<XxlJobExecutorCustomizer>> xxlJobExecutorCustomizersProvider) {
        this.properties = properties;
        this.customizers = xxlJobExecutorCustomizersProvider.getIfAvailable();
    }

    @Bean
    @ConditionalOnMissingBean
    public XxlJobExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(properties.getAdminAddresses());
        xxlJobSpringExecutor.setAppname(properties.getExecutorAppName());
        xxlJobSpringExecutor.setPort(properties.getExecutorPort());
        xxlJobSpringExecutor.setLogPath(System.getProperty("user.home") + "/logs/jobhandler");
        xxlJobSpringExecutor.setLogRetentionDays(7);
        customizers.forEach(customizer -> customizer.customize(xxlJobSpringExecutor));
        return xxlJobSpringExecutor;
    }
}
