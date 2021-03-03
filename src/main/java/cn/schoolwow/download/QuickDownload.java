package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadProgress;
import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.download.pool.DownloadPool;
import cn.schoolwow.download.pool.DownloadPoolConfig;
import cn.schoolwow.download.pool.DownloadPoolImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**构建下载任务池*/
public class QuickDownload {
    /**
     * 默认QuickHttp客户端
     */
    private static DownloadPool downloadPool = new DownloadPoolImpl();

    /**
     * 获取下载进度列表
     * */
    public static DownloadPoolConfig downloadPoolConfig(){
        return downloadPool.downloadPoolConfig();
    }

    /**
     * 获取下载进度列表
     * */
    public static List<DownloadProgress> getProgressList(){
        return downloadPool.getProgressList();
    }

    /**
     * 打印下载进度表
     * */
    public static void printDownloadProgress(){
        downloadPool.printDownloadProgress();
    }

    /**
     * 下载任务
     * @param downloadTasks 下载任务
     * */
    public static void download(DownloadTask... downloadTasks) throws IOException{
        downloadPool.download(downloadTasks);
    }

    /**
     * 下载任务
     * @param downloadFinished 指定下载任务列表完成后执行
     * @param downloadTasks 下载任务
     * */
    public static void download(Consumer<Path[]> downloadFinished, DownloadTask... downloadTasks) throws IOException{
        downloadPool.download(downloadFinished,downloadTasks);
    }

    /**
     * 新建下载池
     */
    public static DownloadPool newDownloadPool() {
        return new DownloadPoolImpl();
    }
}
