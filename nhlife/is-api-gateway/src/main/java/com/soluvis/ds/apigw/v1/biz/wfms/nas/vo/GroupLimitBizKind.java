package com.soluvis.ds.apigw.v1.biz.wfms.nas.vo;

import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasReadVo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Class 		: GroupLimitBizKind
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  그룹별 정원정보
 */
@Data
@AllArgsConstructor
public class GroupLimitBizKind implements CommonNasReadVo {
	String sysKind;			//1 시스템구분
	String dateFrom;		//2 기준년월
	String arrCenterCd;		//3 센터
	String arrGroupCd;		//4 그룹
	String bizClasCd;		//5 업무구분
	String limitPerson;		//6 정원
	String agentId;			//7 수정자ID
}
