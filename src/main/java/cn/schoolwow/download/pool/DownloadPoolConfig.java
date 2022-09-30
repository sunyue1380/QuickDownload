package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.PoolConfig;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;
import java.util.function.BiPredicate;

public interface DownloadPoolConfig {
    /**
     * 指定临时文件目录
     * @param temporaryDirectoryPath 临时文件目录
     * */
    DownloadPoolConfig temporaryDirectoryPath(String temporaryDirectoryPath);

    /**
     * 下载失败重试次数(默认3次)
     * @param retryTimes 下载失败重试次数
     * */
    DownloadPoolConfig retryTimes(int retryTimes);

    /**
     * 是否删除临时文件(默认删除)
     * @param deleteTemporaryFile 是否删除临时文件
     * */
    DownloadPoolConfig deleteTemporaryFile(boolean deleteTemporaryFile);

    /**
     * 是否强制单线程下载
     * @param singleThread 是否强制单线程下载
     * */
    DownloadPoolConfig singleThread(boolean singleThread);

    /**
     * 指定全局下载任务超时时间
     * @param downloadTimeoutMillis 下载任务超时时间(ms)
     * */
    DownloadPoolConfig maxDownloadTimeout(int downloadTimeoutMillis);

    /**
     * 指定最大下载速度(kb/s)
     * @param maxDownloadSpeed 最大下载速度(kb/s)
     * */
    DownloadPoolConfig maxDownloadSpeed(int maxDownloadSpeed);

    /**
     * 指定全局最大线程连接个数
     * @param maxThreadConnection 最大线程连接个数(默认个数为CPU核心数*2)
     * */
    DownloadPoolConfig maxThreadConnection(int maxThreadConnection);

    /**
     * 指定全局文件保存目录
     * @param directoryPath 文件保存目录
     * */
    DownloadPoolConfig directoryPath(String directoryPath);

    /**
     * 指定最大同时下载任务个数
     * @param parallelDownloadCount 最大同时下载任务个数(默认个数为CPU核心数)
     * */
    DownloadPoolConfig parallelDownloadCount(int parallelDownloadCount);

    /**
     * 指定全局文件完整性校验函数
     * @param fileIntegrityChecker 文件完整性校验函数
     * */
    DownloadPoolConfig fileIntegrityChecker(BiPredicate<Response, Path> fileIntegrityChecker);

    /**
     * 指定线程池事件监听接口
     * @param downloadPoolListener 线程池事件监听接口
     * */
    DownloadPoolConfig downloadPoolListener(DownloadPoolListener downloadPoolListener);

    /**
     * 获取配置参数
     * */
    PoolConfig getPoolConfig();
}
