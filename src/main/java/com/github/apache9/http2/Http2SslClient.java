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
import io.netty.handler.codec.http2.Http2OrHttpChooser.SelectedProtocol;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.HttpUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangduo
 */
public class Http2SslClient {

    public static void main(String[] args) throws Exception {
        String addr = args[0];
        String[] hostAndPort = addr.split(":");

        final SslContext sslCtx = SslContextBuilder
                .forClient()
                .sslProvider(SslProvider.OPENSSL)
                /*
                 * NOTE: the cipher filter may not include all ciphers required
                 * by the HTTP/2 specification. Please refer to the HTTP/2
                 * specification for cipher requirements.
                 */
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .applicationProtocolConfig(
                        new ApplicationProtocolConfig(Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by
                        // both OpenSsl and JDK providers.
                                SelectorFailureBehavior.NO_ADVERTISE,
                                // ACCEPT is currently the only mode supported
                                // by both OpenSsl and JDK providers.
                                SelectedListenerFailureBehavior.ACCEPT, SelectedProtocol.HTTP_2
                                        .protocolName(), SelectedProtocol.HTTP_1_1.protocolName()))
                .build();

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Http2SslClientInitializer initializer = new Http2SslClientInitializer(sslCtx);

            Bootstrap bootstrap = new Bootstrap().group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1]))
                    .handler(initializer);

            Channel channel = bootstrap.connect().syncUninterruptibly().channel();
            System.out.println("Connected to [" + addr + ']');

            initializer.getSettingsHandler().awaitSettings(2, TimeUnit.SECONDS);

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
