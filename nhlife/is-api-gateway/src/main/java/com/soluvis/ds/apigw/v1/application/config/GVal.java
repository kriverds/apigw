package com.soluvis.ds.apigw.v1.application.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @Class 		: GVal
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  글로벌 변수 관리 클래스
 */
public class GVal {

	GVal() {}

	@Getter
	@Setter
	static String nasSuccessDirectory;
	@Getter
	@Setter
	static String nasFailDirectory;
	
	@Getter
	@Setter
	static String eaiHost;
}
