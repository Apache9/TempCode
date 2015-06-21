/**
 * 
 */
package com.github.apache9.http2.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 * @author zhangduo
 */
public class LastChunkedInput implements ChunkedInput<Object> {

    private final ChunkedInput<ByteBuf> in;

    public LastChunkedInput(ChunkedInput<ByteBuf> in) {
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
    public Object readChunk(ChannelHandlerContext ctx) throws Exception {
        if (isEndOfInput()) {
            return null;
        }
        ByteBuf chunk = in.readChunk(ctx);
        if (isEndOfInput()) {
            return new LastMessage(chunk);
        } else {
            return chunk;
        }
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
