/**
 * 
 */
package com.github.apache9.http2.server;

import com.github.apache9.http2.Http2StreamChannel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2InboundFrameLogger;
import io.netty.handler.codec.http2.Http2OutboundFrameLogger;
import io.netty.handler.logging.LogLevel;

/**
 * @author zhangduo
 */
public class ServerHttp2ConnectionHandler extends Http2ConnectionHandler {

    private static final Http2FrameLogger FRAME_LOGGER = new Http2FrameLogger(LogLevel.INFO,
            ServerHttp2ConnectionHandler.class);

    private ServerHttp2ConnectionHandler(Http2Connection connection, Http2FrameReader frameReader,
            Http2FrameWriter frameWriter, ServerHttp2EventListener listener) {
        super(connection, frameReader, frameWriter, listener);
    }

    public static ServerHttp2ConnectionHandler create(Channel channel,
            ChannelInitializer<Http2StreamChannel> initializer, boolean verbose) {
        Http2Connection conn = new DefaultHttp2Connection(true);
        ServerHttp2EventListener listener = new ServerHttp2EventListener(channel, conn, initializer);
        conn.addListener(listener);
        Http2FrameReader frameReader;
        Http2FrameWriter frameWriter;
        if (verbose) {
            frameReader = new Http2InboundFrameLogger(new DefaultHttp2FrameReader(), FRAME_LOGGER);
            frameWriter = new Http2OutboundFrameLogger(new DefaultHttp2FrameWriter(), FRAME_LOGGER);
        } else {
            frameReader = new DefaultHttp2FrameReader();
            frameWriter = new DefaultHttp2FrameWriter();
        }
        return new ServerHttp2ConnectionHandler(conn, frameReader, frameWriter, listener);
    }
}
