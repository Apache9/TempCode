package com.github.apache9.http2;

import static io.netty.handler.codec.http2.Http2CodecUtil.CONNECTION_STREAM_ID;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.PropertyKey;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2EventAdapter;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.Http2StreamVisitor;

/**
 * @author zhangduo
 */
public abstract class AbstractHttp2EventListener extends Http2EventAdapter {

    protected final Channel parentChannel;

    protected final Http2Connection conn;

    protected final PropertyKey subChannelPropKey;

    protected AbstractHttp2EventListener(Channel parentChannel, Http2Connection conn) {
        this.parentChannel = parentChannel;
        this.conn = conn;
        this.subChannelPropKey = conn.newKey();
    }

    protected abstract void initOnStreamAdded(Http2StreamChannel subChannel);

    @Override
    public void onStreamAdded(final Http2Stream stream) {
        Http2StreamChannel subChannel = new Http2StreamChannel(parentChannel, stream);
        stream.setProperty(subChannelPropKey, subChannel);
        initOnStreamAdded(subChannel);
    }

    @Override
    public void onStreamClosed(Http2Stream stream) {
        Http2StreamChannel subChannel = stream.removeProperty(subChannelPropKey);
        if (subChannel != null) {
            subChannel.close();
        }
    }

    private Http2StreamChannel getSubChannel(int streamId) throws Http2Exception {
        Http2StreamChannel subChannel = conn.stream(streamId).getProperty(subChannelPropKey);
        if (subChannel == null) {
            throw Http2Exception.streamError(streamId, Http2Error.INTERNAL_ERROR,
                    "No sub channel found");
        }
        return subChannel;
    }

    private boolean writeInbound(int streamId, Object msg, boolean endOfStream)
            throws Http2Exception {
        Http2StreamChannel subChannel = getSubChannel(streamId);
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
                    Http2StreamChannel subChannel = stream.getProperty(subChannelPropKey);
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
}
