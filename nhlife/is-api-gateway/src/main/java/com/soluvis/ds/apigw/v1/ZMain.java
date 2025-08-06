package com.soluvis.ds.apigw.v1;

public class ZMain {
	
	public static void main(String[] args) {
		String param3 = "ErrorCode [unknown] ErrorMessage [Can not find person to delete] EmpID=12508503";
		String empId = param3.substring(param3.indexOf("=")+1, param3.length());
		System.out.println(empId);
	}
}
