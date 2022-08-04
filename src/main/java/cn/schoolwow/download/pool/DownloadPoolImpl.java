package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class DownloadPoolImpl implements DownloadPool{
    private Logger logger = LoggerFactory.getLogger(DownloadPoolImpl.class);

    /**线程池配置信息*/
    public PoolConfig poolConfig = new PoolConfig();

    /**线程池配置类*/
    private DownloadPoolConfig downloadPoolConfig = new DownloadPoolConfigImpl(poolConfig);

    /**下载进度列表*/
    public List<DownloadHolder> downloadHolderList = new CopyOnWriteArrayList<>();

    /**同步锁*/
    public ReentrantLock downloadHolderListLock = new ReentrantLock();

    @Override
    public DownloadPoolConfig downloadPoolConfig() {
        return downloadPoolConfig;
    }

    @Override
    public List<DownloadProgress> getProgressList() {
        Iterator<DownloadHolder> iterator = downloadHolderList.iterator();
        List<DownloadProgress> downloadProgressList = new ArrayList<>(downloadHolderList.size());
        int no = 1;
        while(iterator.hasNext()){
            DownloadProgress downloadProgress = iterator.next().downloadProgress;
            //如果间隔时间小于1s,直接返回
            long intervalTimes = System.currentTimeMillis()-downloadProgress.lastTime;
            if(intervalTimes>=1000&&"下载中".equals(downloadProgress.state)){
                long currentFileSize = 0;
                int successDownloadCount = 0;
                for (Path subFile : downloadProgress.subFileList) {
                    if (null != subFile && Files.exists(subFile)) {
                        try {
                            currentFileSize += Files.size(subFile);
                        } catch (IOException e) {
                            logger.warn("[统计文件大小失败]{}", e.getMessage());
                        }
                        successDownloadCount++;
                    }
                }
                downloadProgress.currentFileSize = currentFileSize;
                if(downloadProgress.m3u8){
                    downloadProgress.currentFileSizeFormat = successDownloadCount+"("+String.format("%.2fMB",downloadProgress.currentFileSize/1.0/1024/1024)+")";
                }else{
                    downloadProgress.currentFileSizeFormat = String.format("%.2fMB",downloadProgress.currentFileSize/1.0/1024/1024);
                }
                downloadProgress.downloadSpeed = (downloadProgress.currentFileSize - downloadProgress.lastDownloadedFileSize)/(intervalTimes/1000);
                if(downloadProgress.downloadSpeed<1024*1024){
                    downloadProgress.downloadSpeedFormat = String.format("%.2fKB/s",downloadProgress.downloadSpeed/1.0/1024);
                }else{
                    downloadProgress.downloadSpeedFormat = String.format("%.2fMB/s",downloadProgress.downloadSpeed/1.0/1024/1024);
                }
                if(downloadProgress.m3u8&&downloadProgress.subFileList.length>0){
                    downloadProgress.percent = successDownloadCount*100/downloadProgress.subFileList.length;
                }else if(downloadProgress.totalFileSize>0){
                    downloadProgress.percent = (int) (downloadProgress.currentFileSize*100/downloadProgress.totalFileSize);
                }
                downloadProgress.lastTime = System.currentTimeMillis();
                downloadProgress.lastDownloadedFileSize = downloadProgress.currentFileSize;
            }
            downloadProgress.no = no++;
            downloadProgressList.add(downloadProgress);
        }
        return downloadProgressList;
    }

    @Override
    public void printDownloadProgress() {
        List<DownloadProgress> downloadProgressList = getProgressList();
        for(DownloadProgress downloadProgress:downloadProgressList){
            System.out.println(String.format(
                    "|%-3d|%-5s|%-15s|%-5s/%-5s|%02d%%|%-4s",
                    downloadProgress.no,
                    downloadProgress.state,
                    downloadProgress.filePath,
                    downloadProgress.currentFileSizeFormat,
                    downloadProgress.totalFileSizeFormat,
                    downloadProgress.percent,
                    downloadProgress.downloadSpeedFormat
            ));
        }
        if(!downloadProgressList.isEmpty()){
            System.out.println();
        }
    }

    @Override
    public DownloadFuture[] download(DownloadTask... downloadTasks){
        if(null==downloadTasks||downloadTasks.length==0){
            logger.warn("[下载任务数组为空]");
            return null;
        }
        DownloadFuture[] downloadFutures = new DownloadFuture[downloadTasks.length];
        for(int i=0;i<downloadTasks.length;i++){
            DownloadHolder downloadHolder = getDownloadHolder(downloadTasks[i]);
            if(null==downloadHolder){
                logger.warn("[下载任务为空]数组下标:"+i);
                continue;
            }
            logger.trace("[添加下载任务到线程池]下标:{}",i);
            DownloadFuture downloadFuture = new DownloadFuture();
            downloadFuture.downloadPool = this;
            downloadFuture.downloadHolder = downloadHolder;
            downloadFuture.downloadThreadFuture = poolConfig.threadPoolExecutor.submit(downloadHolder.priorityThread);
            downloadFutures[i] = downloadFuture;
        }
        return downloadFutures;
    }

    @Override
    public void download(Consumer<Path[]> downloadFinished, DownloadTask... downloadTasks){
        if(null==downloadTasks||downloadTasks.length==0){
            logger.warn("[下载任务数组为空]");
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(downloadTasks.length);
        DownloadHolder[] downloadHolders = new DownloadHolder[downloadTasks.length];
        for(int i=0;i<downloadTasks.length;i++){
            downloadHolders[i] = getDownloadHolder(downloadTasks[i]);
            if(null==downloadHolders[i]){
                countDownLatch.countDown();
                continue;
            }
            downloadHolders[i].countDownLatch = countDownLatch;
            poolConfig.threadPoolExecutor.submit(downloadHolders[i].priorityThread);
        }
        poolConfig.batchDownloadTaskThreadPoolExecutor.execute(()->{
            try {
                countDownLatch.await(2, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Path[] paths = new Path[downloadHolders.length];
            for(int i=0;i<paths.length;i++){
                if(null!=downloadHolders[i]&&null!=downloadHolders[i].file){
                    paths[i] = downloadHolders[i].file;
                }else{
                    paths[i] = Paths.get(downloadTasks[i].filePath);
                }
            }
            downloadFinished.accept(paths);
        });
    }

    /**
     * 获取DownloadHolder对象
     * */
    private DownloadHolder getDownloadHolder(DownloadTask downloadTask){
        if(null==downloadTask){
            logger.warn("[下载任务为空]");
            return null;
        }
        //新建DownloadHolder对象
        DownloadHolder downloadHolder = new DownloadHolder();
        downloadHolder.downloadTask = downloadTask;
        downloadHolder.poolConfig = poolConfig;
        if(null==downloadHolder.downloadProgress){
            downloadHolder.downloadProgress = new DownloadProgress();
            downloadHolder.downloadProgress.m3u8 = downloadTask.m3u8;
        }
        //检查下载任务文件目录是否合法
        if(!checkDownloadTask(downloadHolder)){
            return null;
        }
        //添加到下载进度列表
        if(!addToDownloadHolderList(downloadHolder)){
            return null;
        }
        downloadHolder.priorityThread = new PriorityThread(this,downloadHolder);
        return downloadHolder;
    }

    /**
     * 将DownloadHolder对象添加到下载进度列表
     * */
    private boolean addToDownloadHolderList(DownloadHolder downloadHolder){
        downloadHolderListLock.lock();
        downloadHolder.downloadProgress.m3u8 = downloadHolder.downloadTask.m3u8;
        //是否添加成功
        boolean result = true;
        if(null!=downloadHolder.downloadTask.filePath){
            downloadHolder.downloadProgress.filePath = downloadHolder.downloadTask.filePath;
            Path path = Paths.get(downloadHolder.downloadProgress.filePath);
            if(Files.exists(path)){
                logger.debug("[跳过下载任务]文件已存在,路径:{}",path);
                result = false;
            }
        }else{
            String directoryPath = downloadHolder.downloadTask.directoryPath;
            if(null==directoryPath){
                directoryPath = downloadHolder.poolConfig.directoryPath;
            }
            downloadHolder.downloadProgress.filePath = directoryPath+"/{{文件名}}";
            result = true;
        }
        if(result){
            downloadHolderList.add(downloadHolder);
        }
        downloadHolderListLock.unlock();
        return result;
    }

    /**
     * 检查下载任务是否合法
     * */
    private boolean checkDownloadTask(DownloadHolder downloadHolder) {
        if(null!=downloadHolder.downloadTask.directoryPath){
            Path path = Paths.get(downloadHolder.downloadTask.directoryPath);
            if(Files.notExists(path)){
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.warn("[目标目录创建失败]路径:{}",path);
                    return false;
                }
            }
        }else if(null!=downloadHolder.downloadTask.filePath){
            downloadHolder.file = Paths.get(downloadHolder.downloadTask.filePath);
            if(Files.notExists(downloadHolder.file.getParent())){
                try {
                    Files.createDirectories(downloadHolder.file.getParent());
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.warn("[目标文件所在目录创建失败]路径:{}",downloadHolder.file.getParent());
                    return false;
                }
            }
        }else if(null==poolConfig.directoryPath){
            logger.warn("未指定下载路径,请指定文件全路径或者文件保存目录!");
            return false;
        }
        return true;
    }
}
