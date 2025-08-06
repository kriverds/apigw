package com.soluvis.ds.apigw.v1.biz.bake.rest.feign;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Class 		: BakeRestService
 * @date   		: 2025. 2. 27.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  Bake 사용자 생성 삭제 Feign 서비스
 */
@FeignClient(name = "BakeRestService", url = "${bake.url}")
public interface BakeRestFeignService {

	@PostMapping(value="/api/v1/usersapi/save", produces="application/json; charset=UTF-8")
	String saveUser(@RequestBody List<Map<String,Object>> param); // Bake 사용자 생성
	
	@PostMapping(value="/api/v1/usersapi/password", produces="application/json; charset=UTF-8")
	String initPassword(@RequestBody List<Map<String,Object>> param); // Bake 계정 비밀번호 초기화

	@PostMapping(value="/api/v1/usersapi/delete", produces="application/json; charset=UTF-8")
	String deleteUser(@RequestBody List<Map<String,Object>> param); // Bake 사용자 삭제
}