/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus.message;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Message
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public abstract class Message implements Serializable {

    private static volatile long globalSerial = 0L;

    public interface MessageType {
        byte METHOD_CALL = 1;
        byte METHOD_RETURN = 2;
        byte ERROR = 3;
    }

    public interface HeaderField {
        byte SOURCE = 1;
        byte DEST = 2;
        byte INTERFACE = 3;
        byte ACTION = 4;
        byte ERROR_CODE = 5;
        byte REPLY_SERIAL = 6;
    }

    protected final long serial;
    protected final byte type;
    protected HashMap<Byte, Object> headers;
    protected Object[] args;

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

    public String getInterface() {
        return (String) headers.get(HeaderField.INTERFACE);
    }

    public String getAction() {
        return (String) headers.get(HeaderField.ACTION);
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
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
