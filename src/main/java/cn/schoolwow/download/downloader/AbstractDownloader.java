package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDownloader implements Downloader{
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
        for(Path subFile:downloadHolder.downloadProgress.subFileList){
            if(!Files.isReadable(subFile)){
                throw new IOException("文件合并失败,分段文件无法访问!路径:"+subFile.toString());
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
    }
}
