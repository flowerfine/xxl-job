package com.xxl.job.core.context;

import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.util.DateUtil;
import com.xxl.job.core.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * helper for xxl-job
 */
public class XxlJobHelper {

    private static Logger logger = LoggerFactory.getLogger("xxl-job logger");

    public static long getJobId() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getJobId();
    }

    public static String getJobParam() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobParam();
    }

    public static String getJobLogFileName() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobLogFileName();
    }

    public static int getShardIndex() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardIndex();
    }

    public static int getShardTotal() {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardTotal();
    }

    public static boolean log(String appendLogPattern, Object ... appendLogArguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    public static boolean log(Throwable e) {
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        String appendLog = ThrowableUtil.toString(e);
        return logDetail(callInfo, appendLog);
    }

    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("["+ callInfo.getClassName() + "#" + callInfo.getMethodName() +"]").append("-")
                .append("["+ callInfo.getLineNumber() +"]").append("-")
                .append("["+ Thread.currentThread().getName() +"]").append(" ")
                .append(appendLog!=null?appendLog:"");
        String formatAppendLog = stringBuffer.toString();

        // appendlog
        String logFileName = xxlJobContext.getJobLogFileName();

        if (logFileName!=null && logFileName.trim().length()>0) {
            XxlJobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }

    public static boolean handleSuccess(){
        return handleResult(XxlJobContext.HANDLE_COCE_SUCCESS, null);
    }

    public static boolean handleSuccess(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_COCE_SUCCESS, handleMsg);
    }

    public static boolean handleFail(){
        return handleResult(XxlJobContext.HANDLE_COCE_FAIL, null);
    }

    public static boolean handleFail(String handleMsg) {
        return handleResult(XxlJobContext.HANDLE_COCE_FAIL, handleMsg);
    }

    public static boolean handleTimeout(){
        return handleResult(XxlJobContext.HANDLE_COCE_TIMEOUT, null);
    }

    public static boolean handleTimeout(String handleMsg){
        return handleResult(XxlJobContext.HANDLE_COCE_TIMEOUT, handleMsg);
    }

    public static boolean handleResult(int handleCode, String handleMsg) {
        XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        xxlJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            xxlJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }

}
