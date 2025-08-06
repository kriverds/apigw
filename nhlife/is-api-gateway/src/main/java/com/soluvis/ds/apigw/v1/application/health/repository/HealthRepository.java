package com.soluvis.ds.apigw.v1.application.health.repository;

import org.springframework.stereotype.Repository;

import com.soluvis.ds.apigw.v1.application.health.mapper.HealthMapper;

/**
 * @Class 		: HealthRepository
 * @date   		: 2025. 2. 18.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 * 헬스체크 Repository
 */
@Repository
public class HealthRepository {

	/**
	 * 스프링 DI
	 */
	HealthMapper healthMapper;
	public HealthRepository(HealthMapper healthMapper) {
		this.healthMapper = healthMapper;
	}
	
	/**
	 * @Method		: selectDual
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  select 1 from dual
	 */
	public int selectDual() {
		return healthMapper.selectDual();
	}
}
