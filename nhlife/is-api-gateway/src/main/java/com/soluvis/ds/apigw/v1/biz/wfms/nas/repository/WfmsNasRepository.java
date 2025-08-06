package com.soluvis.ds.apigw.v1.biz.wfms.nas.repository;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.soluvis.ds.apigw.v1.biz.wfms.nas.mapper.WfmsNasMapper;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.CdInf;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.GroupLimitBizKind;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.SumChatGroupDay;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.UserEvlt;

/**
 * @Class 		: WfmsNasRepository
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  WFMS NAS Repository
 */
@Repository
public class WfmsNasRepository {
	
	static final Logger logger = LoggerFactory.getLogger(WfmsNasRepository.class);
	
	/**
	 * 스프링 DI
	 */
	WfmsNasMapper wfmsNasMapper;
	public WfmsNasRepository(WfmsNasMapper wfmsNasMapper) {
		this.wfmsNasMapper = wfmsNasMapper;
	}
	
	/**
	 * @Method		: getCdInf
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  예측그룹 코드
	 */
	public List<CdInf> getCdInf(){
		return wfmsNasMapper.selectCdInf();
	}
	/**
	 * @Method		: setGroupLimitBizKind
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  그룹별 정원정보
	 * 
	 *  1. Temp 테이블에 데이터 Insert
	 *  2. Temp 테이블에서 데이터 Merge
	 */
	@Transactional
	public int setGroupLimitBizKind(List<GroupLimitBizKind> voList){
		int insertCnt = wfmsNasMapper.insertGroupLimitBizKindTemp(voList);
		int mergeCnt = wfmsNasMapper.mergeGroupLimitBizKind();
		logger.info("setGroupLimitBizKind insertCnt[{}] mergeCnt[{}] ", insertCnt, mergeCnt);
		return mergeCnt;
	}
	/**
	 * @Method		: getSumAgent30min
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  30분단위 상담사 콜정보
	 */
	public void getSumAgent30min(Map<String,Object> params) {
		wfmsNasMapper.getSumAgent30min(params);
	}
	/**
	 * @Method		: getSumAgentDay
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  상담사실적정보
	 */
	public void getSumAgentDay(Map<String,Object> params) {
		wfmsNasMapper.getSumAgentDay(params);
	}
	/**
	 * @Method		: getSumChatGroupDay
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  채팅집계정보
	 */
	public List<SumChatGroupDay> getSumChatGroupDay(Map<String,Object> params) {
		return wfmsNasMapper.selectSumChatGroupDay(params);
	}
	/**
	 * @Method		: getSumHo30min
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  30분단위 CTIQ별 콜정보
	 */
	public void getSumHo30min(Map<String,Object> params) {
		wfmsNasMapper.getSumHo30min(params);
	}
	/**
	 * @Method		: getSumMrpMonth
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  월별 대표번호 응대현황
	 */
	public void getSumMrpMonth(Map<String,Object> params) {
		wfmsNasMapper.getSumMrpMonth(params);
	}
	/**
	 * @Method		: getSumReason30min
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  30분단위 상담사 상태정보
	 */
	public void getSumReason30min(Map<String,Object> params) {
		wfmsNasMapper.getSumReason30min(params);
	}
	/**
	 * @Method		: setUserEvlt
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  평가대상자 업데이트
	 * 
	 *  1. Temp 테이블에 데이터 Insert
	 *  2. Temp 테이블에서 데이터 Update
	 */
	@Transactional
	public int setUserEvlt(List<UserEvlt> voList){
		int insertCnt = wfmsNasMapper.insertUserEvltTemp(voList);
		int mergeCnt = wfmsNasMapper.updateUserEvlt();
		logger.info("setUserEvlt insertCnt[{}] mergeCnt[{}]", insertCnt, mergeCnt);
		return mergeCnt;
	}

}