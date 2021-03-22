package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.PoolConfig;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.quickhttp.response.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;

public class DownloadPoolConfigImpl implements DownloadPoolConfig{
    private PoolConfig poolConfig;

    public DownloadPoolConfigImpl(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    /**
     * 指定临时文件目录
     * @param temporaryDirectoryPath 临时文件目录
     * */
    public DownloadPoolConfig temporaryDirectoryPath(String temporaryDirectoryPath){
        poolConfig.temporaryDirectoryPath = temporaryDirectoryPath;
        return this;
    }

    /**
     * 下载失败重试次数(默认3次)
     * @param retryTimes 下载失败重试次数
     * */
    public DownloadPoolConfig retryTimes(int retryTimes){
        poolConfig.retryTimes = retryTimes;
        return this;
    }

    /**
     * 是否删除临时文件(默认删除)
     * @param deleteTemporaryFile 是否删除临时文件
     * */
    public DownloadPoolConfig deleteTemporaryFile(boolean deleteTemporaryFile){
        poolConfig.deleteTemporaryFile = deleteTemporaryFile;
        return this;
    }

    /**
     * 是否强制单线程下载
     * @param singleThread 是否强制单线程下载
     * */
    public DownloadPoolConfig singleThread(boolean singleThread){
        poolConfig.singleThread = singleThread;
        return this;
    }

    /**
     * 指定全局下载任务超时时间
     * @param downloadTimeoutMillis 下载任务超时时间(ms)
     * */
    public DownloadPoolConfig maxDownloadTimeout(int downloadTimeoutMillis){
        poolConfig.downloadTimeoutMillis = downloadTimeoutMillis;
        return this;
    }

    /**
     * 指定最大下载速度(kb/s)
     * @param maxDownloadSpeed 最大下载速度(kb/s)
     * */
    public DownloadPoolConfig maxDownloadSpeed(int maxDownloadSpeed){
        poolConfig.maxDownloadSpeed = maxDownloadSpeed;
        return this;
    }

    /**
     * 指定全局最大线程连接个数
     * @param maxThreadConnection 最大线程连接个数
     * */
    public DownloadPoolConfig maxThreadConnection(int maxThreadConnection){
        int parallelDownloadCount = poolConfig.threadPoolExecutor.getCorePoolSize();
        poolConfig.downloadThreadPoolExecutor.setCorePoolSize(parallelDownloadCount*maxThreadConnection);
        poolConfig.downloadThreadPoolExecutor.setMaximumPoolSize(parallelDownloadCount*maxThreadConnection);
        poolConfig.maxThreadConnection = maxThreadConnection;
        return this;
    }

    /**
     * 指定全局文件保存目录
     * @param directoryPath 文件保存目录
     * */
    public DownloadPoolConfig directoryPath(String directoryPath){
        Path path = Paths.get(directoryPath);
        if(Files.notExists(path)){
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        poolConfig.directoryPath = directoryPath;
        return this;
    }

    /**
     * 指定最大同时下载任务个数
     * @param parallelDownloadCount 最大同时下载任务个数
     * */
    public DownloadPoolConfig parallelDownloadCount(int parallelDownloadCount){
        poolConfig.threadPoolExecutor.setCorePoolSize(parallelDownloadCount);
        poolConfig.threadPoolExecutor.setMaximumPoolSize(parallelDownloadCount);
        poolConfig.downloadThreadPoolExecutor.setCorePoolSize(parallelDownloadCount*poolConfig.maxThreadConnection);
        poolConfig.downloadThreadPoolExecutor.setMaximumPoolSize(parallelDownloadCount*poolConfig.maxThreadConnection);
        return this;
    }

    /**
     * 指定全局文件完整性校验函数
     * @param fileIntegrityChecker 文件完整性校验函数
     * */
    public DownloadPoolConfig fileIntegrityChecker(BiFunction<Response, Path,Boolean> fileIntegrityChecker){
        poolConfig.fileIntegrityChecker = fileIntegrityChecker;
        return this;
    }

    /**
     * 指定线程池事件监听接口
     * @param downloadPoolListener 线程池事件监听接口
     * */
    public DownloadPoolConfig downloadPoolListener(DownloadPoolListener downloadPoolListener){
        poolConfig.downloadPoolListenerList.add(downloadPoolListener);
        return this;
    }

    @Override
    public PoolConfig getPoolConfig() {
        return this.poolConfig;
    }
}
