package com.github.apache9.http2.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.ErrorCode;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PriorityFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author zhangduo
 */
public class TestHttp2ServerMultiThread extends AbstractTestHttp2Server {

    private final class DispatchHandler extends SimpleChannelInboundHandler<Http2Headers> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2Headers msg) throws Exception {
            ServerHttp2StreamChannel channel = (ServerHttp2StreamChannel) ctx.channel();
            if (channel.remoteSideClosed()) {
                channel.closeLocalSide();
            }
            ctx.writeAndFlush(new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText()));
            ctx.pipeline().replace(this, "echo", new EchoHandler());
        }
    }

    private final class EchoHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            ServerHttp2StreamChannel channel = (ServerHttp2StreamChannel) ctx.channel();
            if (channel.remoteSideClosed()) {
                channel.closeLocalSide();
            }
            ctx.writeAndFlush(msg.retain());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            handlerClosedCount.incrementAndGet();
        }

    }

    private final AtomicInteger handlerClosedCount = new AtomicInteger(0);

    private int concurrency = 10;

    private ExecutorService executor = Executors.newFixedThreadPool(concurrency,
            new ThreadFactoryBuilder().setNameFormat("Echo-Client-%d").setDaemon(true).build());

    private int requestCount = 10000;

    @Override
    protected Channel initServer() {
        return new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(
                                ServerHttp2EventListener.create(ch,
                                        new ChannelInitializer<ServerHttp2StreamChannel>() {

                                            @Override
                                            protected void initChannel(ServerHttp2StreamChannel ch)
                                                    throws Exception {
                                                ch.pipeline().addLast(new DispatchHandler());
                                            }
                                        }, true));
                    }

                }).bind(0).syncUninterruptibly().channel();
    }

    @Before
    public void setUp() throws Exception {
        start();
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
        stop();
    }

    private void testEcho() throws InterruptedException, ExecutionException, IOException {
        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.C_METHOD, HttpMethod.GET.asString());
        fields.put(HttpHeader.C_PATH, "/");
        FuturePromise<Stream> streamPromise = new FuturePromise<>();
        StreamListener listener = new StreamListener();
        session.newStream(new HeadersFrame(1, new MetaData(
                org.eclipse.jetty.http.HttpVersion.HTTP_2, fields), new PriorityFrame(1, 0, 1,
                false), false), streamPromise, listener);
        Stream stream = streamPromise.get();
        if (ThreadLocalRandom.current().nextInt(5) < 1) { // 20%
            stream.reset(new ResetFrame(stream.getId(), ErrorCode.NO_ERROR.code),
                    new Callback.Adapter());
        } else {
            int numFrames = ThreadLocalRandom.current().nextInt(1, 3);
            ByteArrayOutputStream msg = new ByteArrayOutputStream();
            for (int i = 0; i < numFrames; i++) {
                byte[] frame = new byte[ThreadLocalRandom.current().nextInt(10, 100)];
                ThreadLocalRandom.current().nextBytes(frame);
                stream.data(new DataFrame(stream.getId(), ByteBuffer.wrap(frame),
                        i == numFrames - 1), new Callback.Adapter());
                msg.write(frame);
            }
            assertEquals(HttpStatus.OK_200, listener.getStatus());
            assertArrayEquals(msg.toByteArray(), listener.getData());
        }
    }

    @Test
    public void test() throws InterruptedException {
        final AtomicBoolean succ = new AtomicBoolean(true);
        for (int i = 0; i < requestCount; i++) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        testEcho();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        succ.set(false);
                    }
                }
            });
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.MINUTES));
        assertTrue(succ.get());
        Thread.sleep(1000);
        assertEquals(requestCount, handlerClosedCount.get());
    }

}
