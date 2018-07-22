package com.github.netty.util;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.*;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by acer01 on 2018/7/15/015.
 */
public class ServletUtil {

    private static final HttpDataFactory HTTP_DATA_FACTORY = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Disk

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
        if(cookies == null) {
            return null;
        }
        for(Cookie cookie : cookies){
            if(cookie == null) {
                continue;
            }

            String name = cookie.getName();
            if(cookieName.equalsIgnoreCase(name)){
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

    public static void decodeByBody(Map<String,String[]> parameterMap,FullHttpRequest fullHttpRequest){
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(HTTP_DATA_FACTORY, fullHttpRequest);

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

    public static Long internalParseDate
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
        return Long.valueOf(date.getTime());
    }
}
