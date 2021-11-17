package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.quickhttp.domain.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDownloader implements Downloader{
    private Logger logger = LoggerFactory.getLogger(AbstractDownloader.class);

    /**子下载线程*/
    protected Future[] downloadThreadFutures;

    /**
     * 合并分段文件
     * @param downloadHolder 下载任务
     * */
    protected void mergeSubFileList(DownloadHolder downloadHolder, CountDownLatch countDownLatch) throws IOException {
        int downloadTimeoutMillis = downloadHolder.downloadTask.downloadTimeoutMillis==3600000?downloadHolder.downloadTask.downloadTimeoutMillis:downloadHolder.poolConfig.downloadTimeoutMillis;
        try {
            countDownLatch.await(downloadTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("[线程中断]停止下载文件");
            Thread.currentThread().interrupt();
            if(null!=downloadThreadFutures){
                for(Future future:downloadThreadFutures){
                    future.cancel(true);
                }
            }
            return;
        }
        //检查是否可以合并
        downloadHolder.log(LogLevel.TRACE,"[检查文件是可以合并]");
        long mergeFileSize = 0;
        for(Path subFile:downloadHolder.downloadProgress.subFileList){
            if(Files.notExists(subFile)){
                downloadHolder.log(LogLevel.TRACE,"[文件合并失败]分段文件不存在,路径:{}",subFile);
                throw new IOException("文件合并失败,分段文件不存在!路径:"+subFile.toString());
            }
            mergeFileSize += Files.size(subFile);
        }
        if(!downloadHolder.downloadTask.m3u8){
            //检查合并后文件大小是否相同
            long contentLength = downloadHolder.response.contentLength();
            if(null==downloadHolder.response.contentEncoding()&&contentLength>0&&mergeFileSize!=contentLength){
                downloadHolder.log(LogLevel.WARN,"[文件大小不匹配]预期文件大小:{},实际合并文件大小:{}",contentLength,mergeFileSize);
                return;
            }
        }

        Path file = downloadHolder.file;
        if(Files.notExists(file.getParent())){
            Files.createDirectories(file.getParent());
        }
        Files.deleteIfExists(file);
        Files.createFile(file);
        FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.APPEND);
        long currentSize = 0;
        for(Path subFile:downloadHolder.downloadProgress.subFileList){
            fileChannel.transferFrom(Files.newByteChannel(subFile),currentSize,Files.size(subFile));
            currentSize += Files.size(subFile);
        }
        fileChannel.close();
        downloadHolder.log(LogLevel.INFO,"[合并文件完成]大小:{},合并文件路径:{}",Files.size(downloadHolder.file),downloadHolder.file);
    }
}
