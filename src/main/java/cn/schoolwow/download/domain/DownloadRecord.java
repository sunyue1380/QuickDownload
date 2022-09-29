package cn.schoolwow.download.domain;

import cn.schoolwow.download.pool.DownloadPoolImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**下载记录*/
public class DownloadRecord {
    private Logger logger = LoggerFactory.getLogger(DownloadRecord.class);

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
            logger.debug("下载中断,路径:{}", null==downloadHolder.file?downloadHolder.downloadTask.filePath:downloadHolder.file);
            downloadHolder.downloadThreadFuture.cancel(true);
            downloadHolder.downloadProgress.state = "下载中断";
        }
    }

    /**恢复下载*/
    public void resumeDownload(){
        //判断当前状态是否为下载中断
        if("下载中断".equalsIgnoreCase(downloadHolder.downloadProgress.state)){
            logger.debug("恢复下载,路径:{}", null==downloadHolder.file?downloadHolder.downloadTask.filePath:downloadHolder.file);
            downloadPool.poolConfig.threadPoolExecutor.submit(downloadHolder.priorityThread);
        }
    }

    /**删除记录*/
    public void deleteDownloadRecord(){
        pauseDownload();
        synchronized (downloadPool.downloadHolderList){
            logger.debug("删除下载记录,路径:{}", null==downloadHolder.file?downloadHolder.downloadTask.filePath:downloadHolder.file);
            Path[] paths = downloadHolder.downloadProgress.subFileList;
            for(Path path:paths){
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    logger.warn("停止下载,删除分段文件失败", e);
                }
            }
            downloadPool.downloadHolderList.remove(downloadHolder);
        }
    }
}
