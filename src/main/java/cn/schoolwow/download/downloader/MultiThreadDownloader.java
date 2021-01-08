package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.quickhttp.response.Response;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**多线程下载*/
public class MultiThreadDownloader extends AbstractDownloader{
    private Logger logger = LoggerFactory.getLogger(MultiThreadDownloader.class);

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException {
        int maxDownloadSpeed = downloadHolder.downloadTask.maxDownloadSpeed>0?downloadHolder.downloadTask.maxDownloadSpeed:downloadHolder.downloadPoolConfig.maxDownloadSpeed;
        int maxThreadConnection = downloadHolder.downloadTask.maxThreadConnection==Runtime.getRuntime().availableProcessors()*2?downloadHolder.downloadTask.maxThreadConnection:downloadHolder.downloadPoolConfig.maxThreadConnection;
        CountDownLatch countDownLatch = new CountDownLatch(maxThreadConnection);
        long contentLength = downloadHolder.response.contentLength();
        long per = contentLength / maxThreadConnection;
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreadConnection);
        threadPoolExecutor.setThreadFactory(threadFactory);
        downloadHolder.downloadProgress.subFileList = new Path[maxThreadConnection];
        for (int i = 0; i < maxThreadConnection; i++) {
            final long start = i * per;
            final long end = (i == maxThreadConnection - 1) ? contentLength - 1 : ((i + 1) * per - 1);
            final long expectSize = (end-start+1);
            final Path subFile = Paths.get(downloadHolder.downloadPoolConfig.temporaryDirectoryPath + File.separator + "[" + i + "]." + contentLength + "." + downloadHolder.file.getFileName().toString());
            downloadHolder.downloadProgress.subFileList[i] = subFile;
            threadPoolExecutor.execute(() -> {
                try {
                    if (!Files.exists(subFile)) {
                        Files.createFile(subFile);
                    }
                    if (Files.size(subFile) >= expectSize) {
                        logger.debug("[当前分段已经下载完毕]预期大小:{},实际大小:{},分段文件路径:{}",
                                String.format("%.2fMB", expectSize / 1.0 / 1024 / 1024),
                                String.format("%.2fMB", Files.size(subFile) / 1.0 / 1024 / 1024),
                                subFile);
                        return;
                    }
                    int retryTimes = QuickHttpConfig.retryTimes;
                    while (Files.size(subFile) < expectSize && retryTimes >= 0) {
                        Response subResponse = downloadHolder.downloadTask.connection.clone()
                                .ranges(start + Files.size(subFile), end)
                                .execute();
                        subResponse.maxDownloadSpeed(maxDownloadSpeed/maxThreadConnection).bodyAsFile(subFile);
                        retryTimes--;
                    }
                    logger.debug("[分段文件下载完毕]预期大小:{},当前大小:{},路径:{}",
                            String.format("%.2fMB", expectSize / 1.0 / 1024 / 1024),
                            String.format("%.2fMB", Files.size(subFile) / 1.0 / 1024 / 1024),
                            subFile
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        mergeSubFileList(downloadHolder,countDownLatch);
    }
}
