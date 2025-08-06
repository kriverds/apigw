package com.soluvis.ds.apigw.v1.biz.cti.winker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.cti.winker.repository.CtiWinkerRepository;
import com.soluvis.ds.apigw.v1.lib.winker.engine.WinkerConnector;
import com.soluvis.ds.apigw.v1.lib.winker.manager.WinkerClientManager;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import lombok.Setter;

/**
 * @Class 		: CtiWinkerService
 * @date   		: 2025. 4. 25.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CTI Winker Service
 */
@Service
public class CtiWinkerService {
	
	static final Logger logger = LoggerFactory.getLogger(CtiWinkerService.class);
	
	/**
	 * 스프링 DI
	 */
	CtiWinkerRepository ctiWinkerRepository;
	WinkerClientManager winkerClientManager;
	public CtiWinkerService(CtiWinkerRepository ctiWinkerRepository, WinkerClientManager winkerClientManager) {
		this.ctiWinkerRepository = ctiWinkerRepository;
		this.winkerClientManager = winkerClientManager;
	}
	
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeCheckCtiUser
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 생성/삭제 사용자 목록 조회
	 *  2. 생성/삭제 리스트 분리
	 *  3. 생성 및 삭제 실행
	 */
	public JSONObject executeCheckCtiUser() throws Exception {
		List<Map<String, Object>> mlist = ctiWinkerRepository.checkCtiUser();
		List<Map<String, Object>> createList = new ArrayList<>();
		List<Map<String, Object>> deleteList = new ArrayList<>();
		
		for(final Map<String, Object> user : mlist) {
			logger.info("[{}] {}", uuid, user);
			String createCd = user.get("cticreatecd").toString();
			if("N".equals(createCd)) {
				createList.add(user);
			} else {
				deleteList.add(user);
			}
		}
		
		JSONObject cJob = createCtiUser(createList);
		String cResult = CommonUtil.getJString(cJob, Const.HTTP_ERROR_CD_KEY);
		JSONObject dJob = deleteCtiUser(deleteList);
		String dResult = CommonUtil.getJString(dJob, Const.HTTP_ERROR_CD_KEY);
		
		JSONObject jResult = new JSONObject();
		if(("0".equals(cResult) || "".equals(cResult)) && ("0".equals(dResult) || "".equals(dResult))) {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
		} else {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
		}
		jResult.put(Const.APIGW_KEY_RESULT_MSG, "dataCnt["+mlist.size()+"]"+"createCnt["+createList.size()+"] deleteCnt["+deleteList.size()+"] createResult["+cJob.toString()+"] deleteResult["+dJob.toString()+"]");
		
		return jResult;
	}
	
	/**
	 * @Method		: createCtiUser
	 * @date   		: 2025. 2. 27.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @throws InterruptedException
	 * @notify
	 *  CTI Person 생성
	 * 
	 *  1. Winker Websocket 통신으로 CTI Person 생성
	 *  2. 더미스킬 ID를 조회하여 Person 생성 시 더미스킬 부여(통계에 필요)
	 *  3. Websocket Event 성공으로 수신 시 cticreatecd Y로 업데이트
	 *  4. Websocket Event 모두 수신 후 리턴
	 */
	public JSONObject createCtiUser(List<Map<String, Object>> userList) throws InterruptedException {
		if(userList.isEmpty()) {
			return new JSONObject();
		}
		List<Integer> jobList = new ArrayList<>();
		
		String dummySkillId = ctiWinkerRepository.getDummySkillId();
		WinkerConnector connector = winkerClientManager.getInstance();
		for(final Map<String, Object> user: userList) {
			String name = user.get("name").toString();
			String empId = user.get("userid").toString();
			String syskind = user.get("syskind").toString();
			String ctiLoginId = user.get("ctiLoginId").toString();
			int lRefid = connector.createAgent(0, empId, name, syskind, ctiLoginId+"@*", dummySkillId, "0");
			jobList.add(lRefid);
			WinkerClientManager.getJobMap().put(lRefid, "");
			logger.info("[{}] createCtiUser lRefid[{}]", uuid, lRefid);
		}
		
		int tryCnt = 0;
		int jobCnt = jobList.size();
		int failCnt = 0;
		int successCnt = 0;
		if(!jobList.isEmpty() && winkerClientManager.isOpen()) {
			while(tryCnt<Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
				List<Integer> jobTempList = List.copyOf(jobList);
				for(final int job : jobTempList) {
					String jobMsg = WinkerClientManager.getJobMap().get(job);
					if(jobMsg == null) {
						logger.info("[{}] Complete job[{}]", uuid, job);
						jobList.remove(Integer.valueOf(job));
						successCnt++;
					} else if(!"".equals(jobMsg)) {
						logger.error("[{}] Fail job[{}]>>{}", uuid, job, jobMsg);
						WinkerClientManager.getJobMap().remove(job);
						jobList.remove(Integer.valueOf(job));
						failCnt++;
					}
				}
				if(jobList.isEmpty()) {
					logger.info("[{}] Winker Job All Complete", uuid);
					break;
				}
				Thread.sleep(Const.WINKER_EVENT_CHECK_INTERVAL);
				tryCnt++;
			}
		}
		
		JSONObject jResult = new JSONObject();
		
		if(tryCnt == Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Timeout 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else if(failCnt == 0) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Success 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Fail 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		}
		
		return jResult;
	}
	
	/**
	 * @Method		: deleteCtiUser
	 * @date   		: 2025. 3. 12.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @throws InterruptedException
	 * @notify
	 *  CTI Person 삭제
	 * 
	 *  1. Winker Websocket 통신으로 CTI Person 삭제
	 *  2. Websocket Event 성공으로 수신 시 cticreatecd N으로 업데이트
	 *  3. Websocket Event 모두 수신 후 리턴
	 */
	public JSONObject deleteCtiUser(List<Map<String, Object>> userList) throws InterruptedException {
		if(userList.isEmpty()) {
			return new JSONObject();
		}
		List<Integer> jobList = new ArrayList<>();
		
		WinkerConnector connector = winkerClientManager.getInstance();
		for(final Map<String, Object> user: userList) {
			String empId = user.get("userid").toString();
			int lRefid = connector.deleteAgent(0, empId);
			jobList.add(lRefid);
			WinkerClientManager.getJobMap().put(lRefid, "");
			logger.info("[{}] deleteCtiUser lRefid[{}]", uuid, lRefid);
		}
		
		int tryCnt = 0;
		int jobCnt = jobList.size();
		int failCnt = 0;
		int successCnt = 0;
		if(!jobList.isEmpty() && winkerClientManager.isOpen()) {
			while(tryCnt<Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
				List<Integer> jobTempList = List.copyOf(jobList);
				for(final int job : jobTempList) {
					String jobMsg = WinkerClientManager.getJobMap().get(job);
					if(jobMsg == null) {
						logger.info("[{}] Complete job[{}]", uuid, job);
						jobList.remove(Integer.valueOf(job));
						successCnt++;
					} else if(!"".equals(jobMsg)) {
						logger.error("[{}] Fail job[{}]>>{}", uuid, job, jobMsg);
						WinkerClientManager.getJobMap().remove(job);
						jobList.remove(Integer.valueOf(job));
						failCnt++;
					}
				}
				if(jobList.isEmpty()) {
					logger.info("[{}] Winker Job All Complete", uuid);
					break;
				}
				Thread.sleep(Const.WINKER_EVENT_CHECK_INTERVAL);
				tryCnt++;
			}
		}
		
		JSONObject jResult = new JSONObject();
		
		if(tryCnt == Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Timeout 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else if(failCnt == 0) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Success 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Fail 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		}
		
		return jResult;
	}
	
	/**
	 * @Method		: oneSkillToMultiUser
	 * @date   		: 2025. 3. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  하나의 스킬을 여러 상담사에게 부여
	 * 
	 *  1. User/스킬레벨 N개와 스킬 1개를 파라미터로 받음.
	 *  2. 스킬을 보유한 상담사를 조회
	 *  3. 조회한 상담사와 요청 리스트 비교
	 *  - 요청 스킬과 레벨이 모두 같을경우 유지 > sameUserList
	 *  - 요청 스킬과 레벨이 다를 경우 업데이트 > updateUserList
	 *  - 요청 스킬이 없을경우 추가 > addUserList
	 *  - levelSel이 '_ALL_'일 경우 위 리스트에 없는 스킬 보유중인 상담원 모두 삭제 > deleteUserList
	 *  - levelSel이 숫자일 경우 위 리스트에 없는 levelSel과 같은 레벨 보유중인 상담원 모두 삭제 > deleteUserList
	 *  4. 상담사의 CTI Person DBID를 조회
	 *  5. 스킬 추가 수정 삭제 수행
	 *  6. Websocket Event 모두 수신 후 리턴
	 */
	public JSONObject oneSkillToMultiUser(String[] users, String skill, String[] levelSels, String level, String sysKind) throws Exception{
		Calendar sCal = Calendar.getInstance();
		List<String> requestUserList = new ArrayList<>(Arrays.asList(users));
		List<String> requestLevelList = new ArrayList<>(Arrays.asList(levelSels));
		
		List<String> addUserList = new ArrayList<>();
		List<String> addLevelList = new ArrayList<>();
		
		List<String> updateUserList = new ArrayList<>();
		List<String> updateDbidList = new ArrayList<>();
		List<String> updateLevelList = new ArrayList<>();
		
		List<String> deleteUserList = new ArrayList<>();
		List<String> deleteDbidList = new ArrayList<>();
		
		List<String> sameUserList = new ArrayList<>();
		
		Map<String, Object> queryMap = new HashMap<>();
		queryMap.put("skill", skill);
		queryMap.put("syskind", sysKind);
		
		List<Map<String, Object>> userSkillList = ctiWinkerRepository.getUsersHaveSkill(queryMap);
		for(final Map<String,Object> user: userSkillList) {
			String userid = user.get("userid").toString();
			String dbid = user.get("personDbid").toString();
			String skillLevel = user.get("skillLevel").toString();
			if(requestUserList.contains(userid)) {
				int idx = requestUserList.indexOf(userid);
				String requestLevel = requestLevelList.get(idx);
				if(skillLevel.equals(requestLevel)) {
					sameUserList.add(userid);
				} else {
					updateUserList.add(userid);
					updateDbidList.add(dbid);
					updateLevelList.add(requestLevel);
				}
			} else {
				if("_ALL_".equals(level) || skillLevel.equals(level)) {
					deleteUserList.add(userid);
					deleteDbidList.add(dbid);
				}
			}
		}
		
		for(final String requestUser : requestUserList) {
			if(!"".equals(requestUser) && !updateUserList.contains(requestUser) && !deleteUserList.contains(requestUser) && !sameUserList.contains(requestUser)) {
				addUserList.add(requestUser);
				int idx = requestUserList.indexOf(requestUser);
				addLevelList.add(requestLevelList.get(idx));
			}
		}
		
		String[] skillList = {skill};
		
		List<Integer> jobList = new ArrayList<>();
		
		if(!addUserList.isEmpty()) {
			queryMap.put("list", addUserList);
			List<Map<String, Object>> userDbidList = ctiWinkerRepository.getPersonDbid(queryMap);
			List<String> addDbidList = userDbidList.stream().map(user -> user.get("dbid").toString()).toList();
			for (int i = 0; i < addDbidList.size(); i++) {
				String[] userList = {addDbidList.get(i)};
				String[] levelList = {addLevelList.get(i)};
				List<Integer> lRefList = addSkillMulti(userList, skillList, levelList);
				jobList.addAll(lRefList);
			}
		}
		if(!updateUserList.isEmpty()) {
			for (int i = 0; i < updateDbidList.size(); i++) {
				String[] userList = {updateDbidList.get(i)};
				String[] levelList = {updateLevelList.get(i)};
				List<Integer> lRefList = updateSkillMulti(userList, skillList, levelList);
				jobList.addAll(lRefList);
			}
		}
		if(!deleteDbidList.isEmpty()) {
			for (int i = 0; i < deleteDbidList.size(); i++) {
				String[] userList = {deleteDbidList.get(i)};
				List<Integer> lRefList = deleteSkillMulti(userList, skillList);
				jobList.addAll(lRefList);
			}
		}
		
		int tryCnt = 0;
		int jobCnt = jobList.size();
		int failCnt = 0;
		int successCnt = 0;
		if(!jobList.isEmpty() && winkerClientManager.isOpen()) {
			while(tryCnt<Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
				List<Integer> jobTempList = List.copyOf(jobList);
				for(final int job : jobTempList) {
					String jobMsg = WinkerClientManager.getJobMap().get(job);
					if(jobMsg == null) {
						logger.info("[{}] Complete job[{}]", uuid, job);
						jobList.remove(Integer.valueOf(job));
						successCnt++;
					} else if(!"".equals(jobMsg)) {
						logger.error("[{}] Fail job[{}]>>{}", uuid, job, jobMsg);
						WinkerClientManager.getJobMap().remove(job);
						jobList.remove(Integer.valueOf(job));
						failCnt++;
					}
				}
				if(jobList.isEmpty()) {
					logger.info("[{}] Winker Job All Complete", uuid);
					break;
				}
				Thread.sleep(Const.WINKER_EVENT_CHECK_INTERVAL);
				tryCnt++;
			}
		}
		
		JSONObject jResult = new JSONObject();
		
		if(tryCnt == Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Timeout 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else if(failCnt == 0) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Success 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Fail 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] addUserList[{}] updateUserList[{}] deleteUserList[{}] sameUserList[{}]", uuid, addUserList, updateUserList, deleteUserList, sameUserList);
		return jResult;
	}
	
	/**
	 * @Method		: MultiSkillToOneUser
	 * @date   		: 2025. 3. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  여러개의 스킬을 상담사 한명에게 부여
	 * 
	 *  1. 상담사 한명과 스킬/레벨 N개를 파라미터로 받음.
	 *  2. 상담사의 보유 스킬을 조회
	 *  3. 상담사 보유 스킬과 요청 리스트 비교
	 *  - 요청 스킬과 레벨이 모두 같을경우 유지 > sameSkillList
	 *  - 요청 스킬과 레벨이 다를 경우 업데이트 > updateSkillList
	 *  - 요청 스킬에 없을 경우 삭제 > deleteSkillList
	 *  - 위의 세가지 경우가 아닐 경우 추가 > addSkillList
	 *  4. 상담사의 CTI Person DBID를 조회
	 *  5. 스킬 추가 수정 삭제 수행
	 *  6. Websocket Event 모두 수신 후 리턴
	 */
	public JSONObject multiSkillToOneUser(String user, String[] skills, String[] levels, String sysKind) throws Exception {
		Calendar sCal = Calendar.getInstance();
		
		List<String> requestSkillList = new ArrayList<>(Arrays.asList(skills));
		List<String> requestLevelList = new ArrayList<>(Arrays.asList(levels));
		
		List<String> addSkillList = new ArrayList<>();
		List<String> addLevelList = new ArrayList<>();
		
		List<String> updateSkillList = new ArrayList<>();
		List<String> updateLevelList = new ArrayList<>();
		
		List<String> deleteSkillList = new ArrayList<>();
		
		List<String> sameSkillList = new ArrayList<>();
		
		Map<String, Object> queryMap = new HashMap<>();
		queryMap.put("userid", user);
		queryMap.put("syskind", sysKind);
		
		List<Map<String, Object>> userSkillList = ctiWinkerRepository.getUserSkills(queryMap);
		for(final Map<String,Object> sk: userSkillList) {
			String skillId = sk.get("skillDbid").toString();
			String skillLevel = sk.get("skillLevel").toString();
			
			if(requestSkillList.contains(skillId)) {
				int idx = requestSkillList.indexOf(skillId);
				String requestLevel = requestLevelList.get(idx);
				if(skillLevel.equals(requestLevel)) {
					sameSkillList.add(skillId);
				} else {
					updateSkillList.add(skillId);
					updateLevelList.add(requestLevel);
				}
			} else {
				deleteSkillList.add(skillId);
			}
		}
		
		for(final String requestSkill : requestSkillList) {
			if(!"".equals(requestSkill) && !updateSkillList.contains(requestSkill) && !deleteSkillList.contains(requestSkill) && !sameSkillList.contains(requestSkill)) {
				addSkillList.add(requestSkill);
				int idx = requestSkillList.indexOf(requestSkill);
				addLevelList.add(requestLevelList.get(idx));
			}
		}
		
		List<Integer> jobList = new ArrayList<>();
		
		queryMap.put("list", Arrays.asList(user));
		List<Map<String, Object>> userDbidList = ctiWinkerRepository.getPersonDbid(queryMap);
		List<String> addDbidList = userDbidList.stream().map(u -> u.get("dbid").toString()).toList();
		String[] userList = addDbidList.toArray(new String[0]);
		if(!addSkillList.isEmpty()) {
			List<Integer> lRefList = addSkillMulti(userList, addSkillList.toArray(new String[0]), addLevelList.toArray(new String[0]));
			jobList.addAll(lRefList);
		}
		if(!updateSkillList.isEmpty()) {
			List<Integer> lRefList = updateSkillMulti(userList, updateSkillList.toArray(new String[0]), updateLevelList.toArray(new String[0]));
			jobList.addAll(lRefList);
		}
		if(!deleteSkillList.isEmpty()) {
			List<Integer> lRefList = deleteSkillMulti(userList, deleteSkillList.toArray(new String[0]));
			jobList.addAll(lRefList);
		}
		
		int tryCnt = 0;
		int jobCnt = jobList.size();
		int failCnt = 0;
		int successCnt = 0;
		if(!jobList.isEmpty() && winkerClientManager.isOpen()) {
			while(tryCnt<Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
				List<Integer> jobTempList = List.copyOf(jobList);
				for(final int job : jobTempList) {
					String jobMsg = WinkerClientManager.getJobMap().get(job);
					if(jobMsg == null) {
						logger.info("[{}] Complete job[{}]", uuid, job);
						jobList.remove(Integer.valueOf(job));
						successCnt++;
					} else if(!"".equals(jobMsg)) {
						logger.error("[{}] Fail job[{}]>>{}", uuid, job, jobMsg);
						WinkerClientManager.getJobMap().remove(job);
						jobList.remove(Integer.valueOf(job));
						failCnt++;
					}
				}
				if(jobList.isEmpty()) {
					logger.info("[{}] Winker Job All Complete", uuid);
					break;
				}
				Thread.sleep(Const.WINKER_EVENT_CHECK_INTERVAL);
				tryCnt++;
			}
		}
		
		JSONObject jResult = new JSONObject();
		
		if(tryCnt == Const.WINKER_EVENT_CHECK_MAX_TRY_CNT) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Timeout 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else if(failCnt == 0) {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "0");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Success 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		} else {
			jResult.put(Const.HTTP_ERROR_CD_KEY, "1");
			jResult.put(Const.HTTP_ERROR_MSG_KEY, "Fail 요청건수["+jobCnt+"] 성공건수["+successCnt+"]"+"] 실패건수["+failCnt+"]");
		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		logger.info("[{}] addSkillList[{}] updateSkillList[{}] deleteSkillList[{}] sameSkillList[{}]", uuid, addSkillList, updateSkillList, deleteSkillList, sameSkillList);
		return jResult;
	}
	
	/**
	 * @Method		: addSkillMulti
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  스킬 추가
	 */
	List<Integer> addSkillMulti(String[] userList, String[] skillList, String[] levelList) {
		List<Integer> lRefList = new ArrayList<>();
		WinkerConnector connector = winkerClientManager.getInstance();
		for(final String user: userList) {
			int lRef = connector.addSkillMulti(user, skillList, levelList);
			lRefList.add(lRef);
			WinkerClientManager.getJobMap().put(lRef, "");
		}
		return lRefList;
	}
	
	/**
	 * @Method		: updateSkillMulti
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  스킬 업데이트
	 */
	List<Integer> updateSkillMulti(String[] userList, String[] skillList, String[] levelList) {
		List<Integer> lRefList = new ArrayList<>();
		WinkerConnector connector = winkerClientManager.getInstance();
		for(final String user: userList) {
			int lRef = connector.updateSkillMulti(user, skillList, levelList);
			lRefList.add(lRef);
			WinkerClientManager.getJobMap().put(lRef, "");
		}
		return lRefList;
	}
	
	/**
	 * @Method		: deleteSkillMulti
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  스킬 삭제
	 */
	List<Integer> deleteSkillMulti(String[] userList, String[] skillList) {
		List<Integer> lRefList = new ArrayList<>();
		WinkerConnector connector = winkerClientManager.getInstance();
		for(final String user: userList) {
			int lRef = connector.deleteSkillMulti(user, skillList);
			lRefList.add(lRef);
			WinkerClientManager.getJobMap().put(lRef, "");
		}
		return lRefList;
	}
}
