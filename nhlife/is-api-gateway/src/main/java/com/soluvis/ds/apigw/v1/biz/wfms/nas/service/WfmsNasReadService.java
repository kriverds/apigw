package com.soluvis.ds.apigw.v1.biz.wfms.nas.service;

import java.io.File;
import java.util.ArrayList;
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
import com.soluvis.ds.apigw.v1.biz.common.nas.util.NasUtil;
import com.soluvis.ds.apigw.v1.biz.common.nas.vo.CommonNasReadVo;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.config.WfmsNasProperties;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.repository.WfmsNasRepository;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.GroupLimitBizKind;
import com.soluvis.ds.apigw.v1.biz.wfms.nas.vo.UserEvlt;
import com.soluvis.ds.apigw.v1.util.DateUtil;
import com.soluvis.ds.apigw.v1.util.FileUtil;

import lombok.Setter;

/**
 * @Class 		: WfmsNasReadService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  WFMS와 연동하는 데이터를 Nas에서 읽는다.
 */
@Service
public class WfmsNasReadService {
	
	enum Job {
		GROUP_LIMIT_BIZ_KIND,		//WFMS 그룹 정원정보
		USER_EVLT					//WFMS 평가대상자 정보
	}
	
	static final Logger logger = LoggerFactory.getLogger(WfmsNasReadService.class);
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
	public WfmsNasReadService(WfmsNasRepository wfmsNasRepository, WfmsNasProperties wfmsNasProperties) {
		this.wfmsNasRepository = wfmsNasRepository;
		this.wfmsNasProperties = wfmsNasProperties;
	}
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeGroupLimitBizKind
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeGroupLimitBizKind() {
		return executeRead(Job.GROUP_LIMIT_BIZ_KIND);
	}
	
	/**
	 * @Method		: executeUserEvlt
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 * 
	 */
	public JSONObject executeUserEvlt() {
		return executeRead(Job.USER_EVLT);
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
		
		String fileName = getFileName(job);
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
								voList.add(vo);
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
			jResult.put(Const.APIGW_KEY_RESULT_MSG, "["+fileName+"] 파일이 없습니다.");
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
			case GROUP_LIMIT_BIZ_KIND -> bResult = true;
			case USER_EVLT -> bResult = true;
		}
		return bResult;
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
			case GROUP_LIMIT_BIZ_KIND -> result = wfmsNasProperties.groupLimitBizKind().filename()+"_"+DateUtil.getDateString(wfmsNasProperties.groupLimitBizKind().pattern()) + wfmsNasProperties.groupLimitBizKind().extension();
			case USER_EVLT -> result = wfmsNasProperties.userEvlt().filename()+"_"+DateUtil.getDateString(wfmsNasProperties.userEvlt().pattern()) + wfmsNasProperties.userEvlt().extension();
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
	List<File> getFileList(Job job) {
		List<File> result;
		switch(job) {
			case GROUP_LIMIT_BIZ_KIND -> {
				String name = getFileName(job);
				result = FileUtil.getFileList(getPath(job), name, uuid);
			}
			case USER_EVLT -> {
				String name = getFileName(job);
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
	 *  각 Job별로 파일경로 리턴
	 */
	String getPath(Job job) {
		String result = baseDirectory;
		switch(job) {
			case GROUP_LIMIT_BIZ_KIND -> result += wfmsNasProperties.groupLimitBizKind().directory();
			case USER_EVLT -> result += wfmsNasProperties.userEvlt().directory();
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
			case GROUP_LIMIT_BIZ_KIND -> vo = new GroupLimitBizKind("CS", DateUtil.getDateString("yyyy-MM"), strArrData[0], strArrData[1],
					strArrData[2], strArrData[3], "batch");
			case USER_EVLT -> vo = new UserEvlt("CS", strArrData[0], strArrData[1]);
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
			case GROUP_LIMIT_BIZ_KIND -> {
				List<GroupLimitBizKind> voList = paramList.stream().map(GroupLimitBizKind.class::cast).toList();
				iResult = wfmsNasRepository.setGroupLimitBizKind(voList);
			}
			case USER_EVLT -> {
				List<UserEvlt> voList = paramList.stream().map(UserEvlt.class::cast).toList();
				iResult = wfmsNasRepository.setUserEvlt(voList);
			}
		}
		return iResult;
	}
	
}
