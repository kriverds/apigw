package com.soluvis.ds.apigw.v1.biz.apps.eai.batch;

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
import com.soluvis.ds.apigw.v1.biz.apps.eai.service.AppsEaiService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Class 		: AppsEaiScheduler
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 배치 시작 시 DB에 배치 로그 삽입
 *  2. 서비스 로직 처리
 *  3. 배치 종료 시 DB에 배치 로그 업데이트
 * 
 *  executeIVZZMANBSH003Batch: 				청약진행 현황조회 		| 0 0/5 * * * *
 *  executeIVZZMANBSH003BatchBefore15Day: 	청약진행 현황조회(재집계)	| 0 2/30 * * * *
 * 
 *  EAI는 TM만 존재 CS는 없음
 */
@Component
public class AppsEaiScheduler {
	
	static final Logger logger = LoggerFactory.getLogger(AppsEaiScheduler.class);
	
	/**
	 * 스프링 DI
	 */
	AppsEaiService appsEaiService;
	CommonService commonService;
	public AppsEaiScheduler(AppsEaiService appsEaiService, CommonService commonService) {
		this.appsEaiService = appsEaiService;
		this.commonService = commonService;
	}

	/**
	 * @Method		: executeIVZZMANBSH003Batch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  청약진행현황조회(실적)
	 */
	@Async
	@Scheduled(cron = "${scheduler.eai.tm.normal}")
	@SchedulerLock(name = "executeIVZZMANBSH003Batch")
	public void executeIVZZMANBSH003Batch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeIVZZMANBSH003Batch *Start...", uuid);
		commonService.startBatchLog("EAI", "TM", "executeIVZZMANBSH003Batch", uuid);
		try {
			appsEaiService.setUuid(uuid);
			JSONObject executeResult = appsEaiService.executeIVZZMANBSH003();
			logger.info("{}", executeResult);
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
			
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeIVZZMANBSH003Batch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeIVZZMANBSH003BatchBefore15Day
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  청약진행현황조회(실적) 15일전~1일전까지 재조회
	 */
	@Async
	@Scheduled(cron = "${scheduler.eai.tm.before-15day}")
	@SchedulerLock(name = "executeIVZZMANBSH003BatchBefore15Day")
	public void executeIVZZMANBSH003BatchBefore15Day() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeIVZZMANBSH003BatchBefore15Day *Start...", uuid);
		commonService.startBatchLog("EAI", "TM", "executeIVZZMANBSH003BatchBefore15Day", uuid);
		try {
			appsEaiService.setUuid(uuid);
			JSONObject executeResult = appsEaiService.executeIVZZMANBSH003Before15day();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeIVZZMANBSH003BatchBefore15Day *End...", uuid);
		Thread.sleep(1_000L);
	}
}
