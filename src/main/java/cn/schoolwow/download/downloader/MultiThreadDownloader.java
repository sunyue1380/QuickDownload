package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.quickhttp.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**多线程下载*/
public class MultiThreadDownloader extends AbstractDownloader{
    private Logger logger = LoggerFactory.getLogger(MultiThreadDownloader.class);

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException, InterruptedException {
        downloadHolder.response.disconnect();
        int maxDownloadSpeed = downloadHolder.downloadTask.maxDownloadSpeed>0?downloadHolder.downloadTask.maxDownloadSpeed:downloadHolder.poolConfig.maxDownloadSpeed;
        int maxThreadConnection = downloadHolder.poolConfig.maxThreadConnection;
        CountDownLatch countDownLatch = new CountDownLatch(maxThreadConnection);
        long contentLength = downloadHolder.response.contentLength();
        logger.info("下载方式为多线程下载,文件大小:{},保存路径:{}", contentLength, downloadHolder.file);

        long per = contentLength / maxThreadConnection;
        downloadHolder.downloadProgress.subFileList = new Path[maxThreadConnection];
        super.downloadThreadFutures = new Future[maxThreadConnection];
        for (int i = 0; i < maxThreadConnection; i++) {
            final long start = i * per;
            final long end = (i == maxThreadConnection - 1) ? contentLength - 1 : ((i + 1) * per - 1);
            final long expectSize = (end-start+1);
            final Path subFile = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath + File.separator + "[" + i + "]." + contentLength + "." + downloadHolder.file.getFileName().toString());
            downloadHolder.downloadProgress.subFileList[i] = subFile;
            super.downloadThreadFutures[i] = downloadHolder.poolConfig.downloadThreadPoolExecutor.submit(() -> {
                Response subResponse = null;
                try {
                    if (!Files.exists(subFile)) {
                        Files.createFile(subFile);
                    }
                    if (Files.size(subFile) == expectSize) {
                        logger.debug("分段文件存在且已下载完毕,大小:{},路径:{}", expectSize, subFile);
                        return;
                    }
                    logger.trace("准备下载分段文件,路径:{}", subFile);

                    int retryTimes = 1;
                    while (Files.size(subFile) < expectSize && retryTimes <= downloadHolder.poolConfig.retryTimes) {
                        subResponse = downloadHolder.downloadTask.request.clone()
                                .ranges(start + Files.size(subFile), end)
                                .execute();
                        if(null!=subResponse){
                            subResponse.maxDownloadSpeed(maxDownloadSpeed/maxThreadConnection).bodyAsFile(subFile);
                        }
                        if(Thread.currentThread().isInterrupted()){
                            return;
                        }
                        if(Files.size(subFile)<expectSize){
                            logger.debug("分段文件下载不完整,预期大小:{}, 当前大小:{}", Files.size(subFile), subFile);
                        }
                        retryTimes++;
                    }
                    if(expectSize!=Files.size(subFile)){
                        logger.debug("分段文件下载异常,预期大小:{},当前大小:{},路径:{}", expectSize, Files.size(subFile), subFile);
                    }else{
                        logger.debug("分段文件下载完成,当前大小:{},路径:{}", Files.size(subFile), subFile);
                    }
                } catch (IOException e) {
                    logger.error("多线程下载分段文件失败,文件路径:{}", subFile, e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        mergeSubFileList(downloadHolder,countDownLatch);
    }
}