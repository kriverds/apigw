package com.soluvis.ds.apigw.v1.biz.apps.eai.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.apps.eai.channel.IVZZMANBSH003Handler;
import com.soluvis.ds.apigw.v1.biz.apps.eai.repository.AppsEaiRepository;
import com.soluvis.ds.apigw.v1.biz.apps.eai.vo.IVZZMANBSH003Req;
import com.soluvis.ds.apigw.v1.biz.common.eai.client.EaiClient;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.CommonEaiHeader;
import com.soluvis.ds.apigw.v1.util.DateUtil;

import lombok.Setter;

/**
 * @Class 		: AppsEaiService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  배치에서 execute 메서드 호출
 */
@Service
public class AppsEaiService {
	
	static final Logger logger = LoggerFactory.getLogger(AppsEaiService.class);
	
	/**
	 * 스프링 DI
	 */
	AppsEaiRepository appsEaiRepository;
	EaiClient eaiClient;
	ApplicationContext applicationContext;
	public AppsEaiService(AppsEaiRepository appsEaiRepository, EaiClient eaiClient,
			ApplicationContext applicationContext) {
		this.appsEaiRepository = appsEaiRepository;
		this.eaiClient = eaiClient;
		this.applicationContext = applicationContext;
	}
	
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeIVZZMANBSH003
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 현재 날짜 데이터 호출
	 */
	public JSONObject executeIVZZMANBSH003() throws Exception {
		String stdt = DateUtil.getDateString("yyyyMMdd");
		String eddt = DateUtil.getDateString("yyyyMMdd");
		return loopBrCd(stdt, eddt);
	}
	/**
	 * @Method		: executeIVZZMANBSH003Before15day
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 호출시각 기준으로 15일전~1일전 데이터 호출
	 */
	public JSONObject executeIVZZMANBSH003Before15day() throws Exception {
		String stdt = DateUtil.getDateString("yyyyMMdd", -15);
		String eddt = DateUtil.getDateString("yyyyMMdd", -1);
		return loopBrCd(stdt, eddt);
	}
	/**
	 * @Method		: loopBrCd
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. DB에서 사무소 코드 조회
	 *  2. 사무소 코드 수 만큼 IVZZMANBSH003 전문 실행
	 */
	JSONObject loopBrCd(String stdt, String eddt) throws Exception {
		JSONObject resultJO = new JSONObject();
		JSONArray resultJA = new JSONArray();
		
		List<Map<String, Object>> mlist = appsEaiRepository.getBrCdList(); //사무소 코드 조회
		
		if(mlist.isEmpty()){
			resultJO.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
			resultJO.put(Const.APIGW_KEY_RESULT_MSG, "사무소 코드가 없습니다.");
		} else {
			boolean success = true;
			for(final Map<String,Object> brMap : mlist) {
				String brCd = brMap.get("brCd").toString();
				JSONObject executeResult = sendIVZZMANBSH003(brCd, stdt, eddt);
				String resultCd = executeResult.get(Const.APIGW_KEY_RESULT_CD).toString();
				if(Const.APIGW_FAIL_CD.equals(resultCd)) {
					success = false;
				}
				resultJA.put(executeResult);
			}
			
			if(success) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
				Date sdt = sdf.parse(stdt);
				Date edt = sdf.parse(eddt);
				Map<String, Object> queryParams = new HashMap<>();
				queryParams.put("sdt", sdf2.format(sdt));
				queryParams.put("edt", sdf2.format(edt));
				appsEaiRepository.aggSumAppTm(queryParams);
				
				resultJO.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_SUCCESS_CD);
			} else {
				resultJO.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
			}
			resultJO.put(Const.APIGW_KEY_RESULT_MSG, resultJA);
		}
		
		return resultJO;
	}
	/**
	 * @Method		: sendIVZZMANBSH003
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 공통 헤더 작성
	 *  2. EAI Request 작성
	 *  3. EAIClient 호출
	 *  4. EAIClient 종료 후 후단 프로시저 호출
	 * 
	 *  **해당 전문은 NextKey 존재 시 전문을 재호출 하기 때문에 header와 Request 정보를 같이 넘겨준다.
	 */
	JSONObject sendIVZZMANBSH003(String brCd, String stdt, String eddt) throws Exception {
		String key = IVZZMANBSH003Req.getHEADER_KEY();
		int dataLen = IVZZMANBSH003Req.getHEADER_DATA_LEN();
		String svcId = IVZZMANBSH003Req.getHEADER_SVC_ID();
		String intfId = IVZZMANBSH003Req.getHEADER_INTF_ID();
		
		CommonEaiHeader header = new CommonEaiHeader.Builder()
				.icKey(key).ecDataLen(Integer.toString(dataLen)).ecRcveSvcId(svcId).ecEaiIntfId(intfId)
				.build();
		
		String nextKey = "";
		
		logger.info("[{}] IVZZMANBSH003Req brCd[{}] stdt[{}] eddt[{}] nextKey[{}]", uuid, brCd, stdt, eddt, nextKey);
		IVZZMANBSH003Req req = new IVZZMANBSH003Req.Builder()
				.inqStdt(stdt).inqEddt(eddt).brCd(brCd).nextKey(nextKey)
				.build();
		
		String reqMsg = header.toEaiString()+req.toEaiString();
		
		logger.debug("[{}] EAI Request>>{}", uuid, reqMsg);
		eaiClient.setUuid(uuid);
		eaiClient.setHandlerSendMsg(reqMsg);
		
		JSONObject resultEai = eaiClient.execute(applicationContext.getBean(IVZZMANBSH003Handler.class), header, req);
		
		return resultEai;
	}
}
