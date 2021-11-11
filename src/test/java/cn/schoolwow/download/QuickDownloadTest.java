package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.download.listener.SimpleDownloadPoolListener;
import cn.schoolwow.download.pool.DownloadPool;
import cn.schoolwow.download.pool.DownloadPoolConfig;
import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.response.Response;
import cn.schoolwow.quickserver.QuickServer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class QuickDownloadTest {
    @BeforeClass
    public static void beforeClass(){
        new Thread(()->{
            try {
                QuickServer.newInstance()
                        .staticResourcePath(System.getProperty("user.dir"))
                        .port(10002)
                        .start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        QuickHttp.clientConfig().origin("http://127.0.0.1:10002");
    }

    @Test
    public void singleDownload() throws IOException {
        DownloadTask downloadTask = new DownloadTask();
        Path tempFilePath = Files.createTempFile("QuickDownload.",".LICENSE");
        Files.deleteIfExists(tempFilePath);
        downloadTask.filePath = tempFilePath.toString();
        downloadTask.request = QuickHttp.connect("/LICENSE");
        downloadTask.singleThread = true;
        downloadTask.downloadLogFilePath = System.getProperty("user.dir") + "/logs/singleDownload_" + System.currentTimeMillis()+".txt";
        QuickDownload.download(downloadTask);
        try {
            Thread.sleep(2000);
            Path path = Paths.get(System.getProperty("user.dir")+"/LICENSE");
            Assert.assertEquals(Files.size(path),Files.size(tempFilePath));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            Files.deleteIfExists(tempFilePath);
        }
    }

    @Test
    public void singleSupplierDownload() throws IOException {
        DownloadTask downloadTask = new DownloadTask();
        Path tempFilePath = Files.createTempFile("QuickDownload",".LICENSE");
        Files.deleteIfExists(tempFilePath);
        downloadTask.filePath = tempFilePath.toString();
        downloadTask.requestSupplier = ()->QuickHttp.connect("/LICENSE");
        downloadTask.singleThread = true;
        downloadTask.downloadLogFilePath = System.getProperty("user.dir") + "/logs/singleSupplierDownload_" + System.currentTimeMillis()+".txt";
        QuickDownload.download(downloadTask);
        try {
            Thread.sleep(2000);
            Path path = Paths.get(System.getProperty("user.dir")+"/LICENSE");
            Assert.assertEquals(Files.size(path),Files.size(tempFilePath));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            Files.deleteIfExists(tempFilePath);
        }
    }

    @Test
    public void multiDownload() throws IOException {
        DownloadTask downloadTask = new DownloadTask();
        Path filePath = Paths.get(System.getProperty("user.dir")+"/LICENSE_Test");
        Files.deleteIfExists(filePath);
        downloadTask.filePath = filePath.toString();
        downloadTask.request = QuickHttp.connect("/LICENSE");
        downloadTask.downloadLogFilePath = System.getProperty("user.dir") + "/logs/multiDownload_" + System.currentTimeMillis()+".txt";
        QuickDownload.download(downloadTask);
        try {
            Thread.sleep(2000);
            Path path = Paths.get(System.getProperty("user.dir")+"/LICENSE");
            Assert.assertEquals(Files.size(path),Files.size(filePath));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            Files.deleteIfExists(filePath);
        }
    }

    @Test
    public void deleteTemporaryFile() throws IOException {
        String temporaryDirectoryPath = System.getProperty("user.dir")+"/temporaryFilePath";
        Path temporaryDirectoryFilePath = Paths.get(temporaryDirectoryPath);
        Files.deleteIfExists(temporaryDirectoryFilePath);
        DownloadPoolConfig downloadPoolConfig = QuickDownload.downloadPoolConfig();
        downloadPoolConfig.temporaryDirectoryPath(temporaryDirectoryPath).deleteTemporaryFile(true);
        multiDownload();
        //检查临时文件目录是否为空
        Assert.assertTrue(Files.deleteIfExists(temporaryDirectoryFilePath));
        downloadPoolConfig.temporaryDirectoryPath(temporaryDirectoryPath).deleteTemporaryFile(false);
        multiDownload();
        //检查临时文件目录是否不为空
        try {
            Files.deleteIfExists(temporaryDirectoryFilePath);
        }catch (DirectoryNotEmptyException e){
            Assert.assertTrue(true);
        }finally {
            Files.walkFileTree(temporaryDirectoryFilePath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    @Test
    public void multiDownloadTaskDownload() throws IOException {
        DownloadPool downloadPool = QuickDownload.newDownloadPool();
        downloadPool.downloadPoolConfig().logDirectoryPath(System.getProperty("user.dir")+"/logs");
        Path path = Paths.get(System.getProperty("user.dir")+"/LICENSE");
        downloadPool.downloadPoolConfig().downloadPoolListener(new SimpleDownloadPoolListener() {
            @Override
            public void downloadFinished(Response response, Path file) {
                try {
                    Assert.assertEquals(Files.size(path),Files.size(file));
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        for(int i=0;i<3;i++){
            DownloadTask downloadTask = new DownloadTask();
            Path tempFilePath = Files.createTempFile("QuickDownload.",".LICENSE");
            Files.deleteIfExists(tempFilePath);
            downloadTask.filePath = tempFilePath.toString();
            downloadTask.request = QuickHttp.connect("/LICENSE");
            downloadTask.singleThread = true;
            downloadTask.downloadLogFilePath = System.getProperty("user.dir") + "/logs/singleDownload_" + System.currentTimeMillis()+".txt";
            downloadPool.download(downloadTask);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}