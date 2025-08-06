package com.soluvis.ds.apigw.v1.biz.chatbot.nas.repository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.soluvis.ds.apigw.v1.biz.chatbot.nas.mapper.ChatbotNasMapper;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.CbStatsInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.CmStatsInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.HolInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.SmsSendResult;

/**
 * @Class 		: ChatbotNasRepository
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CHATBOT NAS Repository
 */
@Repository
public class ChatbotNasRepository {
	
	static final Logger logger = LoggerFactory.getLogger(ChatbotNasRepository.class);

	/**
	 * 스프링 DI
	 */
	ChatbotNasMapper chatbotNasMapper;

	public ChatbotNasRepository(ChatbotNasMapper chatbotNasMapper) {
		this.chatbotNasMapper = chatbotNasMapper;
	}

	/**
	 * @Method		: setCbStatsInfo
	 * @date   		: 2025. 3. 5.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 상담현황 데이터 적재
	 * 
	 *  1. Temp 테이블에 데이터 Insert
	 *  2. Temp 테이블에서 데이터 Merge
	 */
	@Transactional
	public int setCbStatsInfo(List<CbStatsInfo> voList) {
		int insertCnt = chatbotNasMapper.insertCbStatsInfoTemp(voList);
		int mergeCnt = chatbotNasMapper.mergeCbStatsInfo();
		logger.info("setCbStatsInfo insertCnt[{}] mergeCnt[{}] ", insertCnt, mergeCnt);
		return mergeCnt;
	}

	/**
	 * @Method		: setCmStatsInfo
	 * @date   		: 2025. 3. 5.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 캠페인결과 데이터 적재
	 * 
	 *  1. Temp 테이블에 데이터 Insert
	 *  2. Temp 테이블에서 데이터 Merge
	 *  3. mergeSumChatbotCampaign 실행
	 *  4. mergeSumChatbotExecCampaign 실행
	 */
	@Transactional
	public int setCmStatsInfo(List<CmStatsInfo> voList) {
		int insertCnt = chatbotNasMapper.insertCmStatsInfoTemp(voList);
		int mergeCnt = chatbotNasMapper.mergeCmStatsInfo();
		int mergeCampaign = chatbotNasMapper.mergeSumChatbotCampaign();
		int mergeCampaignExec = chatbotNasMapper.mergeSumChatbotExecCampaign();
		logger.info("setCmStatsInfo insertCnt[{}] mergeCnt[{}] ", insertCnt, mergeCnt);
		logger.info("setCmStatsInfo mergeCampaign[{}] mergeCampaignExec[{}] ", mergeCampaign, mergeCampaignExec);
		return mergeCnt;
	}

	/**
	 * @Method		: getHolInfo
	 * @date   		: 2025. 3. 5.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 휴일정보 조회
	 */
	public List<HolInfo> getHolInfo() {
		return chatbotNasMapper.selectHolInfo();
	}

	/**
	 * @Method		: setSendSmsResult
	 * @date   		: 2025. 3. 5.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 알림톡 전송결과 데이터 적재
	 * 
	 *  1. Temp 테이블에 데이터 Insert
	 *  2. Temp 테이블에서 데이터 Merge
	 */
	@Transactional
	public int setSendSmsResult(List<SmsSendResult> voList) {
		int insertCnt = chatbotNasMapper.insertSmsSendResultTemp(voList);
		int mergeCnt = chatbotNasMapper.mergeSmsSendResult();
		logger.info("setSendSmsResult insertCnt[{}] mergeCnt[{}] ", insertCnt, mergeCnt);
		return mergeCnt;
	}

}
