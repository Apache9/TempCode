/**
 * 
 */
package com.github.apache9.http2.client;

import static org.junit.Assert.assertEquals;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.apache9.http2.Http2StreamChannel;
import com.github.apache9.http2.LastMessage;

/**
 * @author zhangduo
 */
public class TestHttp2Client {

    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    private Server server;

    private final class EchoHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                HttpServletResponse response) throws IOException, ServletException {
            byte[] msg = IOUtils.toByteArray(request.getInputStream());
            response.getOutputStream().write(msg);
            response.getOutputStream().flush();
        }

    }

    private final class ResponseHandler extends ChannelInboundHandlerAdapter {

        private boolean finished = false;

        private Http2Headers headers;

        private byte[] data;

        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            boolean endOfStream = ((Http2StreamChannel) ctx.channel()).remoteSideClosed();
            synchronized (this) {
                if (msg instanceof Http2Headers) {
                    headers = (Http2Headers) msg;
                } else if (msg instanceof ByteBuf) {
                    ByteBuf buf = (ByteBuf) msg;
                    buf.readBytes(bos, buf.readableBytes());
                }
                if (endOfStream) {
                    finished = true;
                    data = bos.toByteArray();
                    notifyAll();
                }
            }
        }

        public synchronized Http2Headers getHeaders() throws InterruptedException {
            while (!finished) {
                wait();
            }
            return headers;
        }

        public synchronized byte[] getData() throws InterruptedException {
            while (!finished) {
                wait();
            }
            return data;
        }
    }

    private Channel channel;

    @Before
    public void setUp() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server, new HTTP2CServerConnectionFactory(
                new HttpConfiguration()));
        connector.setPort(0);
        server.addConnector(connector);
        server.setHandler(new EchoHandler());
        server.start();
        channel = new Bootstrap().group(workerGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(ClientHttp2ConnectionHandler.create(ch, true));
                    }

                }).connect(new InetSocketAddress("127.0.0.1", connector.getLocalPort())).sync()
                .channel();
    }

    @After
    public void tearDown() throws Exception {
        if (channel != null) {
            channel.close();
        }
        if (server != null) {
            server.stop();
        }
        workerGroup.shutdownGracefully();
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        final ResponseHandler respHandler = new ResponseHandler();
        Http2StreamChannel stream = new Http2StreamBootstrap()
                .channel(channel)
                .handler(new ChannelInitializer<Http2StreamChannel>() {

                    @Override
                    protected void initChannel(Http2StreamChannel ch) throws Exception {
                        ch.pipeline().addLast(respHandler);
                    }

                })
                .headers(
                        new DefaultHttp2Headers()
                                .method(new ByteString(HttpMethod.GET.name(),
                                        StandardCharsets.UTF_8))
                                .path(new ByteString("/", StandardCharsets.UTF_8))
                                .scheme(new ByteString("http", StandardCharsets.UTF_8))
                                .authority(
                                        new ByteString("127.0.0.1:"
                                                + ((InetSocketAddress) channel.remoteAddress())
                                                        .getPort(), StandardCharsets.UTF_8)))
                .endStream(false).connect().sync().get();
        stream.writeAndFlush(new LastMessage(stream.alloc().buffer()
                .writeBytes("Hello World".getBytes(StandardCharsets.UTF_8))));
        assertEquals(respHandler.getHeaders().status(), HttpResponseStatus.OK.codeAsText());
        assertEquals("Hello World", new String(respHandler.getData(), StandardCharsets.UTF_8));
    }
}
