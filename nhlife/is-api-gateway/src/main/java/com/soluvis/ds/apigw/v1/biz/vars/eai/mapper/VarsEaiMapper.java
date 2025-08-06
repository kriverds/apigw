package com.soluvis.ds.apigw.v1.biz.vars.eai.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * @Class 		: VarsEaiMapper
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  VARS EAI Mapper
 */
@Mapper
public interface VarsEaiMapper {
	public Map<String,Object> selectWaitCnt(Map<String,Object> params);
}
