package com.bilibili.xbus.message;

/**
 * Error
 *
 * @author chengyuan
 * @data 16/8/9.
 */
public class Error extends Message {

    public interface ErrorCode {
        int E_READ_MSG = 1;
        int E_INVALID_MSG_TYPE = 1001;
        int E_INVALID_MSG_SOURCE = 1002;
        int E_INVALID_MSG_ACTION = 1003;
        int E_INVALID_MSG_ARGS = 1004;
    }

    public Error(String source, String dest, int errorCode, long replySerial, Object... args) {
        super(MessageType.ERROR, args);
        if (source == null || dest == null) {
            throw new IllegalArgumentException("Must set source, dest to Error");
        }

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
