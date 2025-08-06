package com.soluvis.ds.apigw.v1.biz.apps.rest.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @Class 		: SGUser
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  스킬그룹사용자
 */
@Data
public class SGUser {
	@JsonProperty("UserId")
	String userId;				//1 사용자ID
	@JsonProperty("CTI_LGIN_ID")
	String ctiLginId;			//2 CTI 로그인ID
	@JsonProperty("UserNm")
	String userNm;				//3 사용자명
	@JsonProperty("Level1Cd")
	String level1Cd;			//4 센터코드
	@JsonProperty("Level1Nm")
	String level1Nm;			//5 센터명
	@JsonProperty("Level2Cd")
	String level2Cd;			//6 그룹코드
	@JsonProperty("Level2Nm")
	String level2Nm;			//7 그룹명
	@JsonProperty("Level3Cd")
	String level3Cd;			//8 팀코드
	@JsonProperty("Level3Nm")
	String level3Nm;			//9 팀명
	@JsonProperty("SGLevel")
	String sgLevel;				//10 스킬레벨
}