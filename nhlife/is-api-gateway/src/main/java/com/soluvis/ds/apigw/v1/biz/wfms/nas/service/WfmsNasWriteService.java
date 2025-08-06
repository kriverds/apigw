package com.soluvis.ds.apigw.v1.biz.wfms.nas.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.common.nas.util.NasUtil;
import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.config.WfmsNasProperties;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.repository.WfmsNasRepository;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.SumAgent30min;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.SumAgentDay;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.SumHo30min;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.SumMrpMonth;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.SumReason30min;
import com.soluvis.ds.apigw.v1.util.DateUtil;
import com.soluvis.ds.apigw.v1.util.FileUtil;
import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Setter;

/**
 * @Class 		: WfmsNasWriteService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  WFMS와 연동하는 데이터를 Nas로 파일적재한다.
 */
@Service
public class WfmsNasWriteService {
	
	enum Job{
		CD_INF,					//WFMS 예측그룹코드
		SUM_AGENT_30MIN,		//WFMS 상담사 콜정보 30분
		SUM_AGENT_DAY,			//WFMS 상담사 실적정보 일
		SUM_CHAT_GROUP_DAY,		//WFMS 채팅 집계정보 일
		SUM_HO_30MIN,			//WFMS CTIQ 콜정보 30분
		SUM_MRP_MONTH,			//WFMS 대표번호 응대현황 월
		SUM_REASON_30MIN		//WFMS 상담사 상태정보 30분
	}
	
	static final Logger logger = LoggerFactory.getLogger(WfmsNasWriteService.class);
	@Value("${nas.base.directory}")
	String baseDirectory;
	@Value("${nas.base.directory.success}")
	String successDirectory;
	@Value("${nas.base.directory.fail}")
	String failDirectory;

	/**
	 * 스프링 DI
	 */
	WfmsNasRepository wfmsNasRepository;
	WfmsNasProperties wfmsNasProperties;
	public WfmsNasWriteService(WfmsNasRepository wfmsNasRepository, WfmsNasProperties wfmsNasProperties) {
		this.wfmsNasRepository = wfmsNasRepository;
		this.wfmsNasProperties = wfmsNasProperties;
	}
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeCdInf
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeCdInf() {
		return executeWrite(Job.CD_INF);
	}
	/**
	 * @Method		: executeSumAgent30min
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeSumAgent30min() {
		return executeWrite(Job.SUM_AGENT_30MIN);
	}
	/**
	 * @Method		: executeSumAgentDay
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeSumAgentDay() {
		return executeWrite(Job.SUM_AGENT_DAY);
	}
	/**
	 * @Method		: executeSumChatGroupDay
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeSumChatGroupDay() {
		return executeWrite(Job.SUM_CHAT_GROUP_DAY);
	}
	/**
	 * @Method		: executeSumHo30min
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeSumHo30min() {
		return executeWrite(Job.SUM_HO_30MIN);
	}
	/**
	 * @Method		: executeSumMrpMonth
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeSumMrpMonth() {
		return executeWrite(Job.SUM_MRP_MONTH);
	}
	/**
	 * @Method		: executeSumReason30min
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeSumReason30min() {
		return executeWrite(Job.SUM_REASON_30MIN);
	}
	
	
	/**
	 * @Method		: executeWrite
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 데이터 조회
	 *  2. 헤더 작성
	 *  3. 데이터 작성
	 *  4. 파일 쓰기
	 */
	JSONObject executeWrite(Job job) {
		String path = getPath(job);
		String fileName = getFileName(job);
		
		List<CommonNasWriteVo> voList = executeQuery(job);
		
		List<String> writeList = new ArrayList<>();
		writeList.add(getHeader(job));
		
		voList.forEach(vo -> writeList.add(vo.toFileString()));
		
		JSONObject writeResult = FileUtil.writeLines(path, fileName, writeList, uuid);
		String resultCd = writeResult.getString(Const.APIGW_KEY_RESULT_CD);
		
		if(Const.APIGW_SUCCESS_CD.equals(resultCd)) {
			return NasUtil.writeComplete(fileName, voList.size());
		} else {
			return writeResult;
		}
	}
	/**
	 * @Method		: getHeader
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job별로 파일 헤더문구 리턴
	 */
	String getHeader(Job job) {
		String result = "";
		switch(job) {
			case CD_INF -> result = "LrgCd|LrgNm|SmlCd|SmlNm|Deleted|UpCd";
			case SUM_AGENT_30MIN -> result = "HD|WK|DT|HH|MM|Level1Cd|Level2Cd|Level3Cd|PeripheralNumber|MRP|MenuCode|IB_RNA|IB|Answer10s|Answer20s|IB_TalkTime|OB_Try|OB_DelayTime|OB_Answer|OB_TalkTime|HoldTime|PDS|PDS_TalkTime|PT_Answer|PT_Request|PT_TalkTime|Hold|IB_MOD";
			case SUM_AGENT_DAY -> result = "DT|Level1Cd|Level2Cd|Level3Cd|Level1Nm|Level2Nm|Level3Nm|SortOrd|UserID|UserNm|WorkPeriod|IB_RNA|IB|Answer10s|Answer20s|LoginTime|IB_TalkTime|OB_Try|OB_DelayTime|OB_Answer|OB_TalkTime|CallTime|CallTime_AVG|GROUPRANK|IB_TalkTime_AVG|OB_Try_AVG|OB_Answer_AVG|Reason1_Time_AVG|TOTALCALL|HoldTime|PDS|PT_Answer|PT_Request|PT_TalkTime|Reason|Reason0|Reason1|Reason2|Reason3|Reason4|Reason5|Reason6|Reason7|Reason_Time|Reason1_Time|Reason2_Time|Reason3_Time|Reason4_Time|Reason5_Time|Reason6_Time|Reason7_Time|ReadyTime|Reason_Time_Total|Color|OB_TotalTime|OB_Total_AVG";
			case SUM_CHAT_GROUP_DAY -> result = "HD|DT|WK|MRP|accountGroupName|accountGroupId|AllCalls|ToAgent|Abandon|Callback|Answer|serviceLevelCnt";
			case SUM_HO_30MIN -> result = "HD|WK|DT|HH|MM|MRP|CTIQ|AllCalls|ToAgent|Abandon|Abandon10s|Abandon20s|Abandon30s|Abandon40s|Abandon50s|Abandon60s|Abandon120s|Abandon180s|Abandon300s|Callback|Answer|Answer10s|Answer20s|Answer30s|Answer40s|Answer50s|Answer60s|Answer120s|Answer180s|Answer300s|TalkTime|RingTime|ToVocalARS|ToVisualARS|FromVocalARS|FromVisualARS|CallEndAtQueue|NetQTime|LocalQTime";
			case SUM_MRP_MONTH -> result = "DT|Name|SL|ToAgent|Answer";
			case SUM_REASON_30MIN -> result = "HD|DT|WK|HH|MM|Level1Cd|Level2Cd|Level3Cd|PeripheralNumber|LoginTime|LogoutTime|Reason|Reason0|Reason1|Reason2|Reason3|Reason4|Reason5|Reason6|Reason7|Reason8|Reason9|Reason_Time|Reason0_Time|Reason1_Time|Reason2_Time|Reason3_Time|Reason4_Time|Reason5_Time|Reason6_Time|Reason7_Time|Reason8_Time|Reason9_Time|ReadyTime";
		}
		return result;
	}
	/**
	 * @Method		: getFileName
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job별로 파일명 리턴
	 */
	String getFileName(Job job) {
		String result = "";
		switch(job) {
			case CD_INF -> result = wfmsNasProperties.cdInf().filename() + "_" + DateUtil.getDateString(wfmsNasProperties.cdInf().pattern()) + wfmsNasProperties.cdInf().extension();
			case SUM_AGENT_30MIN -> result = wfmsNasProperties.sumAgent30min().filename() + "_" + DateUtil.getDateString(wfmsNasProperties.sumAgent30min().pattern()) + wfmsNasProperties.sumAgent30min().extension();
			case SUM_AGENT_DAY -> result = wfmsNasProperties.sumAgentDay().filename() + "_" + DateUtil.getDateString(wfmsNasProperties.sumAgentDay().pattern()) + wfmsNasProperties.sumAgentDay().extension();
			case SUM_CHAT_GROUP_DAY -> result = wfmsNasProperties.sumChatGroupDay().filename() + "_" + DateUtil.getDateString(wfmsNasProperties.sumChatGroupDay().pattern()) + wfmsNasProperties.sumChatGroupDay().extension();
			case SUM_HO_30MIN -> result = wfmsNasProperties.sumHo30min().filename() + "_" + DateUtil.getDateString(wfmsNasProperties.sumHo30min().pattern()) + wfmsNasProperties.sumHo30min().extension();
			case SUM_MRP_MONTH -> result = wfmsNasProperties.sumMrpMonth().filename() + "_" + DateUtil.getDateString(wfmsNasProperties.sumMrpMonth().pattern()) + wfmsNasProperties.sumMrpMonth().extension();
			case SUM_REASON_30MIN -> result = wfmsNasProperties.sumReason30min().filename() + "_" + DateUtil.getDateString(wfmsNasProperties.sumReason30min().pattern()) + wfmsNasProperties.sumReason30min().extension();
		}
		return result;
	}
	/**
	 * @Method		: getPath
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job별로 파일경로 리턴
	 */
	String getPath(Job job) {
		String result = baseDirectory;
		switch(job) {
			case CD_INF -> result += wfmsNasProperties.cdInf().directory();
			case SUM_AGENT_30MIN -> result += wfmsNasProperties.sumAgent30min().directory();
			case SUM_AGENT_DAY -> result += wfmsNasProperties.sumAgentDay().directory();
			case SUM_CHAT_GROUP_DAY -> result += wfmsNasProperties.sumChatGroupDay().directory();
			case SUM_HO_30MIN -> result += wfmsNasProperties.sumHo30min().directory();
			case SUM_MRP_MONTH -> result += wfmsNasProperties.sumMrpMonth().directory();
			case SUM_REASON_30MIN -> result += wfmsNasProperties.sumReason30min().directory();
		}
		return result;
	}
	/**
	 * @Method		: executeQuery
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job별로 DB조회
	 */
	@SuppressWarnings("unchecked")
	List<CommonNasWriteVo> executeQuery(Job job) {
		Map<String, Object> queryParams = new HashMap<>();
		List<CommonNasWriteVo> iResult = null;
		switch(job) {
			case CD_INF -> iResult = new ArrayList<>(wfmsNasRepository.getCdInf());
			case SUM_AGENT_30MIN -> {
				String strSdt = DateUtil.getDateString("yyyy-MM-dd");
				String strEdt = DateUtil.getDateString("yyyy-MM-dd");
				queryParams.put("sdt", strSdt);
				queryParams.put("edt", strEdt);
				logger.info("[{}] queryParams {}", uuid, queryParams);
				wfmsNasRepository.getSumAgent30min(queryParams);
				List<SumAgent30min> cursorMap = (List<SumAgent30min>)queryParams.get("cursor");
				iResult = new ArrayList<>(cursorMap);
			}
			case SUM_AGENT_DAY -> {
				String strSdt = DateUtil.getDateString("yyyy-MM-dd", -1);
				String strEdt = DateUtil.getDateString("yyyy-MM-dd", -1);
				queryParams.put("dateFrom", strSdt);
				queryParams.put("dateTo", strEdt);
				queryParams.put("arrCenterCd", "%");
				queryParams.put("arrGroupCd", "%");
				queryParams.put("arrTeamCd", "%");
				queryParams.put("arrAgentId", "%");
				queryParams.put("checkGb", "N");
				queryParams.put("syskind", "CS");
				logger.info("[{}] queryParams {}", uuid, queryParams);
				wfmsNasRepository.getSumAgentDay(queryParams);
				List<SumAgentDay> cursorMap = (List<SumAgentDay>)queryParams.get("cursor");
				iResult = new ArrayList<>(cursorMap);
			}
			case SUM_CHAT_GROUP_DAY -> {
				String strSdt = DateUtil.getDateString("yyyy-MM-dd", -1);
				String strEdt = DateUtil.getDateString("yyyy-MM-dd", -1);
				queryParams.put("sdt", strSdt);
				queryParams.put("edt", strEdt);
				logger.info("[{}] queryParams {}", uuid, queryParams);
				iResult = new ArrayList<>(wfmsNasRepository.getSumChatGroupDay(queryParams));
			}
			case SUM_HO_30MIN -> {
				String strSdt = DateUtil.getDateString("yyyy-MM-dd");
				String strEdt = DateUtil.getDateString("yyyy-MM-dd");
				queryParams.put("sdt", strSdt);
				queryParams.put("edt", strEdt);
				logger.info("[{}] queryParams {}", uuid, queryParams);
				wfmsNasRepository.getSumHo30min(queryParams);
				List<SumHo30min> cursorMap = (List<SumHo30min>)queryParams.get("cursor");
				iResult = new ArrayList<>(cursorMap);
			}
			/**
			 * 1~3일은 전월 조회
			 * 나머지는 당월 조회
			 */
			case SUM_MRP_MONTH -> {
				String dd = DateUtil.getDateString("dd");
				int idd = LangUtil.toInt(dd);
				Calendar eCal = Calendar.getInstance();
				if(idd < 4) {
					eCal.add(Calendar.MONTH, -1);
				}
				int lastDay = eCal.getActualMaximum(Calendar.DAY_OF_MONTH);
				String strSdt = DateUtil.getDateString("yyyy-MM-", eCal.getTime())+"01";
				String strEdt = DateUtil.getDateString("yyyy-MM-", eCal.getTime())+ Integer.toString(lastDay);
				
				queryParams.put("dateFrom", strSdt);
				queryParams.put("dateTo", strEdt);
				logger.info("[{}] queryParams {}", uuid, queryParams);
				wfmsNasRepository.getSumMrpMonth(queryParams);
				List<SumMrpMonth> cursorMap = (List<SumMrpMonth>)queryParams.get("cursor");
				iResult = new ArrayList<>(cursorMap);
			}
			case SUM_REASON_30MIN -> {
				String strSdt = DateUtil.getDateString("yyyy-MM-dd");
				String strEdt = DateUtil.getDateString("yyyy-MM-dd");
				queryParams.put("sdt", strSdt);
				queryParams.put("edt", strEdt);
				logger.info("[{}] queryParams {}", uuid, queryParams);
				wfmsNasRepository.getSumReason30min(queryParams);
				List<SumReason30min> cursorMap = (List<SumReason30min>)queryParams.get("cursor");
				iResult = new ArrayList<>(cursorMap);
			}
		}
		return iResult;
	}
	
}
