package com.bilibili.xbus.message;

/**
 * MethodReturn
 *
 * @author chengyuan
 * @data 16/8/8.
 */
public class MethodReturn extends Message{


    public MethodReturn(String source, String dest, String action, Object... args) {
        super(MessageType.METHOD_RETURN, args);
        this.headers.put(HeaderField.SOURCE, source);
        this.headers.put(HeaderField.DEST, dest);
        this.headers.put(HeaderField.MEMBER, action);
    }
}
