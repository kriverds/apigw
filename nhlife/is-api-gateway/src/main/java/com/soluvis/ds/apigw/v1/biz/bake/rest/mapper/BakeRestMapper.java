package com.soluvis.ds.apigw.v1.biz.bake.rest.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * @Class 		: BakeRestMapper
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  BAKE REST Mapper
 */
@Mapper
public interface BakeRestMapper {
	public List<Map<String,Object>> selectCheckBakeUser();
	public int updateBakeCreated(Map<String,Object> params);
	public int updateBakeDeleted(Map<String,Object> params);
}
