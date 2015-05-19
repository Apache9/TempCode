package com.github.apache9.http2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2OrHttpChooser.SelectedProtocol;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

/**
 * @author zhangduo
 */
public class Http2SslServer {

    public static void main(String[] args) throws CertificateException, SSLException,
            InterruptedException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        final SslContext sslCtx = SslContextBuilder
                .forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(SslProvider.OPENSSL)
                /*
                 * NOTE: the cipher filter may not include all ciphers required
                 * by the HTTP/2 specification. Please refer to the HTTP/2
                 * specification for cipher requirements.
                 */
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
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
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler());
                        }

                    });
            Channel channel = bootstrap.bind(29581).sync().channel();
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
