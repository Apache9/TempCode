package com.github.apache9.http2;

import io.netty.util.concurrent.Promise;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author zhangduo
 */
public class Http2Client {

    private final ConcurrentMap<Integer, Promise<String>> streamId2Promise = new ConcurrentHashMap<>();

    
}
