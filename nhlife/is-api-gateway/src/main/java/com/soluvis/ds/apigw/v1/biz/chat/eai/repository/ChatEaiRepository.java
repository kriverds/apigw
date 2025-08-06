package com.soluvis.ds.apigw.v1.biz.chat.eai.repository;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.soluvis.ds.apigw.v1.biz.chat.eai.mapper.ChatEaiMapper;

/**
 * @Class 		: ChatEaiRepository
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CHAT EAI Repository
 */
@Repository
public class ChatEaiRepository {
	
	static final Logger logger = LoggerFactory.getLogger(ChatEaiRepository.class);
	
	/**
	 * 스프링 DI
	 */
	ChatEaiMapper chatEaiMapper;
	public ChatEaiRepository(ChatEaiMapper chatEaiMapper) {
		this.chatEaiMapper = chatEaiMapper;
	}
	
	/**
	 * @Method		: setEaiChatResult15min
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  채팅상담 통계연동 데이터 적재
	 * 
	 *  1. Temp 테이블에 데이터 Insert
	 *  2. Temp 테이블에서 데이터 Merge
	 */
	@Transactional
	public int setEaiChatResult15min(Map<String,Object> params) {
		int insertCnt = chatEaiMapper.insertEaiChatResult15minTemp(params);
		int mergeCnt = chatEaiMapper.mergeEaiChatResult15min();
		logger.info("setEaiChatResult15min insertCnt[{}] mergeCnt[{}] ", insertCnt, mergeCnt);
		return mergeCnt;
	}
	
	/**
	 * @Method		: aggregateChatStat
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. p_set_chat 호출
	 */
	public void aggregateChatStat(Map<String,Object> params) {
		int insertCnt = chatEaiMapper.callAggChatStat(params);
		logger.info("aggregateChatStat insertCnt[{}]", insertCnt);
	}
}