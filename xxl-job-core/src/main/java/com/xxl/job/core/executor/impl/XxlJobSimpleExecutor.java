package com.xxl.job.core.executor.impl;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * xxl-job executor (for frameless)
 */
public class XxlJobSimpleExecutor extends XxlJobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobSimpleExecutor.class);

    @Setter
    @Getter
    private List<Object> xxlJobBeanList = new ArrayList<>();

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(xxlJobBeanList);
    }

    private void initJobHandlerMethodRepository(List<Object> xxlJobBeanList) {
        if (xxlJobBeanList == null || xxlJobBeanList.size() == 0) {
            return;
        }

        // init job handler from method
        for (Object bean : xxlJobBeanList) {
            // method
            Method[] methods = bean.getClass().getDeclaredMethods();
            if (methods == null || methods.length == 0) {
                continue;
            }
            for (Method executeMethod : methods) {

                // anno
                XxlJob xxlJob = executeMethod.getAnnotation(XxlJob.class);
                if (xxlJob == null) {
                    continue;
                }

                String name = xxlJob.value();
                if (name.trim().length() == 0) {
                    throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + bean.getClass() + "#" + executeMethod.getName() + "] .");
                }
                if (loadJobHandler(name) != null) {
                    throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
                }

                executeMethod.setAccessible(true);

                // init and destory
                Method initMethod = null;
                Method destroyMethod = null;

                if (xxlJob.init().trim().length() > 0) {
                    try {
                        initMethod = bean.getClass().getDeclaredMethod(xxlJob.init());
                        initMethod.setAccessible(true);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("xxl-job method-jobhandler initMethod invalid, for[" + bean.getClass() + "#" + executeMethod.getName() + "] .");
                    }
                }
                if (xxlJob.destroy().trim().length() > 0) {
                    try {
                        destroyMethod = bean.getClass().getDeclaredMethod(xxlJob.destroy());
                        destroyMethod.setAccessible(true);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("xxl-job method-jobhandler destroyMethod invalid, for[" + bean.getClass() + "#" + executeMethod.getName() + "] .");
                    }
                }

                // registry jobhandler
                registJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));


            }

        }

    }

}
