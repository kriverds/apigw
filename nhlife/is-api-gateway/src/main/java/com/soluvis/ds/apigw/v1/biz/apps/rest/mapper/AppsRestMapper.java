package com.soluvis.ds.apigw.v1.biz.apps.rest.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.soluvis.ds.apigw.v1.biz.apps.rest.vo.SG;
import com.soluvis.ds.apigw.v1.biz.apps.rest.vo.SGUser;
import com.soluvis.ds.apigw.v1.biz.apps.rest.vo.UserSG;

/**
 * @Class 		: AppsRestMapper
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  APPS REST Mapper
 */
@Mapper
public interface AppsRestMapper {
	public List<UserSG> selectUserSkill(Map<String,Object> params);
	public int mergeAppUser(Map<String,Object> params);
	public int mergeUser(Map<String,Object> params);
	
	public List<SG> selectSkillList(Map<String,Object> params);
	public List<SGUser> selectSkillUserList(Map<String,Object> params);
	public List<SGUser> selectNotInSkillUserList(Map<String,Object> params);
	public int mergeAppOrg(Map<String,Object> params);
	public int ctiDupCheck(Map<String,Object> params);
}