package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.DownloadProgress;
import cn.schoolwow.download.domain.DownloadTask;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

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
    void download(DownloadTask... downloadTasks);

    /**
     * 下载任务
     * @param downloadFinished 指定下载任务列表完成后执行
     * @param downloadTasks 下载任务
     * */
    void download(Consumer<Path[]> downloadFinished, DownloadTask... downloadTasks);
}
