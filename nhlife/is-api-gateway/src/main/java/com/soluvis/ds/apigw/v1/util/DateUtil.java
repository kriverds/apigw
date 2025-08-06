package com.soluvis.ds.apigw.v1.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Class 		: DateUtil
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  날짜 관련 Util Class
 */
public class DateUtil {
	
	DateUtil() {}
	
	static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

	/**
	 * @Method		: getDateString
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  패턴을 입력받아 현재 날짜 String 출력
	 */
	public static String getDateString(String pattern) {
		return getDateString(pattern, new Date());
	}
	
	/**
	 * @Method		: getDateString
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  패턴과 기간을 입력받아 해당하는 날짜 String 출력
	 */
	public static String getDateString(String pattern, int period) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, period);
		return getDateString(pattern, cal.getTime());
	}
	
	/**
	 * @Method		: getDateString
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  패턴과 Date를 입력받아 해당하는 날짜 String 출력
	 */
	public static String getDateString(String pattern, Date date) {
		String result = "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			result = sdf.format(date);
		} catch (Exception e) {
			logger.error("Date parsing error patter[{}]", pattern);
		}
		return result;
	}
}
