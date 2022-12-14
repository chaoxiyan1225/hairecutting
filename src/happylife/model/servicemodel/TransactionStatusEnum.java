package happylife.model.servicemodel;

public enum TransactionStatusEnum {
	
	NOT_PAY(0,"待支付"),PAYED_NOASSIGNED(1,"已支付"),ASSIGED_STAFF(2,"待受理人完成订单"),FINISHED_FOR_CONFIRM(3,"已完成待核实");
	
	private TransactionStatusEnum(int statusInt,String statusMsg){
		this.statusInt = statusInt;
		this.statusMsg = statusMsg;
	
	}
	
	private int statusInt;
	private String statusMsg;
	
    // 覆盖方法
    @Override
    public String toString() {
       return this.statusMsg + this.statusInt;
      
    }

	public int getStatusInt() {
		return statusInt;
	}

	public String getStatusMsg() {
		return statusMsg;
	}
   
	
}
