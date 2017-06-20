package com.github.apache9.http2;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.unreleasableBuffer;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.CharsetUtil;

/**
 * @author zhangduo
 */
@Sharable
public class HelloWorldHttp2Handler extends SimpleChannelInboundHandler<Http2HeadersFrame> {

    static final ByteBuf RESPONSE_BYTES = unreleasableBuffer(copiedBuffer("Hello World", CharsetUtil.UTF_8));

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2HeadersFrame msg) throws Exception {
        Http2Headers headers = new DefaultHttp2Headers().status(OK.codeAsText());
        ctx.write(new DefaultHttp2HeadersFrame(headers));
        ByteBuf content = ctx.alloc().buffer();
        content.writeBytes(RESPONSE_BYTES.duplicate());
        ByteBufUtil.writeAscii(content, " - via HTTP/2");
        ctx.writeAndFlush(new DefaultHttp2DataFrame(content, true));
    }
}
