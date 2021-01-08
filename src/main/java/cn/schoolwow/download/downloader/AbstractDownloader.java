package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractDownloader implements Downloader{
    /**线程工厂*/
    protected static ThreadFactory threadFactory = new MultiThreadDownloader.DefaultThreadFactory();

    /**
     * 合并分段文件
     * @param downloadHolder 下载任务
     * */
    protected void mergeSubFileList(DownloadHolder downloadHolder, CountDownLatch countDownLatch) throws IOException {
        int downloadTimeoutMillis = downloadHolder.downloadTask.downloadTimeoutMillis==3600?downloadHolder.downloadTask.downloadTimeoutMillis:downloadHolder.downloadPoolConfig.downloadTimeoutMillis;
        try {
            countDownLatch.await(downloadTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "downloadPool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
