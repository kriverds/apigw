package com.soluvis.ds.apigw.v1.biz.chatbot.nas.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.config.ChatbotNasProperties;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.repository.ChatbotNasRepository;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.CbStatsInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.CmStatsInfo;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.vo.SmsSendResult;
import com.soluvis.ds.apigw.v1.biz.common.nas.util.NasUtil;
import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasReadVo;
import com.soluvis.ds.apigw.v1.util.DateUtil;
import com.soluvis.ds.apigw.v1.util.FileUtil;

import lombok.Setter;


/**
 * @Class 		: ChatbotNasReadService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  챗봇과 연동하는 데이터를 Nas에서 읽는다.
 */
@Service
public class ChatbotNasReadService {
	
	enum Job{
		CB_STATS_INFO,			//챗봇 상담현황
		CM_STATS_INFO,			//챗봇 캠페인결과
		CM_STATS_INFO_DAY,		//챗봇 캠페인결과 전일 재집계
		SMS_SEND_RESULT			//챗봇 알림톡전송결과
	}
	
	static final Logger logger = LoggerFactory.getLogger(ChatbotNasReadService.class);
	@Value("${nas.base.directory}")
	String baseDirectory;
	@Value("${nas.base.directory.success}")
	String successDirectory;
	@Value("${nas.base.directory.fail}")
	String failDirectory;

	/**
	 * 스프링 DI
	 */
	ChatbotNasRepository chatbotNasRepository;
	ChatbotNasProperties chatbotNasProperties;
	public ChatbotNasReadService(ChatbotNasRepository chatbotNasRepository, ChatbotNasProperties chatbotNasProperties) {
		this.chatbotNasRepository = chatbotNasRepository;
		this.chatbotNasProperties = chatbotNasProperties;
	}
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeCbStatsInfo
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CB_STATS_INFO
	 */
	public JSONObject executeCbStatsInfo() {
		return executeRead(Job.CB_STATS_INFO);
	}
	/**
	 * @Method		: executeCmStatsInfo
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CM_STATS_INFO
	 */
	public JSONObject executeCmStatsInfo() {
		return executeRead(Job.CM_STATS_INFO);
	}
	/**
	 * @Method		: executeCmStatsInfoDay
	 * @date   		: 2025. 4. 10.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CM_STATS_INFO 전일 재집계
	 */
	public JSONObject executeCmStatsInfoDay() {
		return executeRead(Job.CM_STATS_INFO_DAY);
	}
	/**
	 * @Method		: executeSmsSendResult
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  SMS_SEND_RESULT
	 */
	public JSONObject executeSmsSendResult() {
		return executeRead(Job.SMS_SEND_RESULT);
	}
	
	
	/**
	 * @Method		: executeRead
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. Job을 기반으로 경로와 파일명 가져오기
	 *  2. 경로의 파일 리스트를 파일명 기반으로 가져오기
	 *  3. 헤더가 있는 파일인지 없는 파일인지 판단.
	 *  4. 각 파일을 줄 별로 읽기
	 *  5. 각 라인을 '|'로 split 하여 VO형태로 변환
	 *  6. DB_BATCH_SIZE만큼 묶어서 Insert 처리
	 *  7. 성공/실패 파일을 분류하여 파일 이동 (권한 없음으로 보류)
	 */
	JSONObject executeRead(Job job) {
		List<File> successFileList = new ArrayList<>();
		List<File> failFileList = new ArrayList<>();
		
		int totalDataCnt = 0;
		int totalProgressCnt = 0;
		
		List<File> fileList = getFileList(job);
		Calendar sCal = Calendar.getInstance();
		if(!fileList.isEmpty()) {
			for (final File file : fileList) {
				int fileDataCnt = 0;
				int fileProgressCnt = 0;
				int dataCnt = 0;
				
				List<String> strList = FileUtil.readLines(file, uuid);
				
				if(!strList.isEmpty()) {
					logger.info("[{}] Data size[{}] File header>>{}", uuid, strList.size()-1, strList.get(0));
					
					List<String> copyList;
					if(isHeader(job)) {
						copyList = strList.subList(1, strList.size());
					} else {
						copyList = strList;
					}
					
					dataCnt = copyList.size();
					if(dataCnt > 0) {
						List<CommonNasReadVo> voList = new ArrayList<>();
						
						int pageCnt = (copyList.size()/Const.DB_BATCH_SIZE)+1;
						for (int i = 0; i < pageCnt; i++) {
							
							int pageSize = i<(pageCnt-1) ? Const.DB_BATCH_SIZE : (copyList.size()%Const.DB_BATCH_SIZE);
							for (int j = 0; j < pageSize; j++) {
								
								int index = (i*Const.DB_BATCH_SIZE) + j;
								String strData = copyList.get(index);
								String[] strArrData = strData.split(Pattern.quote("|"), -1);
								
								CommonNasReadVo vo = getVo(job, strArrData);
								
								logger.info("[{}] index[{}] {}", uuid, index, vo);
								if(vo != null) {
									voList.add(vo);
								}
							}
							int iResult = executeQuery(job, voList);
							voList.clear();
							
							fileDataCnt += pageSize;
							fileProgressCnt += iResult;
							logger.info("[{}] iResult[{}] fileDataCnt[{}] fileProgressCnt[{}]", uuid, iResult, fileDataCnt, fileProgressCnt);
						}
					} else {
						logger.warn("[{}] file[{}] Data empty", uuid, file.getName());
					}
				} else {
					logger.warn("[{}] file[{}] Empty", uuid, file.getName());
				}
				
				if(fileDataCnt == fileProgressCnt) {
					successFileList.add(file);
				} else {
					failFileList.add(file);
				}
				
				totalDataCnt += fileDataCnt;
				totalProgressCnt += fileProgressCnt;
			}
		} else {
			logger.error("[{}] {}", uuid, "fileList empty!!");
			JSONObject jResult = new JSONObject();
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, "파일이 없습니다.");
			return jResult;
		}
		
//		if(!successFileList.isEmpty()) {
//			NasUtil.moveSuccessFiles(successFileList, uuid);
//		}
//		if(!failFileList.isEmpty()) {
//			NasUtil.moveFailFiles(failFileList, uuid);
//		}
		Calendar eCal = Calendar.getInstance();
		logger.info("[{}] duration TimeInMillis[{}]", uuid, eCal.getTimeInMillis()-sCal.getTimeInMillis());
		
		return NasUtil.readComplete(totalDataCnt, totalProgressCnt);
	}
	/**
	 * @Method		: isHeader
	 * @date   		: 2025. 3. 5.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  헤더 라인 존재 여부를 판단한다.
	 */
	boolean isHeader(Job job) {
		boolean bResult = true;
		switch(job) {
			case CB_STATS_INFO -> bResult = true;
			case CM_STATS_INFO -> bResult = true;
			case CM_STATS_INFO_DAY -> bResult = true;
			case SMS_SEND_RESULT -> bResult = false;
		}
		return bResult;
	}
	/**
	 * @Method		: getFileName
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job 별로 파일명 리턴
	 */
	String getFileName(Job job) {
		String result = "";
		switch(job) {
			case CB_STATS_INFO -> result = chatbotNasProperties.cbStatsInfo().filename();
			case CM_STATS_INFO -> result = chatbotNasProperties.cmStatsInfo().filename();
			case CM_STATS_INFO_DAY -> result = chatbotNasProperties.cmStatsInfo().filename();
			case SMS_SEND_RESULT -> result = chatbotNasProperties.smsSendResult().filename();
		}
		return result;
	}
	/**
	 * @Method		: getFileList
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job 별로 파일리스트 조회
	 */
	/**
	 * @Method		: getFileList
	 * @date   		: 2025. 4. 10.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	List<File> getFileList(Job job) {
		List<File> result;
		switch(job) {
			/**
			 * 1. 전일데이터 + 당일 00시 파일 조회
			 * 2. 전일 00시 데이터 제외
			 */
			case CB_STATS_INFO -> {
				String name1 = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMdd", -1);
				String name2 = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMdd")+"00";
				List<String> nameList = Arrays.asList(name1,name2);
				
				result = FileUtil.getFileList(getPath(job), nameList, uuid);
				
				String removeFileName = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMdd", -1)+"00";
				List<File> tempFileList = List.copyOf(result);
				tempFileList.forEach(file -> {
					if(file.getName().contains(removeFileName)) {
						result.remove(file);
					}
				});
			}
			/**
			 * 1. 해당시간 데이터 조회
			 */
			case CM_STATS_INFO -> {
				String name = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMddHH");
				result = FileUtil.getFileList(getPath(job), name, uuid);
			}
			/**
			 * 1. 전일데이터 + 당일 00시 파일 조회
			 * 2. 전일 00시 데이터 제외
			 */
			case CM_STATS_INFO_DAY -> {
				String name1 = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMdd", -1);
				String name2 = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMdd")+"00";
				List<String> nameList = Arrays.asList(name1,name2);
				result = FileUtil.getFileList(getPath(job), nameList, uuid);
				
				String removeFileName = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMdd", -1)+"00";
				List<File> tempFileList = List.copyOf(result);
				tempFileList.forEach(file -> {
					if(file.getName().contains(removeFileName)) {
						result.remove(file);
					}
				});
			}
			/**
			 * 1. 당일 데이터 조회
			 */
			case SMS_SEND_RESULT -> {
				String name = getFileName(job) + "_" + DateUtil.getDateString("yyyyMMdd");
				result = FileUtil.getFileList(getPath(job), name, uuid);
			}
			default -> result = new ArrayList<>();
		}
		return result;
	}
	/**
	 * @Method		: getPath
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job 별로 파일 경로 리턴
	 */
	String getPath(Job job) {
		String result = baseDirectory;
		switch(job) {
			case CB_STATS_INFO -> result += chatbotNasProperties.cbStatsInfo().directory();
			case CM_STATS_INFO -> result += chatbotNasProperties.cmStatsInfo().directory();
			case CM_STATS_INFO_DAY -> result += chatbotNasProperties.cmStatsInfo().directory();
			case SMS_SEND_RESULT -> result += chatbotNasProperties.smsSendResult().directory();
		}
		return result;
	}
	/**
	 * @Method		: getVo
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job별로 StringArray를 받아 VO객체를 만들어서 리턴
	 */
	CommonNasReadVo getVo(Job job, String[] strArrData) {
		CommonNasReadVo vo = null;
		switch(job) {
			/**
			 * 데이터가 밀려 제대로 parsing이 안되는 케이스가 있어 예외처리.
			 */
			case CB_STATS_INFO -> {
				try {
					Integer.parseInt(strArrData[5]);
				} catch (NumberFormatException e) {
					logger.error("[{}] Data Parsing Error>>{}", uuid, Arrays.asList(strArrData));
					return null;
				}
				vo = new CbStatsInfo(strArrData[0], strArrData[1], strArrData[2], strArrData[3],
					strArrData[4], strArrData[5], strArrData[6], strArrData[7], strArrData[8]);
			}
			case CM_STATS_INFO -> vo = new CmStatsInfo(strArrData[0], strArrData[1], strArrData[2], strArrData[3],
					strArrData[4], strArrData[5], strArrData[6], strArrData[7], strArrData[8],
					strArrData[9], strArrData[10], strArrData[11], strArrData[12]);
			case CM_STATS_INFO_DAY -> vo = new CmStatsInfo(strArrData[0], strArrData[1], strArrData[2], strArrData[3],
					strArrData[4], strArrData[5], strArrData[6], strArrData[7], strArrData[8],
					strArrData[9], strArrData[10], strArrData[11], strArrData[12]);
			case SMS_SEND_RESULT -> vo = new SmsSendResult(strArrData[0], strArrData[1], strArrData[2], strArrData[3],
					strArrData[4], strArrData[5], strArrData[6], strArrData[7], strArrData[8],
					strArrData[9]);
		}
		return vo;
	}
	/**
	 * @Method		: executeQuery
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  각 Job별로 DB Process 처리
	 */
	int executeQuery(Job job, List<CommonNasReadVo> paramList) {
		int iResult = 0;
		switch(job) {
			case CB_STATS_INFO -> {
				List<CbStatsInfo> voList = paramList.stream().map(CbStatsInfo.class::cast).toList();
				iResult = chatbotNasRepository.setCbStatsInfo(voList);
			}
			case CM_STATS_INFO -> {
				List<CmStatsInfo> voList = paramList.stream().map(CmStatsInfo.class::cast).toList();
				iResult = chatbotNasRepository.setCmStatsInfo(voList);
			}
			case CM_STATS_INFO_DAY -> {
				List<CmStatsInfo> voList = paramList.stream().map(CmStatsInfo.class::cast).toList();
				iResult = chatbotNasRepository.setCmStatsInfo(voList);
			}
			case SMS_SEND_RESULT -> {
				List<SmsSendResult> voList = paramList.stream().map(SmsSendResult.class::cast).toList();
				iResult = chatbotNasRepository.setSendSmsResult(voList);
			}
		}
		return iResult;
	}
	
}
