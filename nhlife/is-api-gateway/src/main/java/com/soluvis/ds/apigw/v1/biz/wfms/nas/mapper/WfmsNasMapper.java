package com.soluvis.ds.apigw.v1.biz.wfms.nas.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.CdInf;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.GroupLimitBizKind;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.SumChatGroupDay;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.UserEvlt;

/**
 * @Class 		: WfmsNasMapper
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  WFMS NAS Mapper
 */
@Mapper
public interface WfmsNasMapper {
	public List<CdInf> selectCdInf();
	
	public int insertGroupLimitBizKindTemp(List<GroupLimitBizKind> voList);
	public int mergeGroupLimitBizKind();
	
	public void getSumAgent30min(Map<String,Object> params);
	public void getSumAgentDay(Map<String,Object> params);
	public List<SumChatGroupDay> selectSumChatGroupDay(Map<String,Object> params);
	public void getSumHo30min(Map<String,Object> params);
	public void getSumMrpMonth(Map<String,Object> params);
	public void getSumReason30min(Map<String,Object> params);
	
	public int insertUserEvltTemp(List<UserEvlt> voList);
	public int updateUserEvlt();
}