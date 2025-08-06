package com.soluvis.ds.apigw.v1.biz.mobile.eai.vo;

import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * @Class 		: IVZZMOZZSH001Res
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IF-IS-0006(모바일앱 콜센터현황조회) Response VO Class
 */
@Getter
@ToString
public class IVZZMOZZSH001Res {
	
	IVZZMOZZSH001Res(){}
	
	static final int LEN_OFFSET		= 334;
	
						//No Name
	String msgCd;		//1  메세지코드
	String msg;			//2  메세지
	
	static final int LEN_MSG_CD 	= 4;
	static final int LEN_MSG 		= 20;
									//24
									//358
	public IVZZMOZZSH001Res(byte[] arrByte) {
		int offset = LEN_OFFSET;
		msgCd 		= LangUtil.splitByteArray(arrByte, offset, LEN_MSG_CD).trim();		offset += LEN_MSG_CD;
		msg 	= LangUtil.splitByteArray(arrByte, offset, LEN_MSG).trim();
	}
}
