# QuickDownload

QuickDownload is a Http Download Tool.

> QuickDownload includes [QuickHttp](https://quickhttp.schoolwow.cn)

# QuickStart

## 1 import QuickDownload
```
<dependency>
  <groupId>cn.schoolwow</groupId>
  <artifactId>QuickDownload</artifactId>
  <version>{最新版本}</version>
</dependency>
```

> [Query QuickDownload lastest version](https://search.maven.org/search?q=a:QuickDownload)

## 2 use QuickDownload
```java
DownloadTask downloadTask = new DownloadTask();
downloadTask.request = QuickHttp.connect("https://www.google.com");
//file path for download file
downloadTask.filePath = System.getProperty("user.dir") + "/files/index.html";
//start download
QuickDownload.download(downloadTask);
```

# Feedback

If you have any suggestions please Pull Request or mailto 648823596@qq.com.

# LICENSE

[LGPL](http://www.gnu.org/licenses/lgpl-3.0-standalone.html)