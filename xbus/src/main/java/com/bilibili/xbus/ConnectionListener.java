package com.bilibili.xbus;

/**
 * ConnectionListener
 *
 * @author chengyuan
 */
public interface ConnectionListener {
    void onConnected(Connection conn);

    void onDisconnected();
}
