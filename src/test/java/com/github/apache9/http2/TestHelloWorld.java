package com.github.apache9.http2;

import static org.junit.Assert.assertTrue;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Apache9
 */
public class TestHelloWorld {

    private static Thread SERVER_THREAD;

    private static int PORT = 29581;

    private static EventLoopGroup GROUP;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        SERVER_THREAD = new Thread(() -> {
            try {
                Http2Server.main(new String[] { Integer.toString(PORT) });
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        });
        SERVER_THREAD.start();
        Thread.sleep(2000);
        GROUP = new NioEventLoopGroup();
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        SERVER_THREAD.interrupt();
        SERVER_THREAD.join();
        GROUP.shutdownGracefully().sync();
    }

    @Test
    public void testHttp() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
        Request req = new Request.Builder().url("http://localhost:" + PORT).build();
        Response resp = client.newCall(req).execute();
        String msg = resp.body().string();
        System.out.println(msg);
        assertTrue(msg.contains("HTTP/1.1"));
    }

    @Test
    public void testHttp2() throws Exception {
        Http2Client client = new Http2Client(GROUP, new InetSocketAddress("localhost", PORT));
        String msg = client.hello();
        System.out.println(msg);
        assertTrue(msg.contains("HTTP/2"));
    }
}
