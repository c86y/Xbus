/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.net.Credentials;
import android.net.LocalSocket;
import android.os.Process;

import java.io.IOException;

/**
 * MyXBusAuth
 *
 * @author chengyuan
 * @data 16/8/5.
 */
public class MyXBusAuth implements XBusAuth {
    @Override
    public boolean auth(LocalSocket socket) {
        try {
            Credentials cre = socket.getPeerCredentials();
            if (cre.getUid() == Process.myUid()) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
