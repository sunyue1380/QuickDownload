package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.DownloadProgress;
import cn.schoolwow.download.domain.DownloadTask;

import java.io.IOException;
import java.util.List;

/**下载线程池接口*/
public interface DownloadPool {
    /**
     * 获取下载进度列表
     * */
    DownloadPoolConfig downloadPoolConfig();

    /**
     * 获取下载进度列表
     * */
    List<DownloadProgress> getProgressList();

    /**
     * 打印下载进度表
     * */
    void printDownloadProgress();

    /**
     * 下载任务
     * @param downloadTasks 下载任务
     * */
    void download(DownloadTask... downloadTasks) throws IOException;
}
