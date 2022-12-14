package happylife.util.config;

public class PageConfigUtil {
	
	public static final String ADMIN_PREFIX = "/admin";
	public static final String MCH_PREFIX = "/mchuser";
	public static final String WECHAT_PREFIX = "/wechatuser";
	
	
	public static class CommonPage{
		//跳转到登录界面的URI
		public static final String TO_LOGIN_URI = "/tologin";
		
		//管理员登录URL
		public static final String LOGIN_URI = "/login";

		//账户注销的URI
		public static final String TO_LOGOUT_URI = "/logout";

		/**
		 * 管理员登录后的首页
		 */
		public static final String TO_INDEX_URI = "/toindex";

		/**
		 * 管理员登录后的首页
		 */
		public static final String INDEX_PAGE = "/index";

		/**
		 * 管理员登录界面页面
		 */
		public static final String LOGIN_PAGE = "/login";
		
		/**
		 * 管理员查看流水的URI
		 */
		public static final String TO_TRANSACTIONS_URI = "/totransactionlist";
		
		/**
		 * 管理员查看流水的URI
		 */
		public static final String TRANSACTION_MANAGER_URI = "/transactionmanage";
		
		/**
		 * 管理员的流水界面
		 */
		public static final String TRANSACTIONS_LIST_PAGE = "/transaction/list";
		
		
		/**
		 * 商家访问的所有商品界面的URI
		 */
		public static final String TO_PRODUCT_LIST_URI = "/toproductlist";

		/**
		 * 商家 的兑换码界面
		 */
		public static final String PRODUCT_LIST_PAGE = "/product/list";

		/**
		 * 商家用户 查询单个 URI
		 */
		public static final String TO_SINGLEPRODUCT_URI = "/tosingleproduct";
		
		/**
		 * 管理员登录界面
		 */
		public static final String TO_REDEEMCODE_URI = "/toredeemcodelist";

		/**
		 * 管理员登录界面
		 */
		public static final String REDEEMCODE_PAGE = "/redeemcode/list";
		
		/**
		 * 管理员查看所有活动URI
		 */
		public static final String TO_ACTIVITY_URI = "/toactivitylist";
		
		/**
		 * 所有活动列表界面
		 */
		public static final String ACTIVITY_LIST_PAGE = "/activity/list";
		
		/**
		 * 管理员 到添加单个活动的URI
		 */
		public static final String TO_ADD_ACTIVITY_URI = "/admin/toaddactivity";
		
		/**
		 * 添加活动界面
		 */
		public static final String ADD_ACTIVITY_PAGE = "/admin/activity/addactivity";
		
		
		/**
		 * 管理员查看 微信账户的建议
		 */
		public static final String TO_SUGGEST_URI = "/tosuggest";
		
		/**
		 * 管理员查看 微信账户的建议
		 */
		public static final String TO_SUGGEST_PAGE = "/suggest/list";
		
		/**
		 * 管理员修改密码 URI
		 */
		public static final String CHANGE_PASSWD_URI = "/changepwd";
		
		/**
		 * 判断商家原密码是否正确 URI
		 */
		public static final String VALIDATE_PASSWD_URI = "/validatepwd";
		
		/**
		 * 商家账户跳转到修改密码界面 URI
		 */
		public static final String TO_CHANGE_PASSWD_URI = "/tochangepwd";
		
		/**
		 * 管理员导出报表
		 */
		public static final String EXPORT_DATA = "/exportdata";
		
		
		
		
	}

	/** 系统管理员界面控制类
	 * @author yanchaoxi
	 *
	 */
	public static class AdminPage extends CommonPage {
	
		// 管理员查看 商家账户URI
		public static final String TO_MCHUSERS_URI = "/tomchuserlist";
		
		/**
		 * 商家列表界面
		 */
		public static final String MCHUSER_LIST_PAGE = "/mchuser/list";
		
		/**
		 * 管理员 到添加商家账户URI
		 */
		public static final String TO_ADD_MCHUSERS_URI = "/toaddmchuser";
		
		/**
		 * 管理员 添加商家URI
		 */
		public static final String ADD_MCHUSERS_URI = "/addmchuser";
		
		/**
		 * 添加商家界面
		 */
		public static final String ADD_MCHUSER_PAGE = "/mchuser/add";
		
		/**
		 * 商家访问的所有商品界面的URI
		 */
		public static final String TO_PRODUCT_LIST_URI = "/toproductlist";

		/**
		 * 商家 的兑换码界面
		 */
		public static final String PRODUCT_LIST_PAGE = "/product/list";

		/**
		 * 商家用户 查询单个 URI
		 */
		public static final String TO_SINGLEPRODUCT_URI = "/tosingleproduct";
		
		/**
		 * 管理员登录界面
		 */
		public static final String TO_REDEEMCODE_URI = "/toredeemcodelist";

		/**
		 * 管理员登录界面
		 */
		public static final String REDEEMCODE_PAGE = "/redeemcode/list";
		
		/**
		 * 管理员查看所有活动URI
		 */
		public static final String TO_ACTIVITY_URI = "/toactivitylist";
		
		/**
		 * 所有活动列表界面
		 */
		public static final String ACTIVITY_LIST_PAGE = "/activity/list";
		
		/**
		 * 管理员 到添加单个活动的URI
		 */
		public static final String TO_ADD_ACTIVITY_URI = "/toaddactivity";
		
		/**
		 * 添加活动界面
		 */
		public static final String ADD_ACTIVITY_PAGE = "/activity/addactivity";
		
		
		/**
		 * 管理员查看 微信账户的建议
		 */
		public static final String TO_SUGGEST_URI = "/tosuggest";
		
		/**
		 * 管理员查看 微信账户的建议
		 */
		public static final String TO_SUGGEST_PAGE = "/suggest/list";
		
		
	}

	/** 商家账户控制界面
	 * @author yanchaoxi
	 *
	 */
	public static class MchUserPage extends CommonPage {
	
		/**
		 * 商家账户修改密码 URI
		 */
		public static final String UPDATE_USERINFO_URI = "/updateuserinfo";
		
		/**
		 * 商家访问的兑换码界面的URI
		 */
		public static final String TO_REDEEMCODE_LIST_URI = "/toredeemcodelist";
		
		/**
		 * 商家置换兑换码URI
		 */
		public static final String CHANGE_REDEEMCODE_URI = "/changeredeemcode";
		
		/**
		 * 商家置换兑换码URI
		 */
		public static final String RESEND_REDEEMCODE_URI = "/resendredeemcode";

		/**
		 * 商家 的兑换码界面
		 */
		public static final String REDEEMCODE_LIST_PAGE = "/redeemcode/list";
		
		/**
		 * 商家访问的所有商品界面的URI
		 */
		public static final String TO_PRODUCT_LIST_URI = "/toproductlist";

		/**
		 * 商家 的兑换码界面
		 */
		public static final String PRODUCT_LIST_PAGE = "/product/list";

		/**
		 * 商家用户 查询单个 URI
		 */
		public static final String TO_SINGLEPRODUCT_URI = "/tosingleproduct";
		
		/**
		 * 商家用户 跳转到添加单个商品 URI
		 */
		public static final String TO_ADD_PRODUCT_URI = "/toaddproduct";
		
		/**
		 * 商家用户 添加单个商品 URI
		 */
		public static final String ADD_PRODUCT_URI = "/addproduct";
		
		/**
		 * 商家用户更新单个商品的状态的URI
		 */
		public static final String SET_PRODUCT_STATUS_URI = "/setproductstatus";
		
		/**
		 * 商家用户编辑单个商品的URI
		 */
		public static final String EDIT_PRODUCT_URI = "/editproduct";
		
		/**
		 * 商家用户 预添加单个商品 URI
		 */
		public static final String PREADD_PRODUCT_URI = "/preaddproduct";
		
		/**
		 * 商家用户 添加单个商品
		 */
		public static final String ADD_PRODUCT_PAGE = "/product/addproduct";
		
		/**
		 * 单个商品
		 */
		public static final String SINGLE_PRODUCT_PAGE = "/product/singleproduct";
		
		/**
		 * 商家账户编辑自己的信息
		 */
		public static final String TO_INFO_URI = "/tomchuserinfo";
		
		/**
		 * 商家账户个人信息页面
		 */
		public static final String INFO_PAGE = "/info";
		
		/**
		 * 商家导出报表
		 */
		public static final String EXPORT_DATA = "/exportdata";
		
		/**
		 * 商家查看 微信账户的建议
		 */
		public static final String TO_SUGGEST = "/tosuggest";
		
		/**
		 * 商家查看微信账户意见的界面
		 */
		public static final String SUGGEST_PAGE = "/suggest";
		
		/**
		 * 商家查看所有活动
		 */
		public static final String TO_ACTIVITY_LIST = "/toactivitylist";
		
		/**
		 * 活动界面
		 */
		public static final String ACTIVITY_PAGE = "/activity/list";
		
		/**
		 * 商家查看员工uri
		 */
		public static final String TO_STAFF_LIST = "/tostafflist";
		
		/**
		 * 商家查看员工uri
		 */
		public static final String TO_WECHAT_LIST = "/towechatusers";
		
		/**
		 * 员工列表界面
		 */
		public static final String STAFF_LIST_PAGE = "/staff/list";
		
		public static final String WECHAT_LIST_PAGE = "/staff/wechatlist";
		
		
		/**
		 * 商家用户 添加、更新某个员工
		 */
		public static final String ADD_OR_UPDATE_STAFF_URI = "/addorupdatestaff";
		
		/**
		 * 商家用户注销某个staff
		 */
		public static final String DELETE_STAFF_URI = "/deletestaff";
		
		
		/**
		 * 商家用户 添加、更新某个员工
		 */
		public static final String TO_SINGLESTAFF_URI = "/tosinglestaff";
		
		/**
		 * 商家的单个员工页面
		 */
		public static final String SINGLESTAFF_PAGE = "/staff/singlestaff";
			
	}

	/** 微信账户控制界面
	 * @author yanchaoxi
	 *
	 */
	public static class WechatUserPage {
		//微信端 商品公共目录
		public static final String PRODUCT_COMMON_PAGE = "/product/";
		
		//微信用户 查询美食的URI
		public static final String TO_PRODUCT_LIST_URI = "/toproductlist";
		
		//微信用户 加载更多美食的URI
		public static final String TO_MORE_PRODUCT_URI = "/getmoreproduct";
		
		//微信用户关注一个商家的URI
		public static final String FOLLOW_MCH_URI = "/followmch";
		
		//微信用户 查询单个美食URI
		public static final String TO_SINGLE_PRODUCT_URI = "/tosingleproduct";
		
		//购买页面
		public static final String BUYPRODUCT_PAGE = "/product/buyproduct";
		
		//购买排队码页面
		public static final String BUY_QUEUE_ORDER_PAGE = "/product/buyqueueorder";
		
		//微信用户 交互的URI
		public static final String INTERACTIVE_URI = "/interactive";
		
		//微信用户 跳转到购买页面
		public static final String TO_BUYPRODUCT_URI = "/tobuyproduct";
		
		//微信用户跳转到关于我们界面
		public static final String TO_ABOUTUS_URI = "/toaboutus";
		
		//微信用户跳转到购买取号码界面
		public static final String TO_BUYQUEUEORDER_URI = "/tobuyqueueorder";
		
		//微信用户 购票行为
		public static final String BUYPRODUCT_URI = "/buyproducturi";
		
		//微信用户付费 取号
		public static final String PAYFOR_QUEUEORDER_URI = "/buyqueueorder";
		
		//微信用户跳转到 联系我们
		public static final String TO_CONTACTUS_URI = "/tocontactus";
		
		//微信用户跳转到 联系我们
		public static final String TO_FAVORITE_MCHS_URI = "/tofavoritemchs";
		
		//微信用户跳转到自己关注的商家列表
		public static final String TO_QUEUE_URI = "/toqueue";
		
		//微信用户跳转到自己信息页面
		public static final String TO_SELFINFO_URI = "/toselfinfo";
		
		//微信用户修改排队信息，普通用户取消，商家完成某个排队，或者从等待中移动到服务中
		public static final String ORDER_CHANGE_URI = "/orderchange";
		
		//微信用户查看所有商家
		public static final String TO_MCHLIST_URI = "/tomchlist";
		
		//微信用户查看所有商家
		public static final String TO_FOLLOW_MCHLIST_URI = "/tofollowmchlist";
		
		//微信用户搜索商家
		public static final String SEARCH_MCHS_URI = "/searchmchs";
		
		//微信用户搜索商家
		public static final String SEARCH_MCHS_ASYNC_URI = "/searchmchsasync";
		
		//微信用户点击查看单一商家界面
		public static final String TO_SINGLE_MCH_URI = "/tosinglemch";
		
		//微信用户点击查看单一商家界面
		public static final String TO_PRODUCTS_OF_ONE_MCH_URI = "/toproductsonemch";
		
		//staff员工通过微信点击完成订单
		public static final String TO_FINISH_TRANSACTION_URI = "/tofinishtransaction";
		
		//staff员工通过微信完成订单
		public static final String FINISH_TRANSACTION_URI = "/finishtransaction";
		
		//微信用户联系我们页面
		public static final String CONTACTUS_PAGE = "/feedback";
		
		//微信用户关注商家页面
		public static final String FAVORITE_MCHS_PAGE = "/favoritemchs";
		
		//微信用户到个人兑换码界面
		public static final String TO_REDEMMCODE_URI = "/toredeemcode";
		
		//微信用户个人兑换码界面
		public static final String REDEEMCODE_PAGE = "/reddemcode";

		//微信用户提交一条意见
		public static final String ADD_SUGGEST_URI = "/addsuggest";
		
		//提交意见成功页面
		public static final String SUCCESS_SUGGEST_PAGE = "/feedbacksuccess";
		
		//微信账户的 支付界面
		public static final String PAY_PAGE = "/wxpay/paypage";
		
		//staff 查订单界面
		public static final String TRANSACTION_LIST_STAFF_PAGE = "/transactionliststaff";
		
		//wechat 查订单界面
		public static final String TRANSACTION_LIST_WECHAT_PAGE = "/transactionlistwechat";
		
		//wechatUser 查订自己的订单界面
		public static final String TO_WECHT_TRANSACTIONS_URI = "/totransactionlistwechat";
		
		//wechatUser 查订自己的订单界面
		public static final String TO_WECHT_SINGLE_TRANS_URI = "/totransactionwechat";
		
		//微信用户查询自己的排队情况 界面
		public static final String WECHAT_QUEUE_PAGE = "/wechatqueuelist";
		
		//商家管理自己的排队情况
		public static final String MCH_QUEUE_PAGE = "/mchqueuelist";
		
		//商家列表页面
		public static final String MCH_LIST_PAGE = "/mchlist";
		
		//商家列表页面
		public static final String MCH_SELFINFO_PAGE = "/mchselfinfo";
		//商家列表页面
		public static final String WECHAT_SELFINFO_PAGE = "/wechatselfinfo";
		
		//商家列表页面
		public static final String FOLLOW_MCH_LIST_PAGE = "/followmchlist";
		
		//单一商家界面
		public static final String MCH_SINGLE_PAGE = "/mchsingle";
		
		//单一商家的所有商品
		public static final String PRODUCTS_ONE_MCH_PAGE = "/productlist";
		
		//单一订单页面，staff用
		public static final String TRANSACTION_STAFF_ONE_PAGE = "/transsinglestaff";
		
		//单一订单页面，wechat用
		public static final String TRANSACTION_WECHAT_ONE_PAGE = "/transsinglewechat";
		
		/**
		 * 微信 支付成功后的回调 URL
		 */
		public static final String WECHAT_PAY_CALL_BACK = "/paycallback";
		
	}

	//公共部分配置
	
	/**
	 * 404 页面
	 */
	public static final String ERROR_404_PAGE = "/404";

	/**
	 * 500 页面
	 */
	public static final String ERROR_500_PAGE = "/500";
	
	/**
	 * 403 没权限页面
	 */
	public static final String ERROR_403_PAGE = "/403";

	/**
	 * 每个页面显示的记录数目:每页 10
	 */
	public static final int PAGE_COUNT_SIZE_10 = 10;
	
	/**
	 * 每个页面显示的记录数目:每页 20
	 */
	public static final int PAGE_COUNT_SIZE_20 = 20;
	
	/**
	 * 每个页面显示的记录数目:每页 5
	 */
	public static final int PAGE_COUNT_SIZE_5 = 5;

}
