package com.soluvis.ds.apigw.v1.biz.apps.eai.channel;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.apps.eai.repository.AppsEaiRepository;
import com.soluvis.ds.apigw.v1.biz.apps.eai.vo.IVZZMANBSH003Req;
import com.soluvis.ds.apigw.v1.biz.apps.eai.vo.IVZZMANBSH003Res;
import com.soluvis.ds.apigw.v1.biz.common.eai.channel.CommonEaiClientHandler;
import com.soluvis.ds.apigw.v1.biz.common.eai.util.EaiUtil;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.CommonEaiHeader;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import io.netty.channel.ChannelHandlerContext;

/**
 * @Class 		: IVZZMANBSH003Handler
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IVZZMANBSH003 전문 핸들러
 * 
 *  1. ETX가 올때까지 전문 송신 받음
 *  2. ETX 수신 후 전문 처리 로직 시작
 *  3. 수신한 ByteArray로 IVZZMANBSH003Res VO객체 생성
 *  4. DataList Size가 0보다 클 경우 DB에 저장
 *  5. NextYn이 Y일 경우 nextKey 설정, STX/ETC 추가하여 전문 재전송 후 1~4 반복
 *  6. NextYn이 N일 경우 채널 종료
 */
@Component
@Scope("prototype")
public class IVZZMANBSH003Handler extends CommonEaiClientHandler{
	
	static final Logger logger = LoggerFactory.getLogger(IVZZMANBSH003Handler.class);
	
	/**
	 * 스프링 DI
	 */
	AppsEaiRepository appsEaiRepository;
	public IVZZMANBSH003Handler(AppsEaiRepository appsEaiRepository) {
		this.appsEaiRepository = appsEaiRepository;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Map<String,Object> queryParams = new HashMap<>();
		try {
			super.channelRead(ctx, msg);
			
			byte lastByte = getLastByte();
	    	if(lastByte == Const.EAI_ETX) {
	    		logger.info("[{}] EAI_ETX received", getUuid());
				
				byte[] bMsg = EaiUtil.read(getCompositeByteBuf());
				logger.info("[{}] Server message>>{}", getUuid(), EaiUtil.toString(bMsg));
				
				IVZZMANBSH003Res res = new IVZZMANBSH003Res(bMsg);
				logger.info("[{}] {}", getUuid(), res);
				
				int listCnt = res.getDataList().size();
				if(listCnt > 0) {
					List<IVZZMANBSH003Res> dataList = res.getDataList();
					logger.info("[{}] IVZZMANBSH003 dataList size[{}]", getUuid(), dataList.size());
					logger.info("[{}] IVZZMANBSH003 dataList[{}]", getUuid(), dataList);
					int pageCnt = (listCnt/Const.DB_BATCH_SIZE)+1;
					for (int i = 0; i < pageCnt; i++) {
						List<IVZZMANBSH003Res> insertList = new ArrayList<>();
						int pageSize = i<(pageCnt-1) ? Const.DB_BATCH_SIZE : (listCnt%Const.DB_BATCH_SIZE);
						for (int j = 0; j < pageSize; j++) {
							int index = (i*Const.DB_BATCH_SIZE) + j;
							insertList.add(dataList.get(index));
						}
						queryParams.put("list", insertList);
						int queryResult = appsEaiRepository.setEaiTmBizData(queryParams);
						logger.info("[{}] Complete queryResult:{}", getUuid(), queryResult);
						insertList.clear();
						queryParams.clear();
					}
					
					logger.info("[{}] nextYn[{}]", getUuid(), res.getNextYn());
					
					if("Y".equals(res.getNextYn()))	{
						logger.info("[{}] set nextKey[{}]", getUuid(), res.getNextKey());
						
						CommonEaiHeader header = getHeader();
						String nextKey = res.getNextKey();
						IVZZMANBSH003Req req = (IVZZMANBSH003Req) getReq();
						req.setNextKey(nextKey);
						
						logger.debug("[{}] EAI IVZZMANBSH003Req>>{}", getUuid(), req.toEaiString());
						
						String reqMsg = header.toEaiString()+req.toEaiString();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    	baos.write(Const.EAI_STX);
				    	baos.write(reqMsg.getBytes());
				    	baos.write(Const.EAI_ETX);
						getCompositeByteBuf().clear();
						ctx.writeAndFlush(baos.toByteArray());
					} else {
						logger.info("[{}] Complete Channel", getUuid());
						ctx.channel().attr(getCHILD_BUF()).set(getCompositeByteBuf());
						EaiUtil.disconnect(ctx, getCompositeByteBuf(), getUuid());
					}
				}else {
					logger.info("[{}] {}", getUuid(), Const.EAI_NO_SEARCH_MSG);
					ctx.channel().attr(getRESULT_CD()).set(Const.APIGW_SUCCESS_CD);
			    	ctx.channel().attr(getRESULT_MSG()).set(Const.EAI_NO_SEARCH_MSG);
			    	ctx.channel().attr(getCHILD_BUF()).set(getCompositeByteBuf());
			    	EaiUtil.disconnect(ctx, getCompositeByteBuf(), getUuid());
				}
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
