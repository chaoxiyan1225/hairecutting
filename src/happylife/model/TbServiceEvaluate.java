package happylife.model;

// Generated 2016-5-21 15:47:52 by Hibernate Tools 3.4.0.CR1

import java.util.Date;

/**
 * TbProduct generated by hbm2java
 */
public class TbServiceEvaluate implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -6414088976126783504L;
	private Integer id;
	private String nickName;
	private String email;
	private int fkMchId;
	private int fkUserId;
	private int fkMchstaffId;
	private int score;
	private int isUsed;
	private String detail;
	private Date createTime;
	private String extProps;

	public TbServiceEvaluate() {
		super();
	}

	public TbServiceEvaluate(Integer id, String nickName, String email, int fkMchId,
                             String info, Date createTime) {
		super();
		this.id = id;
		this.nickName = nickName;
		this.email = email;
		this.fkMchId = fkMchId;
		this.createTime = createTime;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getFkMchId() {
		return fkMchId;
	}

	public void setFkMchId(int fkMchId) {
		this.fkMchId = fkMchId;
	}




	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public int getFkUserId() {
		return fkUserId;
	}

	public void setFkUserId(int fkUserId) {
		this.fkUserId = fkUserId;
	}

	public String getExtProps() {
		return extProps;
	}

	public void setExtProps(String extProps) {
		this.extProps = extProps;
	}

	public int getFkMchstaffId() {
		return fkMchstaffId;
	}

	public void setFkMchstaffId(int fkMchstaffId) {
		this.fkMchstaffId = fkMchstaffId;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getIsUsed() {
		return isUsed;
	}

	public void setIsUsed(int isUsed) {
		this.isUsed = isUsed;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	@Override
	public String toString() {
		return "TbSuggestion [id=" + id + ", nickName=" + nickName + ", email="
				+ email + ", fkMchId=" + fkMchId
				+ ", createTime=" + createTime + "]";
	}
	
	
}