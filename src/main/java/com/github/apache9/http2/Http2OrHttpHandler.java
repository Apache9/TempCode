package com.github.apache9.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2OrHttpChooser.SelectedProtocol;
import io.netty.handler.ssl.SslHandler;

/**
 * @author zhangduo
 */
public class Http2OrHttpHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private void configureHttp1(ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec(), new HelloWorldHttp1Handler());
    }

    private void configureHttp2(ChannelPipeline pipeline) {
        pipeline.addLast(new HelloWorldHttp2Handler());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
        String protocol = sslHandler.engine().getSession().getProtocol();
        if (!protocol.contains(":")) {
            // this should be a request without alpn, just fallback to http/1.1
            configureHttp1(ctx.pipeline());
        } else {
            SelectedProtocol selectedProtocol = SelectedProtocol.protocol(protocol.split(":")[1]);
            System.err.println("Selected Protocol is " + selectedProtocol);
            switch (selectedProtocol) {
                case HTTP_2:
                    configureHttp2(ctx.pipeline());
                    break;
                case HTTP_1_0:
                case HTTP_1_1:
                    configureHttp1(ctx.pipeline());
                    break;
                default:
                    throw new IllegalStateException("Unknown SelectedProtocol");
            }
        }
        ctx.pipeline().remove(this);
        ctx.fireChannelRead(msg.retain());
    }

}
