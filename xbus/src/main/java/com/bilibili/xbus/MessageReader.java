/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * MessageReceiver
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class MessageReader implements Closeable {

    private InputStream in;

    public MessageReader(InputStream in) {
        this.in = new BufferedInputStream(in);
    }

    public Message read() {
        Message msg = null;
        return msg;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
