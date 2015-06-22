/**
 * 
 */
package com.github.apache9.http2.client;

import com.github.apache9.http2.Http2StreamChannel;

import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.Promise;

/**
 * @author zhangduo
 */
class Http2HeadersAndPromise {

    final Http2Headers headers;

    final Promise<Http2StreamChannel> promise;

    Http2HeadersAndPromise(Http2Headers headers, Promise<Http2StreamChannel> promise) {
        this.headers = headers;
        this.promise = promise;
    }

}
