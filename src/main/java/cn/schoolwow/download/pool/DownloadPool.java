package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.DownloadProgress;
import cn.schoolwow.download.domain.DownloadRecord;
import cn.schoolwow.download.domain.DownloadTask;

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
     * @return 下载任务令牌(可用于中断任务等)
     * */
    void download(DownloadTask... downloadTasks);

    /**
     * 下载任务
     * @param downloadFinished 指定下载任务列表完成后执行
     * @param downloadTasks 下载任务
     * */
    void download(Consumer<Path[]> downloadFinished, DownloadTask... downloadTasks);

    /**
     * 获取当前所有下载记录
     * */
    List<DownloadRecord> getDownloadRecordList();
}
