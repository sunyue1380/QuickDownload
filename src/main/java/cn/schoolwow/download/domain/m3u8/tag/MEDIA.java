package cn.schoolwow.download.domain.m3u8.tag;

/**播放媒体资源列表*/
public class MEDIA {
    /**类型,必选,可以为AUDIO,VIDEO,SUBTITLES,CLOSED-CAPTIONS*/
    public String TYPE;
    /**路径,可选*/
    public String URI;
    /**多语言翻译流组,必选*/
    public String GROUP_ID;
    /**指定流语言*/
    public String LANGUAGE;
    /**多语言版本*/
    public String ASSOC_LANGUAGE;
    /**提供可读读的描述信息,必选*/
    public String NAME;
    /**为YES时缺乏其他可选信息时应当播放该翻译流*/
    public String DEFAULT;
    /**为YES时在用户没有显示进行设置时,可以选择播放该翻译流*/
    public String AUTOSELECT;
    /**为YES时客户端应当选择播放匹配当前播放环境最佳的翻译流*/
    public String FORCED;
    /**指示切片的语言（Rendition）版本*/
    public String INSTREAM_ID;
    /**UTI 构成的字符串*/
    public String CHARACTERISTICS;
    /**由反斜杠/分隔的参数列表组成的字符串*/
    public String CHANNELS;
}
