package com.github.apache9.http2;

import static io.netty.handler.logging.LogLevel.INFO;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2InboundFrameLogger;
import io.netty.handler.codec.http2.Http2OutboundFrameLogger;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.ssl.SslContext;

/**
 * @author zhangduo
 */
public class Http2SslClientInitializer extends ChannelInitializer<Channel> {

    private static final Http2FrameLogger LOGGER = new Http2FrameLogger(INFO,
            Http2SslClientInitializer.class);

    private final SslContext sslCtx;

    private final HttpResponseHandler responseHandler = new HttpResponseHandler();

    private Http2SettingsHandler settingsHandler;

    public Http2SslClientInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    public HttpResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public Http2SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        settingsHandler = new Http2SettingsHandler(ch.newPromise());
        Http2Connection connection = new DefaultHttp2Connection(false);
        Http2ConnectionHandler connectionHandler = new HttpToHttp2ConnectionHandler(connection,
                frameReader(), frameWriter(), new DelegatingDecompressorFrameListener(connection,
                        new InboundHttp2ToHttpAdapter.Builder(connection)
                                .maxContentLength(Integer.MAX_VALUE).propagateSettings(true)
                                .build()));
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), connectionHandler, settingsHandler,
                responseHandler);
    }

    private static Http2FrameReader frameReader() {
        return new Http2InboundFrameLogger(new DefaultHttp2FrameReader(), LOGGER);
    }

    private static Http2FrameWriter frameWriter() {
        return new Http2OutboundFrameLogger(new DefaultHttp2FrameWriter(), LOGGER);
    }
}
