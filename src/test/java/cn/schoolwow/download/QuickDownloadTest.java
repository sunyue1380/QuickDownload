package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.quickhttp.QuickHttp;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QuickDownloadTest {

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