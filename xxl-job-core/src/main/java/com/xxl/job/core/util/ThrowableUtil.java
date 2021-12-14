package com.xxl.job.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Throwable Util
 */
public class ThrowableUtil {

    /**
     * parse error to string
     */
    public static String toString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String errorMsg = stringWriter.toString();
        return errorMsg;
    }

}
