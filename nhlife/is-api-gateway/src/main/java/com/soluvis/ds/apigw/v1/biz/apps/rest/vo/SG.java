package com.soluvis.ds.apigw.v1.biz.apps.rest.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @Class 		: SG
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  스킬그룹
 */
@Data
public class SG {
	@JsonProperty("SGCd")
	String sgCd;				//1 스킬그룹
	@JsonProperty("SGNm")
	String sgNm;				//2 스킬그룹명
}