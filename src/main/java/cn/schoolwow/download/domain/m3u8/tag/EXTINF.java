package cn.schoolwow.download.domain.m3u8.tag;

/**指定片时长和标题*/
public class EXTINF {
    /**时长(秒)*/
    public String duration;

    /**标题*/
    public String title;

    @Override
    public String toString() {
        return "{" +
                "时长(秒):" + duration + "," +
                "标题:" + title +
                "}";
    }
}
