package com.xxl.job.dao.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class XxlJobGroup {

    /**
     * id
     */
    private Integer id;

    /**
     * 执行器AppName
     */
    private String appName;

    /**
     * 执行器名称
     */
    private String title;

    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    private Byte addressType;

    /**
     * 执行器地址列表，多地址逗号分隔
     */
    private String addressList;
}