package com.soluvis.ds.apigw.v1.application.manual;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.soluvis.ds.apigw.v1.biz.apps.eai.batch.AppsEaiScheduler;
import com.soluvis.ds.apigw.v1.biz.apps.rest.mapper.AppsRestMapper;
import com.soluvis.ds.apigw.v1.biz.bake.rest.feign.BakeRestFeignService;
import com.soluvis.ds.apigw.v1.biz.bake.rest.service.BakeRestService;
import com.soluvis.ds.apigw.v1.biz.chat.eai.batch.ChatEaiScheduler;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.batch.ChatbotNasScheduler;
import com.soluvis.ds.apigw.v1.biz.cti.winker.service.CtiWinkerService;
import com.soluvis.ds.apigw.v1.biz.mobile.eai.batch.MobileEaiScheduler;
import com.soluvis.ds.apigw.v1.biz.vars.eai.batch.VarsEaiScheduler;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.batch.WfmsNasScheduler;
import com.soluvis.ds.apigw.v1.lib.winker.engine.WinkerConnector;
import com.soluvis.ds.apigw.v1.lib.winker.manager.WinkerClientManager;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Class 		: ManualOptController
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  배치 수동 호출을 위한 Controller
 */
@RestController
@RequestMapping("/api/v1/manual")
public class ManualOptController {
	
	static final Logger logger = LoggerFactory.getLogger(ManualOptController.class);

	/**
	 * 스프링 DI
	 */
	ChatbotNasScheduler chatbotNasScheduler;
	WfmsNasScheduler wfmsNasScheduler;
	AppsEaiScheduler appsEaiScheduler;
	ChatEaiScheduler chatEaiScheduler;
	MobileEaiScheduler mobileEaiScheduler;
	VarsEaiScheduler varsEaiScheduler;
	CtiWinkerService ctiWinkerService;
	WinkerClientManager winkerClientManager;
	BakeRestFeignService bakeRestFeignService;
	BakeRestService bakeRestService;
	AppsRestMapper appsRestMapper;
	public ManualOptController(ChatbotNasScheduler chatbotNasScheduler, WfmsNasScheduler wfmsNasScheduler, AppsEaiScheduler appsEaiScheduler,
			ChatEaiScheduler chatEaiScheduler, MobileEaiScheduler mobileEaiScheduler, VarsEaiScheduler varsEaiScheduler,
			CtiWinkerService ctiWinkerService, WinkerClientManager winkerClientManager, BakeRestFeignService bakeRestFeignService,
			BakeRestService bakeRestService, AppsRestMapper appsRestMapper) {
		this.chatbotNasScheduler = chatbotNasScheduler;
		this.wfmsNasScheduler = wfmsNasScheduler;
		this.appsEaiScheduler = appsEaiScheduler;
		this.chatEaiScheduler = chatEaiScheduler;
		this.mobileEaiScheduler = mobileEaiScheduler;
		this.varsEaiScheduler = varsEaiScheduler;
		this.ctiWinkerService = ctiWinkerService;
		this.winkerClientManager = winkerClientManager;
		this.bakeRestFeignService = bakeRestFeignService;
		this.bakeRestService = bakeRestService;
		this.appsRestMapper = appsRestMapper;
	}
	
	/**
	 *  executeCbStatsInfoBatch: 	챗봇 상담현황 		| 0 0 1 * * *
	 *  executeCmStatsInfoBatch: 	챗봇 캠페인결과 	| 0 0/5 * * * *
	 *  executeHolInfoBatch: 		챗봇 휴일정보 		| 0 0 3 * * *
	 *  executeSendSmsResultBatch: 	챗봇 알림톡 전송결과 	| 0 1/10 * * * *
	 */
	@PostMapping(value = "/chatbot/nas/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postChatNasbotCall(HttpServletRequest request, @PathVariable String service) throws Exception {
		logger.info("POST Chatbot Nas Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "CBSTATSINFO" 				-> chatbotNasScheduler.executeCbStatsInfoBatch();
			case "CMSTATSINFO" 				-> chatbotNasScheduler.executeCmStatsInfoBatch();
			case "CMSTATSINFODAY" 			-> chatbotNasScheduler.executeCmStatsInfoDayBatch();
			case "HOLINFO" 					-> chatbotNasScheduler.executeHolInfoBatch();
			case "SMSSENDRESULT" 			-> chatbotNasScheduler.executeSmsSendResultBatch();
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	/**
	 *  executeCdInfBatch: 				예측그룹 코드 			| 0 10 0 * * *
	 *  executeGroupLimitBizKindBatch: 	그룹별 정원정보 		| 0 10 3 * * *
	 *  executePrdcCallHourBatch: 		시간별 콜예측 현황 		| 0 59 * * * *
	 *  executePrdcGroupMemberBatch: 	그룹별 업무인원 		| 0 0 3 * * *
	 *  executeSumAgent30minBatch: 		30분단위 상담사 콜정보 	| 0 5/30 * * * *
	 *  executeSumAgentDayBatch: 		상담사실적정보 			| 0 30 0 * * *
	 *  executeSumChatGroupDayBatch: 	채팅집계정보 			| 0 20 0 * * *
	 *  executeSumHo30minBatch: 		30분단위 CTIQ별 콜정보	| 0 2/30 * * * *
	 *  executeSumMrpMonthBatch: 		월별 대표번호 응대현황 	| 0 0 5 * * *
	 *  executeSumReason30minBatch: 	30분단위 상담사 상태정보 	| 0 8/30 * * * *
	 *  executeUserEvltBatch: 			평가대상자 업데이트 		| 0 20 3 * * *
	 */
	@PostMapping(value = "/wfms/nas/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postWfmsNasCall(HttpServletRequest request, @PathVariable String service) throws Exception {
		logger.info("POST Wfms Nas Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "CDINF" 					-> wfmsNasScheduler.executeCdInfBatch();
			case "GROUPLIMITBIZKIND" 		-> wfmsNasScheduler.executeGroupLimitBizKindBatch();
			case "SUMAGENT30MIN" 			-> wfmsNasScheduler.executeSumAgent30minBatch();
			case "SUMAGENTDAY" 				-> wfmsNasScheduler.executeSumAgentDayBatch();
			case "SUMCHATGROUPDAY" 			-> wfmsNasScheduler.executeSumChatGroupDayBatch();
			case "SUMHO30MIN" 				-> wfmsNasScheduler.executeSumHo30minBatch();
			case "SUMMRPMONTH" 				-> wfmsNasScheduler.executeSumMrpMonthBatch();
			case "SUMREASON30MIN" 			-> wfmsNasScheduler.executeSumReason30minBatch();
			case "USEREVLT" 				-> wfmsNasScheduler.executeUserEvltBatch();
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	/**
	 *  executeIVZZMANBSH003Batch: 				청약진행 현황조회 		| 0 0/5 * * * *
	 *  executeIVZZMANBSH003BatchBefore15Day: 	청약진행 현황조회(재집계)	| 0 2/30 * * * *
	 */
	@PostMapping(value = "/apps/eai/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postAppsEaiCall(HttpServletRequest request, @PathVariable String service) throws Exception {
		logger.info("POST Apps Eai Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "IVZZMANBSH003" 					-> appsEaiScheduler.executeIVZZMANBSH003Batch();
			case "IVZZMANBSH003BEFORE15DAY" 		-> appsEaiScheduler.executeIVZZMANBSH003BatchBefore15Day();
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	/**
	 *  executeIVZZMOCSSH001Batch: 채팅상담 통계연동 | 0 0/5 * * * *
	 */
	@PostMapping(value = "/chat/eai/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postChatEaiCall(HttpServletRequest request, @PathVariable String service) throws Exception {
		logger.info("POST Chat Eai Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "IVZZMOCSSH001" 			-> chatEaiScheduler.executeIVZZMOCSSH001Batch();
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	/**
	 *  executeIVZZMOZZSH001Batch: 모바일앱용 콜센터 현황 조회 | 0/5 * * * * *
	 */
	@PostMapping(value = "/mobile/eai/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postMobileEaiCall(HttpServletRequest request, @PathVariable String service) throws Exception {
		logger.info("POST Mobile Eai Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "IVZZMOZZSH001" 			-> mobileEaiScheduler.executeIVZZMOZZSH001Batch();
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	/**
	 *  executeIVZZMOARSH001Batch: 보이는ARS용 콜센터 현황 조회 | 0/5 * * * * *
	 */
	@PostMapping(value = "/vars/eai/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postVarsEaiCall(HttpServletRequest request, @PathVariable String service) throws Exception {
		logger.info("POST Vars Eai Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "IVZZMOARSH001" 			-> varsEaiScheduler.executeIVZZMOARSH001Batch();
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	/**
	 *  executeCheckCtiUser: CTI 생성/삭제 사용자 체크 | 0 0/5 * * * *
	 */
	@PostMapping(value = "/cti/winker/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postCtiWinkerCall(HttpServletRequest request, @PathVariable String service,
			@RequestParam Map<String,Object> params) throws Exception {
		logger.info("POST Cti Winker Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "CREATEUSER" 			-> {
				String name = params.get("name").toString();
				String empId = params.get("userid").toString();
				String syskind = params.get("syskind").toString();
				String ctiLoginId = params.get("cti_login_id").toString();
				WinkerConnector connector = winkerClientManager.getInstance();
				int lRefid = connector.createAgent(0, empId, name, syskind, ctiLoginId+"@*", "0", "0");
				logger.info("createCtiUser lRefid[{}]", lRefid);
			}
			case "DELETEUSER" 			-> {
				WinkerConnector connector = winkerClientManager.getInstance();
				String empId = params.get("userid").toString();
				int lRefid = connector.deleteAgent(0, empId);
				logger.info("createCtiUser lRefid[{}]", lRefid);
			}
			case "CHECKCTIUSER" 			-> {
				ctiWinkerService.executeCheckCtiUser();
			}
			case "ADDSKILL" 			-> {
				JSONArray userList = (JSONArray)params.get("userList");
				for (int i = 0; i < userList.length(); i++) {
					JSONObject user = userList.getJSONObject(i);
					String empId = user.getString("userid");
					String syskind = user.getString("syskind");
					String skillList = user.getString("userid");
					String levelList = user.getString("userid");
					ctiWinkerService.multiSkillToOneUser(empId, skillList.split(","), levelList.split(","), syskind);
				}
			}
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	@PostMapping(value = "/cti/skill/{service}", produces = "application/json; charset=UTF-8")
	public String postCtiAddSkill(HttpServletRequest request, @PathVariable String service,
			@RequestBody Map<String,Object> params) throws Exception {
		logger.info("POST Cti Addskill Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "ADDSKILL" 			-> {
				JSONObject jo = new JSONObject(params);
				JSONArray userList = jo.getJSONArray("userList");
				for (int i = 0; i < userList.length(); i++) {
					JSONObject user = userList.getJSONObject(i);
					String empId = user.getString("userid");
					String syskind = user.getString("syskind");
					String skillList = user.getString("skillList");
					String levelList = user.getString("levelList");
					ctiWinkerService.multiSkillToOneUser(empId, skillList.split(","), levelList.split(","), syskind);
				}
			}
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	/**
	 *  executeCheckBakeUser: Bake 생성/삭제 사용자 체크 | 0 0/5 * * * *
	 */
	@PostMapping(value = "/bake/rest/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postBakeRestCall(HttpServletRequest request, @PathVariable String service,
			@RequestParam Map<String,Object> params) throws Exception {
		logger.info("POST Bake Rest Call Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "CREATEUSER" 			-> {
				List<Map<String,Object>> bakeUsers = new ArrayList<>();
				Map<String, Object> bakeUser = new HashMap<>();
//				bakeUser.put("masterCompCd", "");
//				bakeUser.put("compCd", "S0001");
				
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
				authMap.put("userCd", params.get("userid"));
				authMap.put("grpAuthCd", params.get("syskind")+"_"+params.get("userclascd"));
				authList.add(authMap);
				bakeUser.put("authList", authList);
				
				bakeUser.put("menuGrpCd", "SYSTEM_MANAGER");
				bakeUser.put("userCd", params.get("userid"));
				bakeUser.put("userPs", params.get("userid")+"!");
				bakeUser.put("userNm", params.get("name"));
				bakeUser.put("grpAuthCd", params.get("syskind")+"_"+params.get("userclascd"));
				bakeUser.put("company_cd", "1");
				
				bakeUsers.add(bakeUser);
				
//				bakeUser.put("userStatus", "NORMAL");
//				bakeUser.put("locale", "ko_KR");
				
				
//				bakeUser.put("useYn", "Y");
//				bakeUser.put("delYn", "N");
				
				logger.info("{}", bakeUser);
				String response = bakeRestFeignService.saveUser(bakeUsers);
				logger.info("{}", response);
			}
			case "DELETEUSER" 			-> {
				String[] users = {
						"H1200061",
						"12400390",
						"20370083",
						"31234563",
						"dsttt",
						"gh",
						"52587786",
						"52386682",
						"H1200036",
						"H1200062",
						"51580016",
						"51280015",
						"52487566",
						"22470187"
				};
				for(final String user: users) {
					List<Map<String,Object>> bakeUsers = new ArrayList<>();
					Map<String, Object> bakeUser = new HashMap<>();
					bakeUser.put("userCd", user);
					bakeUsers.add(bakeUser);
					String response = bakeRestFeignService.deleteUser(bakeUsers);
					logger.info("{}", response);
				}
				
			}
			case "CHECKBAKEUSER" 			-> {
				bakeRestService.executeCheckBakeUser();
			}
			case "CHANGEPASSWORD" 			-> {
				List<Map<String,Object>> bakeUsers = new ArrayList<>();
				Map<String, Object> bakeUser = new HashMap<>();
				bakeUser.put("userCd", params.get("userid"));
				bakeUser.put("userPs", params.get("userid")+"!");
				bakeUsers.add(bakeUser);
				String response = bakeRestFeignService.initPassword(bakeUsers);
				logger.info("{}", response);
			}
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	/**
	 * @Method		: postTest
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  테스트용도
	 */
	@PostMapping(value = "/test/{service}", produces = "application/x-www-form-urlencoded; charset=UTF-8")
	public String postTest(HttpServletRequest request, @PathVariable String service,
			@RequestParam Map<String,Object> params) throws Exception {
		logger.info("POST Test Controller Service[{}]", service);
		
		JSONObject result = new JSONObject();

		String ucService = service.toUpperCase(Locale.ENGLISH);
		switch (ucService) {
			case "1" 			-> {
				String query = "select * from swm.t_user where rownum < 10";
				List<Map<String,Object>> lm = jdbcTemplate.queryForList(query);
				logger.info("{}",lm);
			}
			case "2" 			-> {
			}
			default -> {
				result.put("timeStamp", LocalDateTime.now());
				result.put("errCd", "404");
				result.put("errMsg", "Check URL");
				result.put("status", 404);
				return result.toString();
			}
		}
		result.put("timeStamp", LocalDateTime.now());
		result.put("status", 200);
		logger.info("{}", result);
		return result.toString();
	}
}
