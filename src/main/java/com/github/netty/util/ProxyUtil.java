package com.github.netty.util;

import org.springframework.cglib.core.DebuggingClassWriter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author acer01
 * @date 2018/7/2/002
 */
@SuppressWarnings("unchecked")
public class ProxyUtil {

    private static boolean enableProxy = false;

    public static boolean isEnableProxy() {
        return enableProxy;
    }

    public static void setEnableProxy(boolean enableProxy) {
        ProxyUtil.enableProxy = enableProxy;
    }

    public static boolean canProxy(Class clazz) {
        int mod = clazz.getModifiers();
        return Modifier.isFinal(mod) || Modifier.isAbstract(mod);
    }

    //============================newInstance=================================

    private static <T>T newInstance(Class<T> sourceClass){
        try {
            return newInstance(sourceClass,new Class[]{},new Object[]{});
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static <T>T newInstance(Class<T> sourceClass, Class[]argTypes, Object[] args){
        try {
            Constructor<T> constructor = sourceClass.getDeclaredConstructor(argTypes);
            T source = constructor.newInstance(args);
            return source;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //============================CglibProxy=================================

    public static <T>T newProxyByCglib(Class<T> sourceClass, String logName, boolean isEnableLog){
        return newProxyByCglib(sourceClass,logName, isEnableLog,new Class[]{},new Object[]{});
    }

    public static <T>T newProxyByCglib(Class<T> sourceClass){
        String logName = NamespaceUtil.getIdNameClass(sourceClass,sourceClass.getSimpleName());
        return newProxyByCglib(sourceClass,logName, true,new Class[]{},new Object[]{});
    }

    public static <T>T newProxyByCglib(Class<T> sourceClass, String logName, boolean isEnableLog, Class[]argTypes, Object[] args){
        if(!isEnableProxy()){
            return newInstance(sourceClass,argTypes,args);
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(sourceClass);
        enhancer.setCallback(new CglibProxy(logName,isEnableLog));

        T proxy = (T) enhancer.create(argTypes,args);
        return proxy;
    }

    public static void setCglibDebugClassWriterPath(String path){
//        "D:\\cglib";
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY,path);
    }

    //============================JdkProxy=================================

    public static <T>T newProxyByJdk(Class<T> sourceClass, String logName,boolean isEnableLog){
        try {
            T source = newInstance(sourceClass);
            return newProxyByJdk(source,logName,isEnableLog);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static <T>T newProxyByJdk(T source){
        return newProxyByJdk(source,source.toString(),true);
    }

    public static <T>T newProxyByJdk(T source, String logName,boolean isEnableLog){
        if(!isEnableProxy()){
            return source;
        }
        return (T) Proxy.newProxyInstance(
                source.getClass().getClassLoader(),
                getInterfaces(source),
                new JdkProxy(source,logName,isEnableLog));
    }

    //============================JdkProxy=================================

    public static Object unWrapper(Object object){
        if(ProxyUtil.isProxyByJdk(object)){
            Proxy loopProxy = (Proxy) object;
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(loopProxy);
            if(invocationHandler instanceof JdkProxy){
                JdkProxy jdkProxy = (JdkProxy) invocationHandler;
                return jdkProxy.getSource();
            }
        }

        if(ProxyUtil.isProxyByCglib(object)){

        }
        return object;
    }


    //============================private=================================

    private static boolean isProxyByCglib(Object object){
        String className = object.getClass().getName();
        return (className != null && className.contains("$$"));
    }

    private static boolean isProxyByJdk(Object object){
        return Proxy.isProxyClass(object.getClass());
    }

    private static Class[] getInterfaces(Object source){
        List<Class> interfaceList = new ArrayList<>();
        Class sourceClass = source.getClass();
        for(Class currClass = sourceClass; currClass != null; currClass = currClass.getSuperclass()){
            interfaceList.addAll(Arrays.asList(currClass.getInterfaces()));
        }
        return interfaceList.toArray(new Class[interfaceList.size()]);
    }


    static void log(String proxyName, Method method,Object[] args){
        if(Arrays.asList("toString","hashCode","equals").contains(method.getName())){
            return;
        }
        System.out.println("--------"+ Thread.currentThread() + "----"+proxyName + " 方法:" + method.getName() +
                (args == null || args.length == 0? "":" 参数:"+Arrays.toString(args)));
    }

    public static class CglibProxy implements MethodInterceptor {

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


    public static class JdkProxy implements InvocationHandler {

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
}
