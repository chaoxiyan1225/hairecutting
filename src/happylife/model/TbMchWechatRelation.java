package happylife.model;

import java.util.Date;

public class TbMchWechatRelation {
	
    private Integer id;
	private String fkOpenId;
	private int fkMchId;
	private String wechatName;
	private int isDelete;
	private String extProps;
	private Date followTime;
	private Date cancelTime;
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getFkOpenId() {
		return fkOpenId;
	}
	public void setFkOpenId(String fkOpenId) {
		this.fkOpenId = fkOpenId;
	}
	public int getFkMchId() {
		return fkMchId;
	}
	public void setFkMchId(int fkMchId) {
		this.fkMchId = fkMchId;
	}
	public String getWechatName() {
		return wechatName;
	}
	public void setWechatName(String wechatName) {
		this.wechatName = wechatName;
	}
	public int getIsDelete() {
		return isDelete;
	}
	public void setIsDelete(int isDelete) {
		this.isDelete = isDelete;
	}
	public String getExtProps() {
		return extProps;
	}
	public void setExtProps(String extProps) {
		this.extProps = extProps;
	}
	public Date getFollowTime() {
		return followTime;
	}
	public void setFollowTime(Date followTime) {
		this.followTime = followTime;
	}
	public Date getCancelTime() {
		return cancelTime;
	}
	public void setCancelTime(Date cancelTime) {
		this.cancelTime = cancelTime;
	}
	@Override
	public String toString() {
		return "TbMchWechatRelation [id=" + id + ", fkOpenId=" + fkOpenId
				+ ", fkMchId=" + fkMchId + ", wechatName=" + wechatName
				+ ", isDelete=" + isDelete + ", extProps=" + extProps
				+ ", followTime=" + followTime + ", cancelTime=" + cancelTime
				+ "]";
	}

}
