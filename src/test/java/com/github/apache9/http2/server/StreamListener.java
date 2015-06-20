package com.github.apache9.http2.server;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http.MetaData.Response;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.util.Callback;

/**
 * @author zhangduo
 */
public class StreamListener extends Stream.Listener.Adapter {

    private boolean finish = false;

    private byte[] buf = new byte[0];

    private int status = -1;

    private boolean reset;

    @Override
    public void onData(Stream stream, DataFrame frame, Callback callback) {
        synchronized (this) {
            if (reset) {
                callback.failed(new IllegalStateException("Stream already closed"));
            }
            if (status == -1) {
                callback.failed(new IllegalStateException("Haven't received header yet"));
            }
            int bufLen = buf.length;
            int newBufLen = bufLen + frame.getData().remaining();
            buf = Arrays.copyOf(buf, newBufLen);
            frame.getData().get(buf, bufLen, frame.getData().remaining());
            if (frame.isEndStream()) {
                finish = true;
            }
            notifyAll();
            callback.succeeded();
        }
    }

    @Override
    public void onHeaders(Stream stream, HeadersFrame frame) {
        synchronized (this) {
            if (reset) {
                throw new IllegalStateException("Stream already closed");
            }
            if (status != -1) {
                throw new IllegalStateException("Header already received");
            }
            MetaData meta = frame.getMetaData();
            if (!meta.isResponse()) {
                throw new IllegalStateException("Received non-response header");
            }
            status = ((Response) meta).getStatus();
            if (frame.isEndStream()) {
                finish = true;
                notifyAll();
            }
        }
    }

    @Override
    public void onReset(Stream stream, ResetFrame frame) {
        synchronized (this) {
            reset = true;
            finish = true;
            notifyAll();
        }
    }

    public int getStatus() throws InterruptedException, IOException {
        synchronized (this) {
            while (!finish) {
                wait();
            }
            if (reset) {
                throw new IOException("Stream reset");
            }
            return status;
        }
    }

    public byte[] getData() throws InterruptedException, IOException {
        synchronized (this) {
            while (!finish) {
                wait();
            }
            if (reset) {
                throw new IOException("Stream reset");
            }
            return buf;
        }
    }
}
