package com.soluvis.ds.apigw.v1.biz.chatbot.nas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Class 		: ChatbotNasProperties
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  nas.chatbot을 prefix로 properties를 가져오는 클래스
 */
@ConfigurationProperties(prefix = "nas.chatbot")
public record ChatbotNasProperties(
		CbStatsInfo cbStatsInfo,
		CmStatsInfo cmStatsInfo,
		HolInfo holInfo,
		SmsSendResult smsSendResult) {
	
	public record CbStatsInfo(String directory, String filename, String extension, String pattern) {}
	public record CmStatsInfo(String directory, String filename, String extension, String pattern) {}
	public record HolInfo(String directory, String filename, String extension, String pattern) {}
	public record SmsSendResult(String directory, String filename, String extension, String pattern) {}
}
