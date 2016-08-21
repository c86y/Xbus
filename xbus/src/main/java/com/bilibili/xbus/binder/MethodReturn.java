package com.bilibili.xbus.binder;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.bilibili.xbus.message.ErrorCode;

/**
 * Created by Kaede on 16/8/21.
 */
public class MethodReturn extends Message implements Parcelable {

    public MethodReturn(@NonNull String source, @NonNull String dest, long replySerial) {
        this(source, dest, replySerial, ErrorCode.SUCCESS, null);
    }

    public MethodReturn(@NonNull String source, @NonNull String dest, long replySerial, int errorCode) {
        this(source, dest, replySerial, errorCode, null);
    }

    public MethodReturn(@NonNull String source, @NonNull String dest, long replySerial, int errorCode, String errorMsg) {
        super(com.bilibili.xbus.message.Message.MessageType.METHOD_RETURN);

        if (source == null || dest == null) {
            throw new IllegalArgumentException("Must set source, dest to MethodReturn");
        }

        this.headers.put(com.bilibili.xbus.message.Message.HeaderField.SOURCE, source);
        this.headers.put(com.bilibili.xbus.message.Message.HeaderField.DEST, dest);
        this.headers.put(com.bilibili.xbus.message.Message.HeaderField.REPLY_SERIAL, replySerial);
        this.headers.put(com.bilibili.xbus.message.Message.HeaderField.ERROR_CODE, errorCode);

        if (errorMsg != null) {
            this.headers.put(com.bilibili.xbus.message.Message.HeaderField.ERROR_MSG, errorMsg);
        }
    }

    public MethodReturn setReturnValue(Object... args) {
        this.args = args == null ? null : args;
        return this;
    }

    public Object getReturnValue() {
        return args == null || args.length == 0 ? null : args.length == 1 ? args[0] : args;
    }

    public long getReplySerial() {
        return (long) headers.get(com.bilibili.xbus.message.Message.HeaderField.REPLY_SERIAL);
    }

    public int getErrorCode() {
        return (int) headers.get(com.bilibili.xbus.message.Message.HeaderField.ERROR_CODE);
    }

    public String getErrorMsg() {
        return (String) headers.get(com.bilibili.xbus.message.Message.HeaderField.ERROR_MSG);
    }

    protected MethodReturn(Parcel in) {
        super(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MethodReturn> CREATOR = new Creator<MethodReturn>() {
        @Override
        public MethodReturn createFromParcel(Parcel in) {
            return new MethodReturn(in);
        }

        @Override
        public MethodReturn[] newArray(int size) {
            return new MethodReturn[size];
        }
    };
}
