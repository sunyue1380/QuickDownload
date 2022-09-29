package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.DownloadHolder;
import cn.schoolwow.download.domain.DownloaderEnum;
import cn.schoolwow.download.domain.PoolConfig;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.download.listener.DownloadTaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 优先级下载线程
 * */
public class PriorityThread implements Runnable, Comparable<PriorityThread>{
    private Logger logger = LoggerFactory.getLogger(PriorityThread.class);

    /**线程池配置信息*/
    private PoolConfig poolConfig;

    /**下载进度列表*/
    private List<DownloadHolder> downloadHolderList;

    /**下载任务*/
    private DownloadHolder downloadHolder;

    public PriorityThread(DownloadPoolImpl downloadPool, DownloadHolder downloadHolder) {
        this.poolConfig = downloadPool.poolConfig;
        this.downloadHolderList = downloadPool.downloadHolderList;
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
        }catch (InterruptedException e){
            logger.debug("用户停止了下载任务,下载任务保存路径:{}", downloadHolder.file);
        }catch (IOException e){
            downloadHolder.downloadProgress.state = "下载失败";
            executeListener("downloadFail", e);
            logger.error("下载文件失败,文件路径:{}", downloadHolder.file, e);
        }finally {
            //关闭请求资源
            downloadHolder.response.disconnect();
            downloadHolder.response = null;

            //从下载进度表移除
            logger.trace("将当前任务从下载进度表中移除");
            synchronized (downloadHolderList){
                downloadHolderList.remove(downloadHolder);
            }
            executeListener("downloadFinished", null);
            //批量下载任务线程同步
            if(null!=downloadHolder.countDownLatch){
                downloadHolder.countDownLatch.countDown();
            }
        }
    }

    /**
     * 线程执行下载任务
     * */
    private void download() throws IOException, InterruptedException {
        logger.trace("下载任务线程启动");
        downloadHolder.downloadProgress.state = "开始下载";
        checkTemporaryDirectory();
        executeListener("afterExecute", null);
        //执行http请求并检查任务是否合法
        if(!executeRequestAndCheck()){
            return;
        }
        executeListener("beforeDownload", null);
        downloadHolder.downloadProgress.state = "下载中";
        doDownload();
        downloadHolder.downloadProgress.state = "下载成功";
        handleTemporayFile();
        executeListener("downloadSuccess", null);
    }

    /**检查临时文件目录*/
    private void checkTemporaryDirectory() throws IOException {
        File file = new File(poolConfig.temporaryDirectoryPath);
        logger.trace("检查临时文件目录,路径:{}", file);
        if(!file.exists()){
            file.mkdirs();
            logger.trace("临时文件目录不存在,创建");
        }
    }

    /**
     * 执行指定事件
     * @param event 事件名称
     * @param e 下载失败时的异常对象
     * @return 是否应该继续下载
     * */
    private boolean executeListener(String event, Exception e){
        logger.trace("执行监听事件:" + event);
        switch (event){
            case "afterExecute":{
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    if(!downloadTaskListener.afterExecute(downloadHolder.downloadTask)){
                        logger.trace("下载任务afterExecute事件返回false,下载任务结束");
                        return false;
                    }
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    if(!downloadPoolListener.afterExecute(downloadHolder.downloadTask)){
                        logger.trace("线程池afterExecute事件返回false,下载任务结束");
                        return false;
                    }
                }
            }break;
            case "beforeDownload":{
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    if(!downloadTaskListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                        logger.trace("下载任务beforeDownload事件返回false,线程结束");
                        return false;
                    }
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    if(!downloadPoolListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                        logger.trace("线程池beforeDownload事件返回false,线程结束");
                        return false;
                    }
                }
            }break;
            case "downloadSuccess":{
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    downloadTaskListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    downloadPoolListener.downloadSuccess(downloadHolder.response,downloadHolder.file);
                }
            }break;
            case "downloadFail":{
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    downloadTaskListener.downloadFail(downloadHolder.response,downloadHolder.file, e);
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    downloadPoolListener.downloadFail(downloadHolder.response,downloadHolder.file, e);
                }
            }break;
            case "downloadFinished":{
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    downloadTaskListener.downloadFinished(downloadHolder.response,downloadHolder.file);
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    downloadPoolListener.downloadFinished(downloadHolder.response,downloadHolder.file);
                }
            }break;
        }
        return true;
    }

    /**获取文件最终保存路径*/
    private void getDownloadHolderPath() throws IOException {
        downloadHolder.response = downloadHolder.downloadTask.request.execute();
        if(null!=downloadHolder.downloadTask.filePath){
            downloadHolder.file = Paths.get(downloadHolder.downloadTask.filePath);
            return;
        }
        String fileName = downloadHolder.response.filename();
        if(null==fileName){
            fileName = downloadHolder.response.url().substring(downloadHolder.response.url().lastIndexOf("/")+1);
        }
        if(fileName.contains("?")){
            fileName = fileName.substring(0,fileName.indexOf("?"));
        }
        downloadHolder.file = Paths.get(downloadHolder.downloadTask.directoryPath+"/"+fileName);
        logger.debug("获取文件最终保存路径:{}", downloadHolder.file);
    }

    /**
     * 文件完整性校验是否通过
     * @param downloadHolder 下载任务
     * */
    private boolean checkFileIntegrity(DownloadHolder downloadHolder) throws IOException {
        logger.trace("执行文件完整性校验函数");
        if(!downloadHolder.file.toFile().exists()){
            logger.warn("文件完整性校验,文件不存在,路径:{}",downloadHolder.file);
            return false;
        }
        if(!downloadHolder.downloadTask.m3u8){
            //m3u8格式不检查大小
            if(null==downloadHolder.response.contentEncoding()){
                //开启了gzip压缩的情况下不检查
                long expectContentLength = downloadHolder.response.contentLength();
                long actualFileSize = downloadHolder.file.toFile().length();
                if(expectContentLength>0&&expectContentLength!=actualFileSize){
                    logger.warn("文件大小不匹配,预期大小:{},实际大小:{}",expectContentLength,actualFileSize);
                    //如果实际文件大小大于预期文件大小,则删除临时文件后重新下载
                    if(actualFileSize>expectContentLength){
                        logger.trace("文件实际大小大于预期大小,删除临时文件后重新下载");
                        for(Path subFile:downloadHolder.downloadProgress.subFileList){
                            Files.deleteIfExists(subFile);
                        }
                    }
                    return false;
                }
            }
        }
        if(null!=downloadHolder.downloadTask.fileIntegrityChecker&&!downloadHolder.downloadTask.fileIntegrityChecker.apply(downloadHolder.response,downloadHolder.file)){
            logger.warn("下载任务文件完整性校验函数未通过");
            return false;
        }
        if(null!=poolConfig.fileIntegrityChecker&&!poolConfig.fileIntegrityChecker.apply(downloadHolder.response,downloadHolder.file)){
            logger.warn("下载池文件完整性校验函数未通过");
            return false;
        }
        logger.trace("文件完整性校验函数通过");
        return true;
    }

    /**
     * 执行http请求
     * @return 是否应该继续下载
     * */
    private boolean executeRequestAndCheck() throws IOException, InterruptedException {
        if(Thread.currentThread().isInterrupted()){
            throw new InterruptedException("线程中断");
        }
        if(null!=downloadHolder.downloadTask.requestSupplier){
            logger.trace("准备获取延时下载任务");
            downloadHolder.downloadTask.request = downloadHolder.downloadTask.requestSupplier.get();
        }
        if(null==downloadHolder.downloadTask.request){
            logger.warn("下载链接获取失败,取消下载");
            return false;
        }
        //获取文件最终保存路径
        getDownloadHolderPath();
        //若用户未指定文件全路径,则需要判断下载任务是否为同一链接
        if(null==downloadHolder.downloadTask.filePath){
            synchronized (downloadHolderList){
                if(downloadHolderList.contains(downloadHolder)){
                    logger.warn("下载任务已经存在,文件路径:{}", downloadHolder.file);
                    downloadHolderList.remove(downloadHolder);
                    return false;
                }
            }
        }
        return true;
    }

    /**开始下载*/
    private void doDownload() throws IOException, InterruptedException {
        //更新下载进度信息
        downloadHolder.downloadProgress.filePath = downloadHolder.file.toString().replace("\\","/");
        downloadHolder.downloadProgress.lastDownloadedFileSize = downloadHolder.file.toFile().exists()?downloadHolder.file.toFile().length():0;
        downloadHolder.downloadProgress.totalFileSize = downloadHolder.response.contentLength();
        if(downloadHolder.downloadProgress.totalFileSize==-1){
            downloadHolder.downloadProgress.totalFileSizeFormat = "大小未知";
        }else{
            downloadHolder.downloadProgress.totalFileSizeFormat = String.format("%.2fMB",downloadHolder.response.contentLength()/1.0/1024/1024);
        }
        downloadHolder.downloadProgress.startTime = System.currentTimeMillis();
        if(Thread.currentThread().isInterrupted()){
            throw new InterruptedException("线程中断");
        }
        //判断下载类型
        String contentType = downloadHolder.response.contentType();
        if(null==contentType){
            contentType = "";
        }
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
        checkFileIntegrity(downloadHolder);
    }

    /**处理临时文件*/
    private void handleTemporayFile(){
        if(downloadHolder.downloadTask.deleteTemporaryFile||downloadHolder.poolConfig.deleteTemporaryFile){
            logger.trace("根据用户设置删除临时文件");
            for(Path subFile:downloadHolder.downloadProgress.subFileList){
                try {
                    Files.deleteIfExists(subFile);
                } catch (IOException e) {
                    logger.warn("删除临时文件失败,路径:{},", subFile, e);
                }
            }
        }
    }
}