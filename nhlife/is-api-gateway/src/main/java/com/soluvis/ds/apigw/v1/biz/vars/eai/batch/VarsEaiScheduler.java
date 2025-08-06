package com.soluvis.ds.apigw.v1.biz.vars.eai.batch;

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
import com.soluvis.ds.apigw.v1.biz.vars.eai.service.VarsEaiService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Class 		: VarsEaiScheduler
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 배치 시작 시 DB에 배치 로그 삽입
 *  2. 서비스 로직 처리
 *  3. 배치 종료 시 DB에 배치 로그 업데이트
 * 
 *  executeIVZZMOARSH001Batch: 보이는ARS용 콜센터 현황 조회 | 0/5 * * * * *
 */
@Component
public class VarsEaiScheduler {
	
	static final Logger logger = LoggerFactory.getLogger(VarsEaiScheduler.class);
	
	/**
	 * 스프링 DI
	 */
	VarsEaiService varsEaiService;
	CommonService commonService;
	public VarsEaiScheduler(VarsEaiService varsEaiService, CommonService commonService) {
		this.varsEaiService = varsEaiService;
		this.commonService = commonService;
	}

	/**
	 * @Method		: executeIVZZMOARSH001Batch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  보이는ARS_콜센터현황조회
	 */
	@Async
	@Scheduled(cron = "${scheduler.eai.vars}")
	@SchedulerLock(name = "executeIVZZMOARSH001Batch", lockAtLeastFor = "4s", lockAtMostFor = "4s")
	public void executeIVZZMOARSH001Batch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeIVZZMOARSH001Batch *Start...", uuid);
//		commonService.startBatchLog("EAI", "CS", "executeIVZZMOARSH001Batch", uuid);
		try {
			varsEaiService.setUuid(uuid);
			JSONObject executeResult = varsEaiService.executeIVZZMOARSH001();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			logger.info("[{}] resultCd[{}] resultMsg[{}]", uuid, resultCd, resultMsg);
//			commonService.endBatchLog(resultCd, resultMsg, uuid);
			
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.startBatchLog("EAI", "CS", "executeIVZZMOARSH001Batch", uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeIVZZMOARSH001Batch *End...", uuid);
		Thread.sleep(1_000L);
	}
}
