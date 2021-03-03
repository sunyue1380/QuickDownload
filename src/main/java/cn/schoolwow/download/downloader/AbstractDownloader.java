package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDownloader implements Downloader{
    private static Logger logger = LoggerFactory.getLogger(AbstractDownloader.class);
    /**
     * 合并分段文件
     * @param downloadHolder 下载任务
     * */
    protected void mergeSubFileList(DownloadHolder downloadHolder, CountDownLatch countDownLatch) throws IOException {
        int downloadTimeoutMillis = downloadHolder.downloadTask.downloadTimeoutMillis==3600000?downloadHolder.downloadTask.downloadTimeoutMillis:downloadHolder.poolConfig.downloadTimeoutMillis;
        try {
            countDownLatch.await(downloadTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //检查是否可以合并
        long mergeFileSize = 0;
        for(Path subFile:downloadHolder.downloadProgress.subFileList){
            if(!Files.isReadable(subFile)){
                throw new IOException("文件合并失败,分段文件无法访问!路径:"+subFile.toString());
            }
            mergeFileSize += Files.size(subFile);
        }
        //检查合并后文件大小是否相同
        long contentLength = downloadHolder.response.contentLength();
        if(contentLength>0&&mergeFileSize!=contentLength){
            logger.warn("[文件大小不匹配]预期文件大小:{},实际合并文件大小:{}",contentLength,mergeFileSize);
            return;
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
    }
}
