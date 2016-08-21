package com.bilibili.xbus.binder;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.bilibili.xbus.proxy.RemoteObject;

/**
 * Created by Kaede on 16/8/21.
 */
public class MethodCall extends Message implements Parcelable {

    public MethodCall(@NonNull String source, @NonNull String dest, @NonNull String action, Object... args) {
        this(source, dest, action, null, args);
    }

    public MethodCall(@NonNull String source, @NonNull String dest, @NonNull String action, @Nullable RemoteObject remoteObject, Object... args) {
        super(com.bilibili.xbus.message.Message.MessageType.METHOD_CALL, args);
        if (source == null || dest == null || action == null) {
            throw new IllegalArgumentException("Must set source, dest, action to MethodCall");
        }
        this.headers.put(com.bilibili.xbus.message.Message.HeaderField.SOURCE, source);
        this.headers.put(com.bilibili.xbus.message.Message.HeaderField.DEST, dest);
        this.headers.put(com.bilibili.xbus.message.Message.HeaderField.ACTION, action);

        if (remoteObject != null) {
            this.headers.put(com.bilibili.xbus.message.Message.HeaderField.REMOTE_OBJECT, remoteObject);
        }
    }

    protected MethodCall(Parcel in) {
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

    public static final Creator<MethodCall> CREATOR = new Creator<MethodCall>() {
        @Override
        public MethodCall createFromParcel(Parcel in) {
            return new MethodCall(in);
        }

        @Override
        public MethodCall[] newArray(int size) {
            return new MethodCall[size];
        }
    };
}
