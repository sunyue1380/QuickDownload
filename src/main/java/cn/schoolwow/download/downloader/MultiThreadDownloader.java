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

/**多线程下载*/
public class MultiThreadDownloader extends AbstractDownloader{
    private Logger logger = LoggerFactory.getLogger(MultiThreadDownloader.class);

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException {
        int maxDownloadSpeed = downloadHolder.downloadTask.maxDownloadSpeed>0?downloadHolder.downloadTask.maxDownloadSpeed:downloadHolder.poolConfig.maxDownloadSpeed;
        int maxThreadConnection = downloadHolder.poolConfig.maxThreadConnection;
        CountDownLatch countDownLatch = new CountDownLatch(maxThreadConnection);
        long contentLength = downloadHolder.response.contentLength();
        long per = contentLength / maxThreadConnection;
        downloadHolder.downloadProgress.subFileList = new Path[maxThreadConnection];
        for (int i = 0; i < maxThreadConnection; i++) {
            final long start = i * per;
            final long end = (i == maxThreadConnection - 1) ? contentLength - 1 : ((i + 1) * per - 1);
            final long expectSize = (end-start+1);
            final Path subFile = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath + File.separator + "[" + i + "]." + contentLength + "." + downloadHolder.file.getFileName().toString());
            downloadHolder.downloadProgress.subFileList[i] = subFile;
            downloadHolder.poolConfig.downloadThreadPoolExecutor.execute(() -> {
                try {
                    if (!Files.exists(subFile)) {
                        Files.createFile(subFile);
                    }
                    if (Files.size(subFile) == expectSize) {
                        return;
                    }
                    int retryTimes = 1;
                    while (Files.size(subFile) < expectSize && retryTimes <= downloadHolder.poolConfig.retryTimes) {
                        Response subResponse = downloadHolder.downloadTask.request.clone()
                                .ranges(start + Files.size(subFile), end)
                                .execute();
                        if(null!=subResponse){
                            subResponse.maxDownloadSpeed(maxDownloadSpeed/maxThreadConnection).bodyAsFile(subFile);
                        }
                        retryTimes++;
                    }
                    if(expectSize!=Files.size(subFile)){
                        logger.warn("[分段文件下载异常]预期大小:{},当前大小:{},路径:{}",
                                expectSize,
                                Files.size(subFile),
                                subFile
                        );
                    }
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
