package cn.schoolwow.download.domain.m3u8.tag;

/**播放列表起始位置*/
public class START {
    /**时间偏移*/
    public String TIME_OFFSET;

    /**是否播放媒体片段*/
    public String PRECISE;

    @Override
    public String toString() {
        return "{" +
                "时间偏移:" + TIME_OFFSET + "," +
                "是否播放媒体片段:" + PRECISE +
                "}";
    }
}
