package com.github.netty.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;

import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * @author 84215
 */
public class NioSocketChannel extends io.netty.channel.socket.nio.NioSocketChannel {

    public NioSocketChannel() {
        super();
    }

    public NioSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    public NioSocketChannel(SocketChannel socket) {
        super(socket);
    }

    public NioSocketChannel(Channel parent, SocketChannel socket) {
        super(parent, socket);
    }

    @Override
    protected boolean isCompatible(EventLoop eventLoop) {
        return super.isCompatible(eventLoop);
    }

    @Override
    public ChannelPipeline pipeline() {
        return super.pipeline();
    }

}
