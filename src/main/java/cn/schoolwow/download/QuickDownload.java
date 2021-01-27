package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadProgress;
import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.download.pool.DownloadPool;
import cn.schoolwow.download.pool.DownloadPoolConfig;
import cn.schoolwow.download.pool.DownloadPoolImpl;

import java.io.IOException;
import java.util.List;

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
     * @param downloadTask 下载任务
     * */
    public static void download(DownloadTask downloadTask) throws IOException{
        downloadPool.download(downloadTask);
    }

    /**
     * 新建下载池
     */
    public static DownloadPool newDownloadPool() {
        return new DownloadPoolImpl();
    }
}
