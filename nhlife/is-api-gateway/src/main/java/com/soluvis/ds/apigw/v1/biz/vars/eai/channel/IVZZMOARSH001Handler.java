package com.soluvis.ds.apigw.v1.biz.vars.eai.channel;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.common.eai.channel.CommonEaiClientHandler;
import com.soluvis.ds.apigw.v1.biz.common.eai.util.EaiUtil;
import com.soluvis.ds.apigw.v1.biz.mobile.eai.vo.IVZZMOZZSH001Res;
import com.soluvis.ds.apigw.v1.biz.vars.eai.repository.VarsEaiRepository;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import io.netty.channel.ChannelHandlerContext;

/**
 * @Class 		: IVZZMOARSH001Handler
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IVZZMOARSH001 전문 핸들러
 * 
 *  1. ETX가 올때까지 전문 송신 받음
 *  2. ETX 수신 후 전문 처리 로직 시작
 *  3. 수신한 ByteArray로 IVZZMANBSH003Res VO객체 생성
 *  4. MessageCd가 "0000"일 경우 성공 아닐경우 실패
 *  5. 채널 종료
 */
@Component
@Scope("prototype")
public class IVZZMOARSH001Handler extends CommonEaiClientHandler{
	
	static final Logger logger = LoggerFactory.getLogger(IVZZMOARSH001Handler.class);
	
	/**
	 * 스프링 DI
	 */
	VarsEaiRepository varsEaiRepository;
	public IVZZMOARSH001Handler(VarsEaiRepository varsEaiRepository) {
		this.varsEaiRepository = varsEaiRepository;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			super.channelRead(ctx, msg);
			
			byte lastByte = getLastByte();
	    	if(lastByte == Const.EAI_ETX) {
	    		logger.info("[{}] EAI_ETX recbeived", getUuid());
	    		
	    		byte[] bMsg = EaiUtil.read(getCompositeByteBuf());
	    		logger.info("[{}] Server message>>{}", getUuid(), EaiUtil.toString(bMsg));
				
				IVZZMOZZSH001Res res = new IVZZMOZZSH001Res(bMsg);
				logger.info("[{}] {}", getUuid(), res);
				String messageCd = res.getMsgCd();
				String message = res.getMsg();
				logger.info("[{}] messageCd[{}] message[{}]", getUuid(), messageCd, message);
				if("0000".equals(messageCd)) {
					ctx.channel().attr(getRESULT_CD()).set(Const.APIGW_SUCCESS_CD);
				} else {
					logger.error("[{}] {}", getUuid(), message);
					ctx.channel().attr(getRESULT_CD()).set(Const.APIGW_FAIL_CD);
				}
				ctx.channel().attr(getRESULT_MSG()).set(message);
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
