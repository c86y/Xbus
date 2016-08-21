/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus.binder;

import android.os.Parcel;
import android.os.Parcelable;

import com.bilibili.xbus.proxy.RemoteObject;
import com.bilibili.xbus.utils.StopWatch;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public class Message implements Parcelable {

    private static volatile long globalSerial = 0L;

    protected final long serial;
    protected final byte type;
    protected HashMap<Byte, Object> headers;
    protected Object[] args;
    protected StopWatch stopWatch;

    public Message(byte type, Object... args) {
        this(type, new HashMap<Byte, Object>(), args);
    }

    public Message(byte type, HashMap<Byte, Object> headers, Object... args) {
        synchronized (Message.class) {
            serial = globalSerial++;
        }
        this.type = type;
        this.headers = headers;
        this.args = args;
    }

    protected Message(Parcel in) {
        serial = in.readLong();
        type = in.readByte();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(serial);
        dest.writeByte(type);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public long getSerial() {
        return serial;
    }

    public byte getType() {
        return type;
    }

    public Object getHeader(byte key) {
        return headers.get(key);
    }

    public String getSource() {
        return (String) headers.get(com.bilibili.xbus.message.Message.HeaderField.SOURCE);
    }

    public String getDest() {
        return (String) headers.get(com.bilibili.xbus.message.Message.HeaderField.DEST);
    }

    public RemoteObject getRemoteObject() {
        return (RemoteObject) headers.get(com.bilibili.xbus.message.Message.HeaderField.REMOTE_OBJECT);
    }

    public String getAction() {
        return (String) headers.get(com.bilibili.xbus.message.Message.HeaderField.ACTION);
    }

    public Object[] getArgs() {
        return args;
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }

    public Message setStopWatch(StopWatch stopWatch) {
        this.stopWatch = stopWatch;
        return this;
    }

    @Override
    public String toString() {
        return "Message{" +
                "serial=" + serial +
                ", type=" + type +
                ", headers=" + headers +
                ", args=" + Arrays.toString(args) +
                '}';
    }

}
