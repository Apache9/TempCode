package com.github.apache9.http2;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author zhangduo
 */
public class Http2Client {

	private final Channel channel;

	public Http2Client(EventLoopGroup group, InetSocketAddress addr) throws Exception {
		this.channel = new Bootstrap().group(group).channel(NioSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<Channel>() {

					@Override
					protected void initChannel(Channel ch) throws Exception {
//						ch.pipeline().addLast(new Http2CodecBuilder(false, new ChannelInitializer<Channel>() {
//
//							@Override
//							protected void initChannel(Channel ch) throws Exception {
//								throw new IOException("Stream created from server is not implemented");
//							}
//						}).build());
					}
				}).connect(addr).sync().channel();
	}

	public String hello() throws Exception {
		return "";
//		Http2Headers headers = new DefaultHttp2Headers().scheme(HttpScheme.HTTP.name())
//				.method(HttpMethod.GET.asciiName()).path("/");
//		Channel stream = new Http2StreamChannelBootstrap().parentChannel(channel)
//				.handler(new ChannelInitializer<Channel>() {
//
//					@Override
//					protected void initChannel(Channel ch) throws Exception {
//						ch.pipeline().addLast(new HelloWorldClientHandler());
//					}
//				}).connect().sync().channel();
//		Promise<String> promise = stream.eventLoop().newPromise();
//		stream.writeAndFlush(new HeadersAndPromise(headers, promise));
//		return promise.get();
	}
}
