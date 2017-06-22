
package com.github.apache9.http2;

import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.Promise;

/**
 * @author zhangduo
 */
public class HeadersAndPromise {

	public final Http2Headers headers;

	public final Promise<String> promise;

	public HeadersAndPromise(Http2Headers headers, Promise<String> promise) {
		this.headers = headers;
		this.promise = promise;
	}
}
