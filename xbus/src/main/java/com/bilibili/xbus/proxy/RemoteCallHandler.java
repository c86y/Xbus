package com.bilibili.xbus.proxy;

import com.bilibili.xbus.CallHandler;
import com.bilibili.xbus.Connection;
import com.bilibili.xbus.XBusException;
import com.bilibili.xbus.message.ErrorCode;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodCall;
import com.bilibili.xbus.message.MethodReturn;
import com.bilibili.xbus.utils.StopWatch;
import com.bilibili.xbus.utils.XBusLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RemoteCallHandler
 *
 * @author chengyuan
 */
public class RemoteCallHandler implements CallHandler {

    private String mDest;
    private Connection mConn;
    private final Map<Long, MethodReturn> mMethodReturns = new HashMap<>();
    private final ConcurrentHashMap<RemoteObject, Object> mRemoteObjectProxyMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RemoteObject, Object> mRemoteObjectImplMap = new ConcurrentHashMap<>();

    public RemoteCallHandler(String dest) {
        mDest = dest;
    }

    public String getDest() {
        return mDest;
    }

    @Override
    public void onConnected(Connection conn) {
        mConn = conn;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        if (msg instanceof MethodCall) {
            // receive invoke request from remote
            StopWatch stopWatch = msg.getStopWatch().split("remote invoke");
            MethodReturn methodReturn;
            methodReturn = delegateCall((MethodCall) msg);
            methodReturn.setStopWatch(stopWatch.split("invoke end"));
            mConn.send(methodReturn);
        } else if (msg instanceof MethodReturn) {
            // receive invoke result from remote
            msg.getStopWatch().split("receive call result");
            synchronized (mMethodReturns) {
                msg.getStopWatch().split("syn in-list");
                mMethodReturns.put(msg.getSerial(), (MethodReturn) msg);
                mMethodReturns.notifyAll();
            }
        }
    }

    @Override
    public void onDisconnected() {
        mConn = null;
    }

    public void registerObject(Class cInterface, Object impl) {
        RemoteObject remoteObject = new RemoteObject(cInterface.getName());
        mRemoteObjectImplMap.put(remoteObject, impl);
    }

    final synchronized Object remoteInvoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        Connection conn = mConn;
        if (conn == null) {
            throw new XBusException("Connection is disconnected");
        }

        RemoteObject remoteObject = getRemoteObjectFromProxy(proxy);
        if (remoteObject == null) {
            remoteObject = new RemoteObject(proxy.getClass().getInterfaces()[0].getName());
        }

        String action = method.getName();
        MethodCall methodCall = new MethodCall(conn.getPath(), mDest, action, remoteObject, args);
        long id = methodCall.getSerial();

        // TODO: 16/8/18  deal with stop watch for better performance
        StopWatch stopWatch = new StopWatch().start(method.getName() + " " + id).split("create call");
        methodCall.setStopWatch(stopWatch);

        conn.send(methodCall);

        MethodReturn methodReturn = null;
        do {
            synchronized (mMethodReturns) {
                methodReturn = mMethodReturns.get(id);
                if (methodReturn != null) {
                    stopWatch = methodReturn.getStopWatch();
                    stopWatch.split("call return");
                    mMethodReturns.remove(id);
                } else {
                    try {
                        stopWatch.split("send wait");
                        mMethodReturns.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        } while (methodReturn == null && mConn != null);

        if (methodReturn.getErrorCode() != ErrorCode.SUCCESS) {
            throw new XBusException(methodReturn.getErrorCode(), methodReturn.getErrorMsg(), (Throwable) methodReturn.getReturnValue());
        }

        XBusLog.d("invoke consumed : " + stopWatch.end("call end"));
        return methodReturn.getReturnValue();
    }

    private MethodReturn delegateCall(MethodCall methodCall) {
        MethodReturn methodReturn;
        RemoteObject remoteObject = methodCall.getRemoteObject();
        Object impl = mRemoteObjectImplMap.get(remoteObject);
        if (impl == null) {
            methodReturn = new MethodReturn(methodCall.getDest(), methodCall.getSource(), methodCall.getSerial(), ErrorCode.E_CLASS_NOT_FOUND,
                    String.format("Class %s doesn't have implementation", remoteObject.getClassName()));
            return methodReturn;
        }

        Method methodImpl = null;
        Method[] methods = impl.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodCall.getAction())) {
                methodImpl = method;
                break;
            }
        }

        if (methodImpl == null) {
            methodReturn = new MethodReturn(methodCall.getDest(), methodCall.getSource(), methodCall.getSerial(), ErrorCode.E_NO_SUCH_METHOD,
                    String.format("There's no method named %s ", methodCall.getAction()));
            return methodReturn;
        }

        Object returnObject;
        try {
            methodImpl.setAccessible(true);
            returnObject = methodImpl.invoke(impl, methodCall.getArgs());
            methodReturn = new MethodReturn(methodCall.getDest(), methodCall.getSource(), methodCall.getSerial()).setReturnValue(returnObject);
        } catch (IllegalAccessException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }
            methodReturn = new MethodReturn(methodCall.getDest(), methodCall.getSource(), methodCall.getSerial(), ErrorCode.E_ILLEGAL_ACCESS).setReturnValue(e);
        } catch (InvocationTargetException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }
            methodReturn = new MethodReturn(methodCall.getDest(), methodCall.getSource(), methodCall.getSerial(), ErrorCode.E_INVOKE_TARGET).setReturnValue(e);
        }
        return methodReturn;
    }

    private Object getProxyFromRemoteObject(RemoteObject remoteObject) {
        Object proxy = mRemoteObjectProxyMap.get(remoteObject);
        if (proxy == null) {
            try {
                proxy = RemoteInvocation.buildProxy(remoteObject, this);
            } catch (ClassNotFoundException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }
            }
            mRemoteObjectProxyMap.put(remoteObject, proxy);
        }
        return proxy;
    }

    private RemoteObject getRemoteObjectFromProxy(Object proxy) {
        Set<Map.Entry<RemoteObject, Object>> entries = mRemoteObjectProxyMap.entrySet();
        for (Map.Entry<RemoteObject, Object> entry : entries) {
            if (entry.getValue() == proxy) {
                return entry.getKey();
            }
        }

        return null;
    }
}
