package happylife.service.impl;

import happylife.dao.GenericDao;
import happylife.model.TbMch;
import happylife.model.TbMchStaff;
import happylife.model.TbMchWechatRelation;
import happylife.model.TbProduct;
import happylife.model.TbQueueRecord;
import happylife.model.TbRedeemCode;
import happylife.model.TbSuggestion;
import happylife.model.TbTransactionRecord;
import happylife.model.TbWechatUser;
import happylife.model.servicemodel.CacheMchUser;
import happylife.model.servicemodel.CacheMchUserQueueOrder;
import happylife.model.servicemodel.CacheProduct;
import happylife.model.servicemodel.CacheRedeemCode;
import happylife.model.servicemodel.CacheTransaction;
import happylife.model.servicemodel.CacheWechatUser;
import happylife.model.servicemodel.HqlQueryCondition;
import happylife.model.servicemodel.MchSearchCondition;
import happylife.model.servicemodel.ProductSearchCondition;
import happylife.model.servicemodel.QueueOrderStatusEnum;
import happylife.model.servicemodel.SearchCondition;
import happylife.model.servicemodel.TransactionStatusEnum;
import happylife.model.servicemodel.HqlQueryCondition.Property;
import happylife.model.servicemodel.HqlQueryCondition.Relation;
import happylife.model.servicemodel.WechatQueueRecordInfo;
import happylife.service.WeChatUserService;
import happylife.service.exception.HappyLifeException;
import happylife.util.DateUtil;
import happylife.util.StrUtil;
import happylife.util.cache.MchStaffProductCacheManager;
import happylife.util.cache.QueueOrderCacheManager;
import happylife.util.config.IndexConfig;
import happylife.util.config.OrderTypeConfigEnum;
import happylife.util.config.PageConfigUtil;
import happylife.util.config.TransactionTypeConfig;
import happylife.util.config.WeChatConfig;
import happylife.util.config.WinterOrangeSysConf;
import happylife.util.requestandresponse.ParseRequest;
import happylife.util.requestandresponse.ResponseToClient;
import happylife.util.requestandresponse.WeChatRequestUtil;
import happylife.util.requestandresponse.WechatMessageUtil;
import happylife.util.requestandresponse.messagebean.ResultMsgBean;
import happylife.util.requestandresponse.messagebean.TextMessage;
import happylife.util.requestandresponse.messagebean.WeChatOauth2Token;
import happylife.util.requestandresponse.messagebean.WechatPayMsg;
import happylife.util.service.ScanProjectTask;
import happylife.util.service.UpdateMchSalesTask;
import happylife.util.service.UpdateProductVisitorTask;

import java.awt.font.OpenType;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.model.RecordStream;
import org.dom4j.DocumentException;
import org.springframework.dao.DataAccessException;
import org.springframework.expression.spel.ast.OpPlus;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 微信用户通过界面与系统交互的业务处理层
 * 
 * @author 闫朝喜
 * 
 */
@Service
public class WeChatUserServiceImpl extends GenericServiceImpl<TbWechatUser>
		implements WeChatUserService<TbWechatUser> {
	private static final Log logger = LogFactory
			.getLog(WeChatUserServiceImpl.class);

	@SuppressWarnings("rawtypes")
	private GenericDao wechatUserDAO;

	@SuppressWarnings("rawtypes")
	public void setwechatUserDAO(GenericDao wechatUserDAO) {
		this.wechatUserDAO = wechatUserDAO;
	}
	
	//支持计划多久刷新一次
	private AtomicLong payCnt = new AtomicLong(0); 
	
	//访问量多久刷新一次
	private AtomicLong visitCnt = new AtomicLong(0); 

	/**
	 * 解析请求中的 微信用户信息，并校验微信信息有效性， 如果有效则放到session中
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean checkWechatUserValid(HttpServletRequest request,
			HttpServletResponse response) {

		if (null == request || null == response) {
			logger.error("invalid param: request or response!");
			return false;
		}

		String code = request.getParameter("code");
		HttpSession session = request.getSession();
		boolean isValidCode = true;
		// 检查是否已验证或者验证是否通过
		if (code == null || code.equals("authdeny")) {
			isValidCode = false;
		}

		logger.info("code=" + code + ", and isValidCode=" + isValidCode);
		// 如果session未空或者取消授权，重定向到授权页面
		if ((!isValidCode) && session.getAttribute("wechatuser") == null) {
			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
			logger.warn("the user session does not exit redirect to :open.weixin.qq.com/connect/oauth2/authorize");
			return false;
		}
		// 如果用户同意授权并且，用户session不存在，通过OAUTH接口调用获取用户信息
		if (isValidCode && session.getAttribute("wechatuser") == null) {
			TbWechatUser wechatUser = null;
			WeChatOauth2Token accessToken = WeChatRequestUtil
					.getOauth2AccessToken(WeChatConfig.APP_ID,
							WeChatConfig.APP_SECRET, code);

			if (null == accessToken) {
				logger.error("access_token is null");
				return false;
			}

			wechatUser = WeChatRequestUtil.getWeChatUserInfo(
					accessToken.getAccessToken(), accessToken.getOpenId());

			if (null == wechatUser) {
				logger.error("wechatUser is null");
				return false;
			}

			// 先从数据库找一遍
			TbWechatUser temWechatUser = (TbWechatUser) wechatUserDAO
					.getObjectByProperty(TbWechatUser.class, "userOpenid",
							wechatUser.getUserOpenid());
			if (null == temWechatUser) {

				logger.info("save the wechatuser :"
						+ wechatUser.getUserOpenid());
				wechatUserDAO.saveOrupdate(wechatUser);
			} else {
				wechatUser = temWechatUser;
			}

			CacheWechatUser cacheWechatUser = new CacheWechatUser();
			cacheWechatUser.setWechatUser(wechatUser);
			cacheWechatUser.setRecords(new ArrayList<TbQueueRecord>());

			session.setAttribute(IndexConfig.SESSION_WECHATUSER_KEY,
					cacheWechatUser);
			logger.info("save the wechatuser to session,user="
					+ wechatUser.toString());

		}

		return true;

	}

	/**
	 * 处理微信的接入的signature确认消息
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@Override
	public boolean checkWeChatAccessSignature(HttpServletRequest request,
			HttpServletResponse response) {
		if (null == request || null == response) {
			logger.error("request or response is null!");
			return false;
		}
		// 微信加密签名
		String signature = request.getParameter("signature");
		// 时间戳
		String timestamp = request.getParameter("timestamp");
		// 随机数
		String nonce = request.getParameter("nonce");
		// 随机字符串
		String echostr = request.getParameter("echostr");
		logger.warn("check signature,signature=" + signature + ",timestamp="
				+ timestamp + ",nonce=" + nonce + ",echostr=" + echostr);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			boolean checkResult = WeChatRequestUtil.checkWeChatSignature(
					signature, timestamp, nonce);
			// 通过检验signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败
			if (checkResult) {
				logger.warn("check signature,value=" + checkResult);
				out.print(echostr);
				return true;
			}

			logger.warn("check signature failed");
			return false;
		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		} finally {
			if (null != out) {
				out.close();
				out = null;
			}

		}

	}

	/**
	 * 微信的非signature确认消息的处理接口类，处理：关注、取消关注、订阅、自定义菜单的点击等请求
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@Override
	public boolean processWechatInteractive(HttpServletRequest request,
			HttpServletResponse response) {
		if (null == request || null == response) {
			logger.error("request or response is null!");
			return false;
		}

		logger.info("now process interactive message:");
		PrintWriter out = null;
		// 将请求、响应的编码均设置为UTF-8（防止中文乱码）
		try {
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			logger.debug("set utf-8 encoding");
			String respMessage = processRequest(request);
			out = response.getWriter();
			out.print(respMessage);
			logger.debug("write the message:" + respMessage + " to client.");

			return true;
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			return false;
		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		} finally {
			if (null != out) {
				out.close();
				out = null;
			}

		}
	}

	/**
	 * 处理微信交互接口发来的请求
	 * 
	 * @param request
	 * @return
	 */
	private String processRequest(HttpServletRequest request) {
		String respMessage = "";
		try {
			logger.debug("handle the request");
			// 默认返回的文本消息内容
			String respContent = "";
			// xml请求解析
			Map<String, String> requestMap = WechatMessageUtil
					.parseXml(request);

			// 发送方帐号（open_id）
			String fromUserName = requestMap.get("FromUserName");
			// 公众帐号
			String toUserName = requestMap.get("ToUserName");
			// 消息类型
			String msgType = requestMap.get("MsgType");

			// 回复文本消息
			TextMessage textMessage = new TextMessage();
			textMessage.setToUserName(fromUserName);
			textMessage.setFromUserName(toUserName);
			textMessage.setCreateTime(new Date().getTime());
			textMessage
					.setMsgType(WechatMessageUtil.MessageType.RESP_MESSAGE_TYPE_TEXT);
			textMessage.setFuncFlag(0);

			// 文本消息
			if (msgType
					.equals(WechatMessageUtil.MessageType.REQ_MESSAGE_TYPE_TEXT)) {
				// TODO : 文本类消息待实现
				logger.warn("need to do,  nothing");
			} else if (msgType
					.equals(WechatMessageUtil.MessageType.REQ_MESSAGE_TYPE_EVENT)) {
				// 事件类型
				String eventType = requestMap.get("Event");
				// 订阅
				if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_SUBSCRIBE)) {
					respContent = "谢谢您的关注";
					// subscribe(fromUserName);

				}
				// 扫描二维码已经关注
				else if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_SCAN)) {
					// 事件KEY值，与创建自定义菜单时指定的KEY值对应
					respContent = "谢谢您的关注";
					// subscribe(fromUserName);
				}
				// 取消订阅
				else if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_UNSUBSCRIBE)) {
					return null;
				} else if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_CLICK)) {
					// 事件KEY值，与创建自定义菜单时指定的KEY值对应
					respContent = "建设中，感谢关注";
				}
			} else {
				logger.warn(" 建设中 。。。");
			}

			textMessage.setContent(respContent);
			respMessage = WechatMessageUtil.textMessageToXml(textMessage);
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		return respMessage;
	}

	/**
	 * 按分页方式查询商家信息，可能支持带商家名检索或者位置检索则 在model 层里面把对应的 数据信息填充为空即可
	 * 
	 * @param request
	 * @param model
	 * @return true 填充 成功 false 填充失败
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void generateFollowMchListByPage(HttpServletRequest request,
			Map<String, Object> model, HttpServletResponse response,
			CacheWechatUser cacheChatUser) {
		if (null == request || null == model || null == cacheChatUser) {
			logger.error("request or model can not be null");
			return;
		}

		// 从ext_props里面解析 字段
		String extPropsStr = cacheChatUser.getWechatUser().getExtProps();

		Map<String, Object> extPropsMap = null;
		if (StringUtils.isBlank(extPropsStr)) {
			extPropsMap = new HashMap();
		} else {
			extPropsMap = JSON.parseObject(extPropsStr, Map.class);
		}

		Object mchids = extPropsMap.get("favoriteMchIds");
		JSONArray mchIds = mchids == null ? new JSONArray()
				: (JSONArray) mchids;
		// 无关注的商家
		if (mchIds.size() == 0) {
			model.put("mchUsers", new ArrayList());

		} else {
			List<CacheMchUser> mchUsers = new ArrayList<>();

			for (int i = 0; i < mchIds.size(); i++) {
				CacheMchUser temMch = MchStaffProductCacheManager.getInstance()
						.getCacheMchUserById(
								Integer.valueOf((String) mchIds.get(i)));
				mchUsers.add(temMch);
			}

			model.put("mchUsers", mchUsers);

		}

	}

	/**
	 * 按分页方式查询商家信息，可能支持带商家名检索或者位置检索则 在model 层里面把对应的 数据信息填充为空即可
	 * 
	 * @param request
	 * @param model
	 * @return true 填充 成功 false 填充失败
	 */
	@Override
	public void generateMchListByPage(HttpServletRequest request,
			Map<String, Object> model, HttpServletResponse response,
			boolean isAjax) {
		if (null == request || null == model) {
			logger.error("request or model can not be null");
			return;
		}

		int currentPage = 0;
		String currentPageStr = request.getParameter("currentPage");
		logger.info("the currentPageStr=" + currentPageStr);
		if (StringUtils.isNotBlank(currentPageStr)
				&& StringUtils.isNumeric(currentPageStr)) {
			currentPage = Integer.valueOf(currentPageStr);
		}
		// 先按name匹配，request里面默认传的就是name
		List<MchSearchCondition> conditions = ParseRequest
				.generateMchSearchConditions(request);
		String mchName = (String) request
				.getParameter(MchSearchCondition.ConditionName.MCH_NAME);
		/* 搜索框是空的时候 */
		List<CacheMchUser> mchUsers = MchStaffProductCacheManager.getInstance()
				.findMchUserByCondition(currentPage, conditions);

		if (null == mchUsers
				|| mchUsers.size() == 0
				&& request
						.getParameter(MchSearchCondition.ConditionName.MCH_NAME) != null) {
			request.removeAttribute(MchSearchCondition.ConditionName.MCH_NAME);

			conditions = new ArrayList<MchSearchCondition>();
			conditions.add(new MchSearchCondition(
					SearchCondition.RelationType.CONTAIN,
					MchSearchCondition.ConditionName.MCH_ADDRESS, mchName));

			mchUsers = MchStaffProductCacheManager.getInstance()
					.findMchUserByCondition(currentPage, conditions);
		}

		String isAjaxStr = request.getParameter("isAjax");
		isAjax = StringUtils.isNotBlank(isAjaxStr) ? Boolean.valueOf(isAjaxStr)
				: isAjax;

		if (isAjax) {
			String returnResult = WechatMessageUtil
					.generateHTMLMchList(mchUsers);
			ResultMsgBean Msg = null;
			if (StringUtils.isBlank(returnResult)) {
				Msg = new ResultMsgBean(false, returnResult);
				Msg.setCurrentPage(currentPage);
			} else {
				Msg = new ResultMsgBean(true, returnResult);
				Msg.setCurrentPage(currentPage + 1);
			}

			ResponseToClient.writeJsonMsg(response, Msg);
			return;
		}

		int totalMchs = mchUsers == null ? 0 : mchUsers.size();
		logger.debug("find the mchUsers");
		model.put("mchUsers", mchUsers);
		logger.debug("mchUsers are:" + mchUsers);
		model.put("currentPage", currentPage + 1);
		model.put("name", mchName);
		model.put("totalMchs", totalMchs);
		return;
	}

	/**
	 * 按分页方式查询 美食信息： 可以支持只查指定商家的美食， 也可以查询所有商家的美食，对应的美食商品的状态要为有效值 如果查询不到 则 在model
	 * 层里面把对应的 数据信息填充为空即可
	 * 
	 * @param request
	 * @param model
	 * @return true 填充 成功 false 填充失败
	 */
	@Override
	public void generateProductListByPage(HttpServletRequest request,
			Map<String, Object> model, HttpServletResponse response,
			boolean isAjax) {
		if (null == request || null == model) {
			logger.error("request or model can not be null");
			return;
		}

		int currentPage = 0;
		String currentPageStr = request.getParameter("currentPage");
		logger.info("the currentPageStr=" + currentPageStr);
		if (StringUtils.isNotBlank(currentPageStr)
				&& StringUtils.isNumeric(currentPageStr)) {
			currentPage = Integer.valueOf(currentPageStr);
		}

		List<ProductSearchCondition> conditions = ParseRequest
				.generateSearchConditions(request);

		/* 这样的方式默认按照 以 商家为维度 查询商品的方式 */
		if (null == conditions || conditions.size() == 0) {
			List<CacheMchUser> mchUsers = MchStaffProductCacheManager
					.getInstance().findMchUserByPage(currentPage);
			if (isAjax) {
				String returnResult = WechatMessageUtil
						.generateHTMLFoodList(mchUsers);
				ResultMsgBean Msg = null;
				if (StringUtils.isBlank(returnResult)) {
					Msg = new ResultMsgBean(false, returnResult);
					Msg.setCurrentPage(currentPage);
				} else {
					Msg = new ResultMsgBean(true, returnResult);
					Msg.setCurrentPage(currentPage + 1);
				}

				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			logger.debug("find the mchUsers");
			model.put("mchUsers", mchUsers);
			logger.info("mchUsers are:" + mchUsers);
			model.put("currentPage", currentPage + 1);
			return;
		} else {
			// TODO : 待实现搜索框的方式 检索 美食
		}

	}

	// private String subscribe(String fromUserName) {
	// TbWechatUser user =
	// (TbWechatUser)wechatUserDAO.getObjectByProperty(TbWechatUser.class,
	// "openid",
	// fromUserName);
	// if(user==null){
	// Config config = configService.getConfig();
	// user = WeixinUtil.getUser(config.getAccesstoken(), fromUserName);
	// if(user!=null){
	// userService.saveOrupdate(user);
	// }
	// }
	// return "谢谢关注";
	// }

	@SuppressWarnings("unchecked")
	/**
	 * 微信用户关注一个商家 或者取消关注一个商家
	 */
	@Override
	public void wechatFollowOneMch(HttpServletRequest request, ModelMap model,
			HttpServletResponse response) {
		CacheWechatUser cacheChatUser = null;
		if (!WinterOrangeSysConf.IS_TEST_VALID) {
			cacheChatUser = ParseRequest.getWechatUserFromSession(request);
			if (null == cacheChatUser
					|| StringUtils.isBlank(cacheChatUser.getWechatUser()
							.getUserOpenid())) {
				logger.error("wechat user session timeout,re auth");
				WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
				return;
			}
		} else {
			logger.debug("current in test model");
			cacheChatUser = new CacheWechatUser();
			TbWechatUser wechatUser = new TbWechatUser();
			wechatUser.setUserOpenid("st0628");
			cacheChatUser.setWechatUser(wechatUser);
		}

		// 解析mchId
		String mchId = ParseRequest.parseRequestByType("mchid", false, request);
		if (StringUtils.isBlank(mchId)) {
			logger.error("the mchId can not be null");
			ResultMsgBean Msg = new ResultMsgBean(false, "您必须选择一个商户");
			ResponseToClient.writeJsonMsg(response, Msg);
			return;
		}

		String optType = ParseRequest.parseRequestByType("type", true, request);
		if (StringUtils.isBlank(optType)) {
			logger.warn("the opt type is null ");
			optType = "add";// 默认是添加一个商家关注
		}

		try {
			// 从ext_props里面解析 字段
			String extPropsStr = cacheChatUser.getWechatUser().getExtProps();

			Map<String, Object> extPropsMap = null;
			if (StringUtils.isBlank(extPropsStr)) {
				extPropsMap = new HashMap();
			} else {
				extPropsMap = JSON.parseObject(extPropsStr, Map.class);
			}

			Object mchids = extPropsMap.get("favoriteMchIds");
			JSONArray mchIds = mchids == null ? new JSONArray()
					: (JSONArray) mchids;

			// 一个用户至多关注10家
			if (mchIds.size() > WinterOrangeSysConf.WECHAT_FOLLOW_MCHS_COUNT) {
				logger.error("the follow mch count exceed");
				ResultMsgBean Msg = new ResultMsgBean(false, "大人您关注的商家数已超上限");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			processFavoriteMchs(mchIds, mchId, optType);
			extPropsMap.put("favoriteMchIds", mchIds);

			TbWechatUser wechatUser = cacheChatUser.getWechatUser();

			wechatUser.setExtProps(JSON.toJSONString(extPropsMap));

			wechatUserDAO.update(wechatUser);

			// 更新下商家的关注情况： 商家总的关注数量要更新，关注的记录数据表要更新
			List<TbMchWechatRelation> records = wechatUserDAO
					.getListByProperty(TbMchWechatRelation.class, "fkOpenId",
							cacheChatUser.getWechatUser().getUserOpenid());
			TbMchWechatRelation newRelation = null;
			if (null != records && records.size() > 0) {
				for (TbMchWechatRelation record : records) {

					// 找到了 再更新
					if (record.getFkMchId() == Integer.valueOf(mchId)
							&& record.getFkOpenId().equals(
									cacheChatUser.getWechatUser()
											.getUserOpenid())) {
						newRelation = record;

					}

				}

			}

			// 说明没找到该商家的关联记录
			if (null == newRelation) {
				newRelation = new TbMchWechatRelation();
				newRelation.setIsDelete(0);
				newRelation.setWechatName(cacheChatUser.getWechatUser()
						.getUserNickName());
				newRelation.setFkOpenId(cacheChatUser.getWechatUser()
						.getUserOpenid());
				newRelation.setFkMchId(Integer.valueOf(mchId));
			}

			TbMch mch = MchStaffProductCacheManager.getInstance()
					.getMchUserById(Integer.valueOf(mchId));
			// 添加操作
			if (optType.equalsIgnoreCase("add")) {
				newRelation.setFollowTime(new Date());
				newRelation.setIsDelete(0);
				mch.setTotalFans(mch.getTotalFans() + 1);

			} else {
				newRelation.setCancelTime(new Date());
				newRelation.setIsDelete(1);
				mch.setTotalFans(mch.getTotalFans() - 1);
			}

			wechatUserDAO.saveOrupdate(newRelation);
			logger.info("success update relation,type=" + optType + ",record="
					+ newRelation.toString());

			// 刷新商户的关注用户量
			wechatUserDAO.saveOrupdate(mch);

			request.getSession().setAttribute(
					IndexConfig.SESSION_WECHATUSER_KEY, cacheChatUser);
			logger.info("the wechat user follow mch succes  mchid=" + mchId);
			ResultMsgBean Msg = new ResultMsgBean(true, "关注成功，请到我的->关注商家查看");

			if (!optType.equals("add")) {
				Msg = new ResultMsgBean(true, "已取消关注，请到我的->关注商家查看");
			}

			ResponseToClient.writeJsonMsg(response, Msg);
			return;

		} catch (Exception e) {
			logger.error(e.getMessage());

			ResultMsgBean Msg = new ResultMsgBean(false, "系统错误，请稍后重试");
			ResponseToClient.writeJsonMsg(response, Msg);

			return;
		}

	}

	public static void main(String[] args) {

		String extPropsStr = "{\"favoriteMchIds\":[\"1\",\"2\"]}";

		Map<String, Object> extProps = JSON.parseObject(extPropsStr, Map.class);

		JSONArray mchIds = (JSONArray) extProps.get("favoriteMchIds");
		if (null != mchIds && mchIds.size() > 0) {
			if (mchIds.contains("1")) {
				System.out.print("true");
			}

		}

	}

	private void processFavoriteMchs(JSONArray srcMchIds, String mchId,
			String type) {

		if (type.equalsIgnoreCase("add")) {
			if (srcMchIds.contains(mchId)) {
				return;
			}

			srcMchIds.add(mchId);

		} else if (type.equalsIgnoreCase("delete")) {
			srcMchIds.remove(mchId);
		}

	}

	/**
	 * 购买商品
	 * 
	 * @param request
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public ModelAndView buyProduct(ModelMap model, HttpServletRequest request,
			HttpServletResponse response) {

		String productId = ParseRequest.parseRequestByType("productId", false,
				request);
		if (null == productId) {
			logger.error("db does not hava the product,and return ");
			return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
		}

		CacheProduct cacheProduct = ParseRequest.findCacheProduct(request);
		TbProduct product = null;
		// 缓存没有 则从数据查询一次 数据库也没有 则 返回
		if (null == cacheProduct) {
			logger.error("not cache the product");
			product = (TbProduct) wechatUserDAO.get(TbProduct.class,
					Integer.valueOf(productId));
			if (null == product) {
				logger.error("db does not hava the product,and return ");
				return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
			}
		}else{
			product = cacheProduct.getProduct();
		}

		logger.info("product is :" + product);
		CacheWechatUser wechatUser = ParseRequest
				.getWechatUserFromSession(request);
		if (null == wechatUser) {
			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
			logger.error("wechat user's session timeout");
			return null;
		}

		try {
			// 请求微信支付,并放到model 里面

			// 解析购买多少个 商品
			String productNum = ParseRequest.parseRequestByType("productNum",
					false, request);
			int productNumInt = productNum == null ? 1 : Integer
					.valueOf(productNum);

			int priceFen = (int) (product.getDiscountPrice() * 100 * productNumInt);
			String out_trade_no = new SimpleDateFormat("yyyyMMddHHmmssSSS")
					.format(new Date());// 商户系统内部的订单号,32 个字符内、可包含字母,确保
										// 在商户系统唯一,详细说明

			WeChatRequestUtil.wechatPayRequest(model, priceFen, wechatUser
					.getWechatUser().getUserOpenid(), out_trade_no, request);

			// 流水记录 （状态置0，等待回调后，更新状态为1）
			TbTransactionRecord tsr = new TbTransactionRecord();
			tsr.setFkMchId(product.getFkMchId());
			tsr.setFkProductId(product.getProductId());
			tsr.setFkOpenId(wechatUser.getWechatUser().getUserOpenid());
			tsr.setOutTradeNo(StrUtil.getRandomTsr());
			tsr.setRecordMoney((int) (product.getDiscountPrice() * 100));
			tsr.setRecordStatus(0);
			tsr.setRecordTime(new Date());
			tsr.setRecordId(1);
			tsr.setProductNum(productNumInt);
			tsr.setRecordType(TransactionTypeConfig.BUY_PRODUCT);

			logger.info("tsr is :" + tsr);
			wechatUserDAO.add(tsr);

			logger.info("now have request to wechat for paying , to pay page.");

			// 跳转到 支付界面
			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX
					+ PageConfigUtil.WechatUserPage.PAY_PAGE);
		} catch (ConnectException e1) {
			logger.error("can not access db " + e1);
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		} catch (DocumentException e1) {
			logger.warn("parse we chat requst faild: " + e1);
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
	}

	/**
	 * 微信用户预付费取号
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ModelAndView payforQueueOrder(ModelMap model,
			HttpServletRequest request, HttpServletResponse response) {

		String mchid = ParseRequest.parseRequestByType("mchid", false, request);
		if (null == mchid) {
			logger.error("request does not hava the mch id ,and return ");
			return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
		}

		String type = ParseRequest.parseRequestByType("type", true, request);

		if (StringUtils.isBlank(type)) {
			type = OrderTypeConfigEnum.XIJIAN.getTypeStr();
		}

		TbMch mch = MchStaffProductCacheManager.getInstance().getMchUserById(
				Integer.valueOf(mchid));
		if (null == mch) {
			logger.error("db does not hava the mch id ,and return ");
			return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
		}

		CacheWechatUser cacheChatUser = null;
		if (!WinterOrangeSysConf.IS_TEST_VALID) {
			cacheChatUser = ParseRequest.getWechatUserFromSession(request);
			if (null == cacheChatUser
					|| StringUtils.isBlank(cacheChatUser.getWechatUser()
							.getUserOpenid())) {
				logger.error("wechat user session timeout,re auth");
				WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
				return null;
			}
		} else {
			logger.debug("current in test model");
			cacheChatUser = new CacheWechatUser();
			TbWechatUser wechatUser = new TbWechatUser();
			wechatUser.setUserOpenid("ycx0st");
			cacheChatUser.setWechatUser(wechatUser);
		}

		TbWechatUser wechatUser = cacheChatUser.getWechatUser();

		boolean isMchOrder = wechatUser.getUserOpenid().equals(
				mch.getFkOpenId());

		// 如果是系统当前不收费 则跳过收费环节或者是商家取号则跳过收费
		if (WinterOrangeSysConf.IS_FREE_SERVICE || isMchOrder) {
			TbTransactionRecord temRecord = new TbTransactionRecord();
			temRecord.setFkMchId(mch.getMchId());
			temRecord.setRecordId(-1);// 无效的交易ID
			temRecord.setFkOpenId(wechatUser.getUserOpenid());
			Map<String, Object> extProps = new HashMap<String, Object>();
			extProps.put("serviceType", type);
			extProps.put("isMchOrder", isMchOrder ? "true" : "false");
			temRecord.setExtProps(JSON.toJSONString(extProps));
			try {
				processNewOrderIn(temRecord, response);
			} catch (IOException e) {
				logger.error("add new order faild," + e.getMessage());
				return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
			}

			// 重定向到 取号url
			WeChatRequestUtil.redirectToGivenUri(response,
					request.getContextPath() + PageConfigUtil.WECHAT_PREFIX
							+ PageConfigUtil.WechatUserPage.TO_QUEUE_URI);
			return null;
		}

		// 取号收费情况下
		try {
			// 请求微信支付,并放到model 里面
			int priceFen = WinterOrangeSysConf.QUEUE_ORDER_MONEY_ONECE; // 早起这个值就是一分钱

			String out_trade_no = new SimpleDateFormat("yyyyMMddHHmmssSSS")
					.format(new Date());// 商户系统内部的订单号,32 个字符内、可包含字母,确保
										// 在商户系统唯一,详细说明

			WeChatRequestUtil.wechatPayRequest(model, priceFen,
					wechatUser.getUserOpenid(), out_trade_no, request);

			// 流水记录 （状态置0，等待回调后，更新状态为1）
			TbTransactionRecord tsr = new TbTransactionRecord();
			tsr.setFkMchId(mch.getMchId());
			tsr.setFkProductId(-1);
			tsr.setFkOpenId(wechatUser.getUserOpenid());
			tsr.setOutTradeNo(out_trade_no);
			tsr.setRecordMoney(priceFen);
			tsr.setRecordStatus(0);
			tsr.setRecordTime(new Date());
			tsr.setProductNum(0);
			tsr.setRecordType(TransactionTypeConfig.QUEUE_ORDER);
			logger.info("payforQueueOrder tsr is :" + tsr);

			Map<String, Object> extProps = new HashMap<String, Object>();
			extProps.put("serviceType", type);
			tsr.setExtProps(JSON.toJSONString(extProps));
			wechatUserDAO.add(tsr);

			logger.info("now have request to wechat for paying , to pay page.");
			// 跳转到 支付界面
			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX
					+ PageConfigUtil.WechatUserPage.PAY_PAGE);
		} catch (ConnectException e1) {
			logger.error("can not access db " + e1);
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		} catch (DocumentException e1) {
			logger.warn("parse we chat requst faild: " + e1);
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
	}

	/**
	 * 微信账户 提交 一条意见
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public void addNewSuggestion(HttpServletRequest request,
			HttpServletResponse response) {

		try {
			logger.info("add suggestion to db.");
			String nickName = request.getParameter("nickName");
			String email = request.getParameter("email");
			String mchIdStr = request.getParameter("fkMchId");
			String info = request.getParameter("info");

			TbSuggestion suggest = new TbSuggestion();
			suggest.setCreateTime(new Date());
			suggest.setEmail(email);
			suggest.setNickName(nickName);
			suggest.setInfo(info);
			if (null != mchIdStr && StringUtils.isNumeric(mchIdStr))
				suggest.setFkMchId(Integer.valueOf(mchIdStr));

			wechatUserDAO.add(suggest);

			logger.warn("add suggestion success");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(true,
					"提交成功"));

		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"更新失败"));
			return;
		}

	}

	/**
	 * 处理 微信 支付的异步回调请求
	 * 
	 * @param request
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public void processWechatPayBack(ModelMap model,
			HttpServletRequest request, HttpServletResponse response) {

		try {
			WechatPayMsg wxmsg = WeChatRequestUtil.parseWxPayResponse(request);
			// 根据out_trade_no去查询订单是否处理过
			TbTransactionRecord record = (TbTransactionRecord) wechatUserDAO
					.getObjectByProperty(TbTransactionRecord.class,
							"outTradeNo", wxmsg.getOut_trade_no());

			logger.info("processwechatpayback:wxmsg=" + wxmsg.toString());

			if (record == null) {
				logger.error("processwechatpayback: record not exit,wxmsg="
						+ wxmsg.getOut_trade_no());
				// 订单不存在
				response.getWriter().println("FAIL");
			
			    //支付记录递增一个
				long currentPay =  payCnt.incrementAndGet();
				
				if(currentPay%WinterOrangeSysConf.PAY_CNT_FOR_TASK == 0){
					ScanProjectTask  task = new ScanProjectTask(wechatUserDAO);
					MchStaffProductCacheManager.getInstance().registerTask(task);
					logger.warn("start update product");
					
					UpdateMchSalesTask mchTask = new UpdateMchSalesTask(wechatUserDAO);
					MchStaffProductCacheManager.getInstance().registerTask(mchTask);
					
					logger.warn("start update mchsales");
					
				}
			}
			
			
			long visit = QueueOrderCacheManager.visitTotalCnt.get();
			if(visit%WinterOrangeSysConf.VISIT_CNT_FOR_TASK == 0){
				
				UpdateProductVisitorTask  task  = new UpdateProductVisitorTask(wechatUserDAO);
				MchStaffProductCacheManager.getInstance().registerTask(task);
				logger.warn("update product visitor task");
			}
			
			logger.info("the transaction record info=" + record.toString());
			// 说明是 微信过来的信息
			if (wxmsg.getAttach().equals(WeChatConfig.WECHAT_ATTACK)) {
				if (record.getRecordStatus() == 0) {
					// 已支付
					record.setRecordStatus(1);
					// 更新 record 状态
					wechatUserDAO.saveOrupdate(record);
					logger.info("update the record success,out_trade_no="
							+ wxmsg.getOut_trade_no());
					// 刷新微信账户积分
					// CacheWechatUser cacheWechatUser =
					// ParseRequest.getWechatUserFromSession(request);
					// TbWechatUser wechatUser =
					// cacheWechatUser.getWechatUser();
					// wechatUser.setUserRewardPoint(wechatUser
					// .getUserRewardPoint() + record.getRecordMoney());
					// wechatUserDAO.saveOrupdate(wechatUser);
					//
					// logger.info("update the wechatuser success,wechat wechatinfo="+wechatUser.toString());

					// 根据不同的recordType做不同的处理
					processByRecordType(record, response);

					response.getWriter().println("SUCCESS"); // 请不要修改或删除
					return;
				} else {

					logger.info("the record is processed, out_trade_no="
							+ wxmsg.getOut_trade_no() + ",record id="
							+ record.getRecordId());
					// 已经处理过了
					response.getWriter().println("SUCCESS"); // 请不要修改或删除
					return;
				}
			} else {
				// 订单不存在
				logger.error("the record not exit, record  out_trade_no="
						+ wxmsg.getOut_trade_no());
				response.getWriter().println("FAIL");
				return;
			}
		} catch (Exception e) {
			logger.error("the record find error, record  " + e.getMessage());
			try {
				response.getWriter().println("FAIL");
			} catch (Exception et) {
				logger.error(e.getMessage());
			}
		}
	}

	// 根据不同的 交易类型做不同的处理
	@SuppressWarnings("unchecked")
	private void processByRecordType(TbTransactionRecord record,
			HttpServletResponse response) throws IOException {
		String recordType = record.getRecordType();
		logger.info("process wx pay back ,the recordmsg=" + record.toString());

		if (recordType.equalsIgnoreCase(TransactionTypeConfig.REDEEMCODE)) {
			TbRedeemCode redeemCode = new TbRedeemCode();
			redeemCode.setRandomCode(StrUtil.getRandomString());
			redeemCode.setFkMchId(record.getFkMchId());
			redeemCode.setFkProductId(record.getFkProductId());
			redeemCode.setIsUsed(false);
			// 0: 购买 1 ：兑换
			redeemCode.setCreateType(0);
			redeemCode.setProductNum(record.getProductNum());
			redeemCode.setPayMoney(record.getRecordMoney());
			// 有效期原则上 是商家那给出的： 或者通过 商品属性 或者通过商家设置
			redeemCode.setValidDate(null);
			redeemCode.setUsedTime(null);
			redeemCode.setCodeCreateTime(new Date());
			redeemCode.setIsSendok(0);

			wechatUserDAO.saveOrupdate(redeemCode);

			JSONObject returnWechat = WechatMessageUtil
					.sendRedeemCodeInfoMsg(redeemCode);
			if (null == returnWechat) {
				// 发送用户失败， 后续定时任务会更新成功
				response.getWriter().println("FAIL");
				return;
			}

			logger.info("the send message return:"
					+ returnWechat.toJSONString());
			// 发送成功则 更新数据库状态
			redeemCode.setIsSendok(1);
			wechatUserDAO.saveOrupdate(redeemCode);
		}
		// 排队取号
		else if (recordType.equalsIgnoreCase(TransactionTypeConfig.QUEUE_ORDER)) {

			logger.info("the record is for queue,to queue page...");
			processNewOrderIn(record, response);

		}// 直接购买
		else if (recordType.equalsIgnoreCase(TransactionTypeConfig.BUY_PRODUCT)) {
			logger.warn("the record type is bug product , nothing to do !");
		}
		
		
		//都需要把商家的售卖信息更新下： 
		TbMch  mchUser = MchStaffProductCacheManager.getInstance().getMchUserById(record.getFkMchId());
		synchronized(mchUser){
			mchUser.setTotalSaleCount(mchUser.getTotalSaleCount()+record.getProductNum());
			mchUser.setTotalMoney(mchUser.getTotalMoney()+record.getRecordMoney());
		}
	
	}

	// 新增一条取号信息
	@SuppressWarnings("unchecked")
	private void processNewOrderIn(TbTransactionRecord record,
			HttpServletResponse response) throws IOException {

		CacheMchUserQueueOrder mchOrder = QueueOrderCacheManager.getInstance()
				.getCacheMchUserQueue(record.getFkMchId());

		if (null == mchOrder) {
			logger.error("QueueOrderCacheManager have no message of mch user order,return  ,mch id ="
					+ record.getFkMchId());
			response.getWriter().println("FAIL");

			return;
		}

		TbQueueRecord newQueue = new TbQueueRecord();
		newQueue.setFkMchId(record.getFkMchId());
		newQueue.setFkOpenId(record.getFkOpenId());
		newQueue.setStartTime(new Date());
		newQueue.setFkTransactionId(record.getRecordId());
		newQueue.setStatus(QueueOrderStatusEnum.WAITING.getStatusInt());
		newQueue.setFkMchstaffId(-1);
		newQueue.setIsScan(0);

		Map extProps = JSON.parseObject(record.getExtProps(), Map.class);
		String serviceType = OrderTypeConfigEnum.XIJIAN.getTypeStr();
		if (null != extProps) {
			String temType = (String) extProps.get("serviceType");
			serviceType = StringUtils.isNotBlank(temType) ? temType
					: serviceType;
		}

		logger.info("save the queue to db and cache , transactionId="
				+ record.getRecordId());

		Map<String, Object> extPropMap = JSON.parseObject(record.getExtProps(),
				Map.class);

		boolean isMchOrder = false;
		if (null != extPropMap) {
			Object isMchOrderStr = extPropMap.get("isMchOrder");
			isMchOrder = isMchOrderStr == null ? false : Boolean
					.valueOf((String) isMchOrderStr);
		}
		
		int waitNum = 0;

		// 加锁 保障数据库跟添加到商户缓存中的排序是一致的
		synchronized (mchOrder) {
			String orderId = null;
			
			//排号满100 就从头开始排号
			if(mchOrder.getQueueNum().get() > 99){
				logger.warn("the mch="+record.getFkMchId()+", exceed 99,now from 1 start ");
				mchOrder.getQueueNum().set(1);
			}

			if (isMchOrder) {
				orderId = serviceType + "-"
						+ mchOrder.getQueueNum().getAndIncrement() + "-占";
			} else {
				orderId = serviceType + "-"
						+ mchOrder.getQueueNum().getAndIncrement();
			}
			newQueue.setOrderId(orderId);
			// 先加入到队列里面
			@SuppressWarnings("unchecked")
			Integer id = (Integer) wechatUserDAO.add(newQueue);
			logger.info("save the queue order success, id=" + id);
			newQueue.setRecordId(id);
			waitNum = mchOrder.getWaitingQueue().size();
			mchOrder.getWaitingQueue().add(newQueue);
			logger.warn("fetch queue order success, order id=" + orderId
					+ ", mchid=" + record.getFkMchId());
		}
		
		//给微信用户发送取号成功消息
		WechatMessageUtil.sendOrderSuccessMsgToWechat(newQueue, waitNum);
	}

	/**
	 * staff 查询委派给自己的订单信息
	 */
	@Override
	public List<CacheTransaction> staffGetTransaction(
			Map<String, Object> model, TbMchStaff staff,
			HttpServletRequest request, HttpServletResponse response) {
		List<TbTransactionRecord> transactions = generateTransactionList(
				request, model, staff);
		// 如果没有查询到 则直接返回
		if (null == transactions || transactions.size() == 0) {
			logger.warn("not find TransactionRecords,return");
			return null;
		}

		List<CacheTransaction> cacheTransactions = new ArrayList<CacheTransaction>();
		String tmpProductName = null;
		String tmpMchShopName = null;
		CacheTransaction tmpCacheTransaction = null;
		MchStaffProductCacheManager manager = MchStaffProductCacheManager
				.getInstance();
		for (TbTransactionRecord transaction : transactions) {
			tmpCacheTransaction = new CacheTransaction();
			tmpCacheTransaction.setTransaction(transaction);

			TransactionStatusEnum[] statusEnums = TransactionStatusEnum
					.values();
			for (TransactionStatusEnum tran : statusEnums) {
				if (tran.getStatusInt() == transaction.getRecordStatus()) {
					tmpCacheTransaction.setStatusMsg(tran.getStatusMsg());
					break;
				}
			}

			tmpProductName = manager
					.getProductById(transaction.getFkProductId()).getProduct()
					.getProductName();
			tmpCacheTransaction.setProductName(tmpProductName);

			tmpMchShopName = manager
					.getProductById(transaction.getFkProductId()).getMchUser()
					.getShopName();

			tmpCacheTransaction.setMchShopName(tmpMchShopName);
			tmpCacheTransaction.setMchStaff(staff);
			cacheTransactions.add(tmpCacheTransaction);
		}

		return cacheTransactions;
	}

	// 平台wechat 查询自己的订单
	@SuppressWarnings("rawtypes")
	@Override
	public List wechatGetTransactions(Map<String, Object> model,
			TbWechatUser wechat, HttpServletRequest request,
			HttpServletResponse response) {

		String fkOpenId = wechat.getUserOpenid();
		request.setAttribute("fkOpenId", fkOpenId);

		List<TbTransactionRecord> transactions = generateTransactionList(
				request, model, null);
		// 如果没有查询到 则直接返回
		if (null == transactions || transactions.size() == 0) {
			logger.warn("not find TransactionRecords,return");
			return null;
		}

		List<CacheTransaction> cacheTransactions = new ArrayList<CacheTransaction>();
		String tmpProductName = null;
		String tmpMchShopName = null;
		CacheTransaction tmpCacheTransaction = null;
		MchStaffProductCacheManager manager = MchStaffProductCacheManager
				.getInstance();
		for (TbTransactionRecord transaction : transactions) {
			tmpCacheTransaction = new CacheTransaction();
			tmpCacheTransaction.setTransaction(transaction);

			TransactionStatusEnum[] statusEnums = TransactionStatusEnum
					.values();
			for (TransactionStatusEnum tran : statusEnums) {
				if (tran.getStatusInt() == transaction.getRecordStatus()) {
					tmpCacheTransaction.setStatusMsg(tran.getStatusMsg());
					break;
				}
			}

			tmpProductName = manager
					.getProductById(transaction.getFkProductId()).getProduct()
					.getProductName();
			tmpCacheTransaction.setProductName(tmpProductName);

			tmpMchShopName = manager
					.getProductById(transaction.getFkProductId()).getMchUser()
					.getShopName();

			tmpCacheTransaction.setMchShopName(tmpMchShopName);
			;
			cacheTransactions.add(tmpCacheTransaction);
		}

		return cacheTransactions;
	}

	/**
	 * 根据查询条件 ： 分页或者全部显示的方式查询具体的商家账户的 流水
	 */
	@SuppressWarnings("unchecked")
	private List<TbTransactionRecord> generateTransactionList(
			HttpServletRequest request, Map<String, Object> model,
			TbMchStaff staff) {
		if (null == request) {
			logger.error("reques can not be null");
			return null;
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);

		HqlQueryCondition query = new HqlQueryCondition();
		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);

		if (null != staff && staff.getId() != null) {
			query.getProperties().add(
					new Property(Relation.EQ, "fkStaffId", staff.getId()));
		}
		query.setOrderName("recordTime");
		query.setAesc(false);

		String fkProductId = request.getParameter("fkProductId");
		if (StringUtils.isNotBlank(fkProductId)
				&& StringUtils.isNumeric(fkProductId)) {
			query.getProperties().add(
					new Property(Relation.EQ, "fkProductId", Integer
							.valueOf(fkProductId)));
		}

		String fkOpenId = request.getParameter("fkOpenId");
		if (StringUtils.isNotBlank(fkOpenId)) {
			query.getProperties().add(
					new Property(Relation.EQ, "fkOpenId", fkOpenId));
		}

		return wechatUserDAO.getListByQueryCondtion(TbTransactionRecord.class,
				query);
	}

	// staff 完成委派给自己的订单，这里有图片的上传操作
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean staffFinishTransaction(Map<String, Object> model,
			TbTransactionRecord record, TbMchStaff staff,
			HttpServletRequest request, HttpServletResponse response) {

		logger.info("begin to generate transaciton comment");
		String recordUUid = record.getRecordId() + "_" + StrUtil.getUUID();
		try {
			// 生成新图片的公共前缀
			String newFilePrefix = recordUUid;
			TbTransactionRecord newRecord = ParseRequest
					.generateNewTransaction(request, newFilePrefix);
			if (null == newRecord) {
				logger.error("generate new record info failed");
				return false;
			}

			String extPropsStr = record.getExtProps();
			Map<String, Object> extPropsMap = null;
			if (StringUtils.isNotBlank(extPropsStr)) {
				extPropsMap = JSON.parseObject(extPropsStr, Map.class);
			}

			extPropsMap = (extPropsMap == null) ? new HashMap() : extPropsMap;
			extPropsMap.put("recordUUid", recordUUid);

			// 把 评论以及图片上传信息合并过来
			String newRecordExtProps = newRecord.getExtProps();
			if (StringUtils.isNotBlank(newRecordExtProps)) {
				Map<String, String> newExtPropsMap = JSON.parseObject(
						newRecordExtProps, Map.class);
				if (null != newExtPropsMap) {
					Iterator entries = newExtPropsMap.entrySet().iterator();
					while (entries.hasNext()) {
						Map.Entry entry = (Map.Entry) entries.next();
						String key = (String) entry.getKey();
						String value = (String) entry.getValue();
						extPropsMap.put(key, value);
					}

				}

			}

			record.setRecordStatus(TransactionStatusEnum.FINISHED_FOR_CONFIRM
					.getStatusInt());

			extPropsMap.put("modifyTime", new Date().toLocaleString());
			record.setExtProps(JSON.toJSONString(extPropsMap));

			// 存到数据库
			wechatUserDAO.saveOrupdate(record);
			logger.warn("update transaction  id=" + record.getRecordId());
			return true;

		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}

	}

	/**
	 * 根据 request 里面的信息 分页或者全部 显示 商家自己的兑换码信息
	 * 
	 * @author happylife
	 * @param request
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public List<CacheRedeemCode> generateRedeemCodeList(
			HttpServletRequest request, Map<String, Object> model) {
		if (null == request || null == model) {
			logger.error("request or model can not be null");
			return null;
		}
		HqlQueryCondition query = new HqlQueryCondition();
		// 根据兑换码序号查询
		if (request.getParameter("query") != null) {
			String randomCode = request.getParameter("query");
			query.getProperties().add(
					new Property(Relation.EQ, "randomCode", randomCode));
		}
		// 根据是否使用查询
		if (request.getParameter("isused") != null) {
			String isused = request.getParameter("isused");
			model.put("isused", isused);
			boolean flag = false;
			if (isused.equals("used")) {
				flag = true;
				query.getProperties().add(
						new Property(Relation.EQ, "isUsed", flag));
			} else if (isused.equals("notused")) {
				flag = false;
				query.getProperties().add(
						new Property(Relation.EQ, "isUsed", flag));
			}
		} else {
			model.put("isused", "all");
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);

		CacheWechatUser user = ParseRequest.getWechatUserFromSession(request);
		if (null == user) {
			logger.error("user can not be null");
			return null;
		}

		int allPages = (wechatUserDAO.getListByProperty(TbRedeemCode.class,
				"fkOpenId", user.getWechatUser().getUserOpenid()).size()
				+ PageConfigUtil.PAGE_COUNT_SIZE_10 - 1)
				/ PageConfigUtil.PAGE_COUNT_SIZE_10;
		model.put("allPages", allPages - 1);

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_10);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_10);
		query.getProperties().add(
				new Property(Relation.EQ, "fkOpenId", user.getWechatUser()
						.getUserOpenid()));
		query.setOrderName("codeCreateTime");
		query.setAesc(false);

		String randomCode = request.getParameter("randomCode");
		logger.debug("the randomCode=" + randomCode);
		if (StringUtils.isNotBlank(randomCode)) {
			query.getProperties().add(
					new Property(Relation.EQ, "randomCode", randomCode));
			model.put("randomCode", randomCode);
		}

		@SuppressWarnings("unchecked")
		List<TbRedeemCode> redeemCodes = wechatUserDAO.getListByQueryCondtion(
				TbRedeemCode.class, query);
		// 封装下 产品信息
		if (null == redeemCodes || redeemCodes.size() == 0) {
			logger.warn("not find redeemCodes,return");
			return null;
		}

		List<CacheRedeemCode> cachedRedeemCodes = new ArrayList<CacheRedeemCode>();
		String tmpProductName = null;
		CacheRedeemCode tmpCacheCode = null;

		MchStaffProductCacheManager manager = MchStaffProductCacheManager
				.getInstance();
		for (TbRedeemCode code : redeemCodes) {
			tmpCacheCode = new CacheRedeemCode();
			tmpCacheCode.setRedeemCode(code);
			tmpProductName = manager.getProductById(code.getFkProductId())
					.getProduct().getProductName();
			tmpCacheCode.setProductName(tmpProductName);
			cachedRedeemCodes.add(tmpCacheCode);
		}
		return cachedRedeemCodes;
	}

	/**
	 * 微信用户查看自己的排队情况
	 * 
	 * @param request
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public ModelAndView querySelfQueueOrder(ModelMap model,
			HttpServletRequest request, CacheWechatUser cacheWechatUser) {
		if (null == cacheWechatUser) {
			return null;
		}

		List<TbQueueRecord> records = null;

		logger.info("the wechat cache detail records, openId="
				+ cacheWechatUser.getWechatUser().getUserOpenid());

		String fkOpenId = cacheWechatUser.getWechatUser().getUserOpenid();
		HqlQueryCondition query = new HqlQueryCondition();

		// 不是导出数据就分页显示，是导出数据则按日期检索
		query.setFirstResult(0);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_10);

		query.getProperties().add(
				new Property(Relation.EQ, "fkOpenId", fkOpenId));
		query.getProperties().add(
				new Property(Relation.LE, "status",
						QueueOrderStatusEnum.FINISHED.getStatusInt()));

		query.setOrderName("startTime");
		query.setAesc(false);

		records = wechatUserDAO.getListByQueryCondtion(TbQueueRecord.class,
				query);

		int size = records == null ? 0 : records.size();
		logger.info("the wechat cache records empty,load from db ,size=" + size);

		List<WechatQueueRecordInfo> queueInfos = new ArrayList<WechatQueueRecordInfo>();
		// 用户就没取号，提示可以尽快去取号
		if (null == records || records.size() == 0) {
			model.put("queueInfos", null);
			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX
					+ PageConfigUtil.WechatUserPage.WECHAT_QUEUE_PAGE);
		}

		// 计算每个取号当前的排队情况
		for (TbQueueRecord record : records) {

			int numFront = 0;
			Integer fkMchId = record.getFkMchId();

			List<TbQueueRecord> runningRecords = QueueOrderCacheManager
					.getInstance().getCacheMchUserQueue(fkMchId)
					.getRunningQueue();
			List<TbQueueRecord> waitingRecords = QueueOrderCacheManager
					.getInstance().getCacheMchUserQueue(fkMchId)
					.getWaitingQueue();
			String mchShopName = MchStaffProductCacheManager.getInstance()
					.getInstance().getMchUserById(fkMchId).getShopName();

			// 在正在进行队列
			if (runningRecords.indexOf(record) != -1) {
				WechatQueueRecordInfo queueInfo = new WechatQueueRecordInfo();
				queueInfo.setIsQueueIn(true);
				queueInfo.setNumFront(0);
				queueInfo.setWaitMins(0);
				queueInfo.setRecord(record);
				queueInfo.setMchShopName(mchShopName);
				queueInfos.add(queueInfo);
				continue;
			}

			// 在 等候队列中
			numFront = waitingRecords.indexOf(record);
			if (numFront != -1) {
				WechatQueueRecordInfo queueInfo = new WechatQueueRecordInfo();

				int averageMin = MchStaffProductCacheManager.getInstance()
						.getMchUserById(fkMchId).getAverageTime();
				queueInfo.setIsQueueIn(true);
				queueInfo.setNumFront(numFront);
				queueInfo.setWaitMins(averageMin * numFront);
				queueInfo.setRecord(record);
				queueInfo.setMchShopName(mchShopName);
				queueInfos.add(queueInfo);

				continue;

			}

		}

		model.put("queueInfos", queueInfos);

		return new ModelAndView(PageConfigUtil.WECHAT_PREFIX
				+ PageConfigUtil.WechatUserPage.WECHAT_QUEUE_PAGE);
	}

	/**
	 * 微信用户修改排队情况，商家完成 或者把排队从等待中转移到服务中等
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void changeQueueOrder(ModelMap model, HttpServletRequest request,
			HttpServletResponse response, CacheWechatUser cacheWechatUser) {
		// TODO Auto型？ staff mch 或者admin? 如果是mch 则跳转到整个商店的排队页面

		TbWechatUser wechatUser = cacheWechatUser.getWechatUser();

		// 解析 queuorder ID 和对应的状态
		String orderId = ParseRequest.parseRequestByType("orderid", false,
				request);
		String statusType = ParseRequest.parseRequestByType("status", true,
				request);

		logger.warn(" change queue order ,order id=" + orderId + " status = "
				+ statusType);
		if (StringUtils.isBlank(orderId) || StringUtils.isBlank(statusType)) {
			logger.error("have no orderId or statusType ,return 404");
			ResultMsgBean Msg = new ResultMsgBean(false, "排号信息不存在");
			ResponseToClient.writeJsonMsg(response, Msg);
			return;
		}

		TbMch mchUser = MchStaffProductCacheManager.getInstance()
				.getMchUserByFkOpenId(wechatUser.getUserOpenid());
		if (null == mchUser) {

			List<TbQueueRecord> records = cacheWechatUser.getRecords();
			if (null == records || records.size() == 0) {
				logger.error("the records is null, openId="
						+ wechatUser.getUserOpenid());
				ResultMsgBean Msg = new ResultMsgBean(false, "排号信息不存在");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			// 确认下这个order是否是这个用户自己的order
			TbQueueRecord exitOrder = null;
			for (TbQueueRecord record : records) {
				if (record.getRecordId().intValue() == Integer.valueOf(orderId)
						.intValue()) {
					exitOrder = record;
					break;
				}
			}

			// 更新orderRecord状态为cancel，且需要更新商家的排队中的信息
			if (null == exitOrder) {
				logger.error("the orderId=" + orderId + " not in openId="
						+ wechatUser.getUserOpenid());
				ResultMsgBean Msg = new ResultMsgBean(false, "排号信息不存在");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			exitOrder.setStatus(QueueOrderStatusEnum.CACELED.getStatusInt());
			exitOrder.setEndTime(new Date());
			// TODO ：涉及到可能要退款。。。后续流程补充
			try {
				wechatUserDAO.update(exitOrder);
				cacheWechatUser.getRecords().remove(exitOrder);

				boolean isDelet = QueueOrderCacheManager.getInstance()
						.deleteSingleOrderById(exitOrder.getFkMchId(),
								exitOrder.getRecordId());

				logger.info("now is wechat delete order success=" + isDelet);
				if (isDelet) {
					// 给商家发个消息说某个商户取消排号了
					WechatMessageUtil.sendCancelOrderToMch(exitOrder);
				}

				ResultMsgBean Msg = new ResultMsgBean(true, "操作成功");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;

			} catch (Exception e) {
				logger.error("wechat user cancel order error," + e.getMessage());
				ResultMsgBean Msg = new ResultMsgBean(false, "操作失败，请重试");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

		} else {

			// 先判断这个recordId 是否是这个mchId下面的
			TbQueueRecord exitRecord = QueueOrderCacheManager.getInstance()
					.isOrderInGivenMch(mchUser.getMchId(),
							Integer.valueOf(orderId));
			if (null == exitRecord) {
				logger.error("the mchId=" + mchUser.getMchId()
						+ " has no priority of order id=" + orderId);
				ResultMsgBean Msg = new ResultMsgBean(false, "无权限操作");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			try {

				logger.info("the exitRecord=" + exitRecord.toString()
						+ ", statusType=" + statusType);
				// 说明是商户在管理某个取号信息
				if (statusType.equalsIgnoreCase("FINISH")
						&& exitRecord.getStatus() != QueueOrderStatusEnum.FINISHED
								.getStatusInt()) {
					// 说明是完成取号服务，这时候应该是从 服务中队列去设置
					exitRecord.setEndTime(new Date());
					exitRecord.setStatus(QueueOrderStatusEnum.FINISHED
							.getStatusInt());
					wechatUserDAO.update(exitRecord);

					boolean isFinish = QueueOrderCacheManager.getInstance()
							.deleteSingleOrderById(exitRecord.getFkMchId(),
									exitRecord.getRecordId());

					logger.info("now is finish order, and isFinish=" + isFinish);
					if (isFinish) {
						WechatMessageUtil.sendFinishOrderToWechat(exitRecord);
					}

				} else if (statusType.equalsIgnoreCase("RUNNING")
						&& exitRecord.getStatus() != QueueOrderStatusEnum.RUNNING
								.getStatusInt()) {// 设置某个取号进入RUNNING队列
					exitRecord.setStatus(QueueOrderStatusEnum.RUNNING
							.getStatusInt());
					exitRecord.setEndTime(new Date());
					wechatUserDAO.update(exitRecord);

					logger.info("now set queue order  running,and  to  move order between two queue");
					TbQueueRecord record = QueueOrderCacheManager.getInstance()
							.moveRecordBetween(exitRecord.getFkMchId(),
									exitRecord.getRecordId(),
									QueueOrderStatusEnum.RUNNING, true);
					if (null == record) {
						logger.error("change record to running queue error, order id ="
								+ exitRecord.getRecordId());
						ResultMsgBean Msg = new ResultMsgBean(false, "内部错误");
						ResponseToClient.writeJsonMsg(response, Msg);
						return;
					}

				} else if (statusType.equalsIgnoreCase("WAITING")
						&& exitRecord.getStatus() != QueueOrderStatusEnum.WAITING
								.getStatusInt()) {
					exitRecord.setStatus(QueueOrderStatusEnum.WAITING
							.getStatusInt());

					logger.info("now set queue order  WAITING,and  to  move order between two queue");
					wechatUserDAO.update(exitRecord);
					TbQueueRecord record = QueueOrderCacheManager.getInstance()
							.moveRecordBetween(exitRecord.getFkMchId(),
									exitRecord.getRecordId(),
									QueueOrderStatusEnum.WAITING, true);
					if (null == record) {
						logger.error("change record to running queue error, order id ="
								+ exitRecord.getRecordId());
						ResultMsgBean Msg = new ResultMsgBean(false, "内部错误");
						ResponseToClient.writeJsonMsg(response, Msg);
						return;
					}

				} else if (statusType.equalsIgnoreCase("OUTING")
						&& exitRecord.getStatus() != QueueOrderStatusEnum.OUTING
								.getStatusInt()) {
					exitRecord.setStatus(QueueOrderStatusEnum.OUTING
							.getStatusInt());
					wechatUserDAO.update(exitRecord);
					logger.info("now set queue order  OUTING,and  to  move order between two queue");
					wechatUserDAO.update(exitRecord);
					TbQueueRecord record = QueueOrderCacheManager.getInstance()
							.moveRecordBetween(exitRecord.getFkMchId(),
									exitRecord.getRecordId(),
									QueueOrderStatusEnum.OUTING, true);
					if (null == record) {
						logger.error("change record to outing queue error, order id ="
								+ exitRecord.getRecordId());
						ResultMsgBean Msg = new ResultMsgBean(false, "内部错误");
						ResponseToClient.writeJsonMsg(response, Msg);
						return;
					}

				}

			} catch (Exception e) {
				logger.error(" process changeQueueOrder error,"
						+ e.getMessage());
				ResultMsgBean Msg = new ResultMsgBean(false, "内部错误");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			logger.warn("change queue order success,and order id=" + orderId
					+ ",status=" + statusType);

			ResultMsgBean Msg = new ResultMsgBean(true, "操作成功");
			ResponseToClient.writeJsonMsg(response, Msg);
			return;
			// return new
			// ModelAndView(PageConfigUtil.WechatUserPage.TO_QUEUE_URI);
		}

	}

	@Override
	public Object getById(Class classT, int id) throws HappyLifeException {
		try {
			// 反向从数据库查询下然后把值新的值重新填下
			@SuppressWarnings("unchecked")
			Object obj = wechatUserDAO.get(classT, id);

			return obj;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new HappyLifeException(e.getMessage());
		}
	}

}
