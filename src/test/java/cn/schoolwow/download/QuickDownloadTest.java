package cn.schoolwow.download;

import cn.schoolwow.download.domain.DownloadRecord;
import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.download.listener.SimpleDownloadPoolListener;
import cn.schoolwow.download.pool.DownloadPool;
import cn.schoolwow.download.pool.DownloadPoolConfig;
import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.response.Response;
import cn.schoolwow.quickserver.QuickServer;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class QuickDownloadTest {
    @BeforeClass
    public static void beforeClass(){
        new Thread(()->{
            try {
                QuickServer.newInstance()
                        .staticResourcePath(System.getProperty("user.dir"))
                        .maxLimitSpeed(128)
                        .port(10002)
                        .start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        QuickHttp.clientConfig().origin("http://127.0.0.1:10002");
    }

    private static long expectFileSize;
    static{
        File file = new File(System.getProperty("user.dir")+"/LICENSE");
        expectFileSize = file.length();
    }

    private File tempFile = null;

    @Before
    public void before() throws IOException {
        tempFile = File.createTempFile("QuickDownload.",".LICENSE");
    }

    @After
    public void after(){
        if(null!=tempFile&&tempFile.exists()){
            try {
                Files.deleteIfExists(tempFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void singleDownload() throws InterruptedException {
        DownloadTask downloadTask = getDownloadTask();
        downloadTask.singleThread = true;
        downloadTask.request = QuickHttp.connect("/LICENSE");
        QuickDownload.download(downloadTask);
        Thread.sleep(2000);
        Assert.assertNotNull(tempFile);
        Assert.assertEquals(expectFileSize, tempFile.length());
    }

    @Test
    public void singleSupplierDownload() throws InterruptedException {
        DownloadTask downloadTask = getDownloadTask();
        downloadTask.requestSupplier = ()->QuickHttp.connect("/LICENSE");
        downloadTask.singleThread = true;
        QuickDownload.download(downloadTask);
        Thread.sleep(2000);
        Assert.assertNotNull(tempFile);
        Assert.assertEquals(expectFileSize, tempFile.length());
    }

    @Test
    public void singleDownloadStopDownload() throws InterruptedException {
        DownloadTask downloadTask = getDownloadTask();
        downloadTask.request = QuickHttp.connect("/LICENSE");
        downloadTask.singleThread = true;
        QuickDownload.download(downloadTask);
        List<DownloadRecord> downloadRecordList = QuickDownload.getDownloadRecordList();
        if(downloadRecordList.isEmpty()){
            throw new IllegalArgumentException("获取下载记录为空!");
        }
        for(DownloadRecord downloadRecord:downloadRecordList){
            System.out.println("下载文件:"+downloadRecord.filePath());
            downloadRecord.pauseDownload();
        }
        Thread.sleep(1000);
        for(DownloadRecord downloadRecord:downloadRecordList){
            downloadRecord.resumeDownload();
        }
        Thread.sleep(10);
        for(DownloadRecord downloadRecord:downloadRecordList){
            downloadRecord.deleteDownloadRecord();
        }
        Thread.sleep(3000);
    }


    @Test
    public void multiDownload() throws InterruptedException {
        DownloadTask downloadTask = getDownloadTask();
        downloadTask.request = QuickHttp.connect("/LICENSE");
        QuickDownload.download(downloadTask);
        Thread.sleep(2000);
        Assert.assertNotNull(tempFile);
        Assert.assertEquals(expectFileSize, tempFile.length());
    }

    @Test
    public void multiDownloadStopDownload() throws InterruptedException {
        DownloadTask downloadTask = getDownloadTask();
        downloadTask.request = QuickHttp.connect("/LICENSE");
        QuickDownload.downloadPoolConfig().maxThreadConnection(4);
        QuickDownload.download(downloadTask);
        List<DownloadRecord> downloadRecordList = QuickDownload.getDownloadRecordList();
        for(DownloadRecord downloadRecord:downloadRecordList){
            downloadRecord.deleteDownloadRecord();
        }
        Thread.sleep(1000);
        Assert.assertNotNull(tempFile);
        Assert.assertTrue(tempFile.exists());
        Assert.assertTrue(tempFile.length()<expectFileSize);
    }

    @Test
    public void deleteTemporaryFile() throws IOException, InterruptedException {
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
            downloadPool.download(downloadTask);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private DownloadTask getDownloadTask(){
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.filePath = tempFile.getAbsolutePath();
        return downloadTask;
    }

}