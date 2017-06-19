package com.github.apache9.http2;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Apache9
 */
public class TestHelloWorld {

    private static Thread SERVER_THREAD;

    @BeforeClass
    public static void setUp() {
        SERVER_THREAD = new Thread(() -> {
            try {
                Http2Server.main(new String[] { "29581" });
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        });
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        SERVER_THREAD.interrupt();
        SERVER_THREAD.join();
    }
}
