package com.bilibili.xbus.message;

import android.support.annotation.NonNull;

/**
 * MethodReturn
 *
 * @author chengyuan
 * @data 16/8/8.
 */
public class MethodReturn extends Message {

    public MethodReturn(@NonNull String source, @NonNull String dest, long replySerial, Object... args) {
        super(MessageType.METHOD_RETURN, args);

        if (source == null || dest == null) {
            throw new IllegalArgumentException("Must set source, dest to MethodReturn");
        }

        this.headers.put(HeaderField.SOURCE, source);
        this.headers.put(HeaderField.DEST, dest);
        this.headers.put(HeaderField.REPLY_SERIAL, replySerial);
    }

    public long getReplySerial() {
        return (long) headers.get(HeaderField.REPLY_SERIAL);
    }
}
