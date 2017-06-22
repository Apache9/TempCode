package com.github.apache9.http2;

import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Exception.StreamException;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.StreamBufferingEncoder.Http2GoAwayException;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author zhangduo
 */
public class HelloWorldClientHandler extends ChannelDuplexHandler {

    private final Map<Integer, Promise<String>> streamId2Promise = new HashMap<>();

    private final Map<Integer, CompositeByteBuf> streamId2Content = new HashMap<>();

    private void onHeaderRead(Http2HeadersFrame header) {
        Promise<String> promise = streamId2Promise.get(header.streamId());
        if (!HttpResponseStatus.OK.codeAsText().equals(header.headers().status())) {
            promise.tryFailure(new IOException("Status: " + header.headers().status()));
            streamId2Promise.remove(header.streamId());
        }
    }

    private void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data) {
        int streamId = data.streamId();
        ctx.write(
                new DefaultHttp2WindowUpdateFrame(data.content().readableBytes() + data.padding()).streamId(streamId));
        Promise<String> promise = streamId2Promise.get(streamId);
        if (promise == null) {
            data.release();
            return;
        }
        CompositeByteBuf buf = streamId2Content.computeIfAbsent(data.streamId(),
                k -> new CompositeByteBuf(ctx.alloc(), false, Integer.MAX_VALUE));
        buf.addComponent(true, data.content().retain());
        data.release();
        if (!data.isEndStream()) {
            return;
        }
        promise.trySuccess(buf.toString(StandardCharsets.UTF_8));
        buf.release();
        streamId2Promise.remove(streamId);
        streamId2Content.remove(streamId);
    }

    private void onGoAwayRead(ChannelHandlerContext ctx, Http2GoAwayFrame goAway) {
        byte[] debugData = new byte[goAway.content().readableBytes()];
        goAway.content().readBytes(debugData);
        Http2GoAwayException error = new Http2GoAwayException(goAway.lastStreamId(), goAway.errorCode(), debugData);
        if (goAway.lastStreamId() == -1) {
            goAway.release();
            exceptionCaught(ctx, error);
            return;
        }
        for (Iterator<Map.Entry<Integer, Promise<String>>> iter = streamId2Promise.entrySet().iterator(); iter
                .hasNext();) {
            Map.Entry<Integer, Promise<String>> entry = iter.next();
            if (entry.getKey().intValue() > goAway.lastStreamId()) {
                iter.remove();
                entry.getValue().tryFailure(error);
                CompositeByteBuf buf = streamId2Content.remove(entry.getKey());
                if (buf != null) {
                    buf.release();
                }
            }
        }
        goAway.release();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http2HeadersFrame) {
            onHeaderRead((Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else if (msg instanceof Http2GoAwayFrame) {
            onGoAwayRead(ctx, (Http2GoAwayFrame) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HeadersAndPromise) {
            HeadersAndPromise hap = (HeadersAndPromise) msg;
            streamId2Promise.put(hap.headers.streamId(), hap.promise);
            ctx.write(hap.headers, promise);
        } else {
            ctx.write(msg, promise);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        exceptionCaught(ctx, new IOException("Stream closed"));
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof StreamException) {
            int streamId = ((StreamException) cause).streamId();
            Promise<String> promise = streamId2Promise.remove(streamId);
            if (promise != null) {
                promise.tryFailure(cause);
            }
            ReferenceCountUtil.safeRelease(streamId2Content.remove(streamId));
            return;
        }
        streamId2Promise.values().forEach(p -> p.tryFailure(cause));
        streamId2Promise.clear();
        streamId2Content.values().forEach(ReferenceCountUtil::safeRelease);
        streamId2Content.clear();
        ctx.close();
    }

}
