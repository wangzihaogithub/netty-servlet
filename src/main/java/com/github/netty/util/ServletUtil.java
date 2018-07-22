package com.github.netty.util;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.*;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by acer01 on 2018/7/15/015.
 */
public class ServletUtil {

    private static HttpDataFactory HTTP_DATA_FACTORY;

    /**
     * The only date format permitted when generating HTTP headers.
     */
    public static final String RFC1123_DATE =
            "EEE, dd MMM yyyy HH:mm:ss zzz";

    //tomcat 居然是写死的地区 US
    public static final SimpleDateFormat FORMATS_TEMPLATE[] = {
            new SimpleDateFormat(RFC1123_DATE, Locale.getDefault()),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.getDefault()),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.getDefault())
    };


    public static String getCookieValue(Cookie[] cookies, String cookieName){
        if(cookies == null || cookieName == null) {
            return null;
        }
        for(Cookie cookie : cookies){
            if(cookie == null) {
                continue;
            }

            String name = cookie.getName();
            if(cookieName.equals(name)){
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void decodeByUrl(Map<String,String[]> parameterMap, String uri){
        QueryStringDecoder decoder = new QueryStringDecoder(uri);

        Map<String, List<String>> parameterListMap = decoder.parameters();
        for(Map.Entry<String,List<String>> entry : parameterListMap.entrySet()){
            List<String> value = entry.getValue();
            parameterMap.put(entry.getKey(), value.toArray(new String[value.size()]));
        }
    }

    public static String decodeCharacterEncoding(String contentType) {
        if (contentType == null) {
            return null;
        }
        int start = contentType.indexOf("charset=");
        if (start < 0) {
            return null;
        }
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0) {
            encoding = encoding.substring(0, end);
        }
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\""))
                && (encoding.endsWith("\""))) {
            encoding = encoding.substring(1, encoding.length() - 1);
        }
        return encoding.trim();
    }

    public static Cookie[] decodeCookie(String value){
        Set<io.netty.handler.codec.http.Cookie> nettyCookieSet = ServerCookieDecoder.decode(value);
        io.netty.handler.codec.http.Cookie[] nettyCookieArr = nettyCookieSet.toArray(new io.netty.handler.codec.http.Cookie[nettyCookieSet.size()]);
        int size = nettyCookieArr.length;
        Cookie[] cookies = new Cookie[size];
        for (int i=0; i< size; i++) {
            io.netty.handler.codec.http.Cookie nettyCookie = nettyCookieArr[i];
            Cookie cookie = new Cookie(nettyCookie.name(),nettyCookie.value());
            cookie.setComment(nettyCookie.comment());
            String domain = nettyCookie.domain();
            if(domain != null) {
                cookie.setDomain(domain);
            }
            cookie.setHttpOnly(nettyCookie.isHttpOnly());
            cookie.setMaxAge((int) nettyCookie.maxAge());
            cookie.setPath(nettyCookie.path());
            cookie.setVersion(nettyCookie.version());
            cookie.setSecure(nettyCookie.isSecure());

            cookies[i] = cookie;
        }
        return cookies;
    }

    public static void decodeByBody(Map<String,String[]> parameterMap,HttpRequest httpRequest){
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(getHttpDataFactory(), httpRequest);

        while (decoder.hasNext()){
            InterfaceHttpData data = decoder.next();

            /**
             * HttpDataType有三种类型
             * Attribute, FileUpload, InternalAttribute
             */
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                Attribute attribute = (Attribute) data;
                String name = attribute.getName();
                String value;
                try {
                    value = attribute.getValue();
                } catch (IOException e) {
                    e.printStackTrace();
                    value = "";
                }
                parameterMap.put(name, new String[]{value});
            }
        }

        decoder.destroy();
    }

    public static Long parseHeaderDate
            (String value, DateFormat[] formats) {
        Date date = null;
        for (int i = 0; (date == null) && (i < formats.length); i++) {
            try {
                date = formats[i].parse(value);
            } catch (ParseException e) {
                // Ignore
            }
        }
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    private static HttpDataFactory getHttpDataFactory(){
        if(HTTP_DATA_FACTORY == null){
            synchronized (ServletUtil.class) {
                if(HTTP_DATA_FACTORY == null) {
                    HTTP_DATA_FACTORY = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Disk
                }
            }
        }
        return HTTP_DATA_FACTORY;
    }

}
