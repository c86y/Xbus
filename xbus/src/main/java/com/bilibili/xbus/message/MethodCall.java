package com.bilibili.xbus.message;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * MethodCall
 *
 * @author chengyuan
 * @data 16/8/8.
 */
public class MethodCall extends Message {

    public MethodCall(@NonNull String source, @NonNull String dest, @NonNull String action, Serializable... args) {
        super(MessageType.METHOD_CALL, args);
        this.headers.put(HeaderField.SOURCE, source);
        this.headers.put(HeaderField.DEST, dest);
        this.headers.put(HeaderField.ACTION, action);
    }
}
