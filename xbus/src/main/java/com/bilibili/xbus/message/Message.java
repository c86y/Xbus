/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus.message;

import com.bilibili.xbus.proxy.RemoteObject;
import com.bilibili.xbus.utils.StopWatch;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Message
 *
 * @author chengyuan
 */
public abstract class Message implements Serializable {

    private static volatile long globalSerial = 0L;

    public interface MessageType {
        byte METHOD_CALL = 1;
        byte METHOD_RETURN = 2;
    }

    public interface HeaderField {
        byte SOURCE = 1;
        byte DEST = 2;
        byte REMOTE_OBJECT = 3;
        byte ACTION = 4;
        byte REPLY_SERIAL = 5;
        byte ERROR_CODE = 6;
        byte ERROR_MSG = 7;
    }

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
        return (String) headers.get(HeaderField.SOURCE);
    }

    public String getDest() {
        return (String) headers.get(HeaderField.DEST);
    }

    public RemoteObject getRemoteObject() {
        return (RemoteObject) headers.get(HeaderField.REMOTE_OBJECT);
    }

    public String getAction() {
        return (String) headers.get(HeaderField.ACTION);
    }

    public Object[] getArgs() {
        return args;
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }

    public void setStopWatch(StopWatch stopWatch) {
        this.stopWatch = stopWatch;
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
