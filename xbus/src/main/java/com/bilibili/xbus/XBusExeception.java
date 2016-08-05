/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import java.io.IOException;

/**
 * XBusExeception
 *
 * @author chengyuan
 * @data 16/8/5.
 */
public class XBusExeception extends IOException {
    public XBusExeception() {
    }

    public XBusExeception(String detailMessage) {
        super(detailMessage);
    }

    public XBusExeception(String message, Throwable cause) {
        super(message, cause);
    }

    public XBusExeception(Throwable cause) {
        super(cause);
    }
}
