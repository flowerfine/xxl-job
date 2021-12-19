package com.xxl.job.core.thread;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.JacksonUtil;
import com.xxl.job.core.util.NetUtil;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.RegistryParam;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

public class ExecutorRegistryTask extends AbstractTask {

    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryTask.class);

    private final String appname;
    private final String host;

    public ExecutorRegistryTask(String appname, String address, int port) {
        if (appname == null || appname.trim().length() == 0) {
            throw new IllegalArgumentException(">>>>>>>>>>> xxl-job, executor registry config fail, appname is null.");
        }
        if (StringUtils.hasText(address) == false) {
            address = IpUtil.getIp();
        }
        if (port == 0) {
            port = NetUtil.findAvailablePort(9998);
        }

        if (XxlJobExecutor.getAdminBizList() == null) {
            throw new IllegalArgumentException(">>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null.");
        }
        this.appname = appname;
        this.host = address + ":" + port;
    }

    @Override
    protected String getThreadName() {
        return "xxl-job, executor ExecutorRegistryThread";
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        registry();
        if (isActive()) {
            timeout.timer().newTimeout(this, RegistryConfig.BEAT_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    @Override
    public void toStop() {
        super.toStop();
        remove();
    }

    private void registry() {
        try {
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, host);
            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                try {
                    ReturnT<String> registryResult = adminBiz.registry(registryParam);
                    if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                        registryResult = ReturnT.SUCCESS;
                        logger.info(">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}",
                                JacksonUtil.toJsonString(registryParam), JacksonUtil.toJsonString(registryResult));
                        break;
                    } else {
                        logger.error(">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}",
                                JacksonUtil.toJsonString(registryParam), JacksonUtil.toJsonString(registryResult));
                    }
                } catch (Exception e) {
                    logger.error(">>>>>>>>>>> xxl-job registry error, registryParam:{}",
                            JacksonUtil.toJsonString(registryParam), e);
                }
            }
        } catch (Exception e) {
            if (isActive()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void remove() {
        try {
            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, host);
            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                try {
                    ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                    if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                        registryResult = ReturnT.SUCCESS;
                        logger.info(">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}",
                                JacksonUtil.toJsonString(registryParam), JacksonUtil.toJsonString(registryResult));
                        break;
                    } else {
                        logger.error(">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}",
                                JacksonUtil.toJsonString(registryParam), JacksonUtil.toJsonString(registryResult));
                    }
                } catch (Exception e) {
                    if (isActive()) {
                        logger.error(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}",
                                JacksonUtil.toJsonString(registryParam), e);
                    }
                }
            }
        } catch (Exception e) {
            if (isActive()) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info(">>>>>>>>>>> xxl-job, executor registry thread destory.");
    }

}
