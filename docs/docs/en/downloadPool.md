# Download Pool

## Print Download Progress

```java
QuickDownload.printDownloadProgress();
List<DownloadProgress> progressList = QuickDownload.getProgressList();
```

## Download Pool Configuration

```java
QuickDownload.downloadPoolConfig()
        //max thread connect for multi-thread download
        .maxThreadConnection(8)
        //parallel download task count
        .parallelDownloadCount(4)
        //max download speed(kb/s)
        .maxDownloadSpeed(1024)
        //download timout
        .maxDownloadTimeout(60000)
        //retry times when fail
        .retryTimes(10)
        //force using single thread download 
        .singleThread(true)
        //weather delete temporary file after finishing download
        .deleteTemporaryFile(true)
        //temporary file saved directory
        .temporaryDirectoryPath("/path/to/temp file/directory")
        //download file saved directory
        .directoryPath("/path/to/download/directory")
        //download log file saved directory
        .logDirectoryPath("/path/to/log/directory");
```

## File Integrity Check

In some cases, you can verify file integrity by comparing response header with file hash.

```java
QuickDownload.downloadPoolConfig().fileIntegrityChecker((response,path)->{
    //compare response header(eg:Content-MD5) with file hash
    return true;
});
```

## Event Listener

```java
QuickDownload.downloadPoolConfig().downloadPoolListener(new DownloadPoolListener() {
    @Override
    public boolean afterExecute(DownloadTask downloadTask) {
        return true;
    }

    @Override
    public boolean beforeDownload(Response response, Path file) {
        return true;
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
```

## Execute function after specific download task list

This feature is very nice if you need to merge file.

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

## Parallel DownloadPool

You can create many ``DownloadPool`` instances and these instances are separately from others.

```java
DownloadPool downloadPool = QuickDownload.newDownloadPool();
downloadPool.download(......)
```