package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: SumHo30min
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  30분단위 CTIQ별 콜정보
 */
@Data
public class SumHo30min implements CommonNasWriteVo {
	String hd;				//1  휴일여부
	String wk;				//2  요일
	String dt;				//3  날짜
	String hh;				//4  시간
	String mm;				//5  분
	String mrp;				//6  MRP
	String ctiq;			//7  CTI큐
	String allCalls;		//8  총통화건수
	String toAgent;			//9  상담원요청건수
	String abandon;			//10 포기건수
	String abandon10s;		//11 10초내포기건수
	String abandon20s;		//12 20초내포기건수
	String abandon30s;		//13 30초내포기건수
	String abandon40s;		//14 40초내포기건수
	String abandon50s;		//15 50초내포기건수
	String abandon60s;		//16 60초내포기건수
	String abandon120s;		//17 120초내포기건수
	String abandon180s;		//18 180초내포기건수
	String abandon300s;		//19 300초내포기건수
	String callback;		//20 콜백건수
	String answer;			//21 응대건수
	String answer10s;		//22 10초내응대건수
	String answer20s;		//23 20초내응대건수
	String answer30s;		//24 30초내응대건수
	String answer40s;		//25 40초내응대건수
	String answer50s;		//26 50초내응대건수
	String answer60s;		//27 60초내응대건수
	String answer120s;		//28 120초내응대건수
	String answer180s;		//29 180초내응대건수
	String answer300s;		//30 300초내응대건수
	String talkTime;		//31 통화시간
	String ringTime;		//32 벨울림시간
	String toVocalArs;		//33 음성ARS요청호
	String toVisualArs;		//34 보이는ARS요청호
	String fromVocalArs;	//35 음성ARS인입호
	String fromVisualArs;	//36 보이는ARS인입호
	String callEndAtQueue;	//37 큐대기종료
	String netQTime;		//38 대기시간
	String localQTime;		//39 포기시간
	
	@Override
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(hd).append("|").append(wk).append("|")
		.append(dt).append("|").append(hh).append("|")
		.append(mm).append("|").append(mrp).append("|")
		.append(ctiq).append("|").append(allCalls).append("|")
		.append(toAgent).append("|").append(abandon).append("|")
		.append(abandon10s).append("|").append(abandon20s).append("|")
		.append(abandon30s).append("|").append(abandon40s).append("|")
		.append(abandon50s).append("|").append(abandon60s).append("|")
		.append(abandon120s).append("|").append(abandon180s).append("|")
		.append(abandon300s).append("|")
		.append(callback).append("|").append(answer).append("|")
		.append(answer10s).append("|").append(answer20s).append("|")
		.append(answer30s).append("|").append(answer40s).append("|")
		.append(answer50s).append("|").append(answer60s).append("|")
		.append(answer120s).append("|").append(answer180s).append("|")
		.append(answer300s).append("|")
		.append(talkTime).append("|").append(ringTime).append("|")
		.append(toVocalArs).append("|").append(toVisualArs).append("|")
		.append(fromVocalArs).append("|").append(fromVisualArs).append("|")
		.append(callEndAtQueue).append("|").append(netQTime).append("|")
		.append(localQTime);
		
		return sb.toString();
	}
	
}
