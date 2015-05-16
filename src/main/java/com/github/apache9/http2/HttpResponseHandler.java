package com.github.apache9.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpUtil;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangduo
 */
public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private Map<Integer, ChannelPromise> streamId2Promise = new HashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        Integer streamId = msg.headers().getInt(HttpUtil.ExtensionHeaderNames.STREAM_ID.text());
        if (streamId == null) {
            System.err.println("HttpResponseHandler unexpected message received: " + msg);
            return;
        }
        ChannelPromise promise;
        synchronized (this) {
            promise = streamId2Promise.get(streamId);
        }
        if (promise == null) {
            System.err.println("Message received for unknown stream id " + streamId);
        } else {
            // Do stuff with the message (for now just print it)
            ByteBuf content = msg.content();
            if (content.isReadable()) {
                int contentLength = content.readableBytes();
                byte[] arr = new byte[contentLength];
                content.readBytes(arr);
                System.out.println(new String(arr, 0, contentLength, CharsetUtil.UTF_8));
            }

            promise.setSuccess();
        }
    }

    public void put(Integer streamId, ChannelPromise promise) {
        streamId2Promise.put(streamId, promise);
    }
}
