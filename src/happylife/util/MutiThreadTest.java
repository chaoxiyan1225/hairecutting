package happylife.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MutiThreadTest {

	public synchronized void method1(){
		/*这是同步方法示例*/
		//step1
		method2();
		//step3
		
	}
	
	public synchronized void method2(){
		/*这是同步方法示例*/
		//step1
		//step2
		//step3
		
	}
	
	public void method3(){
		/*这是同步方法示例*/
		//step1
		synchronized(this){
		}
	}
}
