package com.github.apache9.http2.server;

import com.google.common.base.Preconditions;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

/**
 * @author zhangduo
 */
public class CloseLocalSideMessage<T> implements ChunkedInput<T> {

    private T msg;

    public CloseLocalSideMessage(T msg) {
        this.msg = Preconditions.checkNotNull(msg);
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return msg == null;
    }

    @Override
    public void close() throws Exception {
        msg = null;
    }

    @Override
    public T readChunk(ChannelHandlerContext ctx) throws Exception {
        if (msg == null) {
            return null;
        }
        ((ServerHttp2StreamChannel)ctx.channel()).closeLocalSide();
        T result = msg;
        msg = null;
        return result;
    }

    @Override
    public long length() {
        return -1;
    }

    @Override
    public long progress() {
        return -1;
    }

}
