package com.soluvis.ds.apigw.v1.util;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.ibatis.type.TypeException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.BadSqlGrammarException;

/**
 * @Class 		: CommonUtil
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  공통 유틸 클래스
 */
public class CommonUtil {

	CommonUtil() {}
	
	static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

	/**
	 * @Method		: getJString
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  JSON.getString NPE 방지
	 */
	public static String getJString(JSONObject parents, String param) {
		try {
			String result = parents.get(param).toString();
			if ("null".equals(result)) {
				result = "";
			}
			return result;
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * @Method		: getJLong
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  JSON ParseException 방지
	 */
	public static long getJLong(JSONObject parents, String param) {
		try {
			return Long.parseLong(parents.get(param).toString());
		} catch (Exception e) {
			return 0L;
		}
	}
	/**
	 * @Method		: getJInt
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  JSON ParseException 방지
	 */
	public static int getJInt(JSONObject parents, String param) {
		try {
			return Integer.parseInt(parents.get(param).toString());
		} catch (Exception e) {
			return 0;
		}
	}
	/**
	 * @Method		: getJJSONObject
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  JSON ParseException 방지
	 */
	public static JSONObject getJJSONObject(JSONObject parents, String param) {
		try {
			return parents.getJSONObject(param);
		} catch (Exception e) {
			return new JSONObject();
		}
	}
	/**
	 * @Method		: getJJSONArray
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  JSON ParseException 방지
	 */
	public static JSONArray getJJSONArray(JSONObject parents, String param) {
		try {
			return parents.getJSONArray(param);
		} catch (Exception e) {
			return new JSONArray();
		}
	}

	/**
	 * @Method		: commonException
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  공통 예외처리 메서드
	 *  Exception을 JSON형태로 생성
	 */
	public static Map<String,Object> commonException(Exception ex, UUID uuid) {
		logger.error("[{}] ###Error", uuid, ex);
		Map<String,Object> resultMap = new HashMap<>();
		HttpStatus hs = HttpStatus.BAD_REQUEST;
		resultMap.put("timestamp", LocalDateTime.now());

		String errorMsg = "";
		String errDtlMsg = "";
		try {
			if(ex instanceof ClassCastException) {
				hs = HttpStatus.BAD_REQUEST;
				errorMsg = "check parameter type";
				errDtlMsg = ex.getMessage();
			} else if(ex instanceof SQLRecoverableException) {
				hs = HttpStatus.INTERNAL_SERVER_ERROR;
				errorMsg = "DB Connection ERROR";
				errDtlMsg = ex.getMessage();
			} else if(ex instanceof MyBatisSystemException || ex instanceof TypeException) {
				hs = HttpStatus.INTERNAL_SERVER_ERROR;
				errorMsg = "Mybatis ERROR";
				errDtlMsg = ex.getMessage();
			} else if(ex instanceof SQLException) {
				hs = HttpStatus.INTERNAL_SERVER_ERROR;
				errorMsg = "Database ERROR";
				errDtlMsg = ex.getMessage();
			}else if(ex instanceof BadSqlGrammarException) {
				hs = HttpStatus.INTERNAL_SERVER_ERROR;
				errorMsg = "SQL ERROR";
				errDtlMsg = ex.getMessage();
			} else if(ex instanceof JSONException) {
				hs = HttpStatus.BAD_REQUEST;
				errorMsg = "JSON ERROR";
				errDtlMsg = ex.getMessage();
			} else {
				hs = HttpStatus.INTERNAL_SERVER_ERROR;
				errDtlMsg = ex.getMessage();
			}
		} catch (Exception e) {
			hs = HttpStatus.INTERNAL_SERVER_ERROR;
			errorMsg = "DB Connection ERROR";
			errDtlMsg = e.getMessage();
		}
		resultMap.put("status", hs.value());
		resultMap.put("error", hs);
		resultMap.put("errCd", "1");
		resultMap.put("errMsg", errorMsg);
		resultMap.put("detailMsg", errDtlMsg);
		logger.error("[{}] {}", uuid, resultMap);
		return resultMap;
	}

	/**
	 * @Method		: encodeBase64AsString
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  String >> Base64String
	 */
	public static String encodeBase64AsString(String str) {
		Base64 base64 = new Base64();
		return base64.encodeAsString(str.getBytes());
	}
	/**
	 * @Method		: encodeBase64AsByte
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  byte[] >> Base64byte[]
	 */
	public static byte[] encodeBase64AsByte(byte[] bt) {
		Base64 base64 = new Base64();
		return  base64.encode(bt);
	}
}
