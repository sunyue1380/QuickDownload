package cn.schoolwow.download.domain;

import cn.schoolwow.download.listener.DownloadTaskListener;
import cn.schoolwow.quickhttp.request.Request;
import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**下载任务*/
public class DownloadTask {
    /**下载任务*/
    public Request request;

    /**延时下载任务*/
    public Supplier<Request> requestSupplier;

    /**是否为m3u8任务*/
    public boolean m3u8;

    /**是否删除临时文件*/
    public boolean deleteTemporaryFile = true;

    /**是否强制单线程下载*/
    public boolean singleThread;

    /**下载超时时间*/
    public int downloadTimeoutMillis = 3600000;

    /**指定最大下载速度(kb/s)*/
    public int maxDownloadSpeed;

    /**保存文件全路径*/
    public String filePath;

    /**保存文件目录*/
    public String directoryPath;

    /**文件完整性校验*/
    public BiFunction<Response, Path, Boolean> fileIntegrityChecker;

    /**事件监听*/
    public List<DownloadTaskListener> downloadTaskListenerList = new ArrayList<>();
}
