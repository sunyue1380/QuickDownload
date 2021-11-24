package cn.schoolwow.download.domain;

import cn.schoolwow.download.pool.DownloadPoolImpl;

import java.util.concurrent.Future;

/**下载任务令牌*/
public class DownloadFuture {
    /**下载任务信息*/
    public DownloadHolder downloadHolder;

    /**下载线程Future*/
    public Future downloadThreadFuture;

    /**下载线程池*/
    public DownloadPoolImpl downloadPool;

    /**停止下载*/
    public void stopDownload(){
        if(null!=downloadThreadFuture){
            downloadThreadFuture.cancel(true);
        }
    }

    /**恢复下载*/
    public void resumeDownload(){
        downloadPool.poolConfig.threadPoolExecutor.submit(downloadHolder.priorityThread);
    }
}