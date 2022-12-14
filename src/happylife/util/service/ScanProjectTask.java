package happylife.util.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;

import happylife.dao.GenericDao;
import happylife.model.TbProduct;
import happylife.model.TbTransactionRecord;
import happylife.model.servicemodel.CacheProduct;
import happylife.model.servicemodel.HqlQueryCondition;
import happylife.model.servicemodel.HqlQueryCondition.Property;
import happylife.model.servicemodel.HqlQueryCondition.Relation;
import happylife.service.impl.WeChatUserServiceImpl;
import happylife.util.cache.MchStaffProductCacheManager;

public class ScanProjectTask implements Runnable {

	private static final Log logger = LogFactory.getLog(ScanProjectTask.class);
	private GenericDao genericDao;

	public ScanProjectTask(GenericDao dao) {
		this.genericDao = dao;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		/**
		 * 扫描交易记录，并更新 到商品的销售额度以及销售次数里面 scanRecordsAndUpdate:(这里用一句话描述这个方法的作用). <br/>
		 */
		
		logger.warn("happylife start ScanProjectTask ");

		int failTimes = 0;
		// 每次扫描100 条交易记录，且 交易记录是微信支付成功 且 未扫描过的
		while (failTimes < 10) {
			HqlQueryCondition query = new HqlQueryCondition();
			query.setFirstResult(0);
			query.setMaxResults(100);
			query.getProperties().add(
					new Property(Relation.EQ, "isScaned", Integer.valueOf(0)));
			query.getProperties().add(
					new Property(Relation.EQ, "recordStatus", Integer
							.valueOf(1)));
			query.setAesc(true);
			query.setOrderName("recordTime");
			List<TbTransactionRecord> records = null;
			try {
				logger.info("update the  mchUserinfo to db.");
				records = genericDao.getListByQueryCondtion(
						TbTransactionRecord.class, query);
				if (null == records) {
					logger.warn("all new records have been scaned");
					return;
				}

			} catch (DataAccessException e) {
				logger.error(e.getMessage());
				failTimes++;
			}

			// 把每个交易记录刷新到对应的商品中
			CacheProduct cacheProduct = null;
			TbProduct product = null;
			boolean isProductSuccess = false;
			for (TbTransactionRecord record : records) {
				try {
					// logger.info("update the record id=" +
					// record.getRecordId());
					cacheProduct = MchStaffProductCacheManager.getInstance()
							.getProductById(record.getFkProductId());
					if (null == record || cacheProduct == null) {
						// logger.warn("the prodcut not exit,id="
						// + record.getFkProductId());
						continue;
					}

					product = cacheProduct.getProduct();
					product.setSaleTotalTimes(product.getSaleTotalTimes()
							+ record.getProductNum());
					product.setSaleTotalMoney(product.getSaleTotalMoney()
							+ record.getRecordMoney());

					genericDao.update(product);

					isProductSuccess = true;
					// 更新记录的扫描状态为已扫描
					record.setIsScaned(1);
					genericDao.update(record);

					// 还原标志位
					isProductSuccess = false;

				} catch (DataAccessException e) {
					logger.error(e.getMessage());
					// 失败后将缓存中的商品信息还原
					product.setSaleTotalTimes(product.getSaleTotalTimes()
							- record.getProductNum());
					product.setSaleTotalMoney(product.getSaleTotalMoney()
							- record.getRecordMoney());
					failTimes++;
					// 商品已经刷新过但是 交易记录更新失败则回退一次 商品信息
					if (isProductSuccess) {
						genericDao.update(product);

					}

				}

			}
			
			logger.warn("happylife finish ScanProjectTask");
			return;
		}

		return;
	}

}
