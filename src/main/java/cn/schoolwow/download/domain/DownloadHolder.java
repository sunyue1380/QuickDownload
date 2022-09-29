package cn.schoolwow.download.domain;

import cn.schoolwow.download.pool.PriorityThread;
import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class DownloadHolder {
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

    /**下载线程Future*/
    public Future downloadThreadFuture;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadHolder that = (DownloadHolder) o;

        if (file != null ? !file.equals(that.file) : that.file != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }
}