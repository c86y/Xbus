package com.bilibili.xbus.message;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * MethodCall
 *
 * @author chengyuan
 * @data 16/8/8.
 */
public class MethodCall extends Message {

    public MethodCall(@NonNull String source, @NonNull String dest, @NonNull String action, Object... args) {
        this(source, dest, action, null, args);
    }

    public MethodCall(@NonNull String source, @NonNull String dest, @NonNull String action, @Nullable String iface, Object... args) {
        super(MessageType.METHOD_CALL, args);
        if (source == null || dest == null || action == null) {
            throw new IllegalArgumentException("Must set source, dest, action to MethodCall");
        }
        this.headers.put(HeaderField.SOURCE, source);
        this.headers.put(HeaderField.DEST, dest);
        this.headers.put(HeaderField.ACTION, action);

        if (iface != null) {
            this.headers.put(HeaderField.INTERFACE, iface);
        }
    }
}
