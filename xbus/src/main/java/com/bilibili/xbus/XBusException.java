/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import java.io.IOException;

/**
 * XBusException
 *
 * @author chengyuan
 */
public class XBusException extends IOException {

    private int mErrorCode;

    public XBusException() {
    }

    public XBusException(String detailMessage) {
        super(detailMessage);
    }

    public XBusException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        mErrorCode = errorCode;
    }

    public XBusException(int errorCode, Throwable cause) {
        super(cause);
        mErrorCode = errorCode;
    }

    public int getErrorCode() {
        return mErrorCode;
    }
}
