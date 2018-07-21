package com.github.netty.servlet;

import com.github.netty.util.MimeTypeUtil;
import com.github.netty.util.TodoOptimize;
import com.github.netty.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

/**
 * Created by acer01 on 2018/7/14/014.
 */
public class ServletContext implements javax.servlet.ServletContext {

    public static final String SESSION_TIMEOUT = ServletContext.class.getName().concat(".SESSION_TIMEOUT");

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String,ServletHttpSession> httpSessionMap = new ConcurrentHashMap<>();

    private Map<String,Object> attributeMap = new ConcurrentHashMap<>();
    private Map<String,String> initParamMap = new ConcurrentHashMap<>();

    private ExecutorService asyncExecutorService;

    private Map<String, com.github.netty.servlet.ServletRegistration> servletRegistrationMap = new ConcurrentHashMap<>();
    private Map<String,ServletFilterRegistration> filterRegistrationMap = new ConcurrentHashMap<>();

    @TodoOptimize("事件")
    private List<EventListener> eventListenerList = new CopyOnWriteArrayList<>();
    private Set<SessionTrackingMode> sessionTrackingModeSet = new CopyOnWriteArraySet<>();

    private ServletSessionCookieConfig sessionCookieConfig;
    private RequestUrlPatternMapper servletUrlPatternMapper;
    private String rootDirStr;
    private Charset charset;
    private InetSocketAddress serverSocketAddress;
    private final String serverInfo;
    private String contextPath;
    private volatile boolean initialized; //记录是否初始化完毕
    private final ClassLoader classLoader;

    public ServletContext(InetSocketAddress socketAddress,
                          ExecutorService asyncExecutorService,
                          ClassLoader classLoader,
                          String contextPath, String serverInfo,
                          ServletSessionCookieConfig sessionCookieConfig) {
//        File rootDir = new File("");
//        this.rootDirStr = rootDir.isAbsolute() ? rootDir.getAbsolutePath() : FilenameUtils.concat(new File(".").getAbsolutePath(), rootDir.getPath());
        this.initialized = false;
        this.sessionCookieConfig = sessionCookieConfig;
        this.contextPath = contextPath == null? "" : contextPath;
        this.charset = Charset.defaultCharset();
        this.serverSocketAddress = socketAddress;
        this.classLoader = classLoader;
        this.asyncExecutorService = asyncExecutorService;
        this.servletUrlPatternMapper = new RequestUrlPatternMapper(contextPath);
        this.serverInfo = serverInfo == null? "netty":serverInfo;

        //一分钟检查一次过期session
        new SessionInvalidThread(60 * 1000).start();
    }

    public void addServletMapping(String urlPattern, String name, Servlet servlet) throws ServletException {
        servletUrlPatternMapper.addServlet(urlPattern, servlet, name);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public ExecutorService getAsyncExecutorService() {
        return asyncExecutorService;
    }

    public long getAsyncTimeout(){
        String value = getInitParameter("asyncTimeout");
        if(value == null){
            return 10000;
        }
        try {
            return Long.parseLong(value);
        }catch (NumberFormatException e){
            return 10000;
        }
    }

    public InetSocketAddress getServerSocketAddress() {
        return serverSocketAddress;
    }

    public Map<String, ServletHttpSession> getHttpSessionMap() {
        return httpSessionMap;
    }

    public Charset getCharset() {
        return charset;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public javax.servlet.ServletContext getContext(String uripath) {
        return this;
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        return MimeTypeUtil.getMimeTypeByFileName(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        Set<String> thePaths = new HashSet<>();
        if (!path.endsWith("/")) {
            path += "/";
        }
        String basePath = getRealPath(path);
        if (basePath == null) {
            return thePaths;
        }
        File theBaseDir = new File(basePath);
        if (!theBaseDir.exists() || !theBaseDir.isDirectory()) {
            return thePaths;
        }
        String theFiles[] = theBaseDir.list();
        if (theFiles == null) {
            return thePaths;
        }
        for (String filename : theFiles) {
            File testFile = new File(basePath + File.separator + filename);
            if (testFile.isFile()) {
                thePaths.add(path + filename);
            } else if (testFile.isDirectory()) {
                thePaths.add(path + filename + "/");
            }
        }
        return thePaths;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        if (!path.startsWith("/")) {
            throw new MalformedURLException("Path '" + path + "' does not start with '/'");
        }
        URL url = new URL(getClassLoader().getResource(""), path.substring(1));
        try {
            url.openStream();
        } catch (Throwable t) {
            logger.warn("Throwing exception when getting InputStream of " + path + " in /");
            url = null;
        }
        return url;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return getClass().getResourceAsStream(path);
    }

    @Override
    public ServletRequestDispatcher getRequestDispatcher(String path) {
        String servletName = servletUrlPatternMapper.getServletNameByRequestURI(path);
        return getNamedDispatcher(servletName);
    }

    @Override
    public ServletRequestDispatcher getNamedDispatcher(String name) {
        Servlet servlet;
        try {
            servlet = null == name ? null : getServlet(name);
            if (servlet == null) {
                return null;
            }

            //TODO 过滤器的urlPatter解析
            List<Filter> allNeedFilters = new ArrayList<>();
            for (ServletFilterRegistration registration : filterRegistrationMap.values()) {
                allNeedFilters.add(registration.getFilter());
            }
            FilterChain filterChain = new ServletFilterChain(servlet, allNeedFilters);
            return new ServletRequestDispatcher(this, filterChain);
        } catch (ServletException e) {
            logger.error("Throwing exception when getting Filter from ServletFilterRegistration of name " + name, e);
            return null;
        }
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        com.github.netty.servlet.ServletRegistration registration = servletRegistrationMap.get(name);
        if(registration == null){
            return null;
        }
        return registration.getServlet();
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        List<Servlet> list = new ArrayList<>();
        for(com.github.netty.servlet.ServletRegistration registration : servletRegistrationMap.values()){
            list.add(registration.getServlet());
        }
        return Collections.enumeration(list);
    }

    @Override
    public Enumeration<String> getServletNames() {
        List<String> list = new ArrayList<>();
        for(com.github.netty.servlet.ServletRegistration registration : servletRegistrationMap.values()){
            list.add(registration.getName());
        }
        return Collections.enumeration(list);
    }

    @Override
    public void log(String msg) {
        logger.debug(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        logger.debug(msg,exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        logger.debug(message,throwable);
    }

    @Override
    public String getRealPath(String path) {
        return path;
    }

    @Override
    public String getServerInfo() {
        return serverInfo;
    }

    @Override
    public String getInitParameter(String name) {
        return initParamMap.get(name);
    }

    public <T>T getInitParameter(String name,T def) {
        String value = getInitParameter(name);
        if(value == null){
            return def;
        }
        Class<?> clazz = def.getClass();
        Object valCast = TypeUtil.cast((Object) value,clazz);
        if(valCast != null && valCast.getClass().isAssignableFrom(clazz)){
            return (T) valCast;
        }
        return def;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParamMap.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return initParamMap.putIfAbsent(name,value) == null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributeMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributeMap.keySet());
    }

    @Override
    public void setAttribute(String name, Object object) {
        attributeMap.put(name,object);
    }

    @Override
    public void removeAttribute(String name) {
        attributeMap.remove(name);
    }

    @Override
    public String getServletContextName() {
        return getClass().getName();
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, String className) {
        try {
            return addServlet(servletName, (Class<? extends Servlet>) Class.forName(className).newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        com.github.netty.servlet.ServletRegistration servletRegistration = new com.github.netty.servlet.ServletRegistration(servletName,servlet,this);
        servletRegistrationMap.put(servletName,servletRegistration);
        return servletRegistration;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        Servlet servlet = null;
        try {
            servlet = servletClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return addServlet(servletName,servlet);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration getServletRegistration(String servletName) {
        return servletRegistrationMap.get(servletName);
    }

    @Override
    public Map<String, com.github.netty.servlet.ServletRegistration> getServletRegistrations() {
        return servletRegistrationMap;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        try {
            return addFilter(filterName, (Class<? extends Filter>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        ServletFilterRegistration registration = new ServletFilterRegistration(filterName,filter);
        filterRegistrationMap.put(filterName,registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        try {
            return addFilter(filterName,filterClass.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return filterRegistrationMap.get(filterName);
    }

    @Override
    public Map<String, ServletFilterRegistration> getFilterRegistrations() {
        return filterRegistrationMap;
    }

    @Override
    public ServletSessionCookieConfig getSessionCookieConfig() {
        return sessionCookieConfig;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        sessionTrackingModeSet = sessionTrackingModes;
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return sessionTrackingModeSet;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return sessionTrackingModeSet;
    }

    @Override
    public void addListener(String className) {
        try {
            addListener((Class<? extends EventListener>) Class.forName(className));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        boolean match = false;
        if (t instanceof ServletContextAttributeListener ||
                t instanceof ServletRequestListener ||
                t instanceof ServletRequestAttributeListener ||
                t instanceof HttpSessionIdListener ||
                t instanceof HttpSessionAttributeListener) {
            eventListenerList.add(t);
            match = true;
        }

        if (t instanceof HttpSessionListener
                || (t instanceof ServletContextListener)) {
            // Add listener directly to the list of instances rather than to
            // the list of class names.
            eventListenerList.add(t);
            match = true;
        }

        if (match) {
            return;
        }

        if (t instanceof ServletContextListener) {
            throw new IllegalArgumentException(
                    "applicationContext.addListener.iae.sclNotAllowed"+
                    t.getClass().getName());
        } else {
            throw new IllegalArgumentException("applicationContext.addListener.iae.wrongType"+
                    t.getClass().getName());
        }
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        try {
            addListener(listenerClass.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void declareRoles(String... roleNames) {

    }

    @Override
    public String getVirtualServerName() {
        return serverSocketAddress.getHostString();
    }

    /**
     * 超时的Session无效化，定期执行
     */
    class SessionInvalidThread extends Thread {
        Logger logger = LoggerFactory.getLogger(getClass());

        private final long sessionLifeCheckInter;

        public SessionInvalidThread(long sessionLifeCheckInter) {
            this.sessionLifeCheckInter = sessionLifeCheckInter;
        }

        @Override
        public void run() {
            logger.info("Session Manager CheckInvalidSessionThread has been started...");
            while(true){
                try {
                    Thread.sleep(sessionLifeCheckInter);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(ServletHttpSession session : httpSessionMap.values()){
                    if(!session.isValid()){
                        logger.info("Session(ID={}) is invalidated by Session Manager", session.getId());
                        session.invalidate();
                    }
                }
            }
        }
    }
}
