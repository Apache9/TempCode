package com.github.apache9.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @author zhangduo
 */
public class Http2Server {

    public static final void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Channel channel = null;
        try {
            channel = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new PortUnificationServerHandler());
                        }
                    }).bind(Integer.parseInt(args[0])).sync().channel();
            channel.closeFuture().sync();
        } finally {
            if (channel != null) {
                channel.close().syncUninterruptibly();
            }
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
    }
}
