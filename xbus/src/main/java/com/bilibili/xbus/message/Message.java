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
public abstract class Message implements Serializable{

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", headers=" + headers +
                ", args=" + Arrays.toString(args) +
                '}';
    }

    public interface MessageType {
        public static final byte METHOD_CALL = 1;
        public static final byte METHOD_RETURN = 2;
    }

    public interface HeaderField {
        public static final byte SOURCE = 1;
        public static final byte DEST = 2;
        public static final byte MEMBER = 3;
    }

    private final byte type;
    protected Map<Byte, Object> headers;
    private Object[] args;

    public Message(byte type, Object... args) {
        this(type, new HashMap<Byte, Object>(), args);
    }

    public Message(byte type, Map<Byte, Object> headers, Object... args) {
        this.type = type;
        this.headers = headers;
        this.args = args;
    }

    public byte getType() {
        return type;
    }

    public Object getHeader(byte key) {
        return headers.get(key);
    }

    public String getSource() {
        return (String)headers.get(HeaderField.SOURCE);
    }

    public String getDest() {
        return (String)headers.get(HeaderField.DEST);
    }

    public String getMember() {
        return (String) headers.get(HeaderField.MEMBER);
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

}
