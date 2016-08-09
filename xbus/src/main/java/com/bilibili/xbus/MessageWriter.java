/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.utils.XBusLog;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * MessageWriter
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class MessageWriter implements Closeable {

    private String name;
    private ObjectOutputStream out;

    public MessageWriter(String name, OutputStream out) {
        try {
            this.name = name;
            this.out = new ObjectOutputStream(out);
        } catch (IOException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }

            XBus.closeQuietly(this);
        }
    }

    public void write(Message msg) throws IOException {
        if (out == null) {
            throw new XBusException("Output stream is closed!");
        }

        if (XBusLog.ENABLE) {
            XBusLog.d(name + " write msg: " + msg);
        }

        out.writeObject(msg);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
        out = null;
    }
}
