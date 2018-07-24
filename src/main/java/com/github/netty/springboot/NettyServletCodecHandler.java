package com.github.netty.springboot;

import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpServletRequest;
import com.github.netty.servlet.ServletInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

/**
 * channel激活时， 开启一个新的输入流
 * 有信息/请求进入时，封装请求和响应对象，执行读操作
 * channel恢复时，关闭输入流，等待下一次连接到来
 * @author 84215
 */
public class NettyServletCodecHandler extends SimpleChannelInboundHandler<HttpObject> {

    private ServletContext servletContext;
    private ServletInputStream inputStream; // FIXME this feels wonky, need a better approach

    public NettyServletCodecHandler(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        inputStream = new ServletInputStream(ctx.channel());
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;

            if (HttpHeaderUtil.is100ContinueExpected(request)) { //请求头包含Expect: 100-continue
                ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE), ctx.voidPromise());
            }

            ServletHttpServletRequest servletRequest = new ServletHttpServletRequest(inputStream, servletContext, request);
            ctx.fireChannelRead(servletRequest);
        }

        if (msg instanceof HttpContent) { //EmptyLastHttpContent, DefaultLastHttpContent
            inputStream.addContent((HttpContent) msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        inputStream.close();
    }
}