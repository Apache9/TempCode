package com.github.apache9.http2.server;

import java.util.Iterator;
import java.util.List;

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

/**
 * @author zhangduo
 */
public class TestCloseLocalSideChunkedInput extends AbstractTestCloseLocalSide {

    private final class ChunkedHandler extends SimpleChannelInboundHandler<Http2Headers> {

        private final List<byte[]> chunkList;

        public ChunkedHandler(List<byte[]> chunkList) {
            this.chunkList = chunkList;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http2Headers msg) throws Exception {
            ctx.write(new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText()));
            ctx.writeAndFlush(new CloseLocalSideChunkedInput(new ChunkedInput<ByteBuf>() {

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

}
