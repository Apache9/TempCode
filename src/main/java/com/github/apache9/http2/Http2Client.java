package com.github.apache9.http2;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http2.HttpUtil;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangduo
 */
public class Http2Client {

    public static void main(String[] args) throws Exception {
        args = new String[] {
            "192.168.198.248:29581"
        };
        String addr = args[0];
        String[] hostAndPort = addr.split(":");
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Http2ClientInitializer initializer = new Http2ClientInitializer();
            Bootstrap bootstrap = new Bootstrap().group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1]))
                    .handler(initializer);

            Channel channel = bootstrap.connect().syncUninterruptibly().channel();
            System.out.println("Connected to [" + addr + ']');

            initializer.getSettingsHandler().awaitSettings(120000, TimeUnit.SECONDS);

            int streamId = 11;

            URI hostName = URI.create("http://" + addr);
            System.out.println("Send request to [" + addr + "]");
            FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, hostName.toURL()
                    .toString());
            request.headers().add(HttpHeaderNames.HOST, hostName);
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
            request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.DEFLATE);
            request.headers().add(HttpUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
            HttpResponseHandler responseHandler = initializer.getResponseHandler();
            ChannelPromise promise;
            synchronized (responseHandler) {
                channel.writeAndFlush(request);
                promise = channel.newPromise();
                responseHandler.put(streamId, promise);
            }
            promise.awaitUninterruptibly();
            System.out.println("Finished request to [" + addr + "]");

            channel.close().syncUninterruptibly();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
