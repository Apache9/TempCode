package com.github.apache9.http2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.concurrent.Promise;

/**
 * @author zhangduo
 *
 */
public class HelloWorldClientHandler extends ChannelDuplexHandler {

	private Promise<String> promise;

	private CompositeByteBuf buf;

	private void onHeaderRead(Http2HeadersFrame header) {
		if (!HttpResponseStatus.OK.codeAsText().equals(header.headers().status())) {
			promise.tryFailure(new IOException("Status: " + header.headers().status()));
			promise = null;
		}
	}

	private void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) {
		if (promise == null) {
			data.release();
			return;
		}
		if (buf == null) {
			buf = new CompositeByteBuf(ctx.alloc(), false, Integer.MAX_VALUE);
		}
		buf.addComponent(true, data.content().retain());
		data.release();
		if (!data.isEndStream()) {
			return;
		}
		promise.trySuccess(buf.toString(StandardCharsets.UTF_8));
		buf.release();
		buf = null;
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof HeadersAndPromise) {
			HeadersAndPromise hap = (HeadersAndPromise) msg;
			this.promise = hap.promise;
			ctx.write(new DefaultHttp2HeadersFrame(hap.headers), promise);
		} else {
			ctx.write(msg, promise);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof Http2HeadersFrame) {
			onHeaderRead((Http2HeadersFrame) msg);
		} else if (msg instanceof Http2DataFrame) {
			onDataRead(ctx, (Http2DataFrame) msg);
		} else {
			ctx.fireChannelRead(msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		exceptionCaught(ctx, new IOException("Stream closed"));
		ctx.fireChannelInactive();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (promise != null) {
			promise.tryFailure(cause);
			promise = null;
		}
	}

}
