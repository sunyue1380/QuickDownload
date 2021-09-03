package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.quickhttp.domain.m3u8.M3u8Type;
import cn.schoolwow.quickhttp.domain.m3u8.MediaPlaylist;
import cn.schoolwow.quickhttp.response.Response;
import cn.schoolwow.quickhttp.util.M3u8Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**m3u8格式视频下载*/
public class M3u8Downloader extends AbstractDownloader{
    private Logger logger = LoggerFactory.getLogger(M3u8Downloader.class);

    @Override
    public void download(DownloadHolder downloadHolder) throws Exception {
        downloadHolder.downloadProgress.m3u8 = true;
        int maxThreadConnection = downloadHolder.poolConfig.maxThreadConnection;
        if(!M3u8Type.MediaPlayList.equals(M3u8Util.getM3u8Type(downloadHolder.response.body()))){
            throw new IllegalArgumentException("m3u8地址不是媒体播放列表!m3u8地址:"+downloadHolder.response.url());
        }

        MediaPlaylist mediaPlaylist = M3u8Util.getMediaPlaylist(downloadHolder.response.url(),downloadHolder.response.body());
        CountDownLatch countDownLatch = new CountDownLatch(maxThreadConnection);
        int per = mediaPlaylist.segmentList.size()/maxThreadConnection;
        downloadHolder.downloadProgress.subFileList = new Path[mediaPlaylist.segmentList.size()];
        for(int i=0;i<mediaPlaylist.segmentList.size();i++){
            downloadHolder.downloadProgress.subFileList[i] = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath + File.separator + "["+i+"]." +downloadHolder.response.contentLength() + "." + downloadHolder.file.getFileName().toString()+".ts");
        }
        for(int i=0;i<maxThreadConnection;i++){
            final int start = i*per;
            final int end = (i==maxThreadConnection-1)?mediaPlaylist.segmentList.size()-1:((i+1)*per-1);

            downloadHolder.poolConfig.downloadThreadPoolExecutor.execute(()->{
                for(int j=start;j<=end;j++){
                    Path subFilePath = downloadHolder.downloadProgress.subFileList[j];
                    if(Files.exists(subFilePath)){
                        logger.debug("[分段文件已存在]路径:{}",subFilePath);
                        continue;
                    }
                    try {
                        Response subResponse = downloadHolder.downloadTask.request.clone()
                                .url(mediaPlaylist.segmentList.get(j).URI)
                                .retryTimes(3)
                                .execute();
                        downloadHolder.downloadProgress.totalFileSize += subResponse.contentLength();
                        long estimateTotalSize = downloadHolder.downloadProgress.totalFileSize/downloadHolder.downloadProgress.subFileList.length*mediaPlaylist.segmentList.size();
                        downloadHolder.downloadProgress.totalFileSizeFormat = String.format("%d(%.2fMB)",mediaPlaylist.segmentList.size(),estimateTotalSize/1.0/1024/1024);
                        subResponse.bodyAsFile(subFilePath);
                        logger.debug("[m3u8分段文件下载完成]大小:{},路径:{}",Files.size(subFilePath),subFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                countDownLatch.countDown();
            });
        }
        mergeSubFileList(downloadHolder,countDownLatch);
    }
}
