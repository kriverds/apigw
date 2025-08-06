package com.soluvis.ds.apigw.v1.biz.mobile.eai.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.soluvis.ds.apigw.v1.biz.common.eai.client.EaiClient;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.CommonEaiHeader;
import com.soluvis.ds.apigw.v1.biz.mobile.eai.channel.IVZZMOZZSH001Handler;
import com.soluvis.ds.apigw.v1.biz.mobile.eai.repository.MobileEaiRepository;
import com.soluvis.ds.apigw.v1.biz.mobile.eai.vo.IVZZMOZZSH001Req;

import lombok.Setter;

/**
 * @Class 		: MobileEaiService
 * @date   		: 2025. 2. 14.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  MOBILE EAI Service
 */
@Service
public class MobileEaiService {
	
	static final Logger logger = LoggerFactory.getLogger(MobileEaiService.class);
	
	/**
	 * 스프링 DI
	 */
	MobileEaiRepository mobileEaiRepository;
	EaiClient eaiClient;
	ApplicationContext applicationContext;
	public MobileEaiService(MobileEaiRepository mobileEaiRepository, EaiClient eaiClient,
			ApplicationContext applicationContext) {
		this.mobileEaiRepository = mobileEaiRepository;
		this.eaiClient = eaiClient;
		this.applicationContext = applicationContext;
	}
	
	static final String SEARCH_MRP = "60001";
	
	@Setter
	UUID uuid;
	
	/**
	 * @Method		: executeIVZZMOZZSH001
	 * @date   		: 2025. 2. 14.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 공통 헤더 작성
	 *  2. EAI Request 작성
	 *  3. EAIClient 호출
	 */
	public JSONObject executeIVZZMOZZSH001() throws Exception {
		String key = IVZZMOZZSH001Req.getHEADER_KEY();
		int dataLen = IVZZMOZZSH001Req.getHEADER_DATA_LEN();
		String svcId = IVZZMOZZSH001Req.getHEADER_SVC_ID();
		String intfId = IVZZMOZZSH001Req.getHEADER_INTF_ID();
		
		CommonEaiHeader header = new CommonEaiHeader.Builder()
				.icKey(key).ecDataLen(Integer.toString(dataLen)).ecRcveSvcId(svcId).ecEaiIntfId(intfId)
				.build();
		
		String mrp = SEARCH_MRP;
		
		Map<String,Object> qMap = getWaitCnt();
		String waitCustCnt = qMap.get("waitCustCnt")==null?"0":qMap.get("waitCustCnt").toString();
		String workTimeYn = qMap.get("workTimeYn")==null?"N":qMap.get("workTimeYn").toString();
		logger.info("[{}] IVZZMOZZSH001Req MRP[{}] waitCustCnt[{}] workTimeYn[{}]", uuid, mrp, waitCustCnt, workTimeYn);
		
		IVZZMOZZSH001Req req = new IVZZMOZZSH001Req.Builder()
				.mrp(mrp).waitCustCnt(waitCustCnt).workTimeYn(workTimeYn)
				.build();
		
		String reqMsg = header.toEaiString()+req.toEaiString();
		
		logger.debug("[{}] EAI Request>>{}", uuid, reqMsg);
		eaiClient.setUuid(uuid);
		eaiClient.setHandlerSendMsg(reqMsg);
		
		return eaiClient.execute(applicationContext.getBean(IVZZMOZZSH001Handler.class));
	}
	
	/**
	 * @Method		: getWaitCnt
	 * @date   		: 2025. 2. 18.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  특정 MRP로 대기고객 수 조회
	 */
	Map<String,Object> getWaitCnt(){
		Map<String, Object> queryParam = new HashMap<>();
		queryParam.put("MRP", SEARCH_MRP);
		return mobileEaiRepository.getIVZZMOZZSH001(queryParam);
	}
}
