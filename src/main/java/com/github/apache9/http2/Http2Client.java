package com.github.apache9.http2;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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
import io.netty.handler.codec.http2.Http2CodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.Promise;

/**
 * @author zhangduo
 */
public class Http2Client {

	private final ConcurrentMap<Integer, Promise<String>> streamId2Promise = new ConcurrentHashMap<>();

	private final Channel channel;

	private final AtomicInteger streamId = new AtomicInteger(11);

	public Http2Client(EventLoopGroup group, InetSocketAddress addr) throws Exception {
		Http2SettingsHandler settingsHandler = new Http2SettingsHandler(group.next().newPromise());
		this.channel = new Bootstrap().group(group).channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<Channel>() {

					@Override
					protected void initChannel(Channel ch) throws Exception {
						ch.pipeline().addLast(settingsHandler,
								new Http2CodecBuilder(false, new Http2StreamChannelBootstrap().handler(null)).build());
					}
				}).connect(addr).sync().channel();
		settingsHandler.await();
	}

	private int nextStreamId() {
		return streamId.getAndAdd(2);
	}

	public String hello() {
		Http2Headers headers = new DefaultHttp2Headers().scheme(HttpScheme.HTTP.name())
				.method(HttpMethod.GET.asciiName()).path("/");
		channel.writeAndFlush(new DefaultHttp2HeadersFrame(headers).streamId(nextStreamId()));
		return null;
	}
}
