package com.github.apache9.http2.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PriorityFrame;
import org.eclipse.jetty.util.FuturePromise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author zhangduo
 */
public class TestLastChunkedInput extends AbstractTestHttp2Server {

    protected List<byte[]> expectedChunkList;

    protected byte[] combinedChunkes;

    private final class ChunkedHandler extends SimpleChannelInboundHandler<Http2Headers> {

        private final List<byte[]> chunkList;

        public ChunkedHandler(List<byte[]> chunkList) {
            this.chunkList = chunkList;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2Headers msg) throws Exception {
            ctx.write(new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText()));
            ctx.writeAndFlush(new LastChunkedInput(new ChunkedInput<ByteBuf>() {

                private Iterator<byte[]> iter = chunkList.iterator();

                @Override
                public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
                    if (isEndOfInput()) {
                        return null;
                    }
                    return ctx.alloc().buffer().writeBytes(iter.next());
                }

                @Override
                public long progress() {
                    return -1;
                }

                @Override
                public long length() {
                    return -1;
                }

                @Override
                public boolean isEndOfInput() throws Exception {
                    return !iter.hasNext();
                }

                @Override
                public void close() throws Exception {
                    while (iter.hasNext()) {
                        iter.next();
                    }
                }
            })).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }

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
        start();
    }

    @After
    public void tearDown() throws Exception {
        stop();
    }

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
                                                ch.pipeline().addLast(new ChunkedWriteHandler(),
                                                        new ChunkedHandler(expectedChunkList));
                                            }
                                        }, true));
                    }

                }).bind(0).syncUninterruptibly().channel();
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
