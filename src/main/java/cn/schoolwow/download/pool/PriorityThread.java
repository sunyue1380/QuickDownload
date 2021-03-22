package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.download.domain.DownloaderEnum;
import cn.schoolwow.download.domain.PoolConfig;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.download.listener.DownloadTaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 优先级下载线程
 * */
public class PriorityThread implements Runnable,Comparable<PriorityThread>{
    private static Logger logger = LoggerFactory.getLogger(PriorityThread.class);

    /**线程池配置信息*/
    private PoolConfig poolConfig;

    /**下载进度列表*/
    private List<DownloadHolder> downloadHolderList;

    /**同步锁*/
    private ReentrantLock downloadHolderListLock;

    /**下载任务*/
    private DownloadHolder downloadHolder;

    public PriorityThread(DownloadPoolImpl downloadPool, DownloadHolder downloadHolder) {
        this.poolConfig = downloadPool.poolConfig;
        this.downloadHolderList = downloadPool.downloadHolderList;
        this.downloadHolderListLock = downloadPool.downloadHolderListLock;
        this.downloadHolder = downloadHolder;
    }

    @Override
    public int compareTo(PriorityThread o) {
        return this.downloadHolder.downloadTask.compareTo(o.downloadHolder.downloadTask);
    }

    @Override
    public void run() {
        try {
            download();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null!=downloadHolder.countDownLatch){
                downloadHolder.countDownLatch.countDown();
            }
        }
    }

    /**
     * 线程执行下载任务
     * */
    private void download(){
        //检查临时文件目录是否存在
        Path path = Paths.get(poolConfig.temporaryDirectoryPath);
        logger.trace("[检查临时文件目录是否存在]是否存在:{},路径:{}",Files.exists(path),path);
        if(Files.notExists(path)){
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //线程执行事件监听
        for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
            if(!downloadPoolListener.afterExecute(downloadHolder.downloadTask)){
                logger.trace("[afterExecute监听事件返回]");
                return;
            }
        }

        downloadHolder.downloadProgress.state = "准备下载";

        if(null!=downloadHolder.downloadTask.requestSupplier){
            downloadHolder.downloadTask.request = downloadHolder.downloadTask.requestSupplier.get();
        }
        if(null==downloadHolder.downloadTask.request){
            logger.warn("[下载链接为空]");
            return;
        }
        logger.trace("[下载任务线程启动]链接:{}",downloadHolder.downloadTask.request.requestMeta().url);
        //判断下载任务是否存在
        logger.trace("[检查下载任务是否存在]");
        if(isDownloadTaskExist(downloadHolder)){
            logger.warn("[下载任务已经存在]路径:{}",downloadHolder.file);
            return;
        }
        try {
            //判断文件是否下载完成
            logger.trace("[判断文件是否下载完成]路径:{}",downloadHolder.file);
            if(isFileDownloadedAlready(downloadHolder)){
                logger.info("[文件已经下载完毕]大小:{},文件路径:{}",String.format("%.2fMB",Files.size(downloadHolder.file)/1.0/1024/1024),downloadHolder.file);
                return;
            }

            if(null==downloadHolder.response){
                logger.warn("[下载任务执行失败]获取链接请求结果失败!");
                return;
            }

            logger.trace("[监听beforeDownload事件]");
            //下载前事件监听
            for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                if(!downloadTaskListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                    logger.debug("[监听下载任务beforeDownload事件]该事件返回false,线程返回");
                    return;
                }
            }
            for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                if(!downloadPoolListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                    logger.debug("[监听线程池beforeDownload事件]该事件返回false,线程返回");
                    return;
                }
            }
            logger.trace("[开始正式下载]链接:{}",downloadHolder.downloadTask.request.requestMeta().url);
            //开始正式下载
            int retryTimes = 1;
            while(retryTimes<=poolConfig.retryTimes&&!startDownload(downloadHolder)){
                logger.warn("[下载失败]重试{}/{}次,路径:{}",retryTimes,downloadHolder.poolConfig.retryTimes,downloadHolder.file);
                retryTimes++;
            }
            if(retryTimes>=poolConfig.retryTimes||Files.notExists(downloadHolder.file)){
                logger.warn("[下载失败]路径:{}",downloadHolder.file);
            }else{
                logger.info("[文件下载完成]大小:{},路径:{}",String.format("%.2fMB",Files.size(downloadHolder.file)/1.0/1024/1024),downloadHolder.file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //下载完成事件监听
            for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                downloadTaskListener.downloadFinished(downloadHolder.response,downloadHolder.file);
            }
            for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                downloadPoolListener.downloadFinished(downloadHolder.response,downloadHolder.file);
            }
            //从下载进度表移除
            downloadHolderListLock.lock();
            downloadHolderList.remove(downloadHolder);
            downloadHolderListLock.unlock();
        }
    }

    /**
     * 开始正式执行下载任务
     * @param downloadHolder 下载任务
     * @return 是否下载成功
     * */
    private boolean startDownload(DownloadHolder downloadHolder) {
        //检查临时下载文件目录是否存在
        logger.trace("[检查临时文件目录是否存在]路径:{}",downloadHolder.poolConfig.temporaryDirectoryPath);
        Path temporaryDirectoryPath = Paths.get(downloadHolder.poolConfig.temporaryDirectoryPath);
        if(Files.notExists(temporaryDirectoryPath)){
            try {
                Files.createDirectories(temporaryDirectoryPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //更新下载进度信息
        logger.trace("[更新下载进度信息]");
        {
            downloadHolder.downloadProgress.state = "下载中";
            downloadHolder.downloadProgress.filePath = downloadHolder.file.toString();
            try {
                downloadHolder.downloadProgress.lastDownloadedFileSize = Files.exists(downloadHolder.file)?Files.size(downloadHolder.file):0;
            } catch (IOException e) {
                e.printStackTrace();
            }
            downloadHolder.downloadProgress.totalFileSize = downloadHolder.response.contentLength();
            if(downloadHolder.downloadProgress.totalFileSize==-1){
                downloadHolder.downloadProgress.totalFileSizeFormat = "大小未知";
            }else{
                downloadHolder.downloadProgress.totalFileSizeFormat = String.format("%.2fMB",downloadHolder.response.contentLength()/1.0/1024/1024);
            }
            downloadHolder.downloadProgress.startTime = System.currentTimeMillis();
        }
        logger.trace("[判断下载类型]");
        //判断下载类型
        String contentType = downloadHolder.response.contentType();
        if(null==contentType){
            contentType = "";
        }
        try{
            if(downloadHolder.downloadTask.m3u8
                    ||contentType.contains("audio/x-mpegurl")
                    ||downloadHolder.response.url().endsWith(".m3u")
                    ||downloadHolder.response.url().endsWith(".m3u8")
            ){
                downloadHolder.downloadTask.m3u8 = true;
                downloadHolder.downloadProgress.m3u8 = true;
                DownloaderEnum.M3u8.download(downloadHolder);
                logger.debug("[m3u8下载]路径:{},url:{}",downloadHolder.file,downloadHolder.downloadTask.request.requestMeta().url);
            }else if(downloadHolder.response.contentLength()==-1||downloadHolder.downloadTask.singleThread||downloadHolder.poolConfig.singleThread||!downloadHolder.response.acceptRanges()){
                DownloaderEnum.SingleThread.download(downloadHolder);
                logger.debug("[单线程下载]路径:{},url:{}",downloadHolder.file,downloadHolder.downloadTask.request.requestMeta().url);
            }else{
                DownloaderEnum.MultiThread.download(downloadHolder);
                logger.debug("[多线程下载]路径:{},url:{}",downloadHolder.file,downloadHolder.downloadTask.request.requestMeta().url);
            }
            logger.trace("[执行文件完整性校验函数]");
            if(isFileIntegrityPass(downloadHolder)){
                logger.trace("[文件完整性校验函数通过]");
                downloadHolder.downloadProgress.state = "下载完成";
                if(downloadHolder.downloadTask.deleteTemporaryFile||downloadHolder.poolConfig.deleteTemporaryFile){
                    for(Path subFile:downloadHolder.downloadProgress.subFileList){
                        Files.deleteIfExists(subFile);
                    }
                }
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    downloadTaskListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    downloadPoolListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
                }
                return true;
            }else{
                logger.trace("[文件完整性校验函数未通过]");
                IOException exception = new IOException("文件完整性校验失败!");
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    downloadTaskListener.downloadFail(downloadHolder.response,downloadHolder.file,exception);
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    downloadPoolListener.downloadFail(downloadHolder.response,downloadHolder.file,exception);
                }
                downloadHolder.downloadProgress.state = "下载失败";
                return false;
            }
        }catch (Exception exception){
            exception.printStackTrace();
            for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                downloadTaskListener.downloadFail(downloadHolder.response,downloadHolder.file,exception);
            }
            for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                downloadPoolListener.downloadFail(downloadHolder.response,downloadHolder.file,exception);
            }
            return false;
        }
    }

    /**
     * 文件完整性校验是否通过
     * @param downloadHolder 下载任务
     * @return 文件完整性校验结果
     * */
    private boolean isFileIntegrityPass(DownloadHolder downloadHolder) throws IOException {
        long contentLength = downloadHolder.response.contentLength();
        if(!downloadHolder.downloadTask.m3u8&&null==downloadHolder.response.contentEncoding()&&Files.exists(downloadHolder.file)&&contentLength>0&&contentLength!=Files.size(downloadHolder.file)){
            logger.warn("[文件大小不匹配]预期大小:{},实际大小:{},路径:{}",downloadHolder.response.contentLength(),Files.size(downloadHolder.file),downloadHolder.file);
            return false;
        }
        if(null!=downloadHolder.downloadTask.fileIntegrityChecker&&!downloadHolder.downloadTask.fileIntegrityChecker.apply(downloadHolder.response,downloadHolder.file)){
            logger.warn("[下载任务文件完整性校验未通过]路径:{}",downloadHolder.file);
            return false;
        }
        if(null!=poolConfig.fileIntegrityChecker&&!poolConfig.fileIntegrityChecker.apply(downloadHolder.response,downloadHolder.file)){
            logger.warn("[下载池文件完整性校验未通过]路径:{}",downloadHolder.file);
            return false;
        }
        return true;
    }

    /**
     * 文件是否已经下载完成
     * @param downloadHolder 下载任务
     * @return 文件是否下载完成
     * */
    private boolean isFileDownloadedAlready(DownloadHolder downloadHolder) throws IOException {
        //获取文件大小信息
        if(null==downloadHolder.response){
            downloadHolder.response = downloadHolder.downloadTask.request.execute();
        }
        if(Files.exists(downloadHolder.file)&&isFileIntegrityPass(downloadHolder)){
            for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                downloadTaskListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
            }
            for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                downloadPoolListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
            }
            return true;
        }
        return false;
    }

    /**
     * 判断下载任务是否已经存在
     * @param downloadHolder 下载任务
     * */
    private boolean isDownloadTaskExist(DownloadHolder downloadHolder){
        try {
            downloadHolderListLock.lock();
            if(null==downloadHolder.response){
                downloadHolder.response = downloadHolder.downloadTask.request.execute();
            }
            //获取最终文件保存路径
            if(null!=downloadHolder.downloadTask.filePath){
                downloadHolder.file = Paths.get(downloadHolder.downloadTask.filePath);
            }else{
                String fileName = downloadHolder.response.filename();
                if(null==fileName){
                    fileName = downloadHolder.response.url().substring(downloadHolder.response.url().lastIndexOf("/")+1);
                }
                if(fileName.contains("?")){
                    fileName = fileName.substring(0,fileName.indexOf("?"));
                }
                String directoryPath = downloadHolder.downloadTask.directoryPath;
                if(null==directoryPath){
                    directoryPath = downloadHolder.poolConfig.directoryPath;
                }
                downloadHolder.file = Paths.get(directoryPath+"/"+fileName);
            }
            //判断是否在列表中已经存在
            Iterator<DownloadHolder> iterator = downloadHolderList.iterator();
            while(iterator.hasNext()){
                DownloadHolder downloadHolder1 = iterator.next();
                if(downloadHolder==downloadHolder1){
                    continue;
                }
                if(null==downloadHolder1||null==downloadHolder1.file){
                    continue;
                }
                if(downloadHolder1.file.toString().equals(downloadHolder.file.toString())){
                    downloadHolderList.remove(downloadHolder);
                    return true;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            downloadHolderListLock.unlock();
        }
        return false;
    }
}
