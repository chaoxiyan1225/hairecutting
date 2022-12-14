package happylife.util.cache;

import happylife.dao.GenericDao;
import happylife.model.TbActivity;
import happylife.model.TbMch;
import happylife.model.TbMchStaff;
import happylife.model.TbProduct;
import happylife.model.TbProductActivityRecord;
import happylife.model.TbQueueRecord;
import happylife.model.TbTransactionRecord;
import happylife.model.servicemodel.CacheMchUser;
import happylife.model.servicemodel.CacheProduct;
import happylife.model.servicemodel.CacheProductActivity;
import happylife.model.servicemodel.HqlQueryCondition;
import happylife.model.servicemodel.HqlQueryCondition.Property;
import happylife.model.servicemodel.HqlQueryCondition.Relation;
import happylife.model.servicemodel.MchSearchCondition;
import happylife.util.SpringContextUtil;
import happylife.util.StrUtil;
import happylife.util.config.PageConfigUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * 商家以及 商品的 缓存管理类 ：单例模式，默认会把所有的商家以及商品加载到内存，后续如果商家过多则 限制缓存大小
 * //
 * @author 闫朝喜 by 2016-06-05
 */

@Component
public class MchStaffProductCacheManager {

	private static final Log logger = LogFactory
			.getLog(MchStaffProductCacheManager.class);
	/**
	 * 缓存 商品
	 */
	private ConcurrentHashMap<Integer, CacheProduct> productsMap;

	/**
	 * 商户信息
	 */
	private ConcurrentHashMap<Integer, CacheMchUser> mchsMap;

	/**
	 * 商户信息:通过fkOpenId关联查询
	 */
	private ConcurrentHashMap<String, CacheMchUser> wechatMchsMap;

	/**
	 * 商户信息,用于微信前端登陆验证用
	 */
	private ConcurrentHashMap<String, TbMchStaff> wechatMchStaffsMap;

	/**
	 * 商户信息,用于微信前端登陆验证用
	 */
	private ConcurrentHashMap<Integer, TbMchStaff> mchStaffsMap;

	/**
	 * 所有活动表
	 */
	private ConcurrentHashMap<Integer, TbActivity> activitiesMap;

	/**
	 * 商品跟活动的映射表
	 */
	private ConcurrentHashMap<Integer, List<CacheProductActivity>> productActivityMap;

	/**
	 * Map方式缓存所有的商品信息
	 */
	private static ArrayList<CacheProduct> allProducts;

	private static boolean isFinished = false;

	/**
	 * 管理 超级管理员以及商家的 管理界面的统计数据
	 */
	private ConcurrentHashMap<Integer, Map<String, Integer>> staticDataMap;

	@SuppressWarnings("rawtypes")
	
	//@Autowired
	private GenericDao genericDao = null;
	
	/*
	 * 只提供单例方式
	 */
	private static final MchStaffProductCacheManager instance = new MchStaffProductCacheManager();
	
	private ApplicationContext context = null ;
	private BeanFactory factory = null;
	
	private  ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 200, TimeUnit.MINUTES,
             new ArrayBlockingQueue<Runnable>(5));
      

	 /* 私有化构造器
	 */
	private MchStaffProductCacheManager() {
		logger.warn("cache init starting....");
		initCache();
	}

		/**
		 * 初始化
		 * 
		 * @return
		 */
		@SuppressWarnings("rawtypes")
		private boolean initAppContext() {
			context = new ClassPathXmlApplicationContext(
					"resource/applicationContext.xml");
			factory = context;
			genericDao = (GenericDao) factory.getBean("genericDao");
			logger.warn("happylife init app context");
			return genericDao == null?false:true;
		}
		
	

	/**
	 * 初始化缓存数据 ,私有化 仅仅处理一次
	 */
	private void initCache() {
		if (!initAppContext()) {
			logger.error("can not init app context ,now exit");
			return;
		}
		productsMap = new ConcurrentHashMap<Integer, CacheProduct>();
		mchsMap = new ConcurrentHashMap<Integer, CacheMchUser>();
		activitiesMap = new ConcurrentHashMap<Integer, TbActivity>();
		productActivityMap = new ConcurrentHashMap<Integer, List<CacheProductActivity>>();
		allProducts = new ArrayList<CacheProduct>();
		staticDataMap = new ConcurrentHashMap<Integer, Map<String, Integer>>();
		mchStaffsMap = new ConcurrentHashMap<Integer, TbMchStaff>();
		wechatMchStaffsMap = new ConcurrentHashMap<String, TbMchStaff>();
		wechatMchsMap = new ConcurrentHashMap<String, CacheMchUser>();

		// 初始化 超级管理的统计缓存信息
		initAdminStaticCache();

		// 查询所有的商家

		List<TbMch> mchs =genericDao.findAll(TbMch.class);

		logger.warn("got all the TbMchUser.");
		CacheMchUser cacheMch = null;

		// 刷新下商家总数
		staticDataMap.get(CacheIndex.Admin.CACHE_INDEX).put(
				CacheIndex.Admin.MCHUSER_COUNTS, mchs.size());

		int totalSallCounts = 0;
		int totalSallMoney = 0;
		int totalVisitCounts = 0;
		int totalFansCount = 0;

		List<TbQueueRecord> orderRecords = null;

		for (TbMch m : mchs) {

			if (m.getMchStatus() == 0) {
				continue;
			}

			// step 1 加载商品信息
			List<TbProduct> products = initProducts(m);

			cacheMch = new CacheMchUser();
			cacheMch.setMchUser(m);
			cacheMch.setProducts(products);

			String[] contentPictures = m.getContentPicture().replace("\\", "/")
					.split(StrUtil.SPLIT_STR);
			cacheMch.setContentPictures(contentPictures);

			// step 2 加载每个商家下的员工信息
			initStaffs(cacheMch, m);

			mchsMap.put(m.getMchId(), cacheMch);
			wechatMchsMap.put(m.getFkOpenId(), cacheMch);

			logger.info("put the cacheMch: mchusername=" + m.getMchName());

			// 叠加每个商家的 销售次数以及销售金额
			totalSallCounts += m.getTotalSaleCount();
			totalSallMoney += m.getTotalMoney();
			totalVisitCounts += m.getTotalVisitor();
			totalFansCount += m.getTotalFans();

			// step 3:商家排队信息加载

			HqlQueryCondition query = new HqlQueryCondition();

			query.getProperties().add(
					new Property(Relation.EQ, "fkMchId", m.getMchId()));
			query.setOrderName("recordId");
			query.setAesc(true);

			orderRecords =genericDao.getListByQueryCondtion(TbQueueRecord.class,
					query);
			QueueOrderCacheManager.getInstance().cacheManageUpdateMchOrder(
					m.getMchId(), orderRecords);

			// 初始化商家的统计内存
			initMchStaticCache(m);

		}

		// 刷新总的销售次数以及销售金额
		staticDataMap.get(CacheIndex.Admin.CACHE_INDEX).put(
				CacheIndex.Admin.TOTAL_SALL_COUNTS, totalSallCounts);
		staticDataMap.get(CacheIndex.Admin.CACHE_INDEX).put(
				CacheIndex.Admin.TOTAL_SALL_MONEY, totalSallMoney);
		staticDataMap.get(CacheIndex.Admin.CACHE_INDEX).put(CacheIndex.Admin.TOTAL_VISIT_COUNT, totalVisitCounts);
		staticDataMap.get(CacheIndex.Admin.CACHE_INDEX).put(CacheIndex.Admin.TOTAL_FANS_COUNT, totalFansCount);

		// 查询所有的活动
		List<TbActivity> activities =genericDao.findAll(TbActivity.class);

		logger.warn("got all the activities.");
		if (null != activities && activities.size() != 0) {
			for (TbActivity t : activities) {
				activitiesMap.put(t.getId(), t);
			}

		} else {
			logger.warn("now no activity !");
		}

		// 查询所有的商品加入 活动的信息
		initActivities();

		// 完成初始化
		isFinished = true;

		// 商家排队信息完成
		QueueOrderCacheManager.getInstance().setInitFinish();

		logger.warn("cache init finished.");

	}

	@SuppressWarnings("unchecked")
	// 加载商品信息
	private List<TbProduct> initProducts(TbMch m) {
		CacheProduct cacheProduct = null;
		logger.info("got all the products by fkMchId=" + m.getMchId());
		List<TbProduct> products =genericDao.getListByProperty(TbProduct.class,

				"fkMchId", m.getMchId());

		if (null != products && products.size() > 0) {
			// 把查询到的 商品全部放到Map中
			for (TbProduct p : products) {
				if (p.getProductStatus() == 0) {
					continue;
				}
				cacheProduct = new CacheProduct();
				cacheProduct.setMchUser(m);
				cacheProduct.setProduct(p);
				logger.info("put the product: name=" + p.getProductName()
						+ ",id=" + p.getProductId());
				productsMap.put(p.getProductId(), cacheProduct);
				allProducts.add(cacheProduct);
			}
		}

		return products;
	}

	// 加载商家下的员工信息
	@SuppressWarnings("unchecked")
	private void initStaffs(CacheMchUser cacheMch, TbMch m) {
		List<TbMchStaff> staffs =genericDao.getListByProperty(TbMchStaff.class,

				"fkMchId", m.getMchId());

		if (null != staffs && staffs.size() > 0) {
			cacheMch.setStaffs(staffs);
			for (TbMchStaff staff : staffs) {
				if (staff.getIsDelete() == 1) {
					continue;
				}

				String fkOpenId = staff.getFkOpenId();
				if (StringUtils.isNotBlank(fkOpenId)) {
					wechatMchStaffsMap.put(fkOpenId, staff);
				}
				mchStaffsMap.put(staff.getId(), staff);
			}

		}
	}

	// 加载活动信息
	@SuppressWarnings("unchecked")
	private void initActivities() {
		List<TbProductActivityRecord> activityRecords =genericDao
				.findAll(TbProductActivityRecord.class);
		if (null != activityRecords && activityRecords.size() != 0) {
			CacheProductActivity cacheProductActivity = null;
			List<CacheProductActivity> productActivities = null;
			for (TbProductActivityRecord record : activityRecords) {
				TbActivity activity = activitiesMap.get(record
						.getFkActivityId());
				cacheProductActivity = new CacheProductActivity(activity,
						record);
				productActivities = new ArrayList<CacheProductActivity>();
				productActivities.add(cacheProductActivity);

				productActivityMap.put(record.getFkProductId(),
						productActivities);
			}

		} else {
			logger.warn("now no product add  activity !");
		}

	}

	/**
	 * 初始化超级管理的统计缓存信息
	 */
	private void initAdminStaticCache() {
		Map<String, Integer> adminStaticCache = new HashMap<String, Integer>();

		adminStaticCache.put(CacheIndex.Admin.TOTAL_VISIT_COUNT, 0);
		adminStaticCache.put(CacheIndex.Admin.TODAY_VISIT_COUNT, 0);
		adminStaticCache.put(CacheIndex.Admin.MCHUSER_COUNTS, 0);
		adminStaticCache.put(CacheIndex.Admin.TOTAL_SALL_COUNTS, 0);
		adminStaticCache.put(CacheIndex.Admin.TOTAL_SALL_MONEY, 0);

		staticDataMap.put(CacheIndex.Admin.CACHE_INDEX, adminStaticCache);
	}

	/**
	 * 初始化 商家的统计内存
	 * 
	 * @param mchUser
	 */
	private void initMchStaticCache(TbMch mchUser) {
		Map<String, Integer> mchStaticCache = new HashMap<String, Integer>();

		mchStaticCache.put(CacheIndex.MchUser.TOTAL_VISIT_COUNT, mchUser.getTotalVisitor());
		mchStaticCache.put(CacheIndex.MchUser.TODAY_VISIT_COUNT, 0);
		mchStaticCache.put(CacheIndex.MchUser.TOTAL_SALL_COUNTS, mchUser.getTotalSaleCount());
		mchStaticCache.put(CacheIndex.MchUser.TOTAL_SALL_MONEY, mchUser.getTotalMoney());
		mchStaticCache.put(CacheIndex.MchUser.TOTAL_FANS_COUNTS, mchUser.getTotalFans());

		staticDataMap.put(mchUser.getMchId(), mchStaticCache);
	}

	/**
	 * 单例方式获取 实例
	 * 
	 * @return
	 */
	public static MchStaffProductCacheManager getInstance() {
		return instance;
	}
	
	
	/**
	 * 单例方式获取 实例
	 * 
	 * @return
	 */
	public boolean refreshGenoricDAO() {

		try {
			genericDao = (GenericDao) factory.getBean("genericDao");
			logger.warn("happylife refresh genericDao ");
			if (null != genericDao) {
				return true;
			}
			logger.error("happylife refresh  genericDao false ");
			return false;
		} catch (Exception e) {

			logger.error("happylife refresh genericDAO error:" + e.getMessage());
			return false;
		}

	}

	/**
	 * 获取所有的商家列表
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<CacheMchUser> getAllMchUsers() {
		Iterator<Entry<Integer, CacheMchUser>> it = mchsMap.entrySet()
				.iterator();
		;
		List<CacheMchUser> mchUsers = new ArrayList<CacheMchUser>();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			CacheMchUser value = (CacheMchUser) entry.getValue();
			mchUsers.add(value);

		}

		return mchUsers;
	}

	// 查看当前多少注释的商家数量
	public int getAllMchCount() {
		return mchsMap.size();
	}

	/**
	 * getProductById:(这里用一句话描述这个方法的作用). <br/>
	 * 
	 * @author snnile2012
	 * @param id
	 * @return
	 */
	public CacheProduct getProductById(Integer id) {

		if (productsMap.containsKey(id)) {
			return productsMap.get(id);
		}

		logger.debug("the product id=" + id + " not exited");
		return null;

	}

	/**
	 * 根据商家ID 从缓存查找对应的商家： 目前会把所有的商家均填加到缓存中 所以找不到则返回界面查找失败
	 * 
	 * @param id
	 * @return
	 */
	public TbMch getMchUserById(Integer id) {
		if (mchsMap.containsKey(id)) {
			return mchsMap.get(id).getMchUser();
		}

		logger.debug("the mchuser id=" + id + " not exited");
		return null;
	}

	/**
	 * 根据fkOpenId 查询商户信息
	 * 
	 * @param fkOpenId
	 * @return
	 */
	public TbMch getMchUserByFkOpenId(String fkOpenId) {
		if (wechatMchsMap.containsKey(fkOpenId)) {

			return wechatMchsMap.get(fkOpenId).getMchUser();
		}

		logger.info("the mchuser fkOpenId=" + fkOpenId + " not exited");
		return null;
	}

	/**
	 * 根据fkOpenId 查询商户信息
	 * 
	 * @param fkOpenId
	 * @return
	 */
	public void updateMchUserByFkOpenId(String fkOpenIdOld, String fkOpenIdNew) {
		if (wechatMchsMap.containsKey(fkOpenIdOld)) {
			CacheMchUser mchUser = wechatMchsMap.get(fkOpenIdOld);
			wechatMchsMap.remove(fkOpenIdOld);
			wechatMchsMap.put(fkOpenIdNew, mchUser);

			logger.info("update mchuser,mchuser info="
					+ mchUser.getMchUser().toString());
			return;
		}

		logger.warn("the mchuser fkOpenId=" + fkOpenIdOld + " not exited");
		return;
	}

	/**
	 * 根据商家ID 从缓存查找对应的商家： 目前会把所有的商家均填加到缓存中 所以找不到则返回界面查找失败
	 * 
	 * @param id
	 * @return
	 */
	public CacheMchUser getCacheMchUserById(Integer id) {
		if (mchsMap.containsKey(id)) {
			return mchsMap.get(id);
		}

		logger.debug("the mchuser id=" + id + " not exited");
		return null;
	}

	/**
	 * 根据商家id 查询所有的产品
	 * 
	 * @author snnile2012
	 * @param mchId
	 * @return
	 * @since JDK 1.7
	 */
	public List<TbProduct> getProductsByMchId(Integer mchId) {
		if (mchsMap.containsKey(mchId)) {
			return mchsMap.get(mchId).getProducts();
		}

		logger.debug("the mchuser mchId=" + mchId + " has no products");
		return null;

	}

	/**
	 * 根据活动Id 查询 活动
	 * 
	 * @author snnile2012
	 * @param
	 * @return
	 * @since JDK 1.7
	 */
	public TbActivity getActivityById(Integer id) {
		if (activitiesMap.containsKey(id)) {
			return activitiesMap.get(id);
		}

		logger.debug("the activity Id=" + id + " has no record");
		return null;

	}

	/**
	 * 根据商品Id 查询 其参与的所有活动
	 */
	public List<CacheProductActivity> getActivitiesByProductId(Integer id) {
		if (productActivityMap.containsKey(id)) {
			return productActivityMap.get(id);
		}

		logger.debug("the product Id=" + id + " has not add to any activity");
		return null;

	}

	/**
	 * 添加 商家信息到缓存，试用与新添加一个商户信息的时候
	 * 
	 * @author snnile2012
	 * @param :一定是从数据库查询回来的完整信息
	 */
	@SuppressWarnings("unchecked")
	public void addMchUser(TbMch mchUser) {
		if (null == mchUser) {
			logger.warn("can not cache null mchuser!");
			return;
		}

		if (mchsMap.containsKey(mchUser.getMchId())) {
			logger.warn("can not cache exited mchuser :id="
					+ mchUser.getMchId());
			return;
		}

		CacheMchUser cacheMch = new CacheMchUser();
		cacheMch.setMchUser(mchUser);

		List<TbProduct> products =genericDao.getListByProperty(TbProduct.class,

				"fkMchId", mchUser.getMchId());
		if (null != products && products.size() > 0) {
			cacheMch.setProducts(products);
		}else{
		    cacheMch.setProducts(new ArrayList());
		}

		cacheMch.setContentPictures(mchUser.getContentPicture()
				.replace("\\", "/").split(StrUtil.SPLIT_STR));

		mchsMap.put(mchUser.getMchId(), cacheMch);
		wechatMchsMap.put(mchUser.getFkOpenId(), cacheMch);
		logger.warn("cache mchuser finished: id=" + mchUser.getMchId());
	}

	/**
	 * 删除一个 staff
	 * 
	 * @param fkOpenId
	 * @return
	 */
	public boolean deleteStaff(String fkOpenId) {
		if (StringUtils.isBlank(fkOpenId)) {
			logger.warn("the staff fkOpenId not exit, openId=" + fkOpenId);
			return true;
		}

		TbMchStaff oldStaff = null;
		if (wechatMchStaffsMap.get(fkOpenId) != null) {
			oldStaff = wechatMchStaffsMap.remove(fkOpenId);
			mchStaffsMap.remove(oldStaff.getId());
		}

		// 需要把商户缓存中的staff 也删除
		if (null != oldStaff) {
			List<TbMchStaff> staffs = mchsMap.get(oldStaff.getFkMchId())
					.getStaffs();
			TbMchStaff oldOne = null;
			for (TbMchStaff staff : staffs) {
				if (staff.getFkOpenId().equals(fkOpenId)) {
					oldOne = staff;
					break;
				}
			}

			if (null != oldOne) {
				staffs.remove(oldOne);
			}

		}

		return true;
	}

	/**
	 * 删除一个 staff
	 * 
	 * @param fkOpenId
	 * @return
	 */
	public boolean deleteStaffById(Integer id) {
		if (null == id) {
			logger.warn("the staff id not exit, id=" + id);
			return true;
		}

		TbMchStaff oldStaff = null;
		if (mchStaffsMap.get(id) != null) {
			oldStaff = mchStaffsMap.remove(id);
			wechatMchStaffsMap.remove(oldStaff.getFkOpenId());
		}

		// 需要把商户缓存中的staff 也删除
		if (null != oldStaff) {
			List<TbMchStaff> staffs = mchsMap.get(oldStaff.getFkMchId())
					.getStaffs();
			TbMchStaff oldOne = null;
			for (TbMchStaff staff : staffs) {
				if (staff.getId() == id) {
					oldOne = staff;
					break;
				}
			}

			if (null != oldOne) {
				staffs.remove(oldOne);
			}

		}

		return true;
	}

	/**
	 * 查询一个 staff:
	 * 
	 * @param fkOpenId
	 *            或者id都可以
	 * @return
	 */
	public TbMchStaff getStaff(Integer id, String fkOpenId) {
		if (null == id && StringUtils.isBlank(fkOpenId)) {
			logger.warn("the staff id not exit, id=" + id);
			return null;
		}

		TbMchStaff oldStaff = null;
		if (null != id) {
			oldStaff = mchStaffsMap.get(id);
		} else {
			oldStaff = wechatMchStaffsMap.get(fkOpenId);
		}

		return oldStaff;
	}

	/**
	 * 更新员工信息
	 * 
	 * @param fkOpenId
	 * @param newStaff
	 * @return
	 */
	public boolean saveOrUpdateStaff(TbMchStaff newStaff, String oldFkOpenId) {

		if (newStaff == null || null == newStaff.getFkOpenId()
				|| null == newStaff.getId()) {
			logger.error("staff param  error !");
			return false;
		}

		wechatMchStaffsMap.remove(oldFkOpenId);
		wechatMchStaffsMap.put(newStaff.getFkOpenId(), newStaff);
		mchStaffsMap.put(newStaff.getId(), newStaff);

		List<TbMchStaff> staffs = mchsMap.get(newStaff.getFkMchId())
				.getStaffs();

		TbMchStaff oldOne = null;
		for (TbMchStaff staff : staffs) {
			if ((int) staff.getId() == (int) newStaff.getId()) {
				oldOne = staff;
				break;
			}
		}

		if (null != oldOne) {
			staffs.remove(oldOne);
		}

		staffs.add(newStaff);
		return true;
	}

	/**
	 * deleteProduct: 入参 p 一定是从数据库查询的完整记录值
	 * 
	 * @author snnile2012
	 * @param p
	 * @since JDK 1.7
	 */
	public void deleteProduct(TbProduct p) {
		if (p == null) {
			logger.warn("can not delete null product!");
			return;
		}

		if (productsMap.containsKey(p.getProductId())) {
			logger.debug("remove product from productsMap.");
			productsMap.remove(p.getProductId());
			logger.debug("remove product from mchuser's products.");
			mchsMap.get(p.getFkMchId()).getProducts().remove(p);
			logger.debug("remove product from allProducts.");
			allProducts.remove(p);
		}

		logger.warn("remove product finished.");
	}

	/**
	 * 添加 商品到缓存
	 * 
	 * @param product
	 */
	public void addProduct(TbProduct product) {
		if (null == product) {
			logger.warn("can not cache null mchuser!");
			return;
		}

		if (productsMap.containsKey(product.getProductId())) {
			logger.warn("can not cache exited product :id="
					+ product.getProductId());
			return;
		}

		// 更新商户 中的 商品列表
		CacheMchUser cacheMchUser = mchsMap.get(product.getFkMchId());
		if (null != cacheMchUser) {
			cacheMchUser.getProducts().add(product);
		}

		// 添加到 所有的商品列表中
		CacheProduct cacheProduct = new CacheProduct();
		cacheProduct.setMchUser(cacheMchUser.getMchUser());
		cacheProduct.setProduct(product);
		allProducts.add(cacheProduct);

		// 添加到 商品 map中
		productsMap.put(product.getProductId(), cacheProduct);
	}

	/**
	 * 查询指定的 页数的 mchUser 信息
	 * 
	 * @param pageNum
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<CacheMchUser> findMchUserByPage(int pageNum) {
		logger.debug("the pageNum = " + pageNum);
		if (pageNum < 0) {
			pageNum = 0;
		}

		int start = pageNum * PageConfigUtil.PAGE_COUNT_SIZE_10;
		int end = start + PageConfigUtil.PAGE_COUNT_SIZE_10;

		logger.debug("the pageNum = " + pageNum + ",start=" + start + ",end="
				+ end);
		Iterator<Entry<Integer, CacheMchUser>> it = mchsMap.entrySet()
				.iterator();
		int current = 0;
		List<CacheMchUser> mchUsers = new ArrayList<CacheMchUser>();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			if (current >= start && current < end) {
				mchUsers.add((CacheMchUser) entry.getValue());
				logger.debug("find the mchUser mchUserId="
						+ ((CacheMchUser) entry.getValue()).getMchUser()
								.getMchId());
			}

			current++;
		}

		logger.debug("find the total mchUsers's size=" + mchUsers.size());
		return mchUsers.size() == 0 ? null : mchUsers;

	}

	/**
	 * 查询指定的 页数的 mchUser 信息
	 * 
	 * @param pageNum
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<CacheMchUser> findMchUserByCondition(int pageNum,
			List<MchSearchCondition> conditions) {
		logger.debug("the pageNum = " + pageNum);
		if (pageNum < 0) {
			pageNum = 0;
		}

		int start = pageNum * PageConfigUtil.PAGE_COUNT_SIZE_10;
		int end = start + PageConfigUtil.PAGE_COUNT_SIZE_10;

		logger.debug("the pageNum = " + pageNum + ",start=" + start + ",end="
				+ end);
		Iterator<Entry<Integer, CacheMchUser>> it = mchsMap.entrySet()
				.iterator();
		int current = 0;
		List<CacheMchUser> mchUsers = new ArrayList<CacheMchUser>();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();

			boolean isIndexFix = (current >= start && current < end);
			CacheMchUser cacheUser = (CacheMchUser) entry.getValue();
			if (isMchMatchCondition(cacheUser, conditions)) {
				if (isIndexFix) {
					mchUsers.add(cacheUser);
					logger.debug("find the mchUser mchUserId="
							+ ((CacheMchUser) entry.getValue()).getMchUser()
									.getMchId());
				}

				current++;

			}
		}

		logger.debug("find the total mchUsers's size=" + mchUsers.size());
		return mchUsers.size() == 0 ? null : mchUsers;

	}

	// 判断一个mchUser是否满足搜索条件
	private boolean isMchMatchCondition(CacheMchUser cacheMch,
			List<MchSearchCondition> conditions) {
		if (conditions == null || conditions.size() == 0) {
			return true;
		}

		for (MchSearchCondition condition : conditions) {

			if (condition.getConditionName().equals(
					MchSearchCondition.ConditionName.MCH_NAME)) {

				if (!cacheMch.getMchUser().getShopName()
						.contains((String) condition.getConditionValue())) {
					return false;
				}

			}

			if (condition.getConditionName().equals(
					MchSearchCondition.ConditionName.MCH_ADDRESS)) {
				if (!cacheMch.getMchUser().getShopAddress()
						.contains((String) condition.getConditionValue())) {
					return false;
				}
			}

		}

		return true;

	}

	/**
	 * 根据 商家名检索对应的商家信息
	 * 
	 * @param name
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public TbMch getMchUserByName(String name) {
		TbMch returnMchUser = null;
		if (StringUtils.isBlank(name)) {
			logger.error("name is null");
			return returnMchUser;
		}

		TbMch tmpMchUser = null;
		Iterator<Entry<Integer, CacheMchUser>> it = mchsMap.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			tmpMchUser = ((CacheMchUser) entry.getValue()).getMchUser();
			if (name.equals(tmpMchUser.getMchName())) {
				logger.debug("find the mchUser mchUserId="
						+ tmpMchUser.getMchId());
				returnMchUser = tmpMchUser;
				break;
			}

		}

		return returnMchUser;
	}

	/**
	 * 由定时器定时调用更新 商品的 访问次数，刷新到数据库
	 * 
	 * @author snnile2012
	 * @param
	 * @return
	 * @throws IllegalAccessException
	 * @since JDK 1.7
	 */
	@SuppressWarnings({ "unchecked", "restriction" })
	public boolean registerTask(Runnable task) {
		executor.execute(task);
		logger.warn("happylife register task ok");
		return true;
	}

	
	public List<CacheProduct> getAllCacheProducts(){
		return allProducts;
	}

	public  ConcurrentHashMap<Integer, Map<String, Integer>> getStaticDataMap(){
		return staticDataMap;
	}

	/**
	 * 缓存是否初始化成功
	 * 
	 * @return
	 */
	public boolean isCacheOk() {
		return isFinished;
	}

}
