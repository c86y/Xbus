/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus.message;

import java.io.Serializable;
import java.util.Map;

/**
 * Message
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class Message implements Serializable{

    public interface MessageType {
        public static final byte MESSAGE_CALL = 1;
        public static final byte MESSAGE_RETURN = 2;
    }

    public interface HeaderField {
        public static final byte SOURCE = 1;
        public static final byte DEST = 2;
    }

    private byte type;
    private Map<Byte, Object> header;
    private Map<String, Object> args;

    public Message(byte type, Map<Byte, Object> headers, Map<String, Object> args) {
        this.type = type;
        this.header = headers;
        this.args = args;
    }

    public byte getType() {
        return type;
    }

    public Object getHeader(byte key) {
        return header.get(key);
    }

    public String getSource() {
        return (String)header.get(HeaderField.SOURCE);
    }

    public String getDest() {
        return (String)header.get(HeaderField.DEST);
    }

    public Object getArg(String key) {
        return args.get(key);
    }

}
