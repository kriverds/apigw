package com.soluvis.ds.apigw.v1.biz.chatbot.nas.service;

import java.util.ArrayList;
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
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.config.ChatbotNasProperties;
import com.soluvis.ds.apigw.v1.biz.chatbot.nas.repository.ChatbotNasRepository;
import com.soluvis.ds.apigw.v1.biz.common.nas.util.NasUtil;
import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasWriteVo;
import com.soluvis.ds.apigw.v1.util.DateUtil;
import com.soluvis.ds.apigw.v1.util.FileUtil;

import lombok.Setter;

/**
 * @Class 		: ChatbotNasWriteService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  챗봇과 연동하는 데이터를 Nas로 파일적재한다.
 */
@Service
public class ChatbotNasWriteService {
	
	enum Job{
		HOL_INFO			//챗봇 휴일정보
	}
	
	static final Logger logger = LoggerFactory.getLogger(ChatbotNasWriteService.class);
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
	ChatbotNasProperties chatbotProperties;
	public ChatbotNasWriteService(ChatbotNasRepository chatbotNasRepository, ChatbotNasProperties chatbotProperties) {
		this.chatbotNasRepository = chatbotNasRepository;
		this.chatbotProperties = chatbotProperties;
	}
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeHolInfo
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  HOL_INFO
	 */
	public JSONObject executeHolInfo() {
		return executeWrite(Job.HOL_INFO);
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
		Map<String,Object> queryParams = new HashMap<>();
		
		String path = getPath(job);
		String fileName = getFileName(job);
		
		List<CommonNasWriteVo> voList = executeQuery(job, queryParams);
		
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
			case HOL_INFO -> result = "hol_day|create_datm";
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
			case HOL_INFO -> result = chatbotProperties.holInfo().filename() + "_" + DateUtil.getDateString(chatbotProperties.holInfo().pattern()) + chatbotProperties.holInfo().extension();
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
			case HOL_INFO -> result += chatbotProperties.holInfo().directory();
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
	List<CommonNasWriteVo> executeQuery(Job job, Map<String,Object> queryParams) {
		List<CommonNasWriteVo> iResult = null;
		switch(job) {
			case HOL_INFO -> iResult = new ArrayList<>(chatbotNasRepository.getHolInfo());
		}
		return iResult;
	}
	
}
