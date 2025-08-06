package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: SumReason30min
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  30분단위 상담사 상태정보
 */
@Data
public class SumReason30min implements CommonNasWriteVo {
	String hd;					//1  휴일여부
	String dt;					//2  요일
	String wk;					//3  날짜
	String hh;					//4  시간
	String mm;					//5  분
	String level1Cd;			//6  센터코드
	String level2Cd;			//7  그룹코드
	String level3Cd;			//8  팀코드
	String peripheralNumber;	//9  사번
	String loginTime;			//10 로그인시간
	String logoutTime;			//11 로그아웃시간
	String reason;				//12 이석건수
	String reason0;				//13 이석건수0
	String reason1;				//14 이석건수1
	String reason2;				//15 이석건수2
	String reason3;				//16 이석건수3
	String reason4;				//17 이석건수4
	String reason5;				//18 이석건수5
	String reason6;				//19 이석건수6
	String reason7;				//20 이석건수7
	String reason8;				//21 이석건수8
	String reason9;				//22 이석건수9
	String reasonTime;			//23 이석시간
	String reasonTime0;			//24 이석시간0
	String reasonTime1;			//25 이석시간1
	String reasonTime2;			//26 이석시간2
	String reasonTime3;			//27 이석시간3
	String reasonTime4;			//28 이석시간4
	String reasonTime5;			//29 이석시간5
	String reasonTime6;			//30 이석시간6
	String reasonTime7;			//31 이석시간7
	String reasonTime8;			//32 이석시간8
	String reasonTime9;			//33 이석시간9
	String readyTime;			//34 대기시간
	
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(hd).append("|").append(dt).append("|")
		.append(wk).append("|").append(hh).append("|")
		.append(mm).append("|").append(level1Cd).append("|")
		.append(level2Cd).append("|").append(level3Cd).append("|")
		.append(peripheralNumber).append("|").append(loginTime).append("|")
		.append(logoutTime).append("|").append(reason).append("|")
		.append(reason0).append("|").append(reason1).append("|")
		.append(reason2).append("|").append(reason3).append("|")
		.append(reason4).append("|").append(reason5).append("|")
		.append(reason6).append("|").append(reason7).append("|")
		.append(reason8).append("|").append(reason9).append("|")
		.append(reasonTime).append("|").append(reasonTime0).append("|")
		.append(reasonTime1).append("|").append(reasonTime2).append("|")
		.append(reasonTime3).append("|").append(reasonTime4).append("|")
		.append(reasonTime5).append("|").append(reasonTime6).append("|")
		.append(reasonTime7).append("|").append(reasonTime8).append("|")
		.append(reasonTime9).append("|").append(readyTime);
		
		return sb.toString();
	}
}
