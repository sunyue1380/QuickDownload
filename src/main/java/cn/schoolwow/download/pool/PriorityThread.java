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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 优先级下载线程
 * */
public class PriorityThread implements Runnable, Comparable<PriorityThread>{
    private Logger logger = LoggerFactory.getLogger(PriorityThread.class);

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
            if(null==downloadHolder.downloadTask.downloadLogFilePath&&null!=poolConfig.logDirectoryPath){
                downloadHolder.downloadTask.downloadLogFilePath = poolConfig.logDirectoryPath + "/QuickDownload_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))+".txt";
            }
            download();
        }catch (Exception e){
            logger.error("下载文件失败,文件路径:{}", downloadHolder.file, e);
        }finally {
            logger.trace("执行监听事件:downloadFinished");
            try {
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    downloadTaskListener.downloadFinished(downloadHolder.response,downloadHolder.file);
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    downloadPoolListener.downloadFinished(downloadHolder.response,downloadHolder.file);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            if(Thread.currentThread().isInterrupted()){
                downloadHolder.downloadProgress.state = "下载中断";
                downloadHolder.response.disconnect();
                downloadHolder.response = null;
            }
            if(null!=downloadHolder.countDownLatch){
                downloadHolder.countDownLatch.countDown();
            }
        }
    }

    /**
     * 线程执行下载任务
     * */
    private void download() throws IOException {
        logger.trace("下载任务线程启动");
        //检查临时文件目录是否存在
        Path path = Paths.get(poolConfig.temporaryDirectoryPath);
        logger.trace("检查临时文件目录,是否存在:{},路径:{}",Files.exists(path),path);
        if(Files.notExists(path)){
            Files.createDirectories(path);
            logger.trace("创建临时文件目录,路径:{}",path);
        }
        logger.trace("执行监听事件:afterExecute");
        //线程执行事件监听
        for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
            if(!downloadTaskListener.afterExecute(downloadHolder.downloadTask)){
                logger.trace("下载任务afterExecute事件返回false,下载任务结束");
                return;
            }
        }
        for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
            if(!downloadPoolListener.afterExecute(downloadHolder.downloadTask)){
                logger.trace("线程池afterExecute事件返回false,下载任务结束");
                return;
            }
        }

        if(null!=downloadHolder.downloadTask.requestSupplier){
            logger.trace("准备获取延时下载任务");
            downloadHolder.downloadTask.request = downloadHolder.downloadTask.requestSupplier.get();
        }
        if(null==downloadHolder.downloadTask.request){
            logger.warn("下载链接为空,取消下载");
            return;
        }
        if("等待下载".equals(downloadHolder.downloadProgress.state)&&isDownloadTaskExist(downloadHolder)){
            logger.warn("下载任务已经存在,文件路径:{}", downloadHolder.file);
            return;
        }
        //判断文件是否下载完成
        if(isFileDownloadedAlready(downloadHolder)){
            logger.info("文件已经下载完毕,大小:{}", String.format("%.2fMB",Files.size(downloadHolder.file)/1.0/1024/1024));
            return;
        }

        if(null==downloadHolder.response){
            logger.warn("下载任务执行失败,获取链接请求结果失败,文件路径:{}", downloadHolder.file);
            return;
        }
        logger.trace("执行监听事件:beforeDownload");
        //下载前事件监听
        for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
            if(!downloadTaskListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                logger.trace("下载任务beforeDownload事件返回false,线程结束");
                return;
            }
        }
        for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
            if(!downloadPoolListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                logger.trace("线程池beforeDownload事件返回false,线程结束");
                return;
            }
        }
        logger.trace("更新下载状态为开始下载");
        downloadHolder.downloadProgress.state = "开始下载";
        //开始正式下载
        int retryTimes = 1;
        Exception exception = null;
        while(retryTimes<=poolConfig.retryTimes){
            exception = startDownload(downloadHolder);
            if(null==exception){
                break;
            }
            if(Thread.currentThread().isInterrupted()){
                logger.debug("用户停止下载任务,下载任务保存路径:{}",downloadHolder.file);
                return;
            }
            logger.warn("下载失败,重试{}/{}次", retryTimes, downloadHolder.poolConfig.retryTimes);
            exception.printStackTrace();
            retryTimes++;
        }

        logger.trace("将当前任务从下载进度表中移除");
        //从下载进度表移除
        downloadHolderListLock.lock();
        downloadHolderList.remove(downloadHolder);
        downloadHolderListLock.unlock();

        if(retryTimes>=poolConfig.retryTimes||Files.notExists(downloadHolder.file)||null!=exception){
            logger.warn("下载失败,原因:{}, 文件路径:{}", exception.getMessage(), downloadHolder.file);
            logger.trace("更新下载状态为下载失败");
            downloadHolder.downloadProgress.state = "下载失败";
            logger.trace("执行监听事件:downloadFail");
            for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                downloadTaskListener.downloadFail(downloadHolder.response,downloadHolder.file, exception);
            }
            for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                downloadPoolListener.downloadFail(downloadHolder.response,downloadHolder.file, exception);
            }
        }else{
            logger.trace("更新下载状态为下载完成");
            downloadHolder.downloadProgress.state = "下载完成";
            if(downloadHolder.downloadTask.deleteTemporaryFile||downloadHolder.poolConfig.deleteTemporaryFile){
                logger.trace("删除临时文件");
                for(Path subFile:downloadHolder.downloadProgress.subFileList){
                    Files.deleteIfExists(subFile);
                }
            }
            logger.trace("执行监听事件:downloadSuccess");
            for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                downloadTaskListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
            }
            for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                downloadPoolListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
            }
        }
    }

    /**
     * 开始正式执行下载任务
     * @param downloadHolder 下载任务
     * @return 是否下载成功
     * */
    private Exception startDownload(DownloadHolder downloadHolder) {
        //更新下载进度信息
        logger.trace("更新下载状态为下载中");
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
        logger.trace("判断下载类型");
        //判断下载类型
        String contentType = downloadHolder.response.contentType();
        if(null==contentType){
            contentType = "";
        }
        try {
            if(downloadHolder.downloadTask.m3u8
                    ||contentType.contains("audio/x-mpegurl")
                    ||downloadHolder.response.url().endsWith(".m3u")
                    ||downloadHolder.response.url().endsWith(".m3u8")
            ){
                downloadHolder.downloadTask.m3u8 = true;
                downloadHolder.downloadProgress.m3u8 = true;
                DownloaderEnum.M3u8.download(downloadHolder);
            }else if(downloadHolder.response.contentLength()==-1||downloadHolder.downloadTask.singleThread||downloadHolder.poolConfig.singleThread||!downloadHolder.response.acceptRanges()){
                DownloaderEnum.SingleThread.download(downloadHolder);
            }else{
                DownloaderEnum.MultiThread.download(downloadHolder);
            }
            if(Thread.currentThread().isInterrupted()){
                throw new InterruptedException("线程中断!");
            }
            String fileIntegrityResult = isFileIntegrityPass(downloadHolder);
            if(null!=fileIntegrityResult){
                throw new IOException("文件完整性校验失败!"+fileIntegrityResult);
            }
        }catch (Exception e){
            return e;
        }
        return null;
    }

    /**
     * 文件完整性校验是否通过
     * @param downloadHolder 下载任务
     * @return 文件完整性校验结果
     * */
    private String isFileIntegrityPass(DownloadHolder downloadHolder) throws IOException {
        logger.trace("[执行文件完整性校验函数]");
        if(Files.notExists(downloadHolder.file)){
            logger.warn("[文件完整性校验]文件不存在,路径:{}",downloadHolder.file);
            return "文件不存在,路径:"+downloadHolder.file;
        }
        if(!downloadHolder.downloadTask.m3u8){
            //m3u8格式不检查大小
            if(null==downloadHolder.response.contentEncoding()){
                //开启了gzip压缩的情况下不检查
                long expectContentLength = downloadHolder.response.contentLength();
                long actualFileSize = Files.size(downloadHolder.file);
                if(expectContentLength>0&&expectContentLength!=actualFileSize){
                    logger.warn("文件大小不匹配,预期大小:{},实际大小:{}",expectContentLength,actualFileSize);
                    //如果实际文件大小大于预期文件大小,则删除临时文件后重新下载
                    if(actualFileSize>expectContentLength){
                        logger.trace("文件实际大小大于预期大小,删除临时文件后重新下载");
                        for(Path subFile:downloadHolder.downloadProgress.subFileList){
                            Files.deleteIfExists(subFile);
                        }
                    }
                    return "文件大小不匹配!预期大小:"+expectContentLength+",实际大小:"+actualFileSize;
                }
            }
        }
        if(null!=downloadHolder.downloadTask.fileIntegrityChecker&&!downloadHolder.downloadTask.fileIntegrityChecker.apply(downloadHolder.response,downloadHolder.file)){
            logger.warn("下载任务文件完整性校验函数未通过");
            return "下载任务文件完整性校验函数未通过";
        }
        if(null!=poolConfig.fileIntegrityChecker&&!poolConfig.fileIntegrityChecker.apply(downloadHolder.response,downloadHolder.file)){
            logger.warn("下载池文件完整性校验函数未通过");
            return "下载池文件完整性校验函数未通过";
        }
        logger.trace("[文件完整性校验函数通过]");
        return null;
    }

    /**
     * 文件是否已经下载完成
     * @param downloadHolder 下载任务
     * @return 文件是否下载完成
     * */
    private boolean isFileDownloadedAlready(DownloadHolder downloadHolder) throws IOException {
        logger.trace("判断文件是否已经下载完成");
        //获取文件大小信息
        if(null==downloadHolder.response){
            String logFilePath = Files.createTempFile("QuickHttp.","txt").toString();
            downloadHolder.response = downloadHolder.downloadTask.request.logFilePath(logFilePath).execute();
        }
        if(Files.exists(downloadHolder.file)&&null==isFileIntegrityPass(downloadHolder)){
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
        downloadHolderListLock.lock();
        try {
            logger.trace("判断下载任务是否存在");
            if(null==downloadHolder.response){
                logger.trace("准备执行http请求,链接:{}",downloadHolder.downloadTask.request.requestMeta().url);
                String logFilePath = Files.createTempFile("QuickHttp.","txt").toString();
                downloadHolder.response = downloadHolder.downloadTask.request.logFilePath(logFilePath).execute();
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
            logger.trace("文件最终保存路径获取成功,{}",downloadHolder.file);
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