package happylife.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import happylife.dao.GenericDao;
import happylife.model.*;
import happylife.model.servicemodel.CacheMchUser;
import happylife.model.servicemodel.CacheProduct;
import happylife.model.servicemodel.CacheTransaction;
import happylife.model.servicemodel.HqlQueryCondition;
import happylife.model.servicemodel.TransactionReportBean;
import happylife.model.servicemodel.TransactionStatusEnum;
import happylife.model.servicemodel.HqlQueryCondition.Property;
import happylife.model.servicemodel.HqlQueryCondition.Relation;
import happylife.service.AdminService;
import happylife.service.MchUserService;
import happylife.service.exception.HappyLifeException;
import happylife.service.exception.RequestInvalidException;
import happylife.service.exception.SessionInvalidException;
import happylife.util.DateUtil;
import happylife.util.DesUtil;
import happylife.util.StrUtil;
import happylife.util.cache.MchStaffProductCacheManager;
import happylife.util.config.IndexConfig;
import happylife.util.config.PageConfigUtil;
import happylife.util.config.WinterOrangeSysConf;
import happylife.util.requestandresponse.ParseRequest;
import happylife.util.service.MchUserCareOfUtil;
import jodd.util.StringUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

import com.alibaba.fastjson.JSON;

/**
 * @author orange
 * 
 */
@Service
public class AdminServiceImpl extends GenericServiceImpl<TbAdmin> implements
		AdminService<TbAdmin> {
	private static final Log logger = LogFactory.getLog(AdminServiceImpl.class);

	@SuppressWarnings("rawtypes")
	private GenericDao adminDAO;

	@SuppressWarnings("rawtypes")
	public void setAdminDAO(GenericDao adminDAO) {
		this.adminDAO = adminDAO;
	}

	/**
	 * ???????????????admin ??????????????????????????????????????????????????????
	 * 
	 * @param adminInfo
	 * @return
	 */
	@Override
	public boolean checkLogin(TbAdmin adminInfo) {
		if (null == adminInfo || StringUtil.isBlank(adminInfo.getAdminName())
				|| StringUtil.isBlank(adminInfo.getAdminPasswd())) {
			logger.warn("the admin login error");
			// ResponseToUser.writeJsonMsg(response, new
			// ResultMsgBean(false,"??????????????????"));
			return false;
		}

		@SuppressWarnings("unchecked")
		Object obj = adminDAO.getObjectByProperty(TbAdmin.class, "adminName",
				adminInfo.getAdminName());

		if (null == obj) {
			logger.warn("the admin info not exit" + adminInfo.getAdminName());
			// ResponseToUser.writeJsonMsg(response, new
			// ResultMsgBean(false,"??????????????????"));
			return false;
		}

		TbAdmin adminTmp = (TbAdmin) obj;
		// ???????????????????????????????????????????????????????????????????????????????????????
		if (DesUtil.encrypt(adminInfo.getAdminPasswd()).equals(
				adminTmp.getAdminPasswd())) {
			adminInfo.setAdminId(adminTmp.getAdminId());
			adminInfo.setAdminLevel(adminTmp.getAdminLevel());
			adminInfo.setStatus(adminTmp.getStatus());
			return true;
		}

		// ResponseToUser.writeJsonMsg(response, new
		// ResultMsgBean(false,"????????????"));
		// ????????????
		return false;
	}

	/**
	 * ????????????
	 * 
	 * @param request
	 * @param response
	 */
	public void changePasswd(HttpServletRequest request,
			HttpServletResponse response) {

	}

	/**
	 * ???????????? ????????????????????????
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

		HqlQueryCondition query = new HqlQueryCondition();

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);

		query.setOrderName("createTime");
		query.setAesc(false);

		try {

			return adminDAO.getListByQueryCondtion(TbSuggestion.class, query);
		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			return null;
		}

	}

	/**
	 * ???????????????????????????????????????????????????
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public List<TbActivity> getActivities(HttpServletRequest request,
			HttpServletResponse response, Map<String, Object> model)
			throws HappyLifeException {

		if (null == request || null == response) {
			logger.error("request or model can not be null");

			throw new RequestInvalidException(
					"request and response can not be null");
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);

		HqlQueryCondition query = new HqlQueryCondition();

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);

		query.setOrderName("createTime");
		query.setAesc(false);

		try {

			return adminDAO.getListByQueryCondtion(TbActivity.class, query);
		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * ????????????????????? ???????????????????????????????????????????????? ???????????????????????????????????????????????????
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

			// tmpProductName = manager
			// .getProductById(transaction.getFkProductId()).getProduct()
			// .getProductName();
			tmpCacheTransaction.setProductName("????????????");

			tmpMchShopName = manager.getMchUserById(transaction.getFkMchId())
					.getShopName();

			tmpCacheTransaction.setMchShopName(tmpMchShopName);

			TbMchStaff staff = manager.getStaff(transaction.getFkStaffId(),
					null);
			tmpCacheTransaction.setMchStaff(staff);
			tmpCacheTransaction.setCacheMch(manager
					.getCacheMchUserById(transaction.getFkMchId()));

			TransactionStatusEnum[] statusEnums = TransactionStatusEnum
					.values();
			for (TransactionStatusEnum tran : statusEnums) {
				if (tran.getStatusInt() == transaction.getRecordStatus()) {
					tmpCacheTransaction.setStatusMsg(tran.getStatusMsg());
					break;
				}
			}

			cacheTransactions.add(tmpCacheTransaction);
		}

		return cacheTransactions;
	}

	/**
	 * ?????????????????? ??? ??????????????????????????????????????????????????????????????? ??????
	 * 
	 * @author snnile2012
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<TbTransactionRecord> generateTransactionList(
			HttpServletRequest request, Map<String, Object> model,
			boolean isExportData) {
		if (null == request || null == model) {
			logger.error("request or model can not be null");
			return null;
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);

		HqlQueryCondition query = new HqlQueryCondition();

		// ????????????????????????????????????????????????????????????????????????
		if (!isExportData) {
			query.setFirstResult(currentPage
					* PageConfigUtil.PAGE_COUNT_SIZE_20);
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

		String fkMchId = request.getParameter("fkMchId");
		if (StringUtils.isNotBlank(fkMchId) && StringUtils.isNumeric(fkMchId)) {
			model.put("fkMchId", fkMchId);
			query.getProperties().add(
					new Property(Relation.EQ, "fkMchId", Integer
							.valueOf(fkMchId)));
		}

		long count = adminDAO.getCountByQueryCondtion(
				TbTransactionRecord.class, query);

		model.put("allTransactionPages", count);

		return adminDAO
				.getListByQueryCondtion(TbTransactionRecord.class, query);
	}

	// ??????????????????????????????
	@Override
	public long queryTransactionCnt(HttpServletRequest request,
			Map<String, Object> model, Map<String, Object> queryParams) {
		if (null == request || null == model) {
			logger.error("request or model can not be null");
			return 0;
		}

		HqlQueryCondition query = new HqlQueryCondition();

		if (queryParams != null) {

			Object start = queryParams.get("start");
			if (start != null) {
				query.getProperties().add(
						new Property(Relation.GE, "recordTime", start));
			}

			Object end = queryParams.get("end");
			if (end != null) {
				query.getProperties().add(
						new Property(Relation.LE, "recordTime", end));
			}

		}

		String fkProductId = request.getParameter("fkProductId");
		if (StringUtils.isNotBlank(fkProductId)
				&& StringUtils.isNumeric(fkProductId)) {
			model.put("fkProductId", fkProductId);
			query.getProperties().add(
					new Property(Relation.EQ, "fkProductId", Integer
							.valueOf(fkProductId)));
		}

		String fkMchId = request.getParameter("fkMchId");
		if (StringUtils.isNotBlank(fkMchId) && StringUtils.isNumeric(fkMchId)) {
			model.put("fkMchId", fkMchId);
			query.getProperties().add(
					new Property(Relation.EQ, "fkMchId", Integer
							.valueOf(fkMchId)));
		}

		long count = adminDAO.getCountByQueryCondtion(
				TbTransactionRecord.class, query);

		return count;
	}

	/**
	 * ??????????????????????????????????????? ???????????? : ????????????????????????????????????????????????
	 * 
	 * @param request
	 * @param model
	 * @return ??????????????????????????????????????????
	 */
	@SuppressWarnings("unchecked")
	public List<TransactionReportBean> generateOneWeekTransactionRecords(
			HttpServletRequest request, Map<String, Object> model) {
		HqlQueryCondition query = new HqlQueryCondition();
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

			long countFee = adminDAO.sumByQueryCondtion(
					TbTransactionRecord.class, query, "recordMoney");
			logger.info("the day:" + day + ",and the countFee=" + countFee);
			reports.add(new TransactionReportBean(day, countFee));
		}

		return reports;
	}

	/**
	 * ???????????????????????????????????????
	 * 
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
	public List<TbMch> getMchUsers(HttpServletRequest request,
			HttpServletResponse response, Map<String, Object> model)
			throws HappyLifeException {

		if (null == request || null == response) {
			logger.error("request or model can not be null");

			throw new RequestInvalidException(
					"request and response can not be null");
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);

		HqlQueryCondition query = new HqlQueryCondition();

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);

		query.setOrderName("registerTime");
		query.setAesc(true);

		try {
			return adminDAO.getListByQueryCondtion(TbMch.class, query);
		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * ???????????????????????????
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List queryListByClassType(HttpServletRequest request,
			HttpServletResponse response, Map<String, Object> model,
			Class objClass, String orderName) throws HappyLifeException {

		if (null == request || null == response) {
			logger.error("request or model can not be null");

			throw new RequestInvalidException(
					"request and response can not be null");
		}

		int currentPage = ParseRequest.parseCurrentPage(request, model);

		HqlQueryCondition query = new HqlQueryCondition();

		query.setFirstResult(currentPage * PageConfigUtil.PAGE_COUNT_SIZE_20);
		query.setMaxResults(PageConfigUtil.PAGE_COUNT_SIZE_20);

		query.setOrderName(orderName);
		query.setAesc(true);

		try {
			return adminDAO.getListByQueryCondtion(objClass, query);
		} catch (DataAccessException e) {
			logger.error(e.getMessage());
			return null;
		}

	}

	/**
	 * ????????????????????????????????????
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean saveOrUpdateMchUser(HttpServletRequest request,
			HttpServletResponse response, ModelMap model)
			throws HappyLifeException {
		// ???request????????????????????????
		// ???????????????????????????
		TbAdmin admin = (TbAdmin) request.getSession().getAttribute(
				IndexConfig.ADMIN_SESSION_KEY);

		// ??????????????????
		if (null == admin) {
			logger.error("admin session time out ,to login ");
			try {
				response.sendRedirect(request.getContextPath()
						+ PageConfigUtil.AdminPage.TO_LOGIN_URI);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}

			return false;
		}

		logger.info("begin to generate  mchuserInfo");
		String mchUUid = StrUtil.getUUID();
		try {

			// ??????????????????????????????
			String newFilePrefix = mchUUid;
			TbMch newMch = ParseRequest.generateNewMchInfo(request,
					newFilePrefix);
			if (null == newMch) {
				logger.error("generate new mch info failed");
				return false;
			}

			newMch.setPoints(5);// ????????????5?????????

			Integer fkAdminId = admin.getAdminId();

			Map<String, Object> extPropsMap = new HashMap<String, Object>();
			extPropsMap.put("mchUUid", mchUUid);
			extPropsMap.put("fkAdminId", fkAdminId);

			newMch.setExtProps(JSON.toJSONString(extPropsMap));

			// ??????????????????????????????
			TbMch oldMch = (TbMch) adminDAO.getObjectByProperty(TbMch.class,
					"mchName", newMch.getMchName());
			if (null != oldMch) {
				logger.info("update  old mch info ,oldinfo="
						+ oldMch.getMchId());
				newMch.setAverageTime(oldMch.getAverageTime());
				newMch.setPoints(oldMch.getPoints());
				newMch.setMchId(oldMch.getMchId());
				adminDAO.saveOrupdate(newMch);

				// ????????????????????????
				cleanOldMchInfo(oldMch);
			} else {
				// ???????????????
				Integer mchId = adminDAO.persist(newMch);
				newMch.setMchId(mchId);
				logger.warn("add new mch, mch id=" + newMch.getMchId()
						+ " name=" + newMch.getMchName()
						+ " , belong to admin id=" + admin.getAdminId());
			}

			// ????????????
			MchStaffProductCacheManager.getInstance().addMchUser(newMch);
			return true;

		} catch (Exception e) {
			logger.error(e.getMessage());

			return false;
		}
	}

	// ????????????????????????????????????????????????????????????
	private boolean cleanOldMchInfo(TbMch oldMch) {
		String oldMchPictures = oldMch.getContentPicture();
		if (StringUtils.isBlank(oldMchPictures)) {
			logger.warn("no need to clean old mch,mchId=" + oldMch.getMchId());
			return true;
		}

		String[] picturePaths = oldMchPictures.split(StrUtil.SPLIT_STR);

		String prefixPath = WinterOrangeSysConf.IS_TEST_VALID ? IndexConfig.PROJECT_BASE_PATH_TEST_STRING
				: IndexConfig.PROJECT_BASE_PATH_ALIYUN_STRING;

		for (String picturePath : picturePaths) {

			String tmpPath = prefixPath + picturePath;
			
		    File oldFile = new File(tmpPath);
		    if(oldFile.exists()){
		    	oldFile.delete();
		    }
		    
		}
		
		
		logger.warn("clean old mch finished,mchid="+oldMch.getMchId());

		return true;
	}

}
