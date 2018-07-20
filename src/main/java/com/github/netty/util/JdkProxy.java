package com.github.netty.util;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class JdkProxy implements InvocationHandler {

    private Object source;
    private boolean isEnableLog;
    private String name;

    public JdkProxy(Object source, String name, boolean isEnableLog) {
        this.source = source;
        this.isEnableLog = isEnableLog;
        this.name = name;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(isEnableLog){
            ProxyUtil.log(name,method,args);
        }
        try {
            if("toString".equals(method.getName())){
                return name;
            }
            Object result = method.invoke(source,args);
            return result;
        }catch (Throwable t){
            throw t;
        }
    }

    public Object getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "JdkProxy{" +
                "name='" + name + '\'' +
                '}';
    }
}