/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * MessageWriter
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class MessageWriter implements Closeable {

    private OutputStream out;

    public MessageWriter(OutputStream out) {
        this.out = new BufferedOutputStream(out);
    }

    public void write(Message msg) throws IOException {
        XBus.mMarshalling.marshalling(msg, out);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
