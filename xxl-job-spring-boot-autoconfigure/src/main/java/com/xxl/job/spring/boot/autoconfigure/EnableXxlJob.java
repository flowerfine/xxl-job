package com.xxl.job.spring.boot.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(XxlJobAutoConfiguration.class)
public @interface EnableXxlJob {

}
