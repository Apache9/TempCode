package com.github.apache9.http2.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import com.github.apache9.http2.AbstractHttp2EventListener;
import com.github.apache9.http2.Http2StreamChannel;

/**
 * @author zhangduo
 */
class ServerHttp2EventListener extends AbstractHttp2EventListener {

    private final ChannelInitializer<Http2StreamChannel> subChannelInitializer;

    ServerHttp2EventListener(Channel parentChannel, Http2Connection conn,
            ChannelInitializer<Http2StreamChannel> subChannelInitializer) {
        super(parentChannel, conn);
        this.subChannelInitializer = subChannelInitializer;
    }

    @Override
    protected void initOnStreamAdded(final Http2StreamChannel subChannel) {
        subChannel.pipeline().addFirst(subChannelInitializer);
        parentChannel.eventLoop().register(subChannel).addListener(new FutureListener<Void>() {

            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    subChannel.stream().removeProperty(subChannelPropKey);
                }
            }

        });
    }

    @Override
    public void onStreamActive(Http2Stream stream) {
        Http2StreamChannel subChannel = stream.getProperty(subChannelPropKey);
        if (subChannel != null) {
            subChannel.pipeline().fireChannelActive();
        }

    }

}
