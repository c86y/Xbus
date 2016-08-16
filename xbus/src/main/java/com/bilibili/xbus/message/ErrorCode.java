package com.bilibili.xbus.message;

/**
 * ErrorCode
 *
 * @author chengyuan
 */
public interface ErrorCode {

    int SUCCESS = 0;

    int E_READ_MSG = 1001;
    int E_INVALID_MSG_TYPE = 1002;
    int E_INVALID_MSG_SOURCE = 1003;
    int E_INVALID_MSG_ACTION = 1004;
    int E_INVALID_MSG_ARGS = 1005;

    int E_ILLEGAL_ACCESS = 3001;
    int E_INVOKE_TARGET = 3002;
    int E_CLASS_NOT_FOUND = 3003;
    int E_NO_SUCH_METHOD = 3004;
}
