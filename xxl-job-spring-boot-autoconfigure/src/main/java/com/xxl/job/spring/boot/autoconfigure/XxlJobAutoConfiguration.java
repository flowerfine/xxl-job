package com.xxl.job.spring.boot.autoconfigure;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Configuration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobAutoConfiguration {

    @Autowired
    private XxlJobProperties properties;
    private final List<XxlJobExecutorCustomizer> customizers;

    public XxlJobAutoConfiguration(ObjectProvider<List<XxlJobExecutorCustomizer>> xxlJobExecutorCustomizersProvider) {
        this.customizers = xxlJobExecutorCustomizersProvider.getIfAvailable();
    }

    @Bean
    @ConditionalOnMissingBean
    public XxlJobExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(properties.getAdminAddresses());
        xxlJobSpringExecutor.setAppname(properties.getExecutorAppName());
        if (properties.getExecutorPort() != null) {
            xxlJobSpringExecutor.setPort(properties.getExecutorPort());
        }
        xxlJobSpringExecutor.setLogPath(System.getProperty("user.home") + "/logs/jobhandler");
        xxlJobSpringExecutor.setLogRetentionDays(7);
        if (CollectionUtils.isEmpty(customizers) == false) {
            customizers.forEach(customizer -> customizer.customize(xxlJobSpringExecutor));
        }
        return xxlJobSpringExecutor;
    }
}
