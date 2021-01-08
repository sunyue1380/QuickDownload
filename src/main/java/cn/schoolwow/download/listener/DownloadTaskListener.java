package cn.schoolwow.download.listener;

import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;

/**单个下载任务事件监听接口*/
public interface DownloadTaskListener {
    /**
     * 开始下载数据之前
     * @param response http请求响应
     * @param file 文件路径
     * */
    void beforeDownload(Response response, Path file);

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
