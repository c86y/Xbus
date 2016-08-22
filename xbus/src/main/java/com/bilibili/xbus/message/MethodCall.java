package com.bilibili.xbus.message;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bilibili.xbus.proxy.RemoteObject;

/**
 * MethodCall
 *
 * @author chengyuan
 */
public class MethodCall extends Message {

    public MethodCall(@NonNull String source, @NonNull String dest, @NonNull String action, Object... args) {
        this(source, dest, action, null, args);
    }

    public MethodCall(@NonNull String source, @NonNull String dest, @NonNull String action, @Nullable RemoteObject remoteObject, Object... args) {
        super(MessageType.METHOD_CALL, args);
        if (source == null || dest == null || action == null) {
            throw new IllegalArgumentException("Must set source, dest, action to MethodCall");
        }
        this.headers.put(HeaderField.SOURCE, source);
        this.headers.put(HeaderField.DEST, dest);
        this.headers.put(HeaderField.ACTION, action);

        if (remoteObject != null) {
            this.headers.put(HeaderField.REMOTE_OBJECT, remoteObject);
        }
    }

    public MethodCall setCallBackAction(String action) {
        this.headers.put(HeaderField.ACTION_CALLBACK, action);
        return this;
    }

    public String getCallBackAction() {
        return (String) headers.get(HeaderField.ACTION_CALLBACK);
    }
}
