package com.bilibili.xbus.message;

/**
 * Error
 *
 * @author chengyuan
 * @data 16/8/9.
 */
public class Error extends Message {

    public Error(String source, String dest, int errorCode, long replySerial, Object... args) {
        super(MessageType.ERROR, args);
        this.headers.put(HeaderField.SOURCE, source);
        this.headers.put(HeaderField.DEST, dest);
        this.headers.put(HeaderField.ERROR_CODE, errorCode);
        this.headers.put(HeaderField.REPLY_SERIAL, replySerial);
    }

    public int getErrorCode() {
        return (int) headers.get(HeaderField.ERROR_CODE);
    }

    public long getReplySerial() {
        return (long) headers.get(HeaderField.REPLY_SERIAL);
    }
}
