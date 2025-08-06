package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: SumChatGroupDay
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  채팅집계 정보
 */
@Data
public class SumChatGroupDay implements CommonNasWriteVo {
	String hd;					//1  휴일
	String dt;					//2  날짜
	String wk;					//3  요일
	String mrp;					//4  MRP
	String accountGroupName;	//5  계좌그룹명
	String accountGroupId;		//6  계좌그룹ID
	String allCalls;			//7  총콜건수
	String toAgent;				//8  상담사연결건수
	String abandon;				//9  포기건수
	String callback;			//10 콜백건수
	String answer;				//11 응답건수
	String serviceLevelCnt;		//12 10초내응답건수
	
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(hd).append("|").append(dt).append("|")
		.append(wk).append("|").append(mrp).append("|")
		.append(accountGroupName).append("|").append(accountGroupId).append("|")
		.append(allCalls).append("|").append(toAgent).append("|")
		.append(abandon).append("|").append(callback).append("|")
		.append(answer).append("|").append(serviceLevelCnt);
		
		return sb.toString();
	}
}
