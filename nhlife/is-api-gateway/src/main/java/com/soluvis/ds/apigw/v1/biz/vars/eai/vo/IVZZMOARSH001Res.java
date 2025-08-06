package com.soluvis.ds.apigw.v1.biz.vars.eai.vo;

import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * @Class 		: IVZZMOARSH001Res
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IF-IS-0004(보이는ARS 콜센터현황조회) Response VO Class
 */
@Getter
@ToString
public class IVZZMOARSH001Res {
	
	IVZZMOARSH001Res(){}
	
	static final int LEN_OFFSET		= 334;
	
						//No Name
	String msgCd;		//1  메세지코드
	String msg;			//2  메세지
	
	static final int LEN_MSG_CD 	= 4;
	static final int LEN_MSG 		= 20;
									//24
									//358
	public IVZZMOARSH001Res(byte[] arrByte) {
		int offset = LEN_OFFSET;
		msgCd 		= LangUtil.splitByteArray(arrByte, offset, LEN_MSG_CD).trim();		offset += LEN_MSG_CD;
		msg 	= LangUtil.splitByteArray(arrByte, offset, LEN_MSG).trim();
	}
}
