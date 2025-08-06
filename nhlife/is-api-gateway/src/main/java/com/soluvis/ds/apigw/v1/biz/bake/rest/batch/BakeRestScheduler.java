package com.soluvis.ds.apigw.v1.biz.bake.rest.batch;

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
import com.soluvis.ds.apigw.v1.biz.bake.rest.service.BakeRestService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Class 		: BakeRestScheduler
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 배치 시작 시 DB에 배치 로그 삽입
 *  2. 서비스 로직 처리
 *  3. 배치 종료 시 DB에 배치 로그 업데이트
 * 
 *  executeCheckBakeUserBatch: Bake 유저 생성 삭제 확인 | 0 0/5 * * * *
 */
@Component
public class BakeRestScheduler {
	
	static final Logger logger = LoggerFactory.getLogger(BakeRestScheduler.class);
	
	/**
	 * 스프링 DI
	 */
	BakeRestService bakeRestService;
	CommonService commonService;
	public BakeRestScheduler(BakeRestService bakeRestService, CommonService commonService) {
		this.bakeRestService = bakeRestService;
		this.commonService = commonService;
	}

	/**
	 * @Method		: executeCheckBakeUserBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Bake 생성/삭제 체크 배치 실행
	 */
	@Async
	@Scheduled(cron = "${scheduler.rest.bake-id-check}")
	@SchedulerLock(name = "executeCheckBakeUserBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeCheckBakeUserBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeCheckBakeUserBatch *Start...", uuid);
		commonService.startBatchLog("http", "CT", "executeCheckBakeUserBatch", uuid);
		try {
			bakeRestService.setUuid(uuid);
			JSONObject executeResult = bakeRestService.executeCheckBakeUser();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
			
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeCheckBakeUserBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
}
