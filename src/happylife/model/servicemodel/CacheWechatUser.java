package happylife.model.servicemodel;

import java.util.List;

import happylife.model.TbMchWechatRelation;
import happylife.model.TbQueueRecord;
import happylife.model.TbWechatUser;

public class CacheWechatUser {
	
	private  TbWechatUser  wechatUser;
	
	private List<TbQueueRecord> records;
	
	private boolean isLoadFromDB =false ;
	
	private TbMchWechatRelation relation;

	public TbWechatUser getWechatUser() {
		return wechatUser;
	}

	public void setWechatUser(TbWechatUser wechatUser) {
		this.wechatUser = wechatUser;
	}
	
	public List<TbQueueRecord> getRecords() {
		return records;
	}

	public void setRecords(List<TbQueueRecord> records) {
		this.records = records;
	}

	public void setIsLoadFromDBTrue(){
		this.isLoadFromDB =  true ;
	}
	
	public boolean getIsLoadFromDB(){
		return this.isLoadFromDB;
	}

	public TbMchWechatRelation getRelation() {
		return relation;
	}

	public void setRelation(TbMchWechatRelation relation) {
		this.relation = relation;
	}
	
	
	
}
