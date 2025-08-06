package com.soluvis.ds.apigw.v1.util;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.soluvis.ds.apigw.v1.application.config.Const;

/**
 * @Class 		: LangUtil
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  LANG Util Class
 */
public class LangUtil {
	
	LangUtil() {}
	
	static final Logger logger = LoggerFactory.getLogger(LangUtil.class);
	
	/**
	 * @Method		: rPad
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  오른쪽을 특정 문자열로 특정 길이만큼 채운다
	 */
	public static String rPad(String source, String padStr, int len) {
		if(len < 1) {
			return "";
		}
		
		String resultStr = "";
		int sourceLen = source.length();
		
		if(sourceLen < len) {
			StringBuilder sb = new StringBuilder();
			sb.append(source);
			for (int i = sourceLen; i < len; i++) {
				sb.append(padStr);
			}
			resultStr = sb.toString();
		} else if(sourceLen == len) {
			resultStr = source;
		} else {
			resultStr = source.substring(0, len);
		}
		
		return resultStr;
	}
	
	/**
	 * @Method		: lPad
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  왼쪽을 특정 문자열로 특정 길이만큼 채운다
	 */
	public static String lPad(String source, String padStr, int len) {
		if(len < 1) {
			return "";
		}
		
		String resultStr = "";
		int sourceLen = source.length();
		
		if(sourceLen < len) {
			Deque<String> deque = new ArrayDeque<>();
			deque.addFirst(source);
			for (int i = sourceLen; i < len; i++) {
				deque.addFirst(padStr);
			}
			resultStr = String.join("", deque);
		} else if(sourceLen == len) {
			resultStr = source;
		} else {
			resultStr = source.substring(0, len);
		}
		
		return resultStr;
	}
	
	/**
	 * @Method		: rSpacePad
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  오른쪽을 스페이스로 특정 길이만큼 채운다
	 */
	public static String rSpacePad(String source, int len) {
		if(len < 1) {
			return "";
		}
		
		String resultStr = "";
		int sourceLen = source.length();
		
		if(sourceLen < len) {
			StringBuilder sb = new StringBuilder();
			sb.append(source);
			for (int i = sourceLen; i < len; i++) {
				sb.append(" ");
			}
			resultStr = sb.toString();
		} else if(sourceLen == len) {
			resultStr = source;
		} else {
			resultStr = source.substring(0, len);
		}
		
		return resultStr;
	}
	
	/**
	 * @Method		: splitByteArray
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  byte[]를 잘라서 String으로 변환
	 */
	public static String splitByteArray(byte[] source, int start, int len) {
		if(start+len > source.length) {
			logger.error("over size splitByteArray: source length[{}] split length[{}]", source.length, start+len);
			logger.error("source[{}]", new String(source, Charset.forName("EUC-KR")));
			return "";
		}
		
		byte[] result = new byte[len];
		System.arraycopy(source, start, result, 0, len);
		
		return new String(result, Const.EAI_CHARSET);
	}
	
	/**
	 * @Method		: toInt
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  ParseException 방지
	 */
	public static int toInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (Exception e) {
			logger.error("{}", e.getMessage());
			return 0;
		}
	}
}
