package com.xxl.job.rpc;

import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class ActorSelectionHelper {

    public static final String ACTOR_SYSTEM = "akka-rpc";
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
