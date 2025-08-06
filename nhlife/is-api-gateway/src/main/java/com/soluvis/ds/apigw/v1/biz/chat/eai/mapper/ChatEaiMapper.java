package com.soluvis.ds.apigw.v1.biz.chat.eai.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * @Class 		: ChatEaiMapper
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CHAT EAI Mapper
 */
@Mapper
public interface ChatEaiMapper {
	public int insertEaiChatResult15minTemp(Map<String,Object> params);
	public int mergeEaiChatResult15min();
	
	public int callAggChatStat(Map<String,Object> params);
}
