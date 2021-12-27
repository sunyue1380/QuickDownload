# QuickDownload

QuickDownload是一个Http协议下载工具类。

> QuickDownload依赖了QuickHttp，[点此访问](https://quickhttp.schoolwow.cn)QuickHttp2文档

# 快速入门

## 1 导入QuickDownload
```
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDownload</artifactId>
  <version>{最新版本}</version>
</dependency>
```

> [QuickDownload最新版本查询](https://search.maven.org/search?q=a:QuickDownload)

## 2 使用QuickDownload
```java
DownloadTask downloadTask = new DownloadTask();
//请求连接
downloadTask.request = QuickHttp.connect("https://www.baidu.com");
//保存路径
downloadTask.filePath = System.getProperty("user.dir") + "/files/index.html";
//开始下载
QuickDownload.download(downloadTask);
```

# 反馈

若有问题请提交Issue或者发送邮件到648823596@qq.com.

# 开源协议
本软件使用[LGPL](http://www.gnu.org/licenses/lgpl-3.0-standalone.html)开源协议!