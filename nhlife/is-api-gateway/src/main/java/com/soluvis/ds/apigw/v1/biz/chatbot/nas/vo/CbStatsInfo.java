package com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasReadVo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Class 		: CbStatsInfo
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  챗봇 상담현황 데이터 적재
 */
@Data
@AllArgsConstructor
public class CbStatsInfo implements CommonNasReadVo {

	String userKey;				//1 사용자키
	String chnlName;			//2 채널명
	String intentLv1;			//3 질의구분 레벨1
	String intentLv2;			//4 질의구분 레벨2
	String intentLv3;			//5 질의구분 레벨3
	String questionCnt;			//6 질문건수
	String callcenterConnYn;	//7 콜센터연결여부
	String frstConnDatm;		//8 최초접속일
	String finlRespDatm;		//9 최종응답일시

}