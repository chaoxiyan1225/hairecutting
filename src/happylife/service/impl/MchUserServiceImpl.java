package happylife.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import happylife.dao.GenericDao;
import happylife.model.*;
import happylife.model.servicemodel.*;
import happylife.model.servicemodel.HqlQueryCondition.Property;
import happylife.model.servicemodel.HqlQueryCondition.Relation;
import happylife.service.MchUserService;
import happylife.service.exception.HappyLifeException;
import happylife.service.exception.RequestInvalidException;
import happylife.service.exception.SessionInvalidException;
import happylife.util.DateUtil;
import happylife.util.DesUtil;
import happylife.util.StrUtil;
import happylife.util.cache.CacheObjectPageUtil;
import happylife.util.cache.MchStaffProductCacheManager;
import happylife.util.config.IndexConfig;
import happylife.util.config.PageConfigUtil;
import happylife.util.requestandresponse.MenuSwitchShowManager;
import happylife.util.requestandresponse.ParseRequest;
import happylife.util.requestandresponse.ResponseToClient;
import happylife.util.requestandresponse.WechatMessageUtil;
import happylife.util.requestandresponse.messagebean.ResultMsgBean;
import happylife.util.service.MchUserCareOfUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 微信用户通过界面与系统交互的业务处理层
 * 
 * @author 闫朝喜
 * 
 */
@Service
public class MchUserServiceImpl extends GenericServiceImpl
		implements MchUserService {
	
	private static final Log logger = LogFactory
			.getLog(MchUserServiceImpl.class);

	@SuppressWarnings("rawtypes")
	private GenericDao mchUserDAO;


	@SuppressWarnings("rawtypes")
	public void setMchUserDAO(GenericDao mchUserDAO) {
		this.mchUserDAO = mchUserDAO;
	}

	/**
	 * 验证登录商家的用户名以及密码是否正确
	 * 
	 * @param
	 * @return
	 */
	public boolean checkLogin(TbMch mchUserInfo) {
		if (null == mchUserInfo || StringUtils.isBlank(mchUserInfo.getMchName())
				|| StringUtils.isBlank(mchUserInfo.getMchPasswd())) {
			logger.warn("the mchUser login error");
			// ResponseToUser.writeJsonMsg(response, new
			// ResultMsgBean(false,"登录信息错误"));
			return false;
		}

		TbMch cacheMchUser = MchStaffProductCacheManager.getInstance()
				.getMchUserByName(mchUserInfo.getMchName());

		if (null == cacheMchUser) {
			logger.warn("the mcUser info not exit " + mchUserInfo.getMchName());
			// ResponseToUser.writeJsonMsg(response, new
			// ResultMsgBean(false,"用户名不存在"));
			return false;
		}

		// 数据库中的账户密码是密文存储的，需要对界面的密码加密后比较
		if (DesUtil.encrypt(mchUserInfo.getMchPasswd()).equals(
				cacheMchUser.getMchPasswd())) {
			logger.warn("the mcUser  exit " + mchUserInfo.getMchName());

			mchUserInfo.setMchId(cacheMchUser.getMchId());
			return true;
		}

		// ResponseToUser.writeJsonMsg(response, new
		// ResultMsgBean(false,"密码错误"));
		// 密码错误
		logger.warn("the mcUser passwd error:" + mchUserInfo.getMchPasswd());
		return false;
	}

	/**
	 * 根据 mchId 获取该用户是否存在
	 * 
	 * @param aid
	 * @return
	 */
	public TbMch getMchUserInfoById(String aid) {
		return null;
	}

	/**
	 * 商家直接获取 自己下面的所有产品信息 ：原则上这个商品信息不会太多，无需分页，全部显示
	 * 
	 * @param request
	 * @param
	 * @return
	 * @throws SessionInvalidException
	 */
	public List<TbProduct> generateProductsList(HttpServletRequest request,
			ModelMap map) throws SessionInvalidException {
		if (null == request || null == map) {
			logger.error("request or model can not be null");
			return null;
		}

		// 解析当前页并放到 session中
		ParseRequest.parseCurrentPage(request, map);

		// 解析商家账户信息
		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchUserId) {
			logger.error("the mchuser not login ,need relogin");
			throw new SessionInvalidException("mch user not login");
		}

		List<TbProduct> products = MchStaffProductCacheManager.getInstance()
				.getProductsByMchId(fkMchUserId);

		return products;

	}

	/**
	 * 根据request里面的信息添加一个新的商品： 还有图片信息需要处理
	 * 
	 * @author happylife
	 * @param
	 * @return 返回添加成功的商品实体，失败的话返回null
	 */
	@SuppressWarnings("unchecked")
	public CacheProduct addNewSingleProduct(HttpServletRequest request) {
		// 从request里面提取商品信息

		// 把商家信息读取出来
		String fkMchId = (String) request.getSession().getAttribute(
				IndexConfig.MCHUSER_SESSION_KEY);

		String fkMchIdStr = DesUtil.decrypt(fkMchId);

		logger.info("begin to mchuserInfo fkMchIdStr:" + fkMchIdStr);
		/* 这样的方式默认按照 以 商家为维度 查询商品的方式 */
		if (StringUtils.isBlank(fkMchId) || !StringUtils.isNumeric(fkMchIdStr)) {
			logger.error("fkMchId can not be null,or not numirreturn");
			return null;
		}

		// 生成新图片的公共前缀
		String newFilePrefix = fkMchIdStr + "_" + StrUtil.getRandomString();
		TbProduct newProduct = ParseRequest.generateNewProductInfo(request,
				newFilePrefix);
		if (null == newProduct) {
			logger.error("fkMchId can not be null,or not numirreturn");
			return null;
		}
		newProduct.setFkMchId(Integer.valueOf(fkMchIdStr));

		// 存到数据库
		mchUserDAO.add(newProduct);

		// 刷新内存
		
		MchStaffProductCacheManager.getInstance().addProduct(newProduct);

		TbMch mchUser = MchStaffProductCacheManager.getInstance()
				.getMchUserById(Integer.valueOf(fkMchIdStr));
		CacheProduct cacheProduct = new CacheProduct();
		cacheProduct.setMchUser(mchUser);
		cacheProduct.setProduct(newProduct);

		return cacheProduct;
	}

	/**
	 * 根据request里面的信息 更新一个商品信息
	 * 
	 * @author happylife
	 * @param requst
	 * @return 返回更新成功的 商品实体， 失败的话返回 null
	 */
	public CacheProduct updateProductInfo(HttpServletRequest requst) {
		return null;
	}

	/**
	 * 更新产品的状态： 由 下架到上架等。出错会直接通过response 返回
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public void changeProductStatus(HttpServletRequest request,
			HttpServletResponse response) {
		// 把商家信息读取出来
		Integer fkMchId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchId) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"用户信息不对，请重新登录"));
			return;
		}

		String status = ParseRequest.parseRequestByType("status", false,
				request);
		if (null == status) {
			logger.error("product status can not be null,or not numirreturn");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"不支持设置状态为：" + status));
			return;
		}

		CacheProduct cacheProduct = ParseRequest.findCacheProduct(request);
		if (null == cacheProduct) {
			logger.error("productId can not be null,or not numirreturn");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"不存在该商品  "));
			return;
		}

		// 检查该用户是否在编辑自己的商品
		TbProduct product = cacheProduct.getProduct();

		if (product.getFkMchId() != fkMchId) {
			logger.error("the mchUserid=" + fkMchId
					+ ",have no permition for productId="
					+ product.getProductId());
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"无权限对该商品：Id=" + product.getProductId()));
			return;
		}

		int oldStatus = product.getProductStatus();
		// 只有状态确实存在编号时候才刷新数据库
		if (Integer.valueOf(status) != product.getProductStatus()) {
			try {
				logger.info("update the  mchUserinfo to db.");
				product.setProductStatus(Integer.valueOf(status));
				mchUserDAO.update(product);

				logger.warn("update product status success to：" + status);
				ResponseToClient.writeJsonMsg(response, new ResultMsgBean(true,
						"更新成功"));

			} catch (DataAccessException e) {
				logger.error(e.getMessage());
				ResponseToClient.writeJsonMsg(response, new ResultMsgBean(
						false, "更新失败"));
				// 还原 缓存中的 tbUser信息
				product.setProductStatus(oldStatus);

				return;
			}
		} else {
			logger.error("same status , no need to update");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"更新成功"));
			return;

		}

		return;

	}

	/**
	 * 更新兑换码的状态： 已使用 未使用， 如果兑换码未发送成功则重新发送。出错会直接通过response 返回
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public void changeRedeemCodeStatus(HttpServletRequest request,
			HttpServletResponse response, boolean isResend) {
		// 把商家信息读取出来
		Integer fkMchId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchId) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"用户信息不对，请重新登录"));
			return;
		}

		String redeemcode = request.getParameter("redeemcode");
		if (StringUtils.isBlank(redeemcode)
				|| !StringUtils.isNumeric(redeemcode)) {
			logger.error("redeemcode can not be null");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"不存在该兑换码 id=" + redeemcode));
			return;
		}

		// 只有状态确实存在编号时候才刷新数据库
		TbRedeemCode redeemCodeOld = null;
		try {
			// 查询下该兑换码
			redeemCodeOld = (TbRedeemCode) mchUserDAO.getObjectByProperty(
					TbRedeemCode.class, "codeId", Integer.valueOf(redeemcode));
			logger.info("update the  redeemCodeOld to db.");
			//如果是重新发送兑换码
			if (isResend) {
				JSONObject sendReturn = WechatMessageUtil
						.sendRedeemCodeInfoMsg(redeemCodeOld);
				
				
//			    {
//			           "errcode":0,
//			           "errmsg":"ok",
//			           "template_id":"Doclyl5uP7Aciu-qZ7mJNPtWkbkYnWBWVja26EGbNyk"
//			       }
				
				
				if (null != sendReturn && sendReturn.get("errcode").toString().equals("0")) {
					redeemCodeOld.setIsSendok(1);
				} 
				//发送失败则退出
				else {
					logger.warn("redeem code send error");
					ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
							"发送失败"));
					logger.error(sendReturn.toJSONString());
					return ;
				}

			}
			// 直接设置兑换码已经使用
			else {
				redeemCodeOld.setIsUsed(true);
			}

			mchUserDAO.update(redeemCodeOld);
			logger.warn("update redeemCode status to used");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(true,
					"更新成功"));

		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"失败请重试"));
			return;
		}

		return;
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
		}else{
			model.put("isused", "all");
		}
		
		int currentPage = ParseRequest.parseCurrentPage(request, model);
		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		
		List  allPage = mchUserDAO.getListByProperty(TbRedeemCode.class,
				"fkMchId", fkMchUserId);
		
		int allPages = 0;
		if(allPage == null){
			allPages = 0;
			
		}else{
		   allPages = (mchUserDAO.getListByProperty(TbRedeemCode.class,
				"fkMchId", fkMchUserId).size()
				+ PageConfigUtil.PAGE_COUNT_SIZE_10 - 1)
				/ PageConfigUtil.PAGE_COUNT_SIZE_10;
		}
		model.put("allPages", allPages - 1);
		if (null == fkMchUserId) {
			logger.error("fkMchUserId can not be null");
			return null;
		}

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_10);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_10);
		query.getProperties().add(
				new Property(Relation.EQ, "fkMchId", fkMchUserId));
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
		List<TbRedeemCode> redeemCodes = mchUserDAO.getListByQueryCondtion(
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
	 * 更新某个兑换码信息
	 * 
	 * @author happylife
	 * @param request
	 * @return: 返回跟新后的 兑换码信息, null 表示更新 失败
	 */
	public TbRedeemCode updateRedeemCodeInfo(HttpServletRequest request) {
		return null;
	}

	/**
	 * 根据查询条件 ： 分页或者全部显示的方式查询具体的商家账户的 流水
	 * 
	 * @author snnile2012
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TbTransactionRecord> generateTransactionList(
			HttpServletRequest request, Map<String, Object> model,
			boolean isExportData) {
		if (null == request || null == model) {
			logger.error("request or model can not be null");
			return null;
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);

		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchUserId) {
			logger.error("fk mch user not exit");
			return null;
		}

		HqlQueryCondition query = new HqlQueryCondition();

		// 不是导出数据就分页显示，是导出数据则按照日期检索
		if (!isExportData) {
			query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
			query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);
		} else {
			String year = request.getParameter("year");
			String month = request.getParameter("month");
			if (StringUtils.isBlank(year) || StringUtils.isBlank(month)) {
				logger.error("request must have year and month");
				return null;
			}
			logger.info("year=" + year + ",month=" + month);

			Date selectDate = DateUtil.StringToDate(year + "-" + month,
					"yyyy-MM");
			Date start = DateUtil.getFirstDayOfMonth(selectDate);
			Date end = DateUtil.getLastDayOfMonth(selectDate);
			query.getProperties().add(
					new Property(Relation.GE, "recordTime", start));
			query.getProperties().add(
					new Property(Relation.LE, "recordTime", end));

		}

		query.getProperties().add(
				new Property(Relation.EQ, "fkMchId", fkMchUserId));
		query.setOrderName("recordTime");
		query.setAesc(false);

		String fkProductId = request.getParameter("fkProductId");
		if (StringUtils.isNotBlank(fkProductId)
				&& StringUtils.isNumeric(fkProductId)) {
			model.put("fkProductId", fkProductId);
			query.getProperties().add(
					new Property(Relation.EQ, "fkProductId", Integer
							.valueOf(fkProductId)));
		}
		
		long count = mchUserDAO.getCountByQueryCondtion(TbTransactionRecord.class, query);
		
		model.put("allTransactionPages", count);
		
		return mchUserDAO.getListByQueryCondtion(TbTransactionRecord.class,
				query);
	}
	
	

	/**
	 * 根据查询条件： 分页或者全部查询商家自己订单信息， 这里会返回商品名称以及商家名等信息
	 * 
	 * @param request
	 * @param model
	 * @return
	 */
	public List<CacheTransaction> generateCacheTransactionList(
			HttpServletRequest request, Map<String, Object> model,
			boolean isExportData) {

		List<TbTransactionRecord> transactions = generateTransactionList(
				request, model, isExportData);
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
			
			TransactionStatusEnum[] statusEnums = TransactionStatusEnum.values();
			for(TransactionStatusEnum tran:statusEnums){
				if(tran.getStatusInt() == transaction.getRecordStatus()){
					tmpCacheTransaction.setStatusMsg(tran.getStatusMsg());
					break ;
				}
			}
			

			//tmpProductName = manager
					//.getProductById(transaction.getFkProductId()).getProduct()
					//.getProductName();
			//tmpCacheTransaction.setProductName(tmpProductName);

			tmpMchShopName = manager.getMchUserById(transaction.getFkMchId())
					.getShopName();
			
			tmpCacheTransaction.setMchShopName(tmpMchShopName);
			
			TbMchStaff staff = manager.getStaff(transaction.getFkStaffId(), null);
			
			if(null != staff){
				tmpCacheTransaction.setMchStaff(staff);
			}
			
			cacheTransactions.add(tmpCacheTransaction);
			
		}

		return cacheTransactions;
	}

	/**
	 * 商家账户更新自己的密码
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void changeMchUserPasswd(HttpServletRequest request,
			HttpServletResponse response) {
		String[] changePasswdInfo = ParseRequest
				.genChangePasswdInfo(request);
		if (null == changePasswdInfo) {
			logger.error("the mchuser change passwd info not right.");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"请同时输入新旧密码"));
			return;
		}

		if (null == response) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"信息有误，请重新输入"));
			return;
		}

		// 判断新密码的是否一致
		if (!changePasswdInfo[1].equals(changePasswdInfo[2])) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"两次密码不一致，重新输入"));
			return;
		}

		// 校验新密码的复杂度
		// ^[a-zA-Z0-9_]+$
		if (!StrUtil.isPasswdValid(changePasswdInfo[1])) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"密码不符合要求，重新输入"));
			return;
		}

		// 更新数据库并更新内存，且 清除session中的用户登录信息
		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchUserId) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"请重新登录"));
			return;
		}

		TbMch mchUser = (TbMch) mchUserDAO.getObjectByProperty(
				TbMch.class, "mchId", fkMchUserId);

		if (null == mchUser) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"用户不存在请重新联系管理员"));
			return;
		}

		// 检查旧密码是否正确
		if (!(DesUtil.encrypt(changePasswdInfo[0]).equals(mchUser
				.getMchPasswd()))) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"旧密码输入错误，修改失败"));
			return;
		}

		// 加密保存
		String passwdEncry = DesUtil.encrypt(changePasswdInfo[1]);
		mchUser.setMchPasswd(passwdEncry);

		// 更新数据库然后更新内存
		mchUserDAO.update(mchUser);
		MchStaffProductCacheManager.getInstance().getMchUserById(mchUser.getMchId())
				.setMchPasswd(passwdEncry);
		// 清除session,返回成功
		request.getSession().removeAttribute(IndexConfig.MCHUSER_SESSION_KEY);
		ResponseToClient.writeJsonMsg(response, new ResultMsgBean(true,
				"修改成功，请重新登录"));
		return;

	}

	/**
	 * 商家账户更新自己个人信息
	 * 
	 * @param request
	 * @param response
	 * @throws RequestInvalidException
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateMchUserInfo(HttpServletRequest request,TbMch mch,
			HttpServletResponse response) throws RequestInvalidException {
		TbMch newMchUserInfo = mch;

		// 从缓存查询原来的 mchUserInfo
		// 更新数据库并更新内存，且 清除session中的用户登录信息
		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchUserId) {

			logger.error("the session does not contain  mchUserinfo .");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"请重新登录"));
			return;
		}

		TbMch mchUserInfo = MchStaffProductCacheManager.getInstance()
				.getMchUserById(fkMchUserId);
		if (null == mchUserInfo) {
			logger.error("the memery does not contain new mchUserinfo .");

			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"更新失败"));
			return;
		}

		// clone一个旧的商家信息，如果数据库更新失败需要还原
		TbMch oldMchUserInfo = mchUserInfo.clone();

		boolean isNeedUpdate = false;

		if (null != newMchUserInfo.getShopName()
				&& newMchUserInfo.getShopName().length() < 50) {
			mchUserInfo.setShopName(newMchUserInfo.getShopName());
			isNeedUpdate = true;
		}

		if (null != newMchUserInfo.getShopAddress()
				&& newMchUserInfo.getShopAddress().length() < 100) {
			mchUserInfo.setShopAddress(newMchUserInfo.getShopAddress());
			isNeedUpdate = true;
		}

		if (null != newMchUserInfo.getPhoneNum()
				&& newMchUserInfo.getPhoneNum().length() < 100) {
			mchUserInfo.setPhoneNum(newMchUserInfo.getPhoneNum());
			isNeedUpdate = true;
		}
		
		if(StringUtils.isNotBlank(newMchUserInfo.getFkOpenId())){
			mchUserInfo.setFkOpenId(newMchUserInfo.getFkOpenId());
			isNeedUpdate = true ;
		}
		
		if(StringUtils.isNotBlank(newMchUserInfo.getExtProps())){
		    Map<String,String> props = JSON.parseObject(mchUserInfo.getExtProps(), Map.class);
		    
		    if(null == props){
		    	props = new HashMap();
		    }
		    
		    String[] times = newMchUserInfo.getExtProps().split("-");
		    props.put("start", times[0]);
		    props.put("end", times[1]);
		    mchUserInfo.setExtProps(JSON.toJSONString(props));
		    isNeedUpdate = true;
		}
		
		if(StringUtils.isNotBlank(newMchUserInfo.getEmail())){
			mchUserInfo.setEmail(newMchUserInfo.getEmail());
			isNeedUpdate = true;
		}
		
		if(StringUtils.isNotBlank(newMchUserInfo.getShopDetail())){
			mchUserInfo.setShopDetail(newMchUserInfo.getShopDetail());
			isNeedUpdate = true;
		}
		

		// 只有存在更新字段且为有效字段时候才更新
		if (isNeedUpdate) {
			try {
				logger.info("update the  mchUserinfo to db.");
				mchUserInfo.setModifyTime(new Date());
				mchUserDAO.update(mchUserInfo);
				logger.info("update the  mchUserinfo success.");
				
				//可能 fkopenId 也有更新，需要替换下内存
				MchStaffProductCacheManager.getInstance().updateMchUserByFkOpenId(oldMchUserInfo.getFkOpenId(), mchUserInfo.getFkOpenId());
				
				//response.sendRedirect("missing"+PageConfigUtil.MCH_PREFIX+PageConfigUtil.MchUserPage.TO_INFO_URI);
			} catch (DataAccessException e) {
				logger.error(e.getMessage());

				ResponseToClient.writeJsonMsg(response, new ResultMsgBean(
						false, "更新失败"));

				// 还原 缓存中的 tbUser信息
				mchUserInfo.setShopName(oldMchUserInfo.getShopName());
				mchUserInfo.setShopAddress(oldMchUserInfo
						.getShopAddress());
				mchUserInfo.setPhoneNum(oldMchUserInfo.getPhoneNum());

				return;
			}
		}


		return;
	}

	/*
	 * ajax验证原密码是否正确
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void validateMchUserPasswd(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			request.setCharacterEncoding("utf-8");
			response.setContentType("text/html; charset=utf-8");
			PrintWriter out = response.getWriter();
			String oldpwd = DesUtil.encrypt(request.getParameter("oldpwd"));

			Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
			TbMch mchUser = (TbMch) mchUserDAO.getObjectByProperty(
					TbMch.class, "mchId", fkMchUserId);
			if (oldpwd.equals(mchUser.getMchPasswd())) {
				out.write("1");
			} else {
				out.write("0");
			}
		} catch (Exception e) {
			logger.error("validateMchUserPasswd has error! the exception is:"
					+ e);
		}
	}

	/**
	 * 商家界面生成自己的一周流水 图表数据
	 * 
	 * @param request
	 * @param model
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<TransactionReportBean> generateOneWeekTransactionRecords(
			HttpServletRequest request, Map<String, Object> model) {

		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchUserId) {
			logger.error("mch user must login first");
			return null;
		}

		HqlQueryCondition query = new HqlQueryCondition();
		query.getProperties().add(
				new Property(Relation.EQ, "fkMchId", fkMchUserId));
		Property pStart = null;
		Property pEnd = null;
		List<TransactionReportBean> reports = new ArrayList<TransactionReportBean>();
		for (int i = 0; i < 7; i++) {
			query.getProperties().remove(pStart);
			query.getProperties().remove(pEnd);
			String day = DateUtil.getDayOverTime(-i);
			pStart = new Property(Relation.LE, "recordTime",
					DateUtil.getDayOverTimeDate(-i + 1));
			query.getProperties().add(pStart);
			pEnd = new Property(Relation.GE, "recordTime",
					DateUtil.getDayOverTimeDate(-i));
			query.getProperties().add(pEnd);

			long countFee = mchUserDAO.sumByQueryCondtion(
					TbTransactionRecord.class, query, "recordMoney");
			logger.info("the day:" + day + ",and the countFee=" + countFee);
			reports.add(new TransactionReportBean(day, countFee));
		}

		return reports;
	}

	/**
	 * 商家账户查询意见 表，支持分页方式
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public List<TbSuggestion> getSuggestions(HttpServletRequest request,
			HttpServletResponse response, Map<String, Object> model)
			throws HappyLifeException {
		if (null == request || null == response) {
			logger.error("request or model can not be null");

			throw new RequestInvalidException(
					"request and response can not be null");
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);
		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		int allPages = (mchUserDAO.getListByProperty(TbSuggestion.class,
				"fkMchId", fkMchUserId).size()
				+ PageConfigUtil.PAGE_COUNT_SIZE_10 - 1)
				/ PageConfigUtil.PAGE_COUNT_SIZE_10;
		model.put("allPages", allPages - 1);
		// session不在重新登录
		if (null == fkMchUserId) {
			throw new SessionInvalidException("fkmchUserid");
		}

		HqlQueryCondition query = new HqlQueryCondition();

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_10);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_10);

		query.getProperties().add(
				new Property(Relation.EQ, "fkMchId", fkMchUserId));
		query.setOrderName("createTime");
		query.setAesc(false);

		try {

			return mchUserDAO.getListByQueryCondtion(TbSuggestion.class, query);
		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			return null;
		}
	}


	//查询商户 下的员工列表
	@Override
	public List<TbMchStaff> getStaffs(HttpServletRequest request,
			HttpServletResponse response, Map<String, Object> model)
			throws HappyLifeException {
		if (null == request || null == response) {
			logger.error("request or model can not be null");
			throw new RequestInvalidException(
					"request and response can not be null");
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);
		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		if(null == fkMchUserId){
			logger.error("the session time out ,have no fkMchUserId");
			throw new HappyLifeException("need login now");
		}
		
		//先从缓存查询，缓存没有就从数据库查
		
		CacheMchUser  mchUser = MchStaffProductCacheManager.getInstance().getCacheMchUserById(fkMchUserId);
		
		List<TbMchStaff> staffs  = null;
		if(null != mchUser){
			staffs = mchUser.getStaffs();
			if(staffs != null && staffs.size() >0){
				model.put("allPages", staffs.size());
				return CacheObjectPageUtil.getObjectByPage(staffs, currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20, PageConfigUtil.PAGE_COUNT_SIZE_20) ;
			}
		}
		
	
		//从数据库读一把
		staffs =  mchUserDAO.getListByProperty(TbMchStaff.class,
				"fkMchId", fkMchUserId);
		
		if(null == staffs || staffs.size() == 0){
			model.put("allPages", 0);
			logger.warn("have null staff, fkMchId ="+fkMchUserId);
			return null ;
		}
		
		int allPages = (staffs.size()
				+ PageConfigUtil.PAGE_COUNT_SIZE_20 - 1)
				/ PageConfigUtil.PAGE_COUNT_SIZE_20;
		model.put("allPages", allPages - 1);
		// session不在重新登录
		if (null == fkMchUserId) {
			throw new SessionInvalidException("fkmchUserid");
		}

		HqlQueryCondition query = new HqlQueryCondition();

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);

		query.getProperties().add(
				new Property(Relation.EQ, "fkMchId", fkMchUserId));
		query.setOrderName("createTime");
		query.setAesc(false);

		try {

			return mchUserDAO.getListByQueryCondtion(TbMchStaff.class, query);
		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			return null;
		}
		
	}
	

	//获取商家的微信关注用户
	@Override
	public List<TbMchWechatRelation> getMchWechatUsers(
			HttpServletRequest request, HttpServletResponse response,
			Map<String, Object> model) throws HappyLifeException {
		if (null == request || null == response) {
			logger.error("request or model can not be null");
			throw new RequestInvalidException(
					"request and response can not be null");
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);
		Integer fkMchUserId = ParseRequest.getMchUserIdFromSession(request);
		if(null == fkMchUserId){
			logger.error("the session time out ,have no fkMchUserId");
			throw new HappyLifeException("need login now");
		}
		

		HqlQueryCondition query = new HqlQueryCondition();
		query.getProperties().add(
				new Property(Relation.EQ, "fkMchId", fkMchUserId));
		
		query.getProperties().add(
				new Property(Relation.EQ, "isDelete", 0));
		
		long allCount = mchUserDAO.getCountByQueryCondtion(TbMchWechatRelation.class, query);
		
		long allPages = (allCount
				+ PageConfigUtil.PAGE_COUNT_SIZE_20 - 1)
				/ PageConfigUtil.PAGE_COUNT_SIZE_20;
		model.put("allPages", allPages - 1);
		

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);


		query.setOrderName("followTime");
		query.setAesc(false);

		try {

			return mchUserDAO.getListByQueryCondtion(TbMchWechatRelation.class, query);
		} catch (DataAccessException e) {
			logger.error("get follow wechatuser error,mchid="+fkMchUserId+e.getMessage());
			return null;
		}
	}

	
	// 新增或者更新一个staff信息
	@SuppressWarnings("unchecked")
	@Override
	public boolean saveOrUpdateStaff(TbMchStaff staff ,HttpServletRequest request,
			HttpServletResponse response, Map<String, Object> model)
			throws HappyLifeException {

		try {
			TbMchStaff newStaff = staff;
			if (null == newStaff) {
				logger.warn("the http request is null");
				return false;
			}

			// 说明是新增，调到新页面
			if (null == newStaff.getId()) {
				logger.warn("to new single staff page");

				newStaff.setPassword(DesUtil.encrypt(newStaff.getPhoneNum()));
				newStaff.setCreateTime(new Date());
				newStaff.setModifyTime(new Date());
				newStaff.setFkMchId(ParseRequest
						.getMchUserIdFromSession(request));
				newStaff.setIsDelete(0);
				Integer staffId = mchUserDAO.persist(newStaff);

				// 是否要加入缓存----TODO
				
				TbMchStaff staffQ = (TbMchStaff)mchUserDAO.getObject(TbMchStaff.class, staffId);
				if(null != staffQ){
					boolean isOk = MchStaffProductCacheManager.getInstance().saveOrUpdateStaff(staffQ,staffQ.getFkOpenId());
				    if(!isOk){
				    	logger.error("update staff to cache error");
				    	return false;
				    }
				}

				logger.warn("add new staff succes,name=" + newStaff.getName());
				return true;
			}
			
			// 说明是编辑已有的员工，先判断这个员工的fkMchId 是否是当前商家的必须有匹配关系
			logger.info("find data and to mch user products page");
			if ((int) ParseRequest.getMchUserIdFromSession(request) != newStaff
					.getFkMchId()) {
				logger.warn("the staff does not exit");
				return false;
			}

			// 反向从数据库查询下然后把值新的值重新填下
			TbMchStaff oldStaff = (TbMchStaff) mchUserDAO.get(TbMchStaff.class,
					newStaff.getId());
			if (null == oldStaff) {
				logger.error("the staff not exit id = " + newStaff.getId());
				return false;
			}
			
			String oldFkOpenId = oldStaff.getFkOpenId();
			
			MchUserCareOfUtil.copyChangedProps(oldStaff, newStaff);
			
			oldStaff.setModifyTime(new Date());
			mchUserDAO.update(oldStaff);
			logger.warn("update ok ,newStaff="+newStaff.toString());
			
			//更新下缓存
			boolean isOk = MchStaffProductCacheManager.getInstance().saveOrUpdateStaff(oldStaff,oldFkOpenId);
		    if(!isOk){
		    	logger.error("update staff to cache error");
		    }
			
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new HappyLifeException(e.getMessage());
		}

	}

	@Override
	public Object getById(Class classT, int id) throws HappyLifeException {
		try {
			// 反向从数据库查询下然后把值新的值重新填下
			@SuppressWarnings("unchecked")
			Object  obj = mchUserDAO.get(classT,id);

			return obj;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new HappyLifeException(e.getMessage());
		}
	}
	
	

	@Override
	public Object getByProperty(Class classT, String key, Object value)
			throws HappyLifeException {
		try {
			// 反向从数据库查询下然后把值新的值重新填下
			@SuppressWarnings("unchecked")
			Object  obj = mchUserDAO.getObjectByProperty(classT, key, value);

			return obj;
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new HappyLifeException(e.getMessage());
		}
	}

	@Override
	public void saveOrUpdateObject(Object obj) throws HappyLifeException {
		try {

			mchUserDAO.saveOrupdate(obj);
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new HappyLifeException(e.getMessage());
		}
		
	}
	
	
	
}
