package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**单线程下载*/
public class SingleThreadDownloader extends AbstractDownloader{

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException {
        Path tempFilePath = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath+"/"+System.currentTimeMillis()+"_"+downloadHolder.file.getFileName().toString());
        downloadHolder.downloadProgress.subFileList = new Path[1];
        downloadHolder.downloadProgress.subFileList[0] = tempFilePath;
        int maxDownloadSpeed = downloadHolder.downloadTask.maxDownloadSpeed>0?downloadHolder.downloadTask.maxDownloadSpeed:downloadHolder.poolConfig.maxDownloadSpeed;
        downloadHolder.response.maxDownloadSpeed(maxDownloadSpeed).bodyAsFile(tempFilePath);
        Files.copy(tempFilePath,downloadHolder.file, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tempFilePath);
    }
}
