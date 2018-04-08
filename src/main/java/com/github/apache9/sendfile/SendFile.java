package com.github.apache9.sendfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SendFile {

    private static void connect(Selector selector, SocketChannel sc, InetSocketAddress addr) throws IOException {
        sc.register(selector, SelectionKey.OP_CONNECT);
        sc.connect(addr);
        while (selector.select() == 0) {

        }
        SelectionKey key = selector.selectedKeys().iterator().next();
        selector.selectedKeys().clear();
        assert key.isAcceptable();
        if (!sc.finishConnect()) {
            throw new IOException("connect failed");
        }
    }

    private static void send(Selector selector, SocketChannel sc, FileChannel fc, long length) throws IOException {
        sc.register(selector, SelectionKey.OP_WRITE);
        for (long position = 0; position < length;) {
            if (selector.select() == 0) {
                continue;
            }
            SelectionKey key = selector.selectedKeys().iterator().next();
            selector.selectedKeys().clear();
            assert key.isWritable();
            while (position < length) {
                long transferred = fc.transferTo(position, length - position, sc);
                if (transferred == 0) {
                    break;
                }
                if (transferred < 0) {
                    throw new IOException("EOF?");
                }
                position += transferred;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String file = args[0];
        String[] hostPort = args[1].split(":");
        try (Selector selector = Selector.open();
                SocketChannel sc = SocketChannel.open();
                FileInputStream in = new FileInputStream(file)) {
            sc.configureBlocking(false);
            connect(selector, sc, new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1])));
            send(selector, sc, in.getChannel(), new File(file).length());
        }
    }
}
