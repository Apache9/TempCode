/**
 * 
 */
package com.github.apache9.http2.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.PropertyKey;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2InboundFrameLogger;
import io.netty.handler.codec.http2.Http2OutboundFrameLogger;
import io.netty.handler.logging.LogLevel;

import com.github.apache9.http2.Http2StreamChannel;
import com.github.apache9.http2.LastMessage;

/**
 * @author zhangduo
 */
public class ClientHttp2ConnectionHandler extends Http2ConnectionHandler {

    private static final Http2FrameLogger FRAME_LOGGER = new Http2FrameLogger(LogLevel.INFO,
            ClientHttp2ConnectionHandler.class);

    private int nextStreamId = 3;

    private final PropertyKey subChannelPropKey;

    private ClientHttp2ConnectionHandler(Http2Connection connection, Http2FrameReader frameReader,
            Http2FrameWriter frameWriter, ClientHttp2EventListener listener) {
        super(connection, frameReader, frameWriter, listener);
        subChannelPropKey = listener.getSubChannelPropKey();
    }

    private int nextStreamId() {
        int streamId = nextStreamId;
        nextStreamId += 2;
        return streamId;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        boolean endStream;
        if (msg instanceof LastMessage) {
            msg = ((LastMessage) msg).get();
            endStream = true;
        } else {
            endStream = false;
        }
        if (msg instanceof Http2HeadersAndPromise) {
            final Http2HeadersAndPromise headersAndPromise = (Http2HeadersAndPromise) msg;
            final int streamId = nextStreamId();
            Http2ConnectionEncoder encoder = encoder();
            encoder.writeHeaders(ctx, streamId, headersAndPromise.headers, 0, endStream, promise)
                    .addListener(new ChannelFutureListener() {

                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                headersAndPromise.promise.setSuccess(connection().stream(streamId)
                                        .<Http2StreamChannel>getProperty(subChannelPropKey));
                            } else {
                                headersAndPromise.promise.setFailure(future.cause());
                            }
                        }
                    });
        } else {
            throw new UnsupportedMessageTypeException(msg, Http2Headers.class);
        }
    }

    public static ClientHttp2ConnectionHandler create(Channel channel, boolean verbose) {
        Http2Connection conn = new DefaultHttp2Connection(false);
        ClientHttp2EventListener listener = new ClientHttp2EventListener(channel, conn);
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
        return new ClientHttp2ConnectionHandler(conn, frameReader, frameWriter, listener);
    }
}
