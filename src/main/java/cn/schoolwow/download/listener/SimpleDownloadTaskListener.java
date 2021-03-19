package cn.schoolwow.download.listener;

import cn.schoolwow.quickhttp.response.Response;

import java.nio.file.Path;

/**下载任务监听器适配类*/
public class SimpleDownloadTaskListener implements DownloadTaskListener{
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
