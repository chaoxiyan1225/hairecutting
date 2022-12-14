package happylife.util.config;

public class WinterOrangeSysConf {
	
	public static final int QUEUE_ORDER_MONEY_ONECE = 100 ;//取号一次的费用 单位是分，后续会改
	
	public static final boolean  IS_FREE_SERVICE = false ; //当前是否免费享受服务
	
	public static final boolean  IS_TEST_VALID = false ;// 是否处于测试模式，正式上线的时候设置为false
	
	public static final int  WECHAT_FOLLOW_MCHS_COUNT = 16 ;// 至多关注多少个商家

	public static final int  PAY_CNT_FOR_TASK= 100 ;// 支付多少次进行一次后台更新任务
	public static final int  VISIT_CNT_FOR_TASK= 1000 ;// 访问多少次行一次后台更新任务
	
	
}
