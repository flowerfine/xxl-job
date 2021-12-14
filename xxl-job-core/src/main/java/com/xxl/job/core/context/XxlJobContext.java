package com.xxl.job.core.context;

/**
 * xxl-job context
 */
public class XxlJobContext {

    public static final int HANDLE_COCE_SUCCESS = 200;
    public static final int HANDLE_COCE_FAIL = 500;
    public static final int HANDLE_COCE_TIMEOUT = 502;

    private final long jobId;
    private final String jobParam;

    private final String jobLogFileName;

    private final int shardIndex;
    private final int shardTotal;

    private int handleCode;
    private String handleMsg;

    public XxlJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;

        this.handleCode = HANDLE_COCE_SUCCESS;  // default success
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public String getJobLogFileName() {
        return jobLogFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    // ---------------------- tool ----------------------

    private static InheritableThreadLocal<XxlJobContext> contextHolder = new InheritableThreadLocal<XxlJobContext>(); // support for child thread of job handler)

    public static void setXxlJobContext(XxlJobContext xxlJobContext){
        contextHolder.set(xxlJobContext);
    }

    public static XxlJobContext getXxlJobContext(){
        return contextHolder.get();
    }

}