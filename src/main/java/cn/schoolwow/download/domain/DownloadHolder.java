package cn.schoolwow.download.domain;

import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public class DownloadHolder {
    /**下载任务*/
    public DownloadTask downloadTask;

    /**下载任务最终保存路径*/
    public Path file;

    /**http请求返回对象*/
    public Response response;

    /**下载池配置项*/
    public DownloadPoolConfig downloadPoolConfig;

    /**关联下载进度*/
    public DownloadProgress downloadProgress;

    /**批量下载任务线程同步*/
    public CountDownLatch countDownLatch;

    /**下载线程*/
    public Thread downloadThread;
}
