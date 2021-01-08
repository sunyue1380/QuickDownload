# QuickDownload

QuickDAO是一个http协议文件下载工具.

# 快速入门

## 引入QuickDownload

```xml
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDownload</artifactId>
  <version>1.0</version>
</dependency>
```

## 创建下载池,添加下载任务

```java
//创建下载线程池
DownloadPool downloadPool = QuickDownload.newInstance()
                //最大同时下载任务个数
                .parallelDownloadCount(Runtime.getRuntime().availableProcessors())
                //全局最大线程连接个数
                .maxThreadConnection(Runtime.getRuntime().availableProcessors()*2)
                //临时文件目录
                .temporaryDirectoryPath(System.getProperty("user.dir")+"/temp")
                .build();
//创建下载任务
DownloadTask downloadTask = new DownloadTask();
downloadTask.connection = QuickHttp.connect("https://ss0.baidu.com/7Po3dSag_xI4khGko9WTAnF6hhy/zhidao/pic/item/9c16fdfaaf51f3de9ba8ee1194eef01f3a2979a8.jpg");
downloadTask.filePath = System.getProperty("user.dir")+"/9c16fdfaaf51f3de9ba8ee1194eef01f3a2979a8.jpg";
downloadPool.download(downloadTask);
```

# 详细配置项

## 下载池配置参数

```java

```
|方法|说明|默认值|
|---|---|---|
|temporaryDirectoryPath|指定临时文件目录|System.getProperty("java.io.tmpdir")+ File.separator+"DownloadPool";|
|retryTimes|重试次数|3|
|deleteTemporaryFile|下载成功后是否删除临时文件|默认删除|
|singleThread|是否强制单线程下载|否|
|connectTimeoutMillis|指定全局下载任务连接超时时间(ms)|0ms-表示不限制|
|readTimeoutMillis|指定全局下载任务读取超时时间(ms)|0ms-表示不限制|
|maxDownloadTimeout|指定全局下载任务超时时间|3600000ms,多线程下载时的最大下载时间|
|maxDownloadSpeed|指定最大下载速度(kb/s)|0-不限制|
|maxThreadConnection|指定全局最大线程连接个数|cpu核心数*2|
|directoryPath|指定全局文件保存目录|无|
|parallelDownloadCount|指定最大同时下载任务个数|cpu核心数|
|fileIntegrityChecker|指定全局文件完整性校验函数|无|
|downloadPoolListener|指定线程池事件监听接口|无|

# 反馈

目前QuickDownload还不成熟,还在不断完善中.若有问题请提交Issue或者发送邮件到648823596@qq.com,作者将第一时间跟进并努力解决.同时欢迎热心人士提交PR,共同完善本项目!

# 开源协议
本软件使用 [GPL](http://www.gnu.org/licenses/gpl-3.0.html) 开源协议!