package com.github.apache9.http2.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PriorityFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author zhangduo
 */
public abstract class AbstractTestCloseLocalSide {

    protected List<byte[]> expectedChunkList;

    protected byte[] combinedChunkes;

    protected EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    protected EventLoopGroup workerGroup = new NioEventLoopGroup();

    private Channel server;

    private HTTP2Client client = new HTTP2Client();

    private Session session;

    protected abstract Channel initServer();

    @Before
    public void setUp() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        expectedChunkList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            byte[] chunk = new byte[ThreadLocalRandom.current().nextInt(10, 50)];
            ThreadLocalRandom.current().nextBytes(chunk);
            expectedChunkList.add(chunk);
            bos.write(chunk);
        }
        combinedChunkes = bos.toByteArray();
        server = initServer();
        client.start();
        int port = ((InetSocketAddress) server.localAddress()).getPort();
        FuturePromise<Session> sessionPromise = new FuturePromise<>();
        client.connect(new InetSocketAddress("127.0.0.1", port), new Session.Listener.Adapter(),
                sessionPromise);
        session = sessionPromise.get();
    }

    @After
    public void tearDown() throws Exception {
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

    @Test
    public void test() throws InterruptedException, ExecutionException, IOException {
        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.C_METHOD, HttpMethod.GET.asString());
        fields.put(HttpHeader.C_PATH, "/");
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        StreamListener listener = new StreamListener();
        session.newStream(new HeadersFrame(1, new MetaData(
                org.eclipse.jetty.http.HttpVersion.HTTP_2, fields), new PriorityFrame(1, 0, 1,
                false), true), streamPromise, listener);
        assertEquals(HttpStatus.OK_200, listener.getStatus());
        assertArrayEquals(combinedChunkes, listener.getData());
    }
}
