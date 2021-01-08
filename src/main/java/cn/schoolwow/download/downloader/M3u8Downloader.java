package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.download.domain.m3u8.MediaPlaylist;
import cn.schoolwow.download.domain.m3u8.tag.SEGMENT;
import cn.schoolwow.download.util.QuickDownloadUtil;
import cn.schoolwow.quickhttp.response.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**m3u8格式视频下载*/
public class M3u8Downloader extends AbstractDownloader{

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException {
        downloadHolder.downloadProgress.m3u8 = true;
        int maxThreadConnection = downloadHolder.downloadTask.maxThreadConnection>0?downloadHolder.downloadTask.maxThreadConnection:downloadHolder.downloadPoolConfig.maxThreadConnection;

        MediaPlaylist mediaPlaylist = getMediaPlaylist(downloadHolder.response);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreadConnection);
        threadPoolExecutor.setThreadFactory(threadFactory);
        CountDownLatch countDownLatch = new CountDownLatch(maxThreadConnection);
        int per = mediaPlaylist.segmentList.size()/maxThreadConnection;
        downloadHolder.downloadProgress.subFileList = new Path[mediaPlaylist.segmentList.size()];
        for(int i=0;i<mediaPlaylist.segmentList.size();i++){
            downloadHolder.downloadProgress.subFileList[i] = Paths.get(downloadHolder.downloadPoolConfig.temporaryDirectoryPath + File.separator + "["+i+"]." +mediaPlaylist.response.contentLength() + "." + downloadHolder.file.getFileName().toString()+".ts");
        }
        for(int i=0;i<maxThreadConnection;i++){
            final int start = i*per;
            final int end = (i==maxThreadConnection-1)?mediaPlaylist.segmentList.size()-1:((i+1)*per-1);

            threadPoolExecutor.execute(()->{
                for(int j=start;j<=end;j++){
                    Path subFilePath = downloadHolder.downloadProgress.subFileList[j];
                    if(Files.exists(subFilePath)){
                        continue;
                    }
                    try {
                        Response subResponse = downloadHolder.downloadTask.connection.clone()
                                .url(mediaPlaylist.segmentList.get(j).URI)
                                .retryTimes(3)
                                .execute();
                        downloadHolder.downloadProgress.totalFileSize += subResponse.contentLength();
                        long estimateTotalSize = downloadHolder.downloadProgress.totalFileSize/downloadHolder.downloadProgress.subFileList.length*mediaPlaylist.segmentList.size();
                        downloadHolder.downloadProgress.totalFileSizeFormat = String.format("%d(%.2fMB)",mediaPlaylist.segmentList.size(),estimateTotalSize/1.0/1024/1024);
                        subResponse.bodyAsFile(subFilePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                countDownLatch.countDown();
            });
        }
        mergeSubFileList(downloadHolder,countDownLatch);
    }

    private MediaPlaylist getMediaPlaylist(Response response) throws IOException {
        Path tempPath = Files.createTempFile("QuickDownload",".m3u8");
        response.bodyAsFile(tempPath);
        Iterator<String> iterable = Files.lines(tempPath).iterator();
        Files.deleteIfExists(tempPath);
        MediaPlaylist mediaPlaylist = QuickDownloadUtil.getMediaPlaylist(iterable);
        String relativePath = response.url().substring(0,response.url().lastIndexOf("/")+1);
        for(SEGMENT segment:mediaPlaylist.segmentList){
            if(!segment.URI.startsWith("http")){
                segment.URI = relativePath + segment.URI;
            }
        }
        mediaPlaylist.response = response;
        return mediaPlaylist;
    }
}
