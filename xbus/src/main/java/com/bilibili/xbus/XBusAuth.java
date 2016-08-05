/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.net.LocalSocket;

/**
 * XBusAuth
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public interface XBusAuth {
    boolean auth(LocalSocket socket);
}
