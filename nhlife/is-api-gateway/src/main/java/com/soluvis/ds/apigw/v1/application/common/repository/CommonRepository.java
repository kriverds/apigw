package com.soluvis.ds.apigw.v1.application.common.repository;

import java.util.Map;

import org.springframework.stereotype.Repository;

import com.soluvis.ds.apigw.v1.application.common.mapper.CommonMapper;

/**
 * @Class 		: CommonRepository
 * @date   		: 2025. 3. 25.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  어플리케이션 공통기능 Repository
 */
@Repository
public class CommonRepository {
	
	/**
	 * 스프링 DI
	 */
	CommonMapper commonMapper;
	public CommonRepository(CommonMapper commonMapper) {
		this.commonMapper = commonMapper;
	}
	
	/**
	 * @Method		: startBatchLog
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  배치 시작 시 DB에 로그 적재
	 */
	public int startBatchLog(Map<String,Object> params) {
		return commonMapper.insertBatchLog(params);
	}
	
	/**
	 * @Method		: endBatchLog
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  배치 종료 시 DB 로그 업데이트
	 */
	public int endBatchLog(Map<String,Object> params) {
		return commonMapper.updateBatchLog(params);
	}

}
