package com.github.netty.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Created by acer01 on 2018/7/15/015.
 */
public class ServletAsyncContext implements AsyncContext {

    private ServletRequest servletRequest;
    private ServletResponse servletResponse;
    private ExecutorService executorService;

    // 0=初始, 1=开始
    private int status;
    private static final int STATUS_INIT = 0;
    private static final int STATUS_START = 1;
    private static final int STATUS_COMPLETE = 2;

    //毫秒
    private long timeout;

    private List<ServletAsyncListenerWrapper> asyncListenerWarpperList;

    private ServletContext servletContext;

    public ServletAsyncContext(ServletContext servletContext, ExecutorService executorService, ServletRequest servletRequest, ServletResponse servletResponse) {
        this.servletContext = servletContext;
        this.executorService = executorService;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.status = STATUS_INIT;
    }

    @Override
    public ServletRequest getRequest() {
        return servletRequest;
    }

    @Override
    public ServletResponse getResponse() {
        return servletResponse;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return true;
    }

    @Override
    public void dispatch() {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            String path = request.getServletPath();
            String pathInfo = request.getPathInfo();
            if (null != pathInfo) {
                path += pathInfo;
            }
            dispatch(path);
        }
    }

    @Override
    public void dispatch(String path) {
        dispatch(servletRequest.getServletContext(), path);
    }

    @Override
    public void dispatch(javax.servlet.ServletContext context, String path) {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        httpRequest.setAttribute(ASYNC_CONTEXT_PATH, httpRequest.getContextPath());
        httpRequest.setAttribute(ASYNC_PATH_INFO, httpRequest.getPathInfo());
        httpRequest.setAttribute(ASYNC_QUERY_STRING, httpRequest.getQueryString());
        httpRequest.setAttribute(ASYNC_REQUEST_URI, httpRequest.getRequestURI());
        httpRequest.setAttribute(ASYNC_SERVLET_PATH, httpRequest.getServletPath());

        ServletContext servletContext = unWrapper(context);
        if(servletContext == null){
            servletContext = this.servletContext;
        }

        ServletRequestDispatcher dispatcher = servletContext.getRequestDispatcher(path);

        start(()->{
            try {
                dispatcher.dispatch(httpRequest, servletResponse,DispatcherType.ASYNC);
            } catch (Throwable throwable) {
                //通知异常
                notifyEvent(listenerWrapper -> {
                    AsyncEvent event = new AsyncEvent(this,listenerWrapper.servletRequest,listenerWrapper.servletResponse,throwable);
                    try {
                        listenerWrapper.asyncListener.onError(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

    }

    @Override
    public void complete() {
        try {
            status = STATUS_COMPLETE;
            servletResponse.getOutputStream().close();
        } catch (IOException e) {
            // TODO notify listeners
            e.printStackTrace();
        }
    }

    @Override
    public void start(Runnable runnable) {
        status = STATUS_START;

        Future future = executorService.submit(runnable);

        try {
            //通知开始
            notifyEvent(listenerWrapper -> {
                AsyncEvent event = new AsyncEvent(this,listenerWrapper.servletRequest,listenerWrapper.servletResponse,null);
                try {
                    listenerWrapper.asyncListener.onStartAsync(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            future.get(getTimeout(), TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            //通知超时
            notifyEvent(listenerWrapper -> {
                try {
                    AsyncEvent event = new AsyncEvent(this,listenerWrapper.servletRequest,listenerWrapper.servletResponse,null);
                    listenerWrapper.asyncListener.onTimeout(event);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });
        }catch (Throwable throwable){
            //通知异常
            notifyEvent(listenerWrapper -> {
                AsyncEvent event = new AsyncEvent(this,listenerWrapper.servletRequest,listenerWrapper.servletResponse,throwable);
                try {
                    listenerWrapper.asyncListener.onError(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }finally {
            complete();

            //通知结束
            notifyEvent(listenerWrapper -> {
                try {
                    AsyncEvent event = new AsyncEvent(this,listenerWrapper.servletRequest,listenerWrapper.servletResponse,null);
                    listenerWrapper.asyncListener.onComplete(event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }



    @Override
    public void addListener(AsyncListener listener) {
        addListener(listener,servletRequest,servletResponse);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
        if(asyncListenerWarpperList == null){
            asyncListenerWarpperList = new LinkedList<>();
        }

        asyncListenerWarpperList.add(new ServletAsyncListenerWrapper(listener,servletRequest,servletResponse));
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
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
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }


    ServletContext unWrapper(javax.servlet.ServletContext context){
        return (ServletContext) context;
    }

    public boolean isStarted(){
        return status == STATUS_START;
    }

    private void notifyEvent(Consumer<ServletAsyncListenerWrapper> consumer){
        if(asyncListenerWarpperList != null) {
            for (ServletAsyncListenerWrapper listenerWrapper : asyncListenerWarpperList){
                consumer.accept(listenerWrapper);
            }
        }
    }

    class ServletAsyncListenerWrapper{
        AsyncListener asyncListener;
        ServletRequest servletRequest;
        ServletResponse servletResponse;

        ServletAsyncListenerWrapper(AsyncListener asyncListener, ServletRequest servletRequest, ServletResponse servletResponse) {
            this.asyncListener = asyncListener;
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;

            DispatcherType ASYNC = DispatcherType.ASYNC;
        }
    }
}
