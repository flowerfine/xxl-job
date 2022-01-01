package com.xxl.job.rpc;

import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class ActorSelectionHelper {

    public static final String ACTOR_SYSTEM = "xxl-job";
    public static final String EXECUTOR_ENDPOINT = "executor";
    public static final String ADMIN_ENDPOINT = "admin";
    public static final int PORT = 20777;

    private static final String AKKA_PATH_TEMPLATE = "akka://${actorSystem}@${address}";
    private static final String AKKA_USER_PATH = "akka://${actorSystem}@${host}:${port}/user/${actorPath}";

    private ActorSelectionHelper() {
        throw new IllegalStateException("no instance");
    }

    public static String getRemotePath(String host, int port, String path) {
        Map<String, String> substitutes = wrapSubstitute(host, port, path);
        StringSubstitutor substitutor = new StringSubstitutor(substitutes);
        return substitutor.replace(AKKA_USER_PATH);
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

    public static String getIp(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        int index = address.indexOf(":");
        if (index > 0) {
            return address.substring(0, index);
        }
        return "";
    }

    public static int getPort(String address) {
        if (address == null || address.isEmpty()) {
            return -1;
        }
        int index = address.indexOf(":");
        if (index > 0) {
            return Integer.parseInt(address.substring(index + 1));
        }
        return -1;
    }

    private static Map<String, String> wrapSubstitute(String host, int port, String actorPath) {
        return wrapSubstitute(ACTOR_SYSTEM, host, port, actorPath);
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
