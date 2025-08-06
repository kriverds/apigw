package com.soluvis.ds.apigw.v1.biz.chat.eai.batch;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.application.common.service.CommonService;
import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.chat.eai.service.ChatEaiService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Class 		: ChatEaiScheduler
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 배치 시작 시 DB에 배치 로그 삽입
 *  2. 서비스 로직 처리
 *  3. 배치 종료 시 DB에 배치 로그 업데이트
 * 
 *  executeIVZZMOCSSH001Batch: 채팅상담 통계연동 | 0 0/5 * * * *
 */
@Component
public class ChatEaiScheduler {
	
	static final Logger logger = LoggerFactory.getLogger(ChatEaiScheduler.class);
	
	/**
	 * 스프링 DI
	 */
	ChatEaiService chatEaiService;
	CommonService commonService;
	public ChatEaiScheduler(ChatEaiService chatEaiService, CommonService commonService) {
		this.chatEaiService = chatEaiService;
		this.commonService = commonService;
	}

	/**
	 * @Method		: executeIVZZMOCSSH001Batch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  채팅상담 통계연동 배치 실행
	 */
	@Async
	@Scheduled(cron = "${scheduler.eai.chat}")
	@SchedulerLock(name = "executeIVZZMOCSSH001Batch")
	public void executeIVZZMOCSSH001Batch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeIVZZMOCSSH001Batch *Start...", uuid);
		commonService.startBatchLog("EAI", "CS", "executeIVZZMOCSSH001Batch", uuid);
		try {
			chatEaiService.setUuid(uuid);
			JSONObject executeResult = chatEaiService.executeIVZZMOCSSH001();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
			
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeIVZZMOCSSH001Batch *End...", uuid);
		Thread.sleep(1_000L);
	}
}
