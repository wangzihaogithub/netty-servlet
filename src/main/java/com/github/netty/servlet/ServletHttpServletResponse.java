package com.github.netty.servlet;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.concurrent.FastThreadLocal;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by acer01 on 2018/7/15/015.
 */
public class ServletHttpServletResponse implements javax.servlet.http.HttpServletResponse {

    /**
     * SimpleDateFormat非线程安全，为了节省内存提高效率，把他放在ThreadLocal里
     * 用于设置HTTP响应头的时间信息
     */
    private static final FastThreadLocal<DateFormat> FORMAT = new FastThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            return df;
        }
    };

    private final ServletContext servletContext;
    private ServletHttpServletRequest httpServletRequest;
    private HttpResponse httpResponse;
    private ServletOutputStream outputStream;
    private boolean usingOutputStream;
    private PrintWriter writer;
    private boolean committed;
    private List<Cookie> cookies;
    private String contentType;
    private String characterEncoding = HttpConstants.DEFAULT_CHARSET.name();
    private Locale locale;

    /**
     * 构造方法
     * @param ctx            Netty的Context
     * @param servletContext ServletContext
     */
    public ServletHttpServletResponse(ChannelHandlerContext ctx, ServletContext servletContext,ServletHttpServletRequest httpServletRequest) {
        this.servletContext = servletContext;
        //Netty自带的http响应对象，初始化为200
        this.httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false);
        this.outputStream = new ServletOutputStream(ctx, this);
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * 设置基本的请求头
     */
    public HttpResponse getNettyResponse() {
        if (committed) {
            return httpResponse;
        }
        committed = true;
        HttpHeaders headers = httpResponse.headers();
        if (null != contentType) {
            String value = null == characterEncoding ? contentType : contentType + "; charset=" + characterEncoding; //Content Type 响应头的内容
            headers.set(HttpHeaderNames.CONTENT_TYPE, value);
        }
        CharSequence date = getFormattedDate();
        headers.set(HttpHeaderNames.DATE, date); // 时间日期响应头
        headers.set(HttpHeaderNames.SERVER, servletContext.getServerInfo()); //服务器信息响应头

        // cookies处理
//        long curTime = System.currentTimeMillis(); //用于根据maxAge计算Cookie的Expires
        //先处理Session ，如果是新Session需要通过Cookie写入
        if (httpServletRequest.getSession().isNew()) {
            String sessionCookieStr = com.github.netty.core.constants.HttpConstants.JSESSION_ID_COOKIE + "=" + httpServletRequest.getRequestedSessionId() + "; path=/; domain=" + httpServletRequest.getServerName();
            headers.add(HttpHeaderNames.SET_COOKIE, sessionCookieStr);
        }

        //其他业务或框架设置的cookie，逐条写入到响应头去
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                StringBuilder sb = new StringBuilder();
                sb.append(cookie.getName()).append("=").append(cookie.getValue())
                        .append("; max-Age=").append(cookie.getMaxAge());
                if (cookie.getPath() != null) {
                    sb.append("; path=").append(cookie.getPath());
                }
                if (cookie.getDomain() != null) {
                    sb.append("; domain=").append(cookie.getDomain());
                }
                headers.add(HttpHeaderNames.SET_COOKIE, sb.toString());
            }
        }
        return httpResponse;
    }

    /**
     * @return 线程安全的获取当前时间格式化后的字符串
     */
    private CharSequence getFormattedDate() {
        return new AsciiString(FORMAT.get().format(new Date()));
    }

    @Override
    public void addCookie(Cookie cookie) {
        if(cookies == null){
            cookies = new ArrayList<>();
        }
        cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return httpResponse.headers().contains(name);
    }

    @Override
    public String encodeURL(String url) {
        if(!httpServletRequest.isRequestedSessionIdFromCookie()){
            //来自Cookie的Session ID,则客户端肯定支持Cookie，无需重写URL
            return url;
        }
        return url + ";" + com.github.netty.core.constants.HttpConstants.JSESSION_ID_PARAMS + "=" + httpServletRequest.getRequestedSessionId();
    }

    @Override
    public String encodeRedirectURL(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        checkNotCommitted();
        httpResponse.setStatus(new HttpResponseStatus(sc, msg));
    }

    @Override
    public void sendError(int sc) throws IOException {
        checkNotCommitted();
        httpResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        checkNotCommitted();
        httpResponse.setStatus(HttpResponseStatus.FOUND);
        httpResponse.headers().set("Location", location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        httpResponse.headers().setLong(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        httpResponse.headers().addLong(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        if (setHeaderField(name, value)) {
            return;
        }
        httpResponse.headers().set(name, value);
    }

    private boolean setHeaderField(String name, String value) {
        char c = name.charAt(0);//减少判断的时间，提高效率
        if ('C' == c || 'c' == c) {
            if (HttpHeaderNames.CONTENT_TYPE.equalsIgnoreCase(name)) {
                setContentType(value);
                return true;
            }
        }
        return false;
    }

    @Override
    public void addHeader(String name, String value) {
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        if (setHeaderField(name, value)) {
            return;
        }
        httpResponse.headers().add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        httpResponse.headers().setInt(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        httpResponse.headers().add(name, String.valueOf(value));
    }

    @Override
    public void setContentType(String type) {
        if (isCommitted()) {
            return;
        }
        if (hasWriter()) {
            return;
        }
        if (null == type) {
            contentType = null;
            return;
        }
        MediaType mediaType = MediaType.parse(type);
        Optional<Charset> charset = mediaType.charset();
        if (charset.isPresent()) {
            setCharacterEncoding(charset.get().name());
        }
        contentType = mediaType.type() + '/' + mediaType.subtype();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setStatus(int sc) {
        httpResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm) {
        httpResponse.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public int getStatus() {
        return httpResponse.status().code();
    }

    @Override
    public String getHeader(String name) {
        return httpResponse.headers().getAndConvert(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return httpResponse.headers().getAllAndConvert(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        Set<CharSequence> nameSet = httpResponse.headers().names();

        List<String> nameList = new LinkedList<>();
        for(CharSequence charSequence : nameSet){
            nameList.add(charSequence.toString());
        }
        return nameList;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    //Writer和OutputStream不能同时使用
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        checkState(!hasWriter(), "getWriter has already been called for this response");
        usingOutputStream = true;
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        checkState(!usingOutputStream, "getOutputStream has already been called for this response");
        if (!hasWriter()) {
            writer = new PrintWriter(outputStream);
        }
        return writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        if (hasWriter()) {
            return;
        }
        characterEncoding = charset;
    }

    private boolean hasWriter() {
        return null != writer;
    }

    private void checkState(boolean expression, String errorMessage) {
        if(!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

    @Override
    public void setContentLength(int len) {
        HttpHeaderUtil.setContentLength(httpResponse, len);
    }

    @Override
    public void setContentLengthLong(long len) {
        HttpHeaderUtil.setContentLength(httpResponse, len);
    }

    @Override
    public void setBufferSize(int size) {
        checkNotCommitted();
        outputStream.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return outputStream.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        checkNotCommitted();
        outputStream.flush();
    }

    @Override
    public void resetBuffer() {
        checkNotCommitted();
        outputStream.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    private void checkNotCommitted() {
        checkState(!committed, "Cannot perform this operation after response has been committed");
    }

    @Override
    public void reset() {
        resetBuffer();
        usingOutputStream = false;
        writer = null;
    }

    @Override
    public void setLocale(Locale loc) {
        locale = loc;
    }

    @Override
    public Locale getLocale() {
        return null == locale ? Locale.getDefault() : locale;
    }
}
