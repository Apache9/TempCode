package com.github.apache9.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.concurrent.Promise;

/**
 * @author zhangduo
 *
 */
public class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {

	private final Promise<Void> promise;

	public Http2SettingsHandler(Promise<Void> promise) {
		this.promise = promise;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Http2Settings msg) throws Exception {
		promise.trySuccess(null);
		ctx.pipeline().remove(this);
	}

	public void await() throws Exception {
		promise.sync();
	}
}
