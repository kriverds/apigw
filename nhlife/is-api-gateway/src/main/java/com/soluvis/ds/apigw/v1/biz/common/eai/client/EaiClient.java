package com.soluvis.ds.apigw.v1.biz.common.eai.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.soluvis.ds.apigw.v1.application.config.Const;
import com.soluvis.ds.apigw.v1.application.config.GVal;
import com.soluvis.ds.apigw.v1.biz.common.eai.channel.CommonEaiClientHandler;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.BaseReq;
import com.soluvis.ds.apigw.v1.biz.common.eai.vo.CommonEaiHeader;
import com.soluvis.ds.apigw.v1.util.CommonUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import lombok.Setter;

/**
 * @Class 		: EaiClient
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. EAI 연결 및 결과 반환
 */
@Component
@Scope("prototype")
public class EaiClient {
	@Value("${eai.host.primary}")
    String eaiHostP;
	@Value("${eai.host.backup}")
    String eaiHostB;
	@Value("${eai.port}")
    int eaiPort;
	
	@Value("${eai.timeout.connect}")
    int connectTimeOut;
	@Value("${eai.timeout.read}")
    int readTimeOut;
	@Value("${eai.timeout.write}")
    int writeTimeOut;
    
    @Setter
    UUID uuid;
    @Setter
    String handlerSendMsg;

    static final Logger logger = LoggerFactory.getLogger(EaiClient.class);

    /**
     * @Method		: execute
     * @date   		: 2025. 2. 17.
     * @author   	: PA2412013
     * ----------------------------------------
     * @notify
     *  EAI 프로세스 실행
     * 
     *  1. EAI 핸들러 설정
     *  2. 채널 그룹 설정
     *  3. 소켓 통신할 채널 설정 (버퍼 및 타임아웃)
     *  4. 채널 연결
     *  5. 채널 연결 여부 확인
     *  6. 채널 종료까지 대기
     *  7. 채널 종료 시 채널 그룹 종료
     *  8. 채널 AttributeKey값 확인하여 결과 값 리턴
     *  9. 통신에러 발생 시 HostIP 변경
     */
    public JSONObject execute(CommonEaiClientHandler handler, CommonEaiHeader header, BaseReq req) throws Exception {
    	JSONObject jResult = new JSONObject();
    	
    	NioEventLoopGroup group = new NioEventLoopGroup();
        handler.setEventLoopGroup(group);
        handler.setUuid(uuid);
        handler.setSendMsg(handlerSendMsg);
        handler.setHeader(header);
        handler.setReq(req);

        try {
        	Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOut*1000)
                     .option(ChannelOption.SO_SNDBUF, 65536)
                     .option(ChannelOption.SO_RCVBUF, 65536)
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel socketChannel) throws Exception {
                             ChannelPipeline pipeline = socketChannel.pipeline();
                             pipeline.addLast(new ReadTimeoutHandler(readTimeOut, TimeUnit.SECONDS));
                             pipeline.addLast(new WriteTimeoutHandler(writeTimeOut, TimeUnit.SECONDS));
                             pipeline.addLast(new ByteArrayEncoder());
                             pipeline.addLast(handler);
                         }
                     });
            
            ChannelFuture future = bootstrap.connect(GVal.getEaiHost(), eaiPort).sync();
            
            future.addListener(f -> {
            	if(f.isSuccess()) {
            		logger.info("[{}] {}:{} {}", uuid, GVal.getEaiHost(), eaiPort, "Connect success");
            	} else {
            		logger.error("[{}] {}:{} {}", uuid, GVal.getEaiHost(), eaiPort, "Connect fail");
            	}
            });
            
            future.channel().closeFuture().sync();
            
            String resultCd = future.channel().attr(CommonEaiClientHandler.getRESULT_CD()).get();
            String resultMsg = future.channel().attr(CommonEaiClientHandler.getRESULT_MSG()).get();
            jResult.put(Const.APIGW_KEY_RESULT_CD, resultCd);
            jResult.put(Const.APIGW_KEY_RESULT_MSG, resultMsg);
            
        } catch(Exception e) {
        	if(GVal.getEaiHost().equals(eaiHostP)) {
        		GVal.setEaiHost(eaiHostB);
        	} else {
        		GVal.setEaiHost(eaiHostP);
        	}
        	Map<String,Object> eResult = CommonUtil.commonException(e, uuid);
        	jResult.put(Const.APIGW_KEY_RESULT_CD, Const.APIGW_FAIL_CD);
            jResult.put(Const.APIGW_KEY_RESULT_MSG, eResult.toString());
            
        } finally {
        	Future<?> future = group.shutdownGracefully(0, 1, TimeUnit.SECONDS);
            future.await();
            
            logger.info("[{}] Channel[{}] terminate[{}]", uuid, handler.getClass().getName(), group.isTerminated());
        }
        return jResult;
    }
    /**
     * @Method		: execute
     * @date   		: 2025. 2. 17.
     * @author   	: PA2412013
     * ----------------------------------------
     * @notify
     *  헤더와 리퀘스트 정보 재사용 필요 없을 경우 사용.
     */
    public JSONObject execute(CommonEaiClientHandler handler) throws Exception {
    	return execute(handler, null, null);
    }
    
}