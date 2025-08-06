package com.soluvis.ds.apigw.v1.biz.wfms.nas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Class 		: WfmsNasProperties
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  nas.wfms를 prefix로 properties를 가져오는 클래스
 */
@ConfigurationProperties(prefix = "nas.wfms")
public record WfmsNasProperties(
		CdInf cdInf,
		GroupLimitBizKind groupLimitBizKind,
		SumAgent30min sumAgent30min,
		SumAgentDay sumAgentDay,
		SumChatGroupDay sumChatGroupDay,
		SumHo30min sumHo30min,
		SumMrpMonth sumMrpMonth,
		SumReason30min sumReason30min,
		UserEvlt userEvlt) {
	
	public record CdInf(String directory, String filename, String extension, String pattern) {}
	public record GroupLimitBizKind(String directory, String filename, String extension, String pattern) {}
	public record SumAgent30min(String directory, String filename, String extension, String pattern) {}
	public record SumAgentDay(String directory, String filename, String extension, String pattern) {}
	public record SumChatGroupDay(String directory, String filename, String extension, String pattern) {}
	public record SumHo30min(String directory, String filename, String extension, String pattern) {}
	public record SumMrpMonth(String directory, String filename, String extension, String pattern) {}
	public record SumReason30min(String directory, String filename, String extension, String pattern) {}
	public record UserEvlt(String directory, String filename, String extension, String pattern) {}
}
