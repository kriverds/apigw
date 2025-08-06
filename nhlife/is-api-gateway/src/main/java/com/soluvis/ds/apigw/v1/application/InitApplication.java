package com.soluvis.ds.apigw.v1.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.application.config.GVal;
import com.soluvis.ds.apigw.v1.lib.winker.manager.WinkerClientManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * @Class 		: InitApplication
 * @date   		: 2025. 2. 18.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. Application 실행 시 global valiable 설정
 *  2. init method 실행
 */
@Component
public class InitApplication {
	static final Logger logger = LoggerFactory.getLogger(InitApplication.class);
	
	/**
	 * 스프링 DI
	 */
	WinkerClientManager winkerClientManager;
	public InitApplication(WinkerClientManager winkerClientManager) {
		this.winkerClientManager = winkerClientManager;
	}
	
	@Value("${spring.profiles.active}")
	String activeProfile;
	
	@Value("${nas.base.directory.success}")
	String nasSuccessDirectory;
	@Value("${nas.base.directory.fail}")
	String nasFailDirectory;
	
	@Value("${eai.host.primary}")
    String eaiHostP;

	/**
	 * @Method		: init
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Application 실행 시 최초 한번 실행되는 메서드
	 * 
	 *  1. Application 전역에서 사용할 글로벌 변수를 설정한다.
	 *  2. Winker Websocket을 연결한다.
	 */
	@PostConstruct
	public void init() {
		logger.info("spring.profiles.active[{}]", activeProfile);
		logger.info("{}", "@PostConstruct");
		
		setGVal();
		connectWinker();
	}
	
	@PreDestroy
	public void stop() {
		logger.info("{}", "@PreDestroy");
		winkerClientManager.getInstance().close();
	}
	
	/**
	 * @Method		: setGVal
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  글로벌 전역 변수 설정
	 */
	void setGVal() {
		GVal.setNasSuccessDirectory(nasSuccessDirectory);
		GVal.setNasFailDirectory(nasFailDirectory);
		GVal.setEaiHost(eaiHostP);
	}
	
	/**
	 * @Method		: connectWinker
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Application 실행 시 Winker Websocket 연결
	 */
	void connectWinker() {
		winkerClientManager.getInstance();
	}
}
