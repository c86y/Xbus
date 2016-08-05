/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import java.io.IOException;

/**
 * XBusException
 *
 * @author chengyuan
 * @data 16/8/5.
 */
public class XBusException extends IOException {
    public XBusException() {
    }

    public XBusException(String detailMessage) {
        super(detailMessage);
    }

    public XBusException(String message, Throwable cause) {
        super(message, cause);
    }

    public XBusException(Throwable cause) {
        super(cause);
    }
}
