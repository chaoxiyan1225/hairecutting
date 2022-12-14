package happylife.util.service;

import java.util.List;

import happylife.dao.GenericDao;
import happylife.model.TbProduct;
import happylife.model.servicemodel.CacheProduct;
import happylife.util.cache.MchStaffProductCacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UpdateProductVisitorTask implements Runnable {

	private static final Log logger = LogFactory.getLog(UpdateMchSalesTask.class);
	private GenericDao genericDao;

	public UpdateProductVisitorTask(GenericDao dao) {
		this.genericDao = dao;
	}
	
	
	@Override
	public void run() {
		logger.warn("happylife start update all products");
		TbProduct p = null;
		List<CacheProduct> allProducts = MchStaffProductCacheManager.getInstance().getAllCacheProducts();
		// 访问次数已经预先在缓存中更新了，只需要回填到数据库
		for (CacheProduct product : allProducts) {
			p = product.getProduct();
			genericDao.update(p);

		}
		
		logger.warn("happylife finish update all products");

		return ;

	}

}
