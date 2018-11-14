package com.github.netty.servlet;

import com.github.netty.core.NettyHttpResponse;
import com.github.netty.core.constants.HttpConstants;
import com.github.netty.core.constants.HttpHeaderConstants;
import com.github.netty.core.util.AbstractRecycler;
import com.github.netty.core.util.CompositeByteBufX;
import com.github.netty.core.util.Recyclable;
import com.github.netty.core.util.HttpHeaderUtil;
import com.github.netty.core.util.RecyclableUtil;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.servlet.util.MediaType;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;

/**
 * servlet响应
 *
 * 频繁更改, 需要cpu对齐. 防止伪共享, 需设置 : -XX:-RestrictContended
 * @author acer01
 *  2018/7/15/015
 */
@sun.misc.Contended
public class ServletHttpServletResponse implements javax.servlet.http.HttpServletResponse,Recyclable {

    private static final AbstractRecycler<ServletHttpServletResponse> RECYCLER = new AbstractRecycler<ServletHttpServletResponse>() {
        @Override
        protected ServletHttpServletResponse newInstance() {
            return new ServletHttpServletResponse();
        }
    };

    private HttpServletObject httpServletObject;
    private NettyHttpResponse nettyResponse = new NettyHttpResponse();
    private PrintWriter writer;
    private List<Cookie> cookies;
    private String contentType;
    private String characterEncoding;
    private Locale locale;
    private boolean commitFlag = false;
    private final ServletOutputStreamWrapper outWrapper = new ServletOutputStreamWrapper(new CloseListener());

    protected ServletHttpServletResponse() {
    }

    public static ServletHttpServletResponse newInstance(HttpServletObject httpServletObject) {
        Objects.requireNonNull(httpServletObject);

        ServletHttpServletResponse instance = RECYCLER.getInstance();
        instance.httpServletObject = httpServletObject;
        return instance;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public HttpServletObject getHttpServletObject() {
        return httpServletObject;
    }

    public NettyHttpResponse getNettyResponse() {
        return nettyResponse;
    }

    /**
     * 检查是否提交
     * @throws IllegalStateException
     */
    private void checkCommitted() throws IllegalStateException {
        if(isCommitted()) {
            throw new IllegalStateException("Cannot perform this operation after response has been committed");
        }
    }

    /**
     * 检查头部特殊字段, 如果是特殊字段, 则进行处理
     * @param name 特殊字段
     * @param value 值
     * @return 是否进行过处理. true=处理过, false=没处理过
     */
    private boolean checkSpecialHeader(String name, String value) {
        if (HttpHeaderConstants.CONTENT_TYPE.toString().equalsIgnoreCase(name)) {
            setContentType(value);
            return true;
        }
        if(HttpHeaderConstants.CONTENT_LENGTH.toString().equalsIgnoreCase(name)) {
            try {
                long cL = Long.parseLong( value );
                setContentLengthLong(cL);
                return true;
            } catch( NumberFormatException ex ) {
                // Do nothing - the spec doesn't have any "throws"
                // and the user might know what he's doing
                return false;
            }
        }
        return false;
    }

    /**
     * 添加头部字段 (仅支持一个字段一个值)
     * @param name
     * @param value
     */
    private void setHeaderObject(String name, Object value){
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (isCommitted()) {
            return;
        }

        //减少判断的时间，提高效率
        char c = name.charAt(0);
        if ('C' == c || 'c' == c) {
            if (checkSpecialHeader(name, value.toString())) {
                return;
            }
        }
        getNettyHeaders().set((CharSequence)name, value);
    }

    /**
     * 添加头部字段, (支持一个字段多个值)
     * @param name
     * @param value
     */
    private void addHeaderObject(String name, Object value){
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        //减少判断的时间，提高效率
        char c = name.charAt(0);
        if ('C' == c || 'c' == c) {
            if (checkSpecialHeader(name, value.toString())) {
                return;
            }
        }

        getNettyHeaders().add((CharSequence) name, value);
    }

    private HttpHeaders getNettyHeaders(){
        HttpHeaders headers;
        if(nettyResponse.isTransferEncodingChunked()){
            headers = nettyResponse.getLastHttpContent().trailingHeaders();
        }else {
            headers = nettyResponse.headers();
        }
        return headers;
    }

    /**
     * 改变为分块传输流
     */
    public void changeToChunkStream() {
        //如果客户端不接受分块传输, 则不进行切换
        if(!HttpHeaderUtil.isAcceptTransferChunked(
                httpServletObject.getHttpServletRequest().getNettyHeaders())){
            return;
        }

        synchronized (outWrapper) {
            ServletOutputStream oldOut = outWrapper.unwrap();
            if(oldOut instanceof ServletOutputChunkedStream){
                return;
            }

            ServletOutputStream newOut = new ServletOutputChunkedStream();
            newOut.setHttpServletObject(httpServletObject);
            if (oldOut == null) {
                outWrapper.wrap(newOut);
                return;
            }

            try {
                CompositeByteBufX content = oldOut.lockBuffer();
                if (content != null) {
                    oldOut.setBuffer(null);
                    newOut.setBuffer(content);
                }
                newOut.setHttpServletObject(oldOut.getHttpServletObject());
                outWrapper.wrap(newOut);
            } finally {
                oldOut.unlockBuffer();
                oldOut.destroy();
            }
        }
    }

    @Override
    public void addCookie(Cookie cookie) {
        if(cookies == null){
            cookies = RecyclableUtil.newRecyclableList(12);
        }
        cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return nettyResponse.headers().contains((CharSequence) name);
    }

    @Override
    public String encodeURL(String url) {
        if(!httpServletObject.getHttpServletRequest().isRequestedSessionIdFromCookie()){
            //来自Cookie的Session ID,则客户端肯定支持Cookie，无需重写URL
            return url;
        }
        return url + ";" + HttpConstants.JSESSION_ID_URL + "=" + httpServletObject.getHttpServletRequest().getRequestedSessionId();
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
        checkCommitted();
        nettyResponse.setStatus(new HttpResponseStatus(sc, msg));
        commitFlag = true;
    }

    @Override
    public void sendError(int sc) throws IOException {
        checkCommitted();
        nettyResponse.setStatus(HttpResponseStatus.valueOf(sc));
        commitFlag = true;
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        checkCommitted();
        nettyResponse.setStatus(HttpResponseStatus.FOUND);
        getNettyHeaders().set(HttpHeaderConstants.LOCATION, (CharSequence)location);
        commitFlag = true;
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeaderObject(name,date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeaderObject(name,date);
    }

    @Override
    public void setHeader(String name, String value) {
        setHeaderObject(name,value);
    }

    @Override
    public void addHeader(String name, String value) {
        addHeaderObject(name,value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeaderObject(name,value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (isCommitted()) {
            return;
        }
        addHeaderObject(name,value);
    }

    @Override
    public void setContentType(String type) {
        if (type == null) {
            contentType = null;
            return;
        }

        MediaType mediaType = MediaType.parseFast(type);
        contentType = mediaType.toStringNoCharset();
        String charset = mediaType.getCharset();
        if (charset != null) {
            setCharacterEncoding(charset);
        }
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setStatus(int sc) {
        nettyResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm) {
        nettyResponse.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public int getStatus() {
        return nettyResponse.getStatus().code();
    }

    @Override
    public String getHeader(String name) {
        Object value = nettyResponse.headers().get((CharSequence) name);
        return value == null? null : String.valueOf(value);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List list = nettyResponse.headers().getAll((CharSequence) name);
        List<String> stringList = new LinkedList<>();
        for(Object charSequence : list){
            stringList.add(String.valueOf(charSequence));
        }
        return stringList;
    }

    @Override
    public Collection<String> getHeaderNames() {
        Set nameSet = nettyResponse.headers().names();

        List<String> nameList = new LinkedList<>();
        for(Object charSequence : nameSet){
            nameList.add(String.valueOf(charSequence));
        }
        return nameList;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    //Writer和OutputStream不能同时使用
    @Override
    public ServletOutputStreamWrapper getOutputStream() throws IOException {
        if(outWrapper.unwrap() == null){
            synchronized (outWrapper) {
                if(outWrapper.unwrap() == null) {
                    outWrapper.wrap(ServletOutputStream.newInstance(httpServletObject));
                }
            }
        }
        return outWrapper;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if(writer != null){
            return writer;
        }

        Charset charset;
        String characterEncoding = getCharacterEncoding();
        if(characterEncoding != null && characterEncoding.length() > 0){
            charset = Charset.forName(characterEncoding);
        }else {
            charset = Charset.forName(httpServletObject.getServletContext().getResponseCharacterEncoding());
        }
        writer = new ServletPrintWriter(getOutputStream(),charset);
        return writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        if(writer != null){
            return;
        }
        characterEncoding = charset;
    }

    @Override
    public void setContentLength(int len) {
        setContentLengthLong(len);
    }

    @Override
    public void setContentLengthLong(long len) {
        HttpHeaderUtil.setContentLength(nettyResponse, len);
        commitFlag = true;
    }

    @Override
    public void setBufferSize(int size) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
        getOutputStream().flush();
    }

    @Override
    public boolean isCommitted() {
        if(commitFlag){
            return true;
        }
        ServletOutputStream out = outWrapper.unwrap();
        return out != null && out.isClosed();
    }

    /**
     * 重置响应 (全部重置, 头部,状态, 响应缓冲区)
     */
    @Override
    public void reset() {
        checkCommitted();
        nettyResponse.headers().clear();
        nettyResponse.setStatus(NettyHttpResponse.DEFAULT_STATUS);
        if(outWrapper.unwrap() == null){
            return;
        }
        outWrapper.resetBuffer();
    }

    /**
     * 重置响应 (仅重置响应缓冲区)
     */
    @Override
    public void resetBuffer() {
        checkCommitted();
        if(outWrapper.unwrap() == null){
            return;
        }
        outWrapper.resetBuffer();
    }

    @Override
    public void setLocale(Locale loc) {
        locale = loc;
    }

    @Override
    public Locale getLocale() {
        return null == locale ? Locale.getDefault() : locale;
    }

    @Override
    public void recycle() {
        //回收顺序 -> 1.先关闭输出流, 2.(通过回调 CloseListener)回收netty响应 3.回收servlet响应
        outWrapper.recycle();
    }

    /**
     * 监听关闭流
     * 优化lambda实例数量, 减少gc次数
     */
    private class CloseListener implements ChannelFutureListener{
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            nettyResponse.recycle();
            httpServletObject = null;
            writer = null;
            cookies = null;
            contentType = null;
            characterEncoding = null;
            locale = null;
            if(commitFlag) {
                commitFlag = false;
            }
            ServletHttpServletResponse.RECYCLER.recycleInstance(ServletHttpServletResponse.this);
        }
    }
}
