package com.soluvis.ds.apigw.v1.application.common.service;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.common.repository.CommonRepository;

/**
 * @Class 		: CommonService
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  어플리케이션 공통기능 서비스
 */
@Service
public class CommonService {
	
	static final Logger logger = LoggerFactory.getLogger(CommonService.class);
	
	/**
	 * 스프링 DI
	 */
	CommonRepository commonRepository;
	public CommonService(CommonRepository commonRepository) {
		this.commonRepository = commonRepository;
	}
	
	/**
	 * @Method		: startBatchLog
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  배치시작 정보 전달 받아 DB에 적재
	 */
	public int startBatchLog(String service, String syskind, String batchName, UUID uuid) throws Exception {
		logger.info("[{}] startBatchLog[{}|{}]", uuid, syskind, batchName);
		Map<String,Object> queryParam = new HashMap<>();
		queryParam.put("service", service);
		queryParam.put("uuid", uuid.toString());
		queryParam.put("syskind", syskind);
		queryParam.put("batchName", batchName);
		queryParam.put("server", InetAddress.getLocalHost().getHostName());
		return commonRepository.startBatchLog(queryParam);
	}
	
	/**
	 * @Method		: endBatchLog
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  배치종료 정보 전달 받아 DB 업데이트
	 */
	public int endBatchLog(String status, String message, UUID uuid) {
		logger.info("[{}] endBatchLog[{}|{}]", uuid, status, message);
		Map<String,Object> queryParam = new HashMap<>();
		queryParam.put("uuid", uuid.toString());
		queryParam.put("status", status);
		queryParam.put("message", message);
		return commonRepository.endBatchLog(queryParam);
	}
}