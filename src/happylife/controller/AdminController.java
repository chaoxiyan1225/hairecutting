package happylife.controller;

import happylife.model.TbActivity;
import happylife.model.TbAdmin;
import happylife.model.TbMch;
import happylife.model.TbMchStaff;
import happylife.model.TbProduct;
import happylife.model.TbRedeemCode;
import happylife.model.TbSuggestion;
import happylife.model.TbTransactionRecord;
import happylife.model.servicemodel.CacheProduct;
import happylife.model.servicemodel.CacheTransaction;
import happylife.model.servicemodel.TransactionReportBean;
import happylife.service.AdminService;
import happylife.service.MchUserService;
import happylife.service.exception.HappyLifeException;
import happylife.service.exception.RequestInvalidException;
import happylife.service.exception.SessionInvalidException;
import happylife.util.ExcelUtil;
import happylife.util.cache.MchStaffProductCacheManager;
import happylife.util.config.IndexConfig;
import happylife.util.config.PageConfigUtil;
import happylife.util.requestandresponse.MenuSwitchShowManager;
import happylife.util.requestandresponse.ParseRequest;
import happylife.util.requestandresponse.ResponseToClient;
import happylife.util.requestandresponse.WechatMessageUtil;
import happylife.util.requestandresponse.messagebean.ResultMsgBean;
import happylife.util.service.ProductUtil;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSONObject;

/**
 * @author yanchaoxi
 * 
 *         by 2016-5-21
 */
@Controller
@RequestMapping(PageConfigUtil.ADMIN_PREFIX)
public class AdminController {
	private static final Log logger = LogFactory.getLog(AdminController.class);
	@Autowired
	private AdminService<TbAdmin> adminService;
	
	@Autowired
	private MchUserService mchUserService;
	

	/**
	 * ??????????????????
	 * 
	 */
	@RequestMapping(PageConfigUtil.AdminPage.LOGIN_URI)
	public void login(HttpServletRequest request, ModelMap map,
			HttpServletResponse response) {
		TbAdmin admin = ParseRequest.generateAdminLoginInfo(request);
		if (null == admin) {
			logger.error("the admin log in faild.");
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"??????????????????"));
			return;
		}

		// ???????????????????????? ????????? ???????????????
		if (adminService.checkLogin(admin)) {
			logger.info("put admin to session name=" + admin.getAdminName());
			request.getSession().setAttribute(IndexConfig.ADMIN_SESSION_KEY,
					admin);

			request.getSession().setAttribute("name", admin.getAdminName());
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(true,
					"????????????"));
			return;
		}

		// ???????????? ????????????
		logger.warn("login failed ,now redirect to login page");
		ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
				"???????????????"));

	}

	/**
	 * ??????????????????
	 * 
	 * @param request
	 * @param map
	 * @param response
	 * @return
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_LOGIN_URI)
	public ModelAndView toLogin(HttpServletRequest request, ModelMap map,
			HttpServletResponse response) {
		logger.info("admin to login page.");
		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.LOGIN_PAGE);
	}

	/**
	 * ??????
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_LOGOUT_URI)
	public ModelAndView logOut(HttpServletRequest request,
			HttpServletResponse response) {
		request.getSession().removeAttribute(IndexConfig.ADMIN_SESSION_KEY);
		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.LOGIN_PAGE);
	}

	/**
	 * ???????????? : ?????? ajax ??????????????????
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_CHANGE_PASSWD_URI)
	public void changePwd(HttpServletRequest request,
			HttpServletResponse response) {
		logger.info("to change passwd .");
		// ajax???????????????????????????????????????
		adminService.changePasswd(request, response);
	}

	/**
	 * ??????????????????????????????????????? : ?????? ajax ??????????????????
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping(PageConfigUtil.AdminPage.VALIDATE_PASSWD_URI)
	public void validatePwd(HttpServletRequest request,
			HttpServletResponse response) {
		logger.info("to validate passwd .");
		// // ajax???????????????????????????????????????
		// mchUserService.validateMchUserPasswd(request, response);
	}

	/**
	 * ?????? ??????????????? ??? ?????? ??????????????? ??????????????????
	 * 
	 * @author snnile2012
	 * @param request
	 * @param map
	 * @param response
	 * @return
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_REDEEMCODE_URI)
	public ModelAndView toRedeemCodeList(HttpServletRequest request,
			ModelMap map, HttpServletResponse response) {
		List<TbRedeemCode> redeemCodes;
		try {
		     redeemCodes = adminService.queryListByClassType(request, response, map, TbRedeemCode.class,"codeCreateTime");
		} catch (HappyLifeException e) {

			logger.error("internel error:"+e.getMessage());
			
			return  new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
		
	
		map.put("redeemCodes", redeemCodes);
		logger.info("find data and to redeemcode list");

		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.REDEEMCODE_PAGE);
	}

	/**
	 * ??????????????????????????????????????????
	 * 
	 * @author snnile2012
	 * @param request
	 * @param map
	 * @param response
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(PageConfigUtil.AdminPage.TO_PRODUCT_LIST_URI)
	public ModelAndView toProductList(HttpServletRequest request, ModelMap map,
			HttpServletResponse response) {
		List<TbProduct> products;
		try {
			products = adminService.queryListByClassType(request, response, map, TbProduct.class,"createTime");
			map.put("products", products);
			logger.info("find data and to admin productlists page");

			MenuSwitchShowManager.updatePageMenuSwitch(map,
					MenuSwitchShowManager.PRODUCT_SWITCH_ALL, "active");
			return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+
					PageConfigUtil.AdminPage.PRODUCT_LIST_PAGE);
		}catch(Exception e){
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}

	}

	/**
	 * ?????????????????? ??????
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_SINGLEPRODUCT_URI)
	public ModelAndView toSingleProductPage(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		logger.info("reward to single product page");

		CacheProduct cacheProduct = ParseRequest.findCacheProduct(request);
		if (null == cacheProduct) {
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

		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.MchUserPage.SINGLE_PRODUCT_PAGE
				+ "_" + cacheProduct.getProduct().getProductType());

	}

	/**
	 * ????????????????????????????????????
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_ADD_MCHUSERS_URI)
	public ModelAndView toAddMchUserPage(ModelMap map,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		logger.info("reward to add mchuser page");

		MenuSwitchShowManager.updatePageMenuSwitch(map,
				MenuSwitchShowManager.USER_SWITCH, "active");
		// ?????????????????????????????????
		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.ADD_MCHUSER_PAGE);
	}
	
	/**
	 * ???????????????????????????
	 */
	
	@RequestMapping(value=PageConfigUtil.AdminPage.ADD_MCHUSERS_URI,method=RequestMethod.POST)
	public ModelAndView  addNewMch(HttpServletRequest request, ModelMap model,
			HttpServletResponse response) {
		logger.info("now add mchuser page");
		
		MenuSwitchShowManager.updatePageMenuSwitch(model,MenuSwitchShowManager.USER_SWITCH, "active");
		try {
			boolean  isSuccess = adminService.saveOrUpdateMchUser(request, response, model);
			
			if(isSuccess){
				response.sendRedirect(request.getContextPath()+PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.TO_MCHUSERS_URI);
				return null;
				
			}else{
				ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
						"????????????"));
				
				return null;
			}
			
		} catch (HappyLifeException | IOException e) {
			logger.error("add mch error,",e);
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"????????????"));
			
			return null;
		

		}
	}
	
	

	/**
	 * ???????????????????????????
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_MCHUSERS_URI)
	public ModelAndView toMchUsers(ModelMap model, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// ???????????????????????? ????????????????????? ??????
		logger.info("reward to mchuser list  page");
		List<TbMch> mchUsers = null;
		try {
			mchUsers = adminService.getMchUsers(request, response, model);
		} catch (RequestInvalidException e) {
			logger.error("request is invalid");
			return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.LOGIN_PAGE);
		} catch (SessionInvalidException e) {
			logger.error("session invalid ,to relogin");
			return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.LOGIN_PAGE);
		} catch (HappyLifeException e) {
			logger.error("internal error!");
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
		// ????????????
		model.put("mchUsers", mchUsers);

		MenuSwitchShowManager.updatePageMenuSwitch(model,
				MenuSwitchShowManager.USER_SWITCH, "active");

		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.MCHUSER_LIST_PAGE);
	}

	// /**
	// * ???????????? ?????????????????? ?????? ????????????????????????
	// *
	// * @param map
	// * @param request
	// * @param response
	// * @return
	// * @throws IOException
	// */
	// @RequestMapping(PageConfigUtil.MchUserPage.SET_PRODUCT_STATUS_URI)
	// public void editProductStatus(ModelMap map, HttpServletRequest request,
	// HttpServletResponse response) throws IOException {
	//
	// logger.info("begin to change product status");
	// //mchUserService.changeProductStatus(request, response);
	// logger.info("finish to change product status");
	//
	// }

	/**
	 * ??????????????????????????????????????????????????????????????????????????????
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_TRANSACTIONS_URI)
	public ModelAndView toTransactionRecords(ModelMap map,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		logger.info("begin to find transactions");
		 List<CacheTransaction> cacheTransactions = adminService.generateCacheTransactionList(request, map, false);
		
		 // ????????????
		 map.put("cacheTransactions", cacheTransactions);
		
		 // ?????????????????????????????????,???????????????
//		 List<TransactionReportBean> reports = mchUserService
//		 .generateOneWeekTransactionRecords(request, map);
//		 map.put("reports", reports);

		MenuSwitchShowManager.updatePageMenuSwitch(map,
				MenuSwitchShowManager.REPORT_SWITCH, "active");

		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.TRANSACTIONS_LIST_PAGE);
	}

	/**
	 * ??????????????????????????????
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(PageConfigUtil.AdminPage.EXPORT_DATA)
	public void exportData(ModelMap map, HttpServletRequest request,
			HttpServletResponse response) {
		logger.info("begin to export data");
		List<CacheTransaction> cacheTransactions = adminService
				.generateCacheTransactionList(request, map, true);
		if (null == cacheTransactions) {
			logger.error("no records");
			return;
		}

		ExcelUtil.exportTsrExcel(cacheTransactions, response);
	}

	/**
	 * ???????????????
	 * 
	 * @author snnile2012
	 * @param request
	 * @param map
	 * @param response
	 * @return
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_INDEX_URI)
	public ModelAndView toIndex(HttpServletRequest request, ModelMap map,
			HttpServletResponse response) {

		// ??????????????????
		List<TransactionReportBean> reports = adminService
				.generateOneWeekTransactionRecords(request, map);
		map.put("reports", reports);
		
		//???????????????
		map.put("totalMchCnt",MchStaffProductCacheManager.getInstance().getAllMchCount());
		
		//???????????????
		long orderCntToday = 0;
		// ????????????????????????????????????????????????????????????????????????
		Calendar currentDate = new GregorianCalendar();   
		  
		currentDate.set(Calendar.HOUR_OF_DAY, 0);  
		currentDate.set(Calendar.MINUTE, 0);  
		currentDate.set(Calendar.SECOND, 0);  
		Date start = (Date)currentDate.getTime().clone();  

		currentDate.set(Calendar.HOUR_OF_DAY, 23);  
		currentDate.set(Calendar.MINUTE, 59);  
		currentDate.set(Calendar.SECOND, 59);  
		Date end = (Date)currentDate.getTime().clone(); 
		
		Map<String,Object> params = new HashedMap();
		params.put("start", start);
		params.put("end", end);
		
		orderCntToday = adminService.queryTransactionCnt(request, map, params);
		map.put("orderCntToday", orderCntToday);
		
		//???????????????
		long orderCnt = adminService.queryTransactionCnt(request, map, null) ;
		map.put("orderCnt", orderCnt);
		
	    //???????????????

		MenuSwitchShowManager.updatePageMenuSwitch(map,
				MenuSwitchShowManager.INDEX_SWITCH, "active");

		logger.info("now to the Index");
		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.INDEX_PAGE);
	}

	//
	/**
	 * ?????????????????????
	 * 
	 * @author snnile2012
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_ACTIVITY_URI)
	public ModelAndView toActivityList(HttpServletRequest request,
			ModelMap model, HttpServletResponse response) {

		logger.info("to activities page");
		// ????????????
		List<TbActivity> activities = null;
		try {
			activities = adminService.getActivities(request, response, model);
		} catch (RequestInvalidException e) {
			logger.error("request is invalid");
			return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.LOGIN_PAGE);
		} catch (HappyLifeException e) {
			logger.error(e.getMessage());
			new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
		model.put("activities", activities);

		MenuSwitchShowManager.updatePageMenuSwitch(model,
				MenuSwitchShowManager.ACTIVITY_SWITCH, "active");

		logger.info("now to activity page");
		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.ACTIVITY_LIST_PAGE);
	}

	/**
	 * ????????????????????????
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TO_SUGGEST_URI)
	public ModelAndView toSuggestionPage(ModelMap model,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		// ???????????????????????? ????????????????????? ??????
		logger.info("reward to suggest  page");
		List<TbSuggestion> suggests = null;
		try {
			suggests = adminService.getSuggestions(request, response, model);
		} catch (RequestInvalidException e) {
			logger.error("request is invalid");
			return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.LOGIN_PAGE);
		} catch (HappyLifeException e) {
			logger.error("internal error!");
			return new ModelAndView(PageConfigUtil.ERROR_500_PAGE);
		}
		// ????????????
		model.put("suggests", suggests);

		MenuSwitchShowManager.updatePageMenuSwitch(model,
				MenuSwitchShowManager.SUGGEST_SWITCH, "active");

		return new ModelAndView(PageConfigUtil.ADMIN_PREFIX+PageConfigUtil.AdminPage.TO_SUGGEST_PAGE);
	}
	
	
	/**
	 * ?????????????????????????????????????????????????????????
	 * @param tbMchStaff
	 * @param map
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping(PageConfigUtil.AdminPage.TRANSACTION_MANAGER_URI)
	public void updateTransaction(@ModelAttribute TbTransactionRecord transaction,ModelMap map, HttpServletRequest request,
			HttpServletResponse response) throws IOException{
		
		if(null == transaction || transaction.getRecordId() == null || transaction.getFkStaffId() ==0){
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"????????????????????????"));
			return ;
		}
		
		// ???????????????????????????
		Integer fkMchId = ParseRequest.getMchUserIdFromSession(request);
		if (null == fkMchId) {
			ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"????????????????????????????????????"));
			return;
		}
	
		try {
			TbTransactionRecord oldRecord = (TbTransactionRecord)mchUserService.getById(TbTransactionRecord.class,transaction.getRecordId());
		
		    if(null == oldRecord){
		    	logger.error("the record not exit ,id="+transaction.getRecordId());
		    	ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
						"???????????????????????????????????????"));
		    	return;
		    }
		    
		    oldRecord.setFkStaffId(transaction.getFkStaffId());
		    
		    //????????????????????????????????????
		    TbMchStaff  staff = (TbMchStaff)mchUserService.getById(TbMchStaff.class,transaction.getFkStaffId());
		    
		    if(null == staff){
		    	ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
						"????????????????????????????????????"));
		    	return;
		    }
		    
		    String  fkStaffOpenId = staff.getFkOpenId();
		    if(StringUtils.isBlank(fkStaffOpenId)){
		    	logger.error("the staff have no weixin fkOpenId id="+staff.getId());
		    }else{
		    	//???????????????
		    	JSONObject respJson = WechatMessageUtil.sendTransactionToStaff(oldRecord, fkStaffOpenId);
		    	logger.warn(respJson);
		    }
		    
		    oldRecord.setRecordStatus(2); //??????????????????
		    mchUserService.saveOrUpdateObject(oldRecord);
		    
		    
	    	ResponseToClient.writeJsonMsg(response, new ResultMsgBean(true,
					"????????????"));
	    	return;
		    
		
		} catch (HappyLifeException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
			
	    	ResponseToClient.writeJsonMsg(response, new ResultMsgBean(false,
					"????????????????????????"));
	    	return;
		}

	}

}
