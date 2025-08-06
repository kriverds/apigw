package com.soluvis.ds.apigw.v1.biz.bake.rest.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.bake.rest.feign.BakeRestFeignService;
import com.soluvis.ds.apigw.v1.biz.bake.rest.repository.BakeRestRepository;

import lombok.Setter;

/**
 * @Class 		: BakeRestService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  BAKE REST Service
 */
@Service
public class BakeRestService {
	
	static final Logger logger = LoggerFactory.getLogger(BakeRestService.class);
	
	/**
	 * 스프링 DI
	 */
	BakeRestRepository bakeRestRepository;
	BakeRestFeignService bakeRestFeignService;
	public BakeRestService(BakeRestRepository bakeRestRepository, BakeRestFeignService bakeRestFeignService) {
		this.bakeRestRepository = bakeRestRepository;
		this.bakeRestFeignService = bakeRestFeignService;
	}
	
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeCheckBakeUser
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 생성/삭제 사용자 목록 조회
	 *  2. 생성/삭제 리스트 분리
	 *  3. 생성 및 삭제 실행
	 */
	public JSONObject executeCheckBakeUser() throws Exception {
		List<Map<String, Object>> mlist = bakeRestRepository.checkBakeUser();
		List<Map<String, Object>> createList = new ArrayList<>();
		List<Map<String, Object>> deleteList = new ArrayList<>();
		for(final Map<String, Object> user : mlist) {
			String createCd = user.get("bakecreatecd").toString();
			if("N".equals(createCd)) {
				createList.add(user);
			} else {
				deleteList.add(user);
			}
		}
		
		int createCnt = createBakeUser(createList);
		int deleteCnt = deleteBakeUser(deleteList);
		
		JSONObject jResult = new JSONObject();
		if(mlist.size() != (createCnt+deleteCnt)) {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
		} else {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
		}
		jResult.put(Const.APIGW_KEY_RESULT_MSG, "dataCnt["+mlist.size()+"]"+"createCnt["+createCnt+"] deleteCnt["+deleteCnt+"]");
		
		return jResult;
	}
	
	/**
	 * @Method		: createBakeUser
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Bake User 생성
	 * 
	 *  1. roleList > ASP_ACCESS롤 및 SYSTEM_MANAGER롤 설정
	 *  2. authList > syskind_userclascd 설정
	 *  3. menuGrpCd/userCd/userPs/userNm/grpAuthCd/company_cd 설정
	 */
	int createBakeUser(List<Map<String, Object>> userList) {
		int iResult = 0;
		if(userList.isEmpty()) {
			return iResult;
		}
		
		for(final Map<String,Object> user : userList) {
			String userid = user.get("userid").toString();
			List<Map<String,Object>> bakeUsers = new ArrayList<>();
			Map<String, Object> bakeUser = new HashMap<>();
			
			List<Map<String, Object>> roleList = new ArrayList<>();
			Map<String, Object> roleMap = new HashMap<>();
			roleMap.put("roleCd", "ASP_ACCESS");
			roleMap.put("roleNm", "관리시스템 접근 롤");
			roleMap.put("hasYn", "Y");
			roleMap.put("userCd", "");
			roleMap.put("__original_index", 0);
			roleMap.put("__index", 0);
			roleList.add(roleMap);
			Map<String, Object> roleMap2 = new HashMap<>();
			roleMap2.put("roleCd", "SYSTEM_MANAGER");
			roleMap2.put("roleNm", "시스템 관리자 롤");
			roleMap2.put("hasYn", "Y");
			roleMap2.put("userCd", "");
			roleMap2.put("__original_index", 1);
			roleMap2.put("__index", 1);
			roleList.add(roleMap2);
			bakeUser.put("roleList", roleList);
			
			List<Map<String, Object>> authList = new ArrayList<>();
			Map<String, Object> authMap = new HashMap<>();
			authMap.put("userCd", userid);
			authMap.put("grpAuthCd", user.get("syskind")+"_"+user.get("userclascd"));
			authList.add(authMap);
			bakeUser.put("authList", authList);
			
			bakeUser.put("menuGrpCd", "SYSTEM_MANAGER");
			bakeUser.put("userCd", userid);
			bakeUser.put("userPs", userid+"!");
			bakeUser.put("userNm", user.get("name"));
			bakeUser.put("grpAuthCd", user.get("syskind")+"_"+user.get("userclascd"));
			bakeUser.put("company_cd", "1");
			
			bakeUsers.add(bakeUser);
			
			logger.info("[{}] createBakeUser request>>{}", uuid, bakeUser);
			String response = bakeRestFeignService.saveUser(bakeUsers);
			logger.info("[{}] Bake Response>>{}", uuid, response);
			JSONObject resJO = new JSONObject(response);
			int status = resJO.getInt("status");
			if(status == 0) {
				iResult++;
				Map<String,Object> queryParams = new HashMap<>();
				queryParams.put("userid", userid);
				bakeRestRepository.setBakeCreated(queryParams);
				logger.info("[{}] update bakecreatecd Y [{}]", uuid, userid);
			} else {
				logger.error("[{}] createBakeUser Fail response>> {}", uuid, resJO);
			}
		}
		return iResult;
		
	}
	
	/**
	 * @Method		: deleteBakeUser
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Bake User 삭제
	 * 
	 *  1. UserCd 설정
	 */
	int deleteBakeUser(List<Map<String, Object>> userList) {
		int iResult = 0;
		if(userList.isEmpty()) {
			return iResult;
		}
		
		for(final Map<String,Object> user : userList) {
			String userid = user.get("userid").toString();
			List<Map<String,Object>> bakeUsers = new ArrayList<>();
			Map<String, Object> bakeUser = new HashMap<>();
			bakeUser.put("userCd", userid);
			bakeUsers.add(bakeUser);
			String response = bakeRestFeignService.deleteUser(bakeUsers);
			logger.info("[{}] Bake Response>>{}", uuid, response);
			JSONObject resJO = new JSONObject(response);
			int status = resJO.getInt("status");
			if(status == 0) {
				iResult++;
				Map<String,Object> queryParams = new HashMap<>();
				queryParams.put("userid", userid);
				bakeRestRepository.setBakeDeleted(queryParams);
				logger.info("[{}] update bakecreatecd N [{}]", uuid, userid);
			} else {
				logger.error("[{}] deleteBakeUser Fail response>> {}", uuid, resJO);
			}
		}
		return iResult;
	}
	
	/**
	 * @Method		: initPassword
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Bake User 패스워드 초기화
	 */
	public boolean initPassword(String userid) {
		boolean bResult = false;
		List<Map<String,Object>> bakeUsers = new ArrayList<>();
		Map<String, Object> bakeUser = new HashMap<>();
		bakeUser.put("userCd", userid);
		bakeUser.put("userPs", userid+"!");
		bakeUsers.add(bakeUser);
		String response = bakeRestFeignService.initPassword(bakeUsers);
		logger.info("[{}] Bake Response>>{}", uuid, response);
		JSONObject resJO = new JSONObject(response);
		int status = resJO.getInt("status");
		if(status == 0) {
			bResult = true;
			logger.info("[{}] Bake initPassword Success[{}]", uuid, userid);
		} else {
			logger.error("[{}] Bake initPassword Fail response>> {}", uuid, resJO);
		}
		return bResult;
	}
}
