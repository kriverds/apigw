package com.soluvis.ds.apigw.v1.biz.apps.rest.service;

import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.apps.rest.repository.AppsRestRepository;
import com.soluvis.ds.apigw.v1.biz.bake.rest.service.BakeRestService;
import com.soluvis.ds.apigw.v1.biz.cti.winker.service.CtiWinkerService;

/**
 * @Class 		: AppsRestService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 * APPS Rest Service
 */
@Service
public class AppsRestService {

	static final Logger logger = LoggerFactory.getLogger(AppsRestService.class);
	
	/**
	 * 스프링 DI
	 */
	AppsRestRepository appsRestRepository;
	CtiWinkerService ctiWinkerService;
	BakeRestService bakeRestService;
	public AppsRestService(AppsRestRepository appsRestRepository, CtiWinkerService ctiWinkerService, BakeRestService bakeRestService) {
        this.appsRestRepository = appsRestRepository;
        this.ctiWinkerService = ctiWinkerService;
        this.bakeRestService = bakeRestService;
    }


	/**
	 * @Method		: ifIs2001
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getUserSGList: 사용자 스킬그룹 정보 조회
	 */
	public JSONObject ifIs2001(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		
		JSONArray list = appsRestRepository.ifIs2001(params);
		
		jResult.put("cnt", list.length());
		jResult.put("UserSGList", list);
		jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
		jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2002
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  setUser: 사용자 정보 저장
	 */
	public JSONObject ifIs2002(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		
		int iResult = appsRestRepository.ifIs2002(params);
		
		jResult.put("cnt", iResult);
		
		if(iResult > 0) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "통합통계-처리 결과가 없습니다");
		}
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2003
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  initPassword: 비밀번호 초기화
	 * 
	 *  1. Bake의 REST서비스 호출
	 */
	public JSONObject ifIs2003(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		String userid = params.get("UserId").toString();
		boolean bResult = bakeRestService.initPassword(userid);
		
		if(bResult) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "비밀번호 초기화 실패");
		}
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2004
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getSGList: 스킬그룹 리스트 조회
	 */
	public JSONObject ifIs2004(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		
		JSONArray list = appsRestRepository.ifIs2004(params);
		
		jResult.put("cnt", list.length());
		jResult.put("SGList", list);
		jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
		jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2005
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getSGUserList: 스킬그룹 별 사용자 리스트 조회
	 */
	public JSONObject ifIs2005(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		
		JSONArray list = appsRestRepository.ifIs2005(params);
		
		jResult.put("cnt", list.length());
		jResult.put("SGUserList", list);
		jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
		jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2006
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getNotInSGUserList: 스킬그룹 별 제외 사용자 리스트 조회
	 */
	public JSONObject ifIs2006(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		JSONArray list = appsRestRepository.ifIs2006(params);
		
		jResult.put("cnt", list.length());
		jResult.put("SGUserList", list);
		jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
		jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2007
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  setSGUserList: 스킬그룹 별 사용자 리스트 저장
	 * 
	 *  1. CTI Winker 서비스 호출
	 */
	public JSONObject ifIs2007(Map<String,Object> params) throws Exception {
		String sysKind = params.get("SysKind").toString();
		String skill = params.get("SGCd").toString();
		String level = params.get("SGLevel").toString();
		String[] arrUser = params.get("UserId").toString().split(Pattern.quote("^"));
		String[] arrLevelSel = params.get("SGLevelSel").toString().split(Pattern.quote("^"));
		
		return ctiWinkerService.oneSkillToMultiUser(arrUser, skill, arrLevelSel, level, sysKind);
	}
	/**
	 * @Method		: ifIs2008
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  setOrgInfo: 조직정보 저장
	 */
	public JSONObject ifIs2008(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		
		int iResult = appsRestRepository.ifIs2008(params);
		
		jResult.put("cnt", iResult);
		if(iResult > 0) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "통합통계-처리 결과가 없습니다");
		}
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2009
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  getUserDupCheck: 사용자 중복체크
	 * 
	 *  조회결과가 존재할 시 에러 리턴
	 */
	public JSONObject ifIs2009(Map<String,Object> params) throws Exception {
		JSONObject jResult = new JSONObject();
		
		int iResult = appsRestRepository.ifIs2009(params);
		
		jResult.put("cnt", iResult);
		if(iResult > 0) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "통합통계-중복된 사용자 입니다");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "");
		}
		
		return jResult;
	}
	/**
	 * @Method		: ifIs2011
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  setUserSG: 사용자 스킬그룹 저장
	 * 
	 *  1. CTI Winker 서비스 호출
	 */
	public JSONObject ifIs2011(Map<String,Object> params) throws Exception {
		String sysKind = params.get("SysKind").toString();
		String[] skills = params.get("SGCd").toString().split(Pattern.quote("^"));
		String[] levels = params.get("SGLevel").toString().split(Pattern.quote("^"));
		String user = params.get("UserId").toString();
		
		return ctiWinkerService.multiSkillToOneUser(user, skills, levels, sysKind);
	}


}
