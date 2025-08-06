package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: SumAgent30min
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  30분단위 상담사 콜정보
 */
@Data
public class SumAgent30min implements CommonNasWriteVo {
	String hd;					//1  휴일여부
	String wk;					//2  요일
	String dt;					//3  날짜
	String hh;					//4  시간
	String mm;					//5  분
	String level1Cd;			//6  센터코드
	String level2Cd;			//7  그룹코드
	String level3Cd;			//8  팀코드
	String peripheralNumber;	//9  사번
	String mrp;					//10 MRP
	String menuCode;			//11 메뉴코드
	String ibRna;				//12 IB RONA
	String ib;					//13 IB 응대호
	String answer10s;			//14 10초내 응답건수
	String answer20s;			//15 20초내 응답건수
	String ibTalkTime;			//16 IB 통화시간
	String obTry;				//17 OB 발신호
	String obDelayTime;			//18 OB 발신시간
	String obAnswer;			//19 OB 응대호
	String obTalkTime;			//20 OB 통화시간
	String holdTime;			//21 보류시간
	String pds;					//22 PDS 발신건수
	String pdsTalkTime;			//23 PDS 통화시간
	String ptAnswer;			//24 호전환 응대건수
	String ptRequest;			//25 호전환 요청건수
	String ptTalkTime;			//26 호전환 통화시간
	String hold;				//27 보류건수
	String ibMod;				//28 그룹기준 IB건수
	
	@Override
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(hd).append("|").append(wk).append("|")
		.append(dt).append("|").append(hh).append("|")
		.append(mm).append("|").append(level1Cd).append("|")
		.append(level2Cd).append("|").append(level3Cd).append("|")
		.append(peripheralNumber).append("|").append(mrp).append("|")
		.append(menuCode).append("|").append(ibRna).append("|")
		.append(ib).append("|").append(answer10s).append("|")
		.append(answer20s).append("|").append(ibTalkTime).append("|")
		.append(obTry).append("|").append(obDelayTime).append("|")
		.append(obAnswer).append("|").append(obTalkTime).append("|")
		.append(holdTime).append("|").append(pds).append("|")
		.append(pdsTalkTime).append("|").append(ptAnswer).append("|")
		.append(ptRequest).append("|").append(ptTalkTime).append("|")
		.append(hold).append("|").append(ibMod);
		return sb.toString();
	}
}
