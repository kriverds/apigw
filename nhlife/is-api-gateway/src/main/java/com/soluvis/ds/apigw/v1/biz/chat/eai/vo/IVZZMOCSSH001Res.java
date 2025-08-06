package com.soluvis.ds.apigw.v1.biz.chat.eai.vo;

import java.util.ArrayList;
import java.util.List;

import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * @Class 		: IVZZMOCSSH001Res
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IF-IS-0006(모바일앱 콜센터현황조회) Response VO Class
 * 
 *  1. 수신 전문 7번째 자리가 공백이 아닐경우 전문을 전체적으로 한자리 뒤로 미룬다. STX(1byte) + length(5~6byte)
 */
@Getter
@ToString
public class IVZZMOCSSH001Res {
	
	IVZZMOCSSH001Res(){}

	static final int LEN_OFFSET				= 334;
	
								//No Name
	String domain;				//1  도메인
	String nodeAlias;			//2  카테고리별칭
	String msgCd;				//3  메세지코드
	String msg;					//4  메세지
	String listCnt;				//5  리스트개수
	
	static final int LEN_DOMAIN 			= 14;
	static final int LEN_NODE_ALIAS 		= 20;
	static final int LEN_MSG_CD 			= 4;
	static final int LEN_MSG 				= 20;
	static final int LEN_LIST_CNT			= 8;
											//66
											//400
								//No Name
	String date;				//1  일자
	String time;				//2  시간
	int inCnt;					//3  인입건수
	int callbackCnt;			//4  콜백건수
	int failInCnt;				//5  인입불가건수
	int totalAnswerCnt;			//6  총처리건수
	int totalNotAnswerCnt;		//7  총포기건수
	int serviceLevelCnt;		//8  목표내수락건수
	String accountGroupName;	//9  상담사그룹
	String accountGroupId;		//10 상담사그룹코드
	
	static final int LEN_DATE 				= 8;
	static final int LEN_TIME 				= 4;
	static final int LEN_IN_CNT 			= 8;
	static final int LEN_CALLBACK_CNT 		= 8;
	static final int LEN_FAIL_IN_CNT 		= 8;
	static final int LEN_TOTAL_ANSWER_CNT 	= 8;
	static final int LEN_TOTAL_NOT_ANSWER_CNT = 8;
	static final int LEN_SERVICE_LEVEL_CNT 	= 8;
	static final int LEN_ACCOUNT_GROUP_NAME = 50;
	static final int LEN_ACCOUNT_GROUP_ID 	= 14;
											//124
	@Getter
	List<IVZZMOCSSH001Res> dataList;

	public IVZZMOCSSH001Res(byte[] arrByte) {
		String ca7 = LangUtil.splitByteArray(arrByte, 6, 1).trim();
		int offset;
		if(!"".equals(ca7)) {
			offset = LEN_OFFSET + 1;
		} else {
			offset = LEN_OFFSET;
		}
		
		this.domain 			= LangUtil.splitByteArray(arrByte, offset, LEN_DOMAIN).trim();				offset += LEN_DOMAIN;
		this.nodeAlias 			= LangUtil.splitByteArray(arrByte, offset, LEN_NODE_ALIAS).trim();			offset += LEN_NODE_ALIAS;
		this.msgCd 				= LangUtil.splitByteArray(arrByte, offset, LEN_MSG_CD).trim();				offset += LEN_MSG_CD;
		this.msg 				= LangUtil.splitByteArray(arrByte, offset, LEN_MSG).trim();					offset += LEN_MSG;
		this.listCnt 			= LangUtil.splitByteArray(arrByte, offset, LEN_LIST_CNT).trim();			offset += LEN_LIST_CNT;
		
		dataList = new ArrayList<>();
		
		int iListCnt = LangUtil.toInt(listCnt);
		for (int i = 0; i < iListCnt; i++) {
			IVZZMOCSSH001Res res = new IVZZMOCSSH001Res();
			res.date 				= LangUtil.splitByteArray(arrByte, offset, LEN_DATE).trim();				offset += LEN_DATE;
			res.time 				= LangUtil.splitByteArray(arrByte, offset, LEN_TIME).trim();				offset += LEN_TIME;
			res.inCnt 				= LangUtil.toInt(LangUtil.splitByteArray(arrByte, offset, LEN_IN_CNT).trim());				offset += LEN_IN_CNT;
			res.callbackCnt 		= LangUtil.toInt(LangUtil.splitByteArray(arrByte, offset, LEN_CALLBACK_CNT).trim());		offset += LEN_CALLBACK_CNT;
			res.failInCnt 			= LangUtil.toInt(LangUtil.splitByteArray(arrByte, offset, LEN_FAIL_IN_CNT).trim());			offset += LEN_FAIL_IN_CNT;
			res.totalAnswerCnt 		= LangUtil.toInt(LangUtil.splitByteArray(arrByte, offset, LEN_TOTAL_ANSWER_CNT).trim());	offset += LEN_TOTAL_ANSWER_CNT;
			res.totalNotAnswerCnt 	= LangUtil.toInt(LangUtil.splitByteArray(arrByte, offset, LEN_TOTAL_NOT_ANSWER_CNT).trim());		offset += LEN_TOTAL_NOT_ANSWER_CNT;
			res.serviceLevelCnt 	= LangUtil.toInt(LangUtil.splitByteArray(arrByte, offset, LEN_SERVICE_LEVEL_CNT).trim());	offset += LEN_SERVICE_LEVEL_CNT;
			res.accountGroupName 	= LangUtil.splitByteArray(arrByte, offset, LEN_ACCOUNT_GROUP_NAME).trim();	offset += LEN_ACCOUNT_GROUP_NAME;
			res.accountGroupId 		= LangUtil.splitByteArray(arrByte, offset, LEN_ACCOUNT_GROUP_ID).trim();	offset += LEN_ACCOUNT_GROUP_ID;
			dataList.add(res);
		}
	}
}
