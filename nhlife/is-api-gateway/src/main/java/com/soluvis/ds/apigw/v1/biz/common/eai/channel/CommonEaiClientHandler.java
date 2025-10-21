package com.soluvis.ds.apigw.v1.biz.common.eai.channel;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.biz.common.eai.util.EaiUtil;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.BaseReq;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.CommonEaiHeader;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.util.internal.PlatformDependent;
import lombok.Getter;
import lombok.Setter;

/**
 * @Class 		: CommonEaiClientHandler
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. ChannelActive 이벤트 발생 시 메세지 전송
 *  2. ChannelRead 이벤트 발생 시 메세지를 Buffer에 저장
 *  3. ChannelInactive 이벤트 발생 시 채널 종료
 *  4. ExceptionCaught 이벤트 발생 시 에러 표출 후 채널 종료
 */
@Component
@Scope("prototype")
public class CommonEaiClientHandler extends ChannelInboundHandlerAdapter {
	
	static final Logger logger = LoggerFactory.getLogger(CommonEaiClientHandler.class);
	
	@Getter
	@Setter
	UUID uuid;
	
	ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
	@Getter
	CompositeByteBuf compositeByteBuf = allocator.compositeBuffer();
	@Getter
	byte lastByte;
	
	@Getter
	@Setter
	EventLoopGroup eventLoopGroup;
	@Getter
	@Setter
	String sendMsg;
	
	@Getter
	@Setter
	BaseReq req;
	@Getter
	@Setter
	CommonEaiHeader header;
	
	@Getter
	static final AttributeKey<String> RESULT_CD = AttributeKey.valueOf(Const.APIGW_KEY_RESULT_CD);
	@Getter
	static final AttributeKey<String> RESULT_MSG = AttributeKey.valueOf(Const.APIGW_KEY_RESULT_MSG);
	@Getter
	static final AttributeKey<CompositeByteBuf> CHILD_BUF = AttributeKey.valueOf(Const.APIGW_KEY_BYTEBUF);
	

	/**
	 * @Method		: channelActive
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  채널 활성화시 이벤트 발생
	 * 
	 *  1. AttributeKey를 Default 성공으로 설정.
	 *  2. 보낼 메세지에 STX/ETX 추가
	 *  3. ByteArray 형태로 메세지 전송
	 */
	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	logger.info("[{}] Client request>>{}", uuid, getSendMsg());
    	ctx.channel().attr(RESULT_CD).set(Const.APIGW_SUCCESS_CD);
    	ctx.channel().attr(RESULT_MSG).set(Const.APIGW_SUCCESS_MSG);
    	
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	baos.write(Const.EAI_STX);
    	baos.write(getSendMsg().getBytes());
    	baos.write(Const.EAI_ETX);
    	
    	ctx.writeAndFlush(baos.toByteArray());
    }

	/**
	 * @Method		: channelRead
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  서버로부터 메세지 수신 시 이벤트 발생
	 * 
	 *  1. 메세지 전송 단위인 ByteBuf를 CompositeByteBuf에 추가
	 *  2. ETX 처리를 위해 ByteBuf의 마지막 바이트를 저장.
	 *  3. 전송받은 메세지 로그로 출력
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		long usedDirectMemory = PlatformDependent.usedDirectMemory();
		System.out.println("Used Direct Memory: " + usedDirectMemory + " bytes");
		ByteBuf byteBuf = (ByteBuf) msg;
//		byteBuf.retain(); // 해당 코드 다이렉트 메모리 릭 발생
		
		compositeByteBuf.addComponent(true, byteBuf);
		compositeByteBuf.writerIndex(compositeByteBuf.capacity());
		
		int length = byteBuf.readableBytes();
		
		lastByte = byteBuf.getByte(length-1);
		
		byte[] b = new byte[length];
		byteBuf.readBytes(b);
		
//		logger.info("[{}] channelRead message length[{}]>>{}", uuid, length, EaiUtil.toString(b));
		
		byteBuf.resetReaderIndex();
	}

    /**
     * @Method		: exceptionCaught
     * @date   		: 2025. 2. 17.
     * @author   	: PA2412013
     * ----------------------------------------
     * @notify
     *  Timeout 등 에러 발생시 이벤트 발생
     * 
     *  1. 에러 처리
     *  2. 채널 종료
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	logger.error("[{}] EAI error: {}", uuid, cause.getClass().getName());
    	logger.error("[{}] Error Detail>>", uuid, cause);
    	ctx.channel().attr(RESULT_CD).set(Const.APIGW_FAIL_CD);
    	ctx.channel().attr(RESULT_MSG).set(cause.getClass().getName());
    	
    	CompositeByteBuf buf = ctx.channel().attr(CHILD_BUF).get();
    	EaiUtil.disconnect(ctx, buf, uuid);
    }

	/**
	 * @Method		: channelInactive
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  채널 비활성화 시 이벤트 발생
	 * 
	 *  1. 채널 종료
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("[{}] channel inactive", uuid);
		CompositeByteBuf buf = ctx.channel().attr(CHILD_BUF).get();
		EaiUtil.disconnect(ctx, buf, uuid);
	}
    
}