package com.soluvis.ds.apigw.v1.biz.apps.eai.repository;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.soluvis.ds.apigw.v1.biz.apps.eai.mapper.AppsEaiMapper;

/**
 * @Class 		: AppsEaiRepository
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  APPS EAI Repository
 */
@Repository
public class AppsEaiRepository {
	
	static final Logger logger = LoggerFactory.getLogger(AppsEaiRepository.class);
	
	/**
	 * 스프링 DI
	 */
	AppsEaiMapper appsEaiMapper;
	public AppsEaiRepository(AppsEaiMapper appsEaiMapper) {
		this.appsEaiMapper = appsEaiMapper;
	}
	
	/**
	 * @Method		: setEaiTmBizData
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  TM 청약진행 현황 적재
	 * 
	 *  1. Temp 테이블에 데이터 Insert
	 *  2. Temp 테이블에서 데이터 Merge
	 */
	@Transactional
	public int setEaiTmBizData(Map<String,Object> params) {
		int insertCnt = appsEaiMapper.insertEaiTmBizDataTemp(params);
		int mergeCnt = appsEaiMapper.mergeEaiTmBizData();
		logger.info("setEaiTmBizData insertCnt[{}] mergeCnt[{}] ", insertCnt, mergeCnt);
		return mergeCnt;
	}
	
	/**
	 * @Method		: getBrCdList
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  사무소 코드 조회
	 */
	public List<Map<String,Object>> getBrCdList(){
		return appsEaiMapper.getBrCdList();
	}
	
	/**
	 * @Method		: aggSumAppTm
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  TM 집계 프로시저 호출
	 * 
	 *  1.p_set_sum_apptmagent_time
	 *  2.p_set_sum_apptmagent_day
	 *  3.p_set_sum_apptmteam_day
	 *  4.p_set_sum_apptmteam_month
	 */
	public void aggSumAppTm(Map<String,Object> params) {
		int result1 = appsEaiMapper.callSumAppTmAgentTime(params);
		int result2 = appsEaiMapper.callSumAppTmAgentDay(params);
		int result3 = appsEaiMapper.callSumAppTmTeamDay(params);
		int result4 = appsEaiMapper.callSumAppTmTeamMonth(params);
		logger.info("agentTime[{}] agentDay[{}] teamDay[{}] teamMonth[{}]", result1, result2, result3, result4);
	}

}
