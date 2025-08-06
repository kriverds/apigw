package com.soluvis.ds.apigw.v1.biz.bake.rest.repository;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.soluvis.ds.apigw.v1.biz.bake.rest.mapper.BakeRestMapper;

/**
 * @Class 		: BakeRestRepository
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  BAKE REST Repository
 */
@Repository
public class BakeRestRepository {
	
	/**
	 * 스프링 DI
	 */
	BakeRestMapper bakeRestMapper;
	public BakeRestRepository(BakeRestMapper bakeRestMapper) {
		this.bakeRestMapper = bakeRestMapper;
	}
	
	/**
	 * @Method		: checkBakeUser
	 * @date   		: 2025. 2. 27.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Bake 계정 체크
	 */
	public List<Map<String,Object>> checkBakeUser(){
		return bakeRestMapper.selectCheckBakeUser();
	}
	
	/**
	 * @Method		: setBakeCreated
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  bakeCreateCd Y 업데이트
	 */
	public int setBakeCreated(Map<String,Object> params){
		return bakeRestMapper.updateBakeCreated(params);
	}
	
	/**
	 * @Method		: setBakeDeleted
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  bakeCreateCd N 업데이트
	 */
	public int setBakeDeleted(Map<String,Object> params){
		return bakeRestMapper.updateBakeDeleted(params);
	}
}
