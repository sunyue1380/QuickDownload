package cn.schoolwow.download.domain;

import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.quickhttp.response.Response;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;

/**下载池配置项*/
public class DownloadPoolConfig {
    /**下载失败重试次数*/
    public int retryTimes = 3;

    /**是否删除临时文件*/
    public boolean deleteTemporaryFile = true;

    /**是否强制单线程下载*/
    public boolean singleThread;

    /**连接超时时间*/
    public int connectTimeoutMillis;

    /**读取超时时间*/
    public int readTimeoutMillis;

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

    /**下载任务调度线程池*/
    public ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
}
