package com.bilibili.xbus;

import android.content.Context;

import com.bilibili.xbus.message.ErrorCode;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodCall;
import com.bilibili.xbus.message.MethodReturn;
import com.bilibili.xbus.utils.XBusUtils;

import java.io.IOException;

/**
 * @author kaede
 * @version date 16/8/19
 */
public class XBusHandshakeImpl implements XBusHandshake {

    private static final byte STATE_HANDSHAKE_INIT = 0;
    private static final byte STATE_HANDSHAKE_WAIT = STATE_HANDSHAKE_INIT + 1;
    private static final byte STATE_HANDSHAKE_OK = STATE_HANDSHAKE_WAIT + 1;

    private static final String METHOD_REQUEST_NAME = "requestName";
    private static final String METHOD_ACCEPT = "accept";

    static final String PATH_UNKNOWN = "unknown";

    private static volatile XBusHandshakeImpl instance;

    public static XBusHandshakeImpl instance(Context context) {
        if (instance == null) {
            instance = new XBusHandshakeImpl(context);
        }
        return instance;
    }

    private Context mContext;

    private XBusHandshakeImpl(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void handshakeWithHost(String clientPath, MessageReader in, MessageWriter out) throws IOException {
        Message msg;
        byte state = STATE_HANDSHAKE_INIT;

        while (state != STATE_HANDSHAKE_OK) {
            switch (state) {
                case STATE_HANDSHAKE_INIT:
                    msg = in.read();
                    if (msg == null) {
                        out.write(new MethodReturn(clientPath, XBusUtils.getHostPath(mContext), -1, ErrorCode.E_READ_MSG));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (!XBusUtils.getHostPath(mContext).equals(msg.getSource())) {
                        out.write(new MethodReturn(clientPath, XBusUtils.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_SOURCE));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (msg.getType() != Message.MessageType.METHOD_CALL) {
                        out.write(new MethodReturn(clientPath, XBusUtils.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_TYPE));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (!METHOD_REQUEST_NAME.equals(msg.getAction())) {
                        out.write(new MethodReturn(clientPath, XBusUtils.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_ACTION));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    out.write(new MethodReturn(clientPath, XBusUtils.getHostPath(mContext), msg.getSerial()).setReturnValue(clientPath));
                    state = STATE_HANDSHAKE_WAIT;
                    break;
                case STATE_HANDSHAKE_WAIT:
                    msg = in.read();
                    if (msg == null) {
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (!XBusUtils.getHostPath(mContext).equals(msg.getSource())) {
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (msg.getType() != Message.MessageType.METHOD_CALL) {
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (!METHOD_ACCEPT.equals(msg.getAction())) {
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    state = STATE_HANDSHAKE_OK;
                    break;
            }
        }
    }

    @Override
    public String handshakeWithClient(String hostPath, MessageReader in, MessageWriter out) throws IOException {
        Message msg;
        String remotePath = null;
        byte state = STATE_HANDSHAKE_INIT;
        while (state != STATE_HANDSHAKE_OK) {
            switch (state) {
                case STATE_HANDSHAKE_INIT:
                    out.write(new MethodCall(hostPath, PATH_UNKNOWN, METHOD_REQUEST_NAME));
                    state = STATE_HANDSHAKE_WAIT;
                    break;
                case STATE_HANDSHAKE_WAIT:
                    msg = in.read();
                    if (msg == null) {
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (msg.getType() != Message.MessageType.METHOD_RETURN) {
                        out.write(new MethodReturn(hostPath, PATH_UNKNOWN, msg.getSerial(), ErrorCode.E_INVALID_MSG_TYPE));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    MethodReturn methodReturn = (MethodReturn) msg;
                    if (methodReturn.getErrorCode() != ErrorCode.SUCCESS) {
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    Object[] args = methodReturn.getArgs();
                    if (args == null || args.length == 0) {
                        out.write(new MethodReturn(hostPath, PATH_UNKNOWN, methodReturn.getSerial(), ErrorCode.E_INVALID_MSG_ARGS));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    remotePath = (String) args[0];
                    out.write(new MethodCall(hostPath, remotePath, METHOD_ACCEPT));
                    state = STATE_HANDSHAKE_OK;
                    break;
            }
        }
        return remotePath;
    }
}
