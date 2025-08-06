package com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasReadVo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Class 		: CmStatsInfo
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  챗봇 캠페인결과 데이터 적재
 */
@Data
@AllArgsConstructor
public class CmStatsInfo implements CommonNasReadVo {

	String cmpKey;			//1  캠페인키
	String cmpType;			//2  캠페인유형
	String cmpId;			//3  캠페인ID
	String execCmpId;		//4  실행캠페인ID
	String execCmpCnt;		//5  캠페인회차
	String custNo;			//6  고객번호
	String callListSeqno;	//7  고객순번
	String frstConnDatm;	//8  최초접속일시
	String finlConnDatm;	//9  최종접속일시
	String finlRespDatm;	//10 최종응답일시
	String respCnt;			//11 응답횟수
	String fcYesYn;			//12 상담신청여부
	String cmpltYn;			//13 챗봇완료여부
}