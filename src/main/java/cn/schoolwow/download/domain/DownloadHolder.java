package cn.schoolwow.download.domain;

import cn.schoolwow.download.pool.PriorityThread;
import cn.schoolwow.quickhttp.domain.LogLevel;
import cn.schoolwow.quickhttp.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

public class DownloadHolder {
    private Logger logger = LoggerFactory.getLogger(DownloadHolder.class);

    /**下载任务*/
    public DownloadTask downloadTask;

    /**下载任务最终保存路径*/
    public Path file;

    /**http请求返回对象*/
    public Response response;

    /**下载池配置项*/
    public PoolConfig poolConfig;

    /**关联下载进度*/
    public DownloadProgress downloadProgress;

    /**批量下载任务线程同步*/
    public CountDownLatch countDownLatch;

    /**优先级下载任务线程*/
    public PriorityThread priorityThread;

    /**记录下载日志*/
    public volatile PrintWriter pw;

    public void openDownloadLogFile() {
        if(null!=downloadTask.downloadLogFilePath){
            try {
                File logFile = new File(downloadTask.downloadLogFilePath);
                logFile.getParentFile().mkdirs();
                pw = new PrintWriter(new FileWriter(logFile,true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void appendLog(String logFilePath){
        byte[] bytes = new byte[0];
        try {
            bytes = Files.readAllBytes(Paths.get(logFilePath));
            pw.write(new String(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void log(LogLevel logLevel, String message, Object... parameters){
        if(null!=pw){
            StringBuilder builder = new StringBuilder(message);
            for(Object parameter:parameters){
                int startIndex = builder.indexOf("{");
                int endIndex = builder.indexOf("}");
                if(startIndex>0&&endIndex>0){
                    builder.replace(startIndex,endIndex+1,parameter.toString());
                }
            }
            pw.append((logLevel.name().toString() + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) + " " + builder.toString()+"\n"));
        }
        switch (logLevel){
            case TRACE:logger.trace(message,parameters);break;
            case DEBUG:logger.debug(message,parameters);break;
            case INFO:logger.info(message,parameters);break;
            case WARN:logger.warn(message,parameters);break;
            case ERROR:logger.error(message,parameters);break;
        }
    }

    public void closeDownloadLogFile() {
        if(null!=pw){
            pw.flush();
            pw.close();
            pw = null;
        }
    }
}