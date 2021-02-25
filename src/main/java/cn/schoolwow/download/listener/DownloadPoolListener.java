package cn.schoolwow.download.listener;

import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;

/**线程池事件监听接口*/
public interface DownloadPoolListener {
    /**
     * 下载线程开始执行后
     * @param downloadTask 下载任务
     * @return 是否继续执行
     * */
    boolean afterExecute(DownloadTask downloadTask);

    /**
     * 开始下载数据之前
     * @param response http请求响应
     * @param file 文件路径
     * @return 是否继续下载
     * */
    boolean beforeDownload(Response response, Path file);

    /**
     * 文件下载成功时
     * @param response http请求响应
     * @param file 文件路径
     * */
    void downloadSuccess(Response response, Path file);

    /**
     * 文件下载失败时
     * @param response http请求响应
     * @param file 文件路径
     * @param exception 异常对象
     * */
    void downloadFail(Response response, Path file, Exception exception);

    /**
     * 文件下载完成时
     * @param response http请求响应
     * @param file 文件路径
     * */
    void downloadFinished(Response response, Path file);
}
