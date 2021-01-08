package cn.schoolwow.download.domain.m3u8.tag;

/** I-frame帧定义*/
public class IFRAMESTREAMINF {
    /**路径*/
    public String URI;
    /**带宽,必选*/
    public String BANDWIDTH;
    /**平均切片传输速率*/
    public String AVERAGE_BANDWIDTH;
    /**逗号分隔的格式列表*/
    public String CODECS;
    /**最佳像素方案*/
    public String RESOLUTION;
    /**保护层级,可选值为TYPE-0或者NONE*/
    public String HDCP_LEVEL;
}
