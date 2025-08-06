package com.soluvis.ds.apigw.v1.biz.apps.rest.config;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.soluvis.ds.apigw.v1.application.common.service.CommonService;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Class 		: AppsRestExceptionHandler
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  AppsRestController Exception 핸들링 클래스
 * 
 *  1. com.soluvis.ds.apigw.v1.apps.rest.controller 패키지에서 에러 발생 시 호출
 */
@ControllerAdvice("com.soluvis.ds.apigw.v1.biz.apps.rest.controller")
public class AppsRestExceptionHandler {
	
	/**
	 * 스프링 DI
	 */
	CommonService commonService;

	public AppsRestExceptionHandler(CommonService commonService) {
		this.commonService = commonService;
	}

	static final Logger log = LoggerFactory.getLogger(AppsRestExceptionHandler.class);

	@ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleException(HttpServletRequest request, Exception ex) {
		UUID uuid = (UUID)request.getAttribute("uuid");
		
		Map<String,Object> resultMap = CommonUtil.commonException(ex, uuid);
		
		commonService.endBatchLog("N", resultMap.toString(), uuid);
		HttpStatus hs = (HttpStatus) resultMap.get("error");

        return new ResponseEntity<>(resultMap, hs);
    }
}
