package com.github.netty.servlet;

import com.github.netty.core.constants.HttpConstants;
import com.github.netty.util.ObjectUtil;
import com.github.netty.util.ServletUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by acer01 on 2018/7/15/015.
 */
public class ServletHttpServletRequest implements javax.servlet.http.HttpServletRequest {

    public static final String DISPATCHER_TYPE = ServletRequestDispatcher.class.getName().concat(".DISPATCHER_TYPE");

    private String servletPath;
    private String queryString;
    private String pathInfo;
    private String requestUri;
    private String characterEncoding;
    private String sessionId;

    private transient boolean parsePathsFlag;
    private transient boolean decodeCookieFlag;
    private boolean decodeParameterByUrlFlag;
    private boolean decodeParameterByBodyFlag;
    private boolean usingReaderFlag;
    private boolean asyncSupportedFlag;

    private Map<String,Object> attributeMap;
    private Map<String,String[]> parameterMap;
    private Cookie[] cookies;
    private Locale locale;

    private int sessionIdSource;
    private ServletHttpSession httpSession;
    private ServletInputStream inputStream;
    private ServletContext servletContext;
    private ServletAsyncContext asyncContext;

    private final HttpRequest request;
    private final HttpHeaders headers;

    public ServletHttpServletRequest(ServletInputStream inputStream, ServletContext servletContext, HttpRequest request) {
        this.request = request;
        this.headers = request.headers();
        this.attributeMap = new ConcurrentHashMap<>();
        this.inputStream = inputStream;
        this.servletContext = servletContext;
        this.asyncSupportedFlag = true;
        this.decodeParameterByUrlFlag = false;
        this.decodeParameterByBodyFlag = false;
        this.decodeCookieFlag = false;
        this.parsePathsFlag = false;
        this.usingReaderFlag = false;
    }

    private Map<String, Object> getAttributeMap() {
        if(attributeMap == null){
            attributeMap = new ConcurrentHashMap<>(16);
        }
        return attributeMap;
    }

    private boolean isDecodeParameter(){
        return decodeParameterByBodyFlag || decodeParameterByUrlFlag;
    }

    private void decodeCharacterEncoding() {
        String characterEncoding = ServletUtil.decodeCharacterEncoding(getContentType());
        if (characterEncoding == null) {
            characterEncoding = servletContext.getDefaultCharset().name();
        }
       this.characterEncoding = characterEncoding;
    }

    private void decodeParameter(){
        Map<String,String[]> parameterMap = new HashMap<>(16);
        if(HttpConstants.GET.equalsIgnoreCase(getMethod())){
            ServletUtil.decodeByUrl(parameterMap,request.uri());
            this.decodeParameterByUrlFlag = true;
        }else {
//            ServletUtil.decodeByBody(parameterMap,request);
//            this.decodeParameterByBodyFlag = true;
        }
        this.parameterMap = parameterMap;
    }

    private void decodeCookie(){
        CharSequence value = getHeader(HttpHeaderNames.COOKIE.toString());
        if (value == null) {
            return;
        }
        this.cookies = ServletUtil.decodeCookie(value.toString());
        this.decodeCookieFlag = true;
    }

    private void checkAndParsePaths(){
        if(parsePathsFlag){
            return;
        }

        String servletPath = request.uri().replace(servletContext.getContextPath(), "");
        if (!servletPath.startsWith("/")) {
            servletPath = "/" + servletPath;
        }
        int queryInx = servletPath.indexOf('?');
        if (queryInx > -1) {
            this.queryString = servletPath.substring(queryInx + 1, servletPath.length());
            servletPath = servletPath.substring(0, queryInx);
        }
        this.servletPath = servletPath;
        this.requestUri = this.servletContext.getContextPath() + servletPath; //TODO 加上pathInfo
        this.pathInfo = null;

        parsePathsFlag = true;
    }

    @Override
    public Cookie[] getCookies() {
        if(decodeCookieFlag){
            return cookies;
        }

        decodeCookie();
        return cookies;
    }

    /**
     * servlet标准 :
     *
     * 返回指定请求头的值
     *作为long值，代表a
     * <代码> < /代码>日期对象。使用这种方法
     *包含日期的标头，例如
     * <代码> if - modified - since > < /代码。
     返回日期为
     从1970年1月1日开始的毫秒数。
     头名不区分大小写。
     ，如果请求没有页眉
     *指定名称，此方法返回-1。如果消息头
     不能转换为日期，方法抛出。
     *一个<代码> IllegalArgumentException > < /代码。
     * @param name ，指定标题的名称
     * @return 表示指定的日期 在表示为毫秒数自1970年1月1日起，或-1，如果指定标题。未包括在请求
     */
    @Override
    public long getDateHeader(String name) throws IllegalArgumentException {
        String value = getHeader(name);
        if(ObjectUtil.isEmpty(value)){
            return -1;
        }

        Long timestamp = ServletUtil.parseHeaderDate(value,ServletUtil.FORMATS_TEMPLATE);
        if(timestamp == null){
            throw new IllegalArgumentException(value);
        }
        return timestamp;
    }

    @Override
    public String getHeader(String name) {
        return headers.getAndConvert(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<CharSequence> nameSet = headers.names();
        List<String> nameList = new LinkedList<>();
        for(CharSequence name : nameSet){
            nameList.add(name.toString());
        }
        return Collections.enumeration(nameList);
    }



    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(request.uri());
    }

    //TODO ServletPath和PathInfo应该是互补的，根据URL-Pattern匹配的路径不同而不同
    // 现在把PathInfo恒为null，ServletPath恒为uri-contextPath
    // 可以满足SpringBoot的需求，但不满足ServletPath和PathInfo的语义
    // 需要在RequestUrlPatternMapper匹配的时候设置,new NettyRequestDispatcher的时候传入MapperData
    @Override
    public String getPathInfo() {
        checkAndParsePaths();
        return this.pathInfo;
    }

    @Override
    public String getQueryString() {
        checkAndParsePaths();
        return this.queryString;
    }

    @Override
    public String getRequestURI() {
        checkAndParsePaths();
        return this.requestUri;
    }

    @Override
    public String getServletPath() {
        checkAndParsePaths();
        return this.servletPath;
    }


    @Override
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(this.headers.getAllAndConvert(name));
    }


    /**
     * servlet标准:
     *
     * 返回指定请求头的值
     *作为<代码> int > < /代码。如果请求没有标题
     *指定的名称，此方法返回-1。如果
     该方法不能将header转换为整数
     *抛出一个<代码> NumberFormatException > < /代码。
     头名不区分大小写。
     * @param name String指定请求头的名称
     * @exception NumberFormatException 如果标题值不能转换一个int。
     * @return 一个表示值的整数 请求头或-1 如果请求没有此名称的页眉返回-1
     */
    @Override
    public int getIntHeader(String name) {
        String headerStringValue = getHeader(name);
        if (headerStringValue == null) {
            return -1;
        }
        return Integer.parseInt(headerStringValue);
    }
    /*====== Header 相关方法 结束 ======*/


    @Override
    public String getMethod() {
        return request.method().name().toString();
    }


    @Override
    public String getContextPath() {
        return servletContext.getContextPath();
    }

    @Override
    public ServletHttpSession getSession(boolean create) {
        if(httpSession != null && httpSession.isValid()){
            return httpSession;
        }

        String id = getRequestedSessionId();
        Map<String,ServletHttpSession> sessionMap = servletContext.getHttpSessionMap();
        ServletHttpSession session = sessionMap.get(id);
        if(session == null){
            if(create) {
                session = new ServletHttpSession(id, servletContext,servletContext.getSessionCookieConfig());
                if(isRequestedSessionIdValid()) {
                    sessionMap.put(id, session);
                }
                session.access();
            }
        }else {
            session.access().setNewSessionFlag(false);
        }

        this.httpSession = session;
        return session;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        return getRequestedSessionId();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        getRequestedSessionId();
        return sessionIdSource == HttpConstants.SESSION_ID_SOURCE_COOKIE ||
                sessionIdSource == HttpConstants.SESSION_ID_SOURCE_URL;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        getRequestedSessionId();
        return sessionIdSource == HttpConstants.SESSION_ID_SOURCE_COOKIE;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return isRequestedSessionIdFromUrl();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        getRequestedSessionId();
        return sessionIdSource == HttpConstants.SESSION_ID_SOURCE_URL;
    }

    @Override
    public String getRequestedSessionId() {
        if(ObjectUtil.isNotEmpty(sessionId)){
            return sessionId;
        }

        String sessionId = ServletUtil.getCookieValue(getCookies(),HttpConstants.JSESSION_ID_COOKIE);
        if(ObjectUtil.isEmpty(sessionId)){
            sessionId = getParameter(HttpConstants.JSESSION_ID_PARAMS);
            if(ObjectUtil.isEmpty(sessionId)){
                sessionIdSource = HttpConstants.SESSION_ID_SOURCE_NOT_FOUND_CREATE;
                sessionId = UUID.randomUUID().toString().replace("-","");
            }else {
                sessionIdSource = HttpConstants.SESSION_ID_SOURCE_URL;
            }
        }else {
            sessionIdSource = HttpConstants.SESSION_ID_SOURCE_COOKIE;
        }
        this.sessionId = sessionId;
        return sessionId;
    }

    @Override
    public boolean authenticate(javax.servlet.http.HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return getAttributeMap().get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(getAttributeMap().keySet());
    }

    @Override
    public String getCharacterEncoding() {
        if (characterEncoding == null) {
            decodeCharacterEncoding();
        }
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        characterEncoding = env;
    }

    @Override
    public int getContentLength() {
        return (int) getContentLengthLong();
    }

    @Override
    public long getContentLengthLong() {
        return inputStream.getCurrentLength();
    }

    @Override
    public String getContentType() {
        return getHeader(HttpHeaderNames.CONTENT_TYPE.toString());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if(usingReaderFlag){
            throw new IllegalStateException("stream not double using");
        }

        usingReaderFlag = true;
        return inputStream;
    }

    @Override
    public String getParameter(String name) {
        String[] values = getParameterMap().get(name);
        if(values == null || values.length == 0){
            return null;
        }
        return values[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        Collection<String[]> collection = getParameterMap().values();
        List<String> list = new LinkedList<>();
        for(String[] arr : collection){
            Collections.addAll(list, arr);
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if(!isDecodeParameter()) {
            decodeParameter();
        }
        return Collections.unmodifiableMap(parameterMap);
    }

    @Override
    public String getProtocol() {
        return request.protocolVersion().toString();
    }

    @Override
    public String getScheme() {
        return request.protocolVersion().protocolName().toString();
    }

    @Override
    public String getServerName() {
        return servletContext.getServerSocketAddress().getHostName();
    }

    @Override
    public int getServerPort() {
        return servletContext.getServerSocketAddress().getPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(),getCharacterEncoding()));
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress inetSocketAddress = getRemoteAddress();
        if(inetSocketAddress == null){
            return null;
        }
        InetAddress inetAddress = inetSocketAddress.getAddress();
        if(inetAddress == null){
            return null;
        }
        return inetAddress.getHostAddress();
    }

    private InetSocketAddress getRemoteAddress(){
        SocketAddress socketAddress = inputStream.getChannel().remoteAddress();
        if(socketAddress == null){
            return null;
        }
        if(socketAddress instanceof InetSocketAddress){
            return (InetSocketAddress) socketAddress;
        }
        return null;
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress inetSocketAddress = getRemoteAddress();
        if(inetSocketAddress == null){
            return null;
        }
        return inetSocketAddress.getHostName();
    }

    @Override
    public int getRemotePort() {
        InetSocketAddress inetSocketAddress = getRemoteAddress();
        if(inetSocketAddress == null){
            return 0;
        }
        return inetSocketAddress.getPort();
    }

    @Override
    public void setAttribute(String name, Object o) {
        getAttributeMap().put(name,o);
    }

    @Override
    public void removeAttribute(String name) {
        getAttributeMap().remove(name);
    }

    @Override
    public Locale getLocale() {
        if(locale != null){
            return locale;
        }

        Locale locale;
        String value = getHeader(HttpHeaderNames.ACCEPT_LANGUAGE.toString());
        if(value == null){
            locale = Locale.getDefault();
        }else {
            String[] values = value.split(HttpConstants.SP);
            String localeStr = values[0];
            locale = new Locale(localeStr);
        }

        this.locale = locale;
        return locale;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(Collections.singletonList(getLocale()));
    }

    @Override
    public boolean isSecure() {
        return HttpConstants.HTTPS.equalsIgnoreCase(getScheme());
    }

    @Override
    public ServletRequestDispatcher getRequestDispatcher(String path) {
        return servletContext.getRequestDispatcher(path);
    }

    @Override
    public String getRealPath(String path) {
        return path;
    }

    @Override
    public String getLocalName() {
        return servletContext.getServerSocketAddress().getHostName();
    }

    @Override
    public String getLocalAddr() {
        return servletContext.getServerSocketAddress().getAddress().getHostAddress();
    }

    @Override
    public int getLocalPort() {
        return servletContext.getServerSocketAddress().getPort();
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public ServletAsyncContext startAsync() throws IllegalStateException {
        return startAsync(this,null);
    }

    @Override
    public ServletAsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        ServletAsyncContext asyncContext = new ServletAsyncContext(servletContext,servletContext.getAsyncExecutorService(),servletRequest,servletResponse);
        asyncContext.setTimeout(servletContext.getAsyncTimeout());
        this.asyncContext = asyncContext;
        return asyncContext;
    }

    @Override
    public boolean isAsyncStarted() {
        return asyncContext != null && asyncContext.isStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        return asyncSupportedFlag;
    }

    @Override
    public ServletAsyncContext getAsyncContext() {
        return asyncContext;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return (DispatcherType) getAttributeMap().getOrDefault(DISPATCHER_TYPE,DispatcherType.REQUEST);
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

}
