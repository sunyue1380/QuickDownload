package cn.schoolwow.download.listener;

import cn.schoolwow.download.domain.DownloadTask;
import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;

/**下载池监听器适配类*/
public class SimpleDownloadPoolListener implements DownloadPoolListener{
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
}
