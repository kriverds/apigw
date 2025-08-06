package com.soluvis.ds.apigw.v1.biz.cti.winker.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * @Class 		: CtiWinkerMapper
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CTI WINKER Mapper
 */
@Mapper
public interface CtiWinkerMapper {
	public List<Map<String,Object>> selectCheckCtiUser();
	public int updateCtiCreated(Map<String,Object> params);
	public int updateCtiDeleted(Map<String,Object> params);
	
	public List<Map<String,Object>> selectUsersHaveSkill(Map<String,Object> params);
	public List<Map<String,Object>> selectUserSkills(Map<String,Object> params);
	
	public List<Map<String,Object>> selectPersonDbid(Map<String,Object> params);
	
	public String selectDummySkillId();
	
	
}
