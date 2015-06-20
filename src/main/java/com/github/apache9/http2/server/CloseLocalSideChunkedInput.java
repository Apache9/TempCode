package com.github.apache9.http2.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 * @author zhangduo
 */
public class CloseLocalSideChunkedInput implements ChunkedInput<ByteBuf> {

    private final ChunkedInput<ByteBuf> in;

    public CloseLocalSideChunkedInput(ChunkedInput<ByteBuf> in) {
        this.in = in;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return in.isEndOfInput();
    }

    @Override
    public void close() throws Exception {
        in.close();
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
        ByteBuf chunk = in.readChunk(ctx);
        if (isEndOfInput()) {
            ((ServerHttp2StreamChannel) ctx.channel()).closeLocalSide();
        }
        return chunk;
    }

    @Override
    public long length() {
        return in.length();
    }

    @Override
    public long progress() {
        return in.progress();
    }

}
