package com.xxl.job.core.thread;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.util.JacksonUtil;
import com.xxl.job.remote.ActorSelectionHelper;
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

    public ExecutorRegistryTask(String appname, String host) {
        if (appname == null || appname.trim().length() == 0) {
            throw new IllegalArgumentException(">>>>>>>>>>> xxl-job, executor registry config fail, appname is null.");
        }
        if (XxlJobExecutor.getAdminBizList() == null) {
            throw new IllegalArgumentException(">>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null.");
        }
        if (StringUtils.hasText(host) == false) {
            Config config = ConfigFactory.load("xxl-job-executor.conf");
            host = config.getString("akka.remote.artery.canonical.host") + ":" + config.getInt("akka.remote.artery.canonical.port");
        } else {
            host = ActorSelectionHelper.getIpAndPortFromAkkaPath(host);
        }
        this.appname = appname;
        this.host = host;
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
