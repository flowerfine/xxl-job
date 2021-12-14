package com.xxl.job.core.thread;

import com.xxl.job.core.log.XxlJobFileAppender;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class JobLogFileCleanTask extends AbstractTask {

    private static Logger logger = LoggerFactory.getLogger(JobLogFileCleanTask.class);

    private Duration logRetentionDuration;

    public JobLogFileCleanTask(Duration logRetentionDuration) {
        this.logRetentionDuration = logRetentionDuration;
    }

    @Override
    protected String getThreadName() {
        return "xxl-job, executor JobLogFileCleanThread\"";
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        cleanJobLogFile();
        if (isActive()) {
            timeout.timer().newTimeout(this, 1, TimeUnit.HOURS);
        }
    }

    private void cleanJobLogFile() {
        try {
            Path logPath = Paths.get(XxlJobFileAppender.getLogPath());
            long now = System.currentTimeMillis();
            Files.walkFileTree(logPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    FileTime lastModifiedTime = Files.getLastModifiedTime(file);
                    Instant instant = lastModifiedTime.toInstant();
                    if (now - instant.toEpochMilli() > logRetentionDuration.toMillis()) {
                        Files.deleteIfExists(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    boolean isEmpty = Files.list(dir).findAny().isPresent();
                    if (isEmpty) {
                        Files.deleteIfExists(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            if (isActive()) {
                logger.error("清理日志文件异常!", e);
            }
        }
    }
}
