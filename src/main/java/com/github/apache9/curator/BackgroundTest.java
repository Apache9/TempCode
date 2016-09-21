package com.github.apache9.curator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.hadoop.hbase.HBaseTestingUtility;

public class BackgroundTest {

    private static void testNoNode(CuratorFramework zk) throws Exception {
        CompletableFuture<byte[]> future = new CompletableFuture<byte[]>();
        zk.getData().inBackground(new BackgroundCallback() {

            @Override
            public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                System.out.println("Get event " + event + ", data = " + event.getData());
                future.complete(event.getData());
            }
        }).withUnhandledErrorListener(new UnhandledErrorListener() {

            @Override
            public void unhandledError(String message, Throwable e) {
                System.out.println("Get error msg = " + message);
                e.printStackTrace(System.out);
                future.completeExceptionally(e);
            }
        }).forPath("/whatever");
        future.get();
    }

    private static void testErrorHandle(CuratorFramework zk) throws Exception {
        CompletableFuture<byte[]> future = new CompletableFuture<byte[]>();
        zk.getData().inBackground(new BackgroundCallback() {

            @Override
            public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                throw new RuntimeException("oops");
            }
        }).withUnhandledErrorListener(new UnhandledErrorListener() {

            @Override
            public void unhandledError(String message, Throwable e) {
                // seems can not catch the exception thrown from above method.
                System.out.println("Get error msg = " + message);
                e.printStackTrace(System.out);
                future.completeExceptionally(e);
            }
        }).forPath("/whatever");
        future.get(5, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        HBaseTestingUtility util = new HBaseTestingUtility();
        util.startMiniZKCluster(1);
        try (CuratorFramework zk = CuratorFrameworkFactory.newClient("127.0.0.1:" + util.getZkCluster().getClientPort(),
                new RetryNTimes(3, 1000))) {
            zk.start();
            testNoNode(zk);
            testErrorHandle(zk);
        } finally {
            util.shutdownMiniZKCluster();
        }
    }
}
