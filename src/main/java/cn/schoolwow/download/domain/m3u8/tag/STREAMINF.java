package cn.schoolwow.download.domain.m3u8.tag;

import cn.schoolwow.quickhttp.domain.m3u8.tag.IFRAMESTREAMINF;

/**播放媒体列表备份源*/
public class STREAMINF extends IFRAMESTREAMINF {
    /**视屏最大帧率*/
    public String FRAME_RATE;

    /**与EXT-X-MEDIA标签的TYPE=AUDIO的GROUP_ID匹配*/
    public String AUDIO;

    /**与EXT-X-MEDIA标签的TYPE=VIDEO的GROUP_ID匹配*/
    public String VIDEO;

    /**与EXT-X-MEDIA标签的TYPE=SUBTITLES的GROUP_ID匹配*/
    public String SUBTITLES;

    /**与EXT-X-MEDIA标签的TYPE=CLOSED_CAPTIONS的GROUP_ID匹配*/
    public String CLOSED_CAPTIONS;

    @Override
    public String toString() {
        return "{" +
                "路径:" + URI + "," +
                "带宽:" + BANDWIDTH + "," +
                "平均切片传输速率:" + AVERAGE_BANDWIDTH + "," +
                "逗号分隔的格式列表:" + CODECS + "," +
                "最佳像素方案:" + RESOLUTION + "," +
                "保护层级:" + HDCP_LEVEL + "," +
                "视屏最大帧率:" + FRAME_RATE + "," +
                "AUDIO:" + AUDIO + "," +
                "VIDEO:" + VIDEO + "," +
                "SUBTITLES:" + SUBTITLES + "," +
                "CLOSED_CAPTIONS:" + CLOSED_CAPTIONS +
                "}";
    }
}
