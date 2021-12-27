# 下载任务

DownloadTask类定义了下载任务，配置项如下:

```java
//下载任务配置
DownloadTask downloadTask = new DownloadTask();
//下载链接
downloadTask.request = QuickHttp.connect("https://www.baidu.com");
//文件保存路径
downloadTask.filePath = System.getProperty("user.dir") + "/files/index.html";
//是否单线程下载
downloadTask.singleThread = true;
//是否是m3u8文件
downloadTask.m3u8 = true;
//下载结束后是否删除临时文件
downloadTask.deleteTemporaryFile = true;
//下载超时时间设置(毫秒)
downloadTask.downloadTimeoutMillis = 60000;
//限制最大下载速度(kb/s)
downloadTask.maxDownloadSpeed = 1024;
//下载任务优先级(数字越小优先级越高,默认为100)
downloadTask.priority = 100;
//文件保存文件夹(和filePath二选一)
downloadTask.directoryPath = System.getProperty("user.dir") + "/files/index.html";
//指定下载日志保存路径
downloadTask.downloadLogFilePath =  System.getProperty("user.dir") + "/logs/download.log";
//文件完整性校验函数
downloadTask.fileIntegrityChecker = (response,path)->{
    //校验文件是否下载成功,例如校验md5值,sha1值等等
    return true;
};
```