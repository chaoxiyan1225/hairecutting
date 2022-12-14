package happylife.model.servicemodel;

import happylife.model.TbMch;
import happylife.model.TbProduct;

public class CacheProduct {
	@Override
	public String toString() {
		return "CacheProduct [product=" + product + ", mchUser=" + mchUser
				+ ", discountPrice=" + discountPrice + "]";
	}

	private TbProduct product ;
	
	private TbMch mchUser;
	
	private int  discountPrice;

	public TbProduct getProduct() {
		return product;
	}

	public void setProduct(TbProduct product) {
		this.product = product;
	}


	public TbMch getMchUser() {
		return mchUser;
	}

	public void setMchUser(TbMch mchUser) {
		this.mchUser = mchUser;
	}

	public int getDiscountPrice() {
		return discountPrice;
	}

	public void setDiscountPrice(int discountPrice) {
		this.discountPrice = discountPrice;
	}

}
