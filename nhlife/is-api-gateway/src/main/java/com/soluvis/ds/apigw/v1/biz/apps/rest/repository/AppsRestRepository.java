package com.soluvis.ds.apigw.v1.biz.apps.rest.repository;

import java.util.Map;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.soluvis.ds.apigw.v1.biz.apps.rest.mapper.AppsRestMapper;
import com.soluvis.ds.apigw.v1.util.ObjectMapperUtil;

/**
 * @Class 		: AppsRestRepository
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  APPS REST Repository
 */
@Repository
public class AppsRestRepository {
	
	static final Logger logger = LoggerFactory.getLogger(AppsRestRepository.class);
	/**
	 * 스프링 DI
	 */
	AppsRestMapper appsRestMapper;
	public AppsRestRepository(AppsRestMapper appsRestMapper){
		this.appsRestMapper = appsRestMapper;
	}
	
	/**
	 * @Method		: ifIs2001
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getUserSGList: 사용자 스킬그룹 정보 조회
	 */
	public JSONArray ifIs2001(Map<String,Object> params) throws Exception {
		return ObjectMapperUtil.listVoToJsonArray(appsRestMapper.selectUserSkill(params));
	}
	
	/**
	 * @Method		: ifIs2002
	 * @date   		: 2025. 2. 27.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. t_appuser 데이터 Merge
	 *  2. t_user 데이터 Merge
	 */
	public int ifIs2002(Map<String,Object> params) throws Exception {
		int iResult1 = appsRestMapper.mergeAppUser(params);
		int iResult2 = appsRestMapper.mergeUser(params);
		return Math.min(iResult1, iResult2);
	}
	/**
	 * @Method		: ifIs2004
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getSGList: 스킬그룹 리스트 조회
	 *  VO의 JsonProperty으로 리턴파라미터 설정
	 */
	public JSONArray ifIs2004(Map<String,Object> params) throws Exception {
		return ObjectMapperUtil.listVoToJsonArray(appsRestMapper.selectSkillList(params));
	}
	/**
	 * @Method		: ifIs2005
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getSGUserList: 스킬그룹 별 사용자 리스트 조회
	 *  VO의 JsonProperty으로 리턴파라미터 설정
	 */
	public JSONArray ifIs2005(Map<String,Object> params) throws Exception {
		return ObjectMapperUtil.listVoToJsonArray(appsRestMapper.selectSkillUserList(params));
	}
	/**
	 * @Method		: ifIs2006
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getNotInSGUserList: 스킬그룹 별 제외 사용자 리스트 조회
	 *  VO의 JsonProperty으로 리턴파라미터 설정
	 */
	public JSONArray ifIs2006(Map<String,Object> params) throws Exception {
		return ObjectMapperUtil.listVoToJsonArray(appsRestMapper.selectNotInSkillUserList(params));
	}
	
	/**
	 * @Method		: ifIs2008
	 * @date   		: 2025. 2. 27.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. t_apporg 데이터 Merge
	 */
	public int ifIs2008(Map<String,Object> params) throws Exception {
		return appsRestMapper.mergeAppOrg(params);
	}
	/**
	 * @Method		: ifIs2009
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getUserDupCheck: 사용자 중복체크
	 */
	public int ifIs2009(Map<String,Object> params) throws Exception {
		return appsRestMapper.ctiDupCheck(params);
	}
}
