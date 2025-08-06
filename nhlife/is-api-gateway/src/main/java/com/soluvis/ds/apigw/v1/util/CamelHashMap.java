package com.soluvis.ds.apigw.v1.util;

import java.util.LinkedHashMap;

import com.google.common.base.CaseFormat;

/**
 * @Class 		: CamelHashMap
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  Mybatis Result를 Camel Case 형태로 받기 위한 클래스
 *  - 그냥 Map으로 받을 시 오라클은 컬럼이 기본으로 대문자로 인식되기 때문에 Map 파라미터가 대문자로 리턴됨.
 */
public class CamelHashMap extends LinkedHashMap<Object, Object> {

	static final long serialVersionUID = 1L;

	@Override
    public Object put(Object key, Object value) {
        return super.put(toLowerCamel((String) key), value);
    }

    static String toLowerCamel(String key) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key);
    }
}