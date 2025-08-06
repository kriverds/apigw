package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;

import lombok.Data;

/**
 * @Class 		: CdInf
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  예측그룹 코드
 */
@Data
public class CdInf implements CommonNasWriteVo {
	String lrgCd;		//1 대분류코드
	String lrgNm;		//2 대분류명
	String smlCd;		//3 소분류코드
	String smlNm;		//4 소분류명
	String deleted;		//5 삭제여부
	String upCd;		//6 상위코드
	
	public String toFileString() {
		StringBuilder sb = new StringBuilder();
		sb.append(lrgCd).append("|").append(lrgNm).append("|")
		.append(smlCd).append("|").append(smlNm).append("|")
		.append(deleted).append("|").append(upCd);
		return sb.toString();
	}
}
