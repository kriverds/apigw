package com.soluvis.ds.apigw.v1.biz.apps.rest.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @Class 		: UserSG
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  사용자스킬그룹
 */
@Data
public class UserSG {
	@JsonProperty("SGCd")
	String sgCd;				//1 스킬그룹
	@JsonProperty("SGNm")
	String sgNm;				//2 스킬그룹명
	@JsonProperty("SGLevel")
	String sgLevel;				//3 스킬그룹레벨
	@JsonProperty("HasYN")
	String hasYn;				//4 보유여부
}