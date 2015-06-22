package com.github.apache9.http2.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import com.github.apache9.http2.Http2StreamChannel;
import com.github.apache9.http2.LastMessage;
import com.google.common.base.Preconditions;

/**
 * @author zhangduo
 */
public class Http2StreamBootstrap {

    private Channel channel;

    private Http2Headers headers;

    private boolean endStream;

    private ChannelHandler handler;

    public Http2StreamBootstrap channel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public Http2StreamBootstrap headers(Http2Headers headers) {
        this.headers = headers;
        return this;
    }

    public Http2StreamBootstrap endStream(boolean endStream) {
        this.endStream = endStream;
        return this;
    }

    public Http2StreamBootstrap handler(ChannelHandler handler) {
        this.handler = handler;
        return this;
    }

    public Promise<Http2StreamChannel> connect() {
        Preconditions.checkNotNull(headers);
        Preconditions.checkNotNull(handler);
        final Promise<Http2StreamChannel> registeredPromise = channel.eventLoop()
                .<Http2StreamChannel>newPromise();

        Http2HeadersAndPromise headersAndPromise = new Http2HeadersAndPromise(headers, channel
                .eventLoop().<Http2StreamChannel>newPromise()
                .addListener(new FutureListener<Http2StreamChannel>() {

                    @Override
                    public void operationComplete(Future<Http2StreamChannel> future)
                            throws Exception {
                        if (future.isSuccess()) {
                            final Http2StreamChannel subChannel = future.get();
                            subChannel.pipeline().addFirst(handler);
                            channel.eventLoop().register(subChannel)
                                    .addListener(new ChannelFutureListener() {

                                        @Override
                                        public void operationComplete(ChannelFuture future)
                                                throws Exception {
                                            if (future.isSuccess()) {
                                                subChannel.config().setAutoRead(true);
                                                registeredPromise.setSuccess(subChannel);
                                            } else {
                                                registeredPromise.setFailure(future.cause());
                                            }
                                        }
                                    });
                        } else {
                            registeredPromise.setFailure(future.cause());
                        }
                    }

                }));
        channel.writeAndFlush(endStream ? new LastMessage(headersAndPromise) : headersAndPromise);
        return registeredPromise;
    }
}
