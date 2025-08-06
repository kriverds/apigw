package com.soluvis.ds.apigw.v1.lib.winker.engine.listener;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.soluvis.ds.apigw.v1.biz.cti.winker.repository.CtiWinkerRepository;
import com.soluvis.ds.apigw.v1.lib.winker.engine.WinkerConst;
import com.soluvis.ds.apigw.v1.lib.winker.manager.WinkerClientManager;
import com.soluvis.ds.apigw.v1.util.LangUtil;

/**
 * @Class 		: DefaultEventListener
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  Winker 이벤트 Listner
 * 
 *  EVENT_AGENT_CREATED: 사용자 정보의 ctiCreateCd를 Y로 업데이트
 *  EVENT_AGENT_DELETED: 사용자 정보의 ctiCreateCd를 N으로 업데이트
 *  EVENT_SKILL_ADDED: removeJob
 *  EVENT_SKILL_DELETED: removeJob
 *  EVENT_SKILL_UPDATED: removeJob
 *  EVENT_ERROR: Job 메세지 업데이트
 */
public class DefaultEventListener implements WinkerEventListener{
	
	static final Logger logger = LoggerFactory.getLogger(DefaultEventListener.class);
	
	CtiWinkerRepository ctiWinkerRepository;
	public void setRepository(CtiWinkerRepository ctiWinkerRepository) {
		this.ctiWinkerRepository = ctiWinkerRepository;
	}
	
	@Override
	public void onWinkEvent(int eventID, String eventData, String param1, String param2, String param3, String param4) {
		logger.info("eventID[{}] eventName[{}] eventData[{}] param1[{}] param2[{}] param3[{}] param4[{}]", eventID, WinkerConst.getEVENT_NAME()[eventID], eventData, param1, param2, param3, param4);
		int lRef = LangUtil.toInt(param1);
		if(eventID == WinkerConst.EVENT_AGENT_CREATED) {
			String[] splitEventData = eventData.split("~", -1);
			String[] splitData = splitEventData[2].split(":", -1);
			String userid = splitData[1];
			
			Map<String,Object> queryParams = new HashMap<>();
			queryParams.put("userid", userid);
			ctiWinkerRepository.setCtiCreated(queryParams);
			logger.info("update CtiCreateCd Y [{}]", userid);
			WinkerClientManager.getJobMap().remove(lRef);
		} else if(eventID == WinkerConst.EVENT_AGENT_DELETED) {
			String[] splitEventData = eventData.split("~", -1);
			String[] splitData = splitEventData[2].split(":", -1);
			String userid = splitData[1];
			
			Map<String,Object> queryParams = new HashMap<>();
			queryParams.put("userid", userid);
			ctiWinkerRepository.setCtiDeleted(queryParams);
			logger.info("update CtiCreateCd N [{}]", userid);
			WinkerClientManager.getJobMap().remove(lRef);
		} else if(eventID == WinkerConst.EVENT_SKILL_ADDED) {
			String[] splitEventData = eventData.split("~", -1);
			String[] splitData = splitEventData[2].split(":", -1);
			String personDbid = splitData[0];
			String skillDbid = splitData[1];
			String skillLevel = splitData[2];
			
			logger.info("skill add person[{}] skill[{}] level[{}]", personDbid, skillDbid, skillLevel);
			WinkerClientManager.getJobMap().remove(lRef);
		} else if(eventID == WinkerConst.EVENT_SKILL_DELETED) {
			String[] splitEventData = eventData.split("~", -1);
			String[] splitData = splitEventData[2].split(":", -1);
			String personDbid = splitData[0];
			String skillDbid = splitData[1];
			
			logger.info("skill delete person[{}] skill[{}] ", personDbid, skillDbid);
			WinkerClientManager.getJobMap().remove(lRef);
		} else if(eventID == WinkerConst.EVENT_SKILL_UPDATED) {
			String[] splitEventData = eventData.split("~", -1);
			String[] splitData = splitEventData[2].split(":", -1);
			String personDbid = splitData[0];
			String skillDbid = splitData[1];
			String skillLevel = splitData[2];
			
			logger.info("skill update person[{}] skill[{}] level[{}]", personDbid, skillDbid, skillLevel);
			WinkerClientManager.getJobMap().remove(lRef);
		} else if(eventID == WinkerConst.EVENT_ERROR) {
			
			String errMsg = param3;
			
			if(errMsg.indexOf("Link is a singular") > -1) {
				String[] splitEventData = eventData.split("~", -1);
				String[] splitData = splitEventData[2].split(":", -1);
				String userid = splitData[1];
				
				Map<String,Object> queryParams = new HashMap<>();
				queryParams.put("userid", userid);
				ctiWinkerRepository.setCtiCreated(queryParams);
				logger.info("update CtiCreateCd Y [{}]", userid);
			} else if(errMsg.indexOf("Can not find person") > -1) {
				String userid = param3.substring(param3.indexOf("=")+1, param3.length());
				
				Map<String,Object> queryParams = new HashMap<>();
				queryParams.put("userid", userid);
				ctiWinkerRepository.setCtiDeleted(queryParams);
				logger.info("update CtiCreateCd N [{}]", userid);
			}
			
			WinkerClientManager.getJobMap().put(lRef, errMsg);
			
			Thread.ofVirtual().start(() -> {
				int jobRef = lRef;
			    try {
			        Thread.sleep(30_000L);
			        WinkerClientManager.getJobMap().remove(jobRef);
			    } catch (InterruptedException e) {
			    	logger.error("", e);
					Thread.currentThread().interrupt();
			    }
			});
		}
		
	}
}
