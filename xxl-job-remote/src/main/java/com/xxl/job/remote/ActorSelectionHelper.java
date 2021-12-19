package com.xxl.job.remote;

import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class ActorSelectionHelper {

    public static final String ACTOR_SYSTEM = "xxl-job";
    public static final int PORT = 20777;

    private static final String AKKA_PATH_TEMPLATE = "akka://${actorSystem}@${address}";
    private static final String AKKA_USER_PATH = "akka://${actorSystem}@${host}:${port}/user/${actorPath}";
    private static final String XXL_JOB_ADMIN_PATH = "akka://${actorSystem}@${host}:${port}/user/admin/${actorPath}";
    private static final String XXL_JOB_EXECUTOR_PATH = "akka://${actorSystem}@${host}:${port}/user/executor/${actorPath}";

    private ActorSelectionHelper() {
        throw new IllegalStateException("no instance");
    }

    public static String getAppnameAddress(String actorSystem, String address) {
        Map<String, String> substitutes = new HashMap<>();
        substitutes.put("actorSystem", actorSystem);
        substitutes.put("address", address);
        StringSubstitutor substitutor = new StringSubstitutor(substitutes);
        return substitutor.replace(AKKA_PATH_TEMPLATE);
    }

    public static String getAdminRouterPath(String host, String path) {
        Map<String, String> substitutes = wrapSubstitute(host, path);
        StringSubstitutor substitutor = new StringSubstitutor(substitutes);
        return substitutor.replace(XXL_JOB_ADMIN_PATH);
    }

    public static String getExecutorRouterPath(String host, String path) {
        Map<String, String> substitutes = wrapSubstitute(host, path);
        StringSubstitutor substitutor = new StringSubstitutor(substitutes);
        return substitutor.replace(XXL_JOB_EXECUTOR_PATH);
    }

    public static String getIpAndPortFromAkkaPath(String akkaPath) {
        if (akkaPath == null || akkaPath.isEmpty()) {
            return "";
        }
        String subStr = akkaPath;
        int index = akkaPath.indexOf("@");
        if (index > 0) {
            subStr = akkaPath.substring(index + 1);
        }

        index = subStr.indexOf("/");
        if (index > 0) {
            subStr = subStr.substring(0, index);
        }

        return subStr;
    }

    private static Map<String, String> wrapSubstitute(String host, String actorPath) {
        return wrapSubstitute(ACTOR_SYSTEM, host, PORT, actorPath);
    }

    private static Map<String, String> wrapSubstitute(String actorSystem, String host, int port, String actorPath) {
        Map<String, String> substitutes = new HashMap<>();
        substitutes.put("actorSystem", actorSystem);
        substitutes.put("host", host);
        substitutes.put("port", String.valueOf(port));
        substitutes.put("actorPath", actorPath);
        return substitutes;
    }

}
