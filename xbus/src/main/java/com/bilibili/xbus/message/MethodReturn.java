package com.bilibili.xbus.message;

import android.support.annotation.NonNull;

/**
 * MethodReturn
 *
 * @author chengyuan
 */
public class MethodReturn extends Message {

    public MethodReturn(@NonNull String source, @NonNull String dest, long replySerial) {
        this(source, dest, replySerial, ErrorCode.SUCCESS, null);
    }

    public MethodReturn(@NonNull String source, @NonNull String dest, long replySerial, int errorCode) {
        this(source, dest, replySerial, errorCode, null);
    }

    public MethodReturn(@NonNull String source, @NonNull String dest, long replySerial, int errorCode, String errorMsg) {
        super(MessageType.METHOD_RETURN);

        if (source == null || dest == null) {
            throw new IllegalArgumentException("Must set source, dest to MethodReturn");
        }

        this.headers.put(HeaderField.SOURCE, source);
        this.headers.put(HeaderField.DEST, dest);
        this.headers.put(HeaderField.REPLY_SERIAL, replySerial);
        this.headers.put(HeaderField.ERROR_CODE, errorCode);

        if (errorMsg != null) {
            this.headers.put(HeaderField.ERROR_MSG, errorMsg);
        }
    }

    public MethodReturn setCallBackAction(String action) {
        this.headers.put(HeaderField.ACTION_CALLBACK, action);
        return this;
    }

    public String getCallBackAction() {
        return (String) headers.get(HeaderField.ACTION_CALLBACK);
    }

    public MethodReturn setReturnValue(Object... args) {
        this.args = args == null ? null : args;
        return this;
    }

    public Object getReturnValue() {
        return args == null || args.length == 0 ? null : args.length == 1 ? args[0] : args;
    }

    public long getReplySerial() {
        return (long) headers.get(HeaderField.REPLY_SERIAL);
    }

    public int getErrorCode() {
        return (int) headers.get(HeaderField.ERROR_CODE);
    }

    public String getErrorMsg() {
        return (String) headers.get(HeaderField.ERROR_MSG);
    }
}
