package happylife.util.requestandresponse;

import happylife.model.TbWechatUser;
import happylife.util.MyX509TrustManager;
import happylife.util.StrUtil;
import happylife.util.XmlUtils;
import happylife.util.config.WeChatConfig;
import happylife.util.requestandresponse.messagebean.WeChatOauth2Token;
import happylife.util.requestandresponse.messagebean.WechatPayMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.springframework.ui.ModelMap;

import com.alibaba.fastjson.JSONObject;

/**
 * 高级接口工具类
 * 
 * @author klaus
 * 
 */
public class WeChatRequestUtil {
	private static Log logger = LogFactory.getLog(WeChatRequestUtil.class);

	/**
	 * 获取网页授权凭证
	 * 
	 * @param appId
	 *            公众账号的唯一标识
	 * @param appSecret
	 *            公众账号的密钥
	 * @param code
	 * @return WeixinAouth2Token
	 */
	public static WeChatOauth2Token getOauth2AccessToken(String appId,
			String appSecret, String code) {
		WeChatOauth2Token wat = null;
		// 拼接请求地址
		String requestUrl = WeChatConfig.WEIXIN_ACCESSTOKEN_URL;
		requestUrl = requestUrl.replace("APPID", appId);
		requestUrl = requestUrl.replace("SECRET", appSecret);
		requestUrl = requestUrl.replace("CODE", code);
		// 获取网页授权凭证
		JSONObject jsonObject = httpsRequest(requestUrl, "GET", null);
		if (null != jsonObject) {
			try {
				wat = new WeChatOauth2Token();
				wat.setAccessToken(jsonObject.getString("access_token"));
				wat.setExpiresIn(jsonObject.getIntValue("expires_in"));
				wat.setRefreshToken(jsonObject.getString("refresh_token"));
				wat.setOpenId(jsonObject.getString("openid"));
				wat.setScope(jsonObject.getString("scope"));
			} catch (Exception e) {
				wat = null;
				int errorCode = jsonObject.getIntValue("errcode");
				String errorMsg = jsonObject.getString("errmsg");
				logger.error("failed to getaccessToken errcode=" + errorCode
						+ ",errormessage=" + errorMsg);
			}
		}
		return wat;
	}

	/**
	 * 刷新网页授权凭证
	 * 
	 * @param appId
	 *            公众账号的唯一标识
	 * @param refreshToken
	 * @return WeixinAouth2Token
	 */
	public static WeChatOauth2Token refreshOauth2AccessToken(String appId,
			String refreshToken) {
		WeChatOauth2Token wat = null;
		// 拼接请求地址
		String requestUrl = WeChatConfig.WEIXIN_REFRESHTOKEN_URL;
		requestUrl = requestUrl.replace("APPID", appId);
		requestUrl = requestUrl.replace("REFRESH_TOKEN", refreshToken);
		// 刷新网页授权凭证
		JSONObject jsonObject = httpsRequest(requestUrl, "GET", null);
		if (null != jsonObject) {
			try {
				wat = new WeChatOauth2Token();
				wat.setAccessToken(jsonObject.getString("access_token"));
				wat.setExpiresIn(jsonObject.getIntValue("expires_in"));
				wat.setRefreshToken(jsonObject.getString("refresh_token"));
				wat.setOpenId(jsonObject.getString("openid"));
				wat.setScope(jsonObject.getString("scope"));
			} catch (Exception e) {
				wat = null;
				int errorCode = jsonObject.getIntValue("errcode");
				String errorMsg = jsonObject.getString("errmsg");
				logger.error("failed to getaccessToken errcode=" + errorCode
						+ ",errormessage=" + errorMsg);
			}
		}
		return wat;
	}

	/**
	 * 通过网页授权获取用户信息
	 * 
	 * @param accessToken
	 *            网页授权接口调用凭证
	 * @param openId
	 *            用户标识
	 * @return SNSUserInfo
	 */
	public static TbWechatUser getWeChatUserInfo(String accessToken,
			String openId) {
		// 拼接请求地址
		String requestUrl = WeChatConfig.WEIXIN_GETUSER_URL;
		requestUrl = requestUrl.replace("ACCESS_TOKEN", accessToken).replace(
				"OPENID", openId);
		// 通过网页授权获取用户信息
		JSONObject jsonObject = httpsRequest(requestUrl, "GET", null);

		if (null != jsonObject) {
			try {
				TbWechatUser user = new TbWechatUser();
				user.setUserOpenid(jsonObject.getString("openid"));
				String nickname = jsonObject.getString("nickname").trim();
				nickname = new String(nickname.getBytes(), "utf-8");
				if (nickname.contains("🎀")) {
					nickname = nickname.replaceAll("🎀", "");
				}
				user.setUserNickName(nickname);
				String head = jsonObject.getString("headimgurl");
				if (head != null && !head.equals("")) {
					String headUrl = head.substring(0, head.length() - 1)
							+ "132";
					user.setUserHeadPath(headUrl);
				}
				user.setUserRegisterTime(new Date());
				
				boolean userSex = true;
				
				int sex = Integer.valueOf(jsonObject.getString("sex"));
				if(sex == 2){
					userSex =false;
				}
				
				user.setUserSex(userSex);
				return user;
			} catch (Exception e) {
				int errorCode = jsonObject.getIntValue("errcode");
				String errorMsg = jsonObject.getString("errmsg");
				logger.error("failed to getaccessToken errcode=" + errorCode
						+ ",errormessage=" + errorMsg);
			}
		}
		return null;
	}

	/**
	 * 校验微信传入的signature是否正确
	 * 
	 * @param signature
	 * @param timestamp
	 * @param nonce
	 * @return true OK ，false notOK
	 */
	public static boolean checkWeChatSignature(String signature,
			String timestamp, String nonce) {
		String[] arr = new String[] { WeChatConfig.WECHAT_TOKEN, timestamp,
				nonce };
		// 将token、timestamp、nonce三个参数进行字典序排序
		Arrays.sort(arr);
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			content.append(arr[i]);
		}
		MessageDigest md = null;
		String tmpStr = null;

		try {
			md = MessageDigest.getInstance("SHA-1");
			// 将三个参数字符串拼接成一个字符串进行sha1加密
			byte[] digest = md.digest(content.toString().getBytes());
			tmpStr = byteToStr(digest);
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			return false;
		}

		content = null;
		// 将sha1加密后的字符串可与signature对比，标识该请求来源于微信
		return tmpStr != null ? tmpStr.equals(signature.toUpperCase()) : false;

	}

	/**
	 * 将字节数组转换为十六进制字符
	 * 
	 * @param byteArray
	 * @return
	 */
	private static String byteToStr(byte[] byteArray) {
		String strDigest = "";
		for (int i = 0; i < byteArray.length; i++) {
			strDigest += byteToHexStr(byteArray[i]);
		}
		return strDigest;
	}

	/**
	 * 将单个字节转换为十六进制字符
	 * 
	 * @param mByte
	 * @return
	 */
	private static String byteToHexStr(byte mByte) {
		char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
				'B', 'C', 'D', 'E', 'F' };
		char[] tempArr = new char[2];
		tempArr[0] = Digit[(mByte >>> 4) & 0X0F]; // 取一个字节的高4位，然后获得其对应的十六进制字符
		tempArr[1] = Digit[mByte & 0X0F]; // 取一个字节的低4位，然后获得其对应的十六进制字符

		return new String(tempArr);

	}

	/**
	 * 发起https请求并获取结果
	 * 
	 * @param requestUrl
	 *            请求地址
	 * @param requestMethod
	 *            请求方式（GET、POST）
	 * @param outputStr
	 *            提交的数据
	 * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
	 */
	public static JSONObject httpsRequest(String requestUrl,
			String requestMethod, String outputStr) {
		JSONObject jsonObject = null;
		try {
			System.setProperty("https.protocols", "TLSv1");
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();
			System.setProperty("jsse.enableSNIExtension", "false");
			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url
					.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);

			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if ("GET".equalsIgnoreCase(requestMethod))
				httpUrlConn.connect();

			// 当有数据需要提交时
			if (null != outputStr) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}

			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			String string = HttpUtil.changeInputStream(inputStream, "UTF-8");

			httpUrlConn.disconnect();
			jsonObject = JSONObject.parseObject(string);
		} catch (ConnectException ce) {
			ce.printStackTrace();
			logger.error(ce.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return jsonObject;
	}

	/**
	 * 发起https请求并获取结果
	 * 
	 * @param requestUrl
	 *            请求地址
	 * @param requestMethod
	 *            请求方式（GET、POST）
	 * @param outputStr
	 *            提交的数据
	 * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
	 * @throws ConnectException
	 */
	public static String httpsRequestString(String requestUrl,
			String requestMethod, String outputStr) throws ConnectException {
		JSONObject jsonObject = null;
		try {
			System.setProperty("https.protocols", "TLSv1");
			// 创建SSLContext对象，并使用我们指定的信任管理器初始化
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// 从上述SSLContext对象中得到SSLSocketFactory对象
			SSLSocketFactory ssf = sslContext.getSocketFactory();
			System.setProperty("jsse.enableSNIExtension", "false");
			URL url = new URL(requestUrl);
			HttpsURLConnection httpUrlConn = (HttpsURLConnection) url
					.openConnection();
			httpUrlConn.setSSLSocketFactory(ssf);
			httpUrlConn.setDoOutput(true);
			httpUrlConn.setDoInput(true);
			httpUrlConn.setUseCaches(false);
			httpUrlConn.setConnectTimeout(100 * 1000);
			httpUrlConn.setReadTimeout(100 * 1000);
			httpUrlConn.setRequestProperty("Charsert", "UTF-8");
			// 设置请求方式（GET/POST）
			httpUrlConn.setRequestMethod(requestMethod);

			if ("GET".equalsIgnoreCase(requestMethod))
				httpUrlConn.connect();
			// 当有数据需要提交时
			if (null != outputStr) {
				OutputStream outputStream = httpUrlConn.getOutputStream();
				// 注意编码格式，防止中文乱码
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}
			// 将返回的输入流转换成字符串
			InputStream inputStream = httpUrlConn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					inputStream));
			StringBuilder sb = new StringBuilder();
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					inputStream.close();
					httpUrlConn.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return sb.toString();
		} catch (ConnectException ce) {
			ce.printStackTrace();
			logger.error(ce);
			throw ce;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
		}
		return "";
	}

	/**
	 * 向微信平台申请支付
	 * @param map  用于封装 微信请求的 model
	 * @param priceFen ： 微信交易的 金额：必须是　分表示的整数
	 * @param openId　　：微信用户的　openId
	 * @param request  : http 请求
	 * @throws ConnectException
	 * @throws DocumentException
	 */
	public static void wechatPayRequest(ModelMap map,int priceFen,String openId,String out_trade_no,
			HttpServletRequest request) throws ConnectException, DocumentException {
		if(null == map || 0 == priceFen || null == openId || null ==request)
		{
			logger.error("paramet error");
			return ;
		}
		String flag = "";
	   //商户系统内部的订单号,32 个字符内、可包含字母,确保 在商户系统唯一,详细说明
	   	String attach = WeChatConfig.WECHAT_ATTACK;// 用于验证是从微信过来的链接
		// 微信支付
		String appid = WeChatConfig.APPID_WXPAY;
		String mch_id = WeChatConfig.MCH_ID;// 微信支付分配的商户号
		String openid =  openId;//用户的openid
		String nonce_str = String.valueOf(new Date().getTime());// 随机字符串，不长于32
																// 位
		String body = "product";
		String total_fee = String.valueOf(priceFen);// 订单总金额，单位为分，不 能带小数点
		String spbill_create_ip = HttpUtil.getIpAddr(request);// 订单生成的机器IP
		String notify_url = WeChatConfig.WECHAT_NOTIFY_URL;// 接收微信支付成功通知
		String trade_type = "JSAPI";// 交易类型 JSAPI、NATIVE、APP
		String partnerKey = WeChatConfig.PARTNERKEY;// 签名时侯用    微信商户平台(pay.weixin.qq.com)-->账户中心-->账户设置-->API安全-->密钥设置

		SortedMap<String, String> mp = new TreeMap<String, String>();
		mp.put("openid", openid);
		mp.put("body", body);
		mp.put("appid", appid);
		mp.put("out_trade_no", out_trade_no + "");
		mp.put("mch_id", mch_id);
		mp.put("nonce_str", nonce_str);
		mp.put("total_fee", total_fee);
		mp.put("notify_url", notify_url);
		mp.put("spbill_create_ip", spbill_create_ip);
		mp.put("trade_type", trade_type);
		mp.put("attach", attach);// 附加数据，原样返回:验证是否是从微信支付发过来的单子
		mp.put("sign", StrUtil.createSign(mp, partnerKey));
		String xml = "<xml>";
		Set<String> ss = mp.keySet();
		Iterator<String> iterator = ss.iterator();
		while (iterator.hasNext()) {
			String next = iterator.next();
			xml += ("<" + next + "><![CDATA[" + mp.get(next) + "]]></" + next + ">");
		}
		xml += "</xml>";
		flag = WeChatRequestUtil.httpsRequestString(
				WeChatConfig.UNIFIEDORDER_URL, "POST", xml);
		logger.info("the post xml="+xml+",flag="+flag);
		
		Document document = DocumentHelper.parseText(flag);
		Map<String, Object> mpt = XmlUtils.Dom2Map(document);
		String prepay_id = String.valueOf(mpt.get("prepay_id"));
		map.put("prepay_id", prepay_id);
		TreeMap<String, String> tmap = new TreeMap<String, String>();
		tmap.put("appId", appid);
		tmap.put("timeStamp", new Date().getTime() + "");
		tmap.put("nonceStr", StrUtil.getRandomString());
		tmap.put("package", "prepay_id=" + prepay_id);
		tmap.put("signType", "MD5");
		tmap.put("paySign", StrUtil.createSign(tmap, partnerKey));
		map.put("tmap", tmap);
	} 
	
	/** 解析微信 返回给 系统的消息
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public static WechatPayMsg parseWxPayResponse(HttpServletRequest request)
			throws Exception {
		StringBuffer sb = new StringBuffer();
		InputStream is = request.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String s = "";
		while ((s = br.readLine()) != null) {
			sb.append(s);
		}
		String str = sb.toString();
		Document document = DocumentHelper.parseText(str);
		Map<String, Object> mpt = XmlUtils.Dom2Map(document);
		String result_code = String.valueOf(mpt.get("result_code"));// 是否支付成功
		String attach = String.valueOf(mpt.get("attach"));
		String out_trade_no = String.valueOf(mpt.get("out_trade_no"));// 支付ID
		String transaction_id = String.valueOf(mpt.get("transaction_id"));// 微信支付订单号
		String sign_type = String.valueOf(mpt.get("sign_type"));
		String sign = String.valueOf(mpt.get("sign"));
		String trade_mode = String.valueOf(mpt.get("trade_mode"));
		String time_end = String.valueOf(mpt.get("time_end"));
		String total_fee = String.valueOf(mpt.get("total_fee"));
		String transport_fee = String.valueOf(mpt.get("transport_fee"));
		String product_fee = String.valueOf(mpt.get("product_fee"));

		Date nowtime = new Date();
		// /保存微信发送过来的讯息
		WechatPayMsg wxmsg = new WechatPayMsg();
		wxmsg.setId(StrUtil.getUUID());
		wxmsg.setContent(str);
		wxmsg.setSign_type(sign_type);
		wxmsg.setSign(sign);
		wxmsg.setTrade_mode(trade_mode);
		wxmsg.setTrade_state(result_code);

		wxmsg.setTime_end(time_end);
		wxmsg.setTransaction_id(transaction_id);
		wxmsg.setTotal_fee(total_fee);
		wxmsg.setOut_trade_no(out_trade_no);
		wxmsg.setAttach(attach);
		wxmsg.setTransport_fee(transport_fee);
		wxmsg.setProduct_fee(product_fee);
		wxmsg.setTime(nowtime);
		return wxmsg;
	}
	
	
	/**
	 * 重定向到 鉴权界面
	 * 
	 * @param response
	 */
	public static  void redirectToAuthorize(HttpServletResponse response,String uri) {
		StringBuilder oauth_url = new StringBuilder();
		oauth_url
				.append("https://open.weixin.qq.com/connect/oauth2/authorize?");
		oauth_url.append("appid=").append(WeChatConfig.APP_ID);
		oauth_url.append("&redirect_uri=").append(
				WeChatConfig.HOST+uri);
		oauth_url.append("&response_type=code");
		oauth_url.append("&scope=snsapi_userinfo");
		oauth_url.append("&state=1#wechat_redirect");

		try {
			response.sendRedirect(oauth_url.toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	
	}
	
	
	/**
	 * 重定向到某个url
	 * 
	 * @param response
	 */
	public static  void redirectToGivenUri(HttpServletResponse response ,String URL) {
		try {
			response.sendRedirect(URL);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
	
	
}
