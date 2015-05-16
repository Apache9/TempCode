package com.github.apache9.http2;

import static io.netty.handler.logging.LogLevel.INFO;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2InboundFrameLogger;
import io.netty.handler.codec.http2.Http2OutboundFrameLogger;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;

/**
 * @author zhangduo
 */
public class Http2ClientInitializer extends ChannelInitializer<Channel> {

    private static final Http2FrameLogger LOGGER = new Http2FrameLogger(INFO,
            Http2ClientInitializer.class);

    private final HttpResponseHandler responseHandler = new HttpResponseHandler();

    private Http2SettingsHandler settingsHandler;

    public HttpResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public Http2SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    /**
     * A handler that triggers the cleartext upgrade to HTTP/2 by sending an initial HTTP request.
     */
    private static final class UpgradeRequestHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            DefaultFullHttpRequest upgradeRequest = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
            ctx.writeAndFlush(upgradeRequest);

            ctx.fireChannelActive();

            // Done with this handler, remove it from the pipeline.
            ctx.pipeline().remove(this);

        }
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        settingsHandler = new Http2SettingsHandler(ch.newPromise());
        HttpClientCodec sourceCodec = new HttpClientCodec();
        Http2Connection connection = new DefaultHttp2Connection(false);
        Http2ConnectionHandler connectionHandler = new HttpToHttp2ConnectionHandler(connection,
                frameReader(), frameWriter(), new DelegatingDecompressorFrameListener(connection,
                        new InboundHttp2ToHttpAdapter.Builder(connection)
                                .maxContentLength(Integer.MAX_VALUE).propagateSettings(true)
                                .build()));
        Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(connectionHandler);
        HttpClientUpgradeHandler upgradeHandler = new HttpClientUpgradeHandler(sourceCodec,
                upgradeCodec, 65536);

        ch.pipeline().addLast("Http2SourceCodec", sourceCodec);
        ch.pipeline().addLast("Http2UpgradeHandler", upgradeHandler);
        ch.pipeline().addLast("Http2UpgradeRequestHandler", new UpgradeRequestHandler());
        ch.pipeline().addLast("Http2SettingsHandler", settingsHandler);
        ch.pipeline().addLast("HttpResponseHandler", responseHandler);
    }

    private static Http2FrameReader frameReader() {
        return new Http2InboundFrameLogger(new DefaultHttp2FrameReader(), LOGGER);
    }

    private static Http2FrameWriter frameWriter() {
        return new Http2OutboundFrameLogger(new DefaultHttp2FrameWriter(), LOGGER);
    }
}
