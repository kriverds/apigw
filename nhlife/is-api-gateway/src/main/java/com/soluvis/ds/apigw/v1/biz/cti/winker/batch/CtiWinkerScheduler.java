package com.soluvis.ds.apigw.v1.biz.cti.winker.batch;

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
import com.soluvis.ds.apigw.v1.biz.cti.winker.service.CtiWinkerService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Class 		: CtiWinkerScheduler
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 배치 시작 시 DB에 배치 로그 삽입
 *  2. 서비스 로직 처리
 *  3. 배치 종료 시 DB에 배치 로그 업데이트
 * 
 *  executeCheckCtiUserBatch: CTI 유저 생성 삭제 확인 | 0 0/5 * * * *
 */
@Component
public class CtiWinkerScheduler {
	
	static final Logger logger = LoggerFactory.getLogger(CtiWinkerScheduler.class);
	
	/**
	 * 스프링 DI
	 */
	CtiWinkerService ctiWinkerService;
	CommonService commonService;
	public CtiWinkerScheduler(CtiWinkerService ctiWinkerService, CommonService commonService) {
		this.ctiWinkerService = ctiWinkerService;
		this.commonService = commonService;
	}

	/**
	 * @Method		: executeCheckCtiUserBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CTI 사용자 생성/삭제 실행 배치
	 */
	@Async
	@Scheduled(cron = "${scheduler.winker.cti-id-check}")
	@SchedulerLock(name = "executeCheckCtiUserBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeCheckCtiUserBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeCheckCtiUserBatch *Start...", uuid);
		commonService.startBatchLog("winker", "CT", "executeCheckCtiUserBatch", uuid);
		try {
			ctiWinkerService.setUuid(uuid);
			JSONObject executeResult = ctiWinkerService.executeCheckCtiUser();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
			
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeCheckCtiUserBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
}
