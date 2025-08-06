package com.soluvis.ds.apigw.v1.application.common.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * @Class 		: CommonMapper
 * @date   		: 2025. 3. 25.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  어플리케이션 공통기능 Mapper
 */
@Mapper
public interface CommonMapper {
	public int insertBatchLog(Map<String,Object> params);
	public int updateBatchLog(Map<String,Object> params);
	
}
