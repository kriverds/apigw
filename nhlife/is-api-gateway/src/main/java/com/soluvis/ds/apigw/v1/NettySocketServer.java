package com.soluvis.ds.apigw.v1;

import java.util.Iterator;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettySocketServer {

    private final int port;

    public NettySocketServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        // Boss: 연결 수락, Worker: 실제 데이터 처리
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)   // NIO 기반 채널
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         public void initChannel(SocketChannel ch) {
                             ChannelPipeline pipeline = ch.pipeline();
                             pipeline.addLast(new EchoServerHandler()); // 핸들러 등록
                         }
                     })
                     .option(ChannelOption.SO_BACKLOG, 128)   // 대기열 크기
                     .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 서버 바인드
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("🚀 Netty Socket Server started on port " + port);

            // 서버 채널이 종료될 때까지 대기
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new NettySocketServer(8081).start();  // 포트 8081에서 실행
    }

    // 📌 Echo 핸들러 (클라이언트 메시지를 그대로 돌려줌)
    static class EchoServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf in = (ByteBuf) msg;
            try {
//                System.out.println("📩 Received: " + in.toString(io.netty.util.CharsetUtil.UTF_8));
            	System.out.println("📩 Received: ");
                for (int i = 0; i < 100000; i++) {
                	ctx.write(in.retainedDuplicate()); // 받은 데이터를 그대로 다시 write (release 방지 위해 retain)
				}
            } finally {
//            	ctx.flush();
                // release()는 write 이후 flush까지 하고 난 뒤 Netty가 자동으로 해줌
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush(); // write 버퍼 비우기
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}