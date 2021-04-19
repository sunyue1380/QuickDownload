package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;

import java.io.IOException;
import java.nio.file.*;

/**单线程下载*/
public class SingleThreadDownloader extends AbstractDownloader{

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException {
        Path tempFilePath = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath+"/"+System.currentTimeMillis()+"_"+downloadHolder.file.getFileName().toString());
        downloadHolder.downloadProgress.subFileList = new Path[1];
        downloadHolder.downloadProgress.subFileList[0] = tempFilePath;
        if(downloadHolder.poolConfig.debug){
            int contentLength = (int) downloadHolder.response.contentLength();
            if(contentLength<=0){
                contentLength = 10485760;//10MB
            }
            byte[] bytes = new byte[contentLength];
            Files.write(tempFilePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try {
                Thread.sleep(1000+Math.round(Math.random()*2000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            int maxDownloadSpeed = downloadHolder.downloadTask.maxDownloadSpeed>0?downloadHolder.downloadTask.maxDownloadSpeed:downloadHolder.poolConfig.maxDownloadSpeed;
            downloadHolder.response.maxDownloadSpeed(maxDownloadSpeed).bodyAsFile(tempFilePath);
        }
        Files.copy(tempFilePath,downloadHolder.file, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tempFilePath);
    }
}
