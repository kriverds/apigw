package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: SumAgentDay
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  상담사 실적정보
 */
@Data
public class SumAgentDay implements CommonNasWriteVo {
	String dt;				//1  날짜
	String level1Cd;		//2  센터코드
	String level2Cd;		//3  그룹코드
	String level3Cd;		//4  팀코드
	String level1Nm;		//5  센터명
	String level2Nm;		//6  그룹명
	String level3Nm;		//7  팀명
	String sortOrd;			//8  정렬순서
	String userId;			//9  사번
	String userNm;			//10 상담사명
	String workPeriod;		//11 근무기간
	String ibRna;			//12 IB RONA호
	String ib;				//13 IB 응대호
	String answer10s;		//14 10초내 응답건수
	String answer20s;		//15 20초내 응답건수
	String loginTime;		//16 로그인시간
	String ibTalkTime;		//17 IB통화시간
	String obTry;			//18 OB발신호
	String obDelayTime;		//19 OB발신시간
	String obAnswer;		//20 OB응대호
	String obTalkTime;		//21 OB통화시간
	String callTime;		//22 콜시간
	String callTimeAvg;		//23 콜시간평균
	String groupRank;		//24 그룹순위
	String ibTalkTimeAvg;	//25 평균IB통화시간
	String obTryAvg;		//26 평균OB발신시간
	String obAnswerAvg;		//27 평균OB통화시간
	String reason1TimeAvg;	//28 평균이석시간1
	String totalCall;		//29 총통화건수
	String holdTime;		//30 보류시간
	String pds;				//31 PDS발신건수
	String ptAnswer;		//32 호전환응대건수
	String ptRequest;		//33 호전환요청건수
	String ptTalkTime;		//34 호전환통화시간
	String reason;			//35 이석건수
	String reason0;			//36 이석건수0
	String reason1;			//37 이석건수1
	String reason2;			//38 이석건수2
	String reason3;			//39 이석건수3
	String reason4;			//40 이석건수4
	String reason5;			//41 이석건수5
	String reason6;			//42 이석건수6
	String reason7;			//43 이석건수7
	String reasonTime;		//44 이석시간
	String reasonTime1;		//45 이석시간1
	String reasonTime2;		//46 이석시간2
	String reasonTime3;		//47 이석시간3
	String reasonTime4;		//48 이석시간4
	String reasonTime5;		//49 이석시간5
	String reasonTime6;		//50 이석시간6
	String reasonTime7;		//51 이석시간7
	String readyTime;		//52 대기시간
	String reasonTimeTotal;	//53 총이석시간
	String color;			//54 색상
	String obTotalTime;		//55 총OB시간
	String obTotalAvg;		//56 평균OB시간
	
	@Override
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(dt).append("|").append(level1Cd).append("|")
		.append(level2Cd).append("|").append(level3Cd).append("|")
		.append(level1Nm).append("|").append(level2Nm).append("|")
		.append(level3Nm).append("|").append(sortOrd).append("|")
		.append(userId).append("|").append(userNm).append("|")
		.append(workPeriod).append("|").append(ibRna).append("|")
		.append(ib).append("|").append(answer10s).append("|")
		.append(answer20s).append("|").append(loginTime).append("|")
		.append(ibTalkTime).append("|").append(obTry).append("|")
		.append(obDelayTime).append("|").append(obAnswer).append("|")
		.append(obTalkTime).append("|").append(callTime).append("|")
		.append(callTimeAvg).append("|").append(groupRank).append("|")
		.append(ibTalkTimeAvg).append("|").append(obTryAvg).append("|")
		.append(obAnswerAvg).append("|").append(reason1TimeAvg).append("|")
		.append(totalCall).append("|").append(holdTime).append("|")
		.append(pds).append("|").append(ptAnswer).append("|")
		.append(ptRequest).append("|").append(ptTalkTime).append("|")
		.append(reason).append("|").append(reason0).append("|")
		.append(reason1).append("|").append(reason2).append("|")
		.append(reason3).append("|").append(reason4).append("|")
		.append(reason5).append("|").append(reason6).append("|")
		.append(reason7).append("|").append(reasonTime).append("|")
		.append(reasonTime1).append("|").append(reasonTime2).append("|")
		.append(reasonTime3).append("|").append(reasonTime4).append("|")
		.append(reasonTime5).append("|").append(reasonTime6).append("|")
		.append(reasonTime7).append("|").append(readyTime).append("|")
		.append(reasonTimeTotal).append("|").append(color).append("|")
		.append(obTotalTime).append("|").append(obTotalAvg);
		
		return sb.toString();
	}
}
