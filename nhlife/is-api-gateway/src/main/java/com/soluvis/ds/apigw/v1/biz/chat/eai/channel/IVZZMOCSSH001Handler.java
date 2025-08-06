package com.soluvis.ds.apigw.v1.biz.chat.eai.channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.chat.eai.repository.ChatEaiRepository;
import com.soluvis.ds.apigw.v1.biz.chat.eai.vo.IVZZMOCSSH001Res;
import com.soluvis.ds.apigw.v1.biz.common.eai.channel.CommonEaiClientHandler;
import com.soluvis.ds.apigw.v1.biz.common.eai.util.EaiUtil;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import io.netty.channel.ChannelHandlerContext;

/**
 * @Class 		: IVZZMOCSSH001Handler
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IVZZMOCSSH001 전문 핸들러
 * 
 *  1. ETX가 올때까지 전문 송신 받음
 *  2. ETX 수신 후 전문 처리 로직 시작
 *  3. 수신한 ByteArray로 IVZZMANBSH003Res VO객체 생성
 *  4. DataList Size가 0보다 클 경우 저장 프로세스 수행
 *  5. DB_BATCH_SIZE로 나누어 데이터 insert 수행
 *  6. 채널 종료
 */
@Component
@Scope("prototype")
public class IVZZMOCSSH001Handler extends CommonEaiClientHandler{
	
	static final Logger logger = LoggerFactory.getLogger(IVZZMOCSSH001Handler.class);
	
	/**
	 * 스프링 DI
	 */
	ChatEaiRepository chatEaiRepository;
	public IVZZMOCSSH001Handler(ChatEaiRepository chatEaiRepository) {
		this.chatEaiRepository = chatEaiRepository;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Map<String,Object> queryParams = new HashMap<>();
		try {
			
			super.channelRead(ctx, msg);
			
			byte lastByte = getLastByte();
	    	if(lastByte == Const.EAI_ETX) {
	    		logger.info("[{}] EAI_ETX recbeived", getUuid());
	    		
	    		byte[] bMsg = EaiUtil.read(getCompositeByteBuf());
	    		logger.info("[{}] Server message>>{}", getUuid(), EaiUtil.toString(bMsg));
				
				IVZZMOCSSH001Res res = new IVZZMOCSSH001Res(bMsg);
				logger.info("[{}] {}", getUuid(), res);
				String strListCnt = res.getListCnt();
				int listCnt = res.getDataList().size();
				if(listCnt > 0) {
					List<IVZZMOCSSH001Res> dataList = res.getDataList();
					logger.info("[{}] IVZZMOCSSH001 dataList size[{}]", getUuid(), dataList.size());
					logger.info("[{}] IVZZMOCSSH001 dataList[{}]", getUuid(), dataList);
					int pageCnt = (listCnt/Const.DB_BATCH_SIZE)+1;
					for (int i = 0; i < pageCnt; i++) {
						List<IVZZMOCSSH001Res> insertList = new ArrayList<>();
						int pageSize = i<(pageCnt-1) ? Const.DB_BATCH_SIZE : (listCnt%Const.DB_BATCH_SIZE);
						for (int j = 0; j < pageSize; j++) {
							int index = (i*Const.DB_BATCH_SIZE) + j;
							insertList.add(dataList.get(index));
						}
						queryParams.put("domain", res.getDomain());
						queryParams.put("nodeAlias", res.getNodeAlias());
						queryParams.put("list", insertList);
						int queryResult = chatEaiRepository.setEaiChatResult15min(queryParams);
						logger.info("[{}] Complete queryResult:{}", getUuid(), queryResult);
						insertList.clear();
						queryParams.clear();
					}
				} else if("".equals(strListCnt)){
					logger.error("[{}] EAI 통신 실패>>{}", getUuid(), EaiUtil.toString(bMsg).replaceAll(" ", ""));
					ctx.channel().attr(getRESULT_CD()).set(Const.APIGW_FAIL_CD);
			    	ctx.channel().attr(getRESULT_MSG()).set(Const.EAI_NO_SEARCH_MSG);
				} else {
					logger.info("[{}] {}", getUuid(), Const.EAI_NO_SEARCH_MSG);
					ctx.channel().attr(getRESULT_CD()).set(Const.APIGW_SUCCESS_CD);
			    	ctx.channel().attr(getRESULT_MSG()).set(Const.EAI_NO_SEARCH_MSG);
				}
				ctx.channel().attr(getCHILD_BUF()).set(getCompositeByteBuf());
				EaiUtil.disconnect(ctx, getCompositeByteBuf(), getUuid());
	    	}
		} catch (Exception e) {
			Map<String,Object> exMap = CommonUtil.commonException(e, getUuid());
			ctx.channel().attr(getRESULT_CD()).set(Const.APIGW_FAIL_CD);
			ctx.channel().attr(getRESULT_MSG()).set(exMap.toString());
			ctx.channel().attr(getCHILD_BUF()).set(getCompositeByteBuf());
			EaiUtil.disconnect(ctx, getCompositeByteBuf(), getUuid());
		}
	}
}
