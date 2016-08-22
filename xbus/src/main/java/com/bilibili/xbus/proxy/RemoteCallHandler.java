package com.bilibili.xbus.proxy;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.bilibili.xbus.Connection;
import com.bilibili.xbus.XBusException;
import com.bilibili.xbus.annotation.CallBackAction;
import com.bilibili.xbus.message.ErrorCode;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodCall;
import com.bilibili.xbus.message.MethodReturn;
import com.bilibili.xbus.utils.StopWatch;
import com.bilibili.xbus.utils.XBusLog;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RemoteCallHandler
 *
 * @author chengyuan
 */
public class RemoteCallHandler extends BaseRemoteCall {

    private String mDest;
    private Connection mConn;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final Map<Long, MethodReturn> mMethodReturns = new HashMap<>();
    private final List<WeakReference<RemoteCallBack>> mRemoteCallBacks = new LinkedList<>();
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
    public void handleMessage(final Message msg) {
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

            MethodReturn methodReturn = ((MethodReturn) msg);
            String callBackAction = methodReturn.getCallBackAction();
            if (TextUtils.isEmpty(callBackAction)) {
                synchronized (mMethodReturns) {
                    msg.getStopWatch().split("syn in-list");
                    mMethodReturns.put(methodReturn.getSerial(), methodReturn);
                    mMethodReturns.notifyAll();
                }
                return;
            }
            remoteCallBack(methodReturn);
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

    public void registerRemoteCallBack(RemoteCallBack callBack) {
        if (callBack == null) return;
        WeakReference<RemoteCallBack> weak = new WeakReference<>(callBack);
        mRemoteCallBacks.add(weak);
    }

    private void remoteCallBack(final MethodReturn methodReturn) {
        for (int i = 0; i < mRemoteCallBacks.size(); i++) {
            WeakReference<RemoteCallBack> item = mRemoteCallBacks.get(i);
            final RemoteCallBack callBack = item.get();
            if (callBack != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.callBack(methodReturn.getCallBackAction(), methodReturn.getReturnValue());
                    }
                });
            }
        }
    }

    public final synchronized Object remoteInvoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        Connection conn = mConn;
        if (conn == null) {
            throw new XBusException("Connection is disconnected");
        }

        RemoteObject remoteObject = getRemoteObjectFromProxy(proxy);
        if (remoteObject == null) {
            // TODO: 16/8/22  better way to get target interface
            remoteObject = new RemoteObject(proxy.getClass().getInterfaces()[0].getName());
        }

        String action = method.getName();
        MethodCall methodCall = new MethodCall(conn.getPath(), mDest, action, remoteObject, args);
        long id = methodCall.getSerial();

        boolean needReturn = true;
        CallBackAction annotation = method.getAnnotation(CallBackAction.class);
        if (annotation != null) {
            String callBackAction = annotation.value();
            if (!TextUtils.isEmpty(callBackAction)) {
                needReturn = false;
                methodCall.setCallBackAction(callBackAction);
            }
        }
        // TODO: 16/8/18  deal with stop watch for better performance
        StopWatch stopWatch = new StopWatch().start(method.getName() + " " + id).split("create call");
        methodCall.setStopWatch(stopWatch);

        conn.send(methodCall);
        if (!needReturn) {
            return null;
        }
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

        XBusLog.i("invoke consumed : " + stopWatch.end("call end"));
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

        // TODO: 16/8/21 need better way to find target method
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
            methodReturn.setCallBackAction(methodCall.getCallBackAction());
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

    public interface RemoteCallBack {
        void callBack(String action, Object object);
    }
}
