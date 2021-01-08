package cn.schoolwow.download.domain.m3u8.tag;

/**媒体资源加密*/
public class KEY {
    /**加密方法*/
    public String METHOD;
    /**密钥路径*/
    public String URI;
    /**AES-128算法加密使用*/
    public String IV;
    /**密钥文件存储方式,要求VERSION>=5*/
    public String KEYFORMAT;
    /**指定具体版本,要求VERSION>=5*/
    public String KEYFORMATVERSIONS;

}
