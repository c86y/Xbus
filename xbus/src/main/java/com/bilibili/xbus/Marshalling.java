/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Marshalling
 *
 * @author chengyuan
 * @data 16/8/4.
 */
public interface Marshalling {

    void marshalling(Message msg, OutputStream out) throws IOException;

    Message deMarshalling(InputStream in) throws IOException;
}
