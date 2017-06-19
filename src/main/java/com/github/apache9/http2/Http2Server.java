package com.github.apache9.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.Http2CodecBuilder;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AsciiString;

/**
 * @author zhangduo
 */
public class Http2Server {

    private static final UpgradeCodecFactory FACTORY = new UpgradeCodecFactory() {
        @Override
        public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(new Http2CodecBuilder(true, new HelloWorldHttp2Handler()).build());
            } else {
                return null;
            }
        }
    };

    public static final void main(String[] args) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Channel channel = null;
        try {
            channel = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            HttpServerCodec sourceCodec = new HttpServerCodec();
                            ch.pipeline().addLast(sourceCodec, new HttpServerUpgradeHandler(sourceCodec, FACTORY),
                                    new HelloWorldHttp1Handler());
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
