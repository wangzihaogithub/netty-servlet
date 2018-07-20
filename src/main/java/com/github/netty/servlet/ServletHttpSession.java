package com.github.netty.servlet;

import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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


    ServletHttpSession(String id, ServletContext servletContext, SessionCookieConfig sessionCookieConfig) {
        this.id = id;
        this.servletContext = servletContext;
        this.attributeMap = new ConcurrentHashMap<>();
        this.creationTime = System.currentTimeMillis();
        this.newSessionFlag = true;
        this.maxInactiveInterval = servletContext.getInitParameter(ServletContext.SESSION_TIMEOUT,20 * 60);
        this.accessCount = new AtomicInteger(0);
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
        return attributeMap.get(name);
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributeMap.keySet());
    }

    @Override
    public String[] getValueNames() {
        return attributeMap.keySet().toArray(new String[attributeMap.size()]);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributeMap.put(name,value);
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name,value);
    }

    @Override
    public void removeAttribute(String name) {
        attributeMap.remove(name);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        servletContext.getHttpSessionMap().remove(id);
        attributeMap.clear();
        attributeMap = null;
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
