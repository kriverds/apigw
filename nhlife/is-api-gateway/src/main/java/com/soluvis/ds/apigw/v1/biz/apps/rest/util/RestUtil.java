package com.soluvis.ds.apigw.v1.biz.apps.rest.util;

import org.json.JSONObject;

public class RestUtil {

	RestUtil() {}
	
	/**
	 * @Method		: returnCheckParameter
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  파라미터 없는 경우 처리
	 */
	public static JSONObject returnNoParameter(String message) {
		JSONObject jResult = new JSONObject();
		jResult.put("errCd", "400");
		jResult.put("errMsg", message+" parameter does not exist");
		return jResult;
	}
	
	public static JSONObject returnWrongParameter(String message) {
		JSONObject jResult = new JSONObject();
		jResult.put("errCd", "400");
		jResult.put("errMsg", message+" parameter wrong");
		return jResult;
	}

}
