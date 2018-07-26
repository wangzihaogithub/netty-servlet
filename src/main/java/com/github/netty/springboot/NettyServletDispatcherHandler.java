package com.github.netty.springboot;

import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletHttpServletResponse;
import com.github.netty.servlet.ServletRequestDispatcher;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author acer01
 *  2018/7/1/001
 */
@ChannelHandler.Sharable
public class NettyServletDispatcherHandler extends SimpleChannelInboundHandler<ServletHttpServletRequest> {

    private ServletContext servletContext;

    public NettyServletDispatcherHandler(ServletContext servletContext) {
        super();
        this.servletContext = servletContext;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest) throws Exception {
        ServletHttpServletResponse servletResponse = new ServletHttpServletResponse(ctx, servletContext,servletRequest);

        try {
            ServletRequestDispatcher dispatcher = servletContext.getRequestDispatcher(servletRequest.getRequestURI());
            if (dispatcher == null) {
                servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            dispatcher.dispatch(servletRequest, servletResponse, DispatcherType.REQUEST);
        } finally {
            if (!servletRequest.isAsyncStarted()) {
                servletResponse.getOutputStream().close();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exceptionCaught");
        if(null != cause) {
            cause.printStackTrace();
        }
        if(null != ctx) {
            ctx.close();
        }
    }

}
