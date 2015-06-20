/**
 * 
 */
package com.github.apache9.http2.server;

/**
 * @author zhangduo
 */
public interface ServerHttp2StreamChannelInitializer {

    void initChannel(ServerHttp2StreamChannel channel);
}
