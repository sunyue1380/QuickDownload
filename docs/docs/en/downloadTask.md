# Download Task

There are DownloadTask configurations:

Please refer to [QuickHttp](https://quickhttp.schoolwow.cn) for more infomation about ``request`` field. 

```java
DownloadTask downloadTask = new DownloadTask();
//http request
downloadTask.request = QuickHttp.connect("https://www.baidu.com");
//file saved path
downloadTask.filePath = System.getProperty("user.dir") + "/files/index.html";
//whether download with single thread
downloadTask.singleThread = true;
//whether m3u8 file
downloadTask.m3u8 = true;
//whether deleting temporary after finishing download
downloadTask.deleteTemporaryFile = true;
//download timeout(ms)
downloadTask.downloadTimeoutMillis = 60000;
//max download speed(kb/s)
downloadTask.maxDownloadSpeed = 1024;
//priority of download task(default value is 100)
downloadTask.priority = 100;
//file saved in directory path(either downloadTask.filePath or downloadTask.directoryPath)
downloadTask.directoryPath = System.getProperty("user.dir") + "/files/index.html";
//download log saved directory
downloadTask.downloadLogFilePath =  System.getProperty("user.dir") + "/logs/download.log";
//file integrity check
downloadTask.fileIntegrityChecker = (response,path)->{
    //compare response header(eg:Content-MD5) with file hash
    return true;
};
```