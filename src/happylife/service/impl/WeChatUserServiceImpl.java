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
 * ?????????????????????????????????????????????????????????
 * 
 * @author ?????????
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
	
	//??????????????????????????????
	private AtomicLong payCnt = new AtomicLong(0); 
	
	//???????????????????????????
	private AtomicLong visitCnt = new AtomicLong(0); 

	/**
	 * ?????????????????? ?????????????????????????????????????????????????????? ?????????????????????session???
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
		// ?????????????????????????????????????????????
		if (code == null || code.equals("authdeny")) {
			isValidCode = false;
		}

		logger.info("code=" + code + ", and isValidCode=" + isValidCode);
		// ??????session???????????????????????????????????????????????????
		if ((!isValidCode) && session.getAttribute("wechatuser") == null) {
			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
			logger.warn("the user session does not exit redirect to :open.weixin.qq.com/connect/oauth2/authorize");
			return false;
		}
		// ???????????????????????????????????????session??????????????????OAUTH??????????????????????????????
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

			// ????????????????????????
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
	 * ????????????????????????signature????????????
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
		// ??????????????????
		String signature = request.getParameter("signature");
		// ?????????
		String timestamp = request.getParameter("timestamp");
		// ?????????
		String nonce = request.getParameter("nonce");
		// ???????????????
		String echostr = request.getParameter("echostr");
		logger.warn("check signature,signature=" + signature + ",timestamp="
				+ timestamp + ",nonce=" + nonce + ",echostr=" + echostr);

		PrintWriter out = null;
		try {
			out = response.getWriter();
			boolean checkResult = WeChatRequestUtil.checkWeChatSignature(
					signature, timestamp, nonce);
			// ????????????signature??????????????????????????????????????????????????????echostr??????????????????????????????????????????
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
	 * ????????????signature????????????????????????????????????????????????????????????????????????????????????????????????????????????
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
		// ???????????????????????????????????????UTF-8????????????????????????
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
	 * ???????????????????????????????????????
	 * 
	 * @param request
	 * @return
	 */
	private String processRequest(HttpServletRequest request) {
		String respMessage = "";
		try {
			logger.debug("handle the request");
			// ?????????????????????????????????
			String respContent = "";
			// xml????????????
			Map<String, String> requestMap = WechatMessageUtil
					.parseXml(request);

			// ??????????????????open_id???
			String fromUserName = requestMap.get("FromUserName");
			// ????????????
			String toUserName = requestMap.get("ToUserName");
			// ????????????
			String msgType = requestMap.get("MsgType");

			// ??????????????????
			TextMessage textMessage = new TextMessage();
			textMessage.setToUserName(fromUserName);
			textMessage.setFromUserName(toUserName);
			textMessage.setCreateTime(new Date().getTime());
			textMessage
					.setMsgType(WechatMessageUtil.MessageType.RESP_MESSAGE_TYPE_TEXT);
			textMessage.setFuncFlag(0);

			// ????????????
			if (msgType
					.equals(WechatMessageUtil.MessageType.REQ_MESSAGE_TYPE_TEXT)) {
				// TODO : ????????????????????????
				logger.warn("need to do,  nothing");
			} else if (msgType
					.equals(WechatMessageUtil.MessageType.REQ_MESSAGE_TYPE_EVENT)) {
				// ????????????
				String eventType = requestMap.get("Event");
				// ??????
				if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_SUBSCRIBE)) {
					respContent = "??????????????????";
					// subscribe(fromUserName);

				}
				// ???????????????????????????
				else if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_SCAN)) {
					// ??????KEY??????????????????????????????????????????KEY?????????
					respContent = "??????????????????";
					// subscribe(fromUserName);
				}
				// ????????????
				else if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_UNSUBSCRIBE)) {
					return null;
				} else if (eventType
						.equals(WechatMessageUtil.MessageType.EVENT_TYPE_CLICK)) {
					// ??????KEY??????????????????????????????????????????KEY?????????
					respContent = "????????????????????????";
				}
			} else {
				logger.warn(" ????????? ?????????");
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
	 * ??????????????????????????????????????????????????????????????????????????????????????? ???model ????????????????????? ??????????????????????????????
	 * 
	 * @param request
	 * @param model
	 * @return true ?????? ?????? false ????????????
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

		// ???ext_props???????????? ??????
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
		// ??????????????????
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
	 * ??????????????????????????????????????????????????????????????????????????????????????? ???model ????????????????????? ??????????????????????????????
	 * 
	 * @param request
	 * @param model
	 * @return true ?????? ?????? false ????????????
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
		// ??????name?????????request????????????????????????name
		List<MchSearchCondition> conditions = ParseRequest
				.generateMchSearchConditions(request);
		String mchName = (String) request
				.getParameter(MchSearchCondition.ConditionName.MCH_NAME);
		/* ???????????????????????? */
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
	 * ????????????????????? ??????????????? ?????????????????????????????????????????? ???????????????????????????????????????????????????????????????????????????????????? ?????????????????? ??? ???model
	 * ????????????????????? ??????????????????????????????
	 * 
	 * @param request
	 * @param model
	 * @return true ?????? ?????? false ????????????
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

		/* ??????????????????????????? ??? ??????????????? ????????????????????? */
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
			// TODO : ??????????????????????????? ?????? ??????
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
	// return "????????????";
	// }

	@SuppressWarnings("unchecked")
	/**
	 * ?????????????????????????????? ??????????????????????????????
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

		// ??????mchId
		String mchId = ParseRequest.parseRequestByType("mchid", false, request);
		if (StringUtils.isBlank(mchId)) {
			logger.error("the mchId can not be null");
			ResultMsgBean Msg = new ResultMsgBean(false, "???????????????????????????");
			ResponseToClient.writeJsonMsg(response, Msg);
			return;
		}

		String optType = ParseRequest.parseRequestByType("type", true, request);
		if (StringUtils.isBlank(optType)) {
			logger.warn("the opt type is null ");
			optType = "add";// ?????????????????????????????????
		}

		try {
			// ???ext_props???????????? ??????
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

			// ????????????????????????10???
			if (mchIds.size() > WinterOrangeSysConf.WECHAT_FOLLOW_MCHS_COUNT) {
				logger.error("the follow mch count exceed");
				ResultMsgBean Msg = new ResultMsgBean(false, "???????????????????????????????????????");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			processFavoriteMchs(mchIds, mchId, optType);
			extPropsMap.put("favoriteMchIds", mchIds);

			TbWechatUser wechatUser = cacheChatUser.getWechatUser();

			wechatUser.setExtProps(JSON.toJSONString(extPropsMap));

			wechatUserDAO.update(wechatUser);

			// ????????????????????????????????? ?????????????????????????????????????????????????????????????????????
			List<TbMchWechatRelation> records = wechatUserDAO
					.getListByProperty(TbMchWechatRelation.class, "fkOpenId",
							cacheChatUser.getWechatUser().getUserOpenid());
			TbMchWechatRelation newRelation = null;
			if (null != records && records.size() > 0) {
				for (TbMchWechatRelation record : records) {

					// ????????? ?????????
					if (record.getFkMchId() == Integer.valueOf(mchId)
							&& record.getFkOpenId().equals(
									cacheChatUser.getWechatUser()
											.getUserOpenid())) {
						newRelation = record;

					}

				}

			}

			// ???????????????????????????????????????
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
			// ????????????
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

			// ??????????????????????????????
			wechatUserDAO.saveOrupdate(mch);

			request.getSession().setAttribute(
					IndexConfig.SESSION_WECHATUSER_KEY, cacheChatUser);
			logger.info("the wechat user follow mch succes  mchid=" + mchId);
			ResultMsgBean Msg = new ResultMsgBean(true, "???????????????????????????->??????????????????");

			if (!optType.equals("add")) {
				Msg = new ResultMsgBean(true, "??????????????????????????????->??????????????????");
			}

			ResponseToClient.writeJsonMsg(response, Msg);
			return;

		} catch (Exception e) {
			logger.error(e.getMessage());

			ResultMsgBean Msg = new ResultMsgBean(false, "??????????????????????????????");
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
	 * ????????????
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
		// ???????????? ???????????????????????? ?????????????????? ??? ??????
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
			// ??????????????????,?????????model ??????

			// ????????????????????? ??????
			String productNum = ParseRequest.parseRequestByType("productNum",
					false, request);
			int productNumInt = productNum == null ? 1 : Integer
					.valueOf(productNum);

			int priceFen = (int) (product.getDiscountPrice() * 100 * productNumInt);
			String out_trade_no = new SimpleDateFormat("yyyyMMddHHmmssSSS")
					.format(new Date());// ??????????????????????????????,32 ??????????????????????????????,??????
										// ?????????????????????,????????????

			WeChatRequestUtil.wechatPayRequest(model, priceFen, wechatUser
					.getWechatUser().getUserOpenid(), out_trade_no, request);

			// ???????????? ????????????0????????????????????????????????????1???
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

			// ????????? ????????????
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
	 * ???????????????????????????
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

		// ?????????????????????????????? ?????????????????????????????????????????????????????????
		if (WinterOrangeSysConf.IS_FREE_SERVICE || isMchOrder) {
			TbTransactionRecord temRecord = new TbTransactionRecord();
			temRecord.setFkMchId(mch.getMchId());
			temRecord.setRecordId(-1);// ???????????????ID
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

			// ???????????? ??????url
			WeChatRequestUtil.redirectToGivenUri(response,
					request.getContextPath() + PageConfigUtil.WECHAT_PREFIX
							+ PageConfigUtil.WechatUserPage.TO_QUEUE_URI);
			return null;
		}

		// ?????????????????????
		try {
			// ??????????????????,?????????model ??????
			int priceFen = WinterOrangeSysConf.QUEUE_ORDER_MONEY_ONECE; // ??????????????????????????????

			String out_trade_no = new SimpleDateFormat("yyyyMMddHHmmssSSS")
					.format(new Date());// ??????????????????????????????,32 ??????????????????????????????,??????
										// ?????????????????????,????????????

			WeChatRequestUtil.wechatPayRequest(model, priceFen,
					wechatUser.getUserOpenid(), out_trade_no, request);

			// ???????????? ????????????0????????????????????????????????????1???
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
			// ????????? ????????????
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
	 * ???????????? ?????? ????????????
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
					"????????????"));

		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"????????????"));
			return;
		}

	}

	/**
	 * ?????? ?????? ???????????????????????????
	 * 
	 * @param request
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public void processWechatPayBack(ModelMap model,
			HttpServletRequest request, HttpServletResponse response) {

		try {
			WechatPayMsg wxmsg = WeChatRequestUtil.parseWxPayResponse(request);
			// ??????out_trade_no??????????????????????????????
			TbTransactionRecord record = (TbTransactionRecord) wechatUserDAO
					.getObjectByProperty(TbTransactionRecord.class,
							"outTradeNo", wxmsg.getOut_trade_no());

			logger.info("processwechatpayback:wxmsg=" + wxmsg.toString());

			if (record == null) {
				logger.error("processwechatpayback: record not exit,wxmsg="
						+ wxmsg.getOut_trade_no());
				// ???????????????
				response.getWriter().println("FAIL");
			
			    //????????????????????????
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
			// ????????? ?????????????????????
			if (wxmsg.getAttach().equals(WeChatConfig.WECHAT_ATTACK)) {
				if (record.getRecordStatus() == 0) {
					// ?????????
					record.setRecordStatus(1);
					// ?????? record ??????
					wechatUserDAO.saveOrupdate(record);
					logger.info("update the record success,out_trade_no="
							+ wxmsg.getOut_trade_no());
					// ????????????????????????
					// CacheWechatUser cacheWechatUser =
					// ParseRequest.getWechatUserFromSession(request);
					// TbWechatUser wechatUser =
					// cacheWechatUser.getWechatUser();
					// wechatUser.setUserRewardPoint(wechatUser
					// .getUserRewardPoint() + record.getRecordMoney());
					// wechatUserDAO.saveOrupdate(wechatUser);
					//
					// logger.info("update the wechatuser success,wechat wechatinfo="+wechatUser.toString());

					// ???????????????recordType??????????????????
					processByRecordType(record, response);

					response.getWriter().println("SUCCESS"); // ????????????????????????
					return;
				} else {

					logger.info("the record is processed, out_trade_no="
							+ wxmsg.getOut_trade_no() + ",record id="
							+ record.getRecordId());
					// ??????????????????
					response.getWriter().println("SUCCESS"); // ????????????????????????
					return;
				}
			} else {
				// ???????????????
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

	// ??????????????? ??????????????????????????????
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
			// 0: ?????? 1 ?????????
			redeemCode.setCreateType(0);
			redeemCode.setProductNum(record.getProductNum());
			redeemCode.setPayMoney(record.getRecordMoney());
			// ?????????????????? ???????????????????????? ???????????? ???????????? ????????????????????????
			redeemCode.setValidDate(null);
			redeemCode.setUsedTime(null);
			redeemCode.setCodeCreateTime(new Date());
			redeemCode.setIsSendok(0);

			wechatUserDAO.saveOrupdate(redeemCode);

			JSONObject returnWechat = WechatMessageUtil
					.sendRedeemCodeInfoMsg(redeemCode);
			if (null == returnWechat) {
				// ????????????????????? ?????????????????????????????????
				response.getWriter().println("FAIL");
				return;
			}

			logger.info("the send message return:"
					+ returnWechat.toJSONString());
			// ??????????????? ?????????????????????
			redeemCode.setIsSendok(1);
			wechatUserDAO.saveOrupdate(redeemCode);
		}
		// ????????????
		else if (recordType.equalsIgnoreCase(TransactionTypeConfig.QUEUE_ORDER)) {

			logger.info("the record is for queue,to queue page...");
			processNewOrderIn(record, response);

		}// ????????????
		else if (recordType.equalsIgnoreCase(TransactionTypeConfig.BUY_PRODUCT)) {
			logger.warn("the record type is bug product , nothing to do !");
		}
		
		
		//????????????????????????????????????????????? 
		TbMch  mchUser = MchStaffProductCacheManager.getInstance().getMchUserById(record.getFkMchId());
		synchronized(mchUser){
			mchUser.setTotalSaleCount(mchUser.getTotalSaleCount()+record.getProductNum());
			mchUser.setTotalMoney(mchUser.getTotalMoney()+record.getRecordMoney());
		}
	
	}

	// ????????????????????????
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

		// ?????? ???????????????????????????????????????????????????????????????
		synchronized (mchOrder) {
			String orderId = null;
			
			//?????????100 ?????????????????????
			if(mchOrder.getQueueNum().get() > 99){
				logger.warn("the mch="+record.getFkMchId()+", exceed 99,now from 1 start ");
				mchOrder.getQueueNum().set(1);
			}

			if (isMchOrder) {
				orderId = serviceType + "-"
						+ mchOrder.getQueueNum().getAndIncrement() + "-???";
			} else {
				orderId = serviceType + "-"
						+ mchOrder.getQueueNum().getAndIncrement();
			}
			newQueue.setOrderId(orderId);
			// ????????????????????????
			@SuppressWarnings("unchecked")
			Integer id = (Integer) wechatUserDAO.add(newQueue);
			logger.info("save the queue order success, id=" + id);
			newQueue.setRecordId(id);
			waitNum = mchOrder.getWaitingQueue().size();
			mchOrder.getWaitingQueue().add(newQueue);
			logger.warn("fetch queue order success, order id=" + orderId
					+ ", mchid=" + record.getFkMchId());
		}
		
		//???????????????????????????????????????
		WechatMessageUtil.sendOrderSuccessMsgToWechat(newQueue, waitNum);
	}

	/**
	 * staff ????????????????????????????????????
	 */
	@Override
	public List<CacheTransaction> staffGetTransaction(
			Map<String, Object> model, TbMchStaff staff,
			HttpServletRequest request, HttpServletResponse response) {
		List<TbTransactionRecord> transactions = generateTransactionList(
				request, model, staff);
		// ????????????????????? ???????????????
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

	// ??????wechat ?????????????????????
	@SuppressWarnings("rawtypes")
	@Override
	public List wechatGetTransactions(Map<String, Object> model,
			TbWechatUser wechat, HttpServletRequest request,
			HttpServletResponse response) {

		String fkOpenId = wechat.getUserOpenid();
		request.setAttribute("fkOpenId", fkOpenId);

		List<TbTransactionRecord> transactions = generateTransactionList(
				request, model, null);
		// ????????????????????? ???????????????
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
	 * ?????????????????? ??? ??????????????????????????????????????????????????????????????? ??????
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

	// staff ???????????????????????????????????????????????????????????????
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean staffFinishTransaction(Map<String, Object> model,
			TbTransactionRecord record, TbMchStaff staff,
			HttpServletRequest request, HttpServletResponse response) {

		logger.info("begin to generate transaciton comment");
		String recordUUid = record.getRecordId() + "_" + StrUtil.getUUID();
		try {
			// ??????????????????????????????
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

			// ??? ??????????????????????????????????????????
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

			// ???????????????
			wechatUserDAO.saveOrupdate(record);
			logger.warn("update transaction  id=" + record.getRecordId());
			return true;

		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}

	}

	/**
	 * ?????? request ??????????????? ?????????????????? ?????? ??????????????????????????????
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
		// ???????????????????????????
		if (request.getParameter("query") != null) {
			String randomCode = request.getParameter("query");
			query.getProperties().add(
					new Property(Relation.EQ, "randomCode", randomCode));
		}
		// ????????????????????????
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
		// ????????? ????????????
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
	 * ???????????????????????????????????????
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

		// ?????????????????????????????????????????????????????????????????????
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
		// ????????????????????????????????????????????????
		if (null == records || records.size() == 0) {
			model.put("queueInfos", null);
			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX
					+ PageConfigUtil.WechatUserPage.WECHAT_QUEUE_PAGE);
		}

		// ???????????????????????????????????????
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

			// ?????????????????????
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

			// ??? ???????????????
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
	 * ????????????????????????????????????????????? ????????????????????????????????????????????????
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void changeQueueOrder(ModelMap model, HttpServletRequest request,
			HttpServletResponse response, CacheWechatUser cacheWechatUser) {
		// TODO Auto?????? staff mch ??????admin? ?????????mch ???????????????????????????????????????

		TbWechatUser wechatUser = cacheWechatUser.getWechatUser();

		// ?????? queuorder ID ??????????????????
		String orderId = ParseRequest.parseRequestByType("orderid", false,
				request);
		String statusType = ParseRequest.parseRequestByType("status", true,
				request);

		logger.warn(" change queue order ,order id=" + orderId + " status = "
				+ statusType);
		if (StringUtils.isBlank(orderId) || StringUtils.isBlank(statusType)) {
			logger.error("have no orderId or statusType ,return 404");
			ResultMsgBean Msg = new ResultMsgBean(false, "?????????????????????");
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
				ResultMsgBean Msg = new ResultMsgBean(false, "?????????????????????");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			// ???????????????order??????????????????????????????order
			TbQueueRecord exitOrder = null;
			for (TbQueueRecord record : records) {
				if (record.getRecordId().intValue() == Integer.valueOf(orderId)
						.intValue()) {
					exitOrder = record;
					break;
				}
			}

			// ??????orderRecord?????????cancel?????????????????????????????????????????????
			if (null == exitOrder) {
				logger.error("the orderId=" + orderId + " not in openId="
						+ wechatUser.getUserOpenid());
				ResultMsgBean Msg = new ResultMsgBean(false, "?????????????????????");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			exitOrder.setStatus(QueueOrderStatusEnum.CACELED.getStatusInt());
			exitOrder.setEndTime(new Date());
			// TODO ??????????????????????????????????????????????????????
			try {
				wechatUserDAO.update(exitOrder);
				cacheWechatUser.getRecords().remove(exitOrder);

				boolean isDelet = QueueOrderCacheManager.getInstance()
						.deleteSingleOrderById(exitOrder.getFkMchId(),
								exitOrder.getRecordId());

				logger.info("now is wechat delete order success=" + isDelet);
				if (isDelet) {
					// ???????????????????????????????????????????????????
					WechatMessageUtil.sendCancelOrderToMch(exitOrder);
				}

				ResultMsgBean Msg = new ResultMsgBean(true, "????????????");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;

			} catch (Exception e) {
				logger.error("wechat user cancel order error," + e.getMessage());
				ResultMsgBean Msg = new ResultMsgBean(false, "????????????????????????");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

		} else {

			// ???????????????recordId ???????????????mchId?????????
			TbQueueRecord exitRecord = QueueOrderCacheManager.getInstance()
					.isOrderInGivenMch(mchUser.getMchId(),
							Integer.valueOf(orderId));
			if (null == exitRecord) {
				logger.error("the mchId=" + mchUser.getMchId()
						+ " has no priority of order id=" + orderId);
				ResultMsgBean Msg = new ResultMsgBean(false, "???????????????");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			try {

				logger.info("the exitRecord=" + exitRecord.toString()
						+ ", statusType=" + statusType);
				// ??????????????????????????????????????????
				if (statusType.equalsIgnoreCase("FINISH")
						&& exitRecord.getStatus() != QueueOrderStatusEnum.FINISHED
								.getStatusInt()) {
					// ??????????????????????????????????????????????????? ????????????????????????
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
								.getStatusInt()) {// ????????????????????????RUNNING??????
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
						ResultMsgBean Msg = new ResultMsgBean(false, "????????????");
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
						ResultMsgBean Msg = new ResultMsgBean(false, "????????????");
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
						ResultMsgBean Msg = new ResultMsgBean(false, "????????????");
						ResponseToClient.writeJsonMsg(response, Msg);
						return;
					}

				}

			} catch (Exception e) {
				logger.error(" process changeQueueOrder error,"
						+ e.getMessage());
				ResultMsgBean Msg = new ResultMsgBean(false, "????????????");
				ResponseToClient.writeJsonMsg(response, Msg);
				return;
			}

			logger.warn("change queue order success,and order id=" + orderId
					+ ",status=" + statusType);

			ResultMsgBean Msg = new ResultMsgBean(true, "????????????");
			ResponseToClient.writeJsonMsg(response, Msg);
			return;
			// return new
			// ModelAndView(PageConfigUtil.WechatUserPage.TO_QUEUE_URI);
		}

	}

	@Override
	public Object getById(Class classT, int id) throws HappyLifeException {
		try {
			// ????????????????????????????????????????????????????????????
			@SuppressWarnings("unchecked")
			Object obj = wechatUserDAO.get(classT, id);

			return obj;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new HappyLifeException(e.getMessage());
		}
	}

}
