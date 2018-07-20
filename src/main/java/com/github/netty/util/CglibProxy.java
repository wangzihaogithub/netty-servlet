package com.github.netty.util;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class CglibProxy implements MethodInterceptor {

    private boolean isEnableLog;
    private String name;

    public CglibProxy(String name, boolean isEnableLog) {
        this.isEnableLog = isEnableLog;
        this.name = name;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if(isEnableLog){
            ProxyUtil.log(name,method,args);
        }
        try {
            if("toString".equals(method.getName())){
                return name;
            }
            Object result = methodProxy.invokeSuper(o,args);
            return result;
        }catch (Throwable t){
            throw t;
        }
    }

    @Override
    public String toString() {
        return "CglibProxy{" +
                "name='" + name + '\'' +
                '}';
    }
}
