package com.soluvis.ds.apigw.v1.application.health.controller;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Iterator;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soluvis.ds.apigw.v1.application.health.service.HealthService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Class 		: HealthController
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  헬스체크용 Controller
 */
@RestController
@RequestMapping("/health")
public class HealthController {
	
	static final Logger logger = LoggerFactory.getLogger(HealthController.class);

	/**
	 * 스프링 DI
	 */
	HealthService healthService;
	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}
	
	
	/**
	 * @Method		: getSimpleCheck
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  서버의 현재 시각을 반환하는 헬스체크
	 */
	@GetMapping(value = "", produces = "application/json; charset=UTF-8")
	public String getSimpleCheck(HttpServletRequest request) throws Exception{
		JSONObject jResult = new JSONObject();
		jResult.put("timeStamp", LocalDateTime.now());
		jResult.put("hostName", InetAddress.getLocalHost().getHostName());
		jResult.put("hostIP", InetAddress.getLocalHost().getHostAddress());
		logger.info("getSimpleCheck {}", jResult);
		return jResult.toString();
	}
	
	/**
	 * @Method		: getHeaderTest
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  서버의 request header 정보를 반환하는 헬스체크 (헤더 확인용)
	 *  - 아파치를 통해서 호출하는것과 로컬에서 호출하는 것의 헤더 정보가 다름.
	 */
	@GetMapping(value = "/header", produces = "application/json; charset=UTF-8")
	public String getHeaderTest(HttpServletRequest request) throws Exception{
		Iterator<String> it = request.getHeaderNames().asIterator();
		JSONObject jResult = new JSONObject();
		
		while(it.hasNext()) {
			String key = it.next();
			jResult.put(key, request.getHeader(key));
		}
		jResult.put("timeStamp", LocalDateTime.now());
		jResult.put("hostName", InetAddress.getLocalHost().getHostName());
		jResult.put("hostIP", InetAddress.getLocalHost().getHostAddress());
		logger.info("getHeaderTest {}", jResult);
		return jResult.toString();
	}
	
	/**
	 * @Method		: getLogicCheck
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  서버에 인터페이스 대상 정상 여부를 확인하는 헬스체크
	 *  1. DB 연결여부 확인
	 *  2. EAI 연결여부 확인
	 *  3. NAS 연결여부 확인
	 *  4. Winker 연결여부 확인
	 *  5. 서버 현재시각
	 *  6. 호스트 명/IP
	 */
	@GetMapping(value = "/detail", produces = "application/json; charset=UTF-8")
	public String getLogicCheck(HttpServletRequest request) throws Exception{
		JSONObject jResult = new JSONObject();
		jResult.put("DB", healthService.isDbOpen());
		jResult.put("EAI", healthService.isEaiOpen());
		jResult.put("NAS", healthService.isNasOpen());
		jResult.put("WINKER", healthService.isWinkerOpen());
		jResult.put("timeStamp", LocalDateTime.now());
		jResult.put("hostName", InetAddress.getLocalHost().getHostName());
		jResult.put("hostIP", InetAddress.getLocalHost().getHostAddress());
		logger.info("getLogicCheck {}", jResult);
		return jResult.toString();
	}
	
}
