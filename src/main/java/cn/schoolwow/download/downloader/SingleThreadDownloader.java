package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**单线程下载*/
public class SingleThreadDownloader extends AbstractDownloader{
    private Logger logger = LoggerFactory.getLogger(SingleThreadDownloader.class);

    @Override
    public void download(DownloadHolder downloadHolder) throws IOException {
        long contentLength = downloadHolder.response.contentLength();
        logger.info("下载方式为单线程下载,文件大小:{},保存路径:{}", contentLength, downloadHolder.file);

        Path tempFilePath = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath+"/"+System.currentTimeMillis()+"_"+downloadHolder.file.getFileName().toString());
        downloadHolder.downloadProgress.subFileList = new Path[1];
        downloadHolder.downloadProgress.subFileList[0] = tempFilePath;
        int maxDownloadSpeed = downloadHolder.downloadTask.maxDownloadSpeed>0?downloadHolder.downloadTask.maxDownloadSpeed:downloadHolder.poolConfig.maxDownloadSpeed;
        logger.debug("准备下载临时文件,路径:{}", tempFilePath);
        downloadHolder.response.maxDownloadSpeed(maxDownloadSpeed).bodyAsFile(tempFilePath);
        logger.debug("准备复制文件,临时文件大小:{},原路径:{},目标路径:{}", tempFilePath.toFile().length(), tempFilePath, downloadHolder.file);
        Files.copy(tempFilePath,downloadHolder.file, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tempFilePath);
        logger.info("文件下载完毕,大小:{},路径:{}", downloadHolder.file.toFile().length(),downloadHolder.file);
        downloadHolder.response.disconnect();
    }
}