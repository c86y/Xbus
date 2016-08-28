package com.bilibili.xbus.utils;

import android.content.Context;
import android.net.LocalSocket;

import java.io.Closeable;
import java.io.IOException;

/**
 * XBusUtils
 *
 * @author chengyuan
 */
public class XBusUtils {
    public static final String HOST_SOCKET_NAME = ".XBusHost";

    public static void closeQuietly(LocalSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static String getHostAddress(Context context) {
        return context.getPackageName() + HOST_SOCKET_NAME;
    }
}
