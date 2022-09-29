package cn.schoolwow.download.domain;

import cn.schoolwow.download.downloader.Downloader;
import cn.schoolwow.download.downloader.M3u8Downloader;
import cn.schoolwow.download.downloader.MultiThreadDownloader;
import cn.schoolwow.download.downloader.SingleThreadDownloader;

import java.io.IOException;

/**下载器枚举类*/
public enum DownloaderEnum {
    SingleThread(new SingleThreadDownloader()),
    MultiThread(new MultiThreadDownloader()),
    M3u8(new M3u8Downloader());

    private Downloader downloader;

    DownloaderEnum(Downloader downloader) {
        this.downloader = downloader;
    }

    public void download(DownloadHolder downloadHolder) throws IOException, InterruptedException {
        downloader.download(downloadHolder);
    }
}
