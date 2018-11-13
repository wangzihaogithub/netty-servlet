package com.github.netty.servlet.handler;

import com.github.netty.springboot.NettyProperties;
import com.github.netty.core.constants.CoreConstants;
import com.github.netty.core.util.AbstractRecycler;
import com.github.netty.core.util.ByteBufAllocatorX;
import com.github.netty.core.util.Recyclable;
import com.github.netty.core.util.ExceptionUtil;
import com.github.netty.core.util.HttpHeaderUtil;
import com.github.netty.servlet.*;
import com.github.netty.servlet.support.HttpServletObject;
import com.github.netty.springboot.NettyEmbeddedServletContainer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicLong;

/**
 * http任务
 * @author 84215
 */
public class HttpTaskFactory implements TaskFactory{

    public static final AtomicLong SERVLET_AND_FILTER_TIME = new AtomicLong();
    public static final AtomicLong SERVLET_QUERY_COUNT = new AtomicLong();

    private static final AbstractRecycler<HttpTask> RECYCLER = new AbstractRecycler<HttpTask>() {
        @Override
        protected HttpTask newInstance() {
            return new HttpTask();
        }
    };

    @Override
    public Runnable newTask(ServletContext servletContext, NettyProperties config, ChannelHandlerContext context, Object msg) {
        if(CoreConstants.isEnableRawNetty()){
          return new RawTask(context,msg);
        }

        if(msg instanceof FullHttpRequest) {
            HttpTask instance = RECYCLER.getInstance();
            instance.httpServletObject = HttpServletObject.newInstance(
                    servletContext,
                    config,
                    ByteBufAllocatorX.forceDirectAllocator(context),
                    (FullHttpRequest) msg);;

            if(instance.httpServletObject.isHttpKeepAlive()){
                //分段写入, 用于流传输, 防止响应数据过大
                if(context.channel().pipeline().get(NettyEmbeddedServletContainer.HANDLER_CHUNKED_WRITE) == null) {
                    context.channel().pipeline().addAfter(
                            NettyEmbeddedServletContainer.HANDLER_HTTP_CODEC,NettyEmbeddedServletContainer.HANDLER_CHUNKED_WRITE, new ChunkedWriteHandler());
                }
            }
            return instance;
        }

        throw new IllegalStateException("不支持的类型");
    }

    /**
     * http任务
     */
    public static class HttpTask implements Runnable,Recyclable {
        private HttpServletObject httpServletObject;

        @Override
        public void recycle() {
            httpServletObject = null;
            RECYCLER.recycleInstance(HttpTask.this);
        }

        @Override
        public void run() {
            ServletHttpServletRequest httpServletRequest = httpServletObject.getHttpServletRequest();
            ServletHttpServletResponse httpServletResponse = httpServletObject.getHttpServletResponse();

            long beginTime = System.currentTimeMillis();
            try {
                ServletRequestDispatcher dispatcher = httpServletRequest.getRequestDispatcher(httpServletRequest.getRequestURI());
                if (dispatcher == null) {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                dispatcher.dispatch(httpServletRequest, httpServletResponse);

            }catch (Throwable throwable){
                ExceptionUtil.printRootCauseStackTrace(throwable);
            }finally {
                long totalTime = System.currentTimeMillis() - beginTime;
                SERVLET_AND_FILTER_TIME.addAndGet(totalTime);

                /*
                 * 如果不是异步， 或者异步已经结束
                 * 每个响应对象是只有当在servlet的service方法的范围内或在filter的doFilter方法范围内是有效的，除非该
                 * 组件关联的请求对象已经开启异步处理。如果相关的请求已经启动异步处理，那么直到AsyncContext的
                 * complete 方法被调用，请求对象一直有效。为了避免响应对象创建的性能开销，容器通常回收响应对象。
                 * 在相关的请求的startAsync 还没有调用时，开发人员必须意识到保持到响应对象引用，超出之上描述的范
                 * 围可能导致不确定的行为
                 */
                if(httpServletRequest.isAsync()){
                    ServletAsyncContext asyncContext = httpServletRequest.getAsyncContext();
                    //如果异步执行完成, 进行回收
                    if(asyncContext.isComplete()){
                        httpServletObject.recycle();
                    }else {
                        //标记主线程已经执行结束
                        httpServletRequest.getAsyncContext().markIoThreadOverFlag();
                    }
                }else {
                    //不是异步直接回收
                    httpServletObject.recycle();
                }

                HttpTask.this.recycle();
                SERVLET_QUERY_COUNT.incrementAndGet();
            }
        }
    }

    /**
     * 原生不加业务的代码, 用于测试原生的响应速度
     */
    public static class RawTask implements Runnable {
        private ChannelHandlerContext context;
        private Object msg;

        RawTask(ChannelHandlerContext context, Object msg) {
            this.context = context;
            this.msg = msg;
        }

        @Override
        public void run() {
            if(!(msg instanceof FullHttpRequest)){
                return;
            }

            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
            boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(fullHttpRequest);
            ByteBuf content = Unpooled.wrappedBuffer("ok".getBytes());
            FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

            HttpHeaderUtil.setKeepAlive(fullHttpResponse, isKeepAlive);
            if (isKeepAlive && !HttpHeaderUtil.isContentLengthSet(fullHttpResponse)) {
                HttpHeaderUtil.setContentLength(fullHttpResponse, content.readableBytes());
            }

            context.writeAndFlush(fullHttpResponse)
                    .addListener((ChannelFutureListener) future -> {
                        if(!isKeepAlive){
                            future.channel().close();
                        }
                        fullHttpRequest.release();
                    });
        }
    }
}
