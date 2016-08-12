/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.net.Credentials;
import android.net.LocalSocket;

/**
 * XBusAuth
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public interface XBusAuth {

    int MODE_SERVER = 1;
    int MODE_CLIENT = 2;

    AuthResult auth(int mode, LocalSocket socket);

    class AuthResult {

        boolean success;
        Credentials credentials;

        AuthResult(boolean success) {
            this.success = success;
        }

        public AuthResult setCredentials(Credentials credentials) {
            this.credentials = credentials;
            return this;
        }
    }
}
