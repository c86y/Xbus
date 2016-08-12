/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.net.Credentials;
import android.net.LocalSocket;
import android.os.Process;

import com.alibaba.fastjson.annotation.JSONType;
import com.bilibili.xbus.utils.XBusLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * FastAuth
 *
 * @author chengyuan
 * @data 16/8/5.
 */
public class FastAuth implements XBusAuth {

    @Override
    public AuthResult auth(int mode, LocalSocket socket) {
        try {
            Credentials cre = socket.getPeerCredentials();
            if (cre == null) {
                return new AuthResult(false);
            }

            int uid = cre.getUid();
            if (uid != Process.myUid()) {
                return new AuthResult(false).setCredentials(cre);
            }

            return new AuthResult(true).setCredentials(cre);
        } catch (IOException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }

            return new AuthResult(false);
        }
    }
}
