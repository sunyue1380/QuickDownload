package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadPoolConfig;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.download.pool.AbstractDownloadPool;
import cn.schoolwow.download.pool.DownloadPool;
import cn.schoolwow.quickhttp.response.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;

/**构建下载任务池*/
public class QuickDownload {
    private DownloadPoolConfig downloadPoolConfig = new DownloadPoolConfig();

    private QuickDownload(){

    }

    public static QuickDownload newInstance(){
        return new QuickDownload();
    }

    /**
     * 指定临时文件目录
     * @param temporaryDirectoryPath 临时文件目录
     * */
    public QuickDownload temporaryDirectoryPath(String temporaryDirectoryPath){
        downloadPoolConfig.temporaryDirectoryPath = temporaryDirectoryPath;
        return this;
    }

    /**
     * 下载失败重试次数(默认3次)
     * @param retryTimes 下载失败重试次数
     * */
    public QuickDownload retryTimes(int retryTimes){
        downloadPoolConfig.retryTimes = retryTimes;
        return this;
    }

    /**
     * 是否删除临时文件(默认删除)
     * @param deleteTemporaryFile 是否删除临时文件
     * */
    public QuickDownload deleteTemporaryFile(boolean deleteTemporaryFile){
        downloadPoolConfig.deleteTemporaryFile = deleteTemporaryFile;
        return this;
    }

    /**
     * 是否强制单线程下载
     * @param singleThread 是否强制单线程下载
     * */
    public QuickDownload singleThread(boolean singleThread){
        downloadPoolConfig.singleThread = singleThread;
        return this;
    }

    /**
     * 指定全局下载任务连接超时时间
     * @param connectTimeoutMillis 下载任务连接超时时间(ms)
     * */
    public QuickDownload connectTimeoutMillis(int connectTimeoutMillis){
        downloadPoolConfig.connectTimeoutMillis = connectTimeoutMillis;
        return this;
    }

    /**
     * 指定全局下载任务读取超时时间
     * @param readTimeoutMillis 下载任务读取超时时间(ms)
     * */
    public QuickDownload readTimeoutMillis(int readTimeoutMillis){
        downloadPoolConfig.readTimeoutMillis = readTimeoutMillis;
        return this;
    }

    /**
     * 指定全局下载任务超时时间
     * @param downloadTimeoutMillis 下载任务超时时间(ms)
     * */
    public QuickDownload maxDownloadTimeout(int downloadTimeoutMillis){
        downloadPoolConfig.downloadTimeoutMillis = downloadTimeoutMillis;
        return this;
    }

    /**
     * 指定最大下载速度(kb/s)
     * @param maxDownloadSpeed 最大下载速度(kb/s)
     * */
    public QuickDownload maxDownloadSpeed(int maxDownloadSpeed){
        downloadPoolConfig.maxDownloadSpeed = maxDownloadSpeed;
        return this;
    }

    /**
     * 指定全局最大线程连接个数
     * @param maxThreadConnection 最大线程连接个数
     * */
    public QuickDownload maxThreadConnection(int maxThreadConnection){
        downloadPoolConfig.maxThreadConnection = maxThreadConnection;
        return this;
    }

    /**
     * 指定全局文件保存目录
     * @param directoryPath 文件保存目录
     * */
    public QuickDownload directoryPath(String directoryPath){
        downloadPoolConfig.directoryPath = directoryPath;
        return this;
    }

    /**
     * 指定最大同时下载任务个数
     * @param parallelDownloadCount 最大同时下载任务个数
     * */
    public QuickDownload parallelDownloadCount(int parallelDownloadCount){
        downloadPoolConfig.threadPoolExecutor.setCorePoolSize(parallelDownloadCount);
        return this;
    }

    /**
     * 指定全局文件完整性校验函数
     * @param fileIntegrityChecker 文件完整性校验函数
     * */
    public QuickDownload fileIntegrityChecker(BiFunction<Response, Path,Boolean> fileIntegrityChecker){
        downloadPoolConfig.fileIntegrityChecker = fileIntegrityChecker;
        return this;
    }

    /**
     * 指定线程池事件监听接口
     * @param downloadPoolListener 线程池事件监听接口
     * */
    public QuickDownload downloadPoolListener(DownloadPoolListener downloadPoolListener){
        downloadPoolConfig.downloadPoolListenerList.add(downloadPoolListener);
        return this;
    }

    public DownloadPool build() throws IOException {
        //检查临时文件目录是否合法
        {
            Path path = Paths.get(downloadPoolConfig.temporaryDirectoryPath);
            if(Files.notExists(path)){
                Files.createDirectories(path);
            }
        }
        //检查全局保存目录是否合法
        if(null!=downloadPoolConfig.directoryPath){
            Path path = Paths.get(downloadPoolConfig.directoryPath);
            if(Files.notExists(path)){
                Files.createDirectories(path);
            }
        }
        return new AbstractDownloadPool(downloadPoolConfig);
    }
}
