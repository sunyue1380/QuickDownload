package cn.schoolwow.download.domain;

import java.nio.file.Path;

/**
 * 下载进度
 * */
public class DownloadProgress {
    /**
     * 序号
     * */
    public int no;
    /**
     * 当前状态
     */
    public String state = "等待运行";
    /**
     * 是否m3u8任务
     * */
    public boolean m3u8;
    /**
     * 目标文件
     */
    public String filePath;
    /**
     * 分段文件数组
     */
    public transient Path[] subFileList = new Path[0];
    /**
     * 开始下载时间
     * */
    public long startTime;
    /**
     * 当前已下载大小
     */
    public long currentFileSize;
    /**
     * 当前已下载大小
     */
    public String currentFileSizeFormat = "-";
    /**
     * 文件总大小
     */
    public long totalFileSize;
    /**
     * 文件总大小
     */
    public String totalFileSizeFormat = "-";
    /**
     * 下载速度(kb/s)
     */
    public long downloadSpeed;
    /**
     * 下载速度(kb/s)
     */
    public String downloadSpeedFormat = "-";
    /**
     * 进度
     */
    public int percent;
    /**
     * 上次统计时间
     * */
    public long lastTime;
    /**
     * 上次已下载大小
     * */
    public long lastDownloadedFileSize;
}
