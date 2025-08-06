package com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasReadVo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Class 		: SmsSendResult
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  챗봇 알림톡 전송결과 데이터 적재
 */
@Data
@AllArgsConstructor
public class SmsSendResult implements CommonNasReadVo {

	String cmpKey;			//1  캠페인키
	String cmpType;			//2  캠페인유형
	String cmpId;			//3  캠페인ID
	String execCmpId;		//4  실행캠페인ID
	String execCmpCnt;		//5  캠페인회차
	String custNo;			//6  고객번호
	String callListSeqno;	//7  고객순번
	String sndReqDtm;		//8  발송요청일자
	String sndDtm;			//9  알림톡 전송일시
	String rsltCd;			//10 알림톡 결과코드
}