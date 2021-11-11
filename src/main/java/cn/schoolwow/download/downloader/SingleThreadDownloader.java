package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.quickhttp.domain.LogLevel;

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
        downloadHolder.log(LogLevel.INFO,"[准备下载文件]临时文件保存路径:{}", tempFilePath);
        downloadHolder.response.maxDownloadSpeed(maxDownloadSpeed).bodyAsFile(tempFilePath);
        downloadHolder.log(LogLevel.INFO,"[临时文件下载完毕]临时文件大小:{}", Files.size(tempFilePath));

        long contentLength = downloadHolder.response.contentLength();
        if(null==downloadHolder.response.contentEncoding()&&contentLength>0&&contentLength!=Files.size(tempFilePath)){
            downloadHolder.appendLog(downloadHolder.response.logFilePath());
            return;
        }
        downloadHolder.log(LogLevel.INFO,"[复制文件]原路径:{},目标路径:{}", tempFilePath, downloadHolder.file);
        Files.copy(tempFilePath,downloadHolder.file, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tempFilePath);
        downloadHolder.log(LogLevel.INFO,"[文件下载完毕]大小:{},路径:{}",Files.size(downloadHolder.file),downloadHolder.file);
    }
}