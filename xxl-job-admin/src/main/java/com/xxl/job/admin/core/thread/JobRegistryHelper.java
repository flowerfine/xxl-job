package com.xxl.job.admin.core.thread;

import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.dao.XxlJobGroupDao;
import com.xxl.job.dao.XxlJobRegistryDao;
import com.xxl.job.dao.model.XxlJobGroup;
import com.xxl.job.dao.model.XxlJobRegistry;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.RegistryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * job registry instance
 */
@Slf4j
@Component
public class JobRegistryHelper implements InitializingBean, DisposableBean {

    private ThreadPoolExecutor registryOrRemoveThreadPool = null;
    private Thread registryMonitorThread;
    private volatile boolean toStop = false;

    @Autowired
    private XxlJobGroupDao xxlJobGroupDao;
    @Autowired
    private XxlJobRegistryDao xxlJobRegistryDao;

    @Override
    public void afterPropertiesSet() throws Exception {

        // for registry or remove
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                        log.warn(">>>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now).");
                    }
                });

        // for monitor
        registryMonitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop) {
                    try {
                        // auto registry group
                        List<XxlJobGroup> groupList = xxlJobGroupDao.findByAddressType(0);
                        if (groupList != null && !groupList.isEmpty()) {

                            // remove dead address (admin/executor)
                            List<Integer> ids = xxlJobRegistryDao.findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                            if (ids != null && ids.size() > 0) {
                                xxlJobRegistryDao.removeDead(ids);
                            }

                            // fresh online address (admin/executor)
                            HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
                            List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                            if (list != null) {
                                for (XxlJobRegistry item : list) {
                                    if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                        String appname = item.getRegistryKey();
                                        List<String> registryList = appAddressMap.get(appname);
                                        if (registryList == null) {
                                            registryList = new ArrayList<String>();
                                        }

                                        if (!registryList.contains(item.getRegistryValue())) {
                                            registryList.add(item.getRegistryValue());
                                        }
                                        appAddressMap.put(appname, registryList);
                                    }
                                }
                            }

                            // fresh group address
                            for (XxlJobGroup group : groupList) {
                                List<String> registryList = appAddressMap.get(group.getAppname());
                                String addressListStr = null;
                                if (registryList != null && !registryList.isEmpty()) {
                                    Collections.sort(registryList);
                                    StringBuilder addressListSB = new StringBuilder();
                                    for (String item : registryList) {
                                        addressListSB.append(item).append(",");
                                    }
                                    addressListStr = addressListSB.toString();
                                    addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
                                }
                                group.setAddressList(addressListStr);
                                group.setUpdateTime(new Date());

                                xxlJobGroupDao.update(group);
                            }
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    } catch (InterruptedException e) {
                        if (!toStop) {
                            log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                        }
                    }
                }
                log.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
            }
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

	@Override
	public void destroy() throws Exception {
        toStop = true;

        // stop registryOrRemoveThreadPool
        registryOrRemoveThreadPool.shutdownNow();

        // stop monitir (interrupt and wait)
        registryMonitorThread.interrupt();
        try {
            registryMonitorThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> registry(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                int ret = xxlJobRegistryDao.registryUpdate(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), new Date());
                if (ret < 1) {
                    xxlJobRegistryDao.registrySave(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue(), new Date());

                    // fresh
                    freshGroupRegistryInfo(registryParam);
                }
            }
        });

        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                int ret = xxlJobRegistryDao.registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
                if (ret > 0) {
                    // fresh
                    freshGroupRegistryInfo(registryParam);
                }
            }
        });

        return ReturnT.SUCCESS;
    }

    private void freshGroupRegistryInfo(RegistryParam registryParam) {
        // Under consideration, prevent affecting core tables
    }


}
