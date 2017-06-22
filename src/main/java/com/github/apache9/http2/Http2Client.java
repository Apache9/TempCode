package com.github.apache9.http2;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangduo
 */
public class Http2Client {

    private static final Http2FrameLogger LOGGER = new Http2FrameLogger(LogLevel.INFO);

    private final Channel channel;

    private final AtomicInteger streamId = new AtomicInteger(11);

    public Http2Client(EventLoopGroup group, InetSocketAddress addr) throws Exception {
        this.channel = new Bootstrap().group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new Http2FrameCodec(false, LOGGER),
                                new HelloWorldClientHandler());
                    }
                }).connect(addr).sync().channel();
    }

    private int nextStreamId() {
        return streamId.getAndAdd(2);
    }

    public String hello() throws InterruptedException, ExecutionException {
        Http2Headers headers = new DefaultHttp2Headers().scheme(HttpScheme.HTTP.name())
                .method(HttpMethod.GET.asciiName()).path("/");
        Promise<String> promise = channel.eventLoop().newPromise();
        channel.writeAndFlush(
                new HeadersAndPromise(new DefaultHttp2HeadersFrame(headers).streamId(nextStreamId()), promise));
        return promise.get();
    }
}
