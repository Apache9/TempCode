package com.github.apache9.sendfile;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class BlackHole {

    private static void receive(Socket s) throws IOException {
        SocketAddress addr = s.getRemoteSocketAddress();
        byte[] buf = new byte[4096];
        InputStream in = s.getInputStream();
        long totalReceived = 0L;
        for (;;) {
            int r = in.read(buf);
            if (r < 0) {
                break;
            }
            totalReceived += r;
        }
        System.out.println(totalReceived + " bytes received from " + addr);
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        try (ServerSocket ss = new ServerSocket(Integer.parseInt(args[0]))) {
            for (;;) {
                Socket s = ss.accept();
                new Thread(() -> {
                    try {
                        receive(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            s.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }
}
