package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.download.listener.DownloadPoolListener;
import cn.schoolwow.download.pool.DownloadPoolConfig;
import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.response.Response;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QuickDownloadTest {

    @Test
    public void priorityTest(){
        DownloadPoolConfig downloadPoolConfig = QuickDownload.downloadPoolConfig();
        downloadPoolConfig.parallelDownloadCount(1);
        downloadPoolConfig.downloadPoolListener(new DownloadPoolListener() {
            @Override
            public boolean afterExecute(DownloadTask downloadTask) {
                System.out.println("当前执行任务优先级:"+downloadTask.priority);
                return false;
            }

            @Override
            public boolean beforeDownload(Response response, Path file) {
                return false;
            }

            @Override
            public void downloadSuccess(Response response, Path file) {

            }

            @Override
            public void downloadFail(Response response, Path file, Exception exception) {

            }

            @Override
            public void downloadFinished(Response response, Path file) {

            }
        });

        //优先级顺序测试
        DownloadTask[] downloadTasks = new DownloadTask[100];
        for(int i=0;i<downloadTasks.length;i++){
            downloadTasks[i] = new DownloadTask();
            downloadTasks[i].priority = i;
            downloadTasks[i].request = QuickHttp.connect("http://127.0.0.1:10000");
            downloadTasks[i].filePath = System.getProperty("user.dir")+"/test/downloadTask-"+i+".txt";
        }
        try {
            QuickDownload.download(downloadTasks);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void newInstance() throws IOException {
        QuickDownload.downloadPoolConfig().temporaryDirectoryPath(System.getProperty("user.dir")+"/temp");
        new Thread(()->{
            while(true){
                QuickDownload.printDownloadProgress();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("等待3s...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i=0;i<5;i++){
            DownloadTask downloadTask = new DownloadTask();
            downloadTask.request = QuickHttp.connect("http://127.0.0.1/video/yibin.mp4");
            downloadTask.filePath = "f:/download/yibin_"+i+".mp4";
            Path path = Paths.get(downloadTask.filePath);
            Files.deleteIfExists(path);
            QuickDownload.download(downloadTask);
        }

        //主线程等待
        try {
            Thread.sleep(1000000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}