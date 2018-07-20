package com.github.netty.core;

import com.github.netty.util.ClassIdFactory;
import com.github.netty.util.ProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

public class MyNioSocketChannel extends NioSocketChannel {

    private NioEventLoop nioEventLoop;
    private ChannelPipeline pipelineProxy;

    public MyNioSocketChannel() {
        super();
    }

    public MyNioSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    public MyNioSocketChannel(SocketChannel socket) {
        super(socket);
    }

    public MyNioSocketChannel(Channel parent, SocketChannel socket) {
        super(parent, socket);
    }

    private void init(EventLoop eventLoop){
        this.nioEventLoop = (NioEventLoop) ProxyUtil.unWrapper(eventLoop);
        this.pipelineProxy = ProxyUtil.newProxyByJdk(pipeline(), ClassIdFactory.newIdName(this,"Pipeline"),true);
    }

    @Override
    protected boolean isCompatible(EventLoop eventLoop) {
        return super.isCompatible((EventLoop) ProxyUtil.unWrapper(eventLoop));
    }

    @Override
    public ChannelPipeline pipeline() {
        if(pipelineProxy != null){
            return pipelineProxy;
        }
        return super.pipeline();
    }

}
