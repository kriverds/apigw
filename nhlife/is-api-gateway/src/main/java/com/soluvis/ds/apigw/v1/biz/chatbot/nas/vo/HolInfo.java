package com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: HolInfo
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  챗봇 휴일정보 조회
 */
@Data
public class HolInfo implements CommonNasWriteVo {

	String holDay;			//1 휴일
	String createDatm;		//2 생성일시
	
	@Override
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(holDay).append("|").append(createDatm);
		
		return sb.toString();
	}
}