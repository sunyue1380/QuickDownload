package cn.schoolwow.download.domain.m3u8.tag;

/**指定获取媒体初始化方法*/
public class MAP {
    /**路径*/
    public String URI;

    /**部分截取*/
    public String BYTERANGE;

    @Override
    public String toString() {
        return "{" +
                "路径:" + URI + "," +
                "部分截取:" + BYTERANGE +
                "}";
    }
}
