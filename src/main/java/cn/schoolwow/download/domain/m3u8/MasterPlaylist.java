package cn.schoolwow.download.domain.m3u8;

import cn.schoolwow.download.domain.m3u8.tag.*;

import java.util.ArrayList;
import java.util.List;

/**主播放列表*/
public class MasterPlaylist {
    /**HLS版本号*/
    public String VERSION;
    /**媒体资源列表*/
    public List<MEDIA> mediaList = new ArrayList<>();
    /**媒体资源列表备份源*/
    public List<STREAMINF> streaminfList = new ArrayList<>();
    /**I-Frame资源列表*/
    public List<IFRAMESTREAMINF> iframestreaminfList = new ArrayList<>();
    /**SESSIONDAT列表*/
    public List<SESSIONDATA> sessiondataList = new ArrayList<>();
    /**SESSIONKEY列表*/
    public List<SESSIONKEY> sessionkeyList = new ArrayList<>();
    /**是否可以独立解码*/
    public boolean INDEPENDENT_SEGMENTS;
    /*指定播放列表起始位置*/
    public START start;
}
