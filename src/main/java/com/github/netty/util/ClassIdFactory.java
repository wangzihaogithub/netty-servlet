package com.github.netty.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassIdFactory {

    private static ClassIdFactory factory = new ClassIdFactory();

    private Map<Object,AtomicInteger> idIncrMap;
    private Map<Object,Integer> idMap;

    private ClassIdFactory(){}

    public static String newIdName(Object obj,String name){
        return name+"@"+ newId(obj);
    }

    public static String getIdName(Object obj,String name){
        return name+"@"+ getId(obj);
    }

    public static String getIdNameClass(Object obj,String name){
        return name+"@"+ getIdClass(obj);
    }

    private static int newId(Object obj){
        AtomicInteger atomicInteger = getIdIncrMap().get(obj);
        if (atomicInteger == null) {
            atomicInteger = new AtomicInteger(0);
            getIdIncrMap().put(obj, atomicInteger);
        }
        return atomicInteger.incrementAndGet();
    }

    public synchronized static int getId(Object obj){
        Integer id = getIdMap().get(obj);
        if(id != null){
            return id;
        }
        id = newId(obj);
        getIdMap().put(obj,id);
        return id;
    }

    public synchronized static int getIdClass(Object obj){
        Integer id = getIdMap().get(obj);
        if(id != null){
            return id;
        }
        id = newId(obj.getClass());
        getIdMap().put(obj,id);
        return id;
    }

    private static Map<Object, Integer> getIdMap() {
        ClassIdFactory factory = getFactory();
        if(factory.idMap == null){
            synchronized (ClassIdFactory.class) {
                if(factory.idMap == null) {
                    factory.idMap = new ConcurrentHashMap<>(16);
                }
            }
        }
        return factory.idMap;
    }

    private static Map<Object, AtomicInteger> getIdIncrMap() {
        ClassIdFactory factory = getFactory();
        if(factory.idIncrMap == null){
            synchronized (ClassIdFactory.class) {
                if(factory.idIncrMap == null) {
                    factory.idIncrMap = new ConcurrentHashMap<>(16);
                }
            }
        }
        return factory.idIncrMap;
    }

    private static ClassIdFactory getFactory() {
        if(factory == null){
            synchronized (ClassIdFactory.class) {
                if(factory == null) {
                    factory = new ClassIdFactory();
                }
            }
        }
        return factory;
    }

}
