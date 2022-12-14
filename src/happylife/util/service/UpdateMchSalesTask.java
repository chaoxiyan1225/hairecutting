package happylife.util.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import happylife.dao.GenericDao;
import happylife.model.TbMch;
import happylife.model.servicemodel.CacheMchUser;
import happylife.util.cache.CacheIndex;
import happylife.util.cache.MchStaffProductCacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;

public class UpdateMchSalesTask implements Runnable {
	private static final Log logger = LogFactory.getLog(UpdateMchSalesTask.class);
	private GenericDao genericDao;

	public UpdateMchSalesTask(GenericDao dao) {
		this.genericDao = dao;
	}
	

	@Override
	public void run() {
        logger.warn("happylife start update mch sale ");
		List<CacheMchUser> cacheMchs = MchStaffProductCacheManager.getInstance().getAllMchUsers();
		//List<TbProduct> products = null;
		TbMch tmpMchUser = null;
		int totalSaleCounts = 0; // 所有商户的销售次数以及销售金额
		int totalSaleMoney = 0;

		for(CacheMchUser cacheMch:cacheMchs) {
		
			tmpMchUser =cacheMch.getMchUser();
//			products = ((CacheMchUser) entry.getValue()).getProducts();
//			if (products == null || products.size() == 0) {
//				logger.warn("the mchuser have no products, id="
//						+ ((CacheMchUser) entry.getValue()).getMchUser()
//								.getMchId());
//				continue;
//			}

			int mchSaleCounts = tmpMchUser.getTotalSaleCount();
			int mchSaleMoney = tmpMchUser.getTotalMoney();
//			for (TbProduct p : products) {
//				mchSaleCounts += p.getSaleTotalTimes();
//				mchSaleMoney += p.getSaleTotalMoney();
//			}

			totalSaleCounts += mchSaleCounts;
			totalSaleMoney += mchSaleMoney;

			// 刷新统计内存中每个商户的销售次数以及金额

			MchStaffProductCacheManager.getInstance().getStaticDataMap().get(tmpMchUser.getMchId()).put(
					CacheIndex.MchUser.TOTAL_SALL_COUNTS, mchSaleCounts);
			MchStaffProductCacheManager.getInstance().getStaticDataMap().get(tmpMchUser.getMchId()).put(
					CacheIndex.MchUser.TOTAL_SALL_MONEY, mchSaleMoney);
			try {
				genericDao.update(tmpMchUser);

			} catch (DataAccessException e) {
				logger.error(tmpMchUser.toString());
				logger.error(e.getMessage());
			}

		}

		MchStaffProductCacheManager.getInstance().getStaticDataMap().get(CacheIndex.Admin.CACHE_INDEX).put(
				CacheIndex.Admin.TOTAL_SALL_COUNTS, totalSaleCounts);
		MchStaffProductCacheManager.getInstance().getStaticDataMap().get(CacheIndex.Admin.CACHE_INDEX).put(
				CacheIndex.Admin.TOTAL_SALL_MONEY, totalSaleMoney);
		
		logger.warn("happylife update mch sale finish ");

		return ;

	}

}
