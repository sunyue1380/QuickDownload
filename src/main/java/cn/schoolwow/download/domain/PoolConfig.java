package cn.schoolwow.download.domain;

import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.quickhttp.response.Response;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**下载池配置项*/
public class PoolConfig {
    /**下载失败重试次数*/
    public int retryTimes = 3;

    /**是否删除临时文件*/
    public boolean deleteTemporaryFile = true;

    /**是否强制单线程下载*/
    public boolean singleThread;

    /**下载超时时间*/
    public int downloadTimeoutMillis = 3600000;

    /**最大线程连接个数*/
    public int maxThreadConnection = Runtime.getRuntime().availableProcessors()*2;

    /**最大下载速度(kb/s)*/
    public int maxDownloadSpeed;

    /**临时文件目录*/
    public String temporaryDirectoryPath = System.getProperty("java.io.tmpdir")+ File.separator+"DownloadPool";

    /**全局保存目录*/
    public String directoryPath;

    /**全局文件完整性校验函数*/
    public BiFunction<Response, Path,Boolean> fileIntegrityChecker;

    /**线程池事件监听接口*/
    public List<DownloadPoolListener> downloadPoolListenerList = new ArrayList<>();

    /**下载日志文件夹路径*/
    public String logDirectoryPath;

    /**下载任务调度线程池*/
    public ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            1,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            new NamedThreadFactory("quickdownload-dispatch")
    );
    {
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    /**实际http下载线程池*/
    public ThreadPoolExecutor downloadThreadPoolExecutor = new ThreadPoolExecutor(
            threadPoolExecutor.getCorePoolSize()*maxThreadConnection,
            threadPoolExecutor.getCorePoolSize()*maxThreadConnection,
            1,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            new NamedThreadFactory("quickdownload-download")
    );
    {
        downloadThreadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    /**批量任务下载完成后处理线程池*/
    public ThreadPoolExecutor batchDownloadTaskThreadPoolExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors()*2,
            Runtime.getRuntime().availableProcessors()*2,
            1,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>()
    );
    {
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

}
