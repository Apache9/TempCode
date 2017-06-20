package com.github.apache9.http2;

import static org.junit.Assert.*;
import java.io.IOException;
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
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        SERVER_THREAD.interrupt();
        SERVER_THREAD.join();
    }

    @Test
    public void testHttp() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();
        Request req = new Request.Builder().url("http://localhost:" + PORT).build();
        Response resp = client.newCall(req).execute();
        assertTrue(resp.body().string().contains("HTTP/1.1"));
    }

    @Test
    public void testHttp2() {

    }
}
