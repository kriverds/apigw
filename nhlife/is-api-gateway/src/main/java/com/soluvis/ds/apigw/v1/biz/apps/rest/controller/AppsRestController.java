package com.soluvis.ds.apigw.v1.biz.apps.rest.controller;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.soluvis.ds.apigw.v1.application.common.service.CommonService;
import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.apps.rest.service.AppsRestService;
import com.soluvis.ds.apigw.v1.biz.apps.rest.util.RestUtil;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Class 		: AppsRestController
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IS-IF-2001 > getUserSGList
 *  IS-IF-2002 > setUser
 *  IS-IF-2003 > initPassword
 *  IS-IF-2004 > getSGList
 *  IS-IF-2005 > getSGUserList
 *  IS-IF-2006 > getNotInSGUserList
 *  IS-IF-2007 > setSGUserList
 *  IS-IF-2008 > setOrgInfo
 *  IS-IF-2009 > getUserDupCheck
 *  IS-IF-2011 > setUserSG
 * 
 *  1. API 호출 시 시작 로그 삽입
 *  2. service로 로직 분리 처리
 *  3. API 종료 시 종료 로그 업데이트
 */
@RestController
@RequestMapping(value = "/api/v1/apps")
public class AppsRestController {

	static final Logger logger = LoggerFactory.getLogger(AppsRestController.class);

	/**
	 * 스프링 DI
	 */
	AppsRestService appsRestService;
	CommonService commonService;
	public AppsRestController(AppsRestService appsRestService, CommonService commonService) {
		this.appsRestService = appsRestService;
		this.commonService = commonService;
	}

	UUID uuid;
	/**
	 * @Method		: getApiService
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. Get 요청을 받음 (Post로만 API 제공중)
	 *  2. 각 서비스별로 메서드를 분배
	 *  3. 결과 Response
	 */
//	@GetMapping(value = "/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String getApiService(HttpServletRequest request, @PathVariable String service,
			@RequestParam Map<String, Object> params) throws Exception {
		Calendar sCal = Calendar.getInstance();
		logger.info("[{}] {}", uuid, request.getContextPath());
		request.setAttribute("uuid", uuid);
		
		String syskind = params.get("SysKind")==null?"CS":params.get("SysKind").toString();
		
		commonService.startBatchLog("http", syskind, service, uuid);
		
		JSONObject result = new JSONObject();

		JSONObject rService;
		switch (service) {
			case "getUserSGList" 		-> rService = getUserSGList(params);
			case "setUser" 				-> rService = setUser(params);
			case "initPassword" 		-> rService = initPassword(params);
			case "getSGList" 			-> rService = getSGList(params);
			case "getSGUserList" 		-> rService = getSGUserList(params);
			case "getNotInSGUserList" 	-> rService = getNotInSGUserList(params);
			case "setSGUserList" 		-> rService = setSGUserList(params);
			case "setOrgInfo" 			-> rService = setOrgInfo(params);
			case "getUserDupCheck" 		-> rService = getUserDupCheck(params);
			case "setUserSG" 			-> rService = setUserSG(params);
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				commonService.endBatchLog("N", "404"+"|"+params.toString(), uuid);
				return result.toString();
			}
		}
		result = rService;
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		String batchStatus = "";
		if("0".equals(result.getString(Const.HTTP_ERROR_CD_KEY)) || "1".equals(result.getString(Const.HTTP_ERROR_CD_KEY))) {
			batchStatus = "Y";
		} else {
			batchStatus = "N";
		}
		commonService.endBatchLog(batchStatus, result.getString(Const.HTTP_ERROR_MSG_KEY)+"|"+params.toString(), uuid);
		logger.info("[{}] {}", uuid, result);
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		return result.toString();
	}
	
	/**
	 * @Method		: postApiService
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. Post 요청을 getApiService로 넘김.
	 */
	@PostMapping(value = "/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postApiService(HttpServletRequest request, @PathVariable String service,
			@RequestParam Map<String, Object> params) throws Exception {
		uuid = UUID.randomUUID();
		logger.info("[{}] POST Apps I/F Controller Service[{}] Params[{}]", uuid, service, params);
		return getApiService(request, service, params);
	}
	/**
	 * @Method		: getUserSGList
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  사용자 스킬그룹 정보 조회
	 */
	JSONObject getUserSGList(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("UserId")) {
			return RestUtil.returnNoParameter("UserId");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2001(params);
	}
	
	/**
	 * @Method		: setUser
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  사용자 정보 저장
	 */
	JSONObject setUser(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("UserId")) {
			return RestUtil.returnNoParameter("UserId");
		}
		if(!params.containsKey("KORN_NM")) {
			return RestUtil.returnNoParameter("KORN_NM");
		}
		if(!params.containsKey("ENG_NM")) {
			return RestUtil.returnNoParameter("ENG_NM");
		}
		if(!params.containsKey("Level1Cd")) {
			return RestUtil.returnNoParameter("Level1Cd");
		}
		if(!params.containsKey("Level2Cd")) {
			return RestUtil.returnNoParameter("Level2Cd");
		}
		if(!params.containsKey("Level3Cd")) {
			return RestUtil.returnNoParameter("Level3Cd");
		}
		if(!params.containsKey("GRP_ATHT")) {
			return RestUtil.returnNoParameter("GRP_ATHT");
		}
		if(!params.containsKey("ORG_GRD")) {
			return RestUtil.returnNoParameter("ORG_GRD");
		}
		if(!params.containsKey("BIZ_CLAS_CD")) {
			return RestUtil.returnNoParameter("BIZ_CLAS_CD");
		}
		if(!params.containsKey("ETCO_DT")) {
			return RestUtil.returnNoParameter("ETCO_DT");
		}
		if(!params.containsKey("LVCO_DT")) {
			return RestUtil.returnNoParameter("LVCO_DT");
		}
		if(!params.containsKey("EXT_NO")) {
			return RestUtil.returnNoParameter("EXT_NO");
		}
		if(!params.containsKey("CTI_LGIN_ID")) {
			return RestUtil.returnNoParameter("CTI_LGIN_ID");
		}
		if(!params.containsKey("CTI_USE_YN")) {
			return RestUtil.returnNoParameter("CTI_USE_YN");
		}
		if(!params.containsKey("MSGR_USE_YN")) {
			return RestUtil.returnNoParameter("MSGR_USE_YN");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2002(params);
	}
	/**
	 * @Method		: initPassword
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  비밀번호 초기화
	 */
	JSONObject initPassword(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("UserId")) {
			return RestUtil.returnNoParameter("UserId");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2003(params);
	}
	/**
	 * @Method		: getSGList
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  스킬그룹 리스트 조회
	 */
	JSONObject getSGList(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2004(params);
	}
	/**
	 * @Method		: getSGUserList
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  스킬그룹 별 사용자 리스트 조회
	 */
	JSONObject getSGUserList(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("SGCd")) {
			return RestUtil.returnNoParameter("SGCd");
		}
		if(!params.containsKey("SGLevel")) {
			return RestUtil.returnNoParameter("SGLevel");
		} else {
			String sgLevel = params.get("SGLevel").toString();
			if(!"_ALL_".equals(sgLevel)) {
				try {
					Integer.parseInt(sgLevel);
				} catch (NumberFormatException e) {
					return RestUtil.returnWrongParameter(sgLevel);
				}
			}
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2005(params);
	}
	/**
	 * @Method		: getNotInSGUserList
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  스킬그룹 별 제외 사용자 리스트 조회
	 */
	JSONObject getNotInSGUserList(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("SGCd")) {
			return RestUtil.returnNoParameter("SGCd");
		}
		if(!params.containsKey("SGLevel")) {
			return RestUtil.returnNoParameter("SGLevel");
		}
		if(!params.containsKey("Level1Cd")) {
			return RestUtil.returnNoParameter("Level1Cd");
		}
		if(!params.containsKey("Level2Cd")) {
			return RestUtil.returnNoParameter("Level2Cd");
		}
		if(!params.containsKey("Level3Cd")) {
			return RestUtil.returnNoParameter("Level3Cd");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2006(params);
	}
	/**
	 * @Method		: setSGUserList
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  스킬그룹 별 사용자 리스트 저장
	 */
	JSONObject setSGUserList(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("SGCd")) {
			return RestUtil.returnNoParameter("SGCd");
		}
		if(!params.containsKey("SGLevel")) {
			return RestUtil.returnNoParameter("SGLevel");
		}
		if(!params.containsKey("SGLevelSel")) {
			return RestUtil.returnNoParameter("SGLevelSel");
		}
		if(!params.containsKey("UserId")) {
			return RestUtil.returnNoParameter("UserId");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		int levelLength = params.get("SGLevelSel").toString().split(Pattern.quote("^")).length;
		int userLength = params.get("UserId").toString().split(Pattern.quote("^")).length;
		if(levelLength != userLength) {
			JSONObject jResult = new JSONObject();
			jResult.put("errCd", "400");
			jResult.put("errMsg", "arraycnt is wrong [SGLevelSel:"+levelLength+" UserID:"+userLength+"]");
			return jResult;
		}
		
		return appsRestService.ifIs2007(params);
	}
	/**
	 * @Method		: setOrgInfo
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  조직정보 저장
	 */
	JSONObject setOrgInfo(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("ORG_CD")) {
			return RestUtil.returnNoParameter("ORG_CD");
		}
		if(!params.containsKey("ORG_LVL")) {
			return RestUtil.returnNoParameter("ORG_LVL");
		}
		if(!params.containsKey("ORG_UP_CD")) {
			return RestUtil.returnNoParameter("ORG_UP_CD");
		}
		if(!params.containsKey("ORG_NM")) {
			return RestUtil.returnNoParameter("ORG_NM");
		}
		if(!params.containsKey("CNTR_TYP_CD")) {
			return RestUtil.returnNoParameter("CNTR_TYP_CD");
		}
		if(!params.containsKey("BIZ_CLAS_CD")) {
			return RestUtil.returnNoParameter("BIZ_CLAS_CD");
		}
		if(!params.containsKey("USE_YN")) {
			return RestUtil.returnNoParameter("USE_YN");
		}
		if(!params.containsKey("SRT_SEQ")) {
			return RestUtil.returnNoParameter("SRT_SEQ");
		}
		if(!params.containsKey("SRT_KEY")) {
			return RestUtil.returnNoParameter("SRT_KEY");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2008(params);
	}
	/**
	 * @Method		: getUserDupCheck
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  사용자 중복체크
	 */
	JSONObject getUserDupCheck(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("UserId")) {
			return RestUtil.returnNoParameter("UserId");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2009(params);
	}
	/**
	 * @Method		: setUserSG
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  사용자 스킬그룹 저장
	 */
	JSONObject setUserSG(Map<String, Object> params) throws Exception {
		if(!params.containsKey("SysKind")) {
			return RestUtil.returnNoParameter("SysKind");
		}
		if(!params.containsKey("UserId")) {
			return RestUtil.returnNoParameter("UserId");
		}
		if(!params.containsKey("CTI_LGIN_ID")) {
			return RestUtil.returnNoParameter("CTI_LGIN_ID");
		}
		if(!params.containsKey("SGCd")) {
			return RestUtil.returnNoParameter("SGCd");
		}
		if(!params.containsKey("SGLevel")) {
			return RestUtil.returnNoParameter("SGLevel");
		}
		if(!params.containsKey("HandleUserId")) {
			return RestUtil.returnNoParameter("HandleUserId");
		}
		
		return appsRestService.ifIs2011(params);
	}
}