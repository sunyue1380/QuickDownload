package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.PoolConfig;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.quickhttp.response.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

/**线程池配置类*/
public class DownloadPoolConfigImpl implements DownloadPoolConfig{
    /**线程池配置*/
    private PoolConfig poolConfig;

    public DownloadPoolConfigImpl(PoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    @Override
    public DownloadPoolConfig temporaryDirectoryPath(String temporaryDirectoryPath){
        poolConfig.temporaryDirectoryPath = temporaryDirectoryPath;
        return this;
    }

    @Override
    public DownloadPoolConfig retryTimes(int retryTimes){
        poolConfig.retryTimes = retryTimes;
        return this;
    }

    @Override
    public DownloadPoolConfig deleteTemporaryFile(boolean deleteTemporaryFile){
        poolConfig.deleteTemporaryFile = deleteTemporaryFile;
        return this;
    }

    @Override
    public DownloadPoolConfig singleThread(boolean singleThread){
        poolConfig.singleThread = singleThread;
        return this;
    }

    @Override
    public DownloadPoolConfig maxDownloadTimeout(int downloadTimeoutMillis){
        poolConfig.downloadTimeoutMillis = downloadTimeoutMillis;
        return this;
    }

    @Override
    public DownloadPoolConfig maxDownloadSpeed(int maxDownloadSpeed){
        poolConfig.maxDownloadSpeed = maxDownloadSpeed;
        return this;
    }

    @Override
    public DownloadPoolConfig maxThreadConnection(int maxThreadConnection){
        int parallelDownloadCount = poolConfig.threadPoolExecutor.getCorePoolSize();
        poolConfig.downloadThreadPoolExecutor.setCorePoolSize(parallelDownloadCount*maxThreadConnection);
        poolConfig.downloadThreadPoolExecutor.setMaximumPoolSize(parallelDownloadCount*maxThreadConnection);
        poolConfig.maxThreadConnection = maxThreadConnection;
        return this;
    }

    @Override
    public DownloadPoolConfig directoryPath(String directoryPath){
        File file = new File(directoryPath);
        if(!file.exists()){
            try {
                Files.createDirectories(file.toPath());
            } catch (IOException e) {
                throw new RuntimeException("创建全局文件夹失败", e);
            }
        }
        poolConfig.directoryPath = directoryPath;
        return this;
    }

    @Override
    public DownloadPoolConfig parallelDownloadCount(int parallelDownloadCount){
        poolConfig.threadPoolExecutor.setCorePoolSize(parallelDownloadCount);
        poolConfig.threadPoolExecutor.setMaximumPoolSize(parallelDownloadCount);
        poolConfig.downloadThreadPoolExecutor.setCorePoolSize(parallelDownloadCount*poolConfig.maxThreadConnection);
        poolConfig.downloadThreadPoolExecutor.setMaximumPoolSize(parallelDownloadCount*poolConfig.maxThreadConnection);
        return this;
    }

    @Override
    public DownloadPoolConfig fileIntegrityChecker(BiFunction<Response, Path,Boolean> fileIntegrityChecker){
        poolConfig.fileIntegrityChecker = fileIntegrityChecker;
        return this;
    }

    @Override
    public DownloadPoolConfig downloadPoolListener(DownloadPoolListener downloadPoolListener){
        poolConfig.downloadPoolListenerList.add(downloadPoolListener);
        return this;
    }

    @Override
    public PoolConfig getPoolConfig() {
        return this.poolConfig;
    }
}
