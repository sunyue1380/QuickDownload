package cn.schoolwow.download.downloader;

import cn.schoolwow.download.domain.DownloadHolder;

import java.io.IOException;

/**下载类*/
public interface Downloader {
    /**
     * 执行下载任务
     * @param downloadHolder 待下载任务
     * */
    void download(DownloadHolder downloadHolder) throws IOException, InterruptedException;
}
