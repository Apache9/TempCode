package com.github.apache9.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;

import java.util.Collections;

/**
 * @author zhangduo
 */
public class Http2Server {

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            HttpServerCodec sourceCodec = new HttpServerCodec();
                            HttpServerUpgradeHandler.UpgradeCodec upgradeCodec = new Http2ServerUpgradeCodec(
                                    new HelloWorldHttp2Handler());
                            HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(
                                    sourceCodec, Collections.singletonList(upgradeCodec), 65536);

                            ch.pipeline().addLast(sourceCodec);
                            ch.pipeline().addLast(upgradeHandler);
                            ch.pipeline().addLast(new HelloWorldHttp1Handler());
                        }

                    });
            Channel channel = bootstrap.bind(29581).sync().channel();
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
