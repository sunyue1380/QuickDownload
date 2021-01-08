package cn.schoolwow.download.domain.m3u8.tag;

/**播放媒体列表备份原*/
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
}
