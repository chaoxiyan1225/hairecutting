package happylife.util.config;

/** 该配置类 配置了系统的索引 key 值
 * @author 闫朝喜
 * 
 * by  2016-5-22
 *
 */
public class IndexConfig {

	/**
	 * 存储在COOKIE中的后台管理员ID
	 */
	public static final String ADMIN_SESSION_KEY="systemAdmin";
	
	/**
	 * 存储在COOKIE中的商户信息
	 */
	public static final String MCHUSER_SESSION_KEY="mchUser";
	
	/**
	 * 存储在COOKIE中的微信用户ID
	 */
	public static final String SESSION_WECHATUSER_KEY = "wechatuser";//用户ID
	
	public static final String RELATIVE_PATH = "\\haircutting\\staticfile\\wechatimages";
	
	public static final String RELATIVE_PATH_PREFIX = "/haircutting/staticfile/wechatimages";
	
	//这个是本地测试时候的路径
	public static final String PROJECT_ABSTRACT_PATH_TEST_STRING = "D:\\workspace\\haircutting\\WebContent\\staticfile\\wechatimages";
	
	//这个是本地测试时候的路径
	public static final String PROJECT_BASE_PATH_TEST_STRING = "D:\\workspace\\haircutting";
	
	//这个是实际阿里云下面的工程里面的tomcat绝对路径下面
	public static final String PROJECT_BASE_PATH_ALIYUN_STRING = "C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 7.0\\webapps\\haircutting";
	
	//这个是实际阿里云下面的工程里面的tomcat绝对路径下面
	public static final String PROJECT_ABSTRACT_PATH_ALIYUN_STRING =  PROJECT_BASE_PATH_ALIYUN_STRING+"\\staticfile\\wechatimages";
	
	/**
	 * 商家的商品图片存储路径
	 */
	public static final String MCHUSER_PRODUCTS_PICTURE_SAVE_BASEPATH = PROJECT_ABSTRACT_PATH_TEST_STRING;
	
	/**
	 * 商家的商品图片存储路径
	 */
	public static final String MCHUSER_PRODUCTS_PICTURE_TMEP_BASEPATH = "\\tem";
	
	/**
	 * 商家自己的图片存储路径
	 */
	public static final String MCHUSER_PICTURE_TMEP_BASEPATH = "\\mchusers\\tem";
	
	/**
	 * 商家的自己的图片存储路径
	 */
	public static final String MCHUSER_PICTURE_SAVE_BASEPATH = "\\mchusers";
	
	
	//商家放到数据库中的地址 
	public static final String MCHUSER_PICTURE_IN_DB = "staticfile\\wechatimages\\mchusers\\";
	
	//商家放到数据库中的地址 
	public static final String MCHUSER_PRODUCT_PICTURE_IN_DB = "staticfile\\wechatimages\\mchusers";
	
	/**
	 * 从 微信接入的链接的公共前缀
	 */
	public static String  USER_FROM_WECHAT_URI_PREFIX= "/wechatuser"; //

	/**
	 * 工程URL
	 */
	public static final String HOME_URL_TEST="http://127.0.0.1:8080/haircutting";
	
	//实际的项目运行时候的URL，阿里云下面的
	public static final String HOME_URL_ALIYUN="http://www.mfqq.club/haircutting";

	
}
