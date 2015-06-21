/**
 * 
 */
package com.github.apache9.http2.server;

/**
 * @author zhangduo
 */
public class LastMessage {

    private final Object msg;

    public LastMessage(Object msg) {
        this.msg = msg;
    }

    public Object get() {
        return msg;
    }

}
