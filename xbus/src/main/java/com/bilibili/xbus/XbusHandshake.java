package com.bilibili.xbus;

import java.io.IOException;

/**
 * @author kaede
 * @version date 16/8/19
 */
public interface XBusHandshake {
    void handshakeWithHost(String clientAddress, MessageReader in, MessageWriter out) throws IOException;

    String handshakeWithClient(String hostAddress, MessageReader in, MessageWriter out) throws IOException;
}
