package com.soluvis.ds.apigw.v1.util;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @Class 		: ObjectMapperUtil
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  ObjectMapper Util Class
 */
public class ObjectMapperUtil {

	ObjectMapperUtil() {}

	static final Logger logger = LoggerFactory.getLogger(ObjectMapperUtil.class);
	static final ObjectMapper om = new ObjectMapper();

	
	/**
	 * @Method		: listVoToJsonArray
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  List<VO> >> JSONArray
	 */
	public static JSONArray listVoToJsonArray(List<?> voMap) {
		JSONArray ja;
		try {
			ja = new JSONArray(om.writeValueAsString(voMap));
		} catch (Exception e) {
			logger.error("", e);
			ja = new JSONArray();
		}
		return ja;
	}

	/**
	 * @Method		: listMapToJsonArray
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  List<Map> >> JSONArray
	 */
	public static JSONArray listMapToJsonArray(List<Map<String, Object>> listMap, boolean logFlag) throws Exception {
		JSONArray ja = new JSONArray(om.writeValueAsString(listMap));
		if (logFlag) {
			logger.info("{}", ja);
		}
		return ja;
	}
	
	/**
	 * @Method		: mapToJsonObject
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Map >> JSONObject
	 */
	public static JSONObject mapToJsonObject(Map<String, Object> map, boolean logFlag) throws Exception {
		JSONObject jo = new JSONObject(om.writeValueAsString(map));
		if (logFlag) {
			logger.info("{}", jo);
		}
		return jo;
	}
	
	/**
	 * @Method		: objectToByteArray
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Object >> byte[]
	 */
	public static byte[] objectToByteArray(Object obj, boolean logFlag) throws Exception {
		byte[] arrByte = om.writeValueAsBytes(obj);
		
		if (logFlag) {
			logger.info("{}", new String(arrByte));
		}
		return arrByte;
	}

}
