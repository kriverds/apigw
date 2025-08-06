package com.soluvis.ds.apigw.v1.application.health.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Class 		: HealthMapper
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  헬스체크 Mapper
 */
@Mapper
public interface HealthMapper {
	@Select("SELECT 1 FROM dual")
	public int selectDual();
}
