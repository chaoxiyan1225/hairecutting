package happylife.util.requestandresponse;

import happylife.model.TbMch;
import happylife.model.TbProduct;
import happylife.model.TbQueueRecord;
import happylife.model.TbRedeemCode;
import happylife.model.TbTransactionRecord;
import happylife.model.servicemodel.CacheMchUser;
import happylife.util.cache.MchStaffProductCacheManager;
import happylife.util.config.WeChatConfig;
import happylife.util.config.WinterOrangeSysConf;
import happylife.util.requestandresponse.messagebean.ArticleMicroChannel;
import happylife.util.requestandresponse.messagebean.BaseMessage;
import happylife.util.requestandresponse.messagebean.ImageMessage;
import happylife.util.requestandresponse.messagebean.MusicMessage;
import happylife.util.requestandresponse.messagebean.NewsMessage;
import happylife.util.requestandresponse.messagebean.TextMessage;
import happylife.util.requestandresponse.messagebean.VideoMessage;
import happylife.util.requestandresponse.messagebean.VoiceMessage;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.bag.SynchronizedBag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.alibaba.fastjson.JSONObject;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;

public class WechatMessageUtil {

	private static Log logger = LogFactory.getLog(WechatMessageUtil.class);

	private static Object lock = new Object();

	/**
	 * 解析微信发来的请求（XML）
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> parseXml(HttpServletRequest request)
			throws Exception {
		// 将解析结果存储在HashMap中
		Map<String, String> map = new HashMap<String, String>();
		// System.out.println(request.getCharacterEncoding());
		// 从request中取得输入流
		InputStream inputStream = request.getInputStream();
		// String str = HttpUtils.changeInputStream(inputStream, "utf-8");
		// System.out.println(str);
		// 读取输入流
		SAXReader reader = new SAXReader();
		// Document document = reader.read(new
		// ByteArrayInputStream(str.getBytes("utf-8")));
		Document document = reader.read(inputStream);
		// 得到xml根元素
		Element root = document.getRootElement();
		// 得到根元素的所有子节点
		List<Element> elementList = root.elements();

		// 遍历所有子节点
		for (Element e : elementList)
			map.put(e.getName(), e.getText());

		// 释放资源
		inputStream.close();
		inputStream = null;

		return map;
	}

	/**
	 * 文本消息对象转换成xml
	 * 
	 * @param textMessage
	 *            文本消息对象
	 * @return xml
	 */
	public static String textMessageToXml(TextMessage textMessage) {
		xstream.alias("xml", textMessage.getClass());
		return xstream.toXML(textMessage);
	}

	/**
	 * 音乐消息对象转换成xml
	 * 
	 * @param musicMessage
	 *            音乐消息对象
	 * @return xml
	 */
	public static String musicMessageToXml(MusicMessage musicMessage) {
		xstream.alias("xml", musicMessage.getClass());
		return xstream.toXML(musicMessage);
	}

	/**
	 * 图文消息对象转换成xml
	 * 
	 * @param newsMessage
	 *            图文消息对象
	 * @return xml
	 */
	public static String newsMessageToXml(NewsMessage newsMessage) {
		xstream.alias("xml", newsMessage.getClass());
		xstream.alias("item", new ArticleMicroChannel().getClass());
		return xstream.toXML(newsMessage);
	}

	/**
	 * 视频消息对象转换成xml
	 * 
	 * @param
	 * @return xml
	 */
	public static String videoMessageToXml(VideoMessage videoMessage) {
		xstream.alias("xml", videoMessage.getClass());
		return xstream.toXML(videoMessage);
	}

	/**
	 * 语音消息对象转换成xml
	 * 
	 * @param
	 * @return xml
	 */
	public static String voiceMessageToXml(VoiceMessage voiceMessage) {
		xstream.alias("xml", voiceMessage.getClass());
		return xstream.toXML(voiceMessage);
	}

	/**
	 * 图片消息对象转换成xml
	 * 
	 * @param
	 * @return xml
	 */
	public static String imageMessageToXml(ImageMessage imageMessage) {
		xstream.alias("xml", imageMessage.getClass());
		xstream.alias("item", new ArticleMicroChannel().getClass());
		return xstream.toXML(imageMessage);
	}

	/**
	 * 公共消息转为xml
	 * 
	 * @param baseMessage
	 *            公共消息
	 * @return
	 */
	public static String baseMessageToXml(BaseMessage baseMessage) {
		xstream.alias("xml", baseMessage.getClass());
		return xstream.toXML(baseMessage);
	}

	/**
	 * 扩展xstream，使其支持CDATA块
	 * 
	 * @date 2013-05-19
	 */
	@SuppressWarnings("rawtypes")
	private static XStream xstream = new XStream(new XppDriver() {
		public HierarchicalStreamWriter createWriter(Writer out) {
			return new PrettyPrintWriter(out) {
				// 对所有xml节点的转换都增加CDATA标记
				boolean cdata = true;

				public void startNode(String name, Class clazz) {
					super.startNode(name, clazz);
				}

				protected void writeText(QuickWriter writer, String text) {
					if (cdata) {
						writer.write("<![CDATA[");
						writer.write(text);
						writer.write("]]>");
					} else {
						writer.write(text);
					}
				}
			};
		}
	});

	/**
	 * 返回内容长度
	 * 
	 * @param content
	 * @return
	 */
	public static int getByteSize(String content) {
		int size = 0;
		if (null != content) {
			try {
				// 汉字采用utf-8编码时占3个字节
				// 汉子采用ISO8859-1编码时占1个字节
				// 汉字采用GBK编码时占2个字节
				// 汉字采用GB2312编码时占2个字节
				size = content.getBytes("utf-8").length;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return size;
	}

	/**
	 * 换行
	 * 
	 * @return
	 */
	public static String getMainMenu() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("您好，我是小q，请回复数字选择服务：").append("\n\n");
		buffer.append("1  天气预报").append("\n");
		buffer.append("2  公交查询").append("\n");
		buffer.append("3  周边搜索").append("\n");
		buffer.append("4  歌曲点播").append("\n");
		buffer.append("5  经典游戏").append("\n");
		buffer.append("6  美女电台").append("\n");
		buffer.append("7  人脸识别").append("\n");
		buffer.append("8  聊天唠嗑").append("\n\n");
		buffer.append("回复“?”显示此帮助菜单");
		return buffer.toString();
	}

	// 发送链接采用双引号
	public static void link() {
		// String content =
		// "如有问题，请点击<a href=\"http://blog.csdn.net/lyq8479?open_id=dd"
		// + "\">此处</a>";
	}

	/**
	 * 将long转换成时间字符串
	 * 
	 * @param longTime
	 * @return
	 */
	public static String formatTime(long longTime) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(new Date(longTime));
	}

	/**
	 * 将微信消息中的CreateTime转换成标准格式的时间（yyyy-MM-dd HH:mm:ss）
	 * 
	 * @param createTime
	 *            消息创建时间
	 * @return
	 */
	public static String formatTime(String createTime) {
		// 将微信传入的CreateTime转换成long类型，再乘以1000
		long msgCreateTime = Long.parseLong(createTime) * 1000L;
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format.format(new Date(msgCreateTime));
	}

	/**
	 * 用 缓存中的 商户信息封装 美食列表界面的元素
	 * 
	 * @param mchUsers
	 * @return
	 */
	public static String generateHTMLFoodList(List<CacheMchUser> mchUsers) {
		if (null == mchUsers || mchUsers.size() == 0) {
			logger.error("paramets error ,can not be null !");
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (CacheMchUser mchUser : mchUsers) {
			// <c:forEach items="${mchUsers}" var="mchUser">
			sb.append("<dl class=\"list\" gaevent=\"common/poilist\">");
			sb.append("<dd class=\"poi-list-item\">");
			sb.append("<a class=\"react\" href=\"#\" data-ctpoi=\"792766175882313728_a73227298_c5_e2978301047565630733\">");
			sb.append("<span class=\"poiname\" style=\"font-size: 22px;\">")
					.append(mchUser.getMchUser().getShopName())
					.append("</span>");
			sb.append("<div class=\"kv-line-r\">");
			sb.append("<h6><span class=\"stars\"><em class=\"star-text\">");
			sb.append("<i class=\"text-icon icon-star\"/><i class=\"text-icon icon-star\"/><i class=\"text-icon icon-star\"/><i class=\"text-icon icon-star\"/><i class=\"text-icon icon-star\"/><i class=\"text-icon icon-star\"/>5星</em></span>");
			sb.append("</h6>");
			sb.append("</div>");
			sb.append("</a>");
			sb.append("</dd>");
			sb.append("<dd>");
			sb.append("<dl class=\"list0\">");
			sb.append("<!-- 商家下面的所有商品 列表 开始 符号 -->");

			List<TbProduct> products = mchUser.getProducts();
			for (TbProduct product : products) {
				// 下架商品不对微信账户显示
				if (product.getProductStatus() == 0)
					continue;

				sb.append("<dd>");
				sb.append("<a href=\"tosingleproduct?type=food&productId=")
						.append(product.getProductId())
						.append("\" class=\"react \">");
				sb.append("<div class=\"dealcard dealcard-poi\">");
				sb.append("<span class=\"dealcard-nobooking\"></span>");
				sb.append("<div  style=\"background: transparent none repeat scroll 0% 0%;\" class=\"dealcard-img imgbox\">");
				sb.append("<img src=\"")
						.append(product.getProductHeadPicture())
						.append("\" style=\"width: 100%;\" />");
				sb.append("</div>");
				sb.append("<div class=\"dealcard-block-right\">");
				sb.append("<div class=\"title text-block\">")
						.append(product.getProductName()).append("</div>");
				sb.append("<div class=\"price\">");
				sb.append("<span class=\"strong\" style=\"color: red\">折扣价: ")
						.append(product.getDiscountPrice()).append("</span>");
				sb.append(
						"<span class=\"strong-color\" style=\"color: red\">元</span> <span class=\"tag\">门店价")
						.append(product.getProductPrice()).append("元</span>");
				sb.append("</div>");
				sb.append("</div>");
				sb.append("</div>");
				sb.append("</a>");
				sb.append("<p class=\"posi-right-bottom\">");
				sb.append(
						"<a class=\"statusInfo\" onclick=\"return false;\" href=\"#\">已售<span class=\"strong\">")
						.append(product.getSaleTotalTimes())
						.append("</span></a>");
				sb.append("</p>");
				sb.append("</dd>");
				sb.append("<!-- 商家下面的所有商品 结束 符号 -->");
			}
			// <c:forEach items="${mchUser.products}" var="product">
			sb.append("</dl>");
			sb.append("</dd>");
			sb.append("</dl>");
			sb.append("<!-- 每个商家的结束符 -->");
		}

		return sb.toString();

	}
	
	/**
	 * 用 缓存中的 商户信息封装 美食列表界面的元素
	 * 
	 * @param mchUsers
	 * @return
	 */
	public static String generateHTMLMchList(List<CacheMchUser> mchUsers) {
		if (null == mchUsers || mchUsers.size() == 0) {
			logger.error("paramets error ,can not be null !");
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (CacheMchUser mchUser : mchUsers) {
			// <c:forEach items="${mchUsers}" var="mchUser">
			sb.append("<dl class=\"list\" gaevent=\"common/poilist\">");
			sb.append("<dd class=\"poi-list-item\">");
			sb.append("<a class=\"react\" href=\"tosinglemch?mchId="+mchUser.getMchUser().getMchId()+"\" data-ctpoi=\"792766175882313728_a73227298_c5_e2978301047565630733\">");
			sb.append("<span class=\"poiname\" style=\"font-size: 22px; font-family: Microsoft YaHei\"><b>")
					.append(mchUser.getMchUser().getShopName())
					.append("</b></span>");
			sb.append("<div class=\"kv-line-r\">");
			sb.append("<h6><span class=\"stars\"><em class=\"star-text\">");
			
			for(int i=0;i<mchUser.getMchUser().getPoints();i++){
				sb.append("<i class=\"text-icon icon-star\"/>");
			}
			
			sb.append(mchUser.getMchUser().getPoints()).append("星</em></span>");
			sb.append("</h6>");
			sb.append("</div>");
			sb.append("</a>");
			sb.append("</dd>");
			sb.append("<dd>");
			sb.append("<dl class=\"list\">");
			sb.append("<dd>");
			sb.append("<a href=\"tosinglemch?mchId="+mchUser.getMchUser().getMchId()+"class=\"react \">");
			sb.append("<div class=\"dealcard dealcard-poi\">");
			sb.append("<div style=\"background: transparent none repeat scroll 0% 0%;\" class=\"dealcard-img imgbox\"> <img src=\"/haircutting/"+mchUser.getContentPictures()[0]+"\" style=\"width: 100%;\" />");
			sb.append("</div>");
			sb.append("<div class=\"dealcard-block-right\">");
			sb.append("<div class=\"title text-block\">"+mchUser.getMchUser().getShopAddress()+"</div>");
			sb.append("<div class=\"price\">");
			sb.append("<span class=\"strong\" style=\"color: red\">"+mchUser.getMchUser().getMchStatus()+"</span>");
			sb.append("<span  class=\"tag\"  style=\"color: #fff; border-color: #E52010; background-color: #E52010;\">进入取号</span>");
			sb.append("</div>");
			sb.append("</div>");
			sb.append("</div>");
			sb.append("</a>");
			sb.append("<p class=\"posi-right-bottom\">");
			sb.append("<a class=\"statusInfo\" onclick=\"return false;\" href=\"#\">累计排队<span  class=\"strong\">"+mchUser.getMchUser().getTotalSaleCount()+"次</span></a>");
			sb.append("</p>");
			sb.append("</dd>");
			sb.append("</dl>");
			sb.append("</dd>");
			sb.append("</dl>");
			sb.append("<!-- 每个商家的结束符 -->");
		}

		return sb.toString();

	}
	

	/**
	 * 给用户发送 兑换码信息
	 * 
	 * @return
	 */
	public static JSONObject sendRedeemCodeInfoMsg(TbRedeemCode redeemCode) {
		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}

		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				getAccessTokent());

		String json = "{\"data\":{\"first\":{\"value\":\"您好！购票成功\"},\"info\":{\"value\":\"INFO\"},"
				+ "\"code\":{\"value\":\"CODE\"},\"time\":{\"value\":\"无\"},\"product\":{\"value\":\"电影票\"},\"remark\":{\"value\":\""
				+ "感谢阁下惠顾！兑换码有效期45日。兑换规则详见抢票指南\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\"}";

		json = json.replaceAll("INFO", redeemCode.getCodeInfo())
				.replaceAll("CODE", redeemCode.getRandomCode())
				.replaceAll("TOUSER", redeemCode.getFkOpenId());

		return HttpUtil.httpsRequest(url, "POST", json);
	}

	/**
	 * 给员工发送订单信息
	 * 
	 * @return
	 */
	public static JSONObject sendTransactionToStaff(TbTransactionRecord record,
			String fkStaffOpenId) {
		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}

		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				getAccessTokent());

		String json = "{\"data\":{\"first\":{\"value\":\"您好！您收到新的订单安排。\"},\"info\":{\"value\":\"INFO\"},"
				+ "\"code\":{\"value\":\"CODE\"},\"time\":{\"value\":\"无\"},\"product\":{\"value\":\"电影票\"},\"remark\":{\"value\":\""
				+ "请您登陆公众号查看待完成订单\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\"}";

		json = json.replaceAll("INFO", record.getOutTradeNo())
				.replaceAll("CODE", record.getOutTradeNo())
				.replaceAll("TOUSER", fkStaffOpenId);

		logger.info("now send sendTransactionToStaff, request=" + json);
		JSONObject response = HttpUtil.httpsRequest(url, "POST", json);

		logger.info("now send sendTransactionToStaff ,response="
				+ response.toJSONString());

		return response;
	}

	/**
	 * 给商家发送排号取消信息
	 * 
	 * @return
	 */
	public static JSONObject sendCancelOrderToMch(TbQueueRecord queueOrder) {
		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}

		logger.info("now send cancelOrder to mch:" + queueOrder.toString());

		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				getAccessTokent());

//		String json = "{\"data\":{\"first\":{\"value\":\"您好！您收到取消排号信息。\"},\"info\":{\"value\":\"INFO\"},"
//				+ "\"code\":{\"value\":\"CODE\"},\"time\":{\"value\":\"无\"},\"remark\":{\"value\":\""
//				+ "请您登陆管理后台查看\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
//				+ "\"touser\":\"TOUSER\"}";

		TbMch mchUser = MchStaffProductCacheManager.getInstance()
				.getMchUserById(queueOrder.getFkMchId());

		if (null == mchUser) {
			logger.error("the mch not exit, mchId=" + queueOrder.getFkMchId());
			return null;
		}

		String openId = mchUser.getFkOpenId();
		
		String json = "{\"data\":{\"first\":{\"value\":\"您好！您收到取消排号信息。\"},\"keyword1\":{\"value\":\"INFO\" ,\"color\":\"#173177\"},"
				+ "\"keyword2\":{\"value\":\"CODE\" ,\"color\":\"#173177\"},\"keyword3\":{\"value\":\"SHOPNAME\" ,\"color\":\"#173177\"},\"remark\":{\"value\":\""
				+ "请您登陆管理后台查看\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\","
				+ "\"topcolor\":\"#FF0000\"}";

		String shopName = MchStaffProductCacheManager.getInstance().getMchUserById(queueOrder.getFkMchId()).getShopName();
		
		json = json.replaceAll("INFO", queueOrder.getOrderId())
				.replaceAll("CODE", "用户取消排号")
				.replaceAll("SHOPNAME",shopName)
				.replaceAll("TOUSER", openId);
		
		

		logger.info("now send sendCancelOrderToMch, request=" + json);
		JSONObject response = HttpUtil.httpsRequest(url, "POST", json);

		logger.info("now send sendCancelOrderToMch to mch user,response="
				+ response.toJSONString());

		return response;
	}

	/**
	 * 给用户发送取号服务完成信息
	 * 
	 * @return
	 */
	public static JSONObject sendFinishOrderToWechat(TbQueueRecord queueOrder) {
		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}

		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				getAccessTokent());

//		String json = "{\"data\":{\"first\":{\"value\":\"您好！您收到取号服务完成通知。\"},\"info\":{\"value\":\"INFO\"},"
//				+ "\"code\":{\"value\":\"CODE\"},\"time\":{\"value\":\"无\"},\"remark\":{\"value\":\""
//				+ "期望下次继续为您服务，你可以通过意见反馈与我们互动！\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
//				+ "\"touser\":\"TOUSER\"}";
//
//		json = json.replaceAll("INFO", queueOrder.getOrderId())
//				.replaceAll("CODE", queueOrder.getFkOpenId())
//				.replaceAll("TOUSER", queueOrder.getFkOpenId());
		
		
		String json = "{\"data\":{\"first\":{\"value\":\"您好！您收到服务完成通知。\"},\"keyword1\":{\"value\":\"INFO\" ,\"color\":\"#173177\"},"
				+ "\"keyword2\":{\"value\":\"CODE\" ,\"color\":\"#173177\"},\"keyword3\":{\"value\":\"SHOPNAME\" ,\"color\":\"#173177\"},\"remark\":{\"value\":\""
				+ "期望下次继续为您服务，你可以通过意见反馈与我们互动！\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\","
				+ "\"topcolor\":\"#FF0000\"}";

		String shopName = MchStaffProductCacheManager.getInstance().getMchUserById(queueOrder.getFkMchId()).getShopName();
		
		json = json.replaceAll("INFO", queueOrder.getOrderId())
				.replaceAll("CODE", "已完成服务")
				.replaceAll("SHOPNAME",shopName)
				.replaceAll("TOUSER", queueOrder.getFkOpenId());
		

		logger.info("now send sendFinishOrderToWechat, request=" + json);
		JSONObject response = HttpUtil.httpsRequest(url, "POST", json);
		logger.info("now send sendFinishOrderToWechat,response="
				+ response.toJSONString());

		return response;
	}

	/**
	 * 给用户发送信息：当前排号已经进去服务中了
	 * 
	 * @return
	 */
	public static JSONObject sendInRunningOrderToWechat(TbQueueRecord queueOrder) {
		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}

		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				getAccessTokent());

//		//String json = "{\"data\":{\"first\":{\"value\":\"您好！您的排号已经到号，请确认已入店。\"},\"info\":{\"value\":\"INFO\"},"
//				+ "\"code\":{\"value\":\"CODE\"},\"time\":{\"value\":\"无\"},\"remark\":{\"value\":\""
//				+ "当前喊号已经到您了，请您尽快确认到店享受服务！\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
//				+ "\"touser\":\"TOUSER\"}";
		
		String json = "{\"data\":{\"first\":{\"value\":\"您好！您的排号已经到号，请确认已入店。\"},\"keyword1\":{\"value\":\"INFO\" ,\"color\":\"#173177\"},"
				+ "\"keyword2\":{\"value\":\"CODE\" ,\"color\":\"#173177\"},\"keyword3\":{\"value\":\"SHOPNAME\" ,\"color\":\"#173177\"},\"remark\":{\"value\":\""
				+ "当前喊号已经到您了，请您尽快确认到店享受服务！\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\","
				+ "\"topcolor\":\"#FF0000\"}";

		String shopName = MchStaffProductCacheManager.getInstance().getMchUserById(queueOrder.getFkMchId()).getShopName();
		
		json = json.replaceAll("INFO", queueOrder.getOrderId())
				.replaceAll("CODE", "您前面的等候人数还剩：0位")
				.replaceAll("SHOPNAME",shopName)
				.replaceAll("TOUSER", queueOrder.getFkOpenId());

		logger.info("now send sendInRunningOrderToWechat, request=" + json);
		JSONObject response = HttpUtil.httpsRequest(url, "POST", json);
		logger.info("now send sendInRunningOrderToWechat,response="
				+ response.toJSONString());

		return response;
	}

	/**
	 * 给用户发送信息：需要提前准备马上就排号过期
	 * 
	 * @return
	 */
	public static JSONObject sendPrepareMsgToWechat(TbQueueRecord queueOrder,
			int waitNum) {

		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}

		logger.info("now start sendPrepareMsgToWechat.....");
		String accessToken = getAccessTokent();
		logger.info("accessToken=" + accessToken);
		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				accessToken);

		String json = "{\"data\":{\"first\":{\"value\":\"您好！您的排号信息有了更新，请关注。\"},\"keyword1\":{\"value\":\"INFO\" ,\"color\":\"#173177\"},"
				+ "\"keyword2\":{\"value\":\"CODE\" ,\"color\":\"#173177\"},\"keyword3\":{\"value\":\"SHOPNAME\" ,\"color\":\"#173177\"},\"remark\":{\"value\":\""
				+ "请您务必提前到店，防止过号！\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\","
		        + "\"topcolor\":\"#FF0000\"}";
		
		String shopName = MchStaffProductCacheManager.getInstance().getMchUserById(queueOrder.getFkMchId()).getShopName();
	
		json = json.replaceAll("INFO", queueOrder.getOrderId())
				.replaceAll("CODE", "您前面的等候人数还剩：" + waitNum + "位")
				.replaceAll("SHOPNAME",shopName)
				.replaceAll("TOUSER", queueOrder.getFkOpenId());
		logger.info("now send sendPrepareMsgToWechat, request=" + json);
		JSONObject response = HttpUtil.httpsRequest(url, "POST", json);
		logger.info("now send sendPrepareMsgToWechat response="
				+ response.toJSONString());

		return response;
	}
	
	
	/**
	 * 给用户发送信息：需要排号成功信息
	 * 
	 * @return
	 */
	public static JSONObject sendOrderSuccessMsgToWechat(TbQueueRecord queueOrder,
			int waitNum) {
		
		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}
		
		logger.info("now start sendOrderSuccessMsgToWechat.....");
		String accessToken = getAccessTokent();
		logger.info("accessToken=" + accessToken);
		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				accessToken);
		
		String json = "{\"data\":{\"first\":{\"value\":\"您好！您取号成功。\"},\"keyword1\":{\"value\":\"INFO\" ,\"color\":\"#173177\"},"
				+ "\"keyword2\":{\"value\":\"CODE\" ,\"color\":\"#173177\"},\"keyword3\":{\"value\":\"SHOPNAME\" ,\"color\":\"#173177\"},\"remark\":{\"value\":\""
				+ "请您务必提前到店，防止过号！\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\","
				+ "\"topcolor\":\"#FF0000\"}";
		
		String shopName = MchStaffProductCacheManager.getInstance().getMchUserById(queueOrder.getFkMchId()).getShopName();
		
		json = json.replaceAll("INFO", queueOrder.getOrderId())
				.replaceAll("CODE", "您前面的人数：" + waitNum + "位")
				.replaceAll("SHOPNAME",shopName)
				.replaceAll("TOUSER", queueOrder.getFkOpenId());
		logger.info("now send sendPrepareMsgToWechat, request=" + json);
		JSONObject response = HttpUtil.httpsRequest(url, "POST", json);
		logger.info("now send sendPrepareMsgToWechat response="
				+ response.toJSONString());
		
		return response;
	}

	public static void main(String[] args) {
		TbQueueRecord record = new TbQueueRecord();
		int waitNum = 1;

		record.setRecordId(18);
		record.setOrderId("A-1");
		record.setFkOpenId("oiTiL6YV7D5UIgG_AlF02T4lECEM");

		JSONObject json = sendPrepareMsgToWechat(record, 1);

		System.out.print(json.toJSONString());

	}

	/**
	 * 给用户发送信息：排号过期
	 * 
	 * @return
	 */
	public static JSONObject sendOutingMsgToWechat(TbQueueRecord queueOrder) {
		// 测试模式 直接返回
		if (WinterOrangeSysConf.IS_TEST_VALID) {
			logger.warn("int test model ,no need send wechat msg ");
			return null;
		}

		String url = WeChatConfig.WEIXIN_TEMPLATE_MSG.replace("ACCESS_TOKEN",
				getAccessTokent());

		String json = "{\"data\":{\"first\":{\"value\":\"您好！您已过号，请关注。\"},\"info\":{\"value\":\"INFO\"},"
				+ "\"code\":{\"value\":\"CODE\"},\"time\":{\"value\":\"无\"},\"remark\":{\"value\":\""
				+ "您当前已过号，请您到店确认，感谢！\"}},\"template_id\":\""+WeChatConfig.TEMPLATE_ID+"\","
				+ "\"touser\":\"TOUSER\"}";

		json = json.replaceAll("INFO", queueOrder.getOrderId())
				.replaceAll("CODE", "您当前已经过号")
				.replaceAll("TOUSER", queueOrder.getFkOpenId());

		JSONObject response = HttpUtil.httpsRequest(url, "POST", json);
		logger.info("now send sendOutingMsgToWechat, request=" + json
				+ ",response=" + response.toJSONString());

		return response;
	}

	/**
	 * 给用户发送信息：排号过期
	 * 
	 * @return
	 */
	public static String getAccessTokent() {
		// 测试模式 直接返回

		String url = WeChatConfig.ACCESS_TOKEN_URL;
		url = url.replaceAll("APPID", WeChatConfig.APP_ID).replaceAll(
				"APPSECRET", WeChatConfig.APP_SECRET);

		logger.info("start get access token....");

		long currMillis = System.currentTimeMillis();

		synchronized (lock) {
			// 两个小时过期
			if (currMillis - WeChatConfig.ACCESS_TOKEN_TIMEOUT_MISSECS > 7000000) {

				logger.info("now start getAccessToken, url=" + url);
				JSONObject response = HttpUtil.httpsRequest(url, "GET", null);
				logger.info("now send sendOutingMsgToWechat,url=" + url
						+ ",response=" + response.toJSONString());
				String access_token = null;
				if (null != response) {
					access_token = response.getString("access_token");
					if (StringUtils.isNotBlank(access_token)) {
						WeChatConfig.APP_ACCESS_TOKEN = access_token;
						WeChatConfig.ACCESS_TOKEN_TIMEOUT_MISSECS = System
								.currentTimeMillis();

						return access_token;
					} else {
						logger.error("get access token error ,is null ");
						return WeChatConfig.APP_ACCESS_TOKEN;
					}
				}

			}

			logger.warn("get access token error,return the defaut value");
			return WeChatConfig.APP_ACCESS_TOKEN;
		}

	}

	public static class MessageType {
		/**
		 * 返回消息类型：文本
		 */
		public static final String RESP_MESSAGE_TYPE_TEXT = "text";

		/**
		 * 返回消息类型：音乐
		 */
		public static final String RESP_MESSAGE_TYPE_MUSIC = "music";

		/**
		 * 返回消息类型：图文
		 */
		public static final String RESP_MESSAGE_TYPE_NEWS = "news";

		/**
		 * 返回消息类型：图片
		 */
		public static final String RESP_MESSAGE_TYPE_IMAGE = "image";

		/**
		 * 返回消息类型：语音
		 */
		public static final String RESP_MESSAGE_TYPE_VOICE = "voice";

		/**
		 * 返回消息类型：视频
		 */
		public static final String RESP_MESSAGE_TYPE_VIDEO = "video";

		/**
		 * 客服系统
		 */
		public static final String RESP_TRANSFER_CUSTOMER_SERVICE = "transfer_customer_service";

		/**
		 * 请求消息类型：文本
		 */
		public static final String REQ_MESSAGE_TYPE_TEXT = "text";

		/**
		 * 请求消息类型：图片
		 */
		public static final String REQ_MESSAGE_TYPE_IMAGE = "image";

		/**
		 * 请求消息类型：链接
		 */
		public static final String REQ_MESSAGE_TYPE_LINK = "link";

		/**
		 * 请求消息类型：地理位置
		 */
		public static final String REQ_MESSAGE_TYPE_LOCATION = "location";

		/**
		 * 请求消息类型：音频
		 */
		public static final String REQ_MESSAGE_TYPE_VOICE = "voice";

		/**
		 * 请求消息类型:视频
		 */
		public static final String REQ_MESSAGE_TYPE_VIDEO = "video";

		/**
		 * 请求消息类型：推送
		 */
		public static final String REQ_MESSAGE_TYPE_EVENT = "event";

		/**
		 * 事件类型：subscribe(订阅)
		 */
		public static final String EVENT_TYPE_SUBSCRIBE = "subscribe";

		/**
		 * 事件类型：unsubscribe(取消订阅)
		 */
		public static final String EVENT_TYPE_UNSUBSCRIBE = "unsubscribe";

		/**
		 * 事件类型：CLICK(自定义菜单点击事件)
		 */
		public static final String EVENT_TYPE_CLICK = "CLICK";

		/**
		 * 事件类型:scan(扫描二维码)
		 */
		public static final String EVENT_TYPE_SCAN = "SCAN";

		/**
		 * 事件类型:scan(扫描二维码EventKey值)
		 */
		public static final String EVENT_TYPE_SCENE_VALUE = "SCENE_VALUE";

		/**
		 * 事件类型:LOCATION(上报地理位置)
		 */
		public static final String EVENT_TYPE_LOCATION = "LOCATION";
	}

}
