package com.soluvis.ds.apigw.v1.biz.mobile.eai.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * @Class 		: MobileEaiMapper
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  MOBILE EAI Mapper
 */
@Mapper
public interface MobileEaiMapper {
	public Map<String,Object> selectWaitCnt(Map<String,Object> params);
}
