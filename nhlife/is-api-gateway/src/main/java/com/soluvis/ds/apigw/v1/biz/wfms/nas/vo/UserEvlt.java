package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasReadVo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Class 		: UserEvlt
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  평가대상자 업데이트 정보
 */
@Data
@AllArgsConstructor
public class UserEvlt implements CommonNasReadVo {
	String syskind;			//1 시스템구분
	String userId;			//2 대상자ID
	String evltYn;			//3 평가대상자여부
}
