package com.soluvis.ds.apigw.v1.biz.chatbot.nas.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.CbStatsInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.CmStatsInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.HolInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.SmsSendResult;

/**
 * @Class 		: ChatbotNasMapper
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CHATBOT NAS Mapper
 */
@Mapper
public interface ChatbotNasMapper {
	public int insertCbStatsInfoTemp(List<CbStatsInfo> voList);
	public int mergeCbStatsInfo();
	
	public int insertCmStatsInfoTemp(List<CmStatsInfo> voList);
	public int mergeCmStatsInfo();
	
	public int mergeSumChatbotCampaign();
	public int mergeSumChatbotExecCampaign();
	
	public List<HolInfo> selectHolInfo();
	
	public int insertSmsSendResultTemp(List<SmsSendResult> voList);
	public int mergeSmsSendResult();
	
}
