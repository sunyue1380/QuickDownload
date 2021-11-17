package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.quickhttp.domain.LogLevel;
import cn.schoolwow.quickhttp.response.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**多线程下载*/
public class MultiThreadDownloader extends AbstractDownloader{
    @Override
    public void download(DownloadHolder downloadHolder) throws IOException {
        downloadHolder.response.disconnect();
        int maxDownloadSpeed = downloadHolder.downloadTask.maxDownloadSpeed>0?downloadHolder.downloadTask.maxDownloadSpeed:downloadHolder.poolConfig.maxDownloadSpeed;
        int maxThreadConnection = downloadHolder.poolConfig.maxThreadConnection;
        CountDownLatch countDownLatch = new CountDownLatch(maxThreadConnection);
        long contentLength = downloadHolder.response.contentLength();
        downloadHolder.log(LogLevel.INFO,"[多线程下载]总大小:{},保存路径:{}",contentLength,downloadHolder.file);
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
                Path tempLogFilePath = null;
                try {
                    if (!Files.exists(subFile)) {
                        Files.createFile(subFile);
                    }
                    if (Files.size(subFile) == expectSize) {
                        downloadHolder.log(LogLevel.INFO,"[分段文件已经存在且下载完成]大小:{},路径:{}", expectSize,subFile);
                        return;
                    }
                    downloadHolder.log(LogLevel.INFO,"[准备下载分段文件]路径:{}", subFile);

                    tempLogFilePath = Files.createTempFile("QuickHttp.","response");
                    int retryTimes = 1;
                    while (Files.size(subFile) < expectSize && retryTimes <= downloadHolder.poolConfig.retryTimes) {
                        subResponse = downloadHolder.downloadTask.request.clone()
                                .ranges(start + Files.size(subFile), end)
                                .logFilePath(tempLogFilePath.toString())
                                .execute();
                        if(null!=subResponse){
                            subResponse.maxDownloadSpeed(maxDownloadSpeed/maxThreadConnection).bodyAsFile(subFile);
                        }
                        if(Thread.currentThread().isInterrupted()){
                            return;
                        }
                        if(Files.size(subFile)<expectSize){
                            downloadHolder.log(LogLevel.WARN,"[分段文件下载不完整]预期大小:{},当前大小:{},路径:{}", expectSize, Files.size(subFile), subFile);
                        }
                        retryTimes++;
                    }
                    if(expectSize!=Files.size(subFile)){
                        downloadHolder.log(LogLevel.WARN,"[分段文件下载异常]预期大小:{},当前大小:{},路径:{}", expectSize, Files.size(subFile), subFile);
                    }else{
                        downloadHolder.log(LogLevel.DEBUG,"[分段文件下载完成]当前大小:{},路径:{}",expectSize,subFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    downloadHolder.log(LogLevel.WARN,"[多线程分段文件下载失败]路径:{}", subFile);
                    if(null!=subResponse){
                        downloadHolder.appendLog(subResponse.logFilePath());
                    }
                    synchronized (downloadHolder){
                        e.printStackTrace(downloadHolder.pw);
                    }
                } finally {
                    countDownLatch.countDown();
                    if(null!=tempLogFilePath){
                        try {
                            Files.deleteIfExists(tempLogFilePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        mergeSubFileList(downloadHolder,countDownLatch);
    }
}