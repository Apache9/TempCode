package com.github.apache9.http2.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2RemoteFlowController;
import io.netty.handler.codec.http2.Http2Stream;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zhangduo
 */
public class ServerHttp2StreamChannel extends AbstractChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);

    private final ChannelHandlerContext http2ConnHandlerCtx;

    private final Http2Stream connStream;

    private final Http2Stream stream;

    private final Http2LocalFlowController localFlowController;

    private final Http2RemoteFlowController remoteFlowController;

    private final Http2ConnectionEncoder encoder;

    private final DefaultChannelConfig config;

    private final Queue<Object> inboundMessageQueue = new ArrayDeque<>();

    private boolean lastInboundMessageAdded = false;

    private boolean lastOutboundMessageAdded = false;

    private int pendingInboundBytes;

    private boolean writePending = false;

    private enum State {
        OPEN, HALF_CLOSED_LOCAL, HALF_CLOSED_REMOTE, CLOSED
    }

    private AtomicReference<State> state;

    public ServerHttp2StreamChannel(Channel parent, Http2Stream stream) {
        super(parent);
        this.http2ConnHandlerCtx = parent.pipeline().context(Http2ConnectionHandler.class);
        Http2ConnectionHandler connHandler = (Http2ConnectionHandler) http2ConnHandlerCtx.handler();
        this.connStream = connHandler.connection().connectionStream();
        this.stream = stream;
        this.localFlowController = connHandler.connection().local().flowController();
        this.remoteFlowController = connHandler.connection().remote().flowController();
        this.encoder = connHandler.encoder();
        this.config = new DefaultChannelConfig(this);
        this.state = new AtomicReference<ServerHttp2StreamChannel.State>(State.OPEN);
        parent().eventLoop().register(this);
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return state.get() != State.CLOSED;
    }

    @Override
    public boolean isActive() {
        return isOpen();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    private final class Http2Unsafe extends AbstractUnsafe {

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress,
                ChannelPromise promise) {
            throw new UnsupportedOperationException();
        }

        public void forceFlush() {
            super.flush0();
        }
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new Http2Unsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    protected SocketAddress localAddress0() {
        return parent().localAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return parent().remoteAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doClose() throws Exception {
        if (stream.state() != Http2Stream.State.CLOSED) {
            encoder.writeRstStream(http2ConnHandlerCtx, stream.id(),
                    Http2Error.INTERNAL_ERROR.code(), http2ConnHandlerCtx.newPromise());
        }
        state.set(State.CLOSED);
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (pendingInboundBytes > 0) {
            localFlowController.consumeBytes(http2ConnHandlerCtx, stream, pendingInboundBytes);
            pendingInboundBytes = 0;
        }
        for (;;) {
            Object msg = inboundMessageQueue.poll();
            if (msg == null) {
                break;
            }
            if (lastInboundMessageAdded && inboundMessageQueue.isEmpty()) {
                for (;;) {
                    State current = state.get();
                    if (current == State.CLOSED) {
                        throw new ClosedChannelException();
                    }
                    State next = current == State.HALF_CLOSED_LOCAL ? State.CLOSED
                            : State.HALF_CLOSED_REMOTE;
                    if (state.compareAndSet(current, next)) {
                        break;
                    }
                }
            }
            pipeline().fireChannelRead(msg);
        }
        pipeline().fireChannelReadComplete();
    }

    private boolean canWrite() {
        return remoteFlowController.windowSize(stream) > 0
                && remoteFlowController.windowSize(connStream) > 0;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        if (canWrite()) {
            for (;;) {
                Object msg = in.current();
                if (msg == null) {
                    break;
                }
                boolean endOfStream;
                if (lastOutboundMessageAdded && in.size() == 1) {
                    for (;;) {
                        State current = state.get();
                        if (current == State.CLOSED) {
                            throw new ClosedChannelException();
                        }
                        State next = current == State.HALF_CLOSED_REMOTE ? State.CLOSED
                                : State.HALF_CLOSED_LOCAL;
                        if (state.compareAndSet(current, next)) {
                            break;
                        }
                    }
                    endOfStream = true;
                } else {
                    endOfStream = false;
                }
                if (msg instanceof Http2Headers) {
                    encoder.writeHeaders(http2ConnHandlerCtx, stream.id(), (Http2Headers) msg, 0,
                            endOfStream, http2ConnHandlerCtx.newPromise());
                } else if (msg instanceof ByteBuf) {
                    encoder.writeData(http2ConnHandlerCtx, stream.id(), (ByteBuf) msg, 0,
                            endOfStream, http2ConnHandlerCtx.newPromise());

                } else {
                    parent().write(msg);
                }
                in.remove();
            }
            parent().flush();
        } else {
            writePending = true;
        }
    }

    public void writeInbound(Object msg) {
        inboundMessageQueue.add(msg);
    }

    public void incrementPendingInboundBytes(int bytes) {
        pendingInboundBytes += bytes;
    }

    public void tryWrite() {
        if (writePending) {
            writePending = false;
            ((Http2Unsafe) unsafe()).forceFlush();
        }
    }

    public void closeRemoteSide() {
        lastInboundMessageAdded = true;
    }

    public void closeLocalSide() {
        lastOutboundMessageAdded = true;
    }
}
