package com.github.apache9.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2CodecBuilder;
import io.netty.handler.codec.http2.Http2CodecUtil;

import java.util.List;

/**
 * A port unification handler to support HTTP/1.1 and HTTP/2 on the same port.
 */
public class PortUnificationServerHandler extends ByteToMessageDecoder {

    private static final ByteBuf HTTP2_CLIENT_CONNECTION_PREFACE = Http2CodecUtil.connectionPrefaceBuf();

    // we only want to support HTTP/1.1 and HTTP/2, so the first 3 bytes is
    // enough. No HTTP/1.1 request could start with "PRI"
    private static final int MAGIC_HEADER_LENGTH = 3;

    private void configureHttp1(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new HttpServerCodec(), new HelloWorldHttp1Handler());
    }

    private void configureHttp2(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new Http2CodecBuilder(true, new HelloWorldHttp2Handler()).build());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < MAGIC_HEADER_LENGTH) {
            return;
        }
        if (ByteBufUtil.equals(in, 0, HTTP2_CLIENT_CONNECTION_PREFACE, 0, MAGIC_HEADER_LENGTH)) {
            configureHttp2(ctx);
        } else {
            configureHttp1(ctx);
        }
        ctx.pipeline().remove(this);
    }

}