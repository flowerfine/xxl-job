package com.xxl.job.admin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.xxl.job.dao")
public class MybatisConfig {

}
