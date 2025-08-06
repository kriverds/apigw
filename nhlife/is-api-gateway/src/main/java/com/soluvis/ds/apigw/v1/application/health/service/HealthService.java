package com.soluvis.ds.apigw.v1.application.health.service;

import java.io.File;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.application.health.repository.HealthRepository;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.config.ChatbotNasProperties;
import com.soluvis.ds.apigw.v1.biz.mobile.eai.service.MobileEaiService;
import com.soluvis.ds.apigw.v1.lib.winker.engine.WinkerConnector;
import com.soluvis.ds.apigw.v1.lib.winker.manager.WinkerClientManager;

/**
 * @Class 		: HealthService
 * @date   		: 2025. 2. 18.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 * 헬스체크 Service
 */
@Service
public class HealthService {
	
	static final Logger logger = LoggerFactory.getLogger(HealthService.class);
	
	@Value("${nas.base.directory}")
	String baseDirectory;

	/**
	 * 스프링 DI
	 */
	HealthRepository healthRepository;
	MobileEaiService mobileEaiService;
	ChatbotNasProperties chatbotNasProperties;
	WinkerClientManager winkerClientManager;
	public HealthService(HealthRepository healthRepository, MobileEaiService mobileEaiService, ChatbotNasProperties chatbotNasProperties, WinkerClientManager winkerClientManager) {
		this.healthRepository = healthRepository;
		this.mobileEaiService = mobileEaiService;
		this.chatbotNasProperties = chatbotNasProperties;
		this.winkerClientManager = winkerClientManager;
	}
	
	/**
	 * @Method		: isDbOpen
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  DB 연결 여부 확인
	 */
	public boolean isDbOpen() {
		boolean open = true;
		try {
			int iResult = healthRepository.selectDual();
			if(iResult < 1) {
				open = false;
			}
		} catch (Exception e) {
			logger.error("", e);
			open = false;
		}
		
		return open;
	}
	
	/**
	 * @Method		: isWinkerOpen
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Winker Websocket 연결 여부 확인
	 */
	public boolean isWinkerOpen() {
		boolean open = true;
		WinkerConnector connector;
		connector = winkerClientManager.getInstance();
		open = connector.isOpen();
		logger.info("{}", connector.isOpen());
		return open;
	}
	
	/**
	 * @Method		: isEaiOpen
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  EAI 통신 여부 확인
	 *  제일 간단한 EAI인 모바일앱 대기고객수 전송으로 확인
	 */
	public boolean isEaiOpen() {
		boolean open = true;
		try {
			JSONObject executeResult = mobileEaiService.executeIVZZMOZZSH001();
			String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
			logger.info("{}", resultCd);
			if(resultCd.equals(Const.APIGW_FAIL_CD)) {
				open = false;
			}
		} catch (Exception e) {
			logger.error("", e);
			open = false;
		}
		return open;
	}
	
	/**
	 * @Method		: isNasOpen
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  NAS 경로 접근 가능 확인
	 */
	public boolean isNasOpen() {
		String path = baseDirectory + chatbotNasProperties.cbStatsInfo().directory();
		File file = new File(path);
		return file.exists();
	}
}
