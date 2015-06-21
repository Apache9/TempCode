package com.github.apache9.http2.server;

import static io.netty.handler.codec.http2.Http2CodecUtil.CONNECTION_STREAM_ID;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.PropertyKey;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2EventAdapter;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2FrameReader;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2InboundFrameLogger;
import io.netty.handler.codec.http2.Http2OutboundFrameLogger;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.Http2StreamVisitor;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 * @author zhangduo
 */
public class ServerHttp2EventListener extends Http2EventAdapter {

    private static final Http2FrameLogger FRAME_LOGGER = new Http2FrameLogger(LogLevel.INFO,
            ServerHttp2EventListener.class);

    private final Channel parentChannel;

    private final ChannelInitializer<ServerHttp2StreamChannel> subChannelInitializer;

    private final Http2Connection conn;

    private final PropertyKey subChannelPropKey;

    public ServerHttp2EventListener(Channel parentChannel, Http2Connection conn,
            ChannelInitializer<ServerHttp2StreamChannel> subChannelInitializer) {
        this.parentChannel = parentChannel;
        this.conn = conn;
        this.subChannelInitializer = subChannelInitializer;
        this.subChannelPropKey = conn.newKey();
    }

    @Override
    public void onStreamAdded(final Http2Stream stream) {
        ServerHttp2StreamChannel subChannel = new ServerHttp2StreamChannel(parentChannel, stream);
        stream.setProperty(subChannelPropKey, subChannel);
        subChannel.pipeline().addFirst(subChannelInitializer);
        parentChannel.eventLoop().register(subChannel).addListener(new FutureListener<Void>() {

            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    stream.removeProperty(subChannelPropKey);
                }
            }

        });
    }

    @Override
    public void onStreamActive(Http2Stream stream) {
        ServerHttp2StreamChannel subChannel = stream.getProperty(subChannelPropKey);
        if (subChannel != null) {
            subChannel.pipeline().fireChannelActive();
        }

    }

    @Override
    public void onStreamClosed(Http2Stream stream) {
        ServerHttp2StreamChannel subChannel = stream.removeProperty(subChannelPropKey);
        if (subChannel != null) {
            subChannel.close();
        }
    }

    private ServerHttp2StreamChannel getSubChannel(int streamId) throws Http2Exception {
        ServerHttp2StreamChannel subChannel = conn.stream(streamId).getProperty(subChannelPropKey);
        if (subChannel == null) {
            throw Http2Exception.streamError(streamId, Http2Error.INTERNAL_ERROR,
                    "No sub channel found");
        }
        return subChannel;
    }

    private boolean writeInbound(int streamId, Object msg, boolean endOfStream)
            throws Http2Exception {
        ServerHttp2StreamChannel subChannel = getSubChannel(streamId);
        if (endOfStream) {
            subChannel.writeInbound(new LastMessage(msg));
        } else {
            subChannel.writeInbound(msg);
        }
        if (subChannel.config().isAutoRead()) {
            subChannel.read();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
            int padding, boolean endOfStream) throws Http2Exception {
        writeInbound(streamId, headers, endOfStream);
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
            int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream)
            throws Http2Exception {
        onHeadersRead(ctx, streamId, headers, padding, endOfStream);
    }

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding,
            boolean endOfStream) throws Http2Exception {
        int pendingBytes = data.readableBytes() + padding;
        if (writeInbound(streamId, data.retain(), endOfStream)) {
            return pendingBytes;
        } else {
            return 0;
        }
    }

    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement)
            throws Http2Exception {
        if (streamId == CONNECTION_STREAM_ID) {
            conn.forEachActiveStream(new Http2StreamVisitor() {

                @Override
                public boolean visit(Http2Stream stream) throws Http2Exception {
                    ServerHttp2StreamChannel subChannel = stream.getProperty(subChannelPropKey);
                    if (subChannel != null) {
                        subChannel.tryWrite();
                    }
                    return true;
                }
            });
        } else {
            getSubChannel(streamId).tryWrite();
        }
    }

    public static Http2ConnectionHandler create(Channel channel,
            ChannelInitializer<ServerHttp2StreamChannel> initializer, boolean verbose) {
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
        return new Http2ConnectionHandler(conn, frameReader, frameWriter, listener);
    }
}
