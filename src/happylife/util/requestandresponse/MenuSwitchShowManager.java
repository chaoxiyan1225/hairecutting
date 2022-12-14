package happylife.util.requestandresponse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.ui.ModelMap;

/** 控制 右边栏的菜单栏
 * @author 闫朝喜
 *
 */
public  class MenuSwitchShowManager {
	
	public static final String INDEX_SWITCH   = "indexSwitch";
	public static final String REPORT_SWITCH  = "reportSwitch";
	
	//员工用户管理
	public static final String USER_SWITCH    = "userSwitch";
	public static final String USER_SWITCH_LIST    = "userSwitch_list";
	public static final String USER_SWITCH_INFO   = "userSwitch_info";
	
	public static final String PRODUCT_SWITCH = "productSwitch";
	public static final String PRODUCT_SWITCH_ALL = "productSwitch_all";
	public static final String PRODUCT_SWITCH_HOT = "productSwitch_hot";
	public static final String PRODUCT_SWITCH_ADD = "productSwitch_add";
	
	
	//微信用户管理
	public static final String WECHATUSER_SWITCH    = "wechatuserSwitch";
	
	public static final String REDEEM_SWITCH  = "redeemSwitch";
	public static final String INFO_SWITCH    = "infoSwitch";
	public static final String SUGGEST_SWITCH = "suggestSwitch";
	
	public static final String ACTIVITY_SWITCH = "activitySwitch";
	
	//订单管理
	public static final String ORDER_SWITCH = "orderSwitch";
	
	
	
	/**更新 商家账户管理的 菜单栏显示情况
	 * @param map
	 */
	public static void updatePageMenuSwitch(ModelMap map ,String key,String value)
	{
		map.put(INDEX_SWITCH, "");
		map.put(REPORT_SWITCH, "");
		map.put(USER_SWITCH, "");
		map.put(USER_SWITCH_LIST, "");
		map.put(USER_SWITCH_INFO, "");
		map.put(PRODUCT_SWITCH, "");
		map.put(PRODUCT_SWITCH_ALL, "");
		map.put(PRODUCT_SWITCH_HOT, "");
		map.put(PRODUCT_SWITCH_ADD, "");
		map.put(REDEEM_SWITCH, "");
		map.put(INFO_SWITCH, "");
		map.put(SUGGEST_SWITCH, "");
		map.put(ACTIVITY_SWITCH,"");
		map.put(ORDER_SWITCH, "");
		
		//刷新下当前要显示的 标签页面
		map.put(key, value);
		
		//如果含有“_” 下划线 说明含有 父类目录，更新其为value 值相同
		if(key.contains("_")){
			map.put(key.substring(0, key.indexOf("_")), value);
		}
	
	}
	
	
	
	
	
	
	
	public static void main(String[] args) {
		String key = "abd_e" ;
		System.out.println(key.substring(0, key.indexOf("_")));
	}

}
