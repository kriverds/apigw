package com.soluvis.ds.apigw.v1.biz.chatbot.nas.batch;

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
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.service.ChatbotNasReadService;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.service.ChatbotNasWriteService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Class 		: ChatbotNasScheduler
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 배치 시작 시 DB에 배치 로그 삽입
 *  2. 서비스 로직 처리
 *  3. 배치 종료 시 DB에 배치 로그 업데이트
 * 
 *  executeCbStatsInfoBatch: 	챗봇 상담현황 		| 0 0 1 * * *
 *  executeCmStatsInfoBatch: 	챗봇 캠페인결과 	| 0 0/5 * * * *
 *  executeHolInfoBatch: 		챗봇 휴일정보 		| 0 0 3 * * *
 *  executeSendSmsResultBatch: 	챗봇 알림톡 전송결과 	| 0 1/10 * * * *
 */
@Component
public class ChatbotNasScheduler {
	
	static final Logger logger = LoggerFactory.getLogger(ChatbotNasScheduler.class);
	
	/**
	 * 스프링 DI
	 */
	ChatbotNasReadService chatbotNasReadService;
	ChatbotNasWriteService chatbotNasWriteService;
	CommonService commonService;
	public ChatbotNasScheduler(ChatbotNasReadService chatbotNasReadService, ChatbotNasWriteService chatbotNasWriteService, CommonService commonService) {
		this.chatbotNasReadService = chatbotNasReadService;
		this.chatbotNasWriteService = chatbotNasWriteService;
		this.commonService = commonService;
	}

	/**
	 * @Method		: executeCbStatsInfoBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 상담현황 조회 배치 실행
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.chatbot.cb-stats-info}")
	@SchedulerLock(name = "executeCbStatsInfoBatch")
	public void executeCbStatsInfoBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeCbStatsInfoBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeCbStatsInfoBatch", uuid);
		try {
			chatbotNasReadService.setUuid(uuid);
			JSONObject executeResult = chatbotNasReadService.executeCbStatsInfo();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeCbStatsInfoBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeCmStatsInfoBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 캠페인결과 조회 배치 실행
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.chatbot.cm-stats-info}")
	@SchedulerLock(name = "executeCmStatsInfoBatch")
	public void executeCmStatsInfoBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeCmStatsInfoBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeCmStatsInfoBatch", uuid);
		try {
			chatbotNasReadService.setUuid(uuid);
			JSONObject executeResult = chatbotNasReadService.executeCmStatsInfo();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeCmStatsInfoBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeCmStatsInfoDayBatch
	 * @date   		: 2025. 4. 10.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 캠페인결과 전일 재집계
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.chatbot.cm-stats-info-day}")
	@SchedulerLock(name = "executeCmStatsInfoDayBatch")
	public void executeCmStatsInfoDayBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeCmStatsInfoDayBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeCmStatsInfoDayBatch", uuid);
		try {
			chatbotNasReadService.setUuid(uuid);
			JSONObject executeResult = chatbotNasReadService.executeCmStatsInfoDay();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeCmStatsInfoDayBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeHolInfoBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 휴일 적재 배치 실행
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.chatbot.hol-info}")
	@SchedulerLock(name = "executeHolInfoBatch")
	public void executeHolInfoBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeHolInfoBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeHolInfoBatch", uuid);
		try {
			chatbotNasReadService.setUuid(uuid);
			JSONObject executeResult = chatbotNasWriteService.executeHolInfo();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeHolInfoBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeSendSmsResultBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  챗봇 알림톡결과 조회 배치 실행
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.chatbot.sms-send-result}")
	@SchedulerLock(name = "executeSmsSendResultBatch")
	public void executeSmsSendResultBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeSmsSendResultBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeSmsSendResultBatch", uuid);
		try {
			chatbotNasReadService.setUuid(uuid);
			JSONObject executeResult = chatbotNasReadService.executeSmsSendResult();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeSmsSendResultBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
}
