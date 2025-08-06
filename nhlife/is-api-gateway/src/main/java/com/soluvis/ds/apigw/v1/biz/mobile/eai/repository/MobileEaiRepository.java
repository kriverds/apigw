package com.soluvis.ds.apigw.v1.biz.mobile.eai.repository;

import java.util.Map;

import org.springframework.stereotype.Repository;

import com.soluvis.ds.apigw.v1.biz.mobile.eai.mapper.MobileEaiMapper;

/**
 * @Class 		: MobileEaiRepository
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  MOBILE EAI Repository
 */
@Repository
public class MobileEaiRepository {
	
	/**
	 * 스프링 DI
	 */
	MobileEaiMapper mobileEaiMapper;
	public MobileEaiRepository(MobileEaiMapper mobileEaiMapper) {
		this.mobileEaiMapper = mobileEaiMapper;
	}
	
	/**
	 * @Method		: getIVZZMOZZSH001
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  모바일앱용 콜센터 현황 조회
	 */
	public Map<String,Object> getIVZZMOZZSH001(Map<String,Object> params) {
		return mobileEaiMapper.selectWaitCnt(params);
	}

}
