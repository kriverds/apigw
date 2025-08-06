package com.soluvis.ds.apigw.v1.biz.chat.eai.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.chat.eai.channel.IVZZMOCSSH001Handler;
import com.soluvis.ds.apigw.v1.biz.chat.eai.repository.ChatEaiRepository;
import com.soluvis.ds.apigw.v1.biz.chat.eai.vo.IVZZMOCSSH001Req;
import com.soluvis.ds.apigw.v1.biz.common.eai.client.EaiClient;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.CommonEaiHeader;
import com.soluvis.ds.apigw.v1.util.DateUtil;

import lombok.Setter;

/**
 * @Class 		: ChatEaiService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CHAT EAI Service
 */
@Service
public class ChatEaiService {
	
	static final Logger logger = LoggerFactory.getLogger(ChatEaiService.class);
	
	/**
	 * 스프링 DI
	 */
	ChatEaiRepository chatEaiRepository;
	EaiClient eaiClient;
	ApplicationContext applicationContext;
	public ChatEaiService(ChatEaiRepository chatEaiRepository, EaiClient eaiClient,
			ApplicationContext applicationContext) {
		this.chatEaiRepository = chatEaiRepository;
		this.eaiClient = eaiClient;
		this.applicationContext = applicationContext;
	}
	
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeIVZZMOCSSH001
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 공통 헤더 작성
	 *  2. EAI Request 작성
	 *  3. EAIClient 호출
	 *  4. EAI 통신 성공 시 Chat 집계 호출
	 */
	public JSONObject executeIVZZMOCSSH001() throws Exception {
		String key = IVZZMOCSSH001Req.getHEADER_KEY();
		int dataLen = IVZZMOCSSH001Req.getHEADER_DATA_LEN();
		String svcId = IVZZMOCSSH001Req.getHEADER_SVC_ID();
		String intfId = IVZZMOCSSH001Req.getHEADER_INTF_ID();
		
		CommonEaiHeader header = new CommonEaiHeader.Builder()
				.icKey(key).ecDataLen(Integer.toString(dataLen)).ecRcveSvcId(svcId).ecEaiIntfId(intfId)
				.build();
		
		String domain = "NODE0000000001";
		String nodeAlias = "category1";
		String fromDate = DateUtil.getDateString("yyyyMMdd");
		String fromTime = "0000";
		String toDate = DateUtil.getDateString("yyyyMMdd");
		String toTime = "2400";
		logger.info("[{}] IVZZMOCSSH001Req domain[{}] nodeAlias[{}] fromDate[{}] fromTime[{}] toDate[{}] toTime[{}]", uuid, domain, nodeAlias, fromDate, fromTime, toDate, toTime);
		
		IVZZMOCSSH001Req req = new IVZZMOCSSH001Req.Builder()
				.domain(domain).nodeAlias(nodeAlias).fromDate(fromDate).fromTime(fromTime).toDate(toDate).toTime(toTime)
				.build();
		
		String reqMsg = header.toEaiString()+req.toEaiString();
		
		logger.debug("[{}] EAI Request>>{}", uuid, reqMsg);
		eaiClient.setUuid(uuid);
		eaiClient.setHandlerSendMsg(reqMsg);
		
		JSONObject resultEai = eaiClient.execute(applicationContext.getBean(IVZZMOCSSH001Handler.class));
		
		if(resultEai.getString(Const.APIGW_KEY_RESULT_CD).equals(Const.APIGW_SUCCESS_CD)) {
			Map<String, Object> queryParams = new HashMap<>();
			String sdt = DateUtil.getDateString("yyyy-MM-dd");
			String edt = DateUtil.getDateString("yyyy-MM-dd",1);
			queryParams.put("sdt", sdt);
			queryParams.put("edt", edt);
			
			chatEaiRepository.aggregateChatStat(queryParams);
		}
		return resultEai;
	}
}
