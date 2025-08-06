package com.soluvis.ds.apigw.v1.biz.vars.eai.repository;

import java.util.Map;

import org.springframework.stereotype.Repository;

import com.soluvis.ds.apigw.v1.biz.vars.eai.mapper.VarsEaiMapper;

/**
 * @Class 		: VarsEaiRepository
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  VARS EAI Repository
 */
@Repository
public class VarsEaiRepository {
	
	/**
	 * 스프링 DI
	 */
	VarsEaiMapper varsEaiMapper;
	public VarsEaiRepository(VarsEaiMapper varsEaiMapper) {
		this.varsEaiMapper = varsEaiMapper;
	}
	
	/**
	 * @Method		: getIVZZMOARSH001
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  보이는ARS용 콜센터 현황 조회
	 */
	public Map<String,Object> getIVZZMOARSH001(Map<String,Object> params) {
		return varsEaiMapper.selectWaitCnt(params);
	}

}
