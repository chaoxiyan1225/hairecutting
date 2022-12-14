package happylife.controller;

import happylife.model.TbMch;
import happylife.model.TbMchStaff;
import happylife.model.TbTransactionRecord;
import happylife.model.TbWechatUser;
import happylife.model.servicemodel.CacheMchUser;
import happylife.model.servicemodel.CacheMchUserQueueOrder;
import happylife.model.servicemodel.CacheProduct;
import happylife.model.servicemodel.CacheRedeemCode;
import happylife.model.servicemodel.CacheTransaction;
import happylife.model.servicemodel.CacheWechatUser;
import happylife.model.servicemodel.MchServiceStatusEnum;
import happylife.model.servicemodel.ProductSearchCondition;
import happylife.model.servicemodel.TransactionStatusEnum;
import happylife.service.WeChatUserService;
import happylife.service.exception.HappyLifeException;
import happylife.util.StrUtil;
import happylife.util.cache.MchStaffProductCacheManager;
import happylife.util.cache.QueueOrderCacheManager;
import happylife.util.config.IndexConfig;
import happylife.util.config.OrderTypeConfigEnum;
import happylife.util.config.PageConfigUtil;
import happylife.util.config.ProductType;
import happylife.util.config.WinterOrangeSysConf;
import happylife.util.requestandresponse.ParseRequest;
import happylife.util.requestandresponse.WeChatRequestUtil;
import happylife.util.service.ProductUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ???????????????controller??????????????????????????????????????????????????????????????????????????????
 *
 * @author ?????????
 */
@Controller

@RequestMapping(PageConfigUtil.WECHAT_PREFIX)
public class WeChatUserController {
    private static final Log logger = LogFactory.getLog(WeChatUserController.class);
    @SuppressWarnings("rawtypes")
	@Autowired
    private WeChatUserService wechatUserService;
    
    
    /**
     * ??????????????????????????????????????????????????????????????????controller?????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.INTERACTIVE_URI)
    public void sortInteractiveRequst(HttpServletRequest request,
                                      HttpServletResponse response){
        String method = request.getMethod();
        
		try {
			// ?????????????????????signature?????????
			if (method.equalsIgnoreCase("GET")) {
				wechatUserService.checkWeChatAccessSignature(request, response);
			} else {
				// ???????????????????????????????????????????????????
				wechatUserService.processWechatInteractive(request, response);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
       
    }
    
    /**
     * ???????????????????????????????????????
     */
    // TODO: ?????????????????? ??????type ???????????????????????? ?????? forwardSingelProductPageByType
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_FOLLOW_MCHLIST_URI)
    public ModelAndView toFollowMchsPage(Map<String, Object> model,
                                          HttpServletRequest request, HttpServletResponse response){
		try {
			CacheWechatUser cacheChatUser = null ;
			// ??????????????????
			if (!WinterOrangeSysConf.IS_TEST_VALID) {
				// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
			    cacheChatUser = ParseRequest
						.getWechatUserFromSession(request);
				if (null == cacheChatUser
						|| StringUtils.isBlank(cacheChatUser.getWechatUser()
								.getUserOpenid())) {
					logger.error("wechat user session timeout,re auth");
					WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
					return null;
				}
			} else {
				logger.debug("current in test model");
				
				TbWechatUser chatUser = new TbWechatUser();
				
				//chatUser.setUserOpenid("666666");//??????
				chatUser.setUserOpenid("oiTiL6YV7D5UIgG_AlF02T4lECEM");//??????
				chatUser.setExtProps("{\"favoriteMchIds\":[\"1\"]}");
				cacheChatUser = new CacheWechatUser();
				cacheChatUser.setWechatUser(chatUser);
				
			}
			
			
			logger.debug("wechat use to mch list page");
			wechatUserService.generateFollowMchListByPage(request, model, response,
					cacheChatUser);
			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+PageConfigUtil.WechatUserPage.FOLLOW_MCH_LIST_PAGE);
		} catch (Exception e) {
			logger.error("toMchsListPage error happends,"+e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
    }
    
    /**
     * ???????????????????????????????????????
     */
    // TODO: ?????????????????? ??????type ???????????????????????? ?????? forwardSingelProductPageByType
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_MCHLIST_URI)
    public ModelAndView toMchsListPage(Map<String, Object> model,
                                          HttpServletRequest request, HttpServletResponse response){
		try {
			CacheWechatUser cacheChatUser = null ;
			// ??????????????????
			if (!WinterOrangeSysConf.IS_TEST_VALID) {
				// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
			    cacheChatUser = ParseRequest
						.getWechatUserFromSession(request);
				if (null == cacheChatUser
						|| StringUtils.isBlank(cacheChatUser.getWechatUser()
								.getUserOpenid())) {
					logger.error("wechat user session timeout,re auth");
					WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
					return null;
				}
			} else {
				logger.debug("current in test model");
				
				TbWechatUser chatUser = new TbWechatUser();
				
				//chatUser.setUserOpenid("666666");//??????
				chatUser.setUserOpenid("0628st");//??????
				cacheChatUser = new CacheWechatUser();
				cacheChatUser.setWechatUser(chatUser);
				
			}
			
			
	    	//?????????????????????????????? staff  mch ??????admin?  ????????????
	    	TbMchStaff staff = MchStaffProductCacheManager.getInstance().getStaff(null, cacheChatUser.getWechatUser().getUserOpenid());
	    	if(null != staff){
	    		logger.warn("the wechat user is staff ,to  staff transaciton page");
	    		//?????????????????????staff ??????????????????staff??????
	    		return toStaffTransactionList(model,request,response,staff);
	    	}
	    	
			
			//???????????????????????????????????? ??????????????????
			String type = ParseRequest.parseRequestByType("type", true, request);
			if(StringUtils.isBlank(type)){
				type = "product";
			}
			
			model.put("type", type);

			logger.debug("wechat use to mch list page");
			wechatUserService.generateMchListByPage(request, model, response,
					false);
			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+PageConfigUtil.WechatUserPage.MCH_LIST_PAGE);
		} catch (Exception e) {
			logger.error("toMchsListPage error happends,"+e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
    }
    
    
    //ajax????????????????????????????????????????????????????????????????????????????????????????????????
    @RequestMapping(PageConfigUtil.WechatUserPage.SEARCH_MCHS_URI)
    public ModelAndView searchMchsByNameOrAddress(Map<String, Object> model,
            HttpServletRequest request,String name, HttpServletResponse response){
		try {
			// ??????????????????
			if (!WinterOrangeSysConf.IS_TEST_VALID) {
				// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
				CacheWechatUser cacheChatUser = ParseRequest
						.getWechatUserFromSession(request);
				if (null == cacheChatUser
						|| StringUtils.isBlank(cacheChatUser.getWechatUser()
								.getUserOpenid())) {
					logger.error("wechat user session timeout,re auth");
					WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
					return null;
				}
			} else {
				logger.debug("current in test model");
			}

			logger.debug("wechat use to mch list page");
			wechatUserService.generateMchListByPage(request, model, response,
					false);
			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+PageConfigUtil.WechatUserPage.MCH_LIST_PAGE);
			
		} catch (Exception e) {
			logger.error("toMchsListPage error happends,"+e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
    	
    }
    
    //ajax????????????????????????????????????????????????????????????????????????????????????????????????
    @RequestMapping(PageConfigUtil.WechatUserPage.SEARCH_MCHS_ASYNC_URI)
    public void queryMchNameListByNameOrAddress(Map<String, Object> model,
            HttpServletRequest request, HttpServletResponse response){
		try {
			// ??????????????????
			if (!WinterOrangeSysConf.IS_TEST_VALID) {
				// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
				CacheWechatUser cacheChatUser = ParseRequest
						.getWechatUserFromSession(request);
				if (null == cacheChatUser
						|| StringUtils.isBlank(cacheChatUser.getWechatUser()
								.getUserOpenid())) {
					logger.error("wechat user session timeout,re auth");
					WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
					return ;
				}
			} else {
				logger.debug("current in test model");
			}

			logger.debug("wechat use to mch list page");
			wechatUserService.generateMchListByPage(request, model, response,
					false);
			
		} catch (Exception e) {
			logger.error("toMchsListPage error happends,"+e.getMessage());
			return ;
		}
    }
    
    //?????????????????????????????????staff?????????????????????
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_FINISH_TRANSACTION_URI)
	public ModelAndView staffToFinishTransaction(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
			// ??????????????????
			if (!WinterOrangeSysConf.IS_TEST_VALID) {
				// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
				CacheWechatUser cacheChatUser = ParseRequest
						.getWechatUserFromSession(request);
				if (null == cacheChatUser
						|| StringUtils.isBlank(cacheChatUser.getWechatUser()
								.getUserOpenid())) {
					logger.error("wechat user session timeout,re auth");
					WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
					return null;
				}
			} else {
				logger.debug("current in test model");
			}

			String recordId = ParseRequest.parseRequestByType("recordId",
					false, request);

			if (StringUtils.isBlank(recordId)) {
				logger.error("must query one transaction info ,request has no recordId");
				return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
			}

			@SuppressWarnings("unchecked")
			TbTransactionRecord record = (TbTransactionRecord) wechatUserService
					.getById(TbTransactionRecord.class,
							Integer.valueOf(recordId));

			CacheTransaction tmpCacheTransaction = null;

			MchStaffProductCacheManager manager = MchStaffProductCacheManager
					.getInstance();
			tmpCacheTransaction = new CacheTransaction();
			tmpCacheTransaction.setTransaction(record);

			TransactionStatusEnum[] statusEnums = TransactionStatusEnum
					.values();
			for (TransactionStatusEnum tran : statusEnums) {
				if (tran.getStatusInt() == record.getRecordStatus()) {
					tmpCacheTransaction.setStatusMsg(tran.getStatusMsg());
					break;
				}
			}

			String tmpProductName = manager
					.getProductById(record.getFkProductId()).getProduct()
					.getProductName();
			tmpCacheTransaction.setProductName(tmpProductName);

			String tmpMchShopName = manager
					.getProductById(record.getFkProductId()).getMchUser()
					.getShopName();

			tmpCacheTransaction.setMchShopName(tmpMchShopName);

			TbMchStaff staff = manager.getStaff(record.getFkStaffId(), null);

			if (null != staff) {
				tmpCacheTransaction.setMchStaff(staff);
			}
			
			model.put("cacheTransaction", tmpCacheTransaction);

			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX
					+ PageConfigUtil.WechatUserPage.TRANSACTION_STAFF_ONE_PAGE);

		} catch (Exception e) {
			logger.error(e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}

	}  
    
    
    //?????????????????????????????????staff?????????????????????
    @RequestMapping(PageConfigUtil.WechatUserPage.FINISH_TRANSACTION_URI)
    public ModelAndView staffFinishTransaction(Map<String, Object> model,
    		HttpServletRequest request, HttpServletResponse response) {
    	try {
    		// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    		// ??????????????????
    		CacheWechatUser cacheChatUser = null;
    		if (!WinterOrangeSysConf.IS_TEST_VALID) {
    			// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    		    cacheChatUser = ParseRequest
    					.getWechatUserFromSession(request);
    			if (null == cacheChatUser
    					|| StringUtils.isBlank(cacheChatUser.getWechatUser()
    							.getUserOpenid())) {
    				logger.error("wechat user session timeout,re auth");
    				WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    				return null;
    			}
    		} else {
    			logger.debug("current in test model");
    		}
    		
    		String recordId = ParseRequest.parseRequestByType("recordId",
    				false, request);
    		if (StringUtils.isBlank(recordId)) {
    			logger.error("must query one transaction info ,request has no recordId");
    			return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
    		}
    		
    		@SuppressWarnings("unchecked")
    		TbTransactionRecord record = (TbTransactionRecord) wechatUserService
    		.getById(TbTransactionRecord.class,
    				Integer.valueOf(recordId));

    		TbMchStaff staff = MchStaffProductCacheManager.getInstance().getStaff(record.getFkStaffId(), null);
    		if(!WinterOrangeSysConf.IS_TEST_VALID && 
    				(null == staff || !staff.getFkOpenId().equals(cacheChatUser.getWechatUser().getUserOpenid()))){
    			logger.error("the current staff not invalid ,can not update transaction id="+recordId);
    			return new ModelAndView(PageConfigUtil.ERROR_403_PAGE);
    		}
    		
    		
    		boolean isFinish = wechatUserService.staffFinishTransaction(model,record, staff, request, response);
    		if(!isFinish){
        		logger.error("update faild ,try later ");
        		return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
    			
    		}
    		
    		//?????????????????????????????????
    		List<CacheTransaction> staffTransaction = wechatUserService.staffGetTransaction(model,staff , request, response);
        	
        	model.put("staff", staff);
        	model.put("cacheTransactions", staffTransaction);
        	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
                    PageConfigUtil.WechatUserPage.TRANSACTION_LIST_STAFF_PAGE);
    		
    	} catch (Exception e) {
    		logger.error(e.getMessage());
    		return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
    	}
    	
    }   
    
    
    
    
    
    /**
     * ???????????????????????????????????? ???????????????????????????
     */
    // TODO: ?????????????????? ??????type ???????????????????????? ?????? forwardSingelProductPageByType
    @RequestMapping(PageConfigUtil.WECHAT_PREFIX+PageConfigUtil.WechatUserPage.TO_PRODUCTS_OF_ONE_MCH_URI)
    public ModelAndView toProductsOfOneMchPage(Map<String, Object> model,
    		HttpServletRequest request, HttpServletResponse response) {
		try {
			// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
			// ??????????????????
			if (!WinterOrangeSysConf.IS_TEST_VALID) {
				// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
				CacheWechatUser cacheChatUser = ParseRequest
						.getWechatUserFromSession(request);
				if (null == cacheChatUser
						|| StringUtils.isBlank(cacheChatUser.getWechatUser()
								.getUserOpenid())) {
					logger.error("wechat user session timeout,re auth");
					WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
					return null;
				}
			} else {
				logger.debug("current in test model");
			}

			logger.debug("wechat use to mch single page");
			String mchIdStr = ParseRequest.parseRequestByType("mchId", false,
					request);

			if (StringUtils.isBlank(mchIdStr)) {
				logger.error("must query one mchId info ,request has no mchId");
				return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
			}

			CacheMchUser cacheUser = MchStaffProductCacheManager.getInstance()
					.getCacheMchUserById(Integer.valueOf(mchIdStr));

			if (null == cacheUser) {
				logger.error("cache has no user ,mchId=" + mchIdStr);
				return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
			}
			
			model.put("cacheUser", cacheUser);

			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
					PageConfigUtil.WechatUserPage.PRODUCTS_ONE_MCH_PAGE);

		} catch (Exception e) {
            logger.error(e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
    	
    }
    
    /**
     * ???????????????????????????????????? ???????????????????????????
     */
    // TODO: ?????????????????? ??????type ???????????????????????? ?????? forwardSingelProductPageByType
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_SINGLE_MCH_URI)
    public ModelAndView toSingleMchPage(Map<String, Object> model,
    		HttpServletRequest request, HttpServletResponse response) {
    	
		try {
			// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
			// ??????????????????
			CacheWechatUser cacheChatUser = null;
			if (!WinterOrangeSysConf.IS_TEST_VALID) {
				// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
				cacheChatUser = ParseRequest
						.getWechatUserFromSession(request);
				if (null == cacheChatUser
						|| StringUtils.isBlank(cacheChatUser.getWechatUser()
								.getUserOpenid())) {
					logger.error("wechat user session timeout,re auth");
					WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
					return null;
				}
			} else {
				logger.debug("current in test model");
			}
			
			String mchIdStr = ParseRequest.parseRequestByType("mchId", false,
					request);

			logger.info("wechat use to mch single page, the mchIdStr="+mchIdStr);
			if (StringUtils.isBlank(mchIdStr)) {
				logger.error("must query one mchId info ,request has no mchId");
				return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
			}

			CacheMchUser cacheMchUser = MchStaffProductCacheManager.getInstance()
					.getCacheMchUserById(Integer.valueOf(mchIdStr));

			if (null == cacheMchUser) {
				logger.error("cache has no user ,mchId=" + mchIdStr);
				return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
			}

			logger.info("the mchUser="+cacheMchUser.getMchUser().toString());
			
			String startTime = "10:00";
			String endTime = "21:00";
			model.put("isFollow", "false");
			@SuppressWarnings("unchecked")
			Map<String, Object> extProps = JSON.parseObject(cacheMchUser
					.getMchUser().getExtProps(), Map.class);
			if (null != extProps) {
				startTime = (String) extProps.get("start");
				startTime = StringUtils.isBlank(startTime) ? "10:00"
						: startTime;

				endTime = (String) extProps.get("end");
				endTime = StringUtils.isBlank(endTime) ? "21:00" : endTime;
			}
			
			logger.info("the wechat = "+cacheChatUser.getWechatUser().toString());
			
			Map<String, Object> wechatExtProps = JSON.parseObject(cacheChatUser
					.getWechatUser().getExtProps(), Map.class);
			if (null != wechatExtProps) {

				JSONArray mchIds = (JSONArray) wechatExtProps.get("favoriteMchIds");
				logger.info("the JSONArray mchIDS= " + mchIds.toJSONString());
				if (null != mchIds && mchIds.size() > 0) {
					if (mchIds.contains(mchIdStr)) {
						model.put("isFollow", "true");
					}

				}

			}
			
			
			logger.info("get the isFollow="+(String)model.get("isFollow"));

			model.put("start", startTime);
			model.put("end", endTime);

			// ??????????????????????????????
			caculateMchInservice(cacheMchUser, startTime, endTime);

			CacheMchUserQueueOrder mchOrder = QueueOrderCacheManager
					.getInstance().getCacheMchUserQueue(
							Integer.valueOf(mchIdStr));
			int waitingNum = (mchOrder == null) ? 0 : mchOrder
					.getWaitingQueue().size();

			model.put("cacheUser", cacheMchUser);
			model.put("waitingNum", waitingNum);

			return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
					PageConfigUtil.WechatUserPage.MCH_SINGLE_PAGE);

		} catch (Exception e) {
            logger.error("toSingleMchPage error,e="+e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
    }
    
    
    /**
     * ???????????????????????????????????????
     */
    @SuppressWarnings("unchecked")
	// TODO: ?????????????????? ??????type ???????????????????????? ?????? forwardSingelProductPageByType
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_PRODUCT_LIST_URI)
    public ModelAndView toProductListPage(Map<String, Object> model,
    		HttpServletRequest request, HttpServletResponse response){
    	
    	// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    	TbWechatUser chatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
            // TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    		CacheWechatUser cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return null;
    		}
    	}else{
    		logger.debug("current in test model,construct wechat user info ");
        	chatUser = new TbWechatUser();
        	chatUser.setUserOpenid("ycx06st");
    	}
    	
    	//?????????????????????????????? staff  mch ??????admin?  ????????????
    	TbMchStaff staff = MchStaffProductCacheManager.getInstance().getStaff(null, chatUser.getUserOpenid());
    	if(null != staff){
    		logger.warn("the wechat user is staff ,to  staff transaciton page");
    		//?????????????????????staff ??????????????????staff??????
    		return toStaffTransactionList(model,request,response,staff);
    	}
    	
    	//?????????????????????????????? mchUser
    	logger.info("reward to food list page");
    	wechatUserService.generateProductListByPage(request, model, response,
    			false);
    	String type = ParseRequest.parseRequestByType(ProductSearchCondition.ConditionName.TYPE, true, request);
    	if (null == type) {
    		type = ProductType.FOOD;
    	}
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
    			PageConfigUtil.WechatUserPage.PRODUCT_COMMON_PAGE + type + "/list");
    }
    
    
    //?????????????????????????????????????????????????????????????????????????????????
    @SuppressWarnings("unchecked")
	private ModelAndView toStaffTransactionList(Map<String, Object> model,
            HttpServletRequest request, HttpServletResponse response,TbMchStaff staff){
    	
    	//??????staff?????????????????????????????????
    	List<CacheTransaction> staffTransaction = wechatUserService.staffGetTransaction(model,staff , request, response);
    	
    	model.put("staff", staff);
    	model.put("cacheTransactions", staffTransaction);
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
                PageConfigUtil.WechatUserPage.TRANSACTION_LIST_STAFF_PAGE);
    }
    
    /**
     *  ?????????????????????????????????
     * @param model
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_QUEUE_URI)
    public ModelAndView  toWechatQueueOrderPage(ModelMap model,
            HttpServletRequest request, HttpServletResponse response){
        // TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    	//??????????????????
    	CacheWechatUser cacheChatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
            // TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    		cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return null;
    		}
    	}else{
    		logger.debug("current in test model");
    		cacheChatUser = new CacheWechatUser();
        	TbWechatUser wechatUser = new TbWechatUser();
        	wechatUser.setUserOpenid("ycx0628st");
        	cacheChatUser.setWechatUser(wechatUser);
    	}
    	
    	//?????????????????????????????? staff  mch ??????admin? ?????????mch ???????????????????????????????????????
    	TbMch mchUser = MchStaffProductCacheManager.getInstance().getMchUserByFkOpenId(cacheChatUser.getWechatUser().getUserOpenid());
    	if(null != mchUser){
    		logger.warn("the wechat user is mchUser ,to  mchUser queue page");
    		//?????????????????????staff ??????????????????staff??????
    		return toMchQueueOrderList(model,request,response,mchUser);
    	}
    
    	//?????????????????? ??????????????????????????????????????????
    	logger.info("reward to wechat user  self order page");
        return wechatUserService.querySelfQueueOrder(model, request, cacheChatUser);
    	
    }
    
    
    /**
     *  ?????????????????????????????????
     * @param model
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_SELFINFO_URI)
    public ModelAndView  toWechatInfoPage(ModelMap model,
            HttpServletRequest request, HttpServletResponse response){
        // TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    	//??????????????????
    	CacheWechatUser cacheChatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
    		cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return null;
    		}
    	}else{
    		logger.debug("current in test model");
    		cacheChatUser = new CacheWechatUser();
        	TbWechatUser wechatUser = new TbWechatUser();
        	wechatUser.setUserOpenid("oiTiL6YV7D5UIgG_AlF02T4lECEM");
        	wechatUser.setUserNickName("T-bag");
        	wechatUser.setUserLevel(1);
        	wechatUser.setUserRewardPoint(50);
        	cacheChatUser.setWechatUser(wechatUser);
    	}
    	
    	//?????????????????????????????? staff  mch ??????admin? ?????????mch ???????????????????????????????????????
    	TbMch mchUser = MchStaffProductCacheManager.getInstance().getMchUserByFkOpenId(cacheChatUser.getWechatUser().getUserOpenid());
    	if(null != mchUser){
    		
    		model.put("mchStatuss", MchServiceStatusEnum.values());
    		model.put("mchUser", mchUser);
    		logger.warn("the wechat user is mchUser ,to  mchUser info page");
            return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+PageConfigUtil.WechatUserPage.MCH_SELFINFO_PAGE);
    	}
    
    	//???????????????????????????????????????
    	logger.info("reward to wechat user  self info page");
    	model.put("cacheChatUser", cacheChatUser);
    	
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+PageConfigUtil.WechatUserPage.WECHAT_SELFINFO_PAGE);
	}
    
    
    
    /**
     *  ??????????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.ORDER_CHANGE_URI)
    public void  queueStatusChange(ModelMap model,
    		HttpServletRequest request, HttpServletResponse response){
    	// TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    	CacheWechatUser cacheChatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
            // TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
    		cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return ;
    		}
    	}else{
    		logger.debug("current in test model");
    		cacheChatUser = new CacheWechatUser();
        	TbWechatUser wechatUser = new TbWechatUser();
        	wechatUser.setUserOpenid("st0628");
        	cacheChatUser.setWechatUser(wechatUser);
    	}
    	
    	wechatUserService.changeQueueOrder(model, request,response, cacheChatUser);
    }
    
    //???????????????????????????????????????????????????????????????????????????
	@SuppressWarnings("unused")
	private ModelAndView toMchQueueOrderList(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response, TbMch mch) {

		CacheMchUserQueueOrder cacheMchOrder = QueueOrderCacheManager.getInstance()
				.getCacheMchUserQueue(mch.getMchId());

		model.put("runningQueues", null);
		model.put("waitingQueues", null);
		model.put("outingQueues", null);
		
		if (null != cacheMchOrder) {
			model.put("runningQueues", cacheMchOrder.getRunningQueue());
			model.put("waitingQueues", cacheMchOrder.getWaitingQueue());
			model.put("outingQueues", cacheMchOrder.getOutingQueue());
		}

        return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
                PageConfigUtil.WechatUserPage.MCH_QUEUE_PAGE);
	}
	    
    /**
     * ajax ????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_MORE_PRODUCT_URI)
    public void getMoreProductList(Map<String, Object> model,
                                   HttpServletRequest request, HttpServletResponse response) {
        logger.info("get more food list page");

        wechatUserService
                .generateProductListByPage(request, model, response, true);
    }

    /**
     * ???????????????????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_SINGLE_PRODUCT_URI)
    public ModelAndView toSingleProduct(Map<String, Object> model,
                                        HttpServletRequest request, HttpServletResponse response)
            throws IOException {
    
    	CacheWechatUser cacheChatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
    		cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return null;
    		}
    	}else{
    		logger.debug("current in test model");
    		cacheChatUser = new CacheWechatUser();
        	TbWechatUser wechatUser = new TbWechatUser();
        	wechatUser.setUserOpenid("ycx0628st");
        	cacheChatUser.setWechatUser(wechatUser);
    	}
    	

        // TODO : ???????????? ????????? ??????????????????????????????????????? ????????????
        logger.info("reward to single product page");
        CacheProduct cacheProduct = ParseRequest.findCacheProduct(request);
        if (cacheProduct == null) {
            logger.error("the product not found,redirect to 404 ");
            // ???????????????????????? ??????????????? 404 ??????
            return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
        }

        // ????????????
        model.put("cacheProduct", cacheProduct);
        model.put("commonPicturePath", IndexConfig.RELATIVE_PATH_PREFIX);
        model.put("contentPicts", ProductUtil
                .translateContentPathToArr(cacheProduct.getProduct()
                        .getProductContentPicture()));

        // ??????????????????1
        synchronized (cacheProduct) {
            cacheProduct.getProduct().setLikeCounts(
                    cacheProduct.getProduct().getLikeCounts() + 1);
        }

        return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
                PageConfigUtil.WechatUserPage.PRODUCT_COMMON_PAGE
                        + cacheProduct.getProduct().getProductType() + "/single");

    }
    
    /**
     * ???????????????????????????????????????
     *
     * @return
     * @throws IOException
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_BUYPRODUCT_URI)
    public ModelAndView toBuyProductPage(Map<String, Object> model,
                                         HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String productId = request.getParameter("productId");
        logger.debug("the productId=" + productId);
        model.put("productId", productId);
        return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
                PageConfigUtil.WechatUserPage.BUYPRODUCT_PAGE);
    }
    
    
    /**
     * ???????????????????????????????????????
     *
     * @return
     * @throws IOException
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_ABOUTUS_URI)
    public ModelAndView toAboutUs(Map<String, Object> model,
                                         HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return new ModelAndView("aboutus");
    }
    
    
    /**
     * ????????????????????????????????????
     *
     * @return
     * @throws IOException
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_BUYQUEUEORDER_URI)
    public ModelAndView toBuyQueueOrderPage(Map<String, Object> model,
    		HttpServletRequest request, HttpServletResponse response)
    				throws IOException {
    	String mchId = request.getParameter("mchid");
    	logger.debug("the mchId=" + mchId);
    	model.put("mchid", mchId);
    	model.put("types", OrderTypeConfigEnum.values());
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
    			PageConfigUtil.WechatUserPage.BUY_QUEUE_ORDER_PAGE);
    }

    /**
     * ?????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.BUYPRODUCT_URI)
    public ModelAndView buyProduct(ModelMap model, HttpServletRequest request,
                                   HttpServletResponse response) {
        logger.info("now wechat user buy the product ....");
        return wechatUserService.buyProduct(model, request, response);
    }
    
    
    /**
     * ???????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.PAYFOR_QUEUEORDER_URI)
    public ModelAndView buyQueueOrder(ModelMap model, HttpServletRequest request,
    		HttpServletResponse response) {
    	logger.info("now wechat user payfor order ....");
    	return wechatUserService.payforQueueOrder(model, request, response);
    }

    /**
     * ????????????????????????????????? ??????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.WECHAT_PAY_CALL_BACK)
    public void wechatPayCallBack(ModelMap model, HttpServletRequest request,
                                  HttpServletResponse response) {

        logger.info("now process wechat callback  ....");
        wechatUserService.processWechatPayBack(model, request, response);
        logger.info("finish process wechat call back ....");
    }

    /**
     * ??????????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_CONTACTUS_URI)
    public ModelAndView toContactusPage(ModelMap map,
                                        Map<String, Object> model, HttpServletRequest request,
                                        HttpServletResponse response) {
        logger.info("to contact us");
        List<CacheMchUser> mchUsers = MchStaffProductCacheManager.getInstance()
                .getAllMchUsers();
        model.put("mchUsers", mchUsers);

        return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
                PageConfigUtil.WechatUserPage.CONTACTUS_PAGE);
    }
    
    /**
     * ????????????????????????????????????????????????
     */
    @SuppressWarnings("rawtypes")
	@RequestMapping(PageConfigUtil.WechatUserPage.TO_FAVORITE_MCHS_URI)
    public ModelAndView toWechatFavoriteMchs(ModelMap map,
    		Map<String, Object> model, HttpServletRequest request,
    		HttpServletResponse response) {
    	logger.info("to wechat favorite mchs");
    	
    	CacheWechatUser cacheChatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
    		cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return null;
    		}
    	}else{
    		logger.debug("current in test model");
    		cacheChatUser = new CacheWechatUser();
        	TbWechatUser wechatUser = new TbWechatUser();
        	wechatUser.setUserOpenid("st0628");
        	cacheChatUser.setWechatUser(wechatUser);
    	}
    	
    	//???ext_props???????????? ??????
    	String extPropsStr = cacheChatUser.getWechatUser().getExtProps();
    	model.put("mchUsers", null);
    	if(StringUtils.isBlank(extPropsStr)){
    		return new ModelAndView();
    	}
    	
    	Map<String,Object> extPropsMap = JSON.parseObject(extPropsStr, Map.class);
    	
    	List<String> mchIds = (ArrayList)extPropsMap.get("favoriteMchIds");
    	if(null != mchIds && mchIds.size() >0){
    		List<TbMch> mchUsers = new ArrayList<TbMch>();
    		for(String s:mchIds){
    			TbMch temMch = MchStaffProductCacheManager.getInstance().getMchUserById(Integer.valueOf(s));
    			mchUsers.add(temMch);
    		}
    		
        	model.put("mchUsers", mchUsers);
    		
    	}
    
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
    			PageConfigUtil.WechatUserPage.FAVORITE_MCHS_PAGE);
    }
    
    /**
     * ajax ????????????????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.FOLLOW_MCH_URI)
    public void wechatUpdateFollowMch(ModelMap model,
                                   HttpServletRequest request, HttpServletResponse response) {
        logger.info("wechat  follow  mch ");
        wechatUserService.wechatFollowOneMch(request, model, response);
    }
   
    /**
     * ?????????????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_REDEMMCODE_URI)
    public ModelAndView toRedeemCodePage(ModelMap map,
    		Map<String, Object> model, HttpServletRequest request,
    		HttpServletResponse response) throws IOException {
    	logger.info("to redeemcode page ");
    	
    	TbWechatUser user = new TbWechatUser();
    	user.setUserOpenid("ycx0628st");
    	
    	request.getSession().setAttribute(IndexConfig.SESSION_WECHATUSER_KEY, user);
    	
    	List<CacheRedeemCode> redeemCodes = wechatUserService.generateRedeemCodeList(request, model);
    	
    	map.put("cacheRedeemCodes", redeemCodes);
    	
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
    			PageConfigUtil.WechatUserPage.REDEEMCODE_PAGE);
    }

    /**
     * ?????????????????? ??????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.ADD_SUGGEST_URI)
    public void submitSuggestion(ModelMap map, Map<String, Object> model,
                                 HttpServletRequest request, HttpServletResponse response) {
        logger.info("submit suggestion");
        wechatUserService.addNewSuggestion(request, response);
    }
    
    
    /**
     * ?????????????????????????????????
     */
    @RequestMapping(PageConfigUtil.WechatUserPage.TO_WECHT_TRANSACTIONS_URI)
    public ModelAndView toWechatTransactions(ModelMap map, Map<String, Object> model,
    		HttpServletRequest request, HttpServletResponse response) {
    	
    	logger.info("start query  wechat user transactions...");
    	
    	CacheWechatUser cacheChatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
    		cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return null;
    		}
    	}else{
    		logger.debug("current in test model");
    		cacheChatUser = new CacheWechatUser();
        	TbWechatUser wechatUser = new TbWechatUser();
        	wechatUser.setUserOpenid("st0628");
        	cacheChatUser.setWechatUser(wechatUser);
    	}
    	
    	List<CacheTransaction> cacheTransactions = wechatUserService.wechatGetTransactions(model, cacheChatUser.getWechatUser(), request, response);
    	model.put("cacheTransactions", cacheTransactions);
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
                PageConfigUtil.WechatUserPage.TRANSACTION_LIST_WECHAT_PAGE);
    	
    }
    
    
    /**
     * wechat??????????????????????????????
     */
    @SuppressWarnings("unused")
	@RequestMapping(PageConfigUtil.WechatUserPage.TO_WECHT_SINGLE_TRANS_URI)
    public ModelAndView toWechatsingleTrans(ModelMap map, Map<String, Object> model,
    		HttpServletRequest request, HttpServletResponse response) {
    	logger.info("start query  wechat user transactions...");
    	
    	CacheWechatUser cacheChatUser = null ;
    	if(!WinterOrangeSysConf.IS_TEST_VALID){
    		cacheChatUser = ParseRequest.getWechatUserFromSession(request);
    		if (null == cacheChatUser
    				|| StringUtils.isBlank(cacheChatUser.getWechatUser().getUserOpenid())) {
    			logger.error("wechat user session timeout,re auth");
    			WeChatRequestUtil.redirectToAuthorize(response,request.getRequestURI());
    			return null;
    		}
    	}else{
    		logger.debug("current in test model");
    		cacheChatUser = new CacheWechatUser();
    		TbWechatUser wechatUser = new TbWechatUser();
    		wechatUser.setUserOpenid("st0628");
    		cacheChatUser.setWechatUser(wechatUser);
    	}
    	
 		
		String recordId = ParseRequest.parseRequestByType("recordId",
				false, request);
		if (StringUtils.isBlank(recordId)) {
			logger.error("must query one transaction info ,request has no recordId");
			return new ModelAndView(PageConfigUtil.ERROR_404_PAGE);
		}
		
		@SuppressWarnings("unchecked")
		TbTransactionRecord record;
		try {
			record = (TbTransactionRecord) wechatUserService
			.getById(TbTransactionRecord.class,Integer.valueOf(recordId));
		} catch (NumberFormatException e) {
			logger.error("format  error ,e"+e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		} catch (HappyLifeException e) {
			logger.error("get record error ,id="+recordId+","+e.getMessage());
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
		
		if(!WinterOrangeSysConf.IS_TEST_VALID && 
				(null == cacheChatUser || !record.getFkOpenId().equals(cacheChatUser.getWechatUser().getUserOpenid()))){
			logger.error("the current wechat use have no pernision ,can not query transaction id="+recordId);
			return new ModelAndView(PageConfigUtil.ERROR_403_PAGE);
		}

		String tmpProductName = null;
		String tmpMchShopName = null;
		CacheTransaction tmpCacheTransaction = null;
		MchStaffProductCacheManager manager = MchStaffProductCacheManager
				.getInstance();

		tmpCacheTransaction = new CacheTransaction();
		tmpCacheTransaction.setTransaction(record);

		TransactionStatusEnum[] statusEnums = TransactionStatusEnum.values();
		for (TransactionStatusEnum tran : statusEnums) {
			if (tran.getStatusInt() == record.getRecordStatus()) {
				tmpCacheTransaction.setStatusMsg(tran.getStatusMsg());
				break;
			}
		}

		tmpProductName = manager.getProductById(record.getFkProductId())
				.getProduct().getProductName();
		tmpCacheTransaction.setProductName(tmpProductName);

		tmpMchShopName = manager.getProductById(record.getFkProductId())
				.getMchUser().getShopName();

		tmpCacheTransaction.setMchShopName(tmpMchShopName);
	
		String extPropsStr = record.getExtProps();
		Map<String, Object> extPropsMap = null ;
		if(StringUtils.isNotBlank(extPropsStr)){
			extPropsMap = JSON.parseObject(extPropsStr, Map.class);
		}
		extPropsMap = (extPropsMap == null)? new HashMap():extPropsMap;
		tmpCacheTransaction.setProcessMsg((String)extPropsMap.get("detail"));
		String contentPictures = (String)extPropsMap.get("contentPicture");
		String[] pictures = null ;
		if(StringUtils.isNotBlank(contentPictures)){
		   pictures = contentPictures.replace("\\", "/").split(StrUtil.SPLIT_STR);
		}
		
		tmpCacheTransaction.setContentPictures(pictures);
		model.put("cacheTransaction", tmpCacheTransaction);
    	return new ModelAndView(PageConfigUtil.WECHAT_PREFIX+
    			PageConfigUtil.WechatUserPage.TRANSACTION_WECHAT_ONE_PAGE);
    	
    }
    
    //?????????????????????????????????????????????
    private void caculateMchInservice(CacheMchUser cacheUser,String startTime,String endTime){
    	
    	//???????????????????????????????????????????????????serviceStatus?????????????????????????????? ?????????????????? ???????????? ????????????????????????????????? ???????????????
    	if(!cacheUser.getMchUser().getServiceStatus().equalsIgnoreCase(MchServiceStatusEnum.IN_SERVICE.getStatusMsg())){
    		cacheUser.setInService(false);
    		return ;
    	}
    	
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");  
		String currentTime = formatter.format(new Date()); 
		
		if(currentTime.compareTo(startTime)>=0 && currentTime.compareTo(endTime)<=0)
		{
			cacheUser.setInService(true);
		}else{
			cacheUser.setInService(false);
		}
    }
    

}