package com.soluvis.ds.apigw.v1.biz.common.nas.util;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.application.config.GVal;
import com.soluvis.ds.apigw.v1.util.FileUtil;

public class NasUtil {

	NasUtil() {}
	
	/**
	 * @Method		: writeComplete
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  파일 쓰기 성공 결과 반환
	 *  데이터가 없는 경우도 있기 때문에 데이터가 없어도 성공으로 반환.
	 */
	public static JSONObject writeComplete(String fileName, int dataCnt) {
		JSONObject jResult = new JSONObject();
		if(dataCnt > 0) {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, "파일명["+fileName+"] 데이터건수["+dataCnt+"]");
		} else {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, "파일명["+fileName+"] 데이터 없음");
		}
		return jResult;
	}
	
	/**
	 * @Method		: readComplete
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  파일 읽기 성공 결과 반환
	 */
	public static JSONObject readComplete(int dataCnt, int progressCnt) {
		JSONObject jResult = new JSONObject();
		if(progressCnt == dataCnt) {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, "총건수["+dataCnt+"] 처리건수["+progressCnt+"]");
		} else {
			jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
			jResult.put(Const.APIGW_KEY_RESULT_MSG, "총건수["+dataCnt+"] 처리건수["+progressCnt+"] 실패건수["+(dataCnt-progressCnt)+"]");
		}
		return jResult;
	}
	
	/**
	 * @Method		: moveSuccessFiles
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  작업 성공 파일 이동 (권한 문제로 사용안함)
	 */
	public static int moveSuccessFiles(List<File> files, UUID uuid) {
		return NasUtil.moveFiles(files, true, uuid);
	}
	/**
	 * @Method		: moveFailFiles
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  작업 실패 파일 이동 (권한 문제로 사용안함)
	 */
	public static int moveFailFiles(List<File> files, UUID uuid) {
		return NasUtil.moveFiles(files, false, uuid);
	}
	
	/**
	 * @Method		: moveFiles
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  파일 이동 (권한 문제로 사용안함)
	 */
	static int moveFiles(List<File> files, boolean success, UUID uuid) {
		int iResult = 0;
		for(final File file: files) {
			String sourcePath = file.getParent();
			String targetPath = file.getParent() + File.separator+ (success?GVal.getNasSuccessDirectory():GVal.getNasFailDirectory());
			String fileName = file.getName();
			JSONObject jResult = FileUtil.fileMove(sourcePath, targetPath, fileName, uuid);
			String resultCd = jResult.getString(Const.APIGW_KEY_RESULT_CD);
			if(Const.APIGW_SUCCESS_CD.equals(resultCd)) {
				iResult++;
			}
		}
		return iResult;
	}

}
