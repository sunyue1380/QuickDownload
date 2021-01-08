package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.download.pool.DownloadPool;
import cn.schoolwow.quickhttp.QuickHttp;
import org.junit.Test;

import java.io.IOException;

public class QuickDownloadTest {

    @Test
    public void newInstance() throws IOException {
        DownloadPool downloadPool = QuickDownload.newInstance()
                //最大同时下载任务个数
                .parallelDownloadCount(Runtime.getRuntime().availableProcessors())
                //全局最大线程连接个数
                .maxThreadConnection(Runtime.getRuntime().availableProcessors()*2)
                //临时文件目录
                .temporaryDirectoryPath(System.getProperty("user.dir")+"/temp")
                .build();
        new Thread(()->{
            while(true){
                downloadPool.printDownloadProgress();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        DownloadTask downloadTask = new DownloadTask();
        downloadTask.connection = QuickHttp.connect("https://ss0.baidu.com/7Po3dSag_xI4khGko9WTAnF6hhy/zhidao/pic/item/9c16fdfaaf51f3de9ba8ee1194eef01f3a2979a8.jpg");
        downloadTask.filePath = System.getProperty("user.dir")+"/9c16fdfaaf51f3de9ba8ee1194eef01f3a2979a8.jpg";
        downloadPool.download(downloadTask);

        //主线程等待
        try {
            Thread.sleep(1000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}