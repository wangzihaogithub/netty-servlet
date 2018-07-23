package com.github.netty.servlet;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.netty.util.ObjectUtil.EMPTY;

/**
 * Created by acer01 on 2018/7/15/015.
 */
public class ServletHttpSession implements HttpSession{

    private ServletContext servletContext;
    private String id;

    private Map<String,Object> attributeMap;
    private long creationTime;
    private long currAccessedTime;
    private long lastAccessedTime;
    //单位 秒
    private volatile int maxInactiveInterval;
    private volatile boolean newSessionFlag;
    private transient AtomicInteger accessCount;


    ServletHttpSession(String id, ServletContext servletContext, ServletSessionCookieConfig sessionCookieConfig) {
        this.id = id;
        this.servletContext = servletContext;
        this.attributeMap = null;
        this.creationTime = System.currentTimeMillis();
        this.newSessionFlag = true;
        this.maxInactiveInterval = sessionCookieConfig.getSessionTimeout();
        this.accessCount = new AtomicInteger(0);
    }

    private Map<String, Object> getAttributeMap() {
        if(attributeMap == null){
            attributeMap = new ConcurrentHashMap<>(16);
        }
        return attributeMap;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        Object value = getAttributeMap().get(name);

        if(value == EMPTY){
            return null;
        }
        return value;
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(getAttributeMap().keySet());
    }

    @Override
    public String[] getValueNames() {
        return getAttributeMap().keySet().toArray(new String[getAttributeMap().size()]);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if(value == null){
            value = EMPTY;
        }
        getAttributeMap().put(name,value);
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name,value);
    }

    @Override
    public void removeAttribute(String name) {
        getAttributeMap().remove(name);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        servletContext.getHttpSessionMap().remove(id);
        if(attributeMap != null) {
            attributeMap.clear();
            attributeMap = null;
        }
        servletContext = null;
    }

    @Override
    public boolean isNew() {
        return newSessionFlag;
    }

    public boolean isValid() {
        return System.currentTimeMillis() - creationTime < (maxInactiveInterval * 1000);
    }

    public void setNewSessionFlag(boolean newSessionFlag) {
        this.newSessionFlag = newSessionFlag;
    }

    public ServletHttpSession access(){
        lastAccessedTime = currAccessedTime = System.currentTimeMillis();
        accessCount.incrementAndGet();
        return this;
    }

}
