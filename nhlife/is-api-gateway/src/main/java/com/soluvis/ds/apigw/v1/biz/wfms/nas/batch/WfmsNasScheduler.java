package com.soluvis.ds.apigw.v1.biz.wfms.nas.batch;

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
import com.soluvis.ds.apigw.v1.biz.wfms.nas.service.WfmsNasReadService;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.service.WfmsNasWriteService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Class 		: WfmsNasScheduler
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 배치 시작 시 DB에 배치 로그 삽입
 *  2. 서비스 로직 처리
 *  3. 배치 종료 시 DB에 배치 로그 업데이트
 * 
 *  executeCdInfBatch: 				예측그룹 코드 			| 0 10 0 * * *
 *  executeGroupLimitBizKindBatch: 	그룹별 정원정보 		| 0 10 3 * * *
 *  executeSumAgent30minBatch: 		30분단위 상담사 콜정보 	| 0 5/30 * * * *
 *  executeSumAgentDayBatch: 		상담사실적정보 			| 0 30 0 * * *
 *  executeSumChatGroupDayBatch: 	채팅집계정보 			| 0 20 0 * * *
 *  executeSumHo30minBatch: 		30분단위 CTIQ별 콜정보	| 0 2/30 * * * *
 *  executeSumMrpMonthBatch: 		월별 대표번호 응대현황 	| 0 0 5 * * *
 *  executeSumReason30minBatch: 	30분단위 상담사 상태정보 	| 0 8/30 * * * *
 *  executeUserEvltBatch: 			평가대상자 업데이트 		| 0 20 3 * * *
 */
@Component
public class WfmsNasScheduler {
	
	static final Logger logger = LoggerFactory.getLogger(WfmsNasScheduler.class);
	
	/**
	 * 스프링 DI
	 */
	WfmsNasReadService wfmsNasReadService;
	WfmsNasWriteService wfmsNasWriteService;
	CommonService commonService;
	public WfmsNasScheduler(WfmsNasReadService wfmsNasReadService, WfmsNasWriteService wfmsNasWriteService, CommonService commonService) {
		this.wfmsNasReadService = wfmsNasReadService;
		this.wfmsNasWriteService = wfmsNasWriteService;
		this.commonService = commonService;
	}
	
	/**
	 * @Method		: executeCdInfBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  예측그룹_코드
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.cdinf}")
	@SchedulerLock(name = "executeCdInfBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeCdInfBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeCdInfBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeCdInfBatch", uuid);
		try {
			wfmsNasWriteService.setUuid(uuid);
			JSONObject executeResult = wfmsNasWriteService.executeCdInf();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeCdInfBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeGroupLimitBizKindBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  그룹별_정원정보
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.group-limit-biz-kind}")
	@SchedulerLock(name = "executeGroupLimitBizKindBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeGroupLimitBizKindBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeGroupLimitBizKindBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeGroupLimitBizKindBatch", uuid);
		try {
			wfmsNasReadService.setUuid(uuid);
			JSONObject executeResult = wfmsNasReadService.executeGroupLimitBizKind();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeGroupLimitBizKindBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeSumAgent30minBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  30분단위_상담사_콜정보
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.sum-agent-30min}")
	@SchedulerLock(name = "executeSumAgent30minBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeSumAgent30minBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeSumAgent30minBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeSumAgent30minBatch", uuid);
		try {
			wfmsNasWriteService.setUuid(uuid);
			JSONObject executeResult = wfmsNasWriteService.executeSumAgent30min();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeSumAgent30minBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeSumAgentDayBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  상담사실적정보
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.sum-agent-day}")
	@SchedulerLock(name = "executeSumAgentDayBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeSumAgentDayBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeSumAgentDayBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeSumAgentDayBatch", uuid);
		try {
			wfmsNasWriteService.setUuid(uuid);
			JSONObject executeResult = wfmsNasWriteService.executeSumAgentDay();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeSumAgentDayBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeSumChatGroupDayBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  채팅집계정보
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.sum-chat-group-day}")
	@SchedulerLock(name = "executeSumChatGroupDayBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeSumChatGroupDayBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeSumChatGroupDayBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeSumChatGroupDayBatch", uuid);
		try {
			wfmsNasWriteService.setUuid(uuid);
			JSONObject executeResult = wfmsNasWriteService.executeSumChatGroupDay();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeSumChatGroupDayBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeSumHo30minBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  30분단위_CTIQ별_콜정보
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.sum-ho-30min}")
	@SchedulerLock(name = "executeSumHo30minBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeSumHo30minBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeSumHo30minBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeSumHo30minBatch", uuid);
		try {
			wfmsNasWriteService.setUuid(uuid);
			JSONObject executeResult = wfmsNasWriteService.executeSumHo30min();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeSumHo30minBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeSumMrpMonthBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  월별_대표번호_응대현황
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.sum-mrp-month}")
	@SchedulerLock(name = "executeSumMrpMonthBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeSumMrpMonthBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeSumMrpMonthBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeSumMrpMonthBatch", uuid);
		try {
			wfmsNasWriteService.setUuid(uuid);
			JSONObject executeResult = wfmsNasWriteService.executeSumMrpMonth();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeSumMrpMonthBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeSumReason30minBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  30분단위_상담사_상태정보
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.sum-reason-30min}")
	@SchedulerLock(name = "executeSumReason30minBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeSumReason30minBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeSumReason30minBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeSumReason30minBatch", uuid);
		try {
			wfmsNasWriteService.setUuid(uuid);
			JSONObject executeResult = wfmsNasWriteService.executeSumReason30min();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeSumReason30minBatch *End...", uuid);
		Thread.sleep(1_000L);
	}
	
	/**
	 * @Method		: executeUserEvltBatch
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  평가대상자_업데이트
	 */
	@Async
	@Scheduled(cron = "${scheduler.nas.wfms.user-evlt}")
	@SchedulerLock(name = "executeUserEvltBatch", lockAtLeastFor = "10s", lockAtMostFor = "10s")
	public void executeUserEvltBatch() throws Exception {
		Calendar sCal = Calendar.getInstance();
		UUID uuid = UUID.randomUUID();
		logger.info("[{}] executeUserEvltBatch *Start...", uuid);
		commonService.startBatchLog("NAS", "CS", "executeUserEvltBatch", uuid);
		try {
			wfmsNasReadService.setUuid(uuid);
			JSONObject executeResult = wfmsNasReadService.executeUserEvlt();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			String resultMsg = executeResult.get(Const.APIGW_KEY_RESULT_MSG).toString();
			commonService.endBatchLog(resultCd, resultMsg, uuid);
		} catch (Exception e) {
			Map<String, Object> jException = CommonUtil.commonException(e, uuid);
			commonService.endBatchLog("N", jException.toString(), uuid);
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] executeUserEvltBatch *End...", uuid);
		Thread.sleep(1_000L);
	}

}
