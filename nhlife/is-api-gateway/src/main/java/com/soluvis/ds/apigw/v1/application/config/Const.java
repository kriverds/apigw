package com.soluvis.ds.apigw.v1.application.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @Class 		: Const
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  Application Const Class
 */
public final class Const {

	Const() {}
	
	public static final String APIGW_KEY_RESULT_CD = "resultCd";
	public static final String APIGW_KEY_RESULT_MSG = "resultMsg";
	public static final String APIGW_KEY_BYTEBUF = "bytebuf";
	
	public static final String APIGW_SUCCESS_CD = "Y";
	public static final String APIGW_SUCCESS_MSG = "Success";
	
	public static final String APIGW_FAIL_CD = "N";
	public static final String APIGW_FAIL_MSG = "Fail";
	
	public static final byte EAI_STX = 0x02;
	public static final byte EAI_ETX = 0x03;
	
	public static final String EAI_NO_SEARCH_MSG = "조회된 데이터가 없습니다.";
	
	public static final Charset EAI_CHARSET = Charset.forName("EUC-KR");
	
	public static final Charset NAS_CHARSET = StandardCharsets.UTF_8;
	
	public static final int DB_BATCH_SIZE = 1000;
	
	public static final String SERVICE_LIST_KEY = "list";
	
	public static final String HTTP_ERROR_CD_KEY = "errCd";
	public static final String HTTP_ERROR_MSG_KEY = "errMsg";
	
	public static final long WINKER_EVENT_CHECK_INTERVAL = 200L;
	public static final int WINKER_EVENT_CHECK_MAX_TRY_CNT = 300;

}
