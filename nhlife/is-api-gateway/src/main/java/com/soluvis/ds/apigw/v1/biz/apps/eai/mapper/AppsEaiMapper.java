package com.soluvis.ds.apigw.v1.biz.apps.eai.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * @Class 		: AppsEaiMapper
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  APPS EAI Mapper
 */
@Mapper
public interface AppsEaiMapper {
	public int insertEaiTmBizDataTemp(Map<String,Object> params);
	public int mergeEaiTmBizData();
	
	public List<Map<String,Object>> getBrCdList();
	
	public int callSumAppTmAgentTime(Map<String,Object> params);
	public int callSumAppTmAgentDay(Map<String,Object> params);
	public int callSumAppTmTeamDay(Map<String,Object> params);
	public int callSumAppTmTeamMonth(Map<String,Object> params);
}
