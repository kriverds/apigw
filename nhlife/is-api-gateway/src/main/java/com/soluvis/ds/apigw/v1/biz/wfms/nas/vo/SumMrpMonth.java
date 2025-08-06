package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: SumMrpMonth
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  월별 대표번호 응대현황
 */
@Data
public class SumMrpMonth implements CommonNasWriteVo {
	String dt;				//1 날짜
	String name;			//2 이름
	String sl;				//3 서비스레벨
	String toAgent;			//4 상담요청건수
	String answer;			//5 응답건수
	String answer10s;		//6 응답건수10초내
	String answer20s;		//7 응답건수20초내
	String sortOrd;			//8 순번
	
	@Override
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(dt).append("|").append(name).append("|")
		.append(sl).append("|").append(toAgent).append("|")
		.append(answer);
		
		return sb.toString();
	}
}
