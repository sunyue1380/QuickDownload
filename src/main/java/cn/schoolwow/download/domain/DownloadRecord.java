package cn.schoolwow.download.domain;

import cn.schoolwow.download.pool.DownloadPoolImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**下载记录*/
public class DownloadRecord {
    /**下载线程Future*/
    private DownloadHolder downloadHolder;

    /**下载线程池*/
    private DownloadPoolImpl downloadPool;

    public DownloadRecord(DownloadHolder downloadHolder, DownloadPoolImpl downloadPool) {
        this.downloadHolder = downloadHolder;
        this.downloadPool = downloadPool;
    }

    /**下载路径*/
    public String filePath(){
        return downloadHolder.file.toString();
    }

    /**暂停下载*/
    public void pauseDownload(){
        if(null!=downloadHolder.downloadThreadFuture){
            downloadHolder.downloadThreadFuture.cancel(true);
        }
    }

    /**恢复下载*/
    public void resumeDownload(){
        //判断当前状态是否为下载中断
        if("下载中断".equalsIgnoreCase(downloadHolder.downloadProgress.state)){
            downloadPool.poolConfig.threadPoolExecutor.submit(downloadHolder.priorityThread);
        }
    }

    /**删除记录*/
    public void deleteDownloadRecord(){
        pauseDownload();
        downloadPool.downloadHolderListLock.lock();
        try {
            Path[] paths = downloadHolder.downloadProgress.subFileList;
            for(Path path:paths){
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            downloadPool.downloadHolderList.remove(downloadHolder);
        }finally {
            downloadPool.downloadHolderListLock.unlock();
        }
    }
}
