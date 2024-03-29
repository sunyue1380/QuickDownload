package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
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
    protected void mergeSubFileList(DownloadHolder downloadHolder, CountDownLatch countDownLatch) throws IOException, InterruptedException {
        int downloadTimeoutMillis = downloadHolder.downloadTask.downloadTimeoutMillis==3600000?downloadHolder.downloadTask.downloadTimeoutMillis:downloadHolder.poolConfig.downloadTimeoutMillis;
        try {
            if(!countDownLatch.await(downloadTimeoutMillis, TimeUnit.MILLISECONDS)){
                logger.warn("分段文件下载时间超过阈值,下载失败!下载时间阈值:{}毫秒,保存路径:{}", downloadTimeoutMillis, downloadHolder.file);
                return;
            }
        } catch (InterruptedException e) {
            logger.debug("等待合并文件时发生线程中断,停止下载文件");
            if(null!=downloadThreadFutures){
                for(Future future:downloadThreadFutures){
                    future.cancel(true);
                }
            }
            throw e;
        }

        long mergeFileSize = 0;
        for(Path subFile:downloadHolder.downloadProgress.subFileList){
            if(Files.notExists(subFile)){
                logger.warn("部分分段文件不存在无法合并文件,分段文件路径:{}", subFile);
                return;
            }
            mergeFileSize += Files.size(subFile);
        }
        if(!downloadHolder.downloadTask.m3u8){
            //检查合并后文件大小是否相同
            long contentLength = downloadHolder.response.contentLength();
            if(null==downloadHolder.response.contentEncoding()&&contentLength>0&&mergeFileSize!=contentLength){
                logger.warn("合并后文件大小不匹配,预期文件大小:{},实际合并文件大小:{}", contentLength,mergeFileSize);
                return;
            }
        }

        Path file = downloadHolder.file;
        if(!file.toFile().getParentFile().exists()){
            Files.createDirectories(file.getParent());
        }
        Files.deleteIfExists(file);
        Files.createFile(file);
        try (FileChannel fileChannel = FileChannel.open(file, StandardOpenOption.APPEND);){
            long currentSize = 0;
            for(Path subFile:downloadHolder.downloadProgress.subFileList){
                try (SeekableByteChannel subFileChannel = Files.newByteChannel(subFile);){
                    fileChannel.transferFrom(subFileChannel, currentSize, Files.size(subFile));
                }
                currentSize += Files.size(subFile);
            }
            fileChannel.close();
            logger.info("合并文件完成,大小:{},合并文件路径:{}", Files.size(downloadHolder.file),downloadHolder.file);
        }

    }
}
