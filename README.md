# QuickDownload

QuickDownload是一个http协议文件下载工具.

> 最新版本v1.0.1 更新日期:2020-01-08

# 快速入门

## 引入QuickDownload

```xml
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDownload</artifactId>
  <version>1.0.1</version>
</dependency>
```

## 创建下载池,添加下载任务

```java
//使用默认下载池下载
//创建下载任务
DownloadTask downloadTask = new DownloadTask();
downloadTask.connection = QuickHttp.connect("https://ss0.baidu.com/7Po3dSag_xI4khGko9WTAnF6hhy/zhidao/pic/item/9c16fdfaaf51f3de9ba8ee1194eef01f3a2979a8.jpg");
downloadTask.filePath = System.getProperty("user.dir")+"/9c16fdfaaf51f3de9ba8ee1194eef01f3a2979a8.jpg";
//下载
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

* 提交Issue
* 邮箱: 648823596@qq.com
* QQ群: 958754367(quick系列交流,群初建,人较少)

# 开源协议
本软件使用 [GPL](http://www.gnu.org/licenses/gpl-3.0.html) 开源协议!