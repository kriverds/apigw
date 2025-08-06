package com.soluvis.ds.apigw.v1.lib.winker.manager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.biz.cti.winker.repository.CtiWinkerRepository;
import com.soluvis.ds.apigw.v1.lib.winker.engine.WinkerConnector;

import lombok.Getter;
import lombok.Setter;

/**
 * @Class 		: WinkerClientManager
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  Winker 매니지먼트 클래스
 */
@Component
public class WinkerClientManager {
	
	static final Logger logger = LoggerFactory.getLogger(WinkerClientManager.class);
	
	/**
	 * 스프링 DI
	 */
	CtiWinkerRepository ctiWinkerRepository;

	public WinkerClientManager(CtiWinkerRepository ctiWinkerRepository) {
		this.ctiWinkerRepository = ctiWinkerRepository;
	}
	
	@Value("${winker.host}")
	String winkerHost;
	@Value("${winker.port}")
	String winkerPort;
	@Value("${winker.protocol}")
	String winkerProtocol;
	@Value("${winker.path}")
	String winkerPath;
	
	@Getter
	@Setter
	static Map<Integer,String> jobMap = new HashMap<>();
	
	static WinkerConnector instance;
	
	/**
	 * @Method		: getInstance
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  WinkerConnector를 Singleton으로 관리
	 * 
	 *  1. instance가 생성 안됐을 경우 생성
	 *  2. instance가 닫혔을 경우 재연결
	 */
	public WinkerConnector getInstance() {
		if(instance == null) {
			String winkerUrl = winkerProtocol + "://" + winkerHost + ":" + winkerPort + winkerPath;
			logger.info("Create WinkerClient URL: {}", winkerUrl);
			URI uri;
			try {
				uri = new URI(winkerUrl);
				instance = new WinkerConnector(uri);
				instance.setRepository(ctiWinkerRepository);
				instance.setEventListener();
				instance.connectWinker();
			} catch (URISyntaxException e) {
				logger.error("", e);
			}
		} else if(instance.isClosed()) {
			logger.info("Reconnect WinkerClient");
			instance.reconnectWinker();
		}
		
		return instance;
	}
	
	/**
	 * @Method		: isOpen
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Winker Connection 오픈 여부
	 */
	public boolean isOpen() {
		return instance.isOpen();
	}
}