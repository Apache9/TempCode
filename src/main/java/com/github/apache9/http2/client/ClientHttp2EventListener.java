package com.github.apache9.http2.client;

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.PropertyKey;

import com.github.apache9.http2.AbstractHttp2EventListener;
import com.github.apache9.http2.Http2StreamChannel;

/**
 * @author zhangduo
 */
class ClientHttp2EventListener extends AbstractHttp2EventListener {

    public ClientHttp2EventListener(Channel parentChannel, Http2Connection conn) {
        super(parentChannel, conn);
    }

    @Override
    protected void initOnStreamAdded(Http2StreamChannel subChannel) {
        subChannel.config().setAutoRead(false);
    }

    PropertyKey getSubChannelPropKey() {
        return subChannelPropKey;
    }
}
