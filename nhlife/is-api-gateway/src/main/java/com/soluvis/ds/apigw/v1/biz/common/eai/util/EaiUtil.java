package com.soluvis.ds.apigw.v1.biz.common.eai.util;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.soluvis.ds.apigw.v1.application.config.Const;

import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class EaiUtil {
	
	static final Logger logger = LoggerFactory.getLogger(EaiUtil.class);

	EaiUtil() {}
	
	/**
	 * @Method		: disconnect
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 채널 종료 호출
	 *  2. Buffer 메모리 클리어
	 */
	public static void disconnect(ChannelHandlerContext ctx, CompositeByteBuf buf, UUID uuid) {
		ctx.close();
		if(buf != null && buf.refCnt() > 0) {
			logger.info("[{}] buffer cnt>>{}", uuid, buf.refCnt());
			buf.release();
		}
		logger.info("[{}] {}", uuid, "disconnect channel");
	}
	
	/**
	 * @Method		: read
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CompositeByteBuf로 부터 ByteArray 읽기
	 */
	public static byte[] read(CompositeByteBuf buf) {
		byte[] bMsg = new byte[buf.readableBytes()];
		buf.readBytes(bMsg);
		
		return bMsg;
	}
	
	/**
	 * @Method		: toString
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  EAI Charset으로 Byte[] > String 변환
	 */
	public static String toString(byte[] bMsg) {
		return new String(bMsg, Const.EAI_CHARSET);
	}

}
