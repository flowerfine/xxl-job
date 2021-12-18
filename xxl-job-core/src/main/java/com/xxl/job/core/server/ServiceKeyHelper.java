package com.xxl.job.core.server;

import akka.actor.typed.receptionist.ServiceKey;

public class ServiceKeyHelper {

    private ServiceKeyHelper() {
        throw new IllegalStateException("no instance");
    }

    public static <T> ServiceKey<T> getServiceKey(Class<T> clazz, String appname) {
        return ServiceKey.create(clazz, appname);
    }
}
