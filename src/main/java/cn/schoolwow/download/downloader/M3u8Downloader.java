package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.download.util.M3u8Util;
import cn.schoolwow.quickhttp.domain.m3u8.M3u8Type;
import cn.schoolwow.quickhttp.domain.m3u8.MediaPlaylist;
import cn.schoolwow.quickhttp.domain.m3u8.tag.SEGMENT;
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

/**m3u8格式视频下载*/
public class M3u8Downloader extends AbstractDownloader{
    private Logger logger = LoggerFactory.getLogger(M3u8Downloader.class);

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException, InterruptedException {
        downloadHolder.downloadProgress.m3u8 = true;
        String url = downloadHolder.response.url();
        if(!M3u8Type.MediaPlayList.equals(M3u8Util.getM3u8Type(downloadHolder.response.body()))){
            logger.warn("m3u8地址不是媒体播放列表,地址:{}", url);
            return;
        }

        MediaPlaylist mediaPlaylist = M3u8Util.getMediaPlaylist(downloadHolder.response.body());
        logger.info("下载方式为m3u8,总分段个数:{},保存路径:{}", mediaPlaylist.segmentList, downloadHolder.file);
        //补充相对路径
        {
            String relativePath = url.substring(0,url.lastIndexOf('/')+1);
            for(SEGMENT segment2:mediaPlaylist.segmentList){
                if(!segment2.URI.startsWith("http")){
                    segment2.URI = relativePath + segment2.URI;
                }
            }
        }
        int maxThreadConnection = downloadHolder.poolConfig.maxThreadConnection;
        CountDownLatch countDownLatch = new CountDownLatch(maxThreadConnection);
        int per = mediaPlaylist.segmentList.size()/maxThreadConnection;
        downloadHolder.downloadProgress.subFileList = new Path[mediaPlaylist.segmentList.size()];
        for(int i=0;i<mediaPlaylist.segmentList.size();i++){
            downloadHolder.downloadProgress.subFileList[i] = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath + File.separator + "["+i+"]." +downloadHolder.response.contentLength() + "." + downloadHolder.file.getFileName().toString()+".ts");
        }
        downloadHolder.response.disconnect();
        super.downloadThreadFutures = new Future[maxThreadConnection];
        for(int i=0;i<maxThreadConnection;i++){
            final int start = i*per;
            final int end = (i==maxThreadConnection-1)?mediaPlaylist.segmentList.size()-1:((i+1)*per-1);

            super.downloadThreadFutures[i] = downloadHolder.poolConfig.downloadThreadPoolExecutor.submit(()->{
                for(int j=start;j<=end;j++){
                    Path subFilePath = downloadHolder.downloadProgress.subFileList[j];
                    if(subFilePath.toFile().exists()){
                        logger.debug("分段文件已存在,路径:{}", subFilePath);
                        continue;
                    }
                    try {
                        logger.trace("准备下载分段文件,路径:{}", subFilePath);
                        Response subResponse = downloadHolder.downloadTask.request.clone()
                                .url(mediaPlaylist.segmentList.get(j).URI)
                                .retryTimes(3)
                                .execute();
                        if(Thread.currentThread().isInterrupted()){
                            return;
                        }
                        downloadHolder.downloadProgress.totalFileSize += subResponse.contentLength();
                        long estimateTotalSize = downloadHolder.downloadProgress.totalFileSize/downloadHolder.downloadProgress.subFileList.length*mediaPlaylist.segmentList.size();
                        downloadHolder.downloadProgress.totalFileSizeFormat = String.format("%d(%.2fMB)",mediaPlaylist.segmentList.size(),estimateTotalSize/1.0/1024/1024);
                        subResponse.bodyAsFile(subFilePath);
                        logger.debug("m3u8分段文件下载完成,大小:{},路径:{}", Files.size(subFilePath),subFilePath);
                    } catch (IOException e) {
                        logger.error("m3u8分段文件下载失败,分段文件路径:{}", subFilePath, e);
                    }
                }
                countDownLatch.countDown();
            });
        }
        mergeSubFileList(downloadHolder,countDownLatch);
    }
}