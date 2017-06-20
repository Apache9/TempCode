package com.github.apache9.http2;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.util.concurrent.Promise;

/**
 * @author zhangduo
 *
 */
public class HelloWorldClientHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

	private final ConcurrentMap<Integer, Promise<String>> streamId2Promise = new ConcurrentHashMap<>();

	private Promise<String> promise;
	
	private ByteBuf buf;

	private void onHeaderRead(Http2HeadersFrame header) {
		promise = streamId2Promise.get(header.streamId());
		if (HttpResponseStatus.OK.codeAsText().equals(header.headers().status())) {
			promise.tryFailure(new IOException("Status: " + header.headers().status()));
		}
	}

	private void onDataRead(ChannelHandlerContext ctx,Http2DataFrame data) {
		if (buf == null) {
			buf = ctx.alloc().buffer();
		}
		buf.writeBytes(data.content());
		if (data.isEndStream()) {
			
		}
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		exceptionCaught(ctx, new IOException("Stream closed"));
		ctx.fireChannelInactive();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (promise != null) {
			
		}
	}

}
