package cn.schoolwow.download.pool;

import cn.schoolwow.download.domain.*;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.download.listener.DownloadTaskListener;
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
import java.util.concurrent.locks.ReentrantLock;

public class DownloadPoolImpl implements DownloadPool{
    private static Logger logger = LoggerFactory.getLogger(DownloadPoolImpl.class);

    /**线程池配置信息*/
    private PoolConfig poolConfig = new PoolConfig();

    /**线程池配置类*/
    private DownloadPoolConfig downloadPoolConfig = new DownloadPoolConfigImpl(poolConfig);

    /**下载进度列表*/
    private List<DownloadHolder> downloadHolderList = new CopyOnWriteArrayList<>();

    /**同步锁*/
    private ReentrantLock downloadHolderListLock = new ReentrantLock();

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
    public void download(DownloadTask downloadTask) throws IOException {
        if(null==downloadTask){
            logger.warn("[下载任务为空]");
            return;
        }
        //新建DownloadHolder对象
        DownloadHolder downloadHolder = new DownloadHolder();
        downloadHolder.downloadTask = downloadTask;
        downloadHolder.poolConfig = poolConfig;
        downloadHolder.downloadProgress = new DownloadProgress();
        downloadHolder.downloadProgress.m3u8 = downloadTask.m3u8;
        //检查下载任务文件目录是否合法
        if(!checkDownloadTask(downloadHolder)){
            return;
        }
        //添加到下载进度列表
        if(!addToDownloadHolderList(downloadHolder)){
            return;
        }

        downloadHolder.downloadThread = new Thread(()->{
            if(null!=downloadHolder.downloadTask.requestSupplier){
                downloadHolder.downloadTask.request = downloadHolder.downloadTask.requestSupplier.get();
            }
            if(null==downloadTask.request){
                logger.warn("[下载链接为空]");
                return;
            }
            //判断下载任务是否存在
            if(isDownloadTaskExist(downloadHolder)){
                logger.warn("[下载任务已经存在]路径:{}",downloadHolder.file);
                return;
            }
            try {
                //判断文件是否下载完成
                if(isFileDownloadedAlready(downloadHolder)){
                    logger.info("[文件已经下载完毕]大小:{},文件路径:{}",String.format("%.2fMB",Files.size(downloadHolder.file)/1.0/1024/1024),downloadHolder.file);
                    return;
                }
                //下载前事件监听
                for(DownloadTaskListener downloadTaskListener:downloadHolder.downloadTask.downloadTaskListenerList){
                    if(!downloadTaskListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                        return;
                    }
                }
                for(DownloadPoolListener downloadPoolListener:poolConfig.downloadPoolListenerList){
                    if(!downloadPoolListener.beforeDownload(downloadHolder.response,downloadHolder.file)){
                        return;
                    }
                }
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
        });
        poolConfig.threadPoolExecutor.execute(downloadHolder.downloadThread);
    }

    /**
     * 开始正式执行下载任务
     * @param downloadHolder 下载任务
     * @return 是否下载成功
     * */
    private boolean startDownload(DownloadHolder downloadHolder) {
        //更新下载进度信息
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
            }else if(downloadHolder.response.contentLength()==-1||downloadHolder.downloadTask.singleThread||downloadHolder.poolConfig.singleThread||!downloadHolder.response.acceptRanges()){
                DownloaderEnum.SingleThread.download(downloadHolder);
            }else{
                DownloaderEnum.MultiThread.download(downloadHolder);
            }
            if(isFileIntegrityPass(downloadHolder)){
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
        if(!downloadHolder.downloadTask.m3u8&&downloadHolder.response.contentLength()!=-1&&downloadHolder.response.contentLength()!=Files.size(downloadHolder.file)){
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

    /**
     * 获取DownloadHolder对象并添加到下载进度列表
     * */
    private boolean addToDownloadHolderList(DownloadHolder downloadHolder){
        downloadHolderListLock.lock();
        downloadHolder.downloadProgress.m3u8 = downloadHolder.downloadTask.m3u8;
        //是否添加成功
        boolean result = false;
        if(null!=downloadHolder.downloadTask.filePath){
            downloadHolder.downloadProgress.filePath = downloadHolder.downloadTask.filePath;
            Path path = Paths.get(downloadHolder.downloadProgress.filePath);
            if(Files.exists(path)){
                result = false;
            }else{
                result = true;
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
    public boolean checkDownloadTask(DownloadHolder downloadHolder) {
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
