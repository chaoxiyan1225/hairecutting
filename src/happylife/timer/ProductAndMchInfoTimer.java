package happylife.timer;

import happylife.util.cache.MchStaffProductCacheManager;

import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 定时刷新 商品以及商家的信息
 * @author 闫朝喜
 *
 */
public class ProductAndMchInfoTimer extends TimerTask {

	private static final Log logger = LogFactory.getLog(ProductAndMchInfoTimer.class);

//	private  MchProductsActivityManager managerInstance = null ;

	public ProductAndMchInfoTimer()
	{
		logger.warn("init the mchandproductsmanager instance start....");
		//genericService.ge
		logger.warn("init the mchandproductsmanager instance finished.");
	}


	/* 
	 * 定时刷新  商品 以及商家信息
	 */
	@Override
	public void run() {
	}

}
