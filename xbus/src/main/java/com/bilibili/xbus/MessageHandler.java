package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;

/**
 * MessageHandler
 *
 * @author chengyuan
 */
public interface MessageHandler {

    void handleMessage(Message msg);
}
