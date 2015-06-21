/**
 * 
 */
package com.github.apache9.http2.server;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;

/**
 * @author zhangduo
 */
public abstract class AbstractTestHttp2Server {

    protected EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    protected EventLoopGroup workerGroup = new NioEventLoopGroup();

    protected Channel server;

    protected HTTP2Client client = new HTTP2Client();

    protected Session session;

    protected abstract Channel initServer();

    protected final void start() throws Exception {
        server = initServer();
        client.start();
        int port = ((InetSocketAddress) server.localAddress()).getPort();
        FuturePromise<Session> sessionPromise = new FuturePromise<>();
        client.connect(new InetSocketAddress("127.0.0.1", port), new Session.Listener.Adapter(),
                sessionPromise);
        session = sessionPromise.get();
    }

    protected final void stop() throws Exception {
        if (session != null) {
            session.close(ErrorCode.NO_ERROR.code, "", new Callback.Adapter());
        }
        if (server != null) {
            server.close();
        }
        client.stop();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
