# 下载线程池

DownloadPool接口定义了下载线程池的相关方法。

## 打印下载进度

```java
QuickDownload.printDownloadProgress();
List<DownloadProgress> progressList = QuickDownload.getProgressList();
```

## 下载池配置项

```java
QuickDownload.downloadPoolConfig()
        //最大线程连接个数
        .maxThreadConnection(8)
        //同时下载任务个数
        .parallelDownloadCount(4)
        //限制最大下载速度(kb/s)
        .maxDownloadSpeed(1024)
        //限制最大下载时间
        .maxDownloadTimeout(60000)
        //下载失败重试次数
        .retryTimes(10)
        //是否使用单线程下载
        .singleThread(true)
        //下载完成后是否删除临时文件
        .deleteTemporaryFile(true)
        //指定临时文件保存文件夹
        .temporaryDirectoryPath("/path/to/temp file/directory")
        //将文件保存指定文件夹路径下
        .directoryPath("/path/to/download/directory")
        //指定下载日志文件夹保存下载日志文件
        .logDirectoryPath("/path/to/log/directory");
```

## 文件完整性校验

QuickDownload支持自定义文件完整性校验。当文件完整性校验未通过时，将重新下载文件。

```java
QuickDownload.downloadPoolConfig().fileIntegrityChecker((response,path)->{
    //可以根据响应头部的md5,sha1头部信息进行文件校验
    return true;
});
```

## 事件监听

```java
QuickDownload.downloadPoolConfig().downloadPoolListener(new DownloadPoolListener() {
    @Override
    public boolean afterExecute(DownloadTask downloadTask) {
        //下载任务被调度执行时，返回true继续下载，返回false停止下载
        return true;
    }

    @Override
    public boolean beforeDownload(Response response, Path file) {
        //发送请求前，返回true继续下载，返回false停止下载
        return true;
    }

    @Override
     public void downloadSuccess(Response response, Path file) {
       //下载成功
    }

    @Override
    public void downloadFail(Response response, Path file, Exception exception) {
        //下载失败
    }

    @Override
    public void downloadFinished(Response response, Path file) {
        //下载完成
    }
});
```

## 下载任务列表完成后执行

QuickDownload支持指定下载任务列表完成执行操作（例如合并文件等）.

```java
QuickDownload.download((paths -> {
    File mergeFile = new File("/path/to/mergeFile");
    try {
        FileOutputStream fos = new FileOutputStream(mergeFile,true);
        for(Path path:paths){
            fos.write(Files.readAllBytes(path));
        }
        fos.close();
    } catch (IOException e){
        e.printStackTrace();
    }
}));
```

## 独立DownloadPool

QuickDownload支持新建多个独立的DownloadPool实例，这些实例相互隔离。

```java
DownloadPool downloadPool = QuickDownload.newDownloadPool();
downloadPool.download(......)
```